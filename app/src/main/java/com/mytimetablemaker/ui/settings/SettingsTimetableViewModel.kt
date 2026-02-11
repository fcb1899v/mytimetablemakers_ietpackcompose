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
    val lineStops: List<TransportationStop>
    val lineSuggestions: List<TransportationLine>
    val selectedLineNumber: Int
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
    private val isLoadingTimetable = MutableStateFlow(false)
    private val loadingMessage = MutableStateFlow<String?>(null)

    // MARK: - Configuration Properties
    private val gtfsService = GTFSDataService(application)
    private val odptService = ODPTDataService(application)
    // Get ODPT access token and challenge token from BuildConfig
    private val consumerKey: String = BuildConfig.ODPT_ACCESS_TOKEN
    private val challengeKey: String = BuildConfig.ODPT_CHALLENGE_TOKEN

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
    
    private val lineStops: List<TransportationStop>
        get() = lineViewModel?.lineStops ?: emptyList()
    
    private val lineSuggestions: List<TransportationLine>
        get() = lineViewModel?.lineSuggestions ?: emptyList()
    
    private val selectedLineNumber: Int
        get() = lineViewModel?.selectedLineNumber ?: 1
    
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
        // Skip timetable generation if required selections are missing
        val hasStops = selectedDepartureStop != null && selectedArrivalStop != null
        if (selectedLine == null || !hasStops) {
            android.util.Log.d(
                "SettingsTimetableViewModel",
                "getTimeTableData: skipped (line=${selectedLine?.code}, hasStops=$hasStops)"
            )
            return emptyMap()
        }
        
        isLoadingTimetable.value = true
        val loadingText = getApplication<Application>().getString(R.string.generatingTimetable)
        loadingMessage.value = loadingText
        lineViewModel?.isLoadingTimetable = true
        lineViewModel?.loadingMessage = loadingText
        
        try {
            android.util.Log.d(
                "SettingsTimetableViewModel",
                "getTimeTableData: begin line=${selectedLine?.code} kind=${selectedLine?.kind} dep=${selectedDepartureStop?.code} arr=${selectedArrivalStop?.code}"
            )
            // Clear existing timetable data for all calendar types before generating new data
            clearAllTimetableData()
            
            // Get available calendar types for this line
            val availableCalendarTypes = getAvailableCalendarTypes()
            android.util.Log.d(
                "SettingsTimetableViewModel",
                "getTimeTableData: calendarTypes=${availableCalendarTypes.map { it.rawValue }}"
            )
            
            // Process data for each calendar type first (create timetables individually)
            val allTimes = mutableMapOf<ODPTCalendarType, List<TransportationTime>>()
            
            // For GTFS routes, fetch all calendar types at once for better performance
            val selectedLine = this.selectedLine
            if (selectedLine?.kind == TransportationLineKind.BUS) {
                android.util.Log.d("SettingsTimetableViewModel", "getTimeTableData: branch=BUS")
                val operatorCode = selectedLine.operatorCode
                val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
                // For bus routes, process each calendar type individually
                // GTFS routes are handled inside processBusTimetableData
                for (calendarType in availableCalendarTypes) {
                    android.util.Log.d(
                        "SettingsTimetableViewModel",
                        "getTimeTableData: bus calendar=${calendarType.rawValue} apiType=${selectedOperator?.apiType()}"
                    )
                    val times = processBusTimetableData(calendarType)
                    allTimes[calendarType] = times
                }
            } else {
                android.util.Log.d("SettingsTimetableViewModel", "getTimeTableData: branch=RAILWAY")
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
            isLoadingTimetable.value = false
            loadingMessage.value = null
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
            return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        }
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
                android.util.Log.d("SettingsTimetableViewModel", "GTFS: Missing routeId for calendar type fetch")
                return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
            }
            
            try {
                // TODO: Implement fetchGTFSCalendarTypes in GTFSDataService
                // For now, return default calendar types
                val calendarTypes = listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
                return calendarTypes
            } catch (e: Exception) {
                android.util.Log.d("SettingsTimetableViewModel", "Failed to fetch GTFS calendar types: ${e.message}", e)
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
                android.util.Log.d("SettingsTimetableViewModel", "$apiTypeName: Missing required information")
                return emptyList()
            }
            
            try {
                // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
                val (data, response) = odptService.fetchODPTData(apiLink)
                
                if (response.code != 200) {
                    android.util.Log.d("SettingsTimetableViewModel", "Failed to fetch calendar types - status: ${response.code}")
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
                return processCalendarTypes(foundCalendarTypes)
                
            } catch (e: Exception) {
                android.util.Log.d("SettingsTimetableViewModel", "Error fetching calendar types from $apiTypeName: ${e.message}", e)
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
            
            try {
                // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
                val (data, response) = odptService.fetchODPTData(ascendingLink)
                
                if (response.code == 200) {
                    val json = odptService.parseJSONArray(data)
                    
                    for (timetable in json) {
                        val calendar = timetable["odpt:calendar"] as? String
                        if (calendar != null) {
                            allCalendarTypes.add(calendar)
                        }
                    }
                    ascendingSuccess = true
                } else {
                    android.util.Log.d("SettingsTimetableViewModel", "Failed to fetch calendar types from ascending direction - status: ${response.code}")
                }
            } catch (e: Exception) {
                android.util.Log.d("SettingsTimetableViewModel", "Error fetching calendar types from ascending direction: ${e.message}", e)
            }
        }
        
        // Try descending direction only if ascending failed or if it's a different direction
        if (!ascendingSuccess && descendingDirection.isNotEmpty() && descendingDirection != ascendingDirection) {
            val descendingLink = "${dataSource.apiLink(APIDataType.STOP_TIMETABLE)}&odpt:station=$stationCode&odpt:railDirection=$descendingDirection"
            
            try {
                // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
                val (data, response) = odptService.fetchODPTData(descendingLink)
                
                if (response.code == 200) {
                    val json = odptService.parseJSONArray(data)
                    
                    for (timetable in json) {
                        val calendar = timetable["odpt:calendar"] as? String
                        if (calendar != null) {
                            allCalendarTypes.add(calendar)
                        }
                    }
                } else {
                    android.util.Log.d("SettingsTimetableViewModel", "Failed to fetch calendar types from descending direction - status: ${response.code}")
                }
            } catch (e: Exception) {
                android.util.Log.d("SettingsTimetableViewModel", "Error fetching calendar types from descending direction: ${e.message}", e)
            }
        }
        
        // Convert to ODPTCalendarType array and process
        return processCalendarTypes(allCalendarTypes)
    }
    
    // Process calendar types: remove duplicates and preserve .specific types
    private fun processCalendarTypes(foundCalendarTypes: Set<String>): List<ODPTCalendarType> {
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
        
        return result
    }
    
    // MARK: - Timetable Data Processing
    // Process bus timetable data for specific day type
    private suspend fun processBusTimetableData(calendarType: ODPTCalendarType): List<TransportationTime> {
        // Check if this is a GTFS route
        val selectedLine = this.selectedLine ?: return emptyList()
        android.util.Log.d(
            "SettingsTimetableViewModel",
            "processBusTimetableData: start calendar=${calendarType.rawValue} line=${selectedLine.code} dep=${selectedDepartureStop?.busStopPole} arr=${selectedArrivalStop?.busStopPole}"
        )
        val operatorCode = selectedLine.operatorCode ?: return emptyList()
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return emptyList()
        android.util.Log.d(
            "SettingsTimetableViewModel",
            "processBusTimetableData: operator=${selectedOperator.name} apiType=${selectedOperator.apiType()}"
        )
        
        // For GTFS routes, fetch timetable data from GTFS files
        if (selectedOperator.apiType() == ODPTAPIType.GTFS) {
            val departureStop = selectedDepartureStop ?: return emptyList()
            val arrivalStop = selectedArrivalStop ?: return emptyList()
            val departureStopId = departureStop.busStopPole ?: departureStop.code ?: return emptyList()
            val arrivalStopId = arrivalStop.busStopPole ?: arrivalStop.code ?: return emptyList()
            
            try {
                android.util.Log.d(
                    "SettingsTimetableViewModel",
                    "processBusTimetableData: GTFS routeId=${selectedLine.code} dep=$departureStopId arr=$arrivalStopId"
                )
                val busTimes = gtfsService.fetchGTFSTimetableForRoute(
                    routeId = selectedLine.code,
                    departureStopId = departureStopId,
                    arrivalStopId = arrivalStopId,
                    calendarType = calendarType,
                    transportOperator = selectedOperator,
                    consumerKey = consumerKey
                )
                android.util.Log.d(
                    "SettingsTimetableViewModel",
                    "processBusTimetableData: GTFS generated=${busTimes.size} calendar=${calendarType.rawValue}"
                )
                return busTimes
            } catch (e: Exception) {
                android.util.Log.d("SettingsTimetableViewModel", "Failed to fetch GTFS bus timetable: ${e.message}", e)
                return emptyList()
            }
        }
        
        // For ODPT routes, use existing API-based processing
        // Fetch bus timetable data from API
        val busTimetableData = fetchBusTimetableData(calendarType)
        android.util.Log.d(
            "SettingsTimetableViewModel",
            "processBusTimetableData: fetched timetables=${busTimetableData.size} (calendar=${calendarType.rawValue})"
        )
        val totalTimetables = busTimetableData.size
        if (totalTimetables == 0) {
            android.util.Log.d(
                "SettingsTimetableViewModel",
                "processBusTimetableData: empty API data (calendar=${calendarType.rawValue}, line=${selectedLine.code})"
            )
        }
        
        // Extract bus information and timetable objects in a single loop
        val transportationTimes = mutableListOf<TransportationTime>()
        
        var totalObjects = 0
        var matchedDeparture = 0
        var matchedArrival = 0
        for (timetable in busTimetableData) {
            val busTimetableObjects = timetable["odpt:busTimetableObject"] as? List<*>
                ?: run {
                    android.util.Log.d(
                        "SettingsTimetableViewModel",
                        "processBusTimetableData: missing odpt:busTimetableObject"
                    )
                    continue
                }
            totalObjects += busTimetableObjects.size
            
            var departureTime: String? = null
            var arrivalTime: String? = null
            
            for (timetableObject in busTimetableObjects) {
                val timetableObj = timetableObject as? Map<*, *> ?: continue
                val currentBusStopPole = timetableObj["odpt:busstopPole"] as? String ?: ""
                val currentDepartureTime = timetableObj["odpt:departureTime"] as? String ?: ""
                val currentArrivalTime = timetableObj["odpt:arrivalTime"] as? String ?: ""
                
                // Check departure stop match using busStopPole
                val selectedDepartureStop = this.selectedDepartureStop
                if (selectedDepartureStop != null) {
                    val departureStop = selectedDepartureStop.busStopPole
                    if (departureStop != null && currentBusStopPole == departureStop) {
                        if (currentDepartureTime.isNotEmpty()) {
                            departureTime = currentDepartureTime
                            matchedDeparture++
                        } else if (currentArrivalTime.isNotEmpty()) {
                            departureTime = currentArrivalTime
                            matchedDeparture++
                        }
                    }
                }
                
                // Check arrival stop match using busStopPole
                val selectedArrivalStop = this.selectedArrivalStop
                if (selectedArrivalStop != null) {
                    val arrivalStop = selectedArrivalStop.busStopPole
                    if (arrivalStop != null && currentBusStopPole == arrivalStop) {
                        if (currentArrivalTime.isNotEmpty()) {
                            arrivalTime = currentArrivalTime
                            matchedArrival++
                        } else if (currentDepartureTime.isNotEmpty()) {
                            arrivalTime = currentDepartureTime
                            matchedArrival++
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
        if (transportationTimes.isEmpty()) {
            android.util.Log.d(
                "SettingsTimetableViewModel",
                "processBusTimetableData: no matched times (calendar=${calendarType.rawValue}, line=${selectedLine.code}, timetables=$totalTimetables, objects=$totalObjects, depMatches=$matchedDeparture, arrMatches=$matchedArrival)"
            )
        } else {
            android.util.Log.d(
                "SettingsTimetableViewModel",
                "processBusTimetableData: generated=${transportationTimes.size} (calendar=${calendarType.rawValue}, timetables=$totalTimetables, objects=$totalObjects, depMatches=$matchedDeparture, arrMatches=$matchedArrival)"
            )
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
            return emptyList()
        }
        
        // Use bus-specific timetable API (force bus API regardless of transportationType)
        val apiLink = "${selectedOperator.apiLink(APIDataType.TIMETABLE, TransportationKind.BUS)}&dc:title=$selectedLineTitle&odpt:calendar=${calendarType.rawValue}"
        android.util.Log.d(
            "SettingsTimetableViewModel",
            "fetchBusTimetableData: calendar=${calendarType.rawValue} title=$selectedLineTitle link=$apiLink"
        )
        
        try {
            // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
            val (data, response) = odptService.fetchODPTData(apiLink)
            if (response.code != 200) {
                android.util.Log.d(
                    "SettingsTimetableViewModel",
                    "fetchBusTimetableData: HTTP ${response.code}"
                )
                return emptyList()
            }
            val json = odptService.parseJSONArray(data)
            android.util.Log.d(
                "SettingsTimetableViewModel",
                "fetchBusTimetableData: received ${json.size} timetables"
            )
            return json.map { it }
        } catch (e: Exception) {
            android.util.Log.d("SettingsTimetableViewModel", "Error fetching bus timetable data: ${e.message}", e)
            return emptyList()
        }
    }
    
    // Process train timetable data for specific day type
    private suspend fun processTrainTimetableData(calendarType: ODPTCalendarType): List<TransportationTime> {
        val selectedLine = this.selectedLine ?: return emptyList()
        
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
            
            // Extract train information and timetable objects in a single loop
            val transportationTimes = mutableListOf<TransportationTime>()
            
            for (timetable in trainTimetableData) {
                val trainNumber = timetable["odpt:trainNumber"] as? String ?: continue
                val trainType = timetable["odpt:trainType"] as? String ?: continue
                
                // Get trainTimetableObject - it can be a List or a single object
                val trainTimetableObjectsRaw = timetable["odpt:trainTimetableObject"]
                val trainTimetableObjects = when (trainTimetableObjectsRaw) {
                    is List<*> -> trainTimetableObjectsRaw
                    is Map<*, *> -> listOf(trainTimetableObjectsRaw)
                    else -> {
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
            if (transportationTimes.isEmpty()) {
                android.util.Log.d(
                    "SettingsTimetableViewModel",
                    "processTrainTimetableData: no matched times (calendar=${calendarType.rawValue}, line=${selectedLine.code}, direction=$direction)"
                )
            }
        }
        
        // Filter out empty results (directions that didn't generate a list)
        val validResults = directionResults.filter { it.isNotEmpty() }
        
        // Choose the direction with smaller average ride time
        // For loop lines, both directions may have data, so compare average ride times
        val result = if (validResults.isEmpty()) {
            emptyList()
        } else if (validResults.size == 1) {
            validResults[0]
        } else {
            // Both directions have data (e.g., loop line), choose the one with smaller average ride time
            val selectedResult = validResults.minByOrNull {
                if (it.isEmpty()) Int.MAX_VALUE else it.sumOf { time -> time.rideTime } / it.size
            } ?: emptyList()
            selectedResult
        }
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
        if (selectedOperator.apiType() == ODPTAPIType.CHALLENGE && challengeKey.isEmpty()) {
            android.util.Log.d("SettingsTimetableViewModel", "challengeKey is empty for CHALLENGE API type!")
        }
        var apiLink = "${selectedOperator.apiLink(APIDataType.TIMETABLE)}&odpt:railway=$selectedLineCode&odpt:calendar=${calendarType.rawValue}"
        if (direction.isNotEmpty()) {
            apiLink += "&odpt:railDirection=$direction"
        }
        android.util.Log.d(
            "SettingsTimetableViewModel",
            "fetchTrainTimetableData: calendar=${calendarType.rawValue} direction=$direction link=$apiLink"
        )
        
        try {
            // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
            val (data, response) = odptService.fetchODPTData(apiLink)
            
            if (response.code != 200) {
                android.util.Log.d("SettingsTimetableViewModel", "HTTP error ${response.code} for train timetable: $apiLink")
                return emptyList()
            }
            val json = odptService.parseJSONArray(data)
            android.util.Log.d(
                "SettingsTimetableViewModel",
                "fetchTrainTimetableData: received ${json.size} timetables"
            )
            return json.map { it }
        } catch (e: Exception) {
            android.util.Log.d("SettingsTimetableViewModel", "Error fetching train timetable data: ${e.message}", e)
            return emptyList()
        }
    }
    
    // MARK: - Timetable Data Saving
    // Save timetable data to SharedPreferences for display in TimetableContentView
    // Saves both departure times and ride times grouped by hour
    private fun saveTimetableToUserDefaults(transportationTimes: List<TransportationTime>, calendarType: ODPTCalendarType) {
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
        
        // Sort and save to SharedPreferences using unified TransportationTime format
        for ((hour, transportationTimesForHour) in hourlyTransportationTimes) {
            val sortedTransportationTimes = transportationTimesForHour.sortedBy { it.departureTime }
            // IMPORTANT: Use initialized goorback and lineIndex to prevent data corruption across routes
            goorback.saveTransportationTimes(sortedTransportationTimes, calendarType, lineIndex, hour, sharedPreferences)
        }
        
        // Save train type list for the entire timetable
        // IMPORTANT: Use initialized goorback and lineIndex to prevent data corruption across routes
        val allTransportationTimes = hourlyTransportationTimes.values.flatten()
        goorback.saveTrainTypeList(allTransportationTimes, calendarType, lineIndex, sharedPreferences)
        
        // Ensure all SharedPreferences changes are synchronized to disk
        sharedPreferences.edit {
            // All changes are already applied by saveTrainTypeList
        }
    }
    
    // MARK: - Common Timetable Data Finalization with Calendar Types
    // Common post-processing for timetable data with individual calendar types
    suspend fun finalizeTimetableData(calendarTimes: Map<ODPTCalendarType, List<TransportationTime>>) {
        // Save timetable data for each calendar type individually
        for ((calendarType, times) in calendarTimes) {
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

