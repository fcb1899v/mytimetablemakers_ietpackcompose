package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.models.*
import com.mytimetablemaker.services.CacheStore
import com.mytimetablemaker.ui.theme.AccentString
import com.mytimetablemaker.extensions.busRouteEnglishName
import com.mytimetablemaker.extensions.fromString
import com.mytimetablemaker.services.GTFSDataService
import com.mytimetablemaker.services.ODPTDataService
import com.mytimetablemaker.services.ODPTParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import com.google.gson.JsonParser
import com.mytimetablemaker.BuildConfig
import com.mytimetablemaker.R

// ViewModel for line settings, data loading, and persistence.
class SettingsLineViewModel(
    application: Application,
    private val sharedPreferences: SharedPreferences,
    goorback: String = "back1",
    lineIndex: Int = 0
) : AndroidViewModel(application), SettingsLineViewModelInterface {
    
    // Configuration properties.
    override val goorback: String
    override val lineIndex: Int
    private val consumerKey: String = BuildConfig.ODPT_ACCESS_TOKEN
    
    init {
        if (consumerKey.isEmpty()) {
            android.util.Log.w(
                "SettingsLineViewModel",
                "ODPT_ACCESS_TOKEN is not set; GTFS operators requiring auth will fail."
            )
        }
    }
    
    // UI state properties.
    val operatorInput = MutableStateFlow("")
    val operatorSuggestions = MutableStateFlow<List<String>>(emptyList())
    val showOperatorSuggestions = MutableStateFlow(false)
    val operatorSelected = MutableStateFlow(false)
    val showOperatorSelection = MutableStateFlow(false)
    val selectedOperatorCode = MutableStateFlow<String?>(null)
    val isOperatorFieldFocused = MutableStateFlow(false)
    
    val lineInput = MutableStateFlow("")
    private val _lineSuggestions = MutableStateFlow<List<TransportationLine>>(emptyList())
    val lineSuggestionsState: StateFlow<List<TransportationLine>> = _lineSuggestions.asStateFlow()
    override val lineSuggestions: List<TransportationLine>
        get() = _lineSuggestions.value
    val isLoadingBusStops = MutableStateFlow(false)
    override var isLoadingTimetable = false
    override var loadingMessage: String? = null
    val isLoadingLines = MutableStateFlow(false)
    val showColorSelection = MutableStateFlow(false)
    val showStationSelection = MutableStateFlow(false)
    
    // Line and station selection state.
    private val _selectedLine = MutableStateFlow<TransportationLine?>(null)
    val selectedLineState: StateFlow<TransportationLine?> = _selectedLine.asStateFlow()
    override val selectedLine: TransportationLine?
        get() = _selectedLine.value
    
    val lineStations = MutableStateFlow<List<TransportationStop>>(emptyList())
    val lineBusStops = MutableStateFlow<List<TransportationStop>>(emptyList())
    
    private val _lineStops = MutableStateFlow<List<TransportationStop>>(emptyList())
    val lineStopsState: StateFlow<List<TransportationStop>> = _lineStops.asStateFlow()
    override val lineStops: List<TransportationStop>
        get() = _lineStops.value
    
    private val _selectedDepartureStop = MutableStateFlow<TransportationStop?>(null)
    val selectedDepartureStopState: StateFlow<TransportationStop?> = _selectedDepartureStop.asStateFlow()
    override val selectedDepartureStop: TransportationStop?
        get() = _selectedDepartureStop.value
    
    // Set selected departure stop.
    fun setSelectedDepartureStop(stop: TransportationStop?) {
        _selectedDepartureStop.value = stop
    }
    
    private val _selectedArrivalStop = MutableStateFlow<TransportationStop?>(null)
    val selectedArrivalStopState: StateFlow<TransportationStop?> = _selectedArrivalStop.asStateFlow()
    override val selectedArrivalStop: TransportationStop?
        get() = _selectedArrivalStop.value
    
    // Set selected arrival stop.
    fun setSelectedArrivalStop(stop: TransportationStop?) {
        _selectedArrivalStop.value = stop
    }
    
    // User input fields.
    val departureStopInput = MutableStateFlow("")
    val arrivalStopInput = MutableStateFlow("")
    
    private val selectedRideTimeFlow = MutableStateFlow(0)
    val selectedRideTimeState: StateFlow<Int> = selectedRideTimeFlow.asStateFlow()
    fun setSelectedRideTime(value: Int) {
        selectedRideTimeFlow.value = value
    }
    
    // Suggestion and focus state.
    val showDepartureSuggestions = MutableStateFlow(false)
    val departureSuggestions = MutableStateFlow<List<TransportationStop>>(emptyList())
    val isDepartureFieldFocused = MutableStateFlow(false)
    val showArrivalSuggestions = MutableStateFlow(false)
    val arrivalSuggestions = MutableStateFlow<List<TransportationStop>>(emptyList())
    val isArrivalFieldFocused = MutableStateFlow(false)
    val showLineSuggestions = MutableStateFlow(false)
    val isLineFieldFocused = MutableStateFlow(false)
    
    // Selection flags for suggestion control.
    val departureStopSelected = MutableStateFlow(false)
    val arrivalStopSelected = MutableStateFlow(false)
    val lineSelected = MutableStateFlow(false)
    
    // Line configuration state.
    val selectedLineColor = MutableStateFlow<String?>(null)
    val selectedTransportationKind = MutableStateFlow(TransportationLineKind.RAILWAY)
    val selectedTransferTime = MutableStateFlow(0)
    val selectedTransportation = MutableStateFlow("none")
    
    private val _selectedLineNumber = MutableStateFlow(1)
    val selectedLineNumberState: StateFlow<Int> = _selectedLineNumber.asStateFlow()
    override val selectedLineNumber: Int
        get() = _selectedLineNumber.value
    
    val availableLineNumbers = MutableStateFlow(listOf(1))
    val isLineNumberChanging = MutableStateFlow(false)
    val isGoOrBackChanging = MutableStateFlow(false)
    val isChangedOperator = MutableStateFlow(false)
    
    private val _selectedGoorback = MutableStateFlow("back1")
    val selectedGoorbackState: StateFlow<String> = _selectedGoorback.asStateFlow()
    // Computed properties.
    val selectedGoorback: String
        get() = _selectedGoorback.value

    val hasSelectedLine: Boolean
        get() = _selectedLine.value != null
    
    val hasStops: Boolean
        get() = _lineStops.value.isNotEmpty()
    
    // All required fields filled.
    val isAllNotEmpty: Boolean
        get() {
            val operatorFilled = operatorInput.value.trim().isNotEmpty()
            val lineFilled = lineInput.value.trim().isNotEmpty()
            val departureFilled = departureStopInput.value.trim().isNotEmpty()
            val arrivalFilled = arrivalStopInput.value.trim().isNotEmpty()
            val rideTimeFilled = selectedRideTimeFlow.value > 0
            val result = operatorFilled && lineFilled && departureFilled && arrivalFilled && rideTimeFilled
            return result
        }
    
    
    // Localized display names for directions.
    val goorbackDisplayNames: Map<String, String>
        get() {
            val context = getApplication<Application>()
            return goorbackDisplayNamesRaw.mapValues { (_, value) ->
                when (value) {
                    "returnRoute1" -> context.getString(R.string.returnRoute1)
                    "returnRoute2" -> context.getString(R.string.returnRoute2)
                    "outboundRoute1" -> context.getString(R.string.outboundRoute1)
                    "outboundRoute2" -> context.getString(R.string.outboundRoute2)
                    else -> value
                }
            }
        }
    
    // Internal data storage.
    private var all: List<TransportationLine> = emptyList()
    private var allData: List<TransportationLine> = emptyList()
    val railwayLines = MutableStateFlow<List<TransportationLine>>(emptyList())
    val busLines = MutableStateFlow<List<TransportationLine>>(emptyList())
    val nameCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    
    
    // Convert line kind to transportation kind.
    private fun TransportationLineKind.toTransportationKind(): TransportationKind {
        return when (this) {
            TransportationLineKind.RAILWAY -> TransportationKind.RAILWAY
            TransportationLineKind.BUS -> TransportationKind.BUS
        }
    }
    
    // Services.
    private val cache = CacheStore(getApplication())
    private val odptService = ODPTDataService(getApplication())
    private val gtfsService = GTFSDataService(getApplication())
    
    // Timetable ViewModel reference.
    val timetableViewModel: SettingsTimetableViewModel
    
    init {
        // Validate goorback value and use default if invalid.
        val validGoorback = if (goorback.isEmpty() || !goorbackList.contains(goorback)) "back1" else goorback
        this.goorback = validGoorback
        this.lineIndex = lineIndex
        this._selectedGoorback.value = validGoorback
        
        // Initialize timetable ViewModel.
        this.timetableViewModel = SettingsTimetableViewModel(getApplication(), sharedPreferences, this)
        
        // Prevent filtering during initial setup.
        this.isLineNumberChanging.value = true
        
        _selectedLineNumber.value = lineIndex + 1
        
        // Initialize transfer count if missing.
        if (!sharedPreferences.contains(_selectedGoorback.value.changeLineKey())) {
            sharedPreferences.edit { putInt(_selectedGoorback.value.changeLineKey(), 0) }
        }
        
        updateAvailableLineNumbers(shouldPreserveLineNumber = false)
        loadSettingsForSelectedLine()
        
        // Load data from cache in background.
        viewModelScope.launch {
            try {
                loadFromCache()
            } catch (e: Exception) {
                android.util.Log.e("SettingsLineViewModel", "init: loadFromCache failed", e)
            }
        }
    }
    
    // Localized display name for a line.
    fun lineDisplayName(line: TransportationLine): String = line.displayName()

    // Whether the selected operator supports timetables. True/false is determined only by LocalDataSource.hasTrainTimeTable() (Enums.kt).
    fun hasTimetableSupport(): Boolean {
        val operatorCode = selectedOperatorCode.value ?: return false
        val dataSource = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode } ?: return false
        return dataSource.hasTrainTimeTable()
    }
    
    // Load data from cache, LineData, or API.
    private suspend fun loadFromCache() = withContext(Dispatchers.IO) {
        val kind = selectedTransportationKind.value.toTransportationKind()
        val loadedLines = mutableListOf<TransportationLine>()
        
        val isBus = kind == TransportationKind.BUS
        
        // Load data from cache for all operators of the selected kind.
        for (transportOperator in LocalDataSource.entries) {
            if (transportOperator.transportationType() != kind) continue
            
            // Handle GTFS operators separately.
            if (transportOperator.apiType() == ODPTAPIType.GTFS) {
                continue
            }
            
            val cacheKey = transportOperator.fileName()
            
            var cachedData = cache.loadData(cacheKey)
            
            // If cache is not available, try LineData directory.
            if (cachedData == null) {
                val fileManager = getApplication<Application>().filesDir
                val lineDataDirectory = java.io.File(fileManager, "LineData")
                val file = java.io.File(lineDataDirectory, cacheKey)
                
                if (file.exists() && file.isFile) {
                    try {
                        cachedData = java.io.FileInputStream(file).use { it.readBytes() }
                    } catch (e: Exception) {
                        if (isBus) {
                            android.util.Log.w(
                                "SettingsLineViewModel",
                                "loadFromCache: failed to load bus data for ${transportOperator.name}",
                                e
                            )
                        } else {
                            android.util.Log.w(
                                "SettingsLineViewModel",
                                "loadFromCache: failed to load data for ${transportOperator.name}",
                                e
                            )
                        }
                    }
                }
            }
            
            // If still no data, try to fetch from API.
            if (cachedData == null) {
                try {
                    cachedData = odptService.fetchIndividualOperatorData(transportOperator)
                    
                    // Save to both CacheStore and LineData directory for future use
                    cache.saveData(cachedData, cacheKey)
                    odptService.writeIndividualOperatorDataToFile(cachedData, transportOperator)
                    
                } catch (e: Exception) {
                    if (isBus) {
                        android.util.Log.e(
                            "SettingsLineViewModel",
                            "loadFromCache: failed to fetch bus data for ${transportOperator.name}",
                            e
                        )
                    } else {
                        android.util.Log.e(
                            "SettingsLineViewModel",
                            "loadFromCache: failed to fetch data for ${transportOperator.name}",
                            e
                        )
                    }
                }
            }
            
            if (cachedData != null) {
                try {
                    val lines = if (kind == TransportationKind.RAILWAY) ODPTParser.parseRailwayRoutes(cachedData) else ODPTParser.parseBusRoutes(cachedData)
                    loadedLines.addAll(lines)
                } catch (e: Exception) {
                    if (isBus) {
                        android.util.Log.e(
                            "SettingsLineViewModel",
                            "loadFromCache: failed to parse bus data for ${transportOperator.name}",
                            e
                        )
                    } else {
                        android.util.Log.e(
                            "SettingsLineViewModel",
                            "loadFromCache: failed to parse data for ${transportOperator.name}",
                            e
                        )
                    }
                }
            }
        }
        
        // Update state on main thread.
        withContext(Dispatchers.Main) {
            // Only update lines for the selected kind.
            val kind = selectedTransportationKind.value.toTransportationKind()
            if (kind == TransportationKind.RAILWAY) {
                val newRailwayLines = loadedLines.filter { it.kind == TransportationLineKind.RAILWAY }
                railwayLines.value = newRailwayLines
            } else {
                val newBusLines = loadedLines.filter { it.kind == TransportationLineKind.BUS }
                busLines.value = newBusLines
            }
            
            // Update combined lists.
            all = railwayLines.value + busLines.value
            allData = all
            
            // Restore saved line if available.
            viewModelScope.launch {
                checkSavedLineInData()
                isLineNumberChanging.value = false
                
                if (operatorSelected.value && selectedOperatorCode.value != null && isLineFieldFocused.value && lineInput.value.isEmpty()) {
                    filterLine("", isFocused = true)
                }
            }
        }
    }
    
    // Handle direction changes.
    fun selectGoorback(newGoorback: String) {
        if (_selectedGoorback.value == newGoorback) return
        
        isGoOrBackChanging.value = true
        
        _selectedGoorback.value = newGoorback

        // Update available line numbers, preserving current selection.
        updateAvailableLineNumbers(shouldPreserveLineNumber = true)
        
        loadSettingsForSelectedLine()
        
        // Restore saved data and reset suggestions.
        viewModelScope.launch {
            checkSavedLineInData()
            
            showOperatorSuggestions.value = false
            operatorSuggestions.value = emptyList()
            showDepartureSuggestions.value = false
            showArrivalSuggestions.value = false
            showLineSuggestions.value = false
            isDepartureFieldFocused.value = false
            isArrivalFieldFocused.value = false
            _lineSuggestions.value = emptyList()
            
            kotlinx.coroutines.delay(100)
            isGoOrBackChanging.value = false
        }
    }
    
    // Update available line numbers.
    private fun updateAvailableLineNumbers(shouldPreserveLineNumber: Boolean) {
        val changeLineValue = _selectedGoorback.value.changeLineKey().userDefaultsInt(sharedPreferences, 0)
        val maxLineNumber = minOf(changeLineValue + 1, 3)
        availableLineNumbers.value = (1..maxLineNumber).toList()
        
        // Reset transportation settings beyond current transfer count.
        for (i in (changeLineValue + 2)..4) {
            val transportationKey = _selectedGoorback.value.transportationKey(i)
            sharedPreferences.edit { putString(transportationKey, "none") }
        }
        
        // Only change selectedLineNumber if not preserving it.
        if (!shouldPreserveLineNumber && _selectedLineNumber.value == 1 && lineIndex > 0) {
            _selectedLineNumber.value = minOf(lineIndex + 1, availableLineNumbers.value.lastOrNull() ?: 1)
        }
    }
    
    // Handle line number selection.
    fun selectLineNumber(lineNumber: Int) {
        isLineNumberChanging.value = true
        
        // Hide suggestions during line number change.
        showDepartureSuggestions.value = false
        showArrivalSuggestions.value = false
        showLineSuggestions.value = false
        isDepartureFieldFocused.value = false
        isArrivalFieldFocused.value = false
        _lineSuggestions.value = emptyList()
        
        _selectedLineNumber.value = lineNumber
        
        loadSettingsForSelectedLine()
        
        // Restore saved line if available.
        viewModelScope.launch {
            checkSavedLineInData()
            kotlinx.coroutines.delay(100)
            isLineNumberChanging.value = false
        }
    }
    
    // Load settings for the current line number.
    private fun loadSettingsForSelectedLine() {
        val currentLineIndex = _selectedLineNumber.value - 1
        
        // Load transportation kind first.
        val lineKindKey = _selectedGoorback.value.lineKindKey(currentLineIndex)
        val savedKindString = sharedPreferences.getString(lineKindKey, null)
        selectedTransportationKind.value = if (savedKindString != null) savedKindString.fromString() ?: TransportationLineKind.RAILWAY else TransportationLineKind.RAILWAY
        
        // Load operator name.
        val operatorNameKey = _selectedGoorback.value.operatorNameKey(currentLineIndex)
        val savedOperatorName = sharedPreferences.getString(operatorNameKey, null)
        
        if (!savedOperatorName.isNullOrEmpty()) {
            operatorInput.value = savedOperatorName

            // Only restore selection if operator code was saved.
            val savedOperatorCode = _selectedGoorback.value.operatorCode(sharedPreferences, currentLineIndex)
            if (savedOperatorCode.isNotEmpty()) {
                selectedOperatorCode.value = savedOperatorCode
                operatorSelected.value = true

                // Load operator line list for GTFS bus routes.
                val dataSourceFromCode = LocalDataSource.entries.firstOrNull { it.operatorCode() == savedOperatorCode }
                if (dataSourceFromCode != null && selectedTransportationKind.value == TransportationLineKind.BUS &&
                    dataSourceFromCode.apiType() == ODPTAPIType.GTFS) {
                    viewModelScope.launch {
                        try {
                            fetchGTFSLinesForOperator(dataSourceFromCode)
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "SettingsLineViewModel",
                                "loadSettingsForSelectedLine: failed to fetch GTFS lines for ${dataSourceFromCode.name}",
                                e
                            )
                            // Fallback to SharedPreferences if fetch fails
                            _selectedGoorback.value.loadOperatorLineList(currentLineIndex, sharedPreferences)?.let { savedLineList ->
                                _lineSuggestions.value = savedLineList
                                showLineSuggestions.value = false
                            }
                        }
                    }
                }
            } else {
                // Name is saved but not selected; keep input and clear selection state.
                selectedOperatorCode.value = null
                operatorSelected.value = false
            }
        } else {
            // Restore operator code if operator name is not saved.
            val savedOperatorCode = _selectedGoorback.value.operatorCode(sharedPreferences, currentLineIndex)
            if (savedOperatorCode.isNotEmpty()) {
                selectedOperatorCode.value = savedOperatorCode
                operatorSelected.value = true
                
                // Fetch GTFS lines from cache.
                val dataSource = LocalDataSource.entries.firstOrNull { it.operatorCode() == savedOperatorCode }
                if (dataSource != null && selectedTransportationKind.value == TransportationLineKind.BUS && 
                    dataSource.apiType() == ODPTAPIType.GTFS) {
                    viewModelScope.launch {
                        try {
                            fetchGTFSLinesForOperator(dataSource)
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "SettingsLineViewModel",
                                "loadSettingsForSelectedLine: failed to fetch GTFS lines for ${dataSource.name}",
                                e
                            )
                            // Fallback to SharedPreferences if fetch fails
                            _selectedGoorback.value.loadOperatorLineList(currentLineIndex, sharedPreferences)?.let { savedLineList ->
                                _lineSuggestions.value = savedLineList
                                showLineSuggestions.value = false
                            }
                        }
                    }
                }
            } else {
                // If operator name and code are not saved, clear operator input.
                operatorInput.value = ""
                selectedOperatorCode.value = null
                operatorSelected.value = false
            }
        }
        
        // Load line name and restore line object.
        val context = getApplication<Application>()
        val savedLineName = _selectedGoorback.value.settingsLineName(sharedPreferences, currentLineIndex, context)
        val notSet = context.getString(R.string.notSet)
        
        if (savedLineName != notSet && savedLineName.isNotEmpty()) {
            lineInput.value = savedLineName
            
            // Restore line object from line name if data is available.
            if (all.isNotEmpty()) {
                findSavedLineInData()?.let { foundLine ->
                    _selectedLine.value = foundLine
                    lineSelected.value = _selectedGoorback.value.lineSelected(sharedPreferences, currentLineIndex)
                    
                    // Load line direction from SharedPreferences.
                    val savedLineDirection = _selectedGoorback.value.lineDirection(sharedPreferences, currentLineIndex)
                    if (savedLineDirection.isNotEmpty() && foundLine.lineDirection == null) {
                        _selectedLine.value = foundLine.copy(lineDirection = savedLineDirection)
                    }
                    
                    // Load line stop list from SharedPreferences for bus routes.
                    if (selectedTransportationKind.value == TransportationLineKind.BUS) {
                        _selectedGoorback.value.loadLineStopList(currentLineIndex, sharedPreferences)?.let { savedStopList ->
                            _lineStops.value = savedStopList
                            // Also update lineBusStops based on loaded stops
                            lineBusStops.value = savedStopList

                            val needsJapaneseNames = savedStopList.any { stop ->
                                val hasJapaneseInNote = stop.note?.containsJapanese() == true
                                val hasJapaneseInTitle = stop.title?.ja?.containsJapanese() == true
                                (!hasJapaneseInNote && !hasJapaneseInTitle) && !stop.busStopPole.isNullOrEmpty()
                            }
                            if (needsJapaneseNames) {
                                viewModelScope.launch {
                                    isLoadingBusStops.value = true
                                    loadingMessage = getApplication<Application>().getString(R.string.loadingBusStops)
                                    try {
                                        fetchJapaneseNamesForAllBusStops()
                                    } finally {
                                        isLoadingBusStops.value = false
                                        loadingMessage = null
                                    }
                                }
                            }
                        } ?: run {
                            // If no saved stop list, set up line stops from line data
                            setupLineStops(foundLine)
                        }
                    } else {
                        // If not bus, set up line stops from line data
                        setupLineStops(foundLine)
                    }
                } ?: run {
                    _selectedLine.value = null
                    lineStations.value = emptyList()
                    lineBusStops.value = emptyList()
                    _lineStops.value = emptyList()
                    lineSelected.value = _selectedGoorback.value.lineSelected(sharedPreferences, currentLineIndex)
                }
            } else {
                // If all data is not loaded yet, try to load line stop list from SharedPreferences for bus routes
                if (selectedTransportationKind.value == TransportationLineKind.BUS) {
                    _selectedGoorback.value.loadLineStopList(currentLineIndex, sharedPreferences)?.let { savedStopList ->
                        _lineStops.value = savedStopList
                        // Also update lineBusStops based on loaded stops
                        lineBusStops.value = savedStopList
                    }
                }
            }
        } else {
            lineInput.value = ""
            _selectedLine.value = null
            lineStations.value = emptyList()
            lineBusStops.value = emptyList()
            _lineStops.value = emptyList()
            lineSelected.value = _selectedGoorback.value.lineSelected(sharedPreferences, currentLineIndex)
        }
        
        // Load line color using settingsLineColorString function
        val savedLineColorString = _selectedGoorback.value.settingsLineColorString(sharedPreferences, currentLineIndex)
        val grayString = "#9C9C9C"
        selectedLineColor.value = if (savedLineColorString != grayString) savedLineColorString else null
        
        // Load departure station name and restore station object from station name
        // Split by ":" and return first component for ODPT format
        val savedDeparture = _selectedGoorback.value.departStation(sharedPreferences, currentLineIndex)
        val savedDepartureCode = _selectedGoorback.value.departStationCode(sharedPreferences, currentLineIndex)
        
        if (savedDeparture.isNotEmpty()) {
            // Split by ":" and return first component for ODPT format
            val components = savedDeparture.split(":")
            val displayDeparture = components.firstOrNull()?.trim() ?: savedDeparture
            departureStopInput.value = displayDeparture
            
            // Restore departure station object from station name if line stops are available
            if (lineStops.isNotEmpty()) {
                val foundStop = lineStops.firstOrNull { stop ->
                    stop.code == savedDepartureCode ||
                    stop.name == savedDeparture ||
                    stop.name == displayDeparture ||
                    stop.title?.ja == savedDeparture ||
                    stop.title?.ja == displayDeparture ||
                    stop.title?.en == savedDeparture ||
                    stop.title?.en == displayDeparture ||
                    stop.displayName() == displayDeparture
                }
                _selectedDepartureStop.value = foundStop
            } else {
                _selectedDepartureStop.value = null
            }
        } else {
            departureStopInput.value = ""
            _selectedDepartureStop.value = null
        }
        
        // Load arrival station name and restore station object from station name
        // Split by ":" and return first component for ODPT format
        val savedArrival = _selectedGoorback.value.arriveStation(sharedPreferences, currentLineIndex)
        val savedArrivalCode = _selectedGoorback.value.arriveStationCode(sharedPreferences, currentLineIndex)
        
        if (savedArrival.isNotEmpty()) {
            // Split by ":" and return first component for ODPT format
            val components = savedArrival.split(":")
            val displayArrival = components.firstOrNull()?.trim() ?: savedArrival
            arrivalStopInput.value = displayArrival
            
            // Restore arrival station object from station name or code if line stops are available
            if (lineStops.isNotEmpty()) {
                val foundStop = lineStops.firstOrNull { stop ->
                    stop.code == savedArrivalCode ||
                    stop.name == savedArrival ||
                    stop.name == displayArrival ||
                    stop.title?.ja == savedArrival ||
                    stop.title?.ja == displayArrival ||
                    stop.title?.en == savedArrival ||
                    stop.title?.en == displayArrival ||
                    stop.displayName() == displayArrival
                }
                _selectedArrivalStop.value = foundStop
            } else {
                _selectedArrivalStop.value = null
            }
        } else {
            arrivalStopInput.value = ""
            _selectedArrivalStop.value = null
        }
        
        // Load ride time
        val rideTimeKey = _selectedGoorback.value.rideTimeKey(currentLineIndex)
        val savedRideTime = rideTimeKey.userDefaultsInt(sharedPreferences, 0)
        selectedRideTimeFlow.value = if (savedRideTime > 0) savedRideTime else 0
        
        // Load transfer settings.
        if (_selectedLineNumber.value < 3) {
            val transportationKey = _selectedGoorback.value.transportationKey(currentLineIndex + 2)
            val savedTransportation = sharedPreferences.getString(transportationKey, null)
            selectedTransportation.value = if (!savedTransportation.isNullOrEmpty()) savedTransportation else "none"
            
            val transferTimeKey = _selectedGoorback.value.transferTimeKey(currentLineIndex + 2)
            val savedTransferTime = transferTimeKey.userDefaultsInt(sharedPreferences, 0)
            selectedTransferTime.value = if (savedTransferTime > 0) savedTransferTime else 0
        } else {
            selectedTransportation.value = "none"
            selectedTransferTime.value = 0
        }
    }
    
    // Restore saved line if present in data.
    private suspend fun checkSavedLineInData() = withContext(Dispatchers.Main) {
        // Wait for data loading to complete.
        while (all.isEmpty()) {
            kotlinx.coroutines.delay(100)
        }
        
        // Restore station information when possible.
        if (lineInput.value.trim().isNotEmpty()) {
            findSavedLineInData()?.let { foundLine ->
                _selectedLine.value = foundLine
                showStationSelection.value = true
                lineSelected.value = true
                
                setupLineStops(foundLine)
                
                showColorSelection.value = false
                
                selectedTransportationKind.value = foundLine.kind
            } ?: run {
                _selectedLine.value = null
                lineStations.value = emptyList()
                showStationSelection.value = false
                lineSelected.value = false
            }
        }
        
        // Always load station settings.
        loadStationSettings()
    }
    
    // Find saved line by code, then by name.
    private fun findSavedLineInData(): TransportationLine? {
        val currentLineIndex = _selectedLineNumber.value - 1
        val lineCodeKey = _selectedGoorback.value.lineCodeKey(currentLineIndex)
        
        // Try to find by line code first (more reliable)
        val savedLineCode = sharedPreferences.getString(lineCodeKey, null)
        if (!savedLineCode.isNullOrEmpty()) {
            all.firstOrNull { line ->
                // Compare with lineCode property if available
                line.lineCode == savedLineCode || 
                // Otherwise, extract from code and compare
                line.code.split(".").lastOrNull() == savedLineCode
            }?.let { return it }
        }
        
        // Fall back to finding by line name
        return all.firstOrNull { line ->
            if (line.kind == TransportationLineKind.BUS) {
                line.name == lineInput.value ||
                line.railwayTitle?.getLocalizedName(fallbackTo = line.name) == lineInput.value ||
                line.busRouteEnglishName() == lineInput.value
            } else {
                line.name == lineInput.value || 
                line.railwayTitle?.getLocalizedName(fallbackTo = line.name) == lineInput.value
            }
        }
    }
    
    // Helper method to set up line stops after finding a line
    // Handles bus stops and updates lineStops for station selection
    private fun setupLineStops(foundLine: TransportationLine) {
        // Set bus stops for bus routes
        if (foundLine.kind == TransportationLineKind.BUS) {
            foundLine.busStopPoleOrder?.let { busStopPoleOrder ->
                lineBusStops.value = busStopPoleOrder
                
                val busStops = busStopPoleOrder.mapNotNull { busStop ->
                    if (busStop.name.isEmpty()) null
                    else TransportationStop(
                        kind = TransportationLineKind.BUS,
                        name = busStop.name,
                        code = busStop.code,
                        index = busStop.index,
                        lineCode = foundLine.code,
                        title = busStop.title,
                        note = busStop.note,
                        busStopPole = busStop.busStopPole
                    )
                }
                lineStations.value = busStops
                _lineStops.value = getStopsForSelectedLine()
            } ?: run {
                // For GTFS routes, don't fetch bus stops here - they will be loaded when needed
                lineBusStops.value = emptyList()
                lineStations.value = emptyList()
                _lineStops.value = getStopsForSelectedLine()
            }
        } else {
            lineBusStops.value = emptyList()
            _lineStops.value = getStopsForSelectedLine()
        }
    }
    
    // Load station settings from SharedPreferences
    // Split by ":" and return first component for ODPT format
    // Also restore _selectedDepartureStop and _selectedArrivalStop so isAllNotEmpty becomes true when reopening the sheet
    private fun loadStationSettings() {
        val currentLineIndex = _selectedLineNumber.value - 1
        val context = getApplication<Application>()
        val lineStops = _lineStops.value
        val notSet = context.getString(R.string.notSet)
        
        // Use settingsDepartStation (same key as departStation; default "notSet")
        val savedDeparture = _selectedGoorback.value.settingsDepartStation(sharedPreferences, currentLineIndex, context)
        if (savedDeparture != notSet && savedDeparture.isNotEmpty()) {
            val components = savedDeparture.split(":")
            val displayDeparture = components.firstOrNull()?.trim() ?: savedDeparture
            departureStopInput.value = displayDeparture
            // Restore departure stop object so isAllNotEmpty and buttons become active
            if (lineStops.isNotEmpty()) {
                val foundStop = lineStops.firstOrNull { stop ->
                    stop.name == savedDeparture ||
                    stop.name == displayDeparture ||
                    stop.title?.ja == savedDeparture ||
                    stop.title?.ja == displayDeparture ||
                    stop.title?.en == savedDeparture ||
                    stop.title?.en == displayDeparture ||
                    stop.displayName() == displayDeparture
                }
                _selectedDepartureStop.value = foundStop
            } else {
                _selectedDepartureStop.value = null
            }
        } else {
            departureStopInput.value = ""
            _selectedDepartureStop.value = null
        }
        
        // Use settingsArriveStation (same key as arriveStation; default "notSet")
        val savedArrival = _selectedGoorback.value.settingsArriveStation(sharedPreferences, currentLineIndex, context)
        val savedArrivalCode = _selectedGoorback.value.arriveStationCode(sharedPreferences, currentLineIndex)
        if (savedArrival != notSet && savedArrival.isNotEmpty()) {
            val components = savedArrival.split(":")
            val displayArrival = components.firstOrNull()?.trim() ?: savedArrival
            arrivalStopInput.value = displayArrival
            // Restore arrival stop object so isAllNotEmpty and buttons become active
            if (lineStops.isNotEmpty()) {
                val foundStop = lineStops.firstOrNull { stop ->
                    stop.code == savedArrivalCode ||
                    stop.name == savedArrival ||
                    stop.name == displayArrival ||
                    stop.title?.ja == savedArrival ||
                    stop.title?.ja == displayArrival ||
                    stop.title?.en == savedArrival ||
                    stop.title?.en == displayArrival ||
                    stop.displayName() == displayArrival
                }
                _selectedArrivalStop.value = foundStop
            } else {
                _selectedArrivalStop.value = null
            }
        } else {
            arrivalStopInput.value = ""
            _selectedArrivalStop.value = null
        }
    }
    
    // Get stops for the selected line.
    fun getStopsForSelectedLine(): List<TransportationStop> {
        val selectedLine = _selectedLine.value ?: return emptyList()

        if (selectedLine.kind != TransportationLineKind.BUS) {
            // Railway lines use stationOrder or lineStations.
            return if (!selectedLine.stationOrder.isNullOrEmpty()) selectedLine.stationOrder else lineStations.value
        }

        // Bus routes use lineBusStops or busStopPoleOrder.
        return if (lineBusStops.value.isNotEmpty()) {
            lineBusStops.value.map { busStop ->
                val hasJapaneseInNote = busStop.note?.containsJapanese() == true

                if ((!hasJapaneseInNote || busStop.note.isEmpty()) && !busStop.busStopPole.isNullOrEmpty()) {
                    // Japanese name will be fetched later.
                    TransportationStop(
                        kind = TransportationLineKind.BUS,
                        name = busStop.title?.ja ?: busStop.name,
                        code = busStop.code,
                        index = busStop.index,
                        lineCode = busStop.lineCode,
                        title = busStop.title,
                        busStopPole = busStop.busStopPole
                    )
                } else {
                    busStop
                }
            }
        } else {
            selectedLine.busStopPoleOrder ?: emptyList()
        }
    }
    
    // Filter lines by search input.
    suspend fun filterLine(q: String, isFocused: Boolean? = null) = withContext(Dispatchers.Main) {
        val t = q.normalizedForSearch()
        
        // Use provided isFocused parameter or fall back to current state.
        val fieldIsFocused = isFocused ?: isLineFieldFocused.value
        
        // Skip suggestions while switching or when a line is selected.
        if (isLineNumberChanging.value || isGoOrBackChanging.value || lineSelected.value) {
            return@withContext
        }
        
        // Require operator selection from dropdown.
        val operatorCode = selectedOperatorCode.value
        if (operatorCode == null || !operatorSelected.value) {
            _lineSuggestions.value = emptyList()
            nameCounts.value = emptyMap()
            showLineSuggestions.value = false
            return@withContext
        }
        
        // Filter by transportation kind.
        var searchData = if (selectedTransportationKind.value == TransportationLineKind.RAILWAY) railwayLines.value else busLines.value
        
        // Wait for data to load before filtering.
        if (searchData.isEmpty()) {
            var retryCount = 0
            val maxRetries = 20 // 2 seconds max (20 * 100ms)
            while (searchData.isEmpty() && retryCount < maxRetries) {
                kotlinx.coroutines.delay(100)
                retryCount++
                searchData = if (selectedTransportationKind.value == TransportationLineKind.RAILWAY) railwayLines.value else busLines.value
            }
        }
        
        // Filter by selected operator.
        searchData = searchData.filter { it.operatorCode == operatorCode }
        
        // For GTFS operators, use stored lines if operator was not changed.
        val dataSource = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
        if (dataSource != null && dataSource.apiType() == ODPTAPIType.GTFS && !isChangedOperator.value) {
            searchData = _selectedGoorback.value.loadOperatorLineList(_selectedLineNumber.value - 1, sharedPreferences) ?: emptyList()
        }
        
        // If query is empty, show all lines for the operator.
        if (t.isEmpty()) {
            if (selectedTransportationKind.value == TransportationLineKind.BUS) {
                // For GTFS routes, group by lineCode to avoid duplicates.
                val uniqueResults = mutableListOf<TransportationLine>()
                val seenLineCodes = mutableSetOf<String>()
                val seenDisplayNames = mutableSetOf<String>()
                
                for (line in searchData) {
                    // For GTFS routes, use lineCode (route_short_name) for grouping
                    if (!line.lineCode.isNullOrEmpty()) {
                        if (!seenLineCodes.contains(line.lineCode)) {
                            seenLineCodes.add(line.lineCode)
                            uniqueResults.add(line)
                        }
                    } else {
                        // For routes without lineCode, use displayName for grouping
                        val displayName = lineDisplayName(line)
                        if (!seenDisplayNames.contains(displayName)) {
                            seenDisplayNames.add(displayName)
                            uniqueResults.add(line)
                        }
                    }
                }
                
                _lineSuggestions.value = uniqueResults
                showLineSuggestions.value = fieldIsFocused && _lineSuggestions.value.isNotEmpty()
                nameCounts.value = emptyMap()
            } else {
                val uniqueResults = removeDuplicates(searchData)
                _lineSuggestions.value = uniqueResults
                showLineSuggestions.value = fieldIsFocused && _lineSuggestions.value.isNotEmpty()
                nameCounts.value = _lineSuggestions.value.groupBy { lineDisplayName(it) }
                    .mapValues { it.value.size }
            }
            return@withContext
        }
        
        // Search key generation helper for different transportation types
        val key: (TransportationLine) -> String = { p ->
            if (p.kind == TransportationLineKind.BUS) {
                p.name.normalizedForSearch()
            } else {
                p.railwayTitle?.getLocalizedName(fallbackTo = p.name)?.normalizedForSearch() 
                    ?: p.name.normalizedForSearch()
            }
        }
        
        // Simplified search for bus data to improve performance
        if (selectedTransportationKind.value == TransportationLineKind.BUS) {
            val starts = searchData.filter { lineDisplayName(it).normalizedForSearch().startsWith(t) }
            val contains = searchData.filter { 
                val displayName = lineDisplayName(it).normalizedForSearch()
                !displayName.startsWith(t) && displayName.contains(t)
            }
            val allResults = starts + contains
            
            // For GTFS routes, group by lineCode (route_short_name) to avoid duplicates
            val uniqueResults = mutableListOf<TransportationLine>()
            val seenLineCodes = mutableSetOf<String>()
            val seenDisplayNames = mutableSetOf<String>()
            
            for (line in allResults) {
                if (!line.lineCode.isNullOrEmpty()) {
                    if (!seenLineCodes.contains(line.lineCode)) {
                        seenLineCodes.add(line.lineCode)
                        uniqueResults.add(line)
                    }
                } else {
                    val displayName = lineDisplayName(line)
                    if (!seenDisplayNames.contains(displayName)) {
                        seenDisplayNames.add(displayName)
                        uniqueResults.add(line)
                    }
                }
            }
            
            _lineSuggestions.value = uniqueResults
            showLineSuggestions.value = isLineFieldFocused.value && _lineSuggestions.value.isNotEmpty()
            nameCounts.value = emptyMap()
            return@withContext
        }
        
        // Railway line search with priority ordering
        val starts = searchData.filter { key(it).startsWith(t) }
        val contains = searchData.filter { 
            val searchKey = key(it)
            !searchKey.startsWith(t) && searchKey.contains(t)
        }
        val allResults = starts + contains
        
        // Remove duplicates for railway lines to ensure unique line representation
        val uniqueResults = removeDuplicates(allResults)
        _lineSuggestions.value = uniqueResults
        
        // Show suggestions based on results count and focus state
        if (_lineSuggestions.value.size == 1) {
            val singleLine = _lineSuggestions.value[0]
            showLineSuggestions.value = fieldIsFocused && 
                lineDisplayName(singleLine).normalizedForSearch() != q.normalizedForSearch()
        } else {
            showLineSuggestions.value = fieldIsFocused && _lineSuggestions.value.isNotEmpty()
        }
        
        nameCounts.value = _lineSuggestions.value.groupBy { lineDisplayName(it) }
            .mapValues { it.value.size }
    }
    
    // Operator search and filtering
    suspend fun filterOperators(q: String, isFocused: Boolean? = null) = withContext(Dispatchers.Main) {
        val t = q.normalizedForSearch()
        
        // Don't show suggestions if line number, direction is changing, or operator already selected
        if (isLineNumberChanging.value || isGoOrBackChanging.value || operatorSelected.value) {
            return@withContext
        }
        
        val fieldIsFocused = isFocused ?: isOperatorFieldFocused.value
        val availableDataSources = LocalDataSource.entries
            .filter { dataSource ->
                dataSource.transportationType() == selectedTransportationKind.value.toTransportationKind() &&
                dataSource.operatorCode() != null
            }
        
        val availableOperatorsWithSource = availableDataSources.mapNotNull { dataSource ->
            dataSource.operatorCode()?.let {
                Pair(dataSource, dataSource.operatorDisplayName(getApplication()))
            }
        }
        
        val filtered = if (t.isEmpty()) {
            if (fieldIsFocused) {
                availableOperatorsWithSource
            } else {
                operatorSuggestions.value = emptyList()
                showOperatorSuggestions.value = false
                return@withContext
            }
        } else {
            if (operatorSelected.value) {
                val currentOperatorName = selectedOperatorCode.value?.let { operatorCode ->
                    LocalDataSource.entries.firstOrNull {
                        it.transportationType() == selectedTransportationKind.value.toTransportationKind() &&
                        it.operatorCode() == operatorCode
                    }?.operatorDisplayName(getApplication())
                }
                if (q == currentOperatorName) {
                    return@withContext
                }
            }
            
            availableOperatorsWithSource.filter { (_, operatorName) ->
                val normalizedName = operatorName.normalizedForSearch()
                normalizedName.startsWith(t) || normalizedName.contains(t)
            }
        }
        
        val sortedResults = if (t.isEmpty()) {
            filtered.map { it.second }
        } else {
            val starts = filtered.filter { it.second.normalizedForSearch().startsWith(t) }
            val contains = filtered.filter { !it.second.normalizedForSearch().startsWith(t) }
            starts.map { it.second } + contains.map { it.second }
        }
        
        operatorSuggestions.value = sortedResults.take(20)
        showOperatorSuggestions.value = fieldIsFocused && operatorSuggestions.value.isNotEmpty()
    }
    
    // Remove duplicates based on operator and line name
    fun removeDuplicates(lines: List<TransportationLine>): List<TransportationLine> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<TransportationLine>()
        
        for (line in lines) {
            val key = "${line.operatorCode ?: ""}_${lineDisplayName(line)}"
            if (!seen.contains(key)) {
                seen.add(key)
                result.add(line)
            }
        }
        
        return result
    }
    
    // Station search and filtering
    fun filterDepartureStops(lineInput: String) {
        val filtered = filterStops(lineInput, excludeStop = _selectedArrivalStop.value, isDeparture = true)
        departureSuggestions.value = filtered
        
        val shouldShow = isDepartureFieldFocused.value && filtered.isNotEmpty() && !departureStopSelected.value
        showDepartureSuggestions.value = shouldShow
    }
    
    fun filterArrivalStops(lineInput: String) {
        val filtered = filterStops(lineInput, excludeStop = _selectedDepartureStop.value, isDeparture = false)
        arrivalSuggestions.value = filtered
        
        val shouldShow = isArrivalFieldFocused.value && filtered.isNotEmpty() && !arrivalStopSelected.value
        showArrivalSuggestions.value = shouldShow
    }
    
    // Unified filtering logic for both railway stations and bus stops
    // isDeparture: true for departure stops, false for arrival stops
    private fun filterStops(lineInput: String, excludeStop: TransportationStop?, isDeparture: Boolean): List<TransportationStop> {
        var filtered: List<TransportationStop> = _lineStops.value
        
        // Filter by order: if excludeStop is selected, apply order constraint
        // Railway lines: skip order constraint (allow any station selection)
        // Bus lines: apply order constraint (departure must be before arrival)
        val isRailway = _selectedLine.value?.kind == TransportationLineKind.RAILWAY || 
            selectedTransportationKind.value == TransportationLineKind.RAILWAY
        
        if (!isRailway && excludeStop != null) {
            val excludeIndex = _lineStops.value.indexOfFirst { it.code == excludeStop.code }
            if (excludeIndex >= 0) {
                filtered = filtered.filterIndexed { index, _ ->
                    if (isDeparture) index < excludeIndex else index > excludeIndex
                }
            }
        }
        
        // Exclude the stop if it's the same as the excludeStop
        filtered = filtered.filter { stop ->
            excludeStop?.code != stop.code
        }
        
        // Filter by name if input is provided
        if (lineInput.isNotEmpty()) {
            filtered = filtered.filter { stop ->
                stop.displayName().contains(lineInput, ignoreCase = true) ||
                    stop.cleanedName().contains(lineInput, ignoreCase = true)
            }
        }
        
        return filtered
    }
    
    // Input processing
    fun processDepartureStopInput(newValue: String) {
        if (isLineNumberChanging.value) return
        
        isDepartureFieldFocused.value = true
        departureStopSelected.value = false
        val arrivalStopName = _selectedArrivalStop.value?.title?.getLocalizedName(
            fallbackTo = _selectedArrivalStop.value?.name ?: "") ?: _selectedArrivalStop.value?.name ?: ""
        val isSameAsArrival = arrivalStopName == newValue || arrivalStopInput.value == newValue
        
        if (isSameAsArrival) {
            departureStopInput.value = ""
            _selectedDepartureStop.value = null
        } else {
            departureStopInput.value = newValue
            filterDepartureStops(newValue)
        }
    }
    
    fun processArrivalStopInput(newValue: String) {
        if (isLineNumberChanging.value) return
        
        isArrivalFieldFocused.value = true
        arrivalStopSelected.value = false
        val departureStopName = _selectedDepartureStop.value?.title?.getLocalizedName(
            fallbackTo = _selectedDepartureStop.value?.name ?: "") ?: _selectedDepartureStop.value?.name ?: ""
        val isSameAsDeparture = departureStopName == newValue || departureStopInput.value == newValue
        
        if (isSameAsDeparture) {
            arrivalStopInput.value = ""
            _selectedArrivalStop.value = null
        } else {
            arrivalStopInput.value = newValue
            filterArrivalStops(newValue)
        }
    }
    
    suspend fun processOperatorInput(newValue: String) = withContext(Dispatchers.Main) {
        if (isLineNumberChanging.value || isGoOrBackChanging.value) return@withContext
        val currentOperatorName = selectedOperatorCode.value?.let { operatorCode ->
            LocalDataSource.entries.firstOrNull {
                it.transportationType() == selectedTransportationKind.value.toTransportationKind() &&
                it.operatorCode() == operatorCode
            }?.operatorDisplayName(getApplication())
        }
        
        // Reset operator selection and code when operatorInput changes
        // This must be done BEFORE filtering to ensure filterOperators works correctly
        if (newValue.isEmpty()) {
            // Clear operator selection when input is empty
            selectedOperatorCode.value = null
            operatorSelected.value = false
            showOperatorSuggestions.value = false
            operatorSuggestions.value = emptyList()
        } else {
            // Always reset selection flag when input changes (unless it exactly matches current selection)
            val shouldResetSelection = newValue != currentOperatorName
            
            if (shouldResetSelection) {
                // Reset selection flag if input changes after selection
                operatorSelected.value = false
                selectedOperatorCode.value = null
            }
        }
        
        // Note: operatorInput.value is already updated in onOperatorInputChanged
        
        // Trigger filtering when operatorInput changes (always filter, regardless of selection state)
        // Pass isFocused = isOperatorFieldFocused.value to maintain focus state
        // Wait for filterOperators to complete
        filterOperators(newValue, isFocused = isOperatorFieldFocused.value)
        
        // Re-filter lines without operator filter if needed
        if (newValue.isEmpty() || (operatorSelected.value && newValue != currentOperatorName)) {
            if (lineInput.value.isNotEmpty()) {
                filterLine(lineInput.value)
            }
        }
        
        // Show operator selection UI for custom operator input
        if (newValue.isNotEmpty()) {
            showOperatorSelection.value = true
        }
    }
    
    fun processLineInput(newValue: String) {
        if (isLineNumberChanging.value || isGoOrBackChanging.value) return
        
        viewModelScope.launch {
            filterLine(newValue, isFocused = isLineFieldFocused.value)
        }
        
        val currentLineName = _selectedLine.value?.name ?: ""
        val currentLineDisplayName = _selectedLine.value?.let { lineDisplayName(it) } ?: ""
        val shouldResetSelection = newValue != currentLineName && newValue != currentLineDisplayName
        
        if (shouldResetSelection) {
            _selectedLine.value = null
            _selectedDepartureStop.value = null
            _selectedArrivalStop.value = null
            showDepartureSuggestions.value = false
            departureSuggestions.value = emptyList()
            showArrivalSuggestions.value = false
            arrivalSuggestions.value = emptyList()
            isDepartureFieldFocused.value = false
            isArrivalFieldFocused.value = false
            departureStopSelected.value = false
            arrivalStopSelected.value = false
            lineSelected.value = false
        }
        
        lineInput.value = newValue
        if (newValue.isNotEmpty()) {
            showStationSelection.value = true
        }
    }
    
    // Line selection management
    fun selectLine(line: TransportationLine) {
        isLineNumberChanging.value = true
        
        _selectedLine.value = line
        
        lineInput.value = lineDisplayName(line)
        departureStopInput.value = ""
        arrivalStopInput.value = ""
        _selectedDepartureStop.value = null
        _selectedArrivalStop.value = null
        
        if (line.kind == TransportationLineKind.BUS) {
            line.busStopPoleOrder?.let { busStopPoleOrder ->
                lineBusStops.value = busStopPoleOrder
                
                val needsJapaneseNames = busStopPoleOrder.any { busStop ->
                    val hasJapaneseInNote = busStop.note?.containsJapanese() == true
                    (!hasJapaneseInNote || busStop.note.isEmpty()) && !busStop.busStopPole.isNullOrEmpty()
                }
                
                if (needsJapaneseNames) {
                    viewModelScope.launch {
                        isLoadingBusStops.value = true
                        loadingMessage = getApplication<Application>().getString(R.string.loadingBusStops)
                        try {
                            fetchJapaneseNamesForAllBusStops()
                        } finally {
                            isLoadingBusStops.value = false
                            loadingMessage = null
                        }
                    }
                }
            } ?: run {
                // For GTFS routes, fetch bus stops from GTFS data.
                line.operatorCode?.let { operatorCode ->
                    LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }?.let { dataSource ->
                        if (dataSource.apiType() == ODPTAPIType.GTFS) {
                            lineBusStops.value = emptyList()
                            
                            viewModelScope.launch {
                                isLoadingBusStops.value = true
                                loadingMessage = getApplication<Application>().getString(R.string.loadingBusStops)
                                try {
                                    fetchGTFSStopsForRoute(line.code, transportOperator = dataSource)
                                } finally {
                                    isLoadingBusStops.value = false
                                    loadingMessage = null
                                }
                            }
                        } else {
                            lineBusStops.value = emptyList()
                        }
                    }
                } ?: run {
                    lineBusStops.value = emptyList()
                }
            }
        } else {
            lineBusStops.value = emptyList()
            
            if (!line.stationOrder.isNullOrEmpty()) {
                lineStations.value = line.stationOrder
            } else {
                lineStations.value = emptyList()
                android.util.Log.w(
                    "SettingsLineViewModel",
                    "selectLine: stationOrder is null or empty for line ${line.code}"
                )
            }
        }
        
        _lineStops.value = getStopsForSelectedLine()
        showDepartureSuggestions.value = false
        departureSuggestions.value = emptyList()
        showArrivalSuggestions.value = false
        arrivalSuggestions.value = emptyList()
        isDepartureFieldFocused.value = false
        isArrivalFieldFocused.value = false
        
        selectedLineColor.value = line.lineColor ?: AccentString
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            isLineNumberChanging.value = false
        }
    }
    
    fun clearAllFormData() {
        operatorInput.value = ""
        operatorSuggestions.value = emptyList()
        showOperatorSuggestions.value = false
        operatorSelected.value = false
        showOperatorSelection.value = false
        selectedOperatorCode.value = null
        
        lineInput.value = ""
        
        resetStationSelection()
        
        departureStopInput.value = ""
        arrivalStopInput.value = ""
        
        selectedRideTimeFlow.value = 0
        
        selectedLineColor.value = AccentString
        
        selectedTransportation.value = "none"
        selectedTransferTime.value = 0
        
        showColorSelection.value = false
    }
    
    private fun resetStationSelection() {
        _selectedLine.value = null
        showStationSelection.value = false
        lineStations.value = emptyList()
        _selectedDepartureStop.value = null
        _selectedArrivalStop.value = null
        selectedRideTimeFlow.value = 0
        showDepartureSuggestions.value = false
        departureSuggestions.value = emptyList()
        showArrivalSuggestions.value = false
        arrivalSuggestions.value = emptyList()
        isDepartureFieldFocused.value = false
        isArrivalFieldFocused.value = false
    }
    
    fun setLineColor(color: String) {
        selectedLineColor.value = color
        showColorSelection.value = false
    }
    
    fun switchTransportationKind(isRailway: Boolean) {
        selectedTransportationKind.value = if (isRailway) TransportationLineKind.RAILWAY else TransportationLineKind.BUS
        _lineSuggestions.value = emptyList()
        showLineSuggestions.value = false
        nameCounts.value = emptyMap()
        showDepartureSuggestions.value = false
        showArrivalSuggestions.value = false
        departureSuggestions.value = emptyList()
        arrivalSuggestions.value = emptyList()
        isDepartureFieldFocused.value = false
        isArrivalFieldFocused.value = false
        showStationSelection.value = false
        
        operatorSuggestions.value = emptyList()
        showOperatorSuggestions.value = false
        operatorSelected.value = false
        selectedOperatorCode.value = null
        
        lineSelected.value = false
        _selectedLine.value = null
        viewModelScope.launch {
            try {
                loadFromCache()
            } catch (e: Exception) {
                android.util.Log.e("SettingsLineViewModel", "switchTransportationKind: loadFromCache failed", e)
            }
            
            if (lineInput.value.isNotEmpty() && lineInput.value.trim().isNotEmpty()) {
                filterLine(lineInput.value, isFocused = isLineFieldFocused.value)
            }
            
            if (operatorInput.value.isNotEmpty() && operatorInput.value.trim().isNotEmpty()) {
                filterOperators(operatorInput.value, isFocused = isOperatorFieldFocused.value)
            }
        }
    }
    
    suspend fun fetchGTFSLinesForOperator(dataSource: LocalDataSource) = withContext(Dispatchers.Main) {
        if (dataSource != LocalDataSource.TOEI_BUS && consumerKey.isEmpty()) {
            android.util.Log.w(
                "SettingsLineViewModel",
                "fetchGTFSLinesForOperator: consumerKey is empty for ${dataSource.name}"
            )
            _lineSuggestions.value = emptyList()
            showLineSuggestions.value = false
            isLoadingLines.value = false
            loadingMessage = null
            return@withContext
        }
        
        _lineSuggestions.value = emptyList()
        showLineSuggestions.value = false
        isLoadingLines.value = true
        loadingMessage = getApplication<Application>().getString(R.string.loadingLines)
        
        try {
            val gtfsLines = withContext(Dispatchers.IO) {
                gtfsService.fetchGTFSData(dataSource, consumerKey)
            }
            
            // Update busLines and remove duplicates for this operator.
            busLines.value = busLines.value.filter { it.operatorCode != dataSource.operatorCode() } + gtfsLines
            
            // Update combined lists.
            all = all.filter { it.operatorCode != dataSource.operatorCode() } + gtfsLines
            allData = all.filter { it.operatorCode != dataSource.operatorCode() } + gtfsLines
            
            // Update lineSuggestions for the selected operator.
            val expectedOperatorCode = dataSource.operatorCode()
            val filteredLines = gtfsLines.filter { 
                val matches = it.operatorCode == expectedOperatorCode
                matches
            }
            
            // Group by lineCode to avoid duplicates.
            val uniqueResults = mutableListOf<TransportationLine>()
            val seenLineCodes = mutableSetOf<String>()
            val seenDisplayNames = mutableSetOf<String>()
            
            for (line in filteredLines) {
                if (!line.lineCode.isNullOrEmpty()) {
                    if (!seenLineCodes.contains(line.lineCode)) {
                        seenLineCodes.add(line.lineCode)
                        uniqueResults.add(line)
                    }
                } else {
                    val displayName = lineDisplayName(line)
                    if (!seenDisplayNames.contains(displayName)) {
                        seenDisplayNames.add(displayName)
                        uniqueResults.add(line)
                    }
                }
            }
            
            _lineSuggestions.value = uniqueResults
            showLineSuggestions.value = _lineSuggestions.value.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.e(
                "SettingsLineViewModel",
                "fetchGTFSLinesForOperator: failed to fetch GTFS lines for ${dataSource.operatorDisplayName(getApplication())}",
                e
            )
            _lineSuggestions.value = emptyList()
            showLineSuggestions.value = false
        } finally {
            isLoadingLines.value = false
            loadingMessage = null
        }
    }
    
    // Fetch bus stops for a GTFS route.
    private suspend fun fetchGTFSStopsForRoute(routeId: String, transportOperator: LocalDataSource) = withContext(Dispatchers.IO) {
        try {
            if (transportOperator != LocalDataSource.TOEI_BUS && consumerKey.isEmpty()) {
                android.util.Log.w(
                    "SettingsLineViewModel",
                    "fetchGTFSStopsForRoute: consumerKey is empty for ${transportOperator.name}"
                )
                return@withContext
            }

            val stops = gtfsService.fetchGTFSStopsForRoute(
                routeId = routeId,
                transportOperator = transportOperator,
                consumerKey = consumerKey
            )
            
            withContext(Dispatchers.Main) {
                lineBusStops.value = stops
                // Update lineStops to reflect the fetched bus stops.
                _lineStops.value = getStopsForSelectedLine()
                // If a stop field is focused, re-run filtering.
                if (isDepartureFieldFocused.value) {
                    filterDepartureStops(departureStopInput.value)
                }
                if (isArrivalFieldFocused.value) {
                    filterArrivalStops(arrivalStopInput.value)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "SettingsLineViewModel",
                "fetchGTFSStopsForRoute: failed to fetch stops for route $routeId",
                e
            )
        }
    }
    
    // Fetch Japanese names for all bus stops in the selected route.
    private suspend fun fetchJapaneseNamesForAllBusStops() = withContext(Dispatchers.IO) {
        val selectedLine = _selectedLine.value ?: return@withContext
        val operatorCode = selectedLine.operatorCode ?: return@withContext
        val dataSource = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode } ?: return@withContext
        val currentStops = lineBusStops.value
        if (currentStops.isEmpty()) return@withContext

        val apiLink = dataSource.apiLink(APIDataType.STOP, TransportationKind.BUS)
        if (apiLink.isEmpty()) return@withContext

        try {
            val (data, response) = odptService.fetchODPTData(apiLink)
            if (response.code != 200) {
                android.util.Log.w(
                    "SettingsLineViewModel",
                    "fetchJapaneseNamesForAllBusStops: HTTP ${response.code}"
                )
                return@withContext
            }

            val jsonElement = JsonParser.parseString(String(data))
            if (!jsonElement.isJsonArray) {
                android.util.Log.w(
                    "SettingsLineViewModel",
                    "fetchJapaneseNamesForAllBusStops: response is not array"
                )
                return@withContext
            }

            val titleMap = mutableMapOf<String, LocalizedTitle>()
            val nameMap = mutableMapOf<String, String>()

            jsonElement.asJsonArray.forEach { element ->
                val obj = element.asJsonObject
                val sameAs = obj.get("owl:sameAs")?.asString ?: return@forEach
                val titleObj = obj.get("odpt:busstopPoleTitle")?.asJsonObject
                val title = titleObj?.let { titleJson ->
                    LocalizedTitle(
                        ja = titleJson.get("ja")?.asString,
                        en = titleJson.get("en")?.asString
                    )
                }
                val dcTitle = obj.get("dc:title")?.asString

                if (title != null) {
                    titleMap[sameAs] = title
                }
                if (!dcTitle.isNullOrBlank()) {
                    nameMap[sameAs] = dcTitle
                }
            }

            val updatedStops = currentStops.map { stop ->
                val key = stop.busStopPole ?: return@map stop
                val title = titleMap[key]
                val name = title?.ja ?: title?.en ?: nameMap[key]
                if (title == null && name.isNullOrEmpty()) {
                    stop
                } else {
                    stop.copy(
                        name = name ?: stop.name,
                        title = title ?: stop.title
                    )
                }
            }

            withContext(Dispatchers.Main) {
                lineBusStops.value = updatedStops
                _lineStops.value = getStopsForSelectedLine()

                val lineIndex = _selectedLineNumber.value - 1
                if (lineIndex >= 0) {
                    saveLineStopList(updatedStops, _selectedGoorback.value, lineIndex)
                }

                if (isDepartureFieldFocused.value) {
                    filterDepartureStops(departureStopInput.value)
                }
                if (isArrivalFieldFocused.value) {
                    filterArrivalStops(arrivalStopInput.value)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsLineViewModel", "fetchJapaneseNamesForAllBusStops failed", e)
        }
    }
    
    // Save operator line list after operator selection.
    override fun saveOperatorLineList(lines: List<TransportationLine>, goorback: String, num: Int) {
        goorback.saveOperatorLineList(lines, num, sharedPreferences)
    }

    // Save line stop list after line selection.
    override fun saveLineStopList(stops: List<TransportationStop>, goorback: String, num: Int) {
        goorback.saveLineStopList(stops, num, sharedPreferences)
    }

    // Save all information when save is pressed.
    override suspend fun saveAllDataToUserDefaults() = withContext(Dispatchers.IO) {
        val lineIndex = _selectedLineNumber.value - 1
        
        // Sync timetable ride time when all entries match.
        syncTimetableRideTimeWhenAllSame(lineIndex, selectedRideTimeFlow.value)
        
        sharedPreferences.edit {
            if (lineInput.value.trim().isNotEmpty()) {
                val lineNameKey = _selectedGoorback.value.lineNameKey(lineIndex)
                putString(lineNameKey, lineInput.value)
            }

            // Save line code for Firestore synchronization.
            val lineCodeKey = _selectedGoorback.value.lineCodeKey(lineIndex)
            _selectedLine.value?.let { selectedLine ->
                val codeToSave = if (!selectedLine.lineCode.isNullOrEmpty()) selectedLine.lineCode else ""
                putString(lineCodeKey, codeToSave)
            } ?: run {
                val existingLineCode = sharedPreferences.getString(lineCodeKey, null) ?: ""
                if (existingLineCode.isEmpty()) {
                    findSavedLineInData()?.let { foundLine ->
                        val codeToSave = if (!foundLine.lineCode.isNullOrEmpty()) foundLine.lineCode else ""
                        putString(lineCodeKey, codeToSave)
                    }
                }
            }

            // Save line color.
            selectedLineColor.value?.let { lineColor ->
                val lineColorKey = _selectedGoorback.value.lineColorKey(lineIndex)
                putString(lineColorKey, lineColor)
            } ?: run {
                val lineColorToSave = _selectedLine.value?.lineColor ?: AccentString
                val lineColorKey = _selectedGoorback.value.lineColorKey(lineIndex)
                putString(lineColorKey, lineColorToSave)
            }

            // Save operator name.
            if (operatorInput.value.trim().isNotEmpty()) {
                val operatorNameKey = _selectedGoorback.value.operatorNameKey(lineIndex)
                putString(operatorNameKey, operatorInput.value)
            }
            // Save operator code when available.
            val operatorCodeToSave = _selectedLine.value?.operatorCode ?: selectedOperatorCode.value
            if (!operatorCodeToSave.isNullOrEmpty()) {
                val operatorCodeKey = _selectedGoorback.value.operatorCodeKey(lineIndex)
                putString(operatorCodeKey, operatorCodeToSave)
            }

            // Save transportation kind.
            val lineKindKey = _selectedGoorback.value.lineKindKey(lineIndex)
            putString(lineKindKey, selectedTransportationKind.value.name)

            // Save line direction.
            val savedLineDirection = _selectedLine.value?.lineDirection 
                ?: _selectedGoorback.value.lineDirection(sharedPreferences, lineIndex)
            if (savedLineDirection.isNotEmpty()) {
                val lineDirectionKey = _selectedGoorback.value.lineDirectionKey(lineIndex)
                putString(lineDirectionKey, savedLineDirection)
            }

            // Save line selected flag.
            val lineSelectedKey = _selectedGoorback.value.lineSelectedKey(lineIndex)
            putBoolean(lineSelectedKey, lineSelected.value)

            // Save departure stop information.
            if (departureStopInput.value.trim().isNotEmpty()) {
                val departureKey = _selectedGoorback.value.departStationKey(lineIndex)
                putString(departureKey, departureStopInput.value)

                // Save departure stop ODPT code.
                _selectedDepartureStop.value?.code?.let { stationCode ->
                    val departureCodeKey = _selectedGoorback.value.departStationCodeKey(lineIndex)
                    putString(departureCodeKey, stationCode)
                }

                // Save departure stop lineCode if available.
                _selectedDepartureStop.value?.lineCode?.let { stationLineCode ->
                    val departureLineCodeKey =
                        "${_selectedGoorback.value.departStationCodeKey(lineIndex)}_lineCode"
                    putString(departureLineCodeKey, stationLineCode)
                }
            }

            // Save arrival stop information.
            if (arrivalStopInput.value.trim().isNotEmpty()) {
                val arrivalKey = _selectedGoorback.value.arriveStationKey(lineIndex)
                putString(arrivalKey, arrivalStopInput.value)

                // Save arrival stop ODPT code.
                _selectedArrivalStop.value?.code?.let { stationCode ->
                    val arrivalCodeKey = _selectedGoorback.value.arriveStationCodeKey(lineIndex)
                    putString(arrivalCodeKey, stationCode)
                }

                // Save arrival stop lineCode if available.
                _selectedArrivalStop.value?.lineCode?.let { stationLineCode ->
                    val arrivalLineCodeKey =
                        "${_selectedGoorback.value.arriveStationCodeKey(lineIndex)}_lineCode"
                    putString(arrivalLineCodeKey, stationLineCode)
                }
            }

            // Save ride time.
            val rideTimeKey = _selectedGoorback.value.rideTimeKey(lineIndex)
            putInt(rideTimeKey, selectedRideTimeFlow.value)

            // Save transfer settings and calculate transfer count.
            val changeLineKey = _selectedGoorback.value.changeLineKey()
            val transportationKey = _selectedGoorback.value.transportationKey(lineIndex + 2)
            val currentChangeLine = changeLineKey.userDefaultsInt(sharedPreferences, 0)
            val currentTransportation = sharedPreferences.getString(transportationKey, null)

            if (currentTransportation != "none" && selectedTransportation.value == "none") {
                val newChangeLine = lineIndex
                putInt(changeLineKey, newChangeLine)
            } else if (currentTransportation == "none" && selectedTransportation.value != "none") {
                val newChangeLine = minOf(2, currentChangeLine + 1)
                putInt(changeLineKey, newChangeLine)
            }

            putString(transportationKey, selectedTransportation.value)

            val transferTimeKey = _selectedGoorback.value.transferTimeKey(lineIndex + 2)
            putInt(transferTimeKey, selectedTransferTime.value)

            // Enable direction 2 display when saving data for direction 2.
            if (_selectedGoorback.value == "back2" || _selectedGoorback.value == "go2") {
                val route2DisplayKey = _selectedGoorback.value.isShowRoute2Key()
                putBoolean(route2DisplayKey, true)
            }
        }
        
        // Save operator line list to SharedPreferences.
        if (_lineSuggestions.value.isNotEmpty()) {
            saveOperatorLineList(_lineSuggestions.value, _selectedGoorback.value, lineIndex)
        }
        
        // Save line stop list to SharedPreferences.
        if (lineStops.isNotEmpty()) {
            saveLineStopList(lineStops, _selectedGoorback.value, lineIndex)
        }
        
        // Update display without overriding user-selected color.
        updateDisplay()
    }
    
    // If all timetable ride times for this route are the same value, update every entry to the new ride time.
    // If they are not all the same, do not change timetable ride times.
    private fun syncTimetableRideTimeWhenAllSame(lineIndex: Int, newRideTime: Int) {
        if (_selectedLine.value?.kind == TransportationLineKind.BUS) return
        val goorback = _selectedGoorback.value
        val calendarTypeStrings = goorback.loadAvailableCalendarTypes(sharedPreferences, lineIndex)
        val calendarTypes = calendarTypeStrings.mapNotNull { ODPTCalendarType.fromRawValue(it) }
        val allRideTimes = mutableListOf<Int>()
        val slotsWithData = mutableListOf<Pair<ODPTCalendarType, Int>>()
        for (calendarType in calendarTypes) {
            val hours = goorback.validHourRange(calendarType, lineIndex, sharedPreferences)
            for (hour in hours) {
                val times = goorback.loadTransportationTimes(calendarType, lineIndex, hour, sharedPreferences)
                if (times.isNotEmpty()) {
                    slotsWithData.add(calendarType to hour)
                    times.mapTo(allRideTimes) { it.rideTime }
                }
            }
        }
        if (allRideTimes.isEmpty()) return
        val distinctRideTimes = allRideTimes.distinct()
        if (distinctRideTimes.size != 1) return
        val previousRideTime = distinctRideTimes.single()
        if (previousRideTime == newRideTime) return
        for ((calendarType, hour) in slotsWithData) {
            val times = goorback.loadTransportationTimes(calendarType, lineIndex, hour, sharedPreferences)
            val updated: List<TransportationTime> = times.map { tt ->
                if (tt is BusTime) tt.copy(rideTime = newRideTime)
                else (tt as TrainTime).copy(rideTime = newRideTime)
            }
            goorback.saveTransportationTimes(updated, calendarType, lineIndex, hour, sharedPreferences)
        }
    }
    
    // Update all line information at once
    // Synchronize UI display with current model state
    private fun updateDisplay() {
        _selectedLine.value?.let { line ->
            lineInput.value = lineDisplayName(line)
        }
        
        // Only update selectedLineColor from line if it's not already set by user
        // This prevents overwriting user-selected color
        if (selectedLineColor.value == null) {
            _selectedLine.value?.lineColor?.let { lineColor ->
                selectedLineColor.value = lineColor
            }
        }
        
        _selectedDepartureStop.value?.let { departureStop ->
            departureStopInput.value = departureStop.displayName()
        }
        
        _selectedArrivalStop.value?.let { arrivalStop ->
            arrivalStopInput.value = arrivalStop.displayName()
        }
    }
    
    // Handle line save
    suspend fun handleLineSave() {
        if (selectedLineColor.value == null) {
            selectedLineColor.value = _selectedLine.value?.lineColor ?: AccentString
        }
        
        saveAllDataToUserDefaults()
        
        sharedPreferences.edit {
            putLong("SettingsLineUpdated", System.currentTimeMillis())
        }
    }
    
    // Auto-generate timetable data.
    suspend fun autoGenerateTimetable() = withContext(Dispatchers.Main) {
        val hasStops = _selectedDepartureStop.value != null && _selectedArrivalStop.value != null
        if (_selectedLine.value == null || !hasStops) {
            return@withContext
        }
        isLoadingTimetable = true
        loadingMessage = getApplication<Application>().getString(R.string.generatingTimetable)
        
        try {
            val timetableViewModel = SettingsTimetableViewModel(
                application = getApplication(),
                sharedPreferences = sharedPreferences,
                lineViewModel = this@SettingsLineViewModel
            )
            
            val timetableData = timetableViewModel.getTimeTableData()
            
            if (timetableData.isNotEmpty()) {
                timetableViewModel.finalizeTimetableData(timetableData)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsLineViewModel", "Failed to auto-generate timetable", e)
        } finally {
            isLoadingTimetable = false
            loadingMessage = null
        }
    }
    
}

