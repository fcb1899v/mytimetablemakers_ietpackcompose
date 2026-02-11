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

// MARK: - Settings Line ViewModel
// ViewModel for SettingsLineSheet view that manages railway line configuration.
// Handles data loading from ODPT API and local JSON files, search functionality,
// station selection, and user preferences persistence.
class SettingsLineViewModel(
    application: Application,
    private val sharedPreferences: SharedPreferences,
    goorback: String = "back1",
    lineIndex: Int = 0
) : AndroidViewModel(application), SettingsLineViewModelInterface {
    
    // MARK: - Configuration Properties
    override val goorback: String
    override val lineIndex: Int
    private val consumerKey: String = BuildConfig.ODPT_ACCESS_TOKEN
    
    init {
        // Log consumerKey status (without exposing the actual key)
        if (consumerKey.isEmpty()) {
            android.util.Log.d("SettingsLineViewModel", "⚠️ ODPT_ACCESS_TOKEN is not set in local.properties. GTFS operators requiring authentication will fail.")
        }
    }
    
    // MARK: - Published Properties (UI State)
    // UI state properties that trigger view updates when changed
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
    
    // MARK: - Data Properties
    // Line and station selection state management
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
    
    // Setter for selectedDepartureStop
    fun setSelectedDepartureStop(stop: TransportationStop?) {
        _selectedDepartureStop.value = stop
    }
    
    private val _selectedArrivalStop = MutableStateFlow<TransportationStop?>(null)
    val selectedArrivalStopState: StateFlow<TransportationStop?> = _selectedArrivalStop.asStateFlow()
    override val selectedArrivalStop: TransportationStop?
        get() = _selectedArrivalStop.value
    
    // Setter for selectedArrivalStop
    fun setSelectedArrivalStop(stop: TransportationStop?) {
        _selectedArrivalStop.value = stop
    }
    
    // User input fields for data entry
    val departureStopInput = MutableStateFlow("")
    val arrivalStopInput = MutableStateFlow("")
    
    private val selectedRideTimeFlow = MutableStateFlow(0)
    val selectedRideTimeState: StateFlow<Int> = selectedRideTimeFlow.asStateFlow()
    fun setSelectedRideTime(value: Int) {
        selectedRideTimeFlow.value = value
    }
    
    // Suggestion and focus state management
    val showDepartureSuggestions = MutableStateFlow(false)
    val departureSuggestions = MutableStateFlow<List<TransportationStop>>(emptyList())
    val isDepartureFieldFocused = MutableStateFlow(false)
    val showArrivalSuggestions = MutableStateFlow(false)
    val arrivalSuggestions = MutableStateFlow<List<TransportationStop>>(emptyList())
    val isArrivalFieldFocused = MutableStateFlow(false)
    val showLineSuggestions = MutableStateFlow(false)
    val isLineFieldFocused = MutableStateFlow(false)
    
    // Selection flags to prevent re-display of suggestions after selection
    val departureStopSelected = MutableStateFlow(false)
    val arrivalStopSelected = MutableStateFlow(false)
    val lineSelected = MutableStateFlow(false)
    
    // Line configuration and customization
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
    // MARK: - Computed Properties
    val selectedGoorback: String
        get() = _selectedGoorback.value

    val hasSelectedLine: Boolean
        get() = _selectedLine.value != null
    
    val hasStops: Boolean
        get() = _lineStops.value.isNotEmpty()
    
    // All required fields "filled" = four input texts non-empty + ride time > 0
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
    
    
    // Get localized display names for direction options
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
    
    // MARK: - Private Properties
    // Internal data storage and state management
    private var all: List<TransportationLine> = emptyList()
    private var allData: List<TransportationLine> = emptyList()
    val railwayLines = MutableStateFlow<List<TransportationLine>>(emptyList())
    val busLines = MutableStateFlow<List<TransportationLine>>(emptyList())
    val nameCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    
    
    // MARK: - Helper Functions for Type Conversion
    // Convert TransportationLineKind to TransportationKind
    private fun TransportationLineKind.toTransportationKind(): TransportationKind {
        return when (this) {
            TransportationLineKind.RAILWAY -> TransportationKind.RAILWAY
            TransportationLineKind.BUS -> TransportationKind.BUS
        }
    }
    
    // Services
    private val cache = CacheStore(getApplication())
    private val odptService = ODPTDataService(getApplication())
    private val gtfsService = GTFSDataService(getApplication())
    
    // MARK: - Timetable ViewModel
    // Reference to timetable view model for timetable generation
    val timetableViewModel: SettingsTimetableViewModel
    
    init {
        // Validate goorback value and use default if invalid
        val validGoorback = if (goorback.isEmpty() || !goorbackList.contains(goorback)) "back1" else goorback
        this.goorback = validGoorback
        this.lineIndex = lineIndex
        this._selectedGoorback.value = validGoorback
        
        // Initialize timetable view model with reference to this view model
        this.timetableViewModel = SettingsTimetableViewModel(getApplication(), sharedPreferences, this)
        
        // Set flag to prevent filterLine() from being called during initial setup
        this.isLineNumberChanging.value = true
        
        _selectedLineNumber.value = lineIndex + 1
        
        // Initialize transfer count if not set
        if (!sharedPreferences.contains(_selectedGoorback.value.changeLineKey())) {
            sharedPreferences.edit { putInt(_selectedGoorback.value.changeLineKey(), 0) }
        }
        
        updateAvailableLineNumbers(shouldPreserveLineNumber = false)
        loadSettingsForSelectedLine()
        
        // Load data from cache in background
        viewModelScope.launch {
            try {
                loadFromCache()
            } catch (e: Exception) {
                android.util.Log.d("SettingsLineViewModel", "init: loadFromCache() failed: ${e.message}", e)
            }
        }
    }
    
    // MARK: - Helper Functions
    // Get localized display name for transportation line
    // Delegates to line.displayName() for consistent behavior with TransportationStop
    fun lineDisplayName(line: TransportationLine): String = line.displayName()

    // MARK: - Check Timetable Support
    // Check if selected line has timetable support (train or bus)
    fun hasTimetableSupport(): Boolean {
        val operatorCode = _selectedLine.value?.operatorCode ?: return false
        val dataSource = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode } ?: return false
        return dataSource.hasTrainTimeTable() || dataSource.hasBusTimeTable()
    }
    
    // MARK: - Data Management
    // Load data from cache, LineData directory, or API for better performance
    // Only load data for the currently selected transportation kind to improve efficiency
    private suspend fun loadFromCache() = withContext(Dispatchers.IO) {
        val kind = selectedTransportationKind.value.toTransportationKind()
        val loadedLines = mutableListOf<TransportationLine>()
        
        val isBus = kind == TransportationKind.BUS
        
        // Load data from cache for all operators of the selected kind
        for (transportOperator in LocalDataSource.entries) {
            if (transportOperator.transportationType() != kind) continue
            
            // Handle GTFS operators separately (they don't have JSON cache)
            if (transportOperator.apiType() == ODPTAPIType.GTFS) {
                continue // GTFS lines will be loaded when operator is selected
            }
            
            val cacheKey = transportOperator.fileName()
            
            var cachedData = cache.loadData(cacheKey)
            
            // If cache is not available, try to load from LineData directory
            if (cachedData == null) {
                val fileManager = getApplication<Application>().filesDir
                val lineDataDirectory = java.io.File(fileManager, "LineData")
                val file = java.io.File(lineDataDirectory, cacheKey)
                
                if (file.exists() && file.isFile) {
                    try {
                        cachedData = java.io.FileInputStream(file).use { it.readBytes() }
                    } catch (e: Exception) {
                        if (isBus) {
                            android.util.Log.d("SettingsLineViewModel", "🚌 loadFromCache: Failed to load BUS data from LineData directory for ${transportOperator.name}: ${e.message}", e)
                        } else {
                            android.util.Log.d("SettingsLineViewModel", "loadFromCache: Failed to load data from LineData directory for ${transportOperator.name}: ${e.message}", e)
                        }
                    }
                }
            }
            
            // If still no data, try to fetch from API
            // Remove the size limit to ensure all operators are loaded
            if (cachedData == null) {
                try {
                    cachedData = odptService.fetchIndividualOperatorData(transportOperator)
                    
                    // Save to both CacheStore and LineData directory for future use
                    cache.saveData(cachedData, cacheKey)
                    odptService.writeIndividualOperatorDataToFile(cachedData, transportOperator)
                    
                } catch (e: Exception) {
                    if (isBus) {
                        android.util.Log.d("SettingsLineViewModel", "🚌 loadFromCache: Failed to fetch BUS data from API for ${transportOperator.name}: ${e.message}", e)
                    } else {
                        android.util.Log.d("SettingsLineViewModel", "🚃 loadFromCache: Failed to fetch data from API for ${transportOperator.name}: ${e.message}", e)
                    }
                }
            }
            
            if (cachedData != null) {
                try {
                    val lines = if (kind == TransportationKind.RAILWAY) ODPTParser.parseRailwayRoutes(cachedData) else ODPTParser.parseBusRoutes(cachedData)
                    loadedLines.addAll(lines)
                } catch (e: Exception) {
                    if (isBus) {
                        android.util.Log.d("SettingsLineViewModel", "🚌 Failed to parse BUS data for ${transportOperator.name}: ${e.message}", e)
                    } else {
                        android.util.Log.d("SettingsLineViewModel", "🚃 Failed to parse data for ${transportOperator.name}: ${e.message}", e)
                    }
                    e.printStackTrace()
                }
            }
        }
        
        // Update state on main thread
        withContext(Dispatchers.Main) {
            // Only update lines for the selected kind, keep existing lines for other kind
            // This ensures that switching between railway and bus doesn't lose previously loaded data
            val kind = selectedTransportationKind.value.toTransportationKind()
            if (kind == TransportationKind.RAILWAY) {
                // Update railway lines, keep existing bus lines
                val newRailwayLines = loadedLines.filter { it.kind == TransportationLineKind.RAILWAY }
                railwayLines.value = newRailwayLines
            } else {
                // Update bus lines, keep existing railway lines
                val newBusLines = loadedLines.filter { it.kind == TransportationLineKind.BUS }
                busLines.value = newBusLines
            }
            
            // Update all and allData to include both railway and bus lines
            all = railwayLines.value + busLines.value
            allData = all
            
            // Check if saved line exists in loaded data and restore it
            viewModelScope.launch {
                checkSavedLineInData()
                isLineNumberChanging.value = false
                
                // If operator is selected and line field is focused, show line suggestions
                if (operatorSelected.value && selectedOperatorCode.value != null && isLineFieldFocused.value && lineInput.value.isEmpty()) {
                    filterLine("", isFocused = true)
                }
            }
        }
    }
    
    // MARK: - Direction Selection Management
    // Handle goorback selection changes and reset related state
    fun selectGoorback(newGoorback: String) {
        // Early return if same value to avoid unnecessary processing
        if (_selectedGoorback.value == newGoorback) return
        
        // Set flag to indicate route is changing
        isGoOrBackChanging.value = true
        
        // Update selectedGoorback and process changes
        _selectedGoorback.value = newGoorback

        // Update available line numbers based on new direction
        // Preserve current line number when switching directions
        updateAvailableLineNumbers(shouldPreserveLineNumber = true)
        
        // Load settings for the selected line after line numbers are updated
        loadSettingsForSelectedLine()
        
        // Check if saved line exists in loaded data and restore it asynchronously
        viewModelScope.launch {
            checkSavedLineInData()
            
            // Hide all suggestions during direction change to prevent UI conflicts
            showOperatorSuggestions.value = false
            operatorSuggestions.value = emptyList()
            showDepartureSuggestions.value = false
            showArrivalSuggestions.value = false
            showLineSuggestions.value = false
            isDepartureFieldFocused.value = false
            isArrivalFieldFocused.value = false
            _lineSuggestions.value = emptyList()
            
            // Reset flag after processing
            kotlinx.coroutines.delay(100)
            isGoOrBackChanging.value = false
        }
    }
    
    // MARK: - Line Number Management
    // Update available line numbers with option to preserve current line number
    private fun updateAvailableLineNumbers(shouldPreserveLineNumber: Boolean) {
        val changeLineValue = _selectedGoorback.value.changeLineKey().userDefaultsInt(sharedPreferences, 0)
        val maxLineNumber = minOf(changeLineValue + 1, 3)
        availableLineNumbers.value = (1..maxLineNumber).toList()
        
        // Reset transportation settings for lines beyond current transfer count
        for (i in (changeLineValue + 2)..4) {
            val transportationKey = _selectedGoorback.value.transportationKey(i)
            sharedPreferences.edit { putString(transportationKey, "none") }
        }
        
        // Only change selectedLineNumber if not preserving it
        if (!shouldPreserveLineNumber && _selectedLineNumber.value == 1 && lineIndex > 0) {
            _selectedLineNumber.value = minOf(lineIndex + 1, availableLineNumbers.value.lastOrNull() ?: 1)
        }
    }
    
    // Handle line number selection and update lineIndex accordingly
    fun selectLineNumber(lineNumber: Int) {
        isLineNumberChanging.value = true
        
        // Hide all suggestions during line number change
        showDepartureSuggestions.value = false
        showArrivalSuggestions.value = false
        showLineSuggestions.value = false
        isDepartureFieldFocused.value = false
        isArrivalFieldFocused.value = false
        _lineSuggestions.value = emptyList()
        
        _selectedLineNumber.value = lineNumber
        
        loadSettingsForSelectedLine()
        
        // Check if saved line exists in loaded data and restore it
        viewModelScope.launch {
            checkSavedLineInData()
            kotlinx.coroutines.delay(100)
            isLineNumberChanging.value = false
        }
    }
    
    // MARK: - Settings Loading
    // Load settings for the currently selected line number
    private fun loadSettingsForSelectedLine() {
        val currentLineIndex = _selectedLineNumber.value - 1
        
        // Load transportation kind first (needed for operator name restoration)
        val lineKindKey = _selectedGoorback.value.lineKindKey(currentLineIndex)
        val savedKindString = sharedPreferences.getString(lineKindKey, null)
        selectedTransportationKind.value = if (savedKindString != null) savedKindString.fromString() ?: TransportationLineKind.RAILWAY else TransportationLineKind.RAILWAY
        
        // Load operator name (consistent with other fields like line name, station names)
        val operatorNameKey = _selectedGoorback.value.operatorNameKey(currentLineIndex)
        val savedOperatorName = sharedPreferences.getString(operatorNameKey, null)
        
        if (savedOperatorName != null && savedOperatorName.isNotEmpty()) {
            operatorInput.value = savedOperatorName

            // Only restore selection if operator code was saved (selected from dropdown).
            val savedOperatorCode = _selectedGoorback.value.operatorCode(sharedPreferences, currentLineIndex)
            if (savedOperatorCode.isNotEmpty()) {
                selectedOperatorCode.value = savedOperatorCode
                operatorSelected.value = true

                // Load operator line list from SharedPreferences only for GTFS bus routes
                val dataSourceFromCode = LocalDataSource.entries.firstOrNull { it.operatorCode() == savedOperatorCode }
                if (dataSourceFromCode != null && selectedTransportationKind.value == TransportationLineKind.BUS &&
                    dataSourceFromCode.apiType() == ODPTAPIType.GTFS) {
                    android.util.Log.d("SettingsLineViewModel", "🚌 loadSettingsForSelectedLine: GTFS operator detected (from code), calling fetchGTFSLinesForOperator for ${dataSourceFromCode.name}")
                    viewModelScope.launch {
                        try {
                            fetchGTFSLinesForOperator(dataSourceFromCode)
                            android.util.Log.d("SettingsLineViewModel", "🚌 loadSettingsForSelectedLine: fetchGTFSLinesForOperator completed for ${dataSourceFromCode.name}")
                        } catch (e: Exception) {
                            android.util.Log.d("SettingsLineViewModel", "🚌 loadSettingsForSelectedLine: fetchGTFSLinesForOperator failed for ${dataSourceFromCode.name}: ${e.message}", e)
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
            // Try to restore operator code from SharedPreferences if operator name is not saved
            val savedOperatorCode = _selectedGoorback.value.operatorCode(sharedPreferences, currentLineIndex)
            if (savedOperatorCode.isNotEmpty()) {
                selectedOperatorCode.value = savedOperatorCode
                operatorSelected.value = true
                
                // For GTFS operators, fetch lines from ZIP cache
                val dataSource = LocalDataSource.entries.firstOrNull { it.operatorCode() == savedOperatorCode }
                if (dataSource != null && selectedTransportationKind.value == TransportationLineKind.BUS && 
                    dataSource.apiType() == ODPTAPIType.GTFS) {
                    android.util.Log.d("SettingsLineViewModel", "🚌 loadSettingsForSelectedLine: GTFS operator detected (from code, no saved name), calling fetchGTFSLinesForOperator for ${dataSource.name}")
                    viewModelScope.launch {
                        try {
                            fetchGTFSLinesForOperator(dataSource)
                            android.util.Log.d("SettingsLineViewModel", "🚌 loadSettingsForSelectedLine: fetchGTFSLinesForOperator completed for ${dataSource.name}")
                        } catch (e: Exception) {
                            android.util.Log.d("SettingsLineViewModel", "🚌 loadSettingsForSelectedLine: fetchGTFSLinesForOperator failed for ${dataSource.name}: ${e.message}", e)
                            // Fallback to SharedPreferences if fetch fails
                            _selectedGoorback.value.loadOperatorLineList(currentLineIndex, sharedPreferences)?.let { savedLineList ->
                                _lineSuggestions.value = savedLineList
                                showLineSuggestions.value = false
                            }
                        }
                    }
                }
            } else {
                // If operator name and code are not saved, clear operator input
                operatorInput.value = ""
                selectedOperatorCode.value = null
                operatorSelected.value = false
            }
        }
        
        // Load line name and restore line object from line name for filtering
        val context = getApplication<Application>()
        val savedLineName = _selectedGoorback.value.settingsLineName(sharedPreferences, currentLineIndex, context)
        val notSet = context.getString(R.string.notSet)
        
        if (savedLineName != notSet && savedLineName.isNotEmpty()) {
            lineInput.value = savedLineName
            
            // Restore line object from line name if data is available
            if (all.isNotEmpty()) {
                findSavedLineInData()?.let { foundLine ->
                    _selectedLine.value = foundLine
                    lineSelected.value = _selectedGoorback.value.lineSelected(sharedPreferences, currentLineIndex)
                    
                    // Load line direction from SharedPreferences
                    val savedLineDirection = _selectedGoorback.value.lineDirection(sharedPreferences, currentLineIndex)
                    if (savedLineDirection.isNotEmpty() && foundLine.lineDirection == null) {
                        // Restore line direction if saved but not in foundLine
                        _selectedLine.value = foundLine.copy(lineDirection = savedLineDirection)
                    }
                    
                    // Load line stop list from SharedPreferences for bus routes to keep display names
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
        
        // Load transfer settings
        if (_selectedLineNumber.value < 3) {
            val transportationKey = _selectedGoorback.value.transportationKey(currentLineIndex + 2)
            val savedTransportation = sharedPreferences.getString(transportationKey, null)
            selectedTransportation.value = if (savedTransportation != null && savedTransportation.isNotEmpty()) savedTransportation else "none"
            
            val transferTimeKey = _selectedGoorback.value.transferTimeKey(currentLineIndex + 2)
            val savedTransferTime = transferTimeKey.userDefaultsInt(sharedPreferences, 0)
            selectedTransferTime.value = if (savedTransferTime > 0) savedTransferTime else 0
        } else {
            selectedTransportation.value = "none"
            selectedTransferTime.value = 0
        }
    }
    
    // MARK: - Saved Line Restoration
    // Check if line read from SharedPreferences exists in current data
    private suspend fun checkSavedLineInData() = withContext(Dispatchers.Main) {
        // Wait for data loading to complete before validation
        while (all.isEmpty()) {
            kotlinx.coroutines.delay(100)
        }
        
        // Always try to restore station information, even if lineInput is empty
        if (lineInput.value.trim().isNotEmpty()) {
            // Try to find and restore saved line if lineInput is not empty
            findSavedLineInData()?.let { foundLine ->
                _selectedLine.value = foundLine
                showStationSelection.value = true
                lineSelected.value = true
                
                // Set up line stops (bus stops, stations, etc.)
                setupLineStops(foundLine)
                
                // Don't show color selection for saved lines
                showColorSelection.value = false
                
                // Update transportation kind to match found line
                selectedTransportationKind.value = foundLine.kind
            } ?: run {
                // Keep user input even if saved line not found in current data
                _selectedLine.value = null
                lineStations.value = emptyList()
                showStationSelection.value = false
                lineSelected.value = false
            }
        }
        
        // Always load station settings regardless of line status
        loadStationSettings()
    }
    
    // Helper method to find saved line in current data
    // First tries to find by line code if available, then falls back to line name
    private fun findSavedLineInData(): TransportationLine? {
        val currentLineIndex = _selectedLineNumber.value - 1
        val lineCodeKey = _selectedGoorback.value.lineCodeKey(currentLineIndex)
        
        // Try to find by line code first (more reliable)
        val savedLineCode = sharedPreferences.getString(lineCodeKey, null)
        if (savedLineCode != null && savedLineCode.isNotEmpty()) {
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
    
    // MARK: - Get Stops for Selected Line
    // Get stops information for the selected line (unified for both railway and bus)
    fun getStopsForSelectedLine(): List<TransportationStop> {
        val selectedLine = _selectedLine.value ?: return emptyList()

        if (selectedLine.kind != TransportationLineKind.BUS) {
            // Handle railway lines: use odpt:stationOrder from Railway JSON
            // First try stationOrder from parsed Railway data, then lineStations if already loaded
            return if (!selectedLine.stationOrder.isNullOrEmpty()) selectedLine.stationOrder else lineStations.value
        }

        // Handle bus routes - use lineBusStops if available, otherwise fallback to busStopPoleOrder
        return if (lineBusStops.value.isNotEmpty()) {
            lineBusStops.value.map { busStop ->
                // Check if we need to fetch Japanese name from API
                val hasJapaneseInNote = busStop.note?.containsJapanese() == true

                if ((!hasJapaneseInNote || busStop.note.isEmpty()) && !busStop.busStopPole.isNullOrEmpty()) {
                    // Japanese name will be fetched later in selectLine
                    // Create TransportationStop with Japanese name from title if available
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
                    // Use existing TransportationStop (already a TransportationStop)
                    busStop
                }
            }
        } else {
            // Japanese name fetch is handled in selectLine when needed.
            selectedLine.busStopPoleOrder ?: emptyList()
        }
    }
    
    // MARK: - Search and Filtering
    // Filter railway lines based on search input with performance optimizations
    suspend fun filterLine(q: String, isFocused: Boolean? = null) = withContext(Dispatchers.Main) {
        val t = q.normalizedForSearch()
        
        // Use provided isFocused parameter or fall back to current state
        val fieldIsFocused = isFocused ?: isLineFieldFocused.value
        
        // Don't show suggestions if line number or direction is being changed or line is already selected
        if (isLineNumberChanging.value || isGoOrBackChanging.value || lineSelected.value) {
            return@withContext
        }
        
        // Don't show line suggestions if operator is not selected from dropdown
        val operatorCode = selectedOperatorCode.value
        if (operatorCode == null || !operatorSelected.value) {
            _lineSuggestions.value = emptyList()
            nameCounts.value = emptyMap()
            showLineSuggestions.value = false
            return@withContext
        }
        
        // Filter by transportation kind (.railway or .bus)
        var searchData = if (selectedTransportationKind.value == TransportationLineKind.RAILWAY) railwayLines.value else busLines.value
        
        // If data is not loaded yet, wait for data to load (max 2 seconds)
        // Wait for data to load before filtering
        if (searchData.isEmpty()) {
            var retryCount = 0
            val maxRetries = 20 // 2 seconds max (20 * 100ms)
            while (searchData.isEmpty() && retryCount < maxRetries) {
                kotlinx.coroutines.delay(100)
                retryCount++
                // Re-read from StateFlow to get latest value
                searchData = if (selectedTransportationKind.value == TransportationLineKind.RAILWAY) railwayLines.value else busLines.value
            }
        }
        
        // Filter by selected operator
        searchData = searchData.filter { it.operatorCode == operatorCode }
        
        android.util.Log.d("SettingsLineViewModel", "filterLine: After operator filter, searchData count=${searchData.size}")
        
        // For GTFS operators, use lines from busLines (already loaded by fetchGTFSLinesForOperator)
        // Load from UserDefaults only if operator has not been changed (onAppear case)
        val dataSource = LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }
        if (dataSource != null && dataSource.apiType() == ODPTAPIType.GTFS && !isChangedOperator.value) {
            // For GTFS operators, load from SharedPreferences only if operator has not been changed (onAppear case)
            searchData = _selectedGoorback.value.loadOperatorLineList(_selectedLineNumber.value - 1, sharedPreferences) ?: emptyList()
        }
        
        // If query is empty but operator is selected, show all lines for that operator
        if (t.isEmpty()) {
            if (selectedTransportationKind.value == TransportationLineKind.BUS) {
                // For GTFS routes, group by lineCode (route_short_name) to avoid duplicates
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
    
    // MARK: - Operator Search and Filtering
    // Filter operators based on search input and transportation kind (railway or bus)
    suspend fun filterOperators(q: String, isFocused: Boolean? = null) = withContext(Dispatchers.Main) {
        val t = q.normalizedForSearch()
        
        // Don't show suggestions if line number or direction is being changed or operator is already selected
        // Don't show suggestions if line number or direction is being changed or line is already selected
        if (isLineNumberChanging.value || isGoOrBackChanging.value || operatorSelected.value) {
            return@withContext
        }
        
        // Use provided isFocused parameter or fall back to current state
        val fieldIsFocused = isFocused ?: isOperatorFieldFocused.value
        
        // Get available operators filtered by transportation kind (railway or bus)
        val availableDataSources = LocalDataSource.entries
            .filter { dataSource ->
                dataSource.transportationType() == selectedTransportationKind.value.toTransportationKind() &&
                dataSource.operatorCode() != null
            }
        
        // Create array of (dataSource, operatorDisplayName) tuples to maintain enum order
        val availableOperatorsWithSource = availableDataSources.mapNotNull { dataSource ->
            dataSource.operatorCode()?.let {
                Pair(dataSource, dataSource.operatorDisplayName(getApplication()))
            }
        }
        
        // Filter operators based on search query
        val filtered = if (t.isEmpty()) {
            // If query is empty, show all operators when field is focused (maintain enum order)
            if (fieldIsFocused) {
                // When field is focused and query is empty, always show all operators
                availableOperatorsWithSource
            } else {
                operatorSuggestions.value = emptyList()
                showOperatorSuggestions.value = false
                return@withContext
            }
        } else {
            // If query is not empty, filter operators based on search input
            // If operator is selected and input matches selected operator, don't show suggestions
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
        
        // Sort results: prioritize operators that start with the query, but maintain enum order within each group
        val sortedResults = if (t.isEmpty()) {
            // If query is empty, maintain enum order (no sorting)
            filtered.map { it.second }
        } else {
            // Prioritize operators that start with the query, but maintain enum order within each group
            val starts = filtered.filter { it.second.normalizedForSearch().startsWith(t) }
            val contains = filtered.filter { !it.second.normalizedForSearch().startsWith(t) }
            starts.map { it.second } + contains.map { it.second }
        }
        
        // Display in enum order (no reversal)
        operatorSuggestions.value = sortedResults.take(20)
        // showOperatorSuggestions = isOperatorFieldFocused && !operatorSuggestions.isEmpty
        showOperatorSuggestions.value = fieldIsFocused && operatorSuggestions.value.isNotEmpty()
    }
    
    // MARK: - Data Processing
    // Remove duplicates based on operator and line name combination
    // Ensures unique line representation in the UI
    fun removeDuplicates(lines: List<TransportationLine>): List<TransportationLine> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<TransportationLine>()
        
        for (line in lines) {
            // Create unique key combining operator code and display name
            val key = "${line.operatorCode ?: ""}_${lineDisplayName(line)}"
            if (!seen.contains(key)) {
                seen.add(key)
                result.add(line)
            }
        }
        
        return result
    }
    
    // MARK: - Station Search and Filtering
    // Filter candidate departure stops based on search input
    // If arrival stop is selected, only show stops before the arrival stop
    fun filterDepartureStops(lineInput: String) {
        val filtered = filterStops(lineInput, excludeStop = _selectedArrivalStop.value, isDeparture = true)
        departureSuggestions.value = filtered
        
        val shouldShow = isDepartureFieldFocused.value && filtered.isNotEmpty() && !departureStopSelected.value
        showDepartureSuggestions.value = shouldShow
    }
    
    // Filter candidate arrival stops based on search input
    // If departure stop is selected, only show stops after the departure stop
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
    
    // MARK: - Input Processing
    // Process departure station input changes and filter suggestions
    fun processDepartureStopInput(newValue: String) {
        // Don't show suggestions if line number is being changed
        if (isLineNumberChanging.value) return
        
        // Set focus state and reset selection flag when input changes
        isDepartureFieldFocused.value = true
        departureStopSelected.value = false
        
        // Clear input if same station as arrival station is entered
        val arrivalStopName = _selectedArrivalStop.value?.title?.getLocalizedName(
            fallbackTo = _selectedArrivalStop.value?.name ?: "") ?: _selectedArrivalStop.value?.name ?: ""
        val isSameAsArrival = arrivalStopName == newValue || arrivalStopInput.value == newValue
        
        if (isSameAsArrival) {
            departureStopInput.value = ""
            _selectedDepartureStop.value = null
        } else {
            // Update departureStopInput value
            departureStopInput.value = newValue
            // Filter suggestions
            filterDepartureStops(newValue)
        }
    }
    
    // Process arrival station input changes and filter suggestions
    fun processArrivalStopInput(newValue: String) {
        // Don't show suggestions if line number is being changed
        if (isLineNumberChanging.value) return
        
        // Set focus state and reset selection flag when input changes
        isArrivalFieldFocused.value = true
        arrivalStopSelected.value = false
        
        // Clear input if same station as departure station is entered
        val departureStopName = _selectedDepartureStop.value?.title?.getLocalizedName(
            fallbackTo = _selectedDepartureStop.value?.name ?: "") ?: _selectedDepartureStop.value?.name ?: ""
        val isSameAsDeparture = departureStopName == newValue || departureStopInput.value == newValue
        
        if (isSameAsDeparture) {
            arrivalStopInput.value = ""
            _selectedArrivalStop.value = null
        } else {
            // Update arrivalStopInput value
            arrivalStopInput.value = newValue
            // Filter suggestions
            filterArrivalStops(newValue)
        }
    }
    
    // Process operator input changes and trigger search/filter
    suspend fun processOperatorInput(newValue: String) = withContext(Dispatchers.Main) {
        // Don't reset operator selection if line number or direction is being changed
        // if isLineNumberChanging || isGoorBackChanging { return }
        if (isLineNumberChanging.value || isGoOrBackChanging.value) return@withContext
        
        // Get current selected operator name for comparison before updating
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
    
    // Process line input changes and trigger search/filter
    // filterLine is called first, then lineInput is updated via binding
    fun processLineInput(newValue: String) {
        // Don't reset station selection if line number or direction is being changed
        // if isLineNumberChanging || isGoorBackChanging { return }
        if (isLineNumberChanging.value || isGoOrBackChanging.value) return
        
        // Trigger filtering when lineInput changes
        // Only show suggestions if operator is selected from dropdown
        viewModelScope.launch {
            filterLine(newValue, isFocused = isLineFieldFocused.value)
        }
        
        // Reset station selection when lineInput changes
        val currentLineName = _selectedLine.value?.name ?: ""
        val currentLineDisplayName = _selectedLine.value?.let { lineDisplayName(it) } ?: ""
        val shouldResetSelection = newValue != currentLineName && newValue != currentLineDisplayName
        
        if (shouldResetSelection) {
            // Clear line selection but preserve station data and ride time
            _selectedLine.value = null
            // DO NOT clear departureStopInput, arrivalStopInput, or selectedRideTime
            _selectedDepartureStop.value = null
            _selectedArrivalStop.value = null
            showDepartureSuggestions.value = false
            departureSuggestions.value = emptyList()
            showArrivalSuggestions.value = false
            arrivalSuggestions.value = emptyList()
            isDepartureFieldFocused.value = false
            isArrivalFieldFocused.value = false
            // Reset station selection flags to allow suggestions to show
            departureStopSelected.value = false
            arrivalStopSelected.value = false
            lineSelected.value = false
        }
        
        // Update lineInput value (lineInput is updated via binding, but we need to set it explicitly)
        lineInput.value = newValue
        
        // Show station selection UI for custom line input
        if (newValue.isNotEmpty()) {
            showStationSelection.value = true
        }
    }
    
    // MARK: - Line Selection Management
    // Handles line selection and updates all related state
    fun selectLine(line: TransportationLine) {
        // Set line number changing flag to prevent unwanted suggestions
        isLineNumberChanging.value = true
        
        // Set selectedLine for proper filtering
        _selectedLine.value = line
        
        // Update display name with operator information on selection
        lineInput.value = lineDisplayName(line)
        
        // Clear departure and arrival stop inputs when line is selected
        departureStopInput.value = ""
        arrivalStopInput.value = ""
        _selectedDepartureStop.value = null
        _selectedArrivalStop.value = null
        
        // Initialize lineBusStops for bus routes
        if (line.kind == TransportationLineKind.BUS) {
            line.busStopPoleOrder?.let { busStopPoleOrder ->
                lineBusStops.value = busStopPoleOrder
                
                // Check if any bus stops need Japanese names and fetch them once
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
                // For GTFS routes, fetch bus stops from GTFS data
                line.operatorCode?.let { operatorCode ->
                    LocalDataSource.entries.firstOrNull { it.operatorCode() == operatorCode }?.let { dataSource ->
                        if (dataSource.apiType() == ODPTAPIType.GTFS) {
                            // Clear line bus stops first to avoid showing old data while loading
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
            // For railway lines, use odpt:stationOrder from Railway JSON
            // TrainTimetable API is only used for timetable generation, not for station list
            lineBusStops.value = emptyList()
            
            if (line.stationOrder != null && line.stationOrder.isNotEmpty()) {
                // Use stationOrder from parsed Railway data (no API call needed)
                lineStations.value = line.stationOrder
            } else {
                // If stationOrder is not available, stations cannot be determined
                // This should not happen if Railway JSON is properly parsed
                lineStations.value = emptyList()
                android.util.Log.d("SettingsLineViewModel", "⚠️ selectLine: stationOrder is null or empty for line ${line.code}")
            }
        }
        
        // Update line stops immediately based on line type
        _lineStops.value = getStopsForSelectedLine()
        
        // Preserve existing station and ride time settings instead of clearing them
        // Only clear suggestion displays
        showDepartureSuggestions.value = false
        departureSuggestions.value = emptyList()
        showArrivalSuggestions.value = false
        arrivalSuggestions.value = emptyList()
        isDepartureFieldFocused.value = false
        isArrivalFieldFocused.value = false
        
        // Set line color or default to accent color
        selectedLineColor.value = line.lineColor ?: AccentString
        
        // Reset flag after a short delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            isLineNumberChanging.value = false
        }
    }
    
    // MARK: - Form Data Management
    // Clears all form data and resets to initial state
    // Also resets all focus states
    fun clearAllFormData() {
        // Clear operator name
        operatorInput.value = ""
        operatorSuggestions.value = emptyList()
        showOperatorSuggestions.value = false
        operatorSelected.value = false
        showOperatorSelection.value = false
        selectedOperatorCode.value = null
        
        // Clear line name
        lineInput.value = ""
        
        // Reset station selection
        resetStationSelection()
        
        // Clear departure and arrival station input fields
        departureStopInput.value = ""
        arrivalStopInput.value = ""
        
        // Reset ride time to 0 minutes
        selectedRideTimeFlow.value = 0
        
        // Reset line color to accent (not saved to SharedPreferences)
        selectedLineColor.value = AccentString
        
        // Reset transfer settings to none
        selectedTransportation.value = "none"
        selectedTransferTime.value = 0
        
        // Hide color selection UI
        showColorSelection.value = false
    }
    
    // Reset station selection and clear all related state
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
    
    // MARK: - Set Line Color
    // Set line color without saving to SharedPreferences
    // Update selected color for display only (saved when user explicitly saves)
    fun setLineColor(color: String) {
        selectedLineColor.value = color
        showColorSelection.value = false
    }
    
    // MARK: - Transportation Kind Switching
    // Handle transportation kind switching without clearing data
    fun switchTransportationKind(isRailway: Boolean) {
        // Update transportation kind immediately for responsive UI
        selectedTransportationKind.value = if (isRailway) TransportationLineKind.RAILWAY else TransportationLineKind.BUS
        
        // Clear only suggestions to prevent UI conflicts
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
        
        // Clear operator suggestions and reset operator selection when switching transportation kind
        // operatorInput and lineInput are kept, but operatorSelected and selectedOperatorCode are reset
        operatorSuggestions.value = emptyList()
        showOperatorSuggestions.value = false
        operatorSelected.value = false
        selectedOperatorCode.value = null
        
        // Reset line selection state when switching transportation kind
        // lineInput is kept, but lineSelected and selectedLine are reset
        lineSelected.value = false
        _selectedLine.value = null
        
        // Load data for the new kind using the same logic as loadFromCache
        // This ensures data is loaded from CacheStore, LineData directory, or API if needed
        viewModelScope.launch {
            try {
                // Use loadFromCache() to ensure consistent data loading logic
                // This will load from CacheStore, LineData directory, or fetch from API if needed
                loadFromCache()
            } catch (e: Exception) {
                android.util.Log.d("SettingsLineViewModel", "switchTransportationKind: loadFromCache() failed: ${e.message}", e)
            }
            
            // Re-filter existing data if line input exists
            // Re-filter line suggestions if lineInput is not empty
            if (lineInput.value.isNotEmpty() && lineInput.value.trim().isNotEmpty()) {
                filterLine(lineInput.value, isFocused = isLineFieldFocused.value)
            }
            
            // Re-filter operator suggestions if operator input exists
            // Re-filter operator suggestions if operatorInput is not empty
            if (operatorInput.value.isNotEmpty() && operatorInput.value.trim().isNotEmpty()) {
                filterOperators(operatorInput.value, isFocused = isOperatorFieldFocused.value)
            }
        }
    }
    
    // MARK: - GTFS Lines Fetching
    // Fetch GTFS lines from ZIP cache for selected operator
    suspend fun fetchGTFSLinesForOperator(dataSource: LocalDataSource) = withContext(Dispatchers.Main) {
        // Check if consumerKey is required and available
        // TOEI_BUS uses api-public.odpt.org, so consumerKey is not required
        if (dataSource != LocalDataSource.TOEI_BUS && consumerKey.isEmpty()) {
            android.util.Log.d("SettingsLineViewModel", "🚌 fetchGTFSLinesForOperator: consumerKey is empty but required for ${dataSource.name}. Please set ODPT_ACCESS_TOKEN in AndroidManifest.xml.")
            _lineSuggestions.value = emptyList()
            showLineSuggestions.value = false
            isLoadingLines.value = false
            loadingMessage = null
            return@withContext
        }
        
        // Clear line suggestions immediately to prevent showing old data
        _lineSuggestions.value = emptyList()
        showLineSuggestions.value = false
        isLoadingLines.value = true
        loadingMessage = getApplication<Application>().getString(R.string.loadingLines)
        
        try {
            // Fetch GTFS lines from ZIP cache using GTFSDataService
            val gtfsLines = withContext(Dispatchers.IO) {
                gtfsService.fetchGTFSData(dataSource, consumerKey)
            }
            android.util.Log.d("SettingsLineViewModel", "🚌 fetchGTFSLinesForOperator: Fetched ${gtfsLines.size} GTFS lines for ${dataSource.operatorDisplayName(getApplication())}")
            
            // Update busLines with fetched GTFS lines
            // Remove existing lines for this operator first to avoid duplicates
            busLines.value = busLines.value.filter { it.operatorCode != dataSource.operatorCode() } + gtfsLines
            
            // Also update all arrays to prevent duplicate loading
            all = all.filter { it.operatorCode != dataSource.operatorCode() } + gtfsLines
            allData = all.filter { it.operatorCode != dataSource.operatorCode() } + gtfsLines
            
            // Update lineSuggestions with fetched lines
            // Filter by selected operator
            val expectedOperatorCode = dataSource.operatorCode()
            val filteredLines = gtfsLines.filter { 
                val matches = it.operatorCode == expectedOperatorCode
                matches
            }
            
            // Group by lineCode (route_short_name) to avoid duplicates
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
            android.util.Log.d("SettingsLineViewModel", "🚌 fetchGTFSLinesForOperator: Failed to fetch GTFS lines for ${dataSource.operatorDisplayName(getApplication())}: ${e.message}", e)
            e.printStackTrace()
            _lineSuggestions.value = emptyList()
            showLineSuggestions.value = false
        } finally {
            isLoadingLines.value = false
            loadingMessage = null
        }
    }
    
    // MARK: - GTFS Bus Stops Fetching
    // Fetch bus stops for a GTFS route from GTFS data
    private suspend fun fetchGTFSStopsForRoute(routeId: String, transportOperator: LocalDataSource) = withContext(Dispatchers.IO) {
        try {
            if (transportOperator != LocalDataSource.TOEI_BUS && consumerKey.isEmpty()) {
                android.util.Log.d("SettingsLineViewModel", "🚌 fetchGTFSStopsForRoute: consumerKey is empty but required for ${transportOperator.name}")
                return@withContext
            }

            val stops = gtfsService.fetchGTFSStopsForRoute(
                routeId = routeId,
                transportOperator = transportOperator,
                consumerKey = consumerKey
            )
            
            withContext(Dispatchers.Main) {
                // stops are already TransportationStop objects
                lineBusStops.value = stops
                android.util.Log.d("SettingsLineViewModel", "fetchGTFSStopsForRoute: Loaded ${stops.size} stops for routeId=$routeId")
                // Update lineStops to reflect the fetched bus stops
                _lineStops.value = getStopsForSelectedLine()
                // If a stop field is focused, re-run filtering to show suggestions.
                if (isDepartureFieldFocused.value) {
                    filterDepartureStops(departureStopInput.value)
                }
                if (isArrivalFieldFocused.value) {
                    filterArrivalStops(arrivalStopInput.value)
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("SettingsLineViewModel", "❌ Failed to fetch GTFS stops for route $routeId: ${e.message}", e)
        }
    }
    
    // MARK: - BusStopPole API Integration
    // Fetches Japanese names for all bus stops in the selected route
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
                android.util.Log.d("SettingsLineViewModel", "fetchJapaneseNamesForAllBusStops: HTTP ${response.code}")
                return@withContext
            }

            val jsonElement = JsonParser.parseString(String(data))
            if (!jsonElement.isJsonArray) {
                android.util.Log.d("SettingsLineViewModel", "fetchJapaneseNamesForAllBusStops: response is not array")
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
            android.util.Log.d("SettingsLineViewModel", "fetchJapaneseNamesForAllBusStops: failed ${e.message}", e)
        }
    }
    
    // MARK: - Operator Line List Persistence
    // Save operator line list to SharedPreferences after operator selection
    override fun saveOperatorLineList(lines: List<TransportationLine>, goorback: String, num: Int) {
        goorback.saveOperatorLineList(lines, num, sharedPreferences)
    }

    // MARK: - Line Stop List Persistence
    // Save line stop list to SharedPreferences after line selection
    override fun saveLineStopList(stops: List<TransportationStop>, goorback: String, num: Int) {
        goorback.saveLineStopList(stops, num, sharedPreferences)
    }

    // MARK: - Data Persistence
    // Save all information when save button is pressed
    override suspend fun saveAllDataToUserDefaults() = withContext(Dispatchers.IO) {
        val lineIndex = _selectedLineNumber.value - 1
        
        // When all timetable ride times are the same, update all to the new ride time BEFORE saving rideTimeKey.
        // If called after rideTimeKey is saved, loadTransportationTimes uses the new value as default when
        // timetableRideTimeKey is empty (ODPT not used), causing previousRideTime == newRideTime and early return.
        syncTimetableRideTimeWhenAllSame(lineIndex, selectedRideTimeFlow.value)
        
        sharedPreferences.edit {
            // Save line name
            if (lineInput.value.trim().isNotEmpty()) {
                val lineNameKey = _selectedGoorback.value.lineNameKey(lineIndex)
                putString(lineNameKey, lineInput.value)
            }

            // Save line code for Firestore synchronization
            // Always save lineCode if selectedLine is available, regardless of lineInput
            val lineCodeKey = _selectedGoorback.value.lineCodeKey(lineIndex)
            _selectedLine.value?.let { selectedLine ->
                // Use lineCode property (short code like "JY", "TT") if available
                val codeToSave = if (!selectedLine.lineCode.isNullOrEmpty()) selectedLine.lineCode else ""
                putString(lineCodeKey, codeToSave)
            } ?: run {
                // Preserve existing lineCode if selectedLine is nil
                val existingLineCode = sharedPreferences.getString(lineCodeKey, null) ?: ""
                if (existingLineCode.isEmpty()) {
                    // Try to find lineCode from saved line name using reverse lookup
                    findSavedLineInData()?.let { foundLine ->
                        val codeToSave = if (!foundLine.lineCode.isNullOrEmpty()) foundLine.lineCode else ""
                        putString(lineCodeKey, codeToSave)
                    }
                }
            }

            // Save line color
            // Only save if selectedLineColor is explicitly set (not null)
            // This ensures the color is saved when user explicitly selects a color
            selectedLineColor.value?.let { lineColor ->
                val lineColorKey = _selectedGoorback.value.lineColorKey(lineIndex)
                putString(lineColorKey, lineColor)
            } ?: run {
                // If selectedLineColor is null, try to use line color or default
                val lineColorToSave = _selectedLine.value?.lineColor ?: AccentString
                val lineColorKey = _selectedGoorback.value.lineColorKey(lineIndex)
                putString(lineColorKey, lineColorToSave)
            }

            // Save operator name (consistent with other fields like line name, station names)
            if (operatorInput.value.trim().isNotEmpty()) {
                val operatorNameKey = _selectedGoorback.value.operatorNameKey(lineIndex)
                putString(operatorNameKey, operatorInput.value)
            }
            // Save operator code when operator is selected (even if no line selected yet)
            val operatorCodeToSave = _selectedLine.value?.operatorCode ?: selectedOperatorCode.value
            if (!operatorCodeToSave.isNullOrEmpty()) {
                val operatorCodeKey = _selectedGoorback.value.operatorCodeKey(lineIndex)
                putString(operatorCodeKey, operatorCodeToSave)
            }

            // Save transportation kind
            val lineKindKey = _selectedGoorback.value.lineKindKey(lineIndex)
            putString(lineKindKey, selectedTransportationKind.value.name)

            // Save line direction using lineDirection function or from selectedLine
            val savedLineDirection = _selectedLine.value?.lineDirection 
                ?: _selectedGoorback.value.lineDirection(sharedPreferences, lineIndex)
            if (savedLineDirection.isNotEmpty()) {
                val lineDirectionKey = _selectedGoorback.value.lineDirectionKey(lineIndex)
                putString(lineDirectionKey, savedLineDirection)
            }

            // Save line selected flag
            val lineSelectedKey = _selectedGoorback.value.lineSelectedKey(lineIndex)
            putBoolean(lineSelectedKey, lineSelected.value)

            // Save departure stop information
            if (departureStopInput.value.trim().isNotEmpty()) {
                val departureKey = _selectedGoorback.value.departStationKey(lineIndex)
                putString(departureKey, departureStopInput.value)

                // Save departure stop ODPT code
                _selectedDepartureStop.value?.code?.let { stationCode ->
                    val departureCodeKey = _selectedGoorback.value.departStationCodeKey(lineIndex)
                    putString(departureCodeKey, stationCode)
                }

                // Save departure stop lineCode if available
                _selectedDepartureStop.value?.lineCode?.let { stationLineCode ->
                    val departureLineCodeKey =
                        "${_selectedGoorback.value.departStationCodeKey(lineIndex)}_lineCode"
                    putString(departureLineCodeKey, stationLineCode)
                }
            }

            // Save arrival stop information
            if (arrivalStopInput.value.trim().isNotEmpty()) {
                val arrivalKey = _selectedGoorback.value.arriveStationKey(lineIndex)
                putString(arrivalKey, arrivalStopInput.value)

                // Save arrival stop ODPT code
                _selectedArrivalStop.value?.code?.let { stationCode ->
                    val arrivalCodeKey = _selectedGoorback.value.arriveStationCodeKey(lineIndex)
                    putString(arrivalCodeKey, stationCode)
                }

                // Save arrival stop lineCode if available
                _selectedArrivalStop.value?.lineCode?.let { stationLineCode ->
                    val arrivalLineCodeKey =
                        "${_selectedGoorback.value.arriveStationCodeKey(lineIndex)}_lineCode"
                    putString(arrivalLineCodeKey, stationLineCode)
                }
            }

            // Save ride time
            val rideTimeKey = _selectedGoorback.value.rideTimeKey(lineIndex)
            putInt(rideTimeKey, selectedRideTimeFlow.value)

            // Save transfer settings and calculate transfer count
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

            // Enable direction 2 display when saving data for direction 2
            if (_selectedGoorback.value == "back2" || _selectedGoorback.value == "go2") {
                val route2DisplayKey = _selectedGoorback.value.isShowRoute2Key()
                putBoolean(route2DisplayKey, true)
            }
        }
        
        // Save operator line list to SharedPreferences
        if (_lineSuggestions.value.isNotEmpty()) {
            saveOperatorLineList(_lineSuggestions.value, _selectedGoorback.value, lineIndex)
        }
        
        // Save line stop list to SharedPreferences
        if (lineStops.isNotEmpty()) {
            saveLineStopList(lineStops, _selectedGoorback.value, lineIndex)
        }
        
        // Update display (but don't override selectedLineColor if user has explicitly set it)
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
    
    // MARK: - Handle Line Save
    // Common save processing for all line types
    // Saves all current settings to persistent storage
    suspend fun handleLineSave() {
        // Ensure selectedLineColor is set before saving
        // If not explicitly set by user, use line color or default to accent
        if (selectedLineColor.value == null) {
            selectedLineColor.value = _selectedLine.value?.lineColor ?: AccentString
        }
        
        saveAllDataToUserDefaults()
        
        // Post notification to update MainContentView
        // Use SharedPreferences to trigger OnSharedPreferenceChangeListener
        sharedPreferences.edit {
            putLong("SettingsLineUpdated", System.currentTimeMillis())
        }
    }
    
    // MARK: - Auto Generate Timetable
    // Automatically generate timetable data using SettingsTimetableViewModel
    suspend fun autoGenerateTimetable() = withContext(Dispatchers.Main) {
        val hasStops = _selectedDepartureStop.value != null && _selectedArrivalStop.value != null
        if (_selectedLine.value == null || !hasStops) {
            android.util.Log.d(
                "SettingsLineViewModel",
                "autoGenerateTimetable: skipped (line=${_selectedLine.value?.code}, hasStops=$hasStops)"
            )
            return@withContext
        }

        android.util.Log.d(
            "SettingsLineViewModel",
            "autoGenerateTimetable: start line=${_selectedLine.value?.code} kind=${_selectedLine.value?.kind} dep=${_selectedDepartureStop.value?.code} arr=${_selectedArrivalStop.value?.code}"
        )
        isLoadingTimetable = true
        loadingMessage = getApplication<Application>().getString(R.string.generatingTimetable)
        
        try {
            // Create SettingsTimetableViewModel instance
            val timetableViewModel = SettingsTimetableViewModel(
                application = getApplication(),
                sharedPreferences = sharedPreferences,
                lineViewModel = this@SettingsLineViewModel
            )
            
            // Generate timetable data
            val timetableData = timetableViewModel.getTimeTableData()
            if (timetableData.isEmpty()) {
                android.util.Log.d("SettingsLineViewModel", "autoGenerateTimetable: no timetable data generated")
            }
            
            // Finalize and save timetable data to SharedPreferences
            if (timetableData.isNotEmpty()) {
                timetableViewModel.finalizeTimetableData(timetableData)
            }
            
        } catch (e: Exception) {
            android.util.Log.d("SettingsLineViewModel", "Failed to auto-generate timetable: ${e.message}", e)
        } finally {
            isLoadingTimetable = false
            loadingMessage = null
        }
    }
    
}

