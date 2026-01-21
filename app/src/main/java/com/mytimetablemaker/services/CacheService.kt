package com.mytimetablemaker.services

import android.content.Context
import android.content.SharedPreferences
import com.mytimetablemaker.models.*
import com.mytimetablemaker.models.APIDataType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

// MARK: - Cache Store
// Handles local storage of ODPT data and metadata
// Provides efficient data persistence and retrieval for offline access
class CacheStore(private val context: Context) {
    private val cacheDir: File
    
    init {
        cacheDir = File(context.cacheDir, "ODPTCache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    // MARK: - File Path Management
    // Get file path for a given filename in the cache directory
    private fun path(fileName: String): File {
        return File(cacheDir, fileName)
    }
    
    // MARK: - Data Operations
    // Load cached data from file system
    fun loadData(fileName: String): ByteArray? {
        val url = path(fileName)
        android.util.Log.d("CacheStore", "loadData: Checking ${url.absolutePath}, exists=${url.exists()}, isFile=${url.isFile}, size=${if (url.exists()) url.length() else 0}")
        return if (url.exists() && url.isFile) {
            try {
                val data = FileInputStream(url).use { input ->
                    input.readBytes()
                }
                android.util.Log.d("CacheStore", "loadData: Successfully loaded ${data.size} bytes from ${url.absolutePath}")
                data
            } catch (e: IOException) {
                android.util.Log.e("CacheStore", "loadData: IOException while loading from ${url.absolutePath}: ${e.message}", e)
                null
            }
        } else {
            android.util.Log.d("CacheStore", "loadData: File does not exist or is not a file: ${url.absolutePath}")
            null
        }
    }
    
    // Save data to cache with atomic write for data integrity
    fun saveData(data: ByteArray, fileName: String) {
        val url = path(fileName)
        try {
            android.util.Log.d("CacheStore", "saveData: Saving ${data.size} bytes to ${url.absolutePath}")
            // Atomic write: write to temporary file first, then rename
            val tempFile = File(url.parent, "${url.name}.tmp")
            FileOutputStream(tempFile).use { output ->
                output.write(data)
            }
            val renamed = tempFile.renameTo(url)
            if (renamed) {
                android.util.Log.d("CacheStore", "saveData: Successfully saved data to ${url.absolutePath}, file exists=${url.exists()}, file size=${url.length()}")
            } else {
                android.util.Log.e("CacheStore", "saveData: Failed to rename temp file to ${url.absolutePath}")
            }
        } catch (e: IOException) {
            android.util.Log.e("CacheStore", "saveData: IOException while saving to ${url.absolutePath}: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    // MARK: - Directory Operations
    // Get directory path for a given directory name in the cache directory
    fun directoryPath(dirName: String): File {
        return File(cacheDir, dirName)
    }
    
    // Check if cached directory exists
    fun directoryExists(dirName: String): Boolean {
        val url = directoryPath(dirName)
        return url.exists() && url.isDirectory
    }
    
    // Copy directory to cache
    fun saveDirectory(sourceDir: File, dirName: String) {
        val destDir = directoryPath(dirName)
        
        // Remove existing directory if it exists
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        
        // Create parent directory if needed
        destDir.parentFile?.mkdirs()
        
        // Copy directory
        sourceDir.copyRecursively(destDir, overwrite = true)
    }
    
    // Load cached directory path
    fun loadDirectoryPath(dirName: String): File? {
        val url = directoryPath(dirName)
        return if (directoryExists(dirName)) {
            url
        } else {
            null
        }
    }
}

// MARK: - Shared Data Manager
// Singleton service for managing transportation line data across the app
// Implements shared cache to avoid repeated loading and improve performance
class SharedDataManager private constructor(private val context: Context) {
    
    // MARK: - Singleton Instance
    // Shared instance for app-wide data access
    companion object {
        @Volatile
        private var INSTANCE: SharedDataManager? = null
        
        fun getInstance(context: Context): SharedDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedDataManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // MARK: - Published Properties (StateFlow)
    // Observable properties for UI updates
    private val _allLines = MutableStateFlow<List<TransportationLine>>(emptyList())
    val allLines: StateFlow<List<TransportationLine>> = _allLines.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _lastUpdated = MutableStateFlow<Date?>(null)
    val lastUpdated: StateFlow<Date?> = _lastUpdated.asStateFlow()
    
    // MARK: - Private Properties
    // Internal state management
    private var isInitialized = false
    private var initializationJob: Job? = null
    private val initializedKinds = mutableSetOf<TransportationLineKind>()
    private val cache = CacheStore(context)
    private val odptService = ODPTDataService(context)
    private val gtfsService = GTFSDataService(context)
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("SharedDataManager", Context.MODE_PRIVATE)
    private val consumerKey: String = "" // TODO: Get from BuildConfig or secure storage
    
    // MARK: - Data Access
    // Get lines for a specific transportation kind
    // Load only data for the requested kind to improve performance
    suspend fun getLines(kind: TransportationLineKind, allowFetch: Boolean = true): List<TransportationLine> = withContext(Dispatchers.IO) {
        // Initialize only if needed for this kind
        if (!initializedKinds.contains(kind)) {
            if (allowFetch) {
                ensureCacheForKind(kind)
            }
            initializedKinds.add(kind)
        }
        
        // Load only the requested kind from cache
        val cacheLines = mutableListOf<TransportationLine>()
        
        val operators = LocalDataSource.values().filter { it.transportationType() == kind.toTransportationKind() }
        
        // Process each operator's cached data and parse into transportation lines
        // Filter by kind to ensure only relevant data is loaded
        for (transportOperator in operators) {
            // Handle GTFS operators separately
            // For GTFS, don't fetch lines at startup - only ensure ZIP cache exists
            // Lines will be fetched lazily when user selects the operator
            if (transportOperator.apiType() == ODPTAPIType.GTFS) {
                // Check if ZIP cache exists, download if not (but don't extract)
                val date = GTFSDates.dateFor(transportOperator) ?: ""
                val gtfsFileName = transportOperator.gtfsFileName()
                val cacheKey = if (date.isEmpty()) "gtfs_${gtfsFileName}.zip" else "gtfs_${gtfsFileName}_${date}.zip"
                
                if (cache.loadData(cacheKey) == null) {
                    // Download ZIP file for caching (without extracting)
                    try {
                        val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
                        if (gtfsURL.isNotEmpty()) {
                            gtfsService.downloadGTFSZipOnly(gtfsURL, consumerKey, transportOperator)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("SharedDataManager", "⚠️ Failed to download GTFS ZIP for ${transportOperator.operatorDisplayName(context)}: ${e.message}")
                    }
                }
                // Don't fetch lines at startup - return empty array
                // Lines will be fetched when user selects this operator
                continue
            }
            
            val cacheKey = transportOperator.fileName()
            val cachedData = cache.loadData(cacheKey) ?: continue
            
            val lines = if (kind == TransportationLineKind.RAILWAY) {
                ODPTParser.parseRailwayRoutes(cachedData)
            } else {
                ODPTParser.parseBusRoutes(cachedData)
            }
            
            cacheLines.addAll(lines)
        }
        
        return@withContext cacheLines
    }
    
    // Ensure cache exists for a specific kind
    private suspend fun ensureCacheForKind(kind: TransportationLineKind) = withContext(Dispatchers.IO) {
        val operators = LocalDataSource.values().filter { it.transportationType() == kind.toTransportationKind() }
        
        // Check if all operators for this kind have cache
        val allHaveCache = operators.all { transportOperator ->
            // For GTFS operators, check GTFS cache key instead of fileName
            if (transportOperator.apiType() == ODPTAPIType.GTFS) {
                val date = GTFSDates.dateFor(transportOperator) ?: ""
                val gtfsFileName = transportOperator.gtfsFileName()
                val cacheKey = if (date.isEmpty()) "gtfs_${gtfsFileName}.zip" else "gtfs_${gtfsFileName}_${date}.zip"
                cache.loadData(cacheKey) != null
            } else {
                cache.loadData(transportOperator.fileName()) != null
            }
        }
        
        if (!allHaveCache) {
            // Some caches are missing, fetch them
            performInitialFetch(kind)
        }
    }
    
    // Perform initial fetch for a specific kind only (without saving to cache)
    private suspend fun performInitialFetch(kind: TransportationLineKind) = withContext(Dispatchers.IO) {
        val operators = LocalDataSource.values().filter { it.transportationType() == kind.toTransportationKind() }
        
        for (transportOperator in operators) {
            // Fetch data
            try {
                // Handle GTFS operators separately
                if (transportOperator.apiType() == ODPTAPIType.GTFS) {
                    // For GTFS, download ZIP file and extract for caching at startup
                    // Check for updates: date-based operators check date, Toei Bus uses ETag/Last-Modified
                    val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
                    if (gtfsURL.isNotEmpty()) {
                        // downloadGTFSZip() handles:
                        // - Date-based operators: cache key includes date, so date change = new cache key = new download
                        // - Toei Bus: checks for updates using ETag/Last-Modified via checkForToeiBusGTFSUpdate()
                        gtfsService.downloadGTFSZipOnly(gtfsURL, consumerKey, transportOperator)
                        android.util.Log.d("SharedDataManager", "✅ Downloaded and extracted GTFS ZIP for caching: ${transportOperator.operatorDisplayName(context)}")
                    }
                } else {
                    // Check if cache exists for non-GTFS operators
                    val cacheKey = transportOperator.fileName()
                    val hasCache = cache.loadData(cacheKey) != null
                    
                    if (hasCache) {
                        continue
                    }
                    
                    val data = odptService.fetchIndividualOperatorData(transportOperator, consumerKey)
                    
                    // Write to file
                    odptService.writeIndividualOperatorDataToFile(data, transportOperator)
                    
                    // Don't save to cache here - only load from cache
                    // Cache will be saved when user presses save button
                    
                    android.util.Log.d("SharedDataManager", "✅ Fetched: ${transportOperator.operatorDisplayName(context)}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SharedDataManager", "❌ Failed to initialize ${transportOperator.operatorDisplayName(context)}: ${e.message}")
            }
        }
    }
    
    // MARK: - Railway Update
    // Perform railway data update (used for both auto and manual updates)
    suspend fun performRailwayUpdate() = withContext(Dispatchers.IO) {
        // TODO: Implement full railway update logic
        android.util.Log.d("SharedDataManager", "🔄 Performing railway update...")
    }
    
    // MARK: - Bus Update
    // Perform bus data update (used for both auto and manual updates)
    suspend fun performBusUpdate() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        
        // TODO: Implement full bus update logic
        android.util.Log.d("SharedDataManager", "🔄 Performing bus update...")
        
        _isLoading.value = false
    }
    
    // MARK: - Cache Availability Check
    // Check if any cache exists to determine if we need to fetch data
    // Cache existence indicates that data has been fetched at least once
    fun checkCacheAvailability(): Boolean {
        return LocalDataSource.values().any { transportOperator ->
            val cacheKey = transportOperator.fileName()
            cache.loadData(cacheKey) != null
        }
    }
    
    // MARK: - Splash Initialization
    // Perform complete initialization for splash screen
    // Handles data loading, fetching, and update checks
    // Note: isLoading should be set to true before calling this method
    suspend fun performSplashInitialization() = withContext(Dispatchers.IO) {
        android.util.Log.d("SharedDataManager", "🔄 Starting data initialization...")
        
        // Check if we have any cache (indicates data has been fetched at least once)
        val hasAnyCache = checkCacheAvailability()
        
        if (!hasAnyCache) {
            // No cache exists: fetch all operators' data
            android.util.Log.d("SharedDataManager", "📥 No cache found - fetching all operators' data")
            // TODO: Implement performInitialFetch()
        } else {
            // Cache exists: load from cache (data already fetched)
            android.util.Log.d("SharedDataManager", "📂 Cache found - loading from cache")
            // TODO: Implement loadFromCache()
        }
        
        // Ensure isLoading is false after all operations complete
        _isLoading.value = false
        
        // Small delay to ensure all log statements complete
        delay(500)
    }
    
    // MARK: - Helper Functions
    // Convert TransportationLineKind to TransportationKind
    private fun TransportationLineKind.toTransportationKind(): TransportationKind {
        return when (this) {
            TransportationLineKind.RAILWAY -> TransportationKind.RAILWAY
            TransportationLineKind.BUS -> TransportationKind.BUS
        }
    }
}
