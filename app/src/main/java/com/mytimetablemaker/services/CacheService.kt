package com.mytimetablemaker.services

import android.app.Application
import android.content.Context
import com.mytimetablemaker.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

// Local cache store for ODPT data and metadata.
// Provides persistence for offline access.
class CacheStore(context: Context) {
    private val cacheDir: File = File(context.cacheDir, "ODPTCache")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    // File path helper for cache entries.
    private fun path(fileName: String): File {
        return File(cacheDir, fileName)
    }
    
    // Load cached data from disk.
    fun loadData(fileName: String): ByteArray? {
        val url = path(fileName)
        return if (url.exists() && url.isFile) {
            try {
                val data = FileInputStream(url).use { input ->
                    input.readBytes()
                }
                data
            } catch (e: IOException) {
                android.util.Log.e("CacheStore", "loadData: IOException while loading from ${url.absolutePath}", e)
                null
            }
        } else {
            null
        }
    }
    
    // Save data with atomic write for integrity.
    fun saveData(data: ByteArray, fileName: String) {
        val url = path(fileName)
        try {
            // Atomic write: write temp then rename.
            val tempFile = File(url.parent, "${url.name}.tmp")
            FileOutputStream(tempFile).use { output ->
                output.write(data)
            }
            tempFile.renameTo(url)
        } catch (e: IOException) {
            android.util.Log.e("CacheStore", "saveData: IOException while saving to ${url.absolutePath}", e)
        }
    }
    
    // Directory helpers for cache folders.
    fun directoryPath(dirName: String): File {
        return File(cacheDir, dirName)
    }
    
    // Check if a cached directory exists.
    fun directoryExists(dirName: String): Boolean {
        val url = directoryPath(dirName)
        return url.exists() && url.isDirectory
    }
    
    // Copy a directory into cache.
    fun saveDirectory(sourceDir: File, dirName: String) {
        val destDir = directoryPath(dirName)
        
        // Remove existing directory if present.
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        
        // Create parent directory if needed.
        destDir.parentFile?.mkdirs()
        
        // Copy directory contents.
        sourceDir.copyRecursively(destDir, overwrite = true)
    }
    
    // Load cached directory path.
    fun loadDirectoryPath(dirName: String): File? {
        val url = directoryPath(dirName)
        return if (directoryExists(dirName)) url else null
    }
}

// Shared data manager for transportation line data.
// Uses cache to avoid repeated loading.
class SharedDataManager private constructor(private val application: Application) {
    
