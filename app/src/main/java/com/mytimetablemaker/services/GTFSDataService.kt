package com.mytimetablemaker.services

import android.content.Context
import android.content.SharedPreferences
import com.mytimetablemaker.R
import com.mytimetablemaker.models.*
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.services.CacheStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import androidx.core.content.edit

// MARK: - GTFS Data Service
// Handles GTFS data processing, parsing, and timetable generation
class GTFSDataService(private val context: Context) {
    private val cache: CacheStore
    // In-memory cache for loaded GTFS files (key: directory.path + filename)
    private val fileCache: MutableMap<String, ByteArray> = mutableMapOf()
    private val sharedPreferences: SharedPreferences
    
    init {
        cache = CacheStore(context)
        sharedPreferences = context.getSharedPreferences("GTFSDataService", Context.MODE_PRIVATE)
    }
    
    // MARK: - Translation Data Structure
    // Structure to hold GTFS translation data
    private data class TranslationEntry(
        val tableName: String,
        val fieldName: String,
        val language: String,
        val translation: String,
        val recordId: String?,
        val fieldValue: String?
    )
    
    // MARK: - Load Translations
    // Load translations.txt from GTFS data and return a dictionary for quick lookup
    // Key format: "tableName|fieldName|recordId" or "tableName|fieldName|fieldValue"
    // Loads English translations for non-Japanese languages
    private fun loadTranslations(extractedDir: File): Map<String, String> {
        val translationsData = try {
            loadGTFSFile(extractedDir, "translations.txt")
        } catch (e: Exception) {
            return emptyMap()
        }
        
        val translations = try {
            parseGTFSCSV(translationsData)
        } catch (e: Exception) {
            return emptyMap()
        }
        
        val translationDict = mutableMapOf<String, String>()
        val currentLanguage = Locale.getDefault().language
        
        // Only load translations for non-Japanese languages
        if (currentLanguage == "ja") {
            return emptyMap()
        }
        
        // Load English translations (most GTFS feeds only have English translations)
        // Try current language first, then fallback to English
        val targetLanguages = if (currentLanguage == "en") listOf("en") else listOf(currentLanguage, "en")
        
        for (row in translations) {
            val tableName = row["table_name"] ?: continue
            val fieldName = row["field_name"] ?: continue
            val language = row["language"] ?: continue
            val translation = row["translation"] ?: continue
            
            if (!targetLanguages.contains(language)) continue
            
            // Create key based on record_id and/or field_value
            // GTFS translations.txt can have both record_id and field_value, or just one of them
            // Create keys for both to ensure we can find translations regardless of which is used
            val recordId = row["record_id"]
            if (!recordId.isNullOrEmpty()) {
                val key = "$tableName|$fieldName|$recordId"
                // Prefer current language, but allow English as fallback
                if (language == currentLanguage || !translationDict.containsKey(key)) {
                    translationDict[key] = translation
                }
            }
            val fieldValue = row["field_value"]
            if (!fieldValue.isNullOrEmpty()) {
                val key = "$tableName|$fieldName|$fieldValue"
                // Prefer current language, but allow English as fallback
                if (language == currentLanguage || !translationDict.containsKey(key)) {
                    translationDict[key] = translation
                }
            }
        }
        
        return translationDict
    }
    
    // MARK: - Convert Fullwidth to Halfwidth
    // Convert fullwidth numbers and alphabets to halfwidth
    private fun convertFullwidthToHalfwidth(text: String): String {
        // Convert fullwidth numbers (０-９) to halfwidth (0-9)
        // Convert fullwidth alphabets (Ａ-Ｚ, ａ-ｚ) to halfwidth (A-Z, a-z)
        var result = text
        val fullwidthNumbers = "０１２３４５６７８９"
        val halfwidthNumbers = "0123456789"
        val fullwidthUpper = "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ"
        val halfwidthUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val fullwidthLower = "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ"
        val halfwidthLower = "abcdefghijklmnopqrstuvwxyz"
        
        // Replace fullwidth numbers
        for (i in fullwidthNumbers.indices) {
            result = result.replace(fullwidthNumbers[i], halfwidthNumbers[i])
        }
        
        // Replace fullwidth uppercase letters
        for (i in fullwidthUpper.indices) {
            result = result.replace(fullwidthUpper[i], halfwidthUpper[i])
        }
        
        // Replace fullwidth lowercase letters
        for (i in fullwidthLower.indices) {
            result = result.replace(fullwidthLower[i], halfwidthLower[i])
        }
        
        return result
    }
    
