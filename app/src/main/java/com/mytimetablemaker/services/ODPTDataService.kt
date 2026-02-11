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

// Railway DTO for ODPT API data.
// Maps JSON fields to internal models.
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

// Bus route pattern DTO for ODPT API data.
// Maps JSON fields to internal models.
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

// ODPT data parser to TransportationLine models.
object ODPTParser {
    
    // Parse bus route patterns into TransportationLine objects.
    fun parseBusRoutes(data: ByteArray): List<TransportationLine> {
        val jsonString = String(data)
        val jsonElement = JsonParser.parseString(jsonString)
        if (!jsonElement.isJsonArray) {
            return emptyList()
        }
        val jsonArray = jsonElement.asJsonArray
        
        // Filter to odpt:BusroutePattern only.
        val busOnlyData = jsonArray.mapNotNull { element: JsonElement ->
            val jsonObject = element.asJsonObject
            val itemType = jsonObject.get("@type")?.asString
            if (itemType == "odpt:BusroutePattern") element else null
        }
        
        // If no bus data found, return empty.
        if (busOnlyData.isEmpty()) {
            return emptyList()
        }
        
        // Decode filtered data into DTOs.
        val gson = Gson()
        val filteredJson = gson.toJson(busOnlyData)
        val dtoListType = object : TypeToken<List<BusRoutePatternDTO>>() {}.type
        val dtos: List<BusRoutePatternDTO> = try {
            gson.fromJson(filteredJson, dtoListType)
        } catch (e: Exception) {
            android.util.Log.d("ODPTParser", "🚌 parseBusRoutes: Failed to decode DTOs: ${e.message}", e)
            return emptyList()
        }
        
        // Map DTOs to models.
        return dtos.mapNotNull { dto: BusRoutePatternDTO ->
            // Ensure sameAs and title are present.
            val sameAs = dto.sameAs ?: return@mapNotNull null
            var title = dto.title ?: return@mapNotNull null
            
            // Fix trailing "行行" to "行".
            if (title.endsWith("行行")) {
                title = title.dropLast(1)
            }
            
            // Extract English name from odpt:busroute.
            val englishName = dto.busRoute?.busRouteEnglishName()
            val busStopPoleOrder = dto.busStopPoleOrder?.mapNotNull { stopDto ->
                val busStopPole = stopDto.busStopPole ?: return@mapNotNull null
                val stopName = stopDto.note?.takeIf { it.isNotBlank() }
                    ?: busStopPole.split(".").lastOrNull().orEmpty()

                if (stopName.length <= 2) {
                    android.util.Log.d(
                        "ODPTParser",
                        "🚌 busStop short name: line=$sameAs pole=$busStopPole note=${stopDto.note} name=$stopName"
                    )
                }

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
    
    // Parse railway data from JSON files.
    // Uses DTOs for type-safe decoding.
    fun parseRailwayRoutes(data: ByteArray): List<TransportationLine> {
        try {
            val jsonString = String(data)
            // Ensure the payload is a JSON array.
            val jsonElement = JsonParser.parseString(jsonString)
            if (!jsonElement.isJsonArray) {
                throw ODPTError.InvalidData("Railway JSON is not an array")
            }
            
            val jsonArray = jsonElement.asJsonArray
            
            // Convert JsonObject to DTO (supports odpt:color and odpt:lineColor).
            val dtos = jsonArray.mapNotNull { element: JsonElement ->
                val jsonObject = element.asJsonObject
                val title = jsonObject.get("dc:title")?.asString
                val sameAs = jsonObject.get("owl:sameAs")?.asString
                
                if (title == null || sameAs == null) {
                    return@mapNotNull null
                }
                
                // Convert JsonObject to Map<String, Any> for extensions.
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
                                        // Skip non-primitive elements in arrays.
                                        null
                                    }
                                }
                            }
                        }
                        is JsonObject -> {
                            // Convert JsonObject to Map<String, String> for railwayTitle.
                            value.entrySet().associate { e ->
                                e.key to when (val nestedValue = e.value) {
                                    is com.google.gson.JsonPrimitive -> {
                                        if (nestedValue.isString) {
                                            nestedValue.asString
                                        } else {
                                            // Convert non-string primitives to string.
                                            nestedValue.toString()
                                        }
                                    }
                                    else -> {
                                        // Skip unexpected nested structures.
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
                
                // Extract railwayTitle using extension.
                val railwayTitle = elementMap.odptRailwayTitle()
                
                // Use LineExtensions utilities for ODPT extraction.
                val lineColor = elementMap.odptLineColor()
                val destinationStation = elementMap.odptDestinationStation()
                
                // Extract rail direction info for timetable calls.
                val ascendingRailDirection = jsonObject.get("odpt:ascendingRailDirection")?.asString
                val descendingRailDirection = jsonObject.get("odpt:descendingRailDirection")?.asString
                
                // Parse odpt:stationOrder into TransportationStop list.
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
            
            // Map DTOs to models.
            return dtos.map { dto: RailwayDTO ->
                // Fix trailing "行行" to "行".
                val fixedTitle = if (dto.title.endsWith("行行")) dto.title.dropLast(1) else dto.title
                
                // Fix railwayTitle.ja trailing "行行".
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
                    lineDirection = dto.ascendingRailDirection, // Backward compatibility.
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

// ODPT API service for HTTP, caching, and data retrieval.
class ODPTDataService(private val context: Context) {
    // HTTP client configuration and timeouts.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)      // 30 seconds for connection.
        .readTimeout(30, TimeUnit.SECONDS)         // 30 seconds for reading.
        .writeTimeout(30, TimeUnit.SECONDS)        // 30 seconds for writing.
        .followRedirects(true)                      // Follow redirects.
        .followSslRedirects(true)
        .cookieJar(CookieJar.NO_COOKIES)           // Disable cookie handling.
        .build()
    private val cache: CacheStore = CacheStore(context)
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ODPTDataService", Context.MODE_PRIVATE)

    // Configure request headers and conditional caching.
    // ODPT uses acl:consumerKey in the URL, not Authorization.
    private fun configureRequest(
        requestBuilder: Request.Builder,
        conditionalHeaders: Pair<String?, String?>? = null
    ): Request.Builder {
        // ODPT uses URL query parameter acl:consumerKey.
        requestBuilder.addHeader("Accept", "application/json")
        
        conditionalHeaders?.let { (etag, lastModified) ->
            etag?.let { requestBuilder.addHeader("If-None-Match", it) }
            lastModified?.let { requestBuilder.addHeader("If-Modified-Since", it) }
        }
        
        return requestBuilder
    }
    
    // Fetch data for a specific operator endpoint.
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
        
        // Save ETag/Last-Modified for future conditional GET.
        if (response.code == 200) {
            // Save ETag and Last-Modified headers.
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
    
    // ETag/Last-Modified helpers for caching.
    
    // Get ETag for a specific operator.
    private fun getETag(transportOperator: LocalDataSource): String? {
        val etagKey = "${transportOperator.fileName()}_etag"
        return sharedPreferences.getString(etagKey, null)
    }
    
    // Save ETag for a specific operator.
    private fun saveETag(etag: String, transportOperator: LocalDataSource) {
        val etagKey = "${transportOperator.fileName()}_etag"
        sharedPreferences.edit {
            putString(etagKey, etag)
        }
    }
    
    // Get Last-Modified for a specific operator.
    private fun getLastModified(transportOperator: LocalDataSource): String? {
        val lastModifiedKey = "${transportOperator.fileName()}_last_modified"
        return sharedPreferences.getString(lastModifiedKey, null)
    }
    
    // Save Last-Modified for a specific operator.
    private fun saveLastModified(lastModified: String, transportOperator: LocalDataSource) {
        val lastModifiedKey = "${transportOperator.fileName()}_last_modified"
        sharedPreferences.edit {
            putString(lastModifiedKey, lastModified)
        }
    }
    
    // Conditional GET check for operator updates.
    // Only runs when cache and headers exist.
    suspend fun checkIndividualOperatorForUpdates(
        transportOperator: LocalDataSource,
    ): Boolean = withContext(Dispatchers.IO) {
        // Check if cached data exists.
        val cacheKey = transportOperator.fileName()
        val cachedData = cache.loadData(cacheKey) ?: return@withContext false
        
        // Check for ETag/Last-Modified.
        val etag = getETag(transportOperator)
        val lastModified = getLastModified(transportOperator)
        
        // No headers: skip update check.
        if (etag == null && lastModified == null) {
            return@withContext false
        }
        
        // Use conditional GET with ETag/Last-Modified.
        try {
            val urlString = transportOperator.apiLink(
                dataType = APIDataType.LINE,
                transportationKind = transportOperator.transportationType()
            )
            
            val url = urlString.toHttpUrl()
            
            val requestBuilder = Request.Builder().url(url)
            configureRequest(requestBuilder, Pair(etag, lastModified))
            
            val response = client.newCall(requestBuilder.build()).execute()
            
            when (response.code) {
                304 -> {
                    // Server confirms no changes.
                    return@withContext false
                }
                200 -> {
                    // Get current ETag.
                    val currentEtag = getETag(transportOperator)
                    
                    // Get new ETag from response.
                    val newEtag = response.header("ETag")
                    
                    // Fast path: if ETags match, skip update.
                    if (currentEtag != null && newEtag != null && currentEtag == newEtag) {
                        return@withContext false // ETag confirms no change.
                    }
                    
                    // Save ETag and Last-Modified headers.
                    newEtag?.let { saveETag(it, transportOperator) }
                    response.header("Last-Modified")?.let { saveLastModified(it, transportOperator) }
                    
                    // Fallback: compare response data to cache.
                    val responseData = response.body.bytes()
                    val dataMatches = cachedData.contentEquals(responseData)
                    return@withContext !dataMatches
                }
                else -> {
                    android.util.Log.d("ODPTDataService", "${transportOperator.operatorDisplayName(context)}: Unexpected response status: ${response.code}")
                    return@withContext false // Don't update on error.
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("ODPTDataService", "${transportOperator.operatorDisplayName(context)}: Request failed: ${e.message}", e)
            return@withContext false // Don't update on error.
        }
    }
    
    // Common operator update flow.
    private suspend fun processOperatorUpdate(
        transportOperator: LocalDataSource,
    ): Pair<ByteArray, Boolean> {
        val needsUpdate = checkIndividualOperatorForUpdates(transportOperator)
        if (needsUpdate) {
            val data = fetchIndividualOperatorData(transportOperator)
            
            // Write updated data to file.
            writeIndividualOperatorDataToFile(data, transportOperator)
            
            // Update cache with new data.
            val cacheKey = transportOperator.fileName()
            cache.saveData(data, cacheKey)
            
            return Pair(data, true)
        } else {
            return Pair(ByteArray(0), false)
        }
    }
    
    // Update operator data and save to storage.
    suspend fun updateIndividualOperator(
        transportOperator: LocalDataSource,
    ): Result<Unit> {
        return try {
            val (_, _) = processOperatorUpdate(transportOperator)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.d("ODPTDataService", "${transportOperator.operatorDisplayName(context)}: Failed to update data: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Write operator data to local storage.
    suspend fun writeIndividualOperatorDataToFile(
        data: ByteArray,
        transportOperator: LocalDataSource
    ) = withContext(Dispatchers.IO) {
        val fileManager = context.filesDir
        val lineDataDirectory = File(fileManager, "LineData")
        
        // Create LineData directory if needed.
        lineDataDirectory.mkdirs()
        
        // Use consistent file naming.
        val fileName = transportOperator.fileName()
        val file = File(lineDataDirectory, fileName)
        
        // Write data to file.
        file.writeBytes(data)
    }
    
    // Fetch ODPT data and return payload + response.
    suspend fun fetchODPTData(urlString: String): Pair<ByteArray, Response> = withContext(Dispatchers.IO) {
        val url = urlString.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(url)
        configureRequest(requestBuilder)
        
        val response = client.newCall(requestBuilder.build()).execute()
        val responseData = response.body.bytes()
        
        Pair(responseData, response)
    }
    
    // Convert JsonObject to Map recursively.
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
                    // Recursively parse array elements.
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
    
    // Parse JSON array from byte array.
    // Handles array and single object responses.
    fun parseJSONArray(data: ByteArray): List<Map<*, *>> {
        val jsonString = String(data)
        val jsonElement = JsonParser.parseString(jsonString)
        
        // Handle array and single object.
        val jsonArray = if (jsonElement.isJsonArray) {
            jsonElement.asJsonArray
        } else if (jsonElement.isJsonObject) {
            // Wrap a single object in an array.
            JsonArray().apply { add(jsonElement) }
        } else {
            android.util.Log.d("ODPTDataService", "parseJSONArray: Invalid JSON type: ${jsonElement.javaClass.simpleName}")
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

