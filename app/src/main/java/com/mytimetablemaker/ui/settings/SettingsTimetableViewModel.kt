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

// Interface for accessing SettingsLineViewModel properties
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

// ViewModel for timetable generation, data fetching, processing, and saving.
class SettingsTimetableViewModel(
    application: Application,
    private val sharedPreferences: SharedPreferences,
    private val lineViewModel: SettingsLineViewModelInterface? = null
) : AndroidViewModel(application) {
    
    private val isLoadingTimetable = MutableStateFlow(false)
    private val loadingMessage = MutableStateFlow<String?>(null)

    private val gtfsService = GTFSDataService(application)
    private val odptService = ODPTDataService(application)
    private val consumerKey: String = BuildConfig.ODPT_ACCESS_TOKEN
    private val challengeKey: String = BuildConfig.ODPT_CHALLENGE_TOKEN

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
    
    // Clear all timetable data for current route and line number
    suspend fun clearAllTimetableData() {
        withContext(Dispatchers.IO) {
            for (calendarType in ODPTCalendarType.allCases) {
                clearTimetableDataForRoute(calendarType, goorback, lineIndex + 1)
            }
            
            selectedLine?.let { line ->
                val cacheKey = "${line.code}_${line.kind.name}_calendarTypes"
                sharedPreferences.edit { remove(cacheKey) }
                
                val lineCacheKey = "${goorback}line${lineIndex + 1}_calendarTypes"
                sharedPreferences.edit { remove(lineCacheKey) }
            }
        }
    }
    
    // Get timetable data for all calendar types, merging duplicates by display type
    suspend fun getTimeTableData(): Map<ODPTCalendarType, List<TransportationTime>> {
        val hasStops = selectedDepartureStop != null && selectedArrivalStop != null
        if (selectedLine == null || !hasStops) {
            return emptyMap()
        }
        
        isLoadingTimetable.value = true
        val loadingText = getApplication<Application>().getString(R.string.generatingTimetable)
        loadingMessage.value = loadingText
        lineViewModel?.isLoadingTimetable = true
        lineViewModel?.loadingMessage = loadingText
        
        try {
            clearAllTimetableData()
            
            val availableCalendarTypes = getAvailableCalendarTypes()
            
            // Process data for each calendar type first (create timetables individually)
            val allTimes = mutableMapOf<ODPTCalendarType, List<TransportationTime>>()
            
            val selectedLine = this.selectedLine
            if (selectedLine?.kind == TransportationLineKind.BUS) {
                for (calendarType in availableCalendarTypes) {
                    val times = processBusTimetableData(calendarType)
                    allTimes[calendarType] = times
                }
            } else {
                for (calendarType in availableCalendarTypes) {
                    val times = processTrainTimetableData(calendarType)
                    allTimes[calendarType] = times
                }
            }
            
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
                    
                    for (typeToDelete in calendarTypes) {
                        allTimes.remove(typeToDelete)
                        mergedSourceTypes.add(typeToDelete)
                    }
                } else {
                    calendarTypes.firstOrNull()?.let { calendarType ->
                        allTimes[calendarType]?.let { times ->
                            mergedTimes[calendarType] = times
                            allTimes.remove(calendarType)
                        }
                    }
                }
            }
            
            if (mergedSourceTypes.isNotEmpty()) {
                for (sourceType in mergedSourceTypes) {
                    clearTimetableDataForRoute(sourceType, goorback, lineIndex + 1)
                }
            }
            
            val lineCacheKey = "${goorback}line${lineIndex + 1}_calendarTypes"
            val typeStrings = availableCalendarTypes.map { it.rawValue }
            sharedPreferences.edit { putStringSet(lineCacheKey, typeStrings.toSet()) }
            
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
    
    // Get available calendar types for the selected line from cache or API
    private suspend fun getAvailableCalendarTypes(): List<ODPTCalendarType> {
        val selectedLine = this.selectedLine ?: return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        val operatorCode = selectedLine.operatorCode ?: return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        
        val cacheKey = "${selectedLine.code}_${selectedLine.kind.name}_calendarTypes"
        val cachedTypes = sharedPreferences.getStringSet(cacheKey, null)
        if (!cachedTypes.isNullOrEmpty()) {
            val cachedCalendarTypes = cachedTypes.mapNotNull { ODPTCalendarType.fromRawValue(it) }
            if (cachedCalendarTypes.isNotEmpty()) {
                return cachedCalendarTypes
            }
        }
        
        val availableTypes = fetchAvailableCalendarTypes(selectedOperator)
        val typeStrings = availableTypes.map { it.rawValue }
        sharedPreferences.edit { putStringSet(cacheKey, typeStrings.toSet()) }
        
        val lineCacheKey = "${goorback}line${lineIndex + 1}_calendarTypes"
        sharedPreferences.edit { putStringSet(lineCacheKey, typeStrings.toSet()) }
        
        if (availableTypes.isEmpty()) {
            return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
        }
        return availableTypes
    }
    
    // Fetch available calendar types from ODPT/GTFS API for the selected line
    private suspend fun fetchAvailableCalendarTypes(dataSource: LocalDataSource): List<ODPTCalendarType> {
        val selectedLine = this.selectedLine ?: return emptyList()
        
        val apiTypeName = if (selectedLine.kind == TransportationLineKind.BUS) "BusTimetable" else "StationTimetable"
        
        if (dataSource.apiType() == ODPTAPIType.GTFS) {
            val routeId = selectedLine.code
            
            if (routeId.isEmpty()) {
                return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
            }
            
            try {
                val calendarTypes = listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
                return calendarTypes
            } catch (e: Exception) {
                android.util.Log.w("SettingsTimetableViewModel", "Failed to fetch GTFS calendar types: ${e.message}", e)
                // Fallback to default calendar types
                return listOf(ODPTCalendarType.Weekday, ODPTCalendarType.SaturdayHoliday)
            }
        }
        
        if (selectedLine.kind == TransportationLineKind.BUS) {
            val apiLink = selectedLine.title?.let { title ->
                "${dataSource.apiLink(APIDataType.TIMETABLE, TransportationKind.BUS)}&dc:title=$title"
            }
            
            if (apiLink == null) {
                android.util.Log.w("SettingsTimetableViewModel", "$apiTypeName: Missing required information")
                return emptyList()
            }
            
            try {
                val (data, response) = odptService.fetchODPTData(apiLink)
                
                if (response.code != 200) {
                    android.util.Log.w("SettingsTimetableViewModel", "Failed to fetch calendar types - status: ${response.code}")
                    return emptyList()
                }
                
                val json = odptService.parseJSONArray(data)
                
                val foundCalendarTypes = mutableSetOf<String>()
                
                for (timetable in json) {
                    val calendar = timetable["odpt:calendar"] as? String
                    if (calendar != null) {
                        foundCalendarTypes.add(calendar)
                    }
                }
                
                return processCalendarTypes(foundCalendarTypes)
                
            } catch (e: Exception) {
                android.util.Log.w("SettingsTimetableViewModel", "Error fetching calendar types from $apiTypeName: ${e.message}", e)
                return emptyList()
            }
        }
        
        val stationCode = selectedDepartureStop?.code ?: return emptyList()
        
        val actualDirection = selectedLine.lineDirection ?: ""
        val ascendingDirection = selectedLine.ascendingRailDirection ?: actualDirection
        val descendingDirection = selectedLine.descendingRailDirection ?: actualDirection
        
        val allCalendarTypes = mutableSetOf<String>()
        var ascendingSuccess = false
        
        if (ascendingDirection.isNotEmpty()) {
            val ascendingLink = "${dataSource.apiLink(APIDataType.STOP_TIMETABLE)}&odpt:station=$stationCode&odpt:railDirection=$ascendingDirection"
            
            try {
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
                    android.util.Log.w("SettingsTimetableViewModel", "Failed to fetch calendar types from ascending direction - status: ${response.code}")
                }
            } catch (e: Exception) {
                android.util.Log.w("SettingsTimetableViewModel", "Error fetching calendar types from ascending direction: ${e.message}", e)
            }
        }
        
        if (!ascendingSuccess && descendingDirection.isNotEmpty() && descendingDirection != ascendingDirection) {
            val descendingLink = "${dataSource.apiLink(APIDataType.STOP_TIMETABLE)}&odpt:station=$stationCode&odpt:railDirection=$descendingDirection"
            
            try {
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
                    android.util.Log.w("SettingsTimetableViewModel", "Failed to fetch calendar types from descending direction - status: ${response.code}")
                }
            } catch (e: Exception) {
                android.util.Log.w("SettingsTimetableViewModel", "Error fetching calendar types from descending direction: ${e.message}", e)
            }
        }
        
        return processCalendarTypes(allCalendarTypes)
    }
    
    // Convert calendar type strings to ODPTCalendarType, removing duplicates while preserving specific types
    private fun processCalendarTypes(foundCalendarTypes: Set<String>): List<ODPTCalendarType> {
        val allTypes = foundCalendarTypes.mapNotNull { ODPTCalendarType.fromRawValue(it) }
        
        val uniqueTypes = mutableListOf<ODPTCalendarType>()
        val seenDisplayTypes = mutableSetOf<ODPTCalendarType>()
        
        for (type in allTypes) {
            if (type is ODPTCalendarType.Specific) {
                uniqueTypes.add(type)
                seenDisplayTypes.add(type.displayCalendarType())
            }
        }
        
        for (type in allTypes) {
            if (type is ODPTCalendarType.Specific) {
                continue
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
    
    // Process bus timetable data for specified calendar type from ODPT or GTFS
    private suspend fun processBusTimetableData(calendarType: ODPTCalendarType): List<TransportationTime> {
        val selectedLine = this.selectedLine ?: return emptyList()
        val operatorCode = selectedLine.operatorCode ?: return emptyList()
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return emptyList()
        
        if (selectedOperator.apiType() == ODPTAPIType.GTFS) {
            val departureStop = selectedDepartureStop ?: return emptyList()
            val arrivalStop = selectedArrivalStop ?: return emptyList()
            val departureStopId = departureStop.busStopPole ?: departureStop.code ?: return emptyList()
            val arrivalStopId = arrivalStop.busStopPole ?: arrivalStop.code ?: return emptyList()
            
            try {
                val busTimes = gtfsService.fetchGTFSTimetableForRoute(
                    routeId = selectedLine.code,
                    departureStopId = departureStopId,
                    arrivalStopId = arrivalStopId,
                    calendarType = calendarType,
                    transportOperator = selectedOperator,
                    consumerKey = consumerKey
                )
                return busTimes
            } catch (e: Exception) {
                android.util.Log.w("SettingsTimetableViewModel", "Failed to fetch GTFS bus timetable: ${e.message}", e)
                return emptyList()
            }
        }
        
        val busTimetableData = fetchBusTimetableData(calendarType)
        val totalTimetables = busTimetableData.size
        if (totalTimetables == 0) {
            return emptyList()
        }
        
        val transportationTimes = mutableListOf<TransportationTime>()
        
        var totalObjects = 0
        var matchedDeparture = 0
        var matchedArrival = 0
        for (timetable in busTimetableData) {
            val busTimetableObjects = timetable["odpt:busTimetableObject"] as? List<*>
                ?: continue
            totalObjects += busTimetableObjects.size
            
            var departureTime: String? = null
            var arrivalTime: String? = null
            
            for (timetableObject in busTimetableObjects) {
                val timetableObj = timetableObject as? Map<*, *> ?: continue
                val currentBusStopPole = timetableObj["odpt:busstopPole"] as? String ?: ""
                val currentDepartureTime = timetableObj["odpt:departureTime"] as? String ?: ""
                val currentArrivalTime = timetableObj["odpt:arrivalTime"] as? String ?: ""
                
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
            
            if (departureTime != null && arrivalTime != null) {
                val depMinutes = departureTime.timeToMinutes
                val arrMinutes = arrivalTime.timeToMinutes
                if (arrMinutes > depMinutes) {
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
    
    // Fetch bus timetable data from ODPT API for specified calendar type
    private suspend fun fetchBusTimetableData(calendarType: ODPTCalendarType): List<Map<*, *>> {
        val selectedLine = this.selectedLine ?: return emptyList()
        val operatorCode = selectedLine.operatorCode ?: return emptyList()
        val selectedLineTitle = selectedLine.title ?: return emptyList()
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return emptyList()
        
        if (selectedOperator.apiType() == ODPTAPIType.GTFS) {
            return emptyList()
        }
        
        val apiLink = "${selectedOperator.apiLink(APIDataType.TIMETABLE, TransportationKind.BUS)}&dc:title=$selectedLineTitle&odpt:calendar=${calendarType.rawValue}"
        
        try {
            // Use challengeKey for CHALLENGE API type, otherwise use consumerKey
            val (data, response) = odptService.fetchODPTData(apiLink)
            if (response.code != 200) {
                android.util.Log.w("SettingsTimetableViewModel", "fetchBusTimetableData: HTTP ${response.code}")
                return emptyList()
            }
            val json = odptService.parseJSONArray(data)
            return json.map { it }
        } catch (e: Exception) {
            android.util.Log.w("SettingsTimetableViewModel", "Error fetching bus timetable data: ${e.message}", e)
            return emptyList()
        }
    }
    
    // Process train timetable data for specified calendar type, trying both directions
    private suspend fun processTrainTimetableData(calendarType: ODPTCalendarType): List<TransportationTime> {
        val selectedLine = this.selectedLine ?: return emptyList()
        
        val actualDirection = selectedLine.lineDirection ?: ""
        val ascendingDirection = selectedLine.ascendingRailDirection ?: actualDirection
        val descendingDirection = selectedLine.descendingRailDirection ?: actualDirection
        
        val directions = listOf(ascendingDirection, descendingDirection)
        val directionResults = mutableListOf<List<TransportationTime>>()
        
        for (direction in directions) {
            val trainTimetableData = fetchTrainTimetableData(calendarType, direction)
            
            val transportationTimes = mutableListOf<TransportationTime>()
            
            for (timetable in trainTimetableData) {
                val trainNumber = timetable["odpt:trainNumber"] as? String ?: continue
                val trainType = timetable["odpt:trainType"] as? String ?: continue
                
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
                    
                    val departureStop = timetableObj["odpt:departureStation"] as? String
                    if (departureStop == selectedDepartureStop?.code) {
                        departureTime = timetableObj["odpt:departureTime"] as? String
                    }
                    
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
                
                if (departureTime != null && arrivalTime != null) {
                    val depMinutes = departureTime.timeToMinutes
                    val arrMinutes = arrivalTime.timeToMinutes
                    if (arrMinutes > depMinutes) {
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
        }
        
        val validResults = directionResults.filter { it.isNotEmpty() }
        
        val result = if (validResults.isEmpty()) {
            emptyList()
        } else if (validResults.size == 1) {
            validResults[0]
        } else {
            val selectedResult = validResults.minByOrNull {
                if (it.isEmpty()) Int.MAX_VALUE else it.sumOf { time -> time.rideTime } / it.size
            } ?: emptyList()
            selectedResult
        }
        return result
    }
    
    // Fetch train timetable data from ODPT API for specified calendar type and direction
    private suspend fun fetchTrainTimetableData(calendarType: ODPTCalendarType, direction: String): List<Map<*, *>> {
        val selectedLine = this.selectedLine ?: return emptyList()
        val operatorCode = selectedLine.operatorCode ?: return emptyList()
        val selectedLineCode = selectedLine.code
        val selectedOperator = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
            ?: return emptyList()
        
        if (selectedOperator.apiType() == ODPTAPIType.CHALLENGE && challengeKey.isEmpty()) {
            android.util.Log.w("SettingsTimetableViewModel", "challengeKey is empty for CHALLENGE API type!")
        }
        var apiLink = "${selectedOperator.apiLink(APIDataType.TIMETABLE)}&odpt:railway=$selectedLineCode&odpt:calendar=${calendarType.rawValue}"
        if (direction.isNotEmpty()) {
            apiLink += "&odpt:railDirection=$direction"
        }
        
        try {

            val (data, response) = odptService.fetchODPTData(apiLink)
            
            if (response.code != 200) {
                android.util.Log.w("SettingsTimetableViewModel", "HTTP error ${response.code} for train timetable: $apiLink")
                return emptyList()
            }
            val json = odptService.parseJSONArray(data)
            return json.map { it }
        } catch (e: Exception) {
            android.util.Log.w("SettingsTimetableViewModel", "Error fetching train timetable data: ${e.message}", e)
            return emptyList()
        }
    }
    
    // Save timetable data to SharedPreferences grouped by hour
    private fun saveTimetableToUserDefaults(transportationTimes: List<TransportationTime>, calendarType: ODPTCalendarType) {
        clearTimetableDataForRoute(calendarType, goorback, lineIndex + 1)
        
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
        
        for ((hour, transportationTimesForHour) in hourlyTransportationTimes) {
            val sortedTransportationTimes = transportationTimesForHour.sortedBy { it.departureTime }
            goorback.saveTransportationTimes(sortedTransportationTimes, calendarType, lineIndex, hour, sharedPreferences)
        }
        
        val allTransportationTimes = hourlyTransportationTimes.values.flatten()
        goorback.saveTrainTypeList(allTransportationTimes, calendarType, lineIndex, sharedPreferences)
        
        sharedPreferences.edit {
        }
    }
    
    // Save all timetable data for each calendar type and update cache
    suspend fun finalizeTimetableData(calendarTimes: Map<ODPTCalendarType, List<TransportationTime>>) {
        for ((calendarType, times) in calendarTimes) {
            saveTimetableToUserDefaults(times, calendarType)
        }
        
        lineViewModel?.saveAllDataToUserDefaults()
        
        val mergedRepresentativeTypes = calendarTimes.keys.toList()
        val lineCacheKey = "${goorback}line${lineIndex + 1}_calendarTypes"
        val typeStrings = mergedRepresentativeTypes.map { it.rawValue }
        sharedPreferences.edit { putStringSet(lineCacheKey, typeStrings.toSet()) }
    }
    
    // Clear timetable data for specified route, line number, and calendar type
    private fun clearTimetableDataForRoute(calendarType: ODPTCalendarType, goorback: String, lineNumber: Int) {
        for (hour in 4..25) {
            val timetableKey = goorback.timetableKey(calendarType, lineNumber - 1, hour)
            val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarType, lineNumber - 1, hour)
            val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarType, lineNumber - 1, hour)
            
            sharedPreferences.edit {
                remove(timetableKey)
                    .remove(timetableRideTimeKey)
                    .remove(timetableTrainTypeKey)
            }
        }
        
        val trainTypeListKey = goorback.trainTypeListKey(calendarType, lineNumber - 1)
        sharedPreferences.edit { remove(trainTypeListKey) }
    }
}

