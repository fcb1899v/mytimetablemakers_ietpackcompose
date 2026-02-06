package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.models.*
import com.mytimetablemaker.services.GTFSDataService
import com.mytimetablemaker.services.ODPTDataService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import com.mytimetablemaker.BuildConfig
import com.mytimetablemaker.R

// MARK: - Settings Line ViewModel Interface
// Interface for accessing SettingsLineViewModel properties
// This allows SettingsTimetableViewModel to access line view model properties without creating a retain cycle
interface SettingsLineViewModelInterface {
    val goorback: String
    val lineIndex: Int
    val selectedLine: TransportationLine?
    val selectedDepartureStop: TransportationStop?
    val selectedArrivalStop: TransportationStop?
    val selectedRideTime: Int
    val lineStops: List<TransportationStop>
    val lineSuggestions: List<TransportationLine>
    val selectedGoorback: String
    val selectedLineNumber: Int
    val isAllNotEmpty: Boolean
    var isLoadingTimetable: Boolean
    var loadingMessage: String?
    
    fun saveOperatorLineList(lines: List<TransportationLine>, goorback: String, num: Int)
    fun saveLineStopList(stops: List<TransportationStop>, goorback: String, num: Int)
    suspend fun saveAllDataToUserDefaults()
}

// MARK: - Settings Timetable ViewModel
// ViewModel for timetable generation and management.
// Handles timetable data fetching, processing, and saving for both railway and bus routes.
// Extracted from SettingsLineViewModel to improve code organization and maintainability.
class SettingsTimetableViewModel(
    application: Application,
    private val sharedPreferences: SharedPreferences,
    private val lineViewModel: SettingsLineViewModelInterface? = null
) : AndroidViewModel(application) {
    
    // MARK: - Published Properties
    // Loading state for timetable generation
    private val _isLoadingTimetable = MutableStateFlow(false)
    val isLoadingTimetable: StateFlow<Boolean> = _isLoadingTimetable.asStateFlow()
    
    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()
    
    // MARK: - Configuration Properties
    private val gtfsService = GTFSDataService(application)
    private val odptService = ODPTDataService(application)
    private val consumerKey: String
    private val challengeKey: String
    
    init {
        // Get ODPT access token and challenge token from BuildConfig
        consumerKey = BuildConfig.ODPT_ACCESS_TOKEN
        challengeKey = BuildConfig.ODPT_CHALLENGE_TOKEN
        android.util.Log.d("SettingsTimetableViewModel", "Initialized: consumerKey length=${consumerKey.length}, challengeKey length=${challengeKey.length}")
    }
    
    // MARK: - Helper Properties
    // Access line view model properties through computed properties
    private val goorback: String
        get() = lineViewModel?.goorback ?: "back1"
    
    private val lineIndex: Int
        get() = lineViewModel?.lineIndex ?: 0
    
    private val selectedLine: TransportationLine?
        get() = lineViewModel?.selectedLine
    
    private val selectedDepartureStop: TransportationStop?
        get() = lineViewModel?.selectedDepartureStop
    
    private val selectedArrivalStop: TransportationStop?
        get() = lineViewModel?.selectedArrivalStop
    
    private val selectedRideTime: Int
        get() = lineViewModel?.selectedRideTime ?: 0
    
    private val lineStops: List<TransportationStop>
        get() = lineViewModel?.lineStops ?: emptyList()
    
    private val lineSuggestions: List<TransportationLine>
        get() = lineViewModel?.lineSuggestions ?: emptyList()
    
    private val selectedGoorback: String
        get() = lineViewModel?.selectedGoorback ?: "back1"
    
    private val selectedLineNumber: Int
        get() = lineViewModel?.selectedLineNumber ?: 1
    
    private val isAllNotEmpty: Boolean
        get() = lineViewModel?.isAllNotEmpty ?: false
    
    // MARK: - Data Clearing
    // Clear timetable data for current route and line number for all calendar types
    suspend fun clearAllTimetableData() {
        withContext(Dispatchers.IO) {
            // Clear data for all calendar types for current route and line number
            for (calendarType in ODPTCalendarType.allCases) {
                clearTimetableDataForRoute(calendarType, goorback, lineIndex + 1)
            }
            
            // Clear cached calendar types to force refresh
            selectedLine?.let { line ->
                val cacheKey = "${line.code}_${line.kind.name}_calendarTypes"
                sharedPreferences.edit { remove(cacheKey) }
                
                // Clear line-level cache for current route and line only
                val lineCacheKey = "${goorback}line${lineIndex + 1}_calendarTypes"
                sharedPreferences.edit { remove(lineCacheKey) }
            }
        }
    }
    
    // MARK: - Unified Timetable Data Processing
    // Get timetable data and extract departure/arrival times for selected stops/stations
    suspend fun getTimeTableData(): Map<ODPTCalendarType, List<TransportationTime>> {
        // Skip timetable generation if not all required fields are filled
        if (!isAllNotEmpty || selectedLine == null) {
            return emptyMap()
        }
        
        _isLoadingTimetable.value = true
        val loadingText = getApplication<Application>().getString(R.string.generatingTimetable)
        _loadingMessage.value = loadingText
        lineViewModel?.isLoadingTimetable = true
        lineViewModel?.loadingMessage = loadingText
        
        try {
            // Clear existing timetable data for all calendar types before generating new data
            clearAllTimetableData()
            
            // Get available calendar types for this line
            val availableCalendarTypes = getAvailableCalendarTypes()
            
            // Process data for each calendar type first (create timetables individually)
            val allTimes = mutableMapOf<ODPTCalendarType, List<TransportationTime>>()
            
            // For GTFS routes, fetch all calendar types at once for better performance
            val selectedLine = this.selectedLine
            if (selectedLine?.kind == TransportationLineKind.BUS) {
                val operatorCode = selectedLine.operatorCode
                val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
                if (selectedOperator?.apiType() == ODPTAPIType.GTFS) {
                    val departureStop = selectedDepartureStop
                    val arrivalStop = selectedArrivalStop
                    if (departureStop != null && arrivalStop != null) {
                        // Fetch all calendar types at once
                        try {
                        // TODO: Implement fetchGTFSBusTimetableForAllCalendarTypes in GTFSDataService
                        // For now, fallback to individual processing
                        for (calendarType in availableCalendarTypes) {
                            val times = processBusTimetableData(calendarType)
                            allTimes[calendarType] = times
                        }
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsTimetableViewModel", "Failed to fetch GTFS bus timetable for all calendar types: ${e.message}")
                            // Fallback to individual processing
                            for (calendarType in availableCalendarTypes) {
                                val times = processBusTimetableData(calendarType)
                                allTimes[calendarType] = times
                            }
                        }
                    }
                } else {
                    // For non-GTFS routes, process each calendar type individually
                    for (calendarType in availableCalendarTypes) {
                        val times = processBusTimetableData(calendarType)
                        allTimes[calendarType] = times
                    }
                }
            } else {
                // For railway routes, process each calendar type individually
                for (calendarType in availableCalendarTypes) {
                    val times = processTrainTimetableData(calendarType)
                    allTimes[calendarType] = times
                }
            }
            
            // After creating all timetables, check and merge timetables with same displayCalendarType
            
            // Group calendar types by displayCalendarType
            val groupedByDisplayType = mutableMapOf<ODPTCalendarType, MutableList<ODPTCalendarType>>()
            for (calendarType in availableCalendarTypes) {
                val displayType = calendarType.displayCalendarType()
                if (!groupedByDisplayType.containsKey(displayType)) {
                    groupedByDisplayType[displayType] = mutableListOf()
                }
                groupedByDisplayType[displayType]?.add(calendarType)
            }
            
            val mergedTimes = mutableMapOf<ODPTCalendarType, List<TransportationTime>>()
            val mergedSourceTypes = mutableSetOf<ODPTCalendarType>() // Track all merged source calendar types (including representative) to clear later
            
            // Process each display calendar type group
            for ((displayType, calendarTypes) in groupedByDisplayType) {
                if (calendarTypes.size > 1) {
                    // Multiple calendar types with same display type - merge them
                    
                    val mergedTimeList = mutableListOf<TransportationTime>()
                    
                    // Use the first calendar type (prefer .specific if available) as the representative
                    val representativeCalendarType = calendarTypes.firstOrNull { it is ODPTCalendarType.Specific }
                        ?: calendarTypes.firstOrNull() ?: displayType
                    
                    for (typeToMerge in calendarTypes) {
                        allTimes[typeToMerge]?.let { times ->
                            mergedTimeList.addAll(times)
                        }
                    }
                    
                    // Remove duplicates and sort by departure time
                    val sortedMergedTimeList = mergedTimeList.mergeAndSortTransportationTimes()
                    
                    // Save merged timetable under representative calendar type
                    mergedTimes[representativeCalendarType] = sortedMergedTimeList
                    
                    // Track ALL merged source types (including representative) for cleanup
                    // We need to clear all of them, including the representative, because it might have old data
                    for (typeToDelete in calendarTypes) {
                        allTimes.remove(typeToDelete)
                        mergedSourceTypes.add(typeToDelete)
                    }
                } else {
                    // Only one calendar type with this display type - keep as is
                    calendarTypes.firstOrNull()?.let { calendarType ->
                        allTimes[calendarType]?.let { times ->
                            mergedTimes[calendarType] = times
                            allTimes.remove(calendarType)
                        }
                    }
                }
            }
            
            // Clear ALL merged source calendar types from SharedPreferences (including representatives)
            // This ensures old data is removed before new merged data is saved
            // IMPORTANT: Use initialized goorback and lineIndex to prevent data corruption across routes
            if (mergedSourceTypes.isNotEmpty()) {
                for (sourceType in mergedSourceTypes) {
                    // Double-check that we're clearing for the correct route and line number
                    clearTimetableDataForRoute(sourceType, goorback, lineIndex + 1)
                }
            }
            
            // Save calendar types list from ODPT API (all types returned by API)
            // When this list has types: display all in UI; when empty: display 3 standard types
            val lineCacheKey = "${goorback}line${lineIndex + 1}_calendarTypes"
            val typeStrings = availableCalendarTypes.map { it.rawValue }
            sharedPreferences.edit { putStringSet(lineCacheKey, typeStrings.toSet()) }
            
            // Save operator line list and line stop list to SharedPreferences after timetable generation
            val saveLineIndex = selectedLineNumber - 1
            if (lineSuggestions.isNotEmpty()) {
                lineViewModel?.saveOperatorLineList(lineSuggestions, goorback, saveLineIndex)
            }
            if (lineStops.isNotEmpty()) {
                lineViewModel?.saveLineStopList(lineStops, goorback, saveLineIndex)
            }
            
            return mergedTimes
        } finally {
            _isLoadingTimetable.value = false
            _loadingMessage.value = null
            lineViewModel?.isLoadingTimetable = false
            lineViewModel?.loadingMessage = null
        }
    }
    
    // MARK: - Available Calendar Types Detection
    // Get available calendar types for the selected line by fetching from timetable API
    private suspend fun getAvailableCalendarTypes(): List<ODPTCalendarType> {
        val selectedLine = this.selectedLine ?: return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        val operatorCode = selectedLine.operatorCode ?: return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        
        // Check cache first
        val cacheKey = "${selectedLine.code}_${selectedLine.kind.name}_calendarTypes"
        val cachedTypes = sharedPreferences.getStringSet(cacheKey, null)
        if (cachedTypes != null && cachedTypes.isNotEmpty()) {
            val cachedCalendarTypes = cachedTypes.mapNotNull { ODPTCalendarType.fromRawValue(it) }
            if (cachedCalendarTypes.isNotEmpty()) {
                android.util.Log.d("SettingsTimetableViewModel", "Found cached calendar types: ${cachedCalendarTypes.joinToString { it.displayName(getApplication()) }}")
                return cachedCalendarTypes
            }
        }
        
        // Fetch available calendar types from timetable API
        val availableTypes = fetchAvailableCalendarTypes(selectedOperator)
        
        // Cache the results
        val typeStrings = availableTypes.map { it.rawValue }
        sharedPreferences.edit { putStringSet(cacheKey, typeStrings.toSet()) }
        
        // Cache at line level (each line has its own calendar types list)
        // Structure: goorback -> line -> calendar types -> timetable data
        val lineCacheKey = "${goorback}line${lineIndex + 1}_calendarTypes"
        sharedPreferences.edit { putStringSet(lineCacheKey, typeStrings.toSet()) }
        
        // Ensure we have at least weekday and saturdayHoliday as fallback
        if (availableTypes.isEmpty()) {
            android.util.Log.d("SettingsTimetableViewModel", "No calendar types found, using fallback: [weekday, saturdayHoliday]")
            return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        }
        
        android.util.Log.d("SettingsTimetableViewModel", "Found calendar types: ${availableTypes.joinToString { it.displayName(getApplication()) }}")
        return availableTypes
    }
    
    // Fetch available calendar types from timetable API
    // Uses BusTimetable for bus, StationTimetable for railway
    private suspend fun fetchAvailableCalendarTypes(dataSource: LocalDataSource): List<ODPTCalendarType> {
        val selectedLine = this.selectedLine ?: return emptyList()
        
        val apiTypeName = if (selectedLine.kind == TransportationLineKind.BUS) "BusTimetable" else "StationTimetable"
        
        // Check if this is a GTFS route - GTFS routes don't use ODPT API
        if (dataSource.apiType() == ODPTAPIType.GTFS) {
            // For GTFS routes, fetch calendar types from GTFS data for the specific route
            val routeId = selectedLine.code
            
            if (routeId.isEmpty()) {
                android.util.Log.w("SettingsTimetableViewModel", "GTFS: Missing routeId for calendar type fetch")
                return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
            }
            
            try {
                // TODO: Implement fetchGTFSCalendarTypes in GTFSDataService
                // For now, return default calendar types
                val calendarTypes = listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
                android.util.Log.d("SettingsTimetableViewModel", "GTFS Calendar Types: ${calendarTypes.joinToString { it.displayName(getApplication()) }}")
                return calendarTypes
            } catch (e: Exception) {
                android.util.Log.e("SettingsTimetableViewModel", "Failed to fetch GTFS calendar types: ${e.message}")
                // Fallback to default calendar types
                return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
            }
        }
        
        // For bus, use title-based API link
        if (selectedLine.kind == TransportationLineKind.BUS) {
            val apiLink = selectedLine.title?.let { title ->
                "${dataSource.apiLink(APIDataType.TIMETABLE, TransportationKind.BUS)}&dc:title=$title"
            }
            
            if (apiLink == null) {
                android.util.Log.w("SettingsTimetableViewModel", "$apiTypeName: Missing required information")
                return emptyList()
            }
            
            android.util.Log.d("SettingsTimetableViewModel", "Fetch URL: $apiLink")
            
            try {
                // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
                val authKey = if (dataSource.apiType() == ODPTAPIType.CHALLENGE) {
                    challengeKey
                } else {
                    consumerKey
                }
                val (data, response) = odptService.fetchODPTDataWithAuth(apiLink, authKey)
                
                if (response.code != 200) {
                    android.util.Log.e("SettingsTimetableViewModel", "Failed to fetch calendar types - status: ${response.code}")
                    return emptyList()
                }
                
                val json = odptService.parseJSONArray(data)
                
                // Extract unique calendar types from the response
                val foundCalendarTypes = mutableSetOf<String>()
                
                for (timetable in json) {
                    val calendar = timetable["odpt:calendar"] as? String
                    if (calendar != null) {
                        foundCalendarTypes.add(calendar)
                    }
                }
                
                // Convert to ODPTCalendarType array and process
                return processCalendarTypes(foundCalendarTypes, apiTypeName)
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsTimetableViewModel", "Error fetching calendar types from $apiTypeName: ${e.message}")
                return emptyList()
            }
        }
        
        // For railway, try both directions (ascending and descending)
        val stationCode = selectedDepartureStop?.code ?: return emptyList()
        
        // Get available directions with fallback
        val actualDirection = selectedLine.lineDirection ?: ""
        val ascendingDirection = selectedLine.ascendingRailDirection ?: actualDirection
        val descendingDirection = selectedLine.descendingRailDirection ?: actualDirection
        
        // Try both directions and collect calendar types from both
        val allCalendarTypes = mutableSetOf<String>()
        var ascendingSuccess = false
        
        // Try ascending direction first
        if (ascendingDirection.isNotEmpty()) {
            val ascendingLink = "${dataSource.apiLink(APIDataType.STOP_TIMETABLE)}&odpt:station=$stationCode&odpt:railDirection=$ascendingDirection"
            
            android.util.Log.d("SettingsTimetableViewModel", "Fetch URL (ascending): $ascendingLink")
            
            try {
                // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
                val authKey = if (dataSource.apiType() == ODPTAPIType.CHALLENGE) {
                    challengeKey
                } else {
                    consumerKey
                }
                val (data, response) = odptService.fetchODPTDataWithAuth(ascendingLink, authKey)
                
                if (response.code == 200) {
                    val json = odptService.parseJSONArray(data)
                    
                    for (timetable in json) {
                        val calendar = timetable["odpt:calendar"] as? String
                        if (calendar != null) {
                            allCalendarTypes.add(calendar)
                        }
                    }
                    ascendingSuccess = true
                    android.util.Log.d("SettingsTimetableViewModel", "Successfully fetched calendar types from ascending direction")
                } else {
                    android.util.Log.w("SettingsTimetableViewModel", "Failed to fetch calendar types from ascending direction - status: ${response.code}")
                }
            } catch (e: Exception) {
                android.util.Log.w("SettingsTimetableViewModel", "Error fetching calendar types from ascending direction: ${e.message}")
            }
        }
        
        // Try descending direction only if ascending failed or if it's a different direction
        if (!ascendingSuccess && descendingDirection.isNotEmpty() && descendingDirection != ascendingDirection) {
            val descendingLink = "${dataSource.apiLink(APIDataType.STOP_TIMETABLE)}&odpt:station=$stationCode&odpt:railDirection=$descendingDirection"
            
            android.util.Log.d("SettingsTimetableViewModel", "Fetch URL (descending): $descendingLink")
            
            try {
                // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
                val authKey = if (dataSource.apiType() == ODPTAPIType.CHALLENGE) {
                    challengeKey
                } else {
                    consumerKey
                }
                val (data, response) = odptService.fetchODPTDataWithAuth(descendingLink, authKey)
                
                if (response.code == 200) {
                    val json = odptService.parseJSONArray(data)
                    
                    for (timetable in json) {
                        val calendar = timetable["odpt:calendar"] as? String
                        if (calendar != null) {
                            allCalendarTypes.add(calendar)
                        }
                    }
                    android.util.Log.d("SettingsTimetableViewModel", "Successfully fetched calendar types from descending direction")
                } else {
                    android.util.Log.w("SettingsTimetableViewModel", "Failed to fetch calendar types from descending direction - status: ${response.code}")
                }
            } catch (e: Exception) {
                android.util.Log.w("SettingsTimetableViewModel", "Error fetching calendar types from descending direction: ${e.message}")
            }
        }
        
        // Convert to ODPTCalendarType array and process
        return processCalendarTypes(allCalendarTypes, apiTypeName)
    }
    
    // Process calendar types: remove duplicates and preserve .specific types
    private fun processCalendarTypes(foundCalendarTypes: Set<String>, apiTypeName: String): List<ODPTCalendarType> {
        // Convert to ODPTCalendarType array
        val allTypes = foundCalendarTypes.mapNotNull { ODPTCalendarType.fromRawValue(it) }
        
        // Remove duplicates based on displayCalendarType while preserving .specific types
        val uniqueTypes = mutableListOf<ODPTCalendarType>()
        val seenDisplayTypes = mutableSetOf<ODPTCalendarType>()
        
        // First pass: Add all .specific types
        for (type in allTypes) {
            if (type is ODPTCalendarType.Specific) {
                uniqueTypes.add(type)
                seenDisplayTypes.add(type.displayCalendarType())
            }
        }
        
        // Second pass: Add standard types only if their display type hasn't been seen
        for (type in allTypes) {
            if (type is ODPTCalendarType.Specific) {
                continue // Already added
            }
            val displayType = type.displayCalendarType()
            if (!seenDisplayTypes.contains(displayType)) {
                uniqueTypes.add(type)
                seenDisplayTypes.add(displayType)
            }
        }
        
        val result = uniqueTypes.sortedBy { it.rawValue }
        
        if (result.isNotEmpty()) {
            android.util.Log.d("SettingsTimetableViewModel", "$apiTypeName: Found calendar types: ${result.joinToString { it.displayName(getApplication()) }}")
        } else {
            android.util.Log.w("SettingsTimetableViewModel", "$apiTypeName: No calendar types found")
        }
        
        return result
    }
    
    // MARK: - Timetable Data Processing
    // Process bus timetable data for specific day type
    private suspend fun processBusTimetableData(calendarType: ODPTCalendarType): List<TransportationTime> {
        // Check if this is a GTFS route
        val selectedLine = this.selectedLine ?: return emptyList()
        val operatorCode = selectedLine.operatorCode ?: return emptyList()
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return emptyList()
        
        // For GTFS routes, fetch timetable data from GTFS files
        if (selectedOperator.apiType() == ODPTAPIType.GTFS) {
            val departureStop = selectedDepartureStop ?: return emptyList()
            val arrivalStop = selectedArrivalStop ?: return emptyList()
            
            try {
                // TODO: Implement fetchGTFSBusTimetable in GTFSDataService
                // For now, return empty list
                val busTimes = emptyList<TransportationTime>()
                android.util.Log.d("SettingsTimetableViewModel", "Fetched ${busTimes.size} GTFS bus times for ${calendarType.displayName(getApplication())}")
                return busTimes
            } catch (e: Exception) {
                android.util.Log.e("SettingsTimetableViewModel", "Failed to fetch GTFS bus timetable: ${e.message}")
                return emptyList()
            }
        }
        
        // For ODPT routes, use existing API-based processing
        // Fetch bus timetable data from API
        val busTimetableData = fetchBusTimetableData(calendarType)
        
        // Extract bus information and timetable objects in a single loop
        val transportationTimes = mutableListOf<TransportationTime>()
        
        for (timetable in busTimetableData) {
            val busTimetableObjects = timetable["odpt:busTimetableObject"] as? List<*>
                ?: continue
            
            var departureTime: String? = null
            var arrivalTime: String? = null
            
            for (timetableObject in busTimetableObjects) {
                val timetableObj = timetableObject as? Map<*, *> ?: continue
                val currentBusstopPole = timetableObj["odpt:busstopPole"] as? String ?: ""
                val currentDepartureTime = timetableObj["odpt:departureTime"] as? String ?: ""
                val currentArrivalTime = timetableObj["odpt:arrivalTime"] as? String ?: ""
                
                // Check departure stop match using busstopPole
                val selectedDepartureStop = this.selectedDepartureStop
                if (selectedDepartureStop != null) {
                    val departureStop = selectedDepartureStop.busstopPole
                    if (departureStop != null && currentBusstopPole == departureStop) {
                        if (currentDepartureTime.isNotEmpty()) {
                            departureTime = currentDepartureTime
                        } else if (currentArrivalTime.isNotEmpty()) {
                            departureTime = currentArrivalTime
                        }
                    }
                }
                
                // Check arrival stop match using busstopPole
                val selectedArrivalStop = this.selectedArrivalStop
                if (selectedArrivalStop != null) {
                    val arrivalStop = selectedArrivalStop.busstopPole
                    if (arrivalStop != null && currentBusstopPole == arrivalStop) {
                        if (currentArrivalTime.isNotEmpty()) {
                            arrivalTime = currentArrivalTime
                        } else if (currentDepartureTime.isNotEmpty()) {
                            arrivalTime = currentDepartureTime
                        }
                    }
                }
            }
            
            // Only append if arrival time is later than departure time
            if (departureTime != null && arrivalTime != null) {
                // Convert time strings to minutes for comparison
                val depMinutes = departureTime.timeToMinutes
                val arrMinutes = arrivalTime.timeToMinutes
                if (arrMinutes > depMinutes) {
                    // Calculate ride time in minutes
                    val rideTime = departureTime.calculateRideTime(arrivalTime)
                    val busNumber = timetable["dc:title"] as? String
                    val routePattern = timetable["odpt:busroutePattern"] as? String
                    val busTime = BusTime(
                        departureTime = departureTime,
                        arrivalTime = arrivalTime,
                        busNumber = busNumber,
                        routePattern = routePattern,
                        rideTime = rideTime
                    )
                    transportationTimes.add(busTime)
                }
            }
        }
        
        return transportationTimes
    }
    
    // Fetch bus timetable data from API
    private suspend fun fetchBusTimetableData(calendarType: ODPTCalendarType): List<Map<*, *>> {
        val selectedLine = this.selectedLine ?: return emptyList()
        val operatorCode = selectedLine.operatorCode ?: return emptyList()
        val selectedLineTitle = selectedLine.title ?: return emptyList()
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return emptyList()
        
        // Check if this is a GTFS route - GTFS routes don't use ODPT API
        if (selectedOperator.apiType() == ODPTAPIType.GTFS) {
            // For GTFS routes, return empty array
            // GTFS timetable data will be fetched separately using GTFS files
            android.util.Log.w("SettingsTimetableViewModel", "GTFS routes don't use ODPT API for timetable data")
            return emptyList()
        }
        
        // Use bus-specific timetable API (force bus API regardless of transportationType)
        val apiLink = "${selectedOperator.apiLink(APIDataType.TIMETABLE, TransportationKind.BUS)}&dc:title=$selectedLineTitle&odpt:calendar=${calendarType.rawValue}"
        
        try {
            android.util.Log.d("SettingsTimetableViewModel", "Fetch URL: $apiLink")
            // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
            val authKey = if (selectedOperator.apiType() == ODPTAPIType.CHALLENGE) {
                challengeKey
            } else {
                consumerKey
            }
            val (data, _) = odptService.fetchODPTDataWithAuth(apiLink, authKey)
            val json = odptService.parseJSONArray(data)
            return json.map { it }
        } catch (e: Exception) {
            android.util.Log.e("SettingsTimetableViewModel", "Error fetching bus timetable data: ${e.message}")
            return emptyList()
        }
    }
    
    // Process train timetable data for specific day type
    private suspend fun processTrainTimetableData(calendarType: ODPTCalendarType): List<TransportationTime> {
        val selectedLine = this.selectedLine ?: return emptyList()
        
        android.util.Log.d("SettingsTimetableViewModel", "processTrainTimetableData: calendarType=${calendarType.rawValue}, departureStop=${selectedDepartureStop?.code}, arrivalStop=${selectedArrivalStop?.code}")
        
        // Get actual directions from JSON data with fallback
        val actualDirection = selectedLine.lineDirection ?: ""
        val ascendingDirection = selectedLine.ascendingRailDirection ?: actualDirection
        val descendingDirection = selectedLine.descendingRailDirection ?: actualDirection
        
        // Try both directions and collect results
        val directions = listOf(ascendingDirection, descendingDirection)
        val directionResults = mutableListOf<List<TransportationTime>>()
        
        for (direction in directions) {
            // Fetch train timetable data from API for this direction
            val trainTimetableData = fetchTrainTimetableData(calendarType, direction)
            android.util.Log.d("SettingsTimetableViewModel", "Fetched ${trainTimetableData.size} train timetable entries for direction=$direction")
            
            // Extract train information and timetable objects in a single loop
            val transportationTimes = mutableListOf<TransportationTime>()
            
            // Log first few timetable entries for debugging
            if (trainTimetableData.isNotEmpty()) {
                val firstTimetable = trainTimetableData.firstOrNull()
                val firstTimetableObjectsRaw = firstTimetable?.get("odpt:trainTimetableObject")
                android.util.Log.d("SettingsTimetableViewModel", "First timetable raw: trainNumber=${firstTimetable?.get("odpt:trainNumber")}, trainTimetableObject type=${firstTimetableObjectsRaw?.javaClass?.simpleName}")
                
                val firstTimetableObjects = when (firstTimetableObjectsRaw) {
                    is List<*> -> firstTimetableObjectsRaw
                    is Map<*, *> -> listOf(firstTimetableObjectsRaw)
                    else -> null
                }
                
                if (firstTimetableObjects != null && firstTimetableObjects.isNotEmpty()) {
                    val firstObject = firstTimetableObjects.firstOrNull() as? Map<*, *>
                    android.util.Log.d("SettingsTimetableViewModel", "First timetable object sample: departureStation=${firstObject?.get("odpt:departureStation")}, arrivalStation=${firstObject?.get("odpt:arrivalStation")}, departureTime=${firstObject?.get("odpt:departureTime")}, keys=${firstObject?.keys}")
                } else {
                    android.util.Log.w("SettingsTimetableViewModel", "First timetable object is null or empty")
                }
            }
            
            var matchedCount = 0
            var unmatchedCount = 0
            
            for (timetable in trainTimetableData) {
                val trainNumber = timetable["odpt:trainNumber"] as? String ?: continue
                val trainType = timetable["odpt:trainType"] as? String ?: continue
                
                // Get trainTimetableObject - it can be a List or a single object
                val trainTimetableObjectsRaw = timetable["odpt:trainTimetableObject"]
                val trainTimetableObjects = when (trainTimetableObjectsRaw) {
                    is List<*> -> trainTimetableObjectsRaw
                    is Map<*, *> -> listOf(trainTimetableObjectsRaw)
                    else -> {
                        android.util.Log.w("SettingsTimetableViewModel", "trainTimetableObject is not List or Map: ${trainTimetableObjectsRaw?.javaClass?.simpleName}")
                        continue
                    }
                }
                
                if (trainTimetableObjects.isEmpty()) {
                    continue
                }
                
                var departureTime: String? = null
                var arrivalTime: String? = null
                
                for (timetableObject in trainTimetableObjects) {
                    val timetableObj = timetableObject as? Map<*, *> ?: continue
                    
                    // Check departure station match
                    val departureStop = timetableObj["odpt:departureStation"] as? String
                    if (departureStop == selectedDepartureStop?.code) {
                        departureTime = timetableObj["odpt:departureTime"] as? String
                    }
                    
                    // Check arrival station match
                    val arrivalStop = timetableObj["odpt:arrivalStation"] as? String
                    if (arrivalStop == selectedArrivalStop?.code) {
                        arrivalTime = timetableObj["odpt:arrivalTime"] as? String
                    } else {
                        val departureStopFromObj = timetableObj["odpt:departureStation"] as? String
                        if (departureStopFromObj == selectedArrivalStop?.code) {
                            arrivalTime = timetableObj["odpt:departureTime"] as? String
                        }
                    }
                }
                
                // Debug: log why times are not matched
                if (departureTime == null || arrivalTime == null) {
                    unmatchedCount++
                    if (unmatchedCount <= 3) {
                        // Log first few unmatched entries for debugging
                        val sampleObjects = trainTimetableObjects.take(3).mapNotNull { it as? Map<*, *> }
                        android.util.Log.d("SettingsTimetableViewModel", "Unmatched train $trainNumber: looking for departure=${selectedDepartureStop?.code}, arrival=${selectedArrivalStop?.code}, found stations=${sampleObjects.map { it["odpt:departureStation"] }}")
                    }
                } else {
                    matchedCount++
                }
                
                // Only append if arrival time is later than departure time
                if (departureTime != null && arrivalTime != null) {
                    // Convert time strings to minutes for comparison
                    val depMinutes = departureTime.timeToMinutes
                    val arrMinutes = arrivalTime.timeToMinutes
                    if (arrMinutes > depMinutes) {
                        // Calculate ride time in minutes
                        val rideTime = departureTime.calculateRideTime(arrivalTime)
                        val trainTime = TrainTime(
                            departureTime = departureTime,
                            arrivalTime = arrivalTime,
                            trainNumber = trainNumber,
                            trainType = trainType,
                            rideTime = rideTime
                        )
                        transportationTimes.add(trainTime)
                    }
                }
            }
            
            directionResults.add(transportationTimes)
            android.util.Log.d("SettingsTimetableViewModel", "Processed ${transportationTimes.size} transportation times for direction=$direction (matched=$matchedCount, unmatched=$unmatchedCount)")
        }
        
        // Filter out empty results (directions that didn't generate a list)
        val validResults = directionResults.filter { it.isNotEmpty() }
        
        // Choose the direction with smaller average ride time
        // For loop lines, both directions may have data, so compare average ride times
        val result = if (validResults.isEmpty()) {
            android.util.Log.w("SettingsTimetableViewModel", "No valid results for calendarType=${calendarType.rawValue}")
            emptyList()
        } else if (validResults.size == 1) {
            android.util.Log.d("SettingsTimetableViewModel", "Returning ${validResults[0].size} times from single direction")
            validResults[0]
        } else {
            // Both directions have data (e.g., loop line), choose the one with smaller average ride time
            val selectedResult = validResults.minByOrNull {
                if (it.isEmpty()) Int.MAX_VALUE else it.sumOf { time -> time.rideTime } / it.size
            } ?: emptyList()
            android.util.Log.d("SettingsTimetableViewModel", "Returning ${selectedResult.size} times from best direction")
            selectedResult
        }
        
        android.util.Log.d("SettingsTimetableViewModel", "processTrainTimetableData result: ${result.size} times for calendarType=${calendarType.rawValue}")
        return result
    }
    
    // Fetch train timetable data from API
    private suspend fun fetchTrainTimetableData(calendarType: ODPTCalendarType, direction: String): List<Map<*, *>> {
        val selectedLine = this.selectedLine ?: return emptyList()
        val operatorCode = selectedLine.operatorCode ?: return emptyList()
        val selectedLineCode = selectedLine.code
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return emptyList()
        
        // Build API link with direction parameter if direction is not empty
        // apiLink method will automatically use challengeKey for CHALLENGE API type
        // Ensure challengeKey is not empty for CHALLENGE API type
        val effectiveChallengeKey = if (selectedOperator.apiType() == ODPTAPIType.CHALLENGE && challengeKey.isEmpty()) {
            android.util.Log.w("SettingsTimetableViewModel", "challengeKey is empty for CHALLENGE API type!")
            consumerKey // Fallback to consumerKey if challengeKey is empty
        } else {
            challengeKey
        }
        var apiLink = "${selectedOperator.apiLink(APIDataType.TIMETABLE)}&odpt:railway=$selectedLineCode&odpt:calendar=${calendarType.rawValue}"
        android.util.Log.d("SettingsTimetableViewModel", "Built API link: $apiLink (challengeKey length=${effectiveChallengeKey.length})")
        if (direction.isNotEmpty()) {
            apiLink += "&odpt:railDirection=$direction"
        }
        
        try {
            android.util.Log.d("SettingsTimetableViewModel", "Fetch URL: $apiLink")
            // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
            val authKey = if (selectedOperator.apiType() == ODPTAPIType.CHALLENGE) {
                challengeKey
            } else {
                consumerKey
            }
            val (data, response) = odptService.fetchODPTDataWithAuth(apiLink, authKey)
            
            if (response.code != 200) {
                android.util.Log.e("SettingsTimetableViewModel", "HTTP error ${response.code} for train timetable: $apiLink")
                return emptyList()
            }
            
            // Log response for debugging
            val responseString = String(data)
            android.util.Log.d("SettingsTimetableViewModel", "Response length: ${responseString.length}, first 200 chars: ${responseString.take(200)}")
            
            val json = odptService.parseJSONArray(data)
            android.util.Log.d("SettingsTimetableViewModel", "Parsed ${json.size} timetable entries")
            return json.map { it }
        } catch (e: Exception) {
            android.util.Log.e("SettingsTimetableViewModel", "Error fetching train timetable data: ${e.message}", e)
            return emptyList()
        }
    }
    
    // MARK: - Train Route Validation
    // Get station timetable data for determined direction and find common train numbers
    suspend fun getStationTimetableData(): Map<ODPTCalendarType, List<TransportationTime>> {
        // Skip timetable generation if not all required fields are filled
        if (!isAllNotEmpty) {
            return emptyMap()
        }
        
        _isLoadingTimetable.value = true
        val loadingText = getApplication<Application>().getString(R.string.generatingTimetable)
        _loadingMessage.value = loadingText
        lineViewModel?.isLoadingTimetable = true
        lineViewModel?.loadingMessage = loadingText
        
        try {
            // Clear existing timetable data for all calendar types before generating new data
            clearAllTimetableData()
            
            val selectedLine = this.selectedLine ?: return emptyMap()
            
            // Get the actual direction from selectedLine
            val actualDirection = selectedLine.lineDirection ?: ""
            
            // Get actual directions from JSON data with fallback
            val ascendingDirection = selectedLine.ascendingRailDirection ?: actualDirection
            val descendingDirection = selectedLine.descendingRailDirection ?: actualDirection
            
            val allTimes = mutableMapOf<ODPTCalendarType, List<TransportationTime>>()
            
            // Get available calendar types dynamically
            // Same calendar types for TrainTimetable and StationTimetable on the same line
            val availableCalendarTypes = getAvailableCalendarTypes()
            
            for (calendarType in availableCalendarTypes) {
                // Get data for both directions
                val directions = listOf(ascendingDirection, descendingDirection)
                val directionResults = mutableListOf<List<TransportationTime>>()
                
                for (direction in directions) {
                    // Get departure and arrival data for this direction
                    val departureLink = stationTimetableApiLink(true, calendarType, direction)
                    val arrivalLink = stationTimetableApiLink(false, calendarType, direction)
                    
                    val departureData = fetchStationTimetableData(departureLink)
                    val arrivalData = fetchStationTimetableData(arrivalLink)
                    
                    // Process this direction if we have both departure and arrival data
                    val result = if (departureData.isNotEmpty()) {
                        getEstimatedTrainTime(
                            departureTimetableData = departureData,
                            arrivalTimetableData = arrivalData,
                            calendarType = calendarType,
                            approxRideTime = selectedRideTime
                        )
                    } else {
                        emptyList()
                    }
                    directionResults.add(result)
                }
                
                // Choose the direction with smaller average ride time
                val selectedResult = directionResults.minByOrNull {
                    if (it.isEmpty()) Int.MAX_VALUE else it.map { time -> time.rideTime }.sum() / it.size
                } ?: emptyList()
                
                // Save to dictionary with calendar type as key
                allTimes[calendarType] = selectedResult
            }
            return allTimes
        } finally {
            _isLoadingTimetable.value = false
            _loadingMessage.value = null
            lineViewModel?.isLoadingTimetable = false
            lineViewModel?.loadingMessage = null
        }
    }
    
    // MARK: - Station Timetable Data Processing
    // Generate station timetable link with flexible parameters
    fun stationTimetableApiLink(isDeparture: Boolean, calendarType: ODPTCalendarType, direction: String? = null): String {
        // Generate timetable information links for departure and arrival stations
        val operatorCode = selectedLine?.operatorCode ?: ""
        val dataSource = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
        val stationTimetableApiLink = dataSource?.apiLink(APIDataType.STOP_TIMETABLE) ?: ""
        
        // Extract station name from station code (remove "odpt.Station:" prefix)
        val lineCode = selectedLine?.code ?: ""
        val lineName = lineCode.replace("odpt.Railway:", "&owl:sameAs=odpt.StationTimetable:")
        
        val stationCode = if (isDeparture) selectedDepartureStop?.code else selectedArrivalStop?.code
        val stationName = stationCode?.split(".")?.lastOrNull() ?: ""
        
        // Use provided direction or fallback to lineDirection from selectedLine
        val directionCode = direction ?: (selectedLine?.lineDirection ?: "")
        val directionName = directionCode.replace("odpt.RailDirection:", "")
        
        val dateSuffix = calendarType.rawValue.replace("odpt.Calendar:", "")
        
        return "$stationTimetableApiLink$lineName.$stationName.$directionName.$dateSuffix"
    }
    
    // MARK: - Timetable Data Retrieval
    // Get timetable data from API endpoint
    private suspend fun fetchStationTimetableData(urlString: String): List<StationTimetableData> {
        try {
            android.util.Log.d("SettingsTimetableViewModel", "Fetch URL: $urlString")
            // Determine which key to use based on operator
            val operatorCode = selectedLine?.operatorCode ?: ""
            val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            val authKey = if (selectedOperator?.apiType() == ODPTAPIType.CHALLENGE) {
                challengeKey
            } else {
                consumerKey
            }
            val (data, response) = odptService.fetchODPTDataWithAuth(urlString, authKey)
            
            if (response.code != 200) {
                android.util.Log.w("SettingsTimetableViewModel", "Failed to fetch calendar types from ${if (urlString.contains("Westbound")) "ascending" else "descending"} direction - status: ${response.code}")
                return emptyList()
            }
            
            // Parse JSON and extract train data
            val json = odptService.parseJSONArray(data)
            val firstObject = json.firstOrNull() ?: return emptyList()
            val stationTimetableObjects = firstObject["odpt:stationTimetableObject"] as? List<*>
                ?: return emptyList()
            
            // Parse timetable objects into structured data
            val parsedData = stationTimetableObjects.mapNotNull { timetableObject ->
                val timetableObj = timetableObject as? Map<*, *> ?: return@mapNotNull null
                val departureTime = timetableObj["odpt:departureTime"] as? String
                    ?: return@mapNotNull null
                
                val trainNumber = timetableObj["odpt:trainNumber"] as? String ?: ""
                val trainType = timetableObj["odpt:trainType"] as? String ?: ""
                val destinationStation = ((timetableObj["odpt:destinationStation"] as? List<*>)?.firstOrNull() as? String)
                    ?: ""
                
                // Apply timetableHour extension for 0-3 AM times (add 24 hours for previous day)
                val adjustedDepartureTime = departureTime.adjustedForTimetable()
                
                StationTimetableData(
                    trainNumber = trainNumber,
                    departureTime = adjustedDepartureTime,
                    destinationStation = destinationStation,
                    trainType = trainType
                )
            }
            
            // Sort by departure time in ascending order (earliest first)
            return parsedData.sortedWith { first, second ->
                val firstMinutes = first.departureTime.timeToMinutes
                val secondMinutes = second.departureTime.timeToMinutes
                firstMinutes.compareTo(secondMinutes)
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsTimetableViewModel", "Failed to process timetable data from $urlString: ${e.message}")
            return emptyList()
        }
    }
    
    // MARK: - trainNumber-less Timetable Generation
    // Estimate departure times using simplified matching when train numbers are not available
    // Returns array of TrainTime objects with estimated departure times
    private suspend fun getEstimatedTrainTime(
        departureTimetableData: List<StationTimetableData>,
        arrivalTimetableData: List<StationTimetableData>,
        calendarType: ODPTCalendarType,
        approxRideTime: Int
    ): List<TransportationTime> {
        // Get unique train types from departure data
        val trainTypeList = departureTimetableData.trainTypeList()
        
        val allTransportationTimes = mutableListOf<TransportationTime>()
        
        // Process each train type separately
        for (trainType in trainTypeList) {
            // Filter departure and arrival data by train type
            val departureData = departureTimetableData.filteredBy(trainType)
            val arrivalData = arrivalTimetableData.filteredBy(trainType)
            
            // Skip if no data for this train type
            if (departureData.isEmpty()) continue
            
            val terminalDepartureData = departureData.filter { data ->
                data.destinationStation.isNotEmpty() && data.destinationStation == selectedArrivalStop?.code
            }
            
            // Create terminalTrainTimes list from terminalDepartureData
            val terminalTrainTimes = terminalDepartureData.map { data ->
                TrainTime(
                    departureTime = data.departureTime,
                    arrivalTime = "",
                    trainNumber = data.trainNumber.ifEmpty { null },
                    trainType = data.trainType.ifEmpty { null },
                    rideTime = selectedRideTime
                )
            }
            if (terminalTrainTimes.isNotEmpty()) {
                allTransportationTimes.addAll(terminalTrainTimes)
            }
            
            // Remove terminalDepartureData from departureData
            val filteredDepartureData = departureData.filter { data ->
                data.destinationStation.isEmpty() || data.destinationStation != selectedArrivalStop?.code
            }
            
            // Process this train type with filtered data
            if (filteredDepartureData.isNotEmpty()) {
                val trainTimes = processTrainType(
                    departureData = filteredDepartureData,
                    arrivalData = arrivalData,
                    trainType = trainType,
                    approxRideTime = approxRideTime
                )
                // Calculate average ride time for this train type
                if (trainTimes.isNotEmpty()) {
                    allTransportationTimes.addAll(trainTimes)
                }
            }
        }
        
        // Sort all train times by departure time
        return allTransportationTimes.sortedWith { first, second ->
            val firstMinutes = first.departureTime.timeToMinutes
            val secondMinutes = second.departureTime.timeToMinutes
            firstMinutes.compareTo(secondMinutes)
        }
    }
    
    // MARK: - Train Type Processing with Advanced Algorithm
    // Process timetable data for a specific train type using advanced statistical methods
    private fun processTrainType(
        departureData: List<StationTimetableData>,
        arrivalData: List<StationTimetableData>,
        trainType: String,
        approxRideTime: Int
    ): List<TransportationTime> {
        // 0) Filter out trains that don't reach the arrival station (only for trains without train numbers)
        val filteredDepartureData = departureData.filter { data ->
            !(data.trainNumber.isEmpty() && !isTrainReachingArrivalStop(data.destinationStation))
        }
        
        // 0.5) Filter arrival data to match departure destinations
        val filteredArrivalData = arrivalData.filter { arrivalDataItem ->
            // Check if there's a matching departure with the same destination
            filteredDepartureData.any { departureDataItem ->
                departureDataItem.destinationStation == arrivalDataItem.destinationStation
            }
        }
        
        // 1) Build departure and arrival station data
        val departureTimes = filteredDepartureData.map { it.departureTime }.filter { it.isNotEmpty() }
        val arrivalTimes = filteredArrivalData.map { it.departureTime }.filter { it.isNotEmpty() }
        
        // 2) Sort time strings directly
        val sortedDepartureTimes = departureTimes.sorted()
        val sortedArrivalTimes = arrivalTimes.sorted()
        
        // 5) Collect pairs using no duplicates with minimum ride time within range
        val transportationTimes = mutableListOf<TransportationTime>()
        val usedArrivals = mutableSetOf<Int>() // Track used arrival indices
        
        for (departureTime in sortedDepartureTimes) {
            var bestArrivalIndex: Int? = null
            var minDistanceFromAverage = Double.MAX_VALUE
            
            for ((index, arrivalTime) in sortedArrivalTimes.withIndex()) {
                // Skip if this arrival has already been used
                if (usedArrivals.contains(index)) continue
                
                val rideTimeMinutes = departureTime.calculateRideTime(arrivalTime)
                // Skip if distance from target exceeds half of approxRideTime
                val distanceFromTarget = kotlin.math.abs(rideTimeMinutes - approxRideTime).toDouble()
                if (distanceFromTarget > approxRideTime / 2.0) continue
                
                if (distanceFromTarget < minDistanceFromAverage) {
                    minDistanceFromAverage = distanceFromTarget
                    bestArrivalIndex = index
                }
            }
            
            // Use the best arrival and mark it as used
            bestArrivalIndex?.let { bestIndex ->
                val trainTime = TrainTime(
                    departureTime = departureTime,
                    arrivalTime = sortedArrivalTimes[bestIndex],
                    trainNumber = null,
                    trainType = trainType,
                    rideTime = departureTime.calculateRideTime(sortedArrivalTimes[bestIndex])
                )
                transportationTimes.add(trainTime)
                usedArrivals.add(bestIndex)
            }
        }
        return transportationTimes
    }
    
    // Check if train reaches the arrival station (destination station index is not between departure and arrival)
    private fun isTrainReachingArrivalStop(destinationStation: String): Boolean {
        // Get station indices for departure, arrival, and destination stations
        val departureIndex = selectedDepartureStop?.index
        val arrivalIndex = selectedArrivalStop?.index
        val destinationIndex = getStationIndexFromDestinationStation(destinationStation)
        
        if (departureIndex == null || arrivalIndex == null || destinationIndex == null) {
            // If any index is missing, allow the train (fallback behavior)
            return true
        }
        
        // Check if destination station index is between departure and arrival stations
        val minIndex = minOf(departureIndex, arrivalIndex)
        val maxIndex = maxOf(departureIndex, arrivalIndex)
        
        // If destination station is between departure and arrival, the train doesn't reach the arrival station
        if (destinationIndex in (minIndex + 1)..<maxIndex) {
            return false
        }
        
        // Train reaches the arrival station
        return true
    }
    
    // MARK: - Station Index Helper
    // Get station index from destination station code (only for selected line)
    private fun getStationIndexFromDestinationStation(destinationStationCode: String): Int? {
        // Only search in the selected line's stations
        val selectedLine = this.selectedLine ?: return null
        
        // Parse destination station code (e.g., "odpt.Station:Odakyu.Odawara.Odawara")
        // Extract the line code from selected (e.g., "Odawara" from "odpt.Railway:Odakyu.Odawara")
        val selectedComponents = selectedLine.code.split(".")
        val selectedLineCode = selectedComponents.getOrNull(2) ?: ""
        
        // Only process if the destination station belongs to the selected line
        // Check if the destination station code contains the selected line code
        if (!destinationStationCode.contains(selectedLineCode)) {
            return null
        }
        
        // Extract the station name from the code
        val stationName = destinationStationCode.split(".").lastOrNull() ?: ""
        
        // Use already loaded line stops and filter for railway stations
        val selectedLineStations = lineStops.filter { stop ->
            stop.kind == TransportationLineKind.RAILWAY
        }.mapNotNull { stop ->
            if (stop.kind == TransportationLineKind.RAILWAY) {
                stop
            } else {
                null
            }
        }
        
        // Find station by code or name match
        val station = selectedLineStations.firstOrNull {
            it.code == destinationStationCode ||
            it.name == stationName ||
            it.title?.getLocalizedName(getApplication(), it.name) == stationName
        }
        return station?.index
    }
    
    // MARK: - Timetable Data Saving
    // Save timetable data to SharedPreferences for display in TimetableContentView
    // Saves both departure times and ride times grouped by hour
    private fun saveTimetableToUserDefaults(transportationTimes: List<TransportationTime>, calendarType: ODPTCalendarType) {
        android.util.Log.d("SettingsTimetableViewModel", "saveTimetableToUserDefaults: ${transportationTimes.size} times for calendarType=${calendarType.rawValue}, goorback=$goorback, lineIndex=${lineIndex + 1}")
        
        // Clear existing timetable data for this line and calendar type
        // IMPORTANT: Use initialized goorback and lineIndex to prevent data corruption across routes
        clearTimetableDataForRoute(calendarType, goorback, lineIndex + 1)
        
        // Group TransportationTime objects by hour
        val hourlyTransportationTimes = mutableMapOf<Int, MutableList<TransportationTime>>()
        
        for (transportationTimeItem in transportationTimes) {
            val timeComponents = transportationTimeItem.departureTime.split(":")
            if (timeComponents.size == 2) {
                val hour = timeComponents[0].toIntOrNull()
                if (hour != null) {
                    hourlyTransportationTimes.getOrPut(hour) { mutableListOf() }.add(transportationTimeItem)
                }
            }
        }
        
        android.util.Log.d("SettingsTimetableViewModel", "Grouped into ${hourlyTransportationTimes.size} hours: ${hourlyTransportationTimes.keys.sorted()}")
        
        // Sort and save to SharedPreferences using unified TransportationTime format
        for ((hour, transportationTimesForHour) in hourlyTransportationTimes) {
            val sortedTransportationTimes = transportationTimesForHour.sortedBy { it.departureTime }
            // IMPORTANT: Use initialized goorback and lineIndex to prevent data corruption across routes
            
            android.util.Log.d("SettingsTimetableViewModel", "Saving ${sortedTransportationTimes.size} times for hour=$hour")
            goorback.saveTransportationTimes(sortedTransportationTimes, calendarType, lineIndex, hour, sharedPreferences)
        }
        
        // Save train type list for the entire timetable
        // IMPORTANT: Use initialized goorback and lineIndex to prevent data corruption across routes
        val allTransportationTimes = hourlyTransportationTimes.values.flatten()
        val trainTypes = allTransportationTimes.mapNotNull { (it as? TrainTime)?.trainType }.distinct()
        android.util.Log.d("SettingsTimetableViewModel", "Saving ${trainTypes.size} train types: $trainTypes")
        
        goorback.saveTrainTypeList(allTransportationTimes, calendarType, lineIndex, sharedPreferences)
        
        // Ensure all SharedPreferences changes are synchronized to disk
        sharedPreferences.edit {
            // All changes are already applied by saveTrainTypeList
        }
    }
    
    // MARK: - Common Timetable Data Finalization with Arrays
    // Common post-processing for timetable data with weekday/weekend arrays
    suspend fun finalizeTimetableData(weekdayTimes: List<TransportationTime>, weekendTimes: List<TransportationTime>) {
        // Save timetable data using unified TransportationTime format
        saveTimetableToUserDefaults(weekdayTimes, ODPTCalendarType.Weekday)
        saveTimetableToUserDefaults(weekendTimes, ODPTCalendarType.SaturdayHoliday)
        
        // Save all data after timetable data has been processed and saved
        lineViewModel?.saveAllDataToUserDefaults()
        
        // Clear cache for available calendar types to force reload
        goorback.clearCalendarTypesCache(lineIndex)
        
        // Notify that timetable data has been updated
        // TODO: Implement notification system for Android
    }
    
    // MARK: - Common Timetable Data Finalization with Calendar Types
    // Common post-processing for timetable data with individual calendar types
    suspend fun finalizeTimetableData(calendarTimes: Map<ODPTCalendarType, List<TransportationTime>>) {
        android.util.Log.d("SettingsTimetableViewModel", "finalizeTimetableData: ${calendarTimes.size} calendar types, total times=${calendarTimes.values.sumOf { it.size }}")
        // Save timetable data for each calendar type individually
        for ((calendarType, times) in calendarTimes) {
            android.util.Log.d("SettingsTimetableViewModel", "Saving ${times.size} times for calendarType=${calendarType.rawValue}")
            saveTimetableToUserDefaults(times, calendarType)
        }
        
        // Save all data after timetable data has been processed and saved
        // Note: saveAllDataToUserDefaults() will save lineCode if selectedLine is available
        lineViewModel?.saveAllDataToUserDefaults()
        
        // Update cache with only the merged representative calendar types
        // This ensures that loadAvailableCalendarTypes returns only the merged types
        val mergedRepresentativeTypes = calendarTimes.keys.toList()
        val lineCacheKey = "${goorback}line${lineIndex + 1}_calendarTypes"
        val typeStrings = mergedRepresentativeTypes.map { it.rawValue }
        sharedPreferences.edit { putStringSet(lineCacheKey, typeStrings.toSet()) }
        
        // Clear cache for available calendar types to force reload
        goorback.clearCalendarTypesCache(lineIndex)
        
        // Notify that timetable data has been updated
        // TODO: Implement notification system for Android
    }
    
    // MARK: - Timetable Data Clearing
    // Clear existing timetable data for specified route, line number, and calendar type
    private fun clearTimetableDataForRoute(calendarType: ODPTCalendarType, goorback: String, lineNumber: Int) {
        var clearedCount = 0
        val clearedKeys = mutableListOf<String>()
        
        // Clear all hours (4-24) for the specified calendar type, route, and line
        for (hour in 4..25) {
            val timetableKey = goorback.timetableKey(calendarType, lineNumber - 1, hour)
            val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarType, lineNumber - 1, hour)
            val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarType, lineNumber - 1, hour)
            
            val hadTimetable = sharedPreferences.contains(timetableKey)
            val hadRideTime = sharedPreferences.contains(timetableRideTimeKey)
            val hadTrainType = sharedPreferences.contains(timetableTrainTypeKey)
            
            if (hadTimetable || hadRideTime || hadTrainType) {
                // Log actual key being deleted for first few hours
                if (hour <= 6 && clearedKeys.size < 3) {
                    clearedKeys.add(timetableKey)
                }
            }
            
            sharedPreferences.edit {
                remove(timetableKey)
                    .remove(timetableRideTimeKey)
                    .remove(timetableTrainTypeKey)
            }
            
            if (hadTimetable || hadRideTime || hadTrainType) {
                clearedCount++
            }
        }
        
        // Clear train type list
        val trainTypeListKey = goorback.trainTypeListKey(calendarType, lineNumber - 1)
        sharedPreferences.edit { remove(trainTypeListKey) }
    }
}