    // Singleton instance for app-wide data access.
    companion object {
        @Volatile
        private var INSTANCE: SharedDataManager? = null
        
        fun getInstance(application: Application): SharedDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedDataManager(application).also { INSTANCE = it }
            }
        }
    }
    
    // Observable state for UI updates.
    private val _isLoading = MutableStateFlow(false)

    // Internal state and services.
    private val initializedKinds = mutableSetOf<TransportationLineKind>()
    private val cache = CacheStore(application)
    private val odptService = ODPTDataService(application)
    // GTFS bootstrap is intentionally disabled for now.
    // To restore quickly, uncomment these lines and GTFS branches below.
    // private val gtfsService = GTFSDataService(application)
    // private val consumerKey: String = BuildConfig.ODPT_ACCESS_TOKEN
    // GTFS operators are currently excluded from cache bootstrap.
    // To resume GTFS bootstrap later, include GTFS operators in this selector.
    private fun activeOperatorsForKind(kind: TransportationLineKind): List<LocalDataSource> {
        return LocalDataSource.entries.filter { operator ->
            operator.transportationType() == kind.toTransportationKind() &&
                operator.apiType() != ODPTAPIType.GTFS
        }
    }
    
    // Get lines for a specific kind.
    // Loads only requested data for performance.
    suspend fun getLines(kind: TransportationLineKind, allowFetch: Boolean = true): List<TransportationLine> = withContext(Dispatchers.IO) {
        // Initialize only if needed for this kind.
        if (!initializedKinds.contains(kind)) {
            if (allowFetch) {
                ensureCacheForKind(kind)
            }
            initializedKinds.add(kind)
        }
        
        // Load only the requested kind from cache.
        val cacheLines = mutableListOf<TransportationLine>()
        
        val operators = activeOperatorsForKind(kind)
        
        // Process cached operator data for this kind.
        for (transportOperator in operators) {
            // GTFS bootstrap (disabled)
            // if (transportOperator.apiType() == ODPTAPIType.GTFS) {
            //     val date = GTFSDates.dateFor(transportOperator) ?: ""
            //     val gtfsFileName = transportOperator.gtfsFileName()
            //     val cacheKey = if (date.isEmpty()) "gtfs_${gtfsFileName}.zip" else "gtfs_${gtfsFileName}_${date}.zip"
            //     if (cache.loadData(cacheKey) == null) {
            //         val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
            //         if (gtfsURL.isNotEmpty()) {
            //             gtfsService.downloadGTFSZipOnly(gtfsURL, consumerKey, transportOperator)
            //         }
            //     }
            //     continue
            // }
            val cacheKey = transportOperator.fileName()
            val cachedData = cache.loadData(cacheKey) ?: continue
            
            val lines = if (kind == TransportationLineKind.RAILWAY) ODPTParser.parseRailwayRoutes(cachedData) else ODPTParser.parseBusRoutes(cachedData)
            
            cacheLines.addAll(lines)
        }
        
        return@withContext cacheLines
    }
    
    // Ensure cache exists for a specific kind.
    private suspend fun ensureCacheForKind(kind: TransportationLineKind) = withContext(Dispatchers.IO) {
        val operators = activeOperatorsForKind(kind)
        
        // Check if all operators for this kind have cache.
        val allHaveCache = operators.all { transportOperator ->
            // GTFS cache check (disabled)
            // if (transportOperator.apiType() == ODPTAPIType.GTFS) {
            //     val date = GTFSDates.dateFor(transportOperator) ?: ""
            //     val gtfsFileName = transportOperator.gtfsFileName()
            //     val cacheKey = if (date.isEmpty()) "gtfs_${gtfsFileName}.zip" else "gtfs_${gtfsFileName}_${date}.zip"
            //     return@all cache.loadData(cacheKey) != null
            // }
            cache.loadData(transportOperator.fileName()) != null
        }
        
        if (!allHaveCache) {
            // Missing caches trigger an initial fetch.
            performInitialFetch(kind)
        }
    }
    
    // Perform initial fetch for a specific kind.
    private suspend fun performInitialFetch(kind: TransportationLineKind) = withContext(Dispatchers.IO) {
        val operators = activeOperatorsForKind(kind)
        
        for (transportOperator in operators) {
            // Fetch data.
            try {
                // GTFS initial fetch (disabled)
                // if (transportOperator.apiType() == ODPTAPIType.GTFS) {
                //     val gtfsURL = transportOperator.apiLink(APIDataType.LINE, TransportationKind.BUS)
                //     if (gtfsURL.isNotEmpty()) {
                //         gtfsService.downloadGTFSZipOnly(gtfsURL, consumerKey, transportOperator)
                //     }
                //     continue
                // }
                // Check cache for active operators.
                val cacheKey = transportOperator.fileName()
                val hasCache = cache.loadData(cacheKey) != null
                
                if (hasCache) {
                    continue
                }
                
                val data = odptService.fetchIndividualOperatorData(transportOperator)
                
                // Save to cache and LineData for future use.
                cache.saveData(data, cacheKey)
                odptService.writeIndividualOperatorDataToFile(data, transportOperator)
            } catch (e: Exception) {
                    android.util.Log.e("SharedDataManager", "Failed to initialize ${transportOperator.operatorDisplayName(application)}", e)
            }
        }
    }
    
    // Check if any cache exists to decide on fetching.
    // Presence indicates at least one prior fetch.
    fun checkCacheAvailability(): Boolean {
        return LocalDataSource.entries.any { transportOperator ->
            val cacheKey = transportOperator.fileName()
            cache.loadData(cacheKey) != null
        }
    }
    
    // Splash initialization for loading and update checks.
    // `isLoading` should be true before calling.
    suspend fun performSplashInitialization() = withContext(Dispatchers.IO) {
        
        // Check if any cache exists.
        val hasAnyCache = checkCacheAvailability()
        
        if (!hasAnyCache) {
            // No cache: fetch all operators from API.
            try {
                performInitialFetch(TransportationLineKind.RAILWAY)
                performInitialFetch(TransportationLineKind.BUS)
            } catch (e: Exception) {
                    android.util.Log.e("SharedDataManager", "Initial fetch failed", e)
            }
        } else {
            // Cache exists: preload into memory for faster access.
            try {
                getLines(TransportationLineKind.RAILWAY, allowFetch = false)
                getLines(TransportationLineKind.BUS, allowFetch = false)
            } catch (e: Exception) {
                    android.util.Log.e("SharedDataManager", "Cache load failed", e)
            }
        }
        
        // Ensure isLoading is false after completion.
        _isLoading.value = false
        
        // Small delay to flush log statements.
        delay(500)
    }

    // Convert TransportationLineKind to TransportationKind.
    private fun TransportationLineKind.toTransportationKind(): TransportationKind {
        return when (this) {
            TransportationLineKind.RAILWAY -> TransportationKind.RAILWAY
            TransportationLineKind.BUS -> TransportationKind.BUS
        }
    }
}