    // MARK: - Get Localized Text
    // Get localized text from translations, or return original if Japanese or no translation found
    // Also converts fullwidth numbers and alphabets to halfwidth
    private fun getLocalizedText(
        original: String,
        tableName: String,
        fieldName: String,
        recordId: String?,
        translations: Map<String, String>
    ): String {
        val currentLanguage = Locale.getDefault().language
        
        val result: String
        
        // Return original for Japanese
        if (currentLanguage == "ja") {
            result = original
        } else {
            // Try to get translation
            if (!recordId.isNullOrEmpty()) {
                val key = "$tableName|$fieldName|$recordId"
                result = if (translations.containsKey(key) && translations[key]!!.isNotEmpty()) {
                    translations[key]!!
                } else {
                    // Try field_value as fallback
                    val key2 = "$tableName|$fieldName|$original"
                    if (translations.containsKey(key2) && translations[key2]!!.isNotEmpty()) {
                        translations[key2]!!
                    } else {
                        original
                    }
                }
            } else {
                // Try field_value as fallback
                val key = "$tableName|$fieldName|$original"
                result = if (translations.containsKey(key) && translations[key]!!.isNotEmpty()) {
                    translations[key]!!
                } else {
                    original
                }
            }
        }
        
        // Convert fullwidth numbers and alphabets to halfwidth
        return convertFullwidthToHalfwidth(result)
    }
    
    // MARK: - Load GTFS File
    // Load a GTFS file from extracted directory.
    // Uses in-memory cache to avoid reading the same file multiple times.
    private fun loadGTFSFile(directory: File, filename: String): ByteArray {
        val cacheKey = "${directory.absolutePath}/$filename"
        
        // Check cache first
        if (fileCache.containsKey(cacheKey)) {
            println("📄 Using cached GTFS file: $filename from ${directory.absolutePath}")
            return fileCache[cacheKey]!!
        }
        
        // Load from disk
        val file = File(directory, filename)
        println("📄 Loading GTFS file: $filename from ${directory.absolutePath}")
        val data = file.readBytes()
        
        // Cache for future use
        fileCache[cacheKey] = data
        
        return data
    }
    
    // MARK: - Parse GTFS CSV File
    // Parse a GTFS CSV file and return array of dictionaries.
    fun parseGTFSCSV(data: ByteArray): List<Map<String, String>> {
        val content = String(data, Charsets.UTF_8)
        
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        if (lines.isEmpty()) {
            return emptyList()
        }
        
        // Parse header
        val header = parseCSVLine(lines[0])
        if (header.isEmpty()) {
            throw ODPTError.InvalidData()
        }
        
        // Parse data rows
        val results = mutableListOf<Map<String, String>>()
        for (i in 1 until lines.size) {
            val values = parseCSVLine(lines[i])
            if (values.size != header.size) {
                continue // Skip malformed rows
            }
            
            val row = mutableMapOf<String, String>()
            for (j in header.indices) {
                row[header[j]] = if (j < values.size) values[j] else ""
            }
            results.add(row)
        }
        
        return results
    }
    
