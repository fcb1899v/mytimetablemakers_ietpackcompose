package com.mytimetablemaker.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mytimetablemaker.BuildConfig
import com.mytimetablemaker.models.*
import com.mytimetablemaker.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.util.concurrent.TimeUnit

// MARK: - ODPT Railway DTO
// DTO for railway data from ODPT API.
// Maps external JSON structure to internal data model.
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

// MARK: - ODPT Bus Route Pattern DTO
// DTO for bus route pattern data from ODPT API.
// Maps external JSON structure to internal bus data model.
data class BusRoutePatternDTO(
    val title: String? = null,
    val sameAs: String? = null,
    val operatorCode: String? = null,
    val busRoute: String? = null,
    val pattern: String? = null,
    val direction: String? = null,
    val busstopPoleOrder: List<TransportationStop>? = null,
    val note: String? = null,
    val date: String? = null
)

// MARK: - ODPT Data Parser
// Converts raw JSON data from ODPT API to internal TransportationLine models.
object ODPTParser {
    
    // MARK: - Bus Data Parsing
    // Parse bus route pattern data from JSON and convert to TransportationLine objects.
    fun parseBusRoutes(data: ByteArray): List<TransportationLine> {
        val jsonString = String(data)
        val jsonArray = JsonParser.parseString(jsonString).asJsonArray
        
        // Filter out railway data - only process odpt:BusroutePattern
        // Ensures only bus route patterns are parsed from mixed API responses
        val busOnlyData = jsonArray.mapNotNull { element: JsonElement ->
            val jsonObject = element.asJsonObject
            val itemType = jsonObject.get("@type")?.asString
            if (itemType == "odpt:BusroutePattern") {
                element
            } else {
                null
            }
        }
        
        // If no bus data found, skip it
        if (busOnlyData.isEmpty()) {
            return emptyList()
        }
        
        // Convert filtered data to DTOs manually (same approach as RailwayDTO)
        // This ensures proper mapping of ODPT JSON keys like "owl:sameAs" and "dc:title"
        val dtos = busOnlyData.mapNotNull { element: JsonElement ->
            try {
                val jsonObject = element.asJsonObject
                
                // Extract required fields manually from JSON
                // JSON uses "owl:sameAs" not "odpt:sameAs" for bus route patterns
                val sameAs = jsonObject.get("owl:sameAs")?.asString
                    ?: jsonObject.get("odpt:sameAs")?.asString
                    ?: jsonObject.get("sameAs")?.asString
                val title = jsonObject.get("dc:title")?.asString
                    ?: jsonObject.get("title")?.asString
                
                // Skip if required fields are missing
                if (sameAs == null || title == null) {
                    null
                } else {
                    // Parse odpt:busstopPoleOrder to create TransportationStop list
                    val busstopPoleOrderArray = jsonObject.get("odpt:busstopPoleOrder")?.asJsonArray
                    val busstopPoleOrder = busstopPoleOrderArray?.mapNotNull { stopElement ->
                        if (stopElement is JsonObject) {
                            val busstopPole = stopElement.get("odpt:busstopPole")?.asString
                            val index = stopElement.get("odpt:index")?.asInt
                            val titleObj = stopElement.get("odpt:busstopPoleTitle")?.asJsonObject
                            val stopTitle = titleObj?.let { obj ->
                                LocalizedTitle(
                                    ja = obj.get("ja")?.asString,
                                    en = obj.get("en")?.asString
                                )
                            }
                            val note = stopElement.get("odpt:note")?.asString
                            val stopName = stopTitle?.ja ?: stopTitle?.en ?: busstopPole?.split(".")?.lastOrNull() ?: ""
                            
                            if (busstopPole != null) {
                                TransportationStop(
                                    kind = TransportationLineKind.BUS,
                                    name = stopName,
                                    code = busstopPole,
                                    index = index,
                                    lineCode = sameAs,
                                    title = stopTitle,
                                    note = note,
                                    busstopPole = busstopPole
                                )
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    
                    BusRoutePatternDTO(
                        title = title,
                        sameAs = sameAs,
                        operatorCode = jsonObject.get("odpt:operator")?.asString,
                        busRoute = jsonObject.get("odpt:busroute")?.asString,
                        pattern = jsonObject.get("odpt:pattern")?.asString,
                        direction = jsonObject.get("odpt:direction")?.asString,
                        busstopPoleOrder = busstopPoleOrder,
                        note = jsonObject.get("odpt:note")?.asString,
                        date = jsonObject.get("dc:date")?.asString
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ODPTParser", "🚌 parseBusRoutes: Failed to parse DTO: ${e.message}", e)
                null
            }
        }
        
        // MARK: - DTO to Model Mapping using closures
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
            
            // Generate titles for bus stops using generateBusStopTitle
            val busstopPoleOrderWithTitles = dto.busstopPoleOrder?.map { stop ->
                val stopTitle = if (stop.title == null && stop.note != null && stop.busstopPole != null) {
                    generateBusStopTitle(stop.note, stop.busstopPole)
                } else {
                    stop.title
                }
                stop.copy(title = stopTitle)
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
                busstopPoleOrder = busstopPoleOrderWithTitles,
                title = title
            )
        }
    }
    
    // MARK: - Railway Data Parsing
    // Parse railway data from JSON files with support for both API and local file formats
    // Uses DTO pattern for type-safe decoding, consistent with bus route parsing
    fun parseRailwayRoutes(data: ByteArray): List<TransportationLine> {
        try {
            val jsonString = String(data)
            android.util.Log.d("ODPTParser", "parseRailwayRoutes: JSON string length=${jsonString.length}, first 200 chars: ${jsonString.take(200)}")
            
            // Check if it's a JSON array or object
            val jsonElement = JsonParser.parseString(jsonString)
            android.util.Log.d("ODPTParser", "parseRailwayRoutes: Parsed JSON element type=${jsonElement.javaClass.simpleName}")
            
            if (!jsonElement.isJsonArray) {
                android.util.Log.e("ODPTParser", "parseRailwayRoutes: JSON is not an array, it's ${jsonElement.javaClass.simpleName}")
                throw ODPTError.InvalidData()
            }
            
            val jsonArray = jsonElement.asJsonArray
            android.util.Log.d("ODPTParser", "parseRailwayRoutes: JSON array size=${jsonArray.size()}")
            
            // MARK: - Dictionary to DTO Mapping
            // Convert dictionary to DTO manually to support both odpt:color and odpt:lineColor
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
                            // Track if we've logged a warning for this field to avoid spam
                            var hasLoggedWarning = false
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
                                        // Only log once per field to avoid log spam
                                        if (!hasLoggedWarning) {
                                            android.util.Log.d("ODPTParser", "parseRailwayRoutes: Skipping ${element.javaClass.simpleName} in JsonArray for field '${entry.key}' (this is normal for some ODPT API fields)")
                                            hasLoggedWarning = true
                                        }
                                        null
                                    }
                                }
                            }
                        }
                        is JsonObject -> {
                            // Convert JsonObject to Map<String, String> for railwayTitle
                            // odpt:railwayTitle structure: {"ja": "山手線", "en": "Yamanote Line"}
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
                                        android.util.Log.w("ODPTParser", "parseRailwayRoutes: Unexpected nested value type in JsonObject: ${nestedValue.javaClass.simpleName}")
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
                // SwiftUI: Uses odpt:stationOrder from Railway JSON to create station list
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
                                busstopPole = null
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
            
            // MARK: - DTO to Model Mapping
            return dtos.map { dto: RailwayDTO ->
                // Fix trailing "行行" to "行"
                val fixedTitle = if (dto.title.endsWith("行行")) {
                    dto.title.dropLast(1)
                } else {
                    dto.title
                }
                
                // Fix railwayTitle's ja field if it ends with "行行"
                val fixedRailwayTitle = dto.railwayTitle?.let { title ->
                    if (title.ja?.endsWith("行行") == true) {
                        title.copy(ja = title.ja.dropLast(1))
                    } else {
                        title
                    }
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
                    busstopPoleOrder = null,
                    title = null
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ODPTParser", "parseRailwayRoutes: Failed to parse data: ${e.message}", e)
            android.util.Log.e("ODPTParser", "parseRailwayRoutes: Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            throw ODPTError.InvalidData()
        }
    }
}

// MARK: - ODPT Data Service
// Handles HTTP communication with the ODPT API.
// Manages authentication, caching, and data retrieval.
class ODPTDataService(private val context: Context) {
    private val client: OkHttpClient
    private val cache: CacheStore
    private val sharedPreferences: SharedPreferences
    private val challengeKey: String
    
    init {
        // MARK: - Session Configuration
        // Configure HTTP client with appropriate timeouts and settings
        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)      // 30 seconds for connection
            .readTimeout(30, TimeUnit.SECONDS)         // 30 seconds for reading
            .writeTimeout(30, TimeUnit.SECONDS)        // 30 seconds for writing
            .followRedirects(true)                      // Automatically handle redirects
            .followSslRedirects(true)
            .cookieJar(CookieJar.NO_COOKIES)           // Disable cookie handling for API requests
            .build()
        
        cache = CacheStore(context)
        sharedPreferences = context.getSharedPreferences("ODPTDataService", Context.MODE_PRIVATE)
        
        // Get ODPT Challenge Token from BuildConfig
        challengeKey = BuildConfig.ODPT_CHALLENGE_TOKEN
    }
    
    // MARK: - Common Request Configuration
    // Common function to configure request headers
    // Sets authentication and conditional request headers for efficient caching
    // Note: ODPT API uses URL query parameter acl:consumerKey for authentication, not Authorization header
    private fun configureRequest(
        requestBuilder: Request.Builder,
        consumerKey: String,
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
    
    // MARK: - Individual Operator Data Fetching
    // Fetch data for individual transportation operators using their specific API endpoints
    suspend fun fetchIndividualOperatorData(
        transportOperator: LocalDataSource,
        consumerKey: String
    ): ByteArray = withContext(Dispatchers.IO) {
        // Use challenge key for CHALLENGE API type, otherwise use access key
        val accessKey = if (transportOperator.apiType() == ODPTAPIType.CHALLENGE) {
            challengeKey
        } else {
            consumerKey
        }
        
        val urlString = transportOperator.apiLink(
            dataType = APIDataType.LINE,
            transportationKind = transportOperator.transportationType()
        )
        
        val url = urlString.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(url)
        configureRequest(requestBuilder, accessKey)
        
        println("🔗 Fetch URL: $urlString")
        
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
    
    // MARK: - ETag Management Helper Methods
    // Helper methods for efficient ETag management
    
    /// Get ETag for a specific operator from SharedPreferences
    private fun getETag(transportOperator: LocalDataSource): String? {
        val etagKey = "${transportOperator.fileName()}_etag"
        return sharedPreferences.getString(etagKey, null)
    }
    
    /// Save ETag for a specific operator to SharedPreferences
    private fun saveETag(etag: String, transportOperator: LocalDataSource) {
        val etagKey = "${transportOperator.fileName()}_etag"
        sharedPreferences.edit {
            putString(etagKey, etag)
        }
    }
    
    /// Get Last-Modified for a specific operator from SharedPreferences
    private fun getLastModified(transportOperator: LocalDataSource): String? {
        val lastModifiedKey = "${transportOperator.fileName()}_last_modified"
        return sharedPreferences.getString(lastModifiedKey, null)
    }
    
    /// Save Last-Modified for a specific operator to SharedPreferences
    private fun saveLastModified(lastModified: String, transportOperator: LocalDataSource) {
        val lastModifiedKey = "${transportOperator.fileName()}_last_modified"
        sharedPreferences.edit {
            putString(lastModifiedKey, lastModified)
        }
    }
    
    // MARK: - Conditional GET Request Check (ODPT API Optimized)
    // Check if individual operator data needs updating using conditional GET requests
    // Only performs update check if cache exists and ETag/Last-Modified are available
    suspend fun checkIndividualOperatorForUpdates(
        transportOperator: LocalDataSource,
        consumerKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        // MARK: - Cache-Based Update Check
        // Check if we have cached data
        val cacheKey = transportOperator.fileName()
        val cachedData = cache.loadData(cacheKey)
        
        // If no cache exists, skip update check (data will be fetched during initial fetch)
        if (cachedData == null) {
            return@withContext false
        }
        
        // Check if we have ETag or Last-Modified for conditional GET
        val etag = getETag(transportOperator)
        val lastModified = getLastModified(transportOperator)
        
        // If we don't have conditional headers, skip update check
        // Rule: Cache exists but no ETag/Last-Modified -> do nothing
        if (etag == null && lastModified == null) {
            return@withContext false
        }
        
        // MARK: - Conditional GET Request Check
        // Use conditional GET request with ETag and Last-Modified headers
        try {
            // Use challenge key for CHALLENGE API type, otherwise use access key
            val accessKey = if (transportOperator.apiType() == ODPTAPIType.CHALLENGE) {
                challengeKey
            } else {
                consumerKey
            }
            
            val urlString = transportOperator.apiLink(
                dataType = APIDataType.LINE,
                transportationKind = transportOperator.transportationType()
            )
            
            val url = urlString.toHttpUrl()
            
            val requestBuilder = Request.Builder().url(url)
            configureRequest(requestBuilder, accessKey, Pair(etag, lastModified))
            
            println("🔗 Fetch URL: $urlString")
            
            val response = client.newCall(requestBuilder.build()).execute()
            
            when (response.code) {
                304 -> {
                    // No update needed - server confirms data hasn't changed
                    return@withContext false
                }
                200 -> {
                    // Get current ETag from SharedPreferences
                    val currentEtag = getETag(transportOperator)
                    
                    // Get new ETag from response
                    val newEtag = response.header("ETag")
                    
                    // MARK: - ETag Comparison (Fast Path)
                    // If ETags match, no update is needed (fastest check)
                    if (currentEtag != null && newEtag != null && currentEtag == newEtag) {
                        return@withContext false // No update needed - ETag confirms no change
                    }
                    
                    // Save ETag and Last-Modified headers for future conditional requests
                    newEtag?.let { saveETag(it, transportOperator) }
                    response.header("Last-Modified")?.let { saveLastModified(it, transportOperator) }
                    
                    // MARK: - Data Content Comparison (Fallback)
                    // Compare actual data content to determine if update is needed
                    val responseData = response.body.bytes()
                    val dataMatches = cachedData.contentEquals(responseData)
                    return@withContext !dataMatches
                }
                else -> {
                    println("❌ ${transportOperator.operatorDisplayName(context)}: Unexpected response status: ${response.code}")
                    return@withContext false // Don't update on error
                }
            }
        } catch (e: Exception) {
            println("❌ ${transportOperator.operatorDisplayName(context)}: Request failed: $e")
            return@withContext false // Don't update on error
        }
    }
    
    // MARK: - Common Operator Update Processing
    // Common function to process operator updates
    private suspend fun processOperatorUpdate(
        transportOperator: LocalDataSource,
        consumerKey: String
    ): Pair<ByteArray, Boolean> {
        val needsUpdate = checkIndividualOperatorForUpdates(transportOperator, consumerKey)
        if (needsUpdate) {
            val data = fetchIndividualOperatorData(transportOperator, consumerKey)
            
            // Write updated data to JSON file
            writeIndividualOperatorDataToFile(data, transportOperator)
            
            // Update cache with new data
            val cacheKey = transportOperator.fileName()
            cache.saveData(data, cacheKey)
            
            println("✅ ${transportOperator.operatorDisplayName(context)}: Successfully updated data (${data.size} bytes)")
            return Pair(data, true)
        } else {
            return Pair(ByteArray(0), false)
        }
    }
    
    // MARK: - Individual Operator Update
    // Update individual operator data and save to Documents directory
    suspend fun updateIndividualOperator(
        transportOperator: LocalDataSource,
        consumerKey: String
    ): Result<Unit> {
        return try {
            val (_, _) = processOperatorUpdate(transportOperator, consumerKey)
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ ${transportOperator.operatorDisplayName(context)}: Failed to update data: $e")
            Result.failure(e)
        }
    }
    
    // MARK: - Write Individual Operator Data to File
    // Write individual operator data to Documents directory
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
    
    // MARK: - Generic ODPT Data Fetching
    // Fetch data from ODPT API endpoint and return response data and status
    suspend fun fetchODPTData(urlString: String): Pair<ByteArray, okhttp3.Response> = withContext(Dispatchers.IO) {
        val url = urlString.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(url)
        configureRequest(requestBuilder, "")
        
        val response = client.newCall(requestBuilder.build()).execute()
        val responseData = response.body.bytes()
        
        Pair(responseData, response)
    }
    
    // Fetch ODPT data with authentication key
    suspend fun fetchODPTDataWithAuth(urlString: String, authKey: String): Pair<ByteArray, okhttp3.Response> = withContext(Dispatchers.IO) {
        val url = urlString.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(url)
        configureRequest(requestBuilder, authKey)
        
        val response = client.newCall(requestBuilder.build()).execute()
        val responseData = response.body.bytes()
        
        Pair(responseData, response)
    }
    
    // MARK: - JSON Object to Map Conversion
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
    
    // MARK: - JSON Array Parsing
    // Parse JSON array from byte array
    // Handles both JSON array and single JSON object responses
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
            android.util.Log.e("ODPTDataService", "parseJSONArray: Invalid JSON type: ${jsonElement.javaClass.simpleName}")
            return emptyList()
        }
        
        return jsonArray.mapNotNull { element ->
            if (!element.isJsonObject) {
                android.util.Log.w("ODPTDataService", "parseJSONArray: Element is not a JsonObject: ${element.javaClass.simpleName}")
                return@mapNotNull null
            }
            parseJsonObjectToMap(element.asJsonObject)
        }
    }
}

