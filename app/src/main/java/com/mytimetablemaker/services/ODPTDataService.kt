package com.mytimetablemaker.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.mytimetablemaker.models.*
import com.mytimetablemaker.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.util.concurrent.TimeUnit

// DTO for railway data from ODPT API
data class RailwayDTO(
    val title: String,
    val sameAs: String,
    val operatorCode: String? = null,
    val lineColor: String? = null,
    val startStation: String? = null,
    val endStation: String? = null,
    val destinationStation: String? = null,
    val railwayTitle: LocalizedTitle? = null,
    val lineCode: String? = null,
    val ascendingRailDirection: String? = null,
    val descendingRailDirection: String? = null,
    val stationOrder: List<TransportationStop>? = null,
    val date: String? = null
)

// DTO for bus route pattern data from ODPT API
data class BusRoutePatternDTO(
    @SerializedName(value = "dc:title", alternate = ["title"])
    val title: String? = null,
    @SerializedName(value = "owl:sameAs", alternate = ["odpt:sameAs", "sameAs"])
    val sameAs: String? = null,
    @SerializedName("odpt:operator")
    val operatorCode: String? = null,
    @SerializedName("odpt:busroute")
    val busRoute: String? = null,
    @SerializedName("odpt:pattern")
    val pattern: String? = null,
    @SerializedName("odpt:direction")
    val direction: String? = null,
    @SerializedName("odpt:busstopPoleOrder")
    val busStopPoleOrder: List<BusStopPoleOrderDTO>? = null,
    @SerializedName("odpt:note")
    val note: String? = null,
    @SerializedName("dc:date")
    val date: String? = null
)

data class BusStopPoleOrderDTO(
    @SerializedName("odpt:busstopPole")
    val busStopPole: String? = null,
    @SerializedName("odpt:index")
    val index: Int? = null,
    @SerializedName("odpt:note")
    val note: String? = null
)

// Converts raw JSON data from ODPT API to internal TransportationLine models
object ODPTParser {
    
    // Parse bus route pattern data and convert to TransportationLine objects
    fun parseBusRoutes(data: ByteArray): List<TransportationLine> {
        val jsonString = String(data)
        val jsonElement = JsonParser.parseString(jsonString)
        if (!jsonElement.isJsonArray) {
            return emptyList()
        }
        val jsonArray = jsonElement.asJsonArray
        
        // Filter to only bus route patterns (odpt:BusroutePattern)
        val busOnlyData = jsonArray.mapNotNull { element: JsonElement ->
            val jsonObject = element.asJsonObject
            val itemType = jsonObject.get("@type")?.asString
            if (itemType == "odpt:BusroutePattern") element else null
        }
        
        // If no bus data found, skip it
        if (busOnlyData.isEmpty()) {
            return emptyList()
        }
        
        // Decode filtered data into DTOs (Swift implementation parity)
        val gson = Gson()
        val filteredJson = gson.toJson(busOnlyData)
        val dtoListType = object : TypeToken<List<BusRoutePatternDTO>>() {}.type
        val dtos: List<BusRoutePatternDTO> = try {
            gson.fromJson(filteredJson, dtoListType)
        } catch (_: Exception) {
            return emptyList()
        }
        
        return dtos.mapNotNull { dto: BusRoutePatternDTO ->
            // Ensure sameAs and title are not null (should be guaranteed by filter above)
            val sameAs = dto.sameAs ?: return@mapNotNull null
            var title = dto.title ?: return@mapNotNull null
            
            // Fix trailing "行行" to "行"
            if (title.endsWith("行行")) {
                title = title.dropLast(1)
            }
            
            // Extract English name from odpt:busroute value using LineExtensions
            val englishName = dto.busRoute?.busRouteEnglishName()
            val busStopPoleOrder = dto.busStopPoleOrder?.mapNotNull { stopDto ->
                val busStopPole = stopDto.busStopPole ?: return@mapNotNull null
                val stopName = stopDto.note?.takeIf { it.isNotBlank() }
                    ?: busStopPole.split(".").lastOrNull().orEmpty()

                TransportationStop(
                    kind = TransportationLineKind.BUS,
                    name = stopName,
                    code = busStopPole,
                    index = stopDto.index,
                    lineCode = sameAs,
                    title = generateBusStopTitle(stopDto.note.orEmpty(), busStopPole),
                    note = stopDto.note,
                    busStopPole = busStopPole
                )
            }
            
            TransportationLine(
                id = sameAs,
                kind = TransportationLineKind.BUS,
                name = title,
                code = sameAs,
                operatorCode = dto.operatorCode,
                lineColor = null,
                startStation = null,
                endStation = null,
                destinationStation = null,
                railwayTitle = LocalizedTitle(ja = title, en = englishName), // title is already fixed above
                lineCode = null,
                lineDirection = null,
                ascendingRailDirection = null,
                descendingRailDirection = null,
                busRoute = dto.busRoute,
                pattern = dto.pattern,
                busDirection = dto.direction,
                busStopPoleOrder = busStopPoleOrder,
                title = title
            )
        }
    }
    
