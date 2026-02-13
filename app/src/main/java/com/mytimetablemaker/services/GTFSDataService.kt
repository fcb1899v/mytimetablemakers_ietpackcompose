package com.mytimetablemaker.services

import android.content.Context
import android.content.SharedPreferences
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.timeToMinutes
import com.mytimetablemaker.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import androidx.core.content.edit

// GTFS data processing, parsing, and timetable generation.
class GTFSDataService(private val context: Context) {
    private val cache: CacheStore = CacheStore(context)

    // In-memory cache for loaded GTFS files (dir + filename).
    private val fileCache: MutableMap<String, ByteArray> = mutableMapOf()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("GTFSDataService", Context.MODE_PRIVATE)

    // Load translations.txt for quick lookup by table/field/record.
    // Uses current language with English fallback.
    private fun loadTranslations(extractedDir: File): Map<String, String> {
        val translationsData = try {
            loadGTFSFile(extractedDir, "translations.txt")
        } catch (_: Exception) {
            return emptyMap()
        }
        
        val translations = try {
            parseGTFSCSV(translationsData)
        } catch (_: Exception) {
            return emptyMap()
        }
        
        val translationDict = mutableMapOf<String, String>()
        val currentLanguage = Locale.getDefault().language
        
        // Skip translations for Japanese.
        if (currentLanguage == "ja") {
            return emptyMap()
        }
        
        // Use current language, then fallback to English.
        val targetLanguages = if (currentLanguage == "en") listOf("en") else listOf(currentLanguage, "en")
        
        for (row in translations) {
            val tableName = row["table_name"] ?: continue
            val fieldName = row["field_name"] ?: continue
            val language = row["language"] ?: continue
            val translation = row["translation"] ?: continue
            
            if (!targetLanguages.contains(language)) continue
            
            // Build keys for record_id and field_value when present.
            val recordId = row["record_id"]
            if (!recordId.isNullOrEmpty()) {
                val key = "$tableName|$fieldName|$recordId"
                // Prefer current language, allow fallback.
                if (language == currentLanguage || !translationDict.containsKey(key)) {
                    translationDict[key] = translation
                }
            }
            val fieldValue = row["field_value"]
            if (!fieldValue.isNullOrEmpty()) {
                val key = "$tableName|$fieldName|$fieldValue"
                // Prefer current language, allow fallback.
                if (language == currentLanguage || !translationDict.containsKey(key)) {
                    translationDict[key] = translation
                }
            }
        }
        
        return translationDict
    }
    
    // Convert fullwidth numbers/letters to halfwidth.
    private fun convertFullwidthToHalfwidth(text: String): String {
        // Map fullwidth digits and letters to ASCII.
        var result = text
        val fullwidthNumbers = "０１２３４５６７８９"
        val halfwidthNumbers = "0123456789"
        val fullwidthUpper = "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ"
        val halfwidthUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val fullwidthLower = "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ"
        val halfwidthLower = "abcdefghijklmnopqrstuvwxyz"
        
        // Replace fullwidth numbers.
        for (i in fullwidthNumbers.indices) {
            result = result.replace(fullwidthNumbers[i], halfwidthNumbers[i])
        }
        
        // Replace fullwidth uppercase letters.
        for (i in fullwidthUpper.indices) {
            result = result.replace(fullwidthUpper[i], halfwidthUpper[i])
        }
        
        // Replace fullwidth lowercase letters.
        for (i in fullwidthLower.indices) {
            result = result.replace(fullwidthLower[i], halfwidthLower[i])
        }
        
        return result
    }
    
    // Resolve localized text with translations and halfwidth conversion.
    private fun getLocalizedText(
        original: String,
        tableName: String,
        fieldName: String,
        recordId: String?,
        translations: Map<String, String>
    ): String {
        val currentLanguage = Locale.getDefault().language
        
        val result: String
        
        // Return original for Japanese.
        if (currentLanguage == "ja") {
            result = original
        } else {
            // Try to get translation.
            if (!recordId.isNullOrEmpty()) {
                val key = "$tableName|$fieldName|$recordId"
                result = if (translations.containsKey(key) && translations[key]!!.isNotEmpty()) {
                    translations[key]!!
                } else {
                    // Try field_value as fallback.
                    val key2 = "$tableName|$fieldName|$original"
                    if (translations.containsKey(key2) && translations[key2]!!.isNotEmpty()) translations[key2]!! else original
                }
            } else {
                // Try field_value as fallback.
                val key = "$tableName|$fieldName|$original"
                result = if (translations.containsKey(key) && translations[key]!!.isNotEmpty()) translations[key]!! else original
            }
        }
        
        // Convert fullwidth numbers and letters to halfwidth.
        return convertFullwidthToHalfwidth(result)
    }
    
    // Load GTFS file from extracted directory with in-memory cache.
    private fun loadGTFSFile(directory: File, filename: String): ByteArray {
        val cacheKey = "${directory.absolutePath}/$filename"
        
        // Check cache first.
        if (fileCache.containsKey(cacheKey)) {
            return fileCache[cacheKey]!!
        }
        
        // Load from disk.
        val file = File(directory, filename)
        val data = file.readBytes()
        
        // Cache for future use.
        fileCache[cacheKey] = data
        
        return data
    }
    