    // MARK: - Parse GTFS CSV File (Streaming)
    // Parse a large GTFS CSV file using streaming to avoid OutOfMemoryError.
    // This function processes the file line by line without loading the entire file into memory.
    private fun parseGTFSCSVStreaming(file: File, onRow: (Map<String, String>) -> Unit) {
        if (!file.exists() || !file.isFile) {
            return
        }
        
        file.bufferedReader(Charsets.UTF_8).use { reader ->
            // Read header
            val headerLine = reader.readLine() ?: return
            val header = parseCSVLine(headerLine.trim())
            if (header.isEmpty()) {
                throw ODPTError.InvalidData()
            }
            
            // Process data rows line by line
            reader.lineSequence().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) {
                    return@forEach
                }
                
                val values = parseCSVLine(trimmedLine)
                if (values.size != header.size) {
                    return@forEach // Skip malformed rows
                }
                
                val row = mutableMapOf<String, String>()
                for (j in header.indices) {
                    row[header[j]] = if (j < values.size) values[j] else ""
                }
                onRow(row)
            }
        }
    }
    
    // MARK: - Parse CSV Line
    // Parse a single CSV line, handling quoted fields.
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
    
    // MARK: - GTFS ZIP Download (Public)
    // Download GTFS ZIP file and extract it for caching at startup.
    // This is used to pre-download and extract ZIP files at startup for faster access later.
    suspend fun downloadGTFSZipOnly(url: String, consumerKey: String, transportOperator: LocalDataSource): ByteArray {
        return downloadGTFSZip(url, consumerKey, transportOperator)
    }
    
    // MARK: - GTFS ZIP Download
    // Download GTFS ZIP file from ODPT API with consumer key authentication.
    // Uses cache to avoid re-downloading the same file.
    // For operators with date in GTFSDates, cache key includes date.
    // For Toei Bus (no date), uses ETag/Last-Modified for conditional GET to detect updates.
    private suspend fun downloadGTFSZip(url: String, consumerKey: String, transportOperator: LocalDataSource? = null): ByteArray = withContext(Dispatchers.IO) {
        // transportOperator must be provided for GTFS
        val transportOp = transportOperator ?: throw ODPTError.InvalidData()
        
        // Generate cache key from transport operator
        // cacheKey includes date, so if cached file exists, it's already for the correct date
        // For Toei Bus, date is empty, so use different cache key format
        val date = GTFSDates.dateFor(transportOp) ?: ""
        val gtfsFileName = transportOp.gtfsFileName()
        val cacheKey = if (date.isEmpty()) "gtfs_$gtfsFileName.zip" else "gtfs_${gtfsFileName}_$date.zip"
        
        // Check if cached file exists
        val cachedData = cache.loadData(cacheKey)
        if (cachedData != null) {
            // Check if extracted directory is also cached
            val extractedCacheDirName = cacheKey.replace(".zip", "_extracted")
            if (!cache.directoryExists(extractedCacheDirName)) {
                // ZIP is cached but extracted directory is not, extract it now
                println("📦 ZIP cached but extracted directory not found, extracting...")
                extractAndCacheGTFSZip(cachedData, transportOp, cacheKey)
            }
            
            // For Toei Bus, check if server has updated file using conditional GET
            // For other operators with date, cache key includes date, so cached file is already for the correct date
            if (transportOp == LocalDataSource.TOEI_BUS) {
                // Toei Bus: Use conditional GET to check if file has been updated
                val updatedData = checkForToeiBusGTFSUpdate(url, cacheKey)
                if (updatedData != null) {
                    // File was updated, return new data
                    return@withContext updatedData
                } else {
                    // File not modified, return cached data
                    println("📦 Using cached GTFS ZIP (not modified): $cacheKey (${cachedData.size} bytes)")
                    return@withContext cachedData
                }
            } else {
                // Operator with date: cache key includes date, so cached file is already for the correct date
                println("📦 Using cached GTFS ZIP: $cacheKey (${cachedData.size} bytes, date: $date)")
                return@withContext cachedData
            }
        }
        
        // No cache exists, download new file
        downloadNewGTFSZip(url, consumerKey, transportOp, cacheKey)
    }
    
    // MARK: - Check for Toei Bus GTFS Update (Conditional GET)
    // Check if Toei Bus GTFS ZIP file has been updated on the server using conditional GET.
    // This function is only for Toei Bus (operators without date in GTFSDates).
    // Returns new data if updated, null if not modified.
    private suspend fun checkForToeiBusGTFSUpdate(urlString: String, cacheKey: String): ByteArray? = withContext(Dispatchers.IO) {
        val url = urlString.toHttpUrl()
        
        // Get ETag and Last-Modified from SharedPreferences
        val etagKey = "${cacheKey}_etag"
        val lastModifiedKey = "${cacheKey}_last_modified"
        val etag = sharedPreferences.getString(etagKey, null)
        val lastModified = sharedPreferences.getString(lastModifiedKey, null)
        
        // If no ETag/Last-Modified, can't use conditional GET, so download new file
        if (etag == null && lastModified == null) {
            println("⚠️ No ETag/Last-Modified for $cacheKey, downloading new file")
            return@withContext downloadNewGTFSZip(urlString, "", LocalDataSource.TOEI_BUS, cacheKey)
        }
        
        val requestBuilder = Request.Builder().url(url)
        // Toei Bus uses public API, no access token needed
        requestBuilder.addHeader("Accept", "application/zip")
        
        // Add conditional headers
        etag?.let { requestBuilder.addHeader("If-None-Match", it) }
        lastModified?.let { requestBuilder.addHeader("If-Modified-Since", it) }
        
        println("🔍 Checking for GTFS update using conditional GET: $url")
        
        val client = OkHttpClient()
        val response = client.newCall(requestBuilder.build()).execute()
        
        // Handle 304 Not Modified
        if (response.code == 304) {
            println("✅ GTFS ZIP not modified (304), using cached file")
            return@withContext null // File not modified, use cached data
        }
        
        // Handle 200 OK (file was updated)
        if (response.code != 200) {
            throw ODPTError.NetworkError("HTTP ${response.code}")
        }
        
        println("🔄 GTFS ZIP updated on server, downloading new file")
        
        val data = response.body.bytes()
        
        // Save ETag and Last-Modified for future conditional GET
        response.header("ETag")?.let { newEtag ->
            sharedPreferences.edit { putString(etagKey, newEtag) }
        }
        response.header("Last-Modified")?.let { newLastModified ->
            sharedPreferences.edit { putString(lastModifiedKey, newLastModified) }
        }
        
        // Save new data to cache
        cache.saveData(data, cacheKey)
        println("✅ Downloaded and cached updated GTFS ZIP: ${data.size} bytes")
        
        // Extract and cache the extracted directory
        extractAndCacheGTFSZip(data, LocalDataSource.TOEI_BUS, cacheKey)
        
        data
    }
    
    // MARK: - Download New GTFS ZIP
    // Download a new GTFS ZIP file from the server.
    private suspend fun downloadNewGTFSZip(url: String, consumerKey: String, transportOperator: LocalDataSource, cacheKey: String): ByteArray = withContext(Dispatchers.IO) {
        val httpUrl = url.toHttpUrl()
        
        val requestBuilder = Request.Builder().url(httpUrl)
        // Toei Bus uses public API, no access token needed
        if (transportOperator != LocalDataSource.TOEI_BUS) {
            requestBuilder.addHeader("Authorization", consumerKey)
        }
        requestBuilder.addHeader("Accept", "application/zip")
        
        println("🔗 Downloading GTFS ZIP from: $httpUrl")
        
        val client = OkHttpClient()
        val response = client.newCall(requestBuilder.build()).execute()
        
        // Handle redirects
        if (response.code == 301 || response.code == 302) {
            val location = response.header("Location")
            if (location != null) {
                println("🔄 Redirecting to: $location")
                val redirectUrl = location.toHttpUrl()
                val redirectRequest = Request.Builder().url(redirectUrl).build()
                val redirectResponse = client.newCall(redirectRequest).execute()
                
                if (redirectResponse.code != 200) {
                    throw ODPTError.NetworkError("Failed to download from redirect URL")
                }
                
                val redirectData = redirectResponse.body.bytes()
                
                // Save ETag and Last-Modified for future conditional GET
                val etagKey = "${cacheKey}_etag"
                val lastModifiedKey = "${cacheKey}_last_modified"
                redirectResponse.header("ETag")?.let { etag ->
                    sharedPreferences.edit { putString(etagKey, etag) }
                }
                redirectResponse.header("Last-Modified")?.let { lastModified ->
                    sharedPreferences.edit { putString(lastModifiedKey, lastModified) }
                }
                
                // Save to cache
                cache.saveData(redirectData, cacheKey)
                println("✅ Downloaded and cached GTFS ZIP: ${redirectData.size} bytes")
                
                // Extract and cache the extracted directory
                extractAndCacheGTFSZip(redirectData, transportOperator, cacheKey)
                
                return@withContext redirectData
            }
        }
        
        if (response.code != 200) {
            throw ODPTError.NetworkError("HTTP ${response.code}")
        }
        
        val data = response.body.bytes()
        
        // Save ETag and Last-Modified for future conditional GET
        val etagKey = "${cacheKey}_etag"
        val lastModifiedKey = "${cacheKey}_last_modified"
        response.header("ETag")?.let { etag ->
            sharedPreferences.edit { putString(etagKey, etag) }
        }
        response.header("Last-Modified")?.let { lastModified ->
            sharedPreferences.edit { putString(lastModifiedKey, lastModified) }
        }
        
        // Save to cache
        cache.saveData(data, cacheKey)
        println("✅ Downloaded and cached GTFS ZIP: ${data.size} bytes")
        
        // Extract and cache the extracted directory
        extractAndCacheGTFSZip(data, transportOperator, cacheKey)
        
        data
    }
    
    // MARK: - Extract and Cache GTFS ZIP
    // Extract GTFS ZIP file and save the extracted directory to cache.
    private suspend fun extractAndCacheGTFSZip(data: ByteArray, transportOperator: LocalDataSource, cacheKey: String) = withContext(Dispatchers.IO) {
        // Generate cache directory name from cache key
        val extractedCacheDirName = cacheKey.replace(".zip", "_extracted")
        
        // Check if extracted directory is already cached
        if (cache.directoryExists(extractedCacheDirName)) {
            println("✅ Extracted directory already cached: $extractedCacheDirName")
            return@withContext
        }
        
        // Extract to temporary directory first
        val tempDir = File(context.cacheDir, "gtfs_extract_temp_${UUID.randomUUID()}")
        val extractedDir = extractGTFSZipToTemp(data, tempDir)
        
        // Copy extracted directory to cache
        cache.saveDirectory(extractedDir, extractedCacheDirName)
        println("✅ Cached extracted GTFS directory: $extractedCacheDirName")
        
        // Clean up temporary directory
        tempDir.deleteRecursively()
    }
    
    // MARK: - Extract GTFS ZIP to Temporary Directory
    // Extract GTFS ZIP file to a temporary directory (for caching purposes).
    private fun extractGTFSZipToTemp(data: ByteArray, directory: File): File {
        val extractedDir = File(directory, "extracted")
        
        // Create directory if needed
        directory.mkdirs()
        extractedDir.mkdirs()
        
        // Write ZIP data to file
        val zipFile = File(directory, "gtfs.zip")
        zipFile.writeBytes(data)
        
        // Extract using ZipInputStream
        ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val file = File(extractedDir, entry.name)
                    // Create parent directories if needed
                    file.parentFile?.mkdirs()
                    
                    // Write file
                    file.outputStream().use { output ->
                        zipInputStream.copyTo(output)
                    }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        }
        
        // Remove temporary ZIP file
        zipFile.delete()
        
        return extractedDir
    }
    
    // MARK: - Get Extracted GTFS Directory
    // Get extracted GTFS directory, using cache if available, otherwise download and extract.
    // Returns File of extracted directory.
    // Uses cached directory directly if available (no copying needed since it's read-only).
    private suspend fun getExtractedGTFSDirectory(transportOperator: LocalDataSource, consumerKey: String, gtfsURL: String): File = withContext(Dispatchers.IO) {
        val date = GTFSDates.dateFor(transportOperator) ?: ""
        val gtfsFileName = transportOperator.gtfsFileName()
        val cacheKey = if (date.isEmpty()) "gtfs_$gtfsFileName.zip" else "gtfs_${gtfsFileName}_$date.zip"
        val extractedCacheDirName = cacheKey.replace(".zip", "_extracted")
        
        // Check if extracted directory is already cached
        val cachedExtractedDir = cache.loadDirectoryPath(extractedCacheDirName)
        if (cachedExtractedDir != null) {
            // Verify that routes.txt exists (valid extracted directory)
            val routesFile = File(cachedExtractedDir, "routes.txt")
            if (routesFile.exists()) {
                // Use cached directory directly (read-only access, no copying needed)
                println("📂 Using cached GTFS directory: ${cachedExtractedDir.absolutePath}")
                return@withContext cachedExtractedDir
            }
        }
        
        // Cache not available or invalid, download and extract
        // downloadGTFSZip() will call extractAndCacheGTFSZip() to cache the extracted directory
        println("📦 Extracted directory not in cache, downloading and extracting ZIP")
        downloadGTFSZip(gtfsURL, consumerKey, transportOperator)
        
        // After download and extraction, the directory should be in cache
        val cachedDir = cache.loadDirectoryPath(extractedCacheDirName)
            ?: throw ODPTError.NetworkError("Failed to cache extracted GTFS directory")
        
        // Verify that routes.txt exists (valid extracted directory)
        val routesFile = File(cachedDir, "routes.txt")
        if (!routesFile.exists()) {
            throw ODPTError.NetworkError("Extracted GTFS directory is invalid")
        }
        
        println("📂 Using cached GTFS directory: ${cachedDir.absolutePath}")
        cachedDir
    }
    
    // MARK: - GTFS Data Processing
    // Download and process GTFS data for operators that use GTFS format.
    // Returns TransportationLine models directly (no intermediate GTFS models).
    // For routes with multiple trip_headsigns (round trips), creates separate lines for each direction.
    suspend fun fetchGTFSData(transportOperator: LocalDataSource, consumerKey: String): List<TransportationLine> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("GTFSDataService", "🚌 fetchGTFSData: Starting for ${transportOperator.name}")
            
            // Get GTFS URL using apiLink
            val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
            if (gtfsURL.isEmpty()) {
                android.util.Log.e("GTFSDataService", "🚌 fetchGTFSData: Empty GTFS URL for ${transportOperator.name}")
                throw ODPTError.InvalidData()
            }
            android.util.Log.d("GTFSDataService", "🚌 fetchGTFSData: GTFS URL=$gtfsURL")
            
            // Get extracted directory (uses cache if available)
            val extractedDir = getExtractedGTFSDirectory(transportOperator, consumerKey, gtfsURL)
            android.util.Log.d("GTFSDataService", "🚌 fetchGTFSData: Extracted directory=${extractedDir.absolutePath}, exists=${extractedDir.exists()}")
            
            // Load translations.txt for localization
            val translations = loadTranslations(extractedDir)
            android.util.Log.d("GTFSDataService", "🚌 fetchGTFSData: Loaded ${translations.size} translations")
            
            // Parse routes.txt to get basic route information
            val routesData = try {
                loadGTFSFile(extractedDir, "routes.txt")
            } catch (e: Exception) {
                android.util.Log.e("GTFSDataService", "🚌 fetchGTFSData: Failed to load routes.txt: ${e.message}", e)
                throw e
            }
            val routes = try {
                parseGTFSCSV(routesData)
            } catch (e: Exception) {
                android.util.Log.e("GTFSDataService", "🚌 fetchGTFSData: Failed to parse routes.txt: ${e.message}", e)
                throw e
            }
            android.util.Log.d("GTFSDataService", "🚌 fetchGTFSData: Parsed ${routes.size} routes")
            
            // Parse trips.txt to get trip_headsign and direction_id information for each route
            val tripsData = try {
                loadGTFSFile(extractedDir, "trips.txt")
            } catch (e: Exception) {
                android.util.Log.e("GTFSDataService", "🚌 fetchGTFSData: Failed to load trips.txt: ${e.message}", e)
                throw e
            }
            val trips = try {
                parseGTFSCSV(tripsData)
            } catch (e: Exception) {
                android.util.Log.e("GTFSDataService", "🚌 fetchGTFSData: Failed to parse trips.txt: ${e.message}", e)
                throw e
            }
            android.util.Log.d("GTFSDataService", "🚌 fetchGTFSData: Parsed ${trips.size} trips")
        
        // Group trips by route_id and get unique (trip_headsign, direction_id) combinations for each route
        // Use a data class to represent direction information
        data class DirectionInfo(
            val headsign: String?,
            val directionId: Int?,
            val firstStopId: String?,  // First stop_id from stop_times.txt (for routes without headsign/direction_id)
            val lastStopId: String?    // Last stop_id from stop_times.txt (for routes without headsign/direction_id)
        )
        
        // If trip_headsign and direction_id are missing, use stop_times.txt to determine directions
        // Load stop_times.txt to get first and last stop_id for each trip
        // Also load stops.txt to get stop names from stop_id
        val tripEndpoints = mutableMapOf<String, Pair<String, String>>()
        val stopsDict = mutableMapOf<String, String>()  // stop_id -> stop_name mapping
        
        // Load stops.txt to get stop names
        val stopsData = try {
            loadGTFSFile(extractedDir, "stops.txt")
        } catch (e: Exception) {
            null
        }
        if (stopsData != null) {
            val stopsRows = parseGTFSCSV(stopsData)
            for (row in stopsRows) {
                val stopId = row["stop_id"]
                val stopName = row["stop_name"]
                if (stopId != null && stopName != null) {
                    // Apply translation to stop name
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
        
        // Process stop_times.txt using streaming to avoid OutOfMemoryError
        val stopTimesFile = File(extractedDir, "stop_times.txt")
        if (stopTimesFile.exists() && stopTimesFile.isFile) {
            val tripStopSequences = mutableMapOf<String, MutableList<Pair<String, Int>>>()
            
            // Process stop_times.txt line by line using streaming
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
            
            // Get first and last stop_id for each trip
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
            // Apply translation to trip_headsign
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
            
            // Get first and last stop_id if available (for routes without headsign/direction_id)
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
        
        // Create TransportationLine for each route and (trip_headsign, direction_id) combination
        val transportationLines = mutableListOf<TransportationLine>()
        var skippedRoutes = 0
        for (route in routes) {
            val routeId = route["route_id"] ?: run {
                skippedRoutes++
                continue
            }
            
            // Get direction info for this route (if any)
            val directions = routeDirections[routeId] ?: emptySet()
            
            if (directions.isEmpty()) {
                // No trip information - create single line without direction info
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
                } else {
                    android.util.Log.w("GTFSDataService", "🚌 fetchGTFSData: createTransportationLine returned null for route_id=$routeId (no directions)")
                }
            } else if (directions.size == 1) {
                // Only one direction - create single line
                val direction = directions.first()
                // Use lastStopId's stop name as headsign if headsign is not available (for display)
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
                } else {
                    android.util.Log.w("GTFSDataService", "🚌 fetchGTFSData: createTransportationLine returned null for route_id=$routeId (single direction)")
                }
            } else {
                // Multiple directions - create separate line for each direction
                // Sort by direction_id first, then by headsign, then by firstStopId/lastStopId for consistent ordering
                val sortedDirections = directions.sortedWith(compareBy(
                    { it.directionId ?: Int.MAX_VALUE },
                    { it.headsign ?: "" },
                    { it.firstStopId ?: "" },
                    { it.lastStopId ?: "" }
                ))
                
                for (direction in sortedDirections) {
                    // Use lastStopId's stop name as headsign if headsign is not available (for display)
                    val displayHeadsign: String? = when {
                        !direction.headsign.isNullOrEmpty() -> direction.headsign
                        direction.lastStopId != null && stopsDict.containsKey(direction.lastStopId) -> stopsDict[direction.lastStopId]
                        else -> null
                    }
                    
                    // Create unique code: use directionId if available, otherwise use firstStopId/lastStopId
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
                    } else {
                        android.util.Log.w("GTFSDataService", "🚌 fetchGTFSData: createTransportationLine returned null for route_id=$routeId, directionCode=$directionCode")
                    }
                }
            }
        }
        
        android.util.Log.d("GTFSDataService", "🚌 fetchGTFSData: Created ${transportationLines.size} transportation lines for ${transportOperator.name} (skipped ${skippedRoutes} routes without route_id)")
        transportationLines
        } catch (e: Exception) {
            android.util.Log.e("GTFSDataService", "🚌 fetchGTFSData: Failed to process GTFS data for ${transportOperator.name}: ${e.message}", e)
            android.util.Log.e("GTFSDataService", "🚌 fetchGTFSData: Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            throw e
        }
    }
    
    // MARK: - Create Transportation Line from Route
    // Creates a TransportationLine from a route row, optionally including trip_headsign and direction_id for direction distinction.
    // directionCode: Additional code for direction distinction (used when directionId is null, e.g., "firstStopId|lastStopId")
    private fun createTransportationLine(
        route: Map<String, String>,
        routeId: String,
        tripHeadsign: String?,
        directionId: Int?,
        directionCode: String?,
        operatorCode: String?,
        translations: Map<String, String> = emptyMap()
    ): TransportationLine? {
        // Get route name: prioritize route_short_name, fallback to route_long_name
        // Some operators (like Keisei Transit Bus) may have empty route_short_name
        var routeShortName = route["route_short_name"]?.trim()
        var routeLongName = route["route_long_name"]?.trim()
        
        // Apply translations to route names
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
            return null // Skip routes without both route_short_name and route_long_name
        }
        
        // Use trip_headsign as destination if available, otherwise extract from route_long_name
        var destinationStop: String? = null
        var departureStop: String? = null
        
        if (!tripHeadsign.isNullOrEmpty()) {
            // Use trip_headsign as destination
            destinationStop = tripHeadsign
        } else if (routeLongName != null && routeLongName.contains("〜")) {
            // Extract destination stop from route_long_name (format: "発車停〜到着停")
            val components = routeLongName.split("〜")
            if (components.size >= 2) {
                // Get the first component as departure stop
                departureStop = components.first().trim()
                // Get the last component as destination stop
                val rawDestination = components.last().trim()
                
                // Clean destination: remove parentheses and extra info
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
        
        // Build route name: add destination if route_short_name exists (not fallback)
        // If trip_headsign is available, use it as destination
        val destinationPrefix = context.getString(R.string.routeDestinationPrefix)
        val destinationSuffix = context.getString(R.string.routeDestinationSuffix)
        val routeName: String = when {
            !tripHeadsign.isNullOrEmpty() -> {
                // Use trip_headsign as destination
                val destination = "${tripHeadsign}$destinationSuffix".replace("行 行", "行").replace("行行", "行")
                "$shortName$destinationPrefix$destination"
            }
            routeShortName?.isNotEmpty() == true && destinationStop?.isNotEmpty() == true -> {
                val destination = "${destinationStop}$destinationSuffix".replace("行 行", "行").replace("行行", "行")
                "$shortName$destinationPrefix$destination"
            }
            else -> shortName
        }
        
        // Convert fullwidth numbers and alphabets to halfwidth
        val cleanedRouteName = convertFullwidthToHalfwidth(routeName)
        
        // Fix trailing "行行" to "行"
        val finalRouteName = if (cleanedRouteName.endsWith("行行")) {
            cleanedRouteName.dropLast(1)
        } else {
            cleanedRouteName
        }
        
        // Create unique code: route_id + direction_id or directionCode (trip_headsign is only for display)
        // Format: "route_id_directionId" or "route_id_directionCode" or "route_id"
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
            lineCode = null,  // GTFS routes don't use lineCode
            lineDirection = null,
            ascendingRailDirection = null,
            descendingRailDirection = null,
            busRoute = routeId,  // Keep original route_id for trip lookup
            pattern = null,
            busDirection = null,
            busstopPoleOrder = null,
            title = cleanedRouteName
        )
    }
    
    // MARK: - Parse GTFS Trips for Route
    // Parse trips.txt to get trip information for a specific route.
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
    
    // MARK: - Trip Info Data Class
    // Data class to hold trip information
    data class TripInfo(
        val tripId: String,
        val serviceId: String,
        val directionId: Int?,
        val tripHeadsign: String?
    )
    
    // MARK: - Fetch GTFS Stops for Route
    // Fetch bus stops for a specific GTFS route.
    // Process: route_id + direction_id -> trip_id list -> stop_id list (from first trip) -> stop names
    // Downloads and parses GTFS data to get stop information for the selected route.
    suspend fun fetchGTFSStopsForRoute(routeId: String, transportOperator: LocalDataSource, consumerKey: String): List<TransportationStop> = withContext(Dispatchers.IO) {
        // Get GTFS URL using apiLink
        val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
        if (gtfsURL.isEmpty()) {
            throw ODPTError.InvalidData()
        }
        
        // Extract route_id and direction info from code
        // Format: "route_id" or "route_id_directionId" or "route_id_directionCode"
        // Note: route_id itself never contains "|", so "|" in the code indicates directionCode format
        // directionCode uses "|" as separator between firstStopId and lastStopId
        // route_id and directionCode are separated by a single "_" (code = "route_id_directionCode")
        val originalRouteId: String
        var targetDirectionId: Int? = null
        var targetFirstStopId: String? = null
        var targetLastStopId: String? = null
        
        // Check if code contains "|" (indicates directionCode format, since route_id never contains "|")
        if (routeId.contains("|")) {
            // Format: "route_id_directionCode" where directionCode = "firstStopId|lastStopId"
            // route_id and directionCode are separated by a single "_"
            // Find the first "_" to separate route_id and directionCode
            val firstUnderscoreIndex = routeId.indexOf("_")
            if (firstUnderscoreIndex >= 0) {
                originalRouteId = routeId.take(firstUnderscoreIndex)
                val directionCode = routeId.substring(firstUnderscoreIndex + 1)
                // Parse directionCode: "firstStopId|lastStopId"
                val directionCodeParts = directionCode.split("|")
                if (directionCodeParts.size >= 2) {
                    targetFirstStopId = directionCodeParts[0]
                    targetLastStopId = directionCodeParts.drop(1).joinToString("|")
                }
            } else {
                originalRouteId = routeId
            }
        } else {
            // No "|" found, so it's either "route_id" or "route_id_directionId" format
            val parts = routeId.split("_")
            originalRouteId = parts[0]
            
            if (parts.size >= 2) {
                // Check if second part is a number (direction_id)
                val dirId = parts[1].toIntOrNull()
                if (dirId != null) {
                    // It's a direction_id (single digit: 0 or 1)
                    targetDirectionId = dirId
                }
            }
        }
        
        // Get extracted directory (uses cache if available)
        val extractedDir = getExtractedGTFSDirectory(transportOperator, consumerKey, gtfsURL)
        
        // Load translations.txt for localization
        val translations = loadTranslations(extractedDir)
        
        // Step 1: Get trips for the selected route_id and direction info from trips.txt
        // Create trip_id list filtered by route_id and direction_id or firstStopId/lastStopId
        val tripsData = loadGTFSFile(extractedDir, "trips.txt")
        val allTrips = parseGTFSTripsForRoute(tripsData, originalRouteId)
        
        // Process stop_times.txt using streaming to avoid OutOfMemoryError
        val stopTimesFile = File(extractedDir, "stop_times.txt")
        
        // If direction_id is specified, filter by direction_id
        // Otherwise, if firstStopId/lastStopId is specified, filter by matching trip endpoints
        var filteredTrips = allTrips
        if (targetDirectionId != null) {
            // Filter by direction_id
            filteredTrips = allTrips.filter { trip ->
                trip.directionId == targetDirectionId
            }
        } else if (targetFirstStopId != null && targetLastStopId != null) {
            // Filter by firstStopId/lastStopId: need to check stop_times.txt for each trip
            // Get first and last stop_id for each trip using streaming
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
                
                // Get first and last stop_id for each trip
                for ((tripId, stops) in tripStopSequences) {
                    val sortedStops = stops.sortedBy { it.second }
                    if (sortedStops.isNotEmpty()) {
                        val first = sortedStops.first()
                        val last = sortedStops.last()
                        tripEndpoints[tripId] = Pair(first.first, last.first)
                    }
                }
            }
            
            // Filter trips that match the target firstStopId/lastStopId
            filteredTrips = allTrips.filter { trip ->
                val endpoints = tripEndpoints[trip.tripId]
                endpoints != null && endpoints.first == targetFirstStopId && endpoints.second == targetLastStopId
            }
        }
        
        if (filteredTrips.isEmpty()) {
            println("⚠️ fetchGTFSStopsForRoute: No trips found for routeId=$routeId, originalRouteId=$originalRouteId, targetDirectionId=$targetDirectionId, targetFirstStopId=$targetFirstStopId, targetLastStopId=$targetLastStopId, allTrips.count=${allTrips.size}")
            return@withContext emptyList()
        }
        
        // Step 2: Get stop_times for the trip (1st trip_id, or 0th if 1st doesn't exist) to get stop_id list
        // Use 1st trip_id if available, otherwise use 0th (to skip depot-only trips)
        val selectedTrip = filteredTrips[if (filteredTrips.size > 1) 1 else 0]
        
        // Process stop_times.txt again using streaming to get stop list for selected trip
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
        
        // Sort by stop_sequence
        tripStopTimes.sortBy { it.second }
        
        // Step 3: Get stops information from stops.txt
        // Create a dictionary of stop_id -> stop_name
        val stopsData = loadGTFSFile(extractedDir, "stops.txt")
        val stopsRows = parseGTFSCSV(stopsData)
        
        val stopsDict = mutableMapOf<String, String>()
        for (row in stopsRows) {
            val stopId = row["stop_id"]
            val stopName = row["stop_name"]
            if (stopId != null && stopName != null) {
                // Apply translation to stop name
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
        
        // Convert to TransportationStop models
        // First stop_id is departure stop, last is destination
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
                busstopPole = stopTime.first
            )
        }
    }
}