    // Parse railway data from JSON (API and local file formats supported)
    fun parseRailwayRoutes(data: ByteArray): List<TransportationLine> {
        try {
            val jsonString = String(data)
            // Check if it's a JSON array or object
            val jsonElement = JsonParser.parseString(jsonString)
            if (!jsonElement.isJsonArray) {
                throw ODPTError.InvalidData("Railway JSON is not an array")
            }
            
            val jsonArray = jsonElement.asJsonArray
            
            // Convert JSON array to DTO objects
            val dtos = jsonArray.mapNotNull { element: JsonElement ->
                val jsonObject = element.asJsonObject
                val title = jsonObject.get("dc:title")?.asString
                val sameAs = jsonObject.get("owl:sameAs")?.asString
                
                if (title == null || sameAs == null) {
                    return@mapNotNull null
                }
                
                // Convert JsonObject to Map<String, Any> for extension functions
                val elementMap = jsonObject.entrySet().associate { entry: Map.Entry<String, JsonElement> ->
                    entry.key to when (val value = entry.value) {
                        is com.google.gson.JsonPrimitive -> {
                            if (value.isString) value.asString
                            else if (value.isNumber) value.asNumber
                            else if (value.isBoolean) value.asBoolean
                            else null
                        }
                        is JsonArray -> {
                            value.mapNotNull { element ->
                                when (element) {
                                    is com.google.gson.JsonPrimitive -> {
                                        if (element.isString) element.asString
                                        else null
                                    }
                                    else -> {
                                        // Skip non-primitive elements in array
                                        // This is expected for some ODPT API fields that may contain objects
                                        // The data will be skipped but the app will continue to work
                                        null
                                    }
                                }
                            }
                        }
                        is JsonObject -> {
                            // Convert JsonObject to Map<String, String> for railwayTitle
                            // Each value should be a JsonPrimitive (string)
                            value.entrySet().associate { e ->
                                e.key to when (val nestedValue = e.value) {
                                    is com.google.gson.JsonPrimitive -> {
                                        if (nestedValue.isString) {
                                            nestedValue.asString
                                        } else {
                                            // For non-string primitives, convert to string
                                            nestedValue.toString()
                                        }
                                    }
                                    else -> {
                                        // If value is not JsonPrimitive, skip it or use empty string
                                        // This handles unexpected nested structures
                                        ""
                                    }
                                }
                            }
                        }
                        else -> null
                    }
                }.filterValues { it != null }.let {
                    @Suppress("UNCHECKED_CAST")
                    it as Map<String, Any>
                }
                
                // Extract railwayTitle using odptRailwayTitle extension function
                val railwayTitle = elementMap.odptRailwayTitle()
                
                // Use LineExtensions utilities for ODPT data extraction
                val lineColor = elementMap.odptLineColor()
                val destinationStation = elementMap.odptDestinationStation()
                
                // Extract rail direction information for timetable API calls
                val ascendingRailDirection = jsonObject.get("odpt:ascendingRailDirection")?.asString
                val descendingRailDirection = jsonObject.get("odpt:descendingRailDirection")?.asString
                
                // Parse odpt:stationOrder to create TransportationStop list
                // Uses odpt:stationOrder from Railway JSON to create station list
                val stationOrderArray = jsonObject.get("odpt:stationOrder")?.asJsonArray
                val stationOrder = stationOrderArray?.mapNotNull { stationElement ->
                    if (stationElement is JsonObject) {
                        val stationCode = stationElement.get("odpt:station")?.asString
                        val stationIndex = stationElement.get("odpt:index")?.asInt
                        val stationTitleObj = stationElement.get("odpt:stationTitle")?.asJsonObject
                        val stationTitle = stationTitleObj?.let { titleObj ->
                            LocalizedTitle(
                                ja = titleObj.get("ja")?.asString,
                                en = titleObj.get("en")?.asString
                            )
                        }
                        val stationName = stationTitle?.ja ?: stationTitle?.en ?: stationCode?.split(".")?.lastOrNull() ?: ""
                        
                        if (stationCode != null) {
                            TransportationStop(
                                kind = TransportationLineKind.RAILWAY,
                                name = stationName,
                                code = stationCode,
                                index = stationIndex,
                                lineCode = sameAs,
                                title = stationTitle,
                                note = null,
                                busStopPole = null
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                
                RailwayDTO(
                    title = title,
                    sameAs = sameAs,
                    operatorCode = jsonObject.get("odpt:operator")?.asString,
                    lineColor = lineColor,
                    startStation = jsonObject.get("odpt:startStation")?.asString,
                    endStation = jsonObject.get("odpt:endStation")?.asString,
                    destinationStation = destinationStation,
                    railwayTitle = railwayTitle,
                    lineCode = jsonObject.get("odpt:lineCode")?.asString,
                    ascendingRailDirection = ascendingRailDirection,
                    descendingRailDirection = descendingRailDirection,
                    stationOrder = stationOrder,
                    date = jsonObject.get("dc:date")?.asString
                )
            }
            
            // Convert DTOs to TransportationLine models
            return dtos.map { dto: RailwayDTO ->
                // Fix trailing "行行" to "行"
                val fixedTitle = if (dto.title.endsWith("行行")) dto.title.dropLast(1) else dto.title
                
                // Fix railwayTitle's ja field if it ends with "行行"
                val fixedRailwayTitle = dto.railwayTitle?.let { title ->
                    if (title.ja?.endsWith("行行") == true) title.copy(ja = title.ja.dropLast(1)) else title
                }
                
                TransportationLine(
                    id = dto.sameAs,
                    kind = TransportationLineKind.RAILWAY,
                    name = fixedTitle,
                    code = dto.sameAs,
                    operatorCode = dto.operatorCode,
                    lineColor = dto.lineColor,
                    startStation = dto.startStation,
                    endStation = dto.endStation,
                    destinationStation = dto.destinationStation,
                    railwayTitle = fixedRailwayTitle,
                    lineCode = dto.lineCode,
                    lineDirection = dto.ascendingRailDirection, // Keep for backward compatibility
                    ascendingRailDirection = dto.ascendingRailDirection,
                    descendingRailDirection = dto.descendingRailDirection,
                    stationOrder = dto.stationOrder,
                    busRoute = null,
                    pattern = null,
                    busDirection = null,
                    busStopPoleOrder = null,
                    title = null
                )
            }
        } catch (_: Exception) {
            throw ODPTError.InvalidData("Failed to parse railway JSON")
        }
    }
}

// Handles HTTP communication with ODPT API (authentication, caching, retrieval)
class ODPTDataService(private val context: Context) {
    // Configure HTTP client with 30-second timeouts and redirect support
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)      // 30 seconds for connection
        .readTimeout(30, TimeUnit.SECONDS)         // 30 seconds for reading
        .writeTimeout(30, TimeUnit.SECONDS)        // 30 seconds for writing
        .followRedirects(true)                      // Automatically handle redirects
        .followSslRedirects(true)
        .cookieJar(CookieJar.NO_COOKIES)           // Disable cookie handling for API requests
        .build()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ODPTDataService", Context.MODE_PRIVATE)

    // Configure request headers (authentication, conditional headers for caching)
    private fun configureRequest(
        requestBuilder: Request.Builder,
        conditionalHeaders: Pair<String?, String?>? = null
    ): Request.Builder {
        // ODPT API uses URL query parameter acl:consumerKey for authentication
        // Authorization header is not used for ODPT API
        requestBuilder.addHeader("Accept", "application/json")
        
        conditionalHeaders?.let { (etag, lastModified) ->
            etag?.let { requestBuilder.addHeader("If-None-Match", it) }
            lastModified?.let { requestBuilder.addHeader("If-Modified-Since", it) }
        }
        
        return requestBuilder
    }
    
    // Fetch data for individual transportation operators using their API endpoints
    suspend fun fetchIndividualOperatorData(
        transportOperator: LocalDataSource,
    ): ByteArray = withContext(Dispatchers.IO) {
        val urlString = transportOperator.apiLink(
            dataType = APIDataType.LINE,
            transportationKind = transportOperator.transportationType()
        )
        
        val url = urlString.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(url)
        configureRequest(requestBuilder)
        
        val response = client.newCall(requestBuilder.build()).execute()
        
        // Check for successful response and save ETag/Last-Modified for future conditional GET
        if (response.code == 200) {
            // Save ETag and Last-Modified headers for future conditional requests
            response.header("ETag")?.let { etag ->
                saveETag(etag, transportOperator)
            }
            
            response.header("Last-Modified")?.let { lastModified ->
                saveLastModified(lastModified, transportOperator)
            }

            response.body.bytes()
        } else {
            throw ODPTError.NetworkError("HTTP ${response.code}")
        }
    }
    
    // ETag and Last-Modified header management for conditional GET requests
    
    private fun saveETag(etag: String, transportOperator: LocalDataSource) {
        val etagKey = "${transportOperator.fileName()}_etag"
        sharedPreferences.edit {
            putString(etagKey, etag)
        }
    }
    
    private fun saveLastModified(lastModified: String, transportOperator: LocalDataSource) {
        val lastModifiedKey = "${transportOperator.fileName()}_last_modified"
        sharedPreferences.edit {
            putString(lastModifiedKey, lastModified)
        }
    }
    
    // Write operator data to local storage (LineData directory)
    suspend fun writeIndividualOperatorDataToFile(
        data: ByteArray,
        transportOperator: LocalDataSource
    ) = withContext(Dispatchers.IO) {
        val fileManager = context.filesDir
        val lineDataDirectory = File(fileManager, "LineData")
        
        // Create LineData directory (will create automatically if needed)
        lineDataDirectory.mkdirs()
        
        // Use consistent file naming with LocalDataSource.fileName
        val fileName = transportOperator.fileName()
        val file = File(lineDataDirectory, fileName)
        
        // Write data to file atomically for safety
        file.writeBytes(data)
    }
    
    // Fetch data from ODPT API and return response data and status
    suspend fun fetchODPTData(urlString: String): Pair<ByteArray, Response> = withContext(Dispatchers.IO) {
        val url = urlString.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(url)
        configureRequest(requestBuilder)
        
        val response = client.newCall(requestBuilder.build()).execute()
        val responseData = response.body.bytes()
        
        Pair(responseData, response)
    }
    
    // Recursively convert JsonObject to Map for nested structures
    private fun parseJsonObjectToMap(jsonObject: JsonObject): Map<*, *> {
        return jsonObject.entrySet().associate { entry ->
            entry.key to when (val value = entry.value) {
                is com.google.gson.JsonPrimitive -> {
                    when {
                        value.isString -> value.asString
                        value.isNumber -> value.asNumber
                        value.isBoolean -> value.asBoolean
                        else -> null
                    }
                }
                is JsonArray -> {
                    // Recursively parse array elements
                    value.mapNotNull { element ->
                        when {
                            element.isJsonPrimitive -> element.asString
                            element.isJsonObject -> parseJsonObjectToMap(element.asJsonObject)
                            else -> null
                        }
                    }
                }
                is JsonObject -> parseJsonObjectToMap(value)
                else -> null
            }
        }
    }
    
    // Parse JSON array from byte array (handles both arrays and single objects)
    fun parseJSONArray(data: ByteArray): List<Map<*, *>> {
        val jsonString = String(data)
        val jsonElement = JsonParser.parseString(jsonString)
        
        // Handle both JSON array and single JSON object
        val jsonArray = if (jsonElement.isJsonArray) {
            jsonElement.asJsonArray
        } else if (jsonElement.isJsonObject) {
            // If it's a single object, wrap it in an array
            JsonArray().apply { add(jsonElement) }
        } else {
            return emptyList()
        }
        
        return jsonArray.mapNotNull { element ->
            if (!element.isJsonObject) {
                return@mapNotNull null
            }
            parseJsonObjectToMap(element.asJsonObject)
        }
    }
}