    // Parse a GTFS CSV file into rows.
    fun parseGTFSCSV(data: ByteArray): List<Map<String, String>> {
        val content = String(data, Charsets.UTF_8)
        
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        if (lines.isEmpty()) {
            return emptyList()
        }
        
        // Parse header.
        val header = parseCSVLine(lines[0])
        if (header.isEmpty()) {
            throw ODPTError.InvalidData("CSV header is empty")
        }
        
        // Parse data rows.
        val results = mutableListOf<Map<String, String>>()
        for (i in 1 until lines.size) {
            val values = parseCSVLine(lines[i])
            if (values.size != header.size) {
                continue // Skip malformed rows.
            }
            
            val row = mutableMapOf<String, String>()
            for (j in header.indices) {
                row[header[j]] = if (j < values.size) values[j] else ""
            }
            results.add(row)
        }
        
        return results
    }
    
    // Stream-parse a large GTFS CSV to avoid OOM.
    private fun parseGTFSCSVStreaming(file: File, onRow: (Map<String, String>) -> Unit) {
        if (!file.exists() || !file.isFile) {
            return
        }
        
        file.bufferedReader(Charsets.UTF_8).use { reader ->
            // Read header.
            val headerLine = reader.readLine() ?: return
            val header = parseCSVLine(headerLine.trim())
            if (header.isEmpty()) {
                throw ODPTError.InvalidData("CSV header is empty (streaming)")
            }
            
            // Process rows line by line.
            reader.lineSequence().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) {
                    return@forEach
                }
                
                val values = parseCSVLine(trimmedLine)
                if (values.size != header.size) {
                    return@forEach // Skip malformed rows.
                }
                
                val row = mutableMapOf<String, String>()
                for (j in header.indices) {
                    row[header[j]] = if (j < values.size) values[j] else ""
                }
                onRow(row)
            }
        }
    }
    
    // Parse a single CSV line with quotes.
    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when (char) {
                '"' -> inQuotes = !inQuotes
                ',' if !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        
        return result
    }
    
    // Download GTFS ZIP and cache it for startup use.
    suspend fun downloadGTFSZipOnly(url: String, consumerKey: String, transportOperator: LocalDataSource): ByteArray {
        return downloadGTFSZip(url, consumerKey, transportOperator)
    }
    
    // Download GTFS ZIP with cache and conditional updates.
    // Date-based operators use date in cache keys.
    private suspend fun downloadGTFSZip(url: String, consumerKey: String, transportOperator: LocalDataSource? = null): ByteArray = withContext(Dispatchers.IO) {
        // transportOperator must be provided for GTFS.
        val transportOp = transportOperator ?: throw ODPTError.InvalidData("Missing transport operator")
        
        // Build cache key; date-based operators embed the date.
        val date = GTFSDates.dateFor(transportOp) ?: ""
        val gtfsFileName = transportOp.gtfsFileName()
        val cacheKey = if (date.isEmpty()) "gtfs_$gtfsFileName.zip" else "gtfs_${gtfsFileName}_$date.zip"
        
        // Check for cached ZIP.
        val cachedData = cache.loadData(cacheKey)
        if (cachedData != null) {
            // Check for cached extraction.
            val extractedCacheDirName = cacheKey.replace(".zip", "_extracted")
            if (!cache.directoryExists(extractedCacheDirName)) {
                // ZIP cached but not extracted; extract now.
                extractAndCacheGTFSZip(cachedData, cacheKey)
            }
            
            // Toei Bus uses conditional GET; date-based operators are stable.
            if (transportOp == LocalDataSource.TOEI_BUS) {
                // Toei Bus: conditional GET for updates.
                val updatedData = checkForToeiBusGTFSUpdate(url, cacheKey)
                if (updatedData != null) {
                    // File updated: return new data.
                    return@withContext updatedData
                } else {
                    // File not modified: return cached data.
                    return@withContext cachedData
                }
            } else {
                // Date-based operator: cached file is current.
                return@withContext cachedData
            }
        }
        
        // No cache exists; download a new file.
        downloadNewGTFSZip(url, consumerKey, transportOp, cacheKey)
    }
    
    // Check for Toei Bus GTFS updates via conditional GET.
    private suspend fun checkForToeiBusGTFSUpdate(urlString: String, cacheKey: String): ByteArray? = withContext(Dispatchers.IO) {
        val url = urlString.toHttpUrl()
        
        // Get ETag and Last-Modified from SharedPreferences.
        val etagKey = "${cacheKey}_etag"
        val lastModifiedKey = "${cacheKey}_last_modified"
        val etag = sharedPreferences.getString(etagKey, null)
        val lastModified = sharedPreferences.getString(lastModifiedKey, null)
        
        // If no ETag/Last-Modified, download a new file.
        if (etag == null && lastModified == null) {
            return@withContext downloadNewGTFSZip(urlString, "", LocalDataSource.TOEI_BUS, cacheKey)
        }
        
        val requestBuilder = Request.Builder().url(url)
        // Toei Bus uses public API with no token.
        requestBuilder.addHeader("Accept", "application/zip")
        
        // Add conditional headers.
        etag?.let { requestBuilder.addHeader("If-None-Match", it) }
        lastModified?.let { requestBuilder.addHeader("If-Modified-Since", it) }
        
        val client = OkHttpClient()
        val response = client.newCall(requestBuilder.build()).execute()
        
        // Handle 304 Not Modified.
        if (response.code == 304) {
            return@withContext null // File not modified, use cached data
        }
        
        // Handle 200 OK (file updated).
        if (response.code != 200) {
            throw ODPTError.NetworkError("HTTP ${response.code}")
        }
        
        val data = response.body.bytes()
        
        // Save ETag and Last-Modified for future checks.
        response.header("ETag")?.let { newEtag ->
            sharedPreferences.edit { putString(etagKey, newEtag) }
        }
        response.header("Last-Modified")?.let { newLastModified ->
            sharedPreferences.edit { putString(lastModifiedKey, newLastModified) }
        }
        
        // Save new data to cache.
        cache.saveData(data, cacheKey)
        // Extract and cache the directory.
        extractAndCacheGTFSZip(data, cacheKey)
        
        data
    }
    
    // Download a new GTFS ZIP from the server.
    private suspend fun downloadNewGTFSZip(url: String, consumerKey: String, transportOperator: LocalDataSource, cacheKey: String): ByteArray = withContext(Dispatchers.IO) {
        val httpUrl = url.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(httpUrl)
        // Toei Bus uses public API with no token.
        if (transportOperator != LocalDataSource.TOEI_BUS) {
            requestBuilder.addHeader("Authorization", consumerKey)
        }
        requestBuilder.addHeader("Accept", "application/zip")
        
        val client = OkHttpClient()
        val response = client.newCall(requestBuilder.build()).execute()
        
        // Handle redirects.
        if (response.code == 301 || response.code == 302) {
            val location = response.header("Location")
            if (location != null) {
                val redirectUrl = location.toHttpUrl()
                val redirectRequest = Request.Builder().url(redirectUrl).build()
                val redirectResponse = client.newCall(redirectRequest).execute()
                
                if (redirectResponse.code != 200) {
                    throw ODPTError.NetworkError("Failed to download from redirect URL")
                }
                
                val redirectData = redirectResponse.body.bytes()
                
                // Save ETag and Last-Modified for future checks.
                val etagKey = "${cacheKey}_etag"
                val lastModifiedKey = "${cacheKey}_last_modified"
                redirectResponse.header("ETag")?.let { etag ->
                    sharedPreferences.edit { putString(etagKey, etag) }
                }
                redirectResponse.header("Last-Modified")?.let { lastModified ->
                    sharedPreferences.edit { putString(lastModifiedKey, lastModified) }
                }
                
                // Save to cache.
                cache.saveData(redirectData, cacheKey)
                // Extract and cache the directory.
                extractAndCacheGTFSZip(redirectData, cacheKey)
                
                return@withContext redirectData
            }
        }
        
        if (response.code != 200) {
            throw ODPTError.NetworkError("HTTP ${response.code}")
        }
        
        val data = response.body.bytes()
        
        // Save ETag and Last-Modified for future checks.
        val etagKey = "${cacheKey}_etag"
        val lastModifiedKey = "${cacheKey}_last_modified"
        response.header("ETag")?.let { etag ->
            sharedPreferences.edit { putString(etagKey, etag) }
        }
        response.header("Last-Modified")?.let { lastModified ->
            sharedPreferences.edit { putString(lastModifiedKey, lastModified) }
        }
        
        // Save to cache.
        cache.saveData(data, cacheKey)
        // Extract and cache the directory.
        extractAndCacheGTFSZip(data, cacheKey)
        
        data
    }
    
    // Extract GTFS ZIP and cache the extracted directory.
    private suspend fun extractAndCacheGTFSZip(data: ByteArray, cacheKey: String) = withContext(Dispatchers.IO) {
        // Generate cache directory name from cache key.
        val extractedCacheDirName = cacheKey.replace(".zip", "_extracted")
        
        // Check if extracted directory is cached.
        if (cache.directoryExists(extractedCacheDirName)) {
            return@withContext
        }
        
        // Extract to a temporary directory first.
        val tempDir = File(context.cacheDir, "gtfs_extract_temp_${UUID.randomUUID()}")
        val extractedDir = extractGTFSZipToTemp(data, tempDir)
        
        // Copy extracted directory to cache.
        cache.saveDirectory(extractedDir, extractedCacheDirName)
        
        // Clean up temporary directory.
        tempDir.deleteRecursively()
    }
    
    // Extract GTFS ZIP into a temporary directory.
    private fun extractGTFSZipToTemp(data: ByteArray, directory: File): File {
        val extractedDir = File(directory, "extracted")
        
        // Create directory if needed.
        directory.mkdirs()
        extractedDir.mkdirs()
        
        // Write ZIP data to file.
        val zipFile = File(directory, "gtfs.zip")
        zipFile.writeBytes(data)
        
        // Extract using ZipInputStream.
        ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val file = File(extractedDir, entry.name)
                    // Create parent directories if needed.
                    file.parentFile?.mkdirs()
                    
                    // Write file.
                    file.outputStream().use { output ->
                        zipInputStream.copyTo(output)
                    }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        }
        
        // Remove temporary ZIP file.
        zipFile.delete()
        
        return extractedDir
    }
    
    // Get extracted GTFS directory from cache or download.
    // Uses cached directory directly when available.
    private suspend fun getExtractedGTFSDirectory(transportOperator: LocalDataSource, consumerKey: String, gtfsURL: String): File = withContext(Dispatchers.IO) {
        val date = GTFSDates.dateFor(transportOperator) ?: ""
        val gtfsFileName = transportOperator.gtfsFileName()
        val cacheKey = if (date.isEmpty()) "gtfs_$gtfsFileName.zip" else "gtfs_${gtfsFileName}_$date.zip"
        val extractedCacheDirName = cacheKey.replace(".zip", "_extracted")
        
        // Check if extracted directory is cached.
        val cachedExtractedDir = cache.loadDirectoryPath(extractedCacheDirName)
        if (cachedExtractedDir != null) {
            // Verify routes.txt exists.
            val routesFile = File(cachedExtractedDir, "routes.txt")
            if (routesFile.exists()) {
                // Use cached directory directly.
                return@withContext cachedExtractedDir
            }
        }
        
        // Cache missing or invalid; download and extract.
        downloadGTFSZip(gtfsURL, consumerKey, transportOperator)
        
        // After download, the directory should be cached.
        val cachedDir = cache.loadDirectoryPath(extractedCacheDirName)
            ?: throw ODPTError.NetworkError("Failed to cache extracted GTFS directory")
        
        // Verify routes.txt exists.
        val routesFile = File(cachedDir, "routes.txt")
        if (!routesFile.exists()) {
            throw ODPTError.NetworkError("Extracted GTFS directory is invalid")
        }
        
        cachedDir
    }
    
    // Download and process GTFS data for GTFS operators.
    // Returns TransportationLine models directly.
    suspend fun fetchGTFSData(transportOperator: LocalDataSource, consumerKey: String): List<TransportationLine> = withContext(Dispatchers.IO) {
        try {
            // Get GTFS URL using apiLink.
            val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
            if (gtfsURL.isEmpty()) {
                throw ODPTError.InvalidData("Empty GTFS URL for ${transportOperator.name}")
            }
            
            // Get extracted directory (uses cache when available).
            val extractedDir = getExtractedGTFSDirectory(transportOperator, consumerKey, gtfsURL)
            
            // Load translations for localization.
            val translations = loadTranslations(extractedDir)
            
            // Parse routes.txt for route info.
            val routesData = loadGTFSFile(extractedDir, "routes.txt")
            val routes = parseGTFSCSV(routesData)
            
            // Parse trips.txt for headsign and direction info.
            val tripsData = loadGTFSFile(extractedDir, "trips.txt")
            val trips = parseGTFSCSV(tripsData)
        
        // Group trips by route_id and unique direction info.
        data class DirectionInfo(
            val headsign: String?,
            val directionId: Int?,
            val firstStopId: String?,  // First stop_id from stop_times.txt (for routes without headsign/direction_id)
            val lastStopId: String?    // Last stop_id from stop_times.txt (for routes without headsign/direction_id)
        )
        
        // Use stop_times.txt endpoints when headsign/direction_id are missing.
        val tripEndpoints = mutableMapOf<String, Pair<String, String>>()
        val stopsDict = mutableMapOf<String, String>()  // stop_id -> stop_name mapping
        
        // Load stops.txt to get stop names.
        val stopsData = try {
            loadGTFSFile(extractedDir, "stops.txt")
        } catch (_: Exception) {
            null
        }
        if (stopsData != null) {
            val stopsRows = parseGTFSCSV(stopsData)
            for (row in stopsRows) {
                val stopId = row["stop_id"]
                val stopName = row["stop_name"]
                if (stopId != null && stopName != null) {
                    // Apply translation to stop name.
                    val localizedStopName = getLocalizedText(
                        original = stopName,
                        tableName = "stops",
                        fieldName = "stop_name",
                        recordId = stopId,
                        translations = translations
                    )
                    stopsDict[stopId] = localizedStopName
                }
            }
        }
        
        // Stream stop_times.txt to avoid OOM.
        val stopTimesFile = File(extractedDir, "stop_times.txt")
        if (stopTimesFile.exists() && stopTimesFile.isFile) {
            val tripStopSequences = mutableMapOf<String, MutableList<Pair<String, Int>>>()
            
            // Process stop_times.txt line by line.
            parseGTFSCSVStreaming(stopTimesFile) { row ->
                val tripId = row["trip_id"] ?: return@parseGTFSCSVStreaming
                val stopId = row["stop_id"] ?: return@parseGTFSCSVStreaming
                val sequenceStr = row["stop_sequence"] ?: return@parseGTFSCSVStreaming
                val sequence = sequenceStr.toIntOrNull() ?: return@parseGTFSCSVStreaming
                
                if (!tripStopSequences.containsKey(tripId)) {
                    tripStopSequences[tripId] = mutableListOf()
                }
                tripStopSequences[tripId]!!.add(Pair(stopId, sequence))
            }
            
            // Get first and last stop_id for each trip.
            for ((tripId, stops) in tripStopSequences) {
                val sortedStops = stops.sortedBy { it.second }
                if (sortedStops.isNotEmpty()) {
                    val first = sortedStops.first()
                    val last = sortedStops.last()
                    tripEndpoints[tripId] = Pair(first.first, last.first)
                }
            }
        }
        
        val routeDirections = mutableMapOf<String, MutableSet<DirectionInfo>>()
        for (trip in trips) {
            val routeId = trip["route_id"] ?: continue
            val tripId = trip["trip_id"] ?: continue
            var headsign = trip["trip_headsign"]?.trim()
            // Apply translation to trip_headsign.
            if (!headsign.isNullOrEmpty()) {
                headsign = getLocalizedText(
                    original = headsign,
                    tableName = "trips",
                    fieldName = "trip_headsign",
                    recordId = tripId,
                    translations = translations
                )
            }
            val directionId = trip["direction_id"]?.toIntOrNull()
            
            // Get endpoints for routes without headsign/direction_id.
            val endpoints = tripEndpoints[tripId]
            val directionInfo = DirectionInfo(
                headsign = if (headsign.isNullOrEmpty()) null else headsign,
                directionId = directionId,
                firstStopId = endpoints?.first,
                lastStopId = endpoints?.second
            )
            
            if (!routeDirections.containsKey(routeId)) {
                routeDirections[routeId] = mutableSetOf()
            }
            routeDirections[routeId]!!.add(directionInfo)
        }
        
        // Create TransportationLine per route and direction.
        val transportationLines = mutableListOf<TransportationLine>()
        var skippedRoutes = 0
        for (route in routes) {
            val routeId = route["route_id"] ?: run {
                skippedRoutes++
                continue
            }
            
            // Get direction info for this route.
            val directions = routeDirections[routeId] ?: emptySet()
            
            if (directions.isEmpty()) {
                // No trip info: create a single line.
                val line = createTransportationLine(
                    route = route,
                    routeId = routeId,
                    tripHeadsign = null,
                    directionId = null,
                    directionCode = null,
                    operatorCode = transportOperator.operatorCode(),
                    translations = translations
                )
                if (line != null) {
                    transportationLines.add(line)
                }
            } else if (directions.size == 1) {
                // One direction: create a single line.
                val direction = directions.first()
                // Use last stop name as headsign when missing.
                val displayHeadsign: String? = when {
                    !direction.headsign.isNullOrEmpty() -> direction.headsign
                    direction.lastStopId != null && stopsDict.containsKey(direction.lastStopId) -> stopsDict[direction.lastStopId]
                    else -> null
                }
                
                val directionCode: String? = when {
                    direction.directionId != null -> direction.directionId.toString()
                    direction.firstStopId != null && direction.lastStopId != null -> "${direction.firstStopId}|${direction.lastStopId}"
                    else -> null
                }
                
                val line = createTransportationLine(
                    route = route,
                    routeId = routeId,
                    tripHeadsign = displayHeadsign,
                    directionId = direction.directionId,
                    directionCode = directionCode,
                    operatorCode = transportOperator.operatorCode(),
                    translations = translations
                )
                if (line != null) {
                    transportationLines.add(line)
                }
            } else {
                // Multiple directions: create a line per direction.
                val sortedDirections = directions.sortedWith(compareBy(
                    { it.directionId ?: Int.MAX_VALUE },
                    { it.headsign ?: "" },
                    { it.firstStopId ?: "" },
                    { it.lastStopId ?: "" }
                ))
                
                for (direction in sortedDirections) {
                    // Use last stop name as headsign when missing.
                    val displayHeadsign: String? = when {
                        !direction.headsign.isNullOrEmpty() -> direction.headsign
                        direction.lastStopId != null && stopsDict.containsKey(direction.lastStopId) -> stopsDict[direction.lastStopId]
                        else -> null
                    }
                    
                    // Create unique code from directionId or endpoints.
                    val directionCode: String = when {
                        direction.directionId != null -> direction.directionId.toString()
                        direction.firstStopId != null && direction.lastStopId != null -> "${direction.firstStopId}|${direction.lastStopId}"
                        else -> ""
                    }
                    
                    val line = createTransportationLine(
                        route = route,
                        routeId = routeId,
                        tripHeadsign = displayHeadsign,
                        directionId = direction.directionId,
                        directionCode = directionCode.ifEmpty { null },
                        operatorCode = transportOperator.operatorCode(),
                        translations = translations
                    )
                    if (line != null) {
                        transportationLines.add(line)
                    }
                }
            }
        }
        
        transportationLines
        } catch (e: Exception) {
            android.util.Log.e("GTFSDataService", "fetchGTFSData failed for ${transportOperator.name}", e)
            throw e
        }
    }
    
    // Create TransportationLine from a GTFS route row.
    // directionCode is used when directionId is null.
    private fun createTransportationLine(
        route: Map<String, String>,
        routeId: String,
        tripHeadsign: String?,
        directionId: Int?,
        directionCode: String?,
        operatorCode: String?,
        translations: Map<String, String> = emptyMap()
    ): TransportationLine? {
        // Prefer route_short_name, fallback to route_long_name.
        var routeShortName = route["route_short_name"]?.trim()
        var routeLongName = route["route_long_name"]?.trim()
        
        // Apply translations to route names.
        if (!routeShortName.isNullOrEmpty()) {
            routeShortName = getLocalizedText(
                original = routeShortName,
                tableName = "routes",
                fieldName = "route_short_name",
                recordId = routeId,
                translations = translations
            )
        }
        if (!routeLongName.isNullOrEmpty()) {
            routeLongName = getLocalizedText(
                original = routeLongName,
                tableName = "routes",
                fieldName = "route_long_name",
                recordId = routeId,
                translations = translations
            )
        }
        
        val shortName = routeShortName?.takeIf { it.isNotEmpty() } ?: routeLongName?.takeIf { it.isNotEmpty() }
        if (shortName == null) {
            return null // Skip routes without a name.
        }
        
        // Use trip_headsign as destination, else parse route_long_name.
        var destinationStop: String? = null
        var departureStop: String? = null
        
        if (!tripHeadsign.isNullOrEmpty()) {
            // Use trip_headsign as destination.
            destinationStop = tripHeadsign
        } else if (routeLongName != null && routeLongName.contains("〜")) {
            // Extract destination from route_long_name.
            val components = routeLongName.split("〜")
            if (components.size >= 2) {
                // First component is departure stop.
                departureStop = components.first().trim()
                // Last component is destination stop.
                val rawDestination = components.last().trim()
                
                // Clean destination by removing parentheses and extra info.
                destinationStop = when {
                    rawDestination.contains("（") -> {
                        rawDestination.substringBefore("（").trim()
                    }
                    rawDestination.contains("(") -> {
                        rawDestination.substringBefore("(").trim()
                    }
                    rawDestination.endsWith("）") -> {
                        rawDestination.dropLast(1).trim()
                    }
                    rawDestination.endsWith(")") -> {
                        rawDestination.dropLast(1).trim()
                    }
                    else -> rawDestination
                }
                if (destinationStop.isEmpty()) {
                    destinationStop = null
                }
            }
        }
        
        // Build route name with destination when available.
        val destinationPrefix = context.getString(R.string.routeDestinationPrefix)
        val destinationSuffix = context.getString(R.string.routeDestinationSuffix)
        val routeName: String = when {
            !tripHeadsign.isNullOrEmpty() -> {
                // Use trip_headsign as destination.
                val destination = "${tripHeadsign}$destinationSuffix".replace("行 行", "行").replace("行行", "行")
                "$shortName$destinationPrefix$destination"
            }
            routeShortName?.isNotEmpty() == true && destinationStop?.isNotEmpty() == true -> {
                val destination = "${destinationStop}$destinationSuffix".replace("行 行", "行").replace("行行", "行")
                "$shortName$destinationPrefix$destination"
            }
            else -> shortName
        }
        
        // Convert fullwidth numbers and letters to halfwidth.
        val cleanedRouteName = convertFullwidthToHalfwidth(routeName)
        
        // Fix trailing "行行" to "行".
        val finalRouteName = if (cleanedRouteName.endsWith("行行")) cleanedRouteName.dropLast(1) else cleanedRouteName
        
        // Create a unique code from route_id and direction info.
        val code: String = when {
            directionId != null -> "${routeId}_$directionId"
            !directionCode.isNullOrEmpty() -> "${routeId}_$directionCode"
            else -> routeId
        }
        
        return TransportationLine(
            id = code,
            kind = TransportationLineKind.BUS,
            name = finalRouteName,
            code = code,
            operatorCode = operatorCode,
            lineColor = route["route_color"],
            startStation = departureStop,  // Store departure stop in startStation
            endStation = destinationStop,  // Store destination stop in endStation
            destinationStation = destinationStop,  // Also store in destinationStation
            railwayTitle = LocalizedTitle(ja = cleanedRouteName, en = null),
            lineCode = null,  // GTFS routes do not use lineCode.
            lineDirection = null,
            ascendingRailDirection = null,
            descendingRailDirection = null,
            busRoute = routeId,  // Keep original route_id for trip lookup.
            pattern = null,
            busDirection = null,
            busStopPoleOrder = null,
            title = cleanedRouteName
        )
    }
    
    // Parse trips.txt to get trip info for a route.
    fun parseGTFSTripsForRoute(data: ByteArray, routeId: String): List<TripInfo> {
        val rows = parseGTFSCSV(data)
        
        return rows.mapNotNull { row ->
            val tripRouteId = row["route_id"]
            if (tripRouteId != routeId) return@mapNotNull null
            
            val tripId = row["trip_id"] ?: return@mapNotNull null
            val serviceId = row["service_id"] ?: return@mapNotNull null
            
            TripInfo(
                tripId = tripId,
                serviceId = serviceId,
                directionId = row["direction_id"]?.toIntOrNull(),
                tripHeadsign = row["trip_headsign"]?.trim()
            )
        }
    }
    
    // Trip info container.
    data class TripInfo(
        val tripId: String,
        val serviceId: String,
        val directionId: Int?,
        val tripHeadsign: String?
    )

    private fun normalizeGtfsTime(time: String?): String? {
        if (time.isNullOrEmpty()) return null
        val parts = time.split(":")
        if (parts.size < 2) return null
        val hour = parts[0].padStart(2, '0')
        val minute = parts[1].padStart(2, '0')
        return "$hour:$minute"
    }

    private fun loadServiceIdsForCalendarType(extractedDir: File, calendarType: ODPTCalendarType): Set<String> {
        val calendarFile = File(extractedDir, "calendar.txt")
        if (!calendarFile.exists() || !calendarFile.isFile) return emptySet()

        val rows = parseGTFSCSV(loadGTFSFile(extractedDir, "calendar.txt"))
        val displayType = calendarType.displayCalendarType()
        val isWeekday = displayType == ODPTCalendarType.Weekday

        return rows.mapNotNull { row ->
            val serviceId = row["service_id"] ?: return@mapNotNull null
            val monday = row["monday"] == "1"
            val tuesday = row["tuesday"] == "1"
            val wednesday = row["wednesday"] == "1"
            val thursday = row["thursday"] == "1"
            val friday = row["friday"] == "1"
            val saturday = row["saturday"] == "1"
            val sunday = row["sunday"] == "1"

            val include = if (isWeekday) {
                monday || tuesday || wednesday || thursday || friday
            } else {
                saturday || sunday
            }

            if (include) serviceId else null
        }.toSet()
    }
    
    // Fetch bus stops for a GTFS route and direction.
    // Uses trips and stop_times to resolve stop order.
    suspend fun fetchGTFSStopsForRoute(routeId: String, transportOperator: LocalDataSource, consumerKey: String): List<TransportationStop> = withContext(Dispatchers.IO) {
        // Get GTFS URL using apiLink.
        val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
        if (gtfsURL.isEmpty()) {
            throw ODPTError.InvalidData("Empty GTFS URL for ${transportOperator.name}")
        }
        
        // Extract route_id and direction info from code.
        // directionCode uses "|" between first/last stop IDs.
        val originalRouteId: String
        var targetDirectionId: Int? = null
        var targetFirstStopId: String? = null
        var targetLastStopId: String? = null
        
        // If code contains "|", treat it as directionCode.
        if (routeId.contains("|")) {
            // Format: "route_id_directionCode" with "firstStopId|lastStopId".
            val firstUnderscoreIndex = routeId.indexOf("_")
            if (firstUnderscoreIndex >= 0) {
                originalRouteId = routeId.take(firstUnderscoreIndex)
                val directionCode = routeId.substring(firstUnderscoreIndex + 1)
                // Parse directionCode into endpoints.
                val directionCodeParts = directionCode.split("|")
                if (directionCodeParts.size >= 2) {
                    targetFirstStopId = directionCodeParts[0]
                    targetLastStopId = directionCodeParts.drop(1).joinToString("|")
                }
            } else {
                originalRouteId = routeId
            }
        } else {
            // No "|": route_id or route_id_directionId.
            val parts = routeId.split("_")
            originalRouteId = parts[0]
            
            if (parts.size >= 2) {
                // Check if second part is a direction_id.
                val dirId = parts[1].toIntOrNull()
                if (dirId != null) {
                    // It's a direction_id (0 or 1).
                    targetDirectionId = dirId
                }
            }
        }
        
        // Get extracted directory (uses cache when available).
        val extractedDir = getExtractedGTFSDirectory(transportOperator, consumerKey, gtfsURL)
        
        // Load translations for localization.
        val translations = loadTranslations(extractedDir)
        
        // Step 1: get trips for route and direction.
        val tripsData = loadGTFSFile(extractedDir, "trips.txt")
        val allTrips = parseGTFSTripsForRoute(tripsData, originalRouteId)
        
        // Stream stop_times.txt to avoid OOM.
        val stopTimesFile = File(extractedDir, "stop_times.txt")
        
        // Filter by direction_id or endpoint IDs.
        var filteredTrips = allTrips
        if (targetDirectionId != null) {
            // Filter by direction_id.
            filteredTrips = allTrips.filter { trip ->
                trip.directionId == targetDirectionId
            }
        } else if (targetFirstStopId != null && targetLastStopId != null) {
            // Filter by endpoints using stop_times.txt.
            val tripEndpoints = mutableMapOf<String, Pair<String, String>>()
            val tripStopSequences = mutableMapOf<String, MutableList<Pair<String, Int>>>()
            
            if (stopTimesFile.exists() && stopTimesFile.isFile) {
                parseGTFSCSVStreaming(stopTimesFile) { row ->
                    val tripId = row["trip_id"] ?: return@parseGTFSCSVStreaming
                    val stopId = row["stop_id"] ?: return@parseGTFSCSVStreaming
                    val sequenceStr = row["stop_sequence"] ?: return@parseGTFSCSVStreaming
                    val sequence = sequenceStr.toIntOrNull() ?: return@parseGTFSCSVStreaming
                    
                    if (!tripStopSequences.containsKey(tripId)) {
                        tripStopSequences[tripId] = mutableListOf()
                    }
                    tripStopSequences[tripId]!!.add(Pair(stopId, sequence))
                }
                
                // Get first and last stop_id for each trip.
                for ((tripId, stops) in tripStopSequences) {
                    val sortedStops = stops.sortedBy { it.second }
                    if (sortedStops.isNotEmpty()) {
                        val first = sortedStops.first()
                        val last = sortedStops.last()
                        tripEndpoints[tripId] = Pair(first.first, last.first)
                    }
                }
            }
            
            // Filter trips that match the target endpoints.
            filteredTrips = allTrips.filter { trip ->
                val endpoints = tripEndpoints[trip.tripId]
                endpoints != null && endpoints.first == targetFirstStopId && endpoints.second == targetLastStopId
            }
        }
        
        if (filteredTrips.isEmpty()) {
            return@withContext emptyList()
        }
        
        // Step 2: choose a trip and collect its stop sequence.
        val selectedTrip = filteredTrips[if (filteredTrips.size > 1) 1 else 0]
        
        // Stream stop_times.txt again to get the stop list.
        val tripStopTimes = mutableListOf<Pair<String, Int>>()
        if (stopTimesFile.exists() && stopTimesFile.isFile) {
            parseGTFSCSVStreaming(stopTimesFile) { row ->
                val tripId = row["trip_id"] ?: return@parseGTFSCSVStreaming
                if (tripId != selectedTrip.tripId) {
                    return@parseGTFSCSVStreaming
                }
                
                val stopId = row["stop_id"] ?: return@parseGTFSCSVStreaming
                val sequenceStr = row["stop_sequence"] ?: return@parseGTFSCSVStreaming
                val sequence = sequenceStr.toIntOrNull() ?: return@parseGTFSCSVStreaming
                
                tripStopTimes.add(Pair(stopId, sequence))
            }
        }
        
        // Sort by stop_sequence.
        tripStopTimes.sortBy { it.second }
        
        // Step 3: load stop names from stops.txt.
        val stopsData = loadGTFSFile(extractedDir, "stops.txt")
        val stopsRows = parseGTFSCSV(stopsData)
        val stopsDict = mutableMapOf<String, String>()
        for (row in stopsRows) {
            val stopId = row["stop_id"]
            val stopName = row["stop_name"]
            if (stopId != null && stopName != null) {
                // Apply translation to stop name.
                val localizedStopName = getLocalizedText(
                    original = stopName,
                    tableName = "stops",
                    fieldName = "stop_name",
                    recordId = stopId,
                    translations = translations
                )
                stopsDict[stopId] = localizedStopName
            }
        }
        
        // Convert to TransportationStop models.
        tripStopTimes.mapIndexedNotNull { index, stopTime ->
            val stopName = stopsDict[stopTime.first] ?: return@mapIndexedNotNull null
            TransportationStop(
                kind = TransportationLineKind.BUS,
                name = stopName,
                code = stopTime.first,
                index = index,
                lineCode = originalRouteId,
                title = LocalizedTitle(ja = stopName, en = null),
                note = stopName,
                busStopPole = stopTime.first
            )
        }
    }

    // Generate bus timetable data for a GTFS route and stops.
    suspend fun fetchGTFSTimetableForRoute(
        routeId: String,
        departureStopId: String,
        arrivalStopId: String,
        calendarType: ODPTCalendarType,
        transportOperator: LocalDataSource,
        consumerKey: String
    ): List<TransportationTime> = withContext(Dispatchers.IO) {
        val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
        if (gtfsURL.isEmpty()) return@withContext emptyList()

        val originalRouteId: String
        var targetDirectionId: Int? = null
        var targetFirstStopId: String? = null
        var targetLastStopId: String? = null

        if (routeId.contains("|")) {
            val firstUnderscoreIndex = routeId.indexOf("_")
            if (firstUnderscoreIndex >= 0) {
                originalRouteId = routeId.take(firstUnderscoreIndex)
                val directionCode = routeId.substring(firstUnderscoreIndex + 1)
                val directionCodeParts = directionCode.split("|")
                if (directionCodeParts.size >= 2) {
                    targetFirstStopId = directionCodeParts[0]
                    targetLastStopId = directionCodeParts[1]
                }
            } else {
                originalRouteId = routeId
            }
        } else {
            val parts = routeId.split("_")
            originalRouteId = parts[0]
            if (parts.size >= 2) {
                val dirId = parts[1].toIntOrNull()
                if (dirId != null) {
                    targetDirectionId = dirId
                }
            }
        }

        val extractedDir = getExtractedGTFSDirectory(transportOperator, consumerKey, gtfsURL)
        val tripsData = loadGTFSFile(extractedDir, "trips.txt")
        val allTrips = parseGTFSTripsForRoute(tripsData, originalRouteId)

        val stopTimesFile = File(extractedDir, "stop_times.txt")
        var filteredTrips = allTrips

        if (targetDirectionId != null) {
            filteredTrips = allTrips.filter { trip -> trip.directionId == targetDirectionId }
        } else if (targetFirstStopId != null && targetLastStopId != null) {
            val tripEndpoints = mutableMapOf<String, Pair<String, String>>()
            val tripStopSequences = mutableMapOf<String, MutableList<Pair<String, Int>>>()

            if (stopTimesFile.exists() && stopTimesFile.isFile) {
                parseGTFSCSVStreaming(stopTimesFile) { row ->
                    val tripId = row["trip_id"] ?: return@parseGTFSCSVStreaming
                    val stopId = row["stop_id"] ?: return@parseGTFSCSVStreaming
                    val sequenceStr = row["stop_sequence"] ?: return@parseGTFSCSVStreaming
                    val sequence = sequenceStr.toIntOrNull() ?: return@parseGTFSCSVStreaming

                    if (!tripStopSequences.containsKey(tripId)) {
                        tripStopSequences[tripId] = mutableListOf()
                    }
                    tripStopSequences[tripId]!!.add(Pair(stopId, sequence))
                }

                for ((tripId, stops) in tripStopSequences) {
                    val sortedStops = stops.sortedBy { it.second }
                    if (sortedStops.isNotEmpty()) {
                        val first = sortedStops.first()
                        val last = sortedStops.last()
                        tripEndpoints[tripId] = Pair(first.first, last.first)
                    }
                }
            }

            filteredTrips = allTrips.filter { trip ->
                val endpoints = tripEndpoints[trip.tripId]
                endpoints != null && endpoints.first == targetFirstStopId && endpoints.second == targetLastStopId
            }
        }

        if (filteredTrips.isEmpty()) return@withContext emptyList()

        val serviceIds = loadServiceIdsForCalendarType(extractedDir, calendarType)
        if (serviceIds.isNotEmpty()) {
            filteredTrips = filteredTrips.filter { trip -> serviceIds.contains(trip.serviceId) }
        }

        if (filteredTrips.isEmpty()) return@withContext emptyList()

        if (!stopTimesFile.exists() || !stopTimesFile.isFile) return@withContext emptyList()

        val tripIds = filteredTrips.map { it.tripId }.toSet()
        val departureTimes = mutableMapOf<String, String>()
        val arrivalTimes = mutableMapOf<String, String>()

        parseGTFSCSVStreaming(stopTimesFile) { row ->
            val tripId = row["trip_id"] ?: return@parseGTFSCSVStreaming
            if (!tripIds.contains(tripId)) return@parseGTFSCSVStreaming
            val stopId = row["stop_id"] ?: return@parseGTFSCSVStreaming
            if (stopId != departureStopId && stopId != arrivalStopId) return@parseGTFSCSVStreaming

            val depTime = normalizeGtfsTime(row["departure_time"])
            val arrTime = normalizeGtfsTime(row["arrival_time"])

            if (stopId == departureStopId) {
                val time = depTime ?: arrTime
                if (time != null) {
                    departureTimes[tripId] = time
                }
            } else {
                val time = arrTime ?: depTime
                if (time != null) {
                    arrivalTimes[tripId] = time
                }
            }
        }

        val transportationTimes = mutableListOf<TransportationTime>()
        for (tripId in tripIds) {
            val departureTime = departureTimes[tripId]
            val arrivalTime = arrivalTimes[tripId]
            if (departureTime == null || arrivalTime == null) continue

            val depMinutes = departureTime.timeToMinutes
            val arrMinutes = arrivalTime.timeToMinutes
            if (arrMinutes <= depMinutes) continue

            val rideTime = departureTime.calculateRideTime(arrivalTime)
            val busTime = BusTime(
                departureTime = departureTime,
                arrivalTime = arrivalTime,
                busNumber = null,
                routePattern = null,
                rideTime = rideTime
            )
            transportationTimes.add(busTime)
        }

        transportationTimes.sortedBy { it.departureTime }
    }
}

