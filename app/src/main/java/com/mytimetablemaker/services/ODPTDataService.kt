package com.mytimetablemaker.services

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
    val date: String? = null
)

// MARK: - ODPT Bus Route Pattern DTO
// DTO for bus route pattern data from ODPT API.
// Maps external JSON structure to internal bus data model.
data class BusRoutePatternDTO(
    val title: String,
    val sameAs: String,
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
        
        // Convert filtered data to DTOs
        val gson = Gson()
        val dtos = busOnlyData.mapNotNull { element: JsonElement ->
            try {
                gson.fromJson(element, BusRoutePatternDTO::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        // MARK: - DTO to Model Mapping using closures
        return dtos.map { dto: BusRoutePatternDTO ->
            // Extract English name from odpt:busroute value using LineExtensions
            val englishName = dto.busRoute?.busRouteEnglishName()
            
            TransportationLine(
                id = dto.sameAs,
                kind = TransportationLineKind.BUS,
                name = dto.title,
                code = dto.sameAs,
                operatorCode = dto.operatorCode,
                lineColor = null,
                startStation = null,
                endStation = null,
                destinationStation = null,
                railwayTitle = LocalizedTitle(ja = dto.title, en = englishName),
                lineCode = null,
                lineDirection = null,
                ascendingRailDirection = null,
                descendingRailDirection = null,
                busRoute = dto.busRoute,
                pattern = dto.pattern,
                busDirection = dto.direction,
                busstopPoleOrder = dto.busstopPoleOrder,
                title = dto.title
            )
        }
    }
    
    // MARK: - Railway Data Parsing
    // Parse railway data from JSON files with support for both API and local file formats
    // Uses DTO pattern for type-safe decoding, consistent with bus route parsing
    fun parseRailwayRoutes(data: ByteArray): List<TransportationLine> {
        try {
            val jsonString = String(data)
            val jsonArray = JsonParser.parseString(jsonString).asJsonArray
            
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
                        is JsonArray -> value.mapNotNull { it.asString }
                        is JsonObject -> {
                            // Convert JsonObject to Map<String, String> for railwayTitle
                            value.entrySet().associate { e ->
                                e.key to (e.value.asString ?: "")
                            }
                        }
                        else -> null
                    }
                }.filterValues { it != null }.let {
                    @Suppress("UNCHECKED_CAST")
                    it as Map<String, Any>
                }
                
                // Extract railwayTitle directly from JsonObject for proper type handling
                val railwayTitle = jsonObject.get("odpt:railwayTitle")?.asJsonObject?.let { titleObj ->
                    LocalizedTitle(
                        ja = titleObj.get("ja")?.asString,
                        en = titleObj.get("en")?.asString
                    )
                }
                
                // Use LineExtensions utilities for ODPT data extraction
                val lineColor = elementMap.odptLineColor()
                val destinationStation = elementMap.odptDestinationStation()
                
                // Extract rail direction information for timetable API calls
                val ascendingRailDirection = jsonObject.get("odpt:ascendingRailDirection")?.asString
                val descendingRailDirection = jsonObject.get("odpt:descendingRailDirection")?.asString
                
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
                    date = jsonObject.get("dc:date")?.asString
                )
            }
            
            // MARK: - DTO to Model Mapping
            return dtos.map { dto: RailwayDTO ->
                TransportationLine(
                    id = dto.sameAs,
                    kind = TransportationLineKind.RAILWAY,
                    name = dto.title,
                    code = dto.sameAs,
                    operatorCode = dto.operatorCode,
                    lineColor = dto.lineColor,
                    startStation = dto.startStation,
                    endStation = dto.endStation,
                    destinationStation = dto.destinationStation,
                    railwayTitle = dto.railwayTitle,
                    lineCode = dto.lineCode,
                    lineDirection = dto.ascendingRailDirection, // Keep for backward compatibility
                    ascendingRailDirection = dto.ascendingRailDirection,
                    descendingRailDirection = dto.descendingRailDirection,
                    busRoute = null,
                    pattern = null,
                    busDirection = null,
                    busstopPoleOrder = null,
                    title = null
                )
            }
        } catch (e: Exception) {
            throw ODPTError.InvalidData
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
    }
    
    // MARK: - Common Request Configuration
    // Common function to configure request headers
    // Sets authentication and conditional request headers for efficient caching
    private fun configureRequest(
        requestBuilder: Request.Builder,
        consumerKey: String,
        conditionalHeaders: Pair<String?, String?>? = null
    ): Request.Builder {
        requestBuilder.addHeader("Authorization", consumerKey)
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
        val urlString = transportOperator.apiLink(
            dataType = APIDataType.LINE,
            transportationKind = transportOperator.transportationType(),
            odptAccessKey = consumerKey
        )
        
        val url = urlString.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(url)
        configureRequest(requestBuilder, consumerKey)
        
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
            
            response.body?.bytes() ?: throw ODPTError.InvalidData
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
        sharedPreferences.edit().putString(etagKey, etag).apply()
    }
    
    /// Get Last-Modified for a specific operator from SharedPreferences
    private fun getLastModified(transportOperator: LocalDataSource): String? {
        val lastModifiedKey = "${transportOperator.fileName()}_last_modified"
        return sharedPreferences.getString(lastModifiedKey, null)
    }
    
    /// Save Last-Modified for a specific operator to SharedPreferences
    private fun saveLastModified(lastModified: String, transportOperator: LocalDataSource) {
        val lastModifiedKey = "${transportOperator.fileName()}_last_modified"
        sharedPreferences.edit().putString(lastModifiedKey, lastModified).apply()
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
            val urlString = transportOperator.apiLink(
                dataType = APIDataType.LINE,
                transportationKind = transportOperator.transportationType(),
                odptAccessKey = consumerKey
            )
            
            val url = urlString.toHttpUrl()
            
            val requestBuilder = Request.Builder().url(url)
            configureRequest(requestBuilder, consumerKey, Pair(etag, lastModified))
            
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
                    val responseData = response.body?.bytes()
                    val dataMatches = cachedData.contentEquals(responseData ?: ByteArray(0))
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
        val responseData = response.body?.bytes() ?: ByteArray(0)
        
        Pair(responseData, response)
    }
    
    // MARK: - JSON Array Parsing
    // Parse JSON array from byte array
    fun parseJSONArray(data: ByteArray): List<Map<*, *>> {
        val jsonString = String(data)
        val jsonArray = JsonParser.parseString(jsonString).asJsonArray
        
        return jsonArray.mapNotNull { element ->
            val jsonObject = element.asJsonObject
            jsonObject.entrySet().associate { entry ->
                entry.key to when (val value = entry.value) {
                    is com.google.gson.JsonPrimitive -> {
                        when {
                            value.isString -> value.asString
                            value.isNumber -> value.asNumber
                            value.isBoolean -> value.asBoolean
                            else -> null
                        }
                    }
                    is JsonArray -> value.mapNotNull { it.asString }
                    is JsonObject -> {
                        value.entrySet().associate { e ->
                            e.key to (e.value.asString ?: "")
                        }
                    }
                    else -> null
                }
            }
        }
    }
}

