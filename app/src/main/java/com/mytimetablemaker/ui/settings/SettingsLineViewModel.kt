package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.Context
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
import java.util.*

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
    private val consumerKey: String = "" // TODO: Get from BuildConfig or secure storage
    
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
    val isLoading = MutableStateFlow(false)
    val lastUpdatedDisplay = MutableStateFlow<String?>(null)
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
    
    val _selectedRideTime = MutableStateFlow(0)
    val selectedRideTimeState: StateFlow<Int> = _selectedRideTime.asStateFlow()
    override val selectedRideTime: Int
        get() = _selectedRideTime.value
    
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
    
    val availableLineNumbers = MutableStateFlow<List<Int>>(listOf(1))
    val isLineNumberChanging = MutableStateFlow(false)
    val isGoorBackChanging = MutableStateFlow(false)
    val isChangedOperator = MutableStateFlow(false)
    
    private val _selectedGoorback = MutableStateFlow("back1")
    val selectedGoorbackState: StateFlow<String> = _selectedGoorback.asStateFlow()
    override val selectedGoorback: String
        get() = _selectedGoorback.value
    
    // MARK: - Computed Properties
    val hasSelectedLine: Boolean
        get() = _selectedLine.value != null
    
    val hasStops: Boolean
        get() = _lineStops.value.isNotEmpty()
    
    override val isAllNotEmpty: Boolean
        get() = !departureStopInput.value.isEmpty() &&
                !arrivalStopInput.value.isEmpty() &&
                !lineInput.value.isEmpty() &&
                (hasTimetableSupport() || _selectedRideTime.value > 0)
    
    val isAllSelected: Boolean
        get() = lineSelected.value && departureStopSelected.value && arrivalStopSelected.value
    
    // Get localized display names for direction options
    val goorbackDisplayNames: Map<String, String>
        get() = goorbackDisplayNamesRaw.mapValues { (_, value) -> value.localized(getApplication()) }
    
    // MARK: - Private Properties
    // Internal data storage and state management
    private var all: List<TransportationLine> = emptyList()
    private var allData: List<TransportationLine> = emptyList()
    val railwayLines = MutableStateFlow<List<TransportationLine>>(emptyList())
    val busLines = MutableStateFlow<List<TransportationLine>>(emptyList())
    val nameCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    
    // Station data files for railway lines
    private val stationDataFiles: List<String> = LocalDataSource.values()
        .filter { it.transportationType() == TransportationKind.RAILWAY }
        .map { it.fileName() }
    
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
        val validGoorback = if (goorback.isEmpty() || !goorbackOptions.contains(goorback)) "back1" else goorback
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
            sharedPreferences.edit().putInt(_selectedGoorback.value.changeLineKey(), 0).apply()
        }
        
        updateAvailableLineNumbers(shouldPreserveLineNumber = false)
        loadSettingsForSelectedLine()
        
        // Load data from cache in background
        viewModelScope.launch {
            loadFromCache()
        }
    }
    
    // MARK: - Helper Functions
    // Get localized display name for transportation line
    fun lineDisplayName(line: TransportationLine): String {
        val currentLanguage = Locale.getDefault().language
        
        if (line.kind == TransportationLineKind.BUS) {
            // For GTFS routes, use railwayTitle if available (it contains localized name)
            line.railwayTitle?.let { railwayTitle ->
                return railwayTitle.getLocalizedName(getApplication(), fallbackTo = line.name)
            }
            // For ODPT bus routes, use title or fallback to name
            if (currentLanguage == "ja") {
                return line.title ?: line.name
            } else {
                return line.busRouteEnglishName() ?: line.railwayTitle?.en ?: line.name
            }
        }
        
        line.railwayTitle?.let { railwayTitle ->
            return railwayTitle.getLocalizedName(getApplication(), fallbackTo = line.name)
        }
        return line.name
    }
    
    // Get localized display name based on operator code
    fun getOperatorDisplayName(operatorCode: String, lineKind: TransportationLineKind? = null): String {
        // Find matching LocalDataSource by operator code and transportation kind
        val matchingDataSources = LocalDataSource.values().filter { dataSource ->
            dataSource.operatorCode() == operatorCode
        }
        
        // If lineKind is provided, prioritize matching transportation type
        lineKind?.let { kind ->
            matchingDataSources.firstOrNull { it.transportationType() == kind.toTransportationKind() }?.let { dataSource ->
                return dataSource.operatorDisplayName(getApplication())
            }
        }
        
        // Fallback to first matching data source if no lineKind or no match found
        matchingDataSources.firstOrNull()?.let { dataSource ->
            return dataSource.operatorDisplayName(getApplication())
        }
        
        // Final fallback: extract operator name from operator code
        return operatorCode.replace("odpt.Operator:", "")
    }
    
    // Get short display name for CustomTag based on operator code
    fun getOperatorDisplayNameForTag(operatorCode: String, lineKind: TransportationLineKind? = null): String {
        // Find matching LocalDataSource by operator code and transportation kind
        val matchingDataSources = LocalDataSource.values().filter { dataSource ->
            dataSource.operatorCode() == operatorCode
        }
        
        // If lineKind is provided, prioritize matching transportation type
        lineKind?.let { kind ->
            matchingDataSources.firstOrNull { it.transportationType() == kind.toTransportationKind() }?.let { dataSource ->
                return dataSource.operatorShortDisplayName(getApplication())
            }
        }
        
        // Fallback to first matching data source if no lineKind or no match found
        matchingDataSources.firstOrNull()?.let { dataSource ->
            return dataSource.operatorShortDisplayName(getApplication())
        }
        
        // Final fallback: extract operator name from operator code
        return operatorCode.replace("odpt.Operator:", "")
    }
    
    // MARK: - Check Timetable Support
    // Check if selected line has timetable support (train or bus)
    fun hasTimetableSupport(): Boolean {
        val operatorCode = _selectedLine.value?.operatorCode ?: return false
        val dataSource = LocalDataSource.values().firstOrNull { it.operatorCode() == operatorCode } ?: return false
        // TODO: Implement hasTrainTimeTable and hasBusTimeTable in LocalDataSource
        // For now, return true for all operators
        return true
    }
    
    // MARK: - Data Management
    // Load data from cache for better performance
    // Only load data for the currently selected transportation kind to improve efficiency
    private suspend fun loadFromCache() = withContext(Dispatchers.IO) {
        val kind = selectedTransportationKind.value.toTransportationKind()
        val loadedLines = mutableListOf<TransportationLine>()
        
        // Load data from cache for all operators of the selected kind
        for (transportOperator in LocalDataSource.values()) {
            if (transportOperator.transportationType() != kind) continue
            
            // Handle GTFS operators separately (they don't have JSON cache)
            if (transportOperator.apiType() == ODPTAPIType.GTFS) {
                continue // GTFS lines will be loaded when operator is selected
            }
            
            val cacheKey = transportOperator.fileName()
            val cachedData = cache.loadData(cacheKey)
            
            if (cachedData != null) {
                try {
                    val lines = if (kind == TransportationKind.RAILWAY) {
                        ODPTParser.parseRailwayRoutes(cachedData)
                    } else {
                        ODPTParser.parseBusRoutes(cachedData)
                    }
                    loadedLines.addAll(lines)
                } catch (e: Exception) {
                    android.util.Log.e("SettingsLineViewModel", "Failed to parse cached data for ${transportOperator.name}: ${e.message}")
                }
            }
        }
        
        // Update state on main thread
        withContext(Dispatchers.Main) {
            all = loadedLines
            allData = all
            railwayLines.value = loadedLines.filter { it.kind == TransportationLineKind.RAILWAY }
            busLines.value = loadedLines.filter { it.kind == TransportationLineKind.BUS }
            
            // Check if saved line exists in loaded data and restore it
            viewModelScope.launch {
                checkSavedLineInData()
                isLineNumberChanging.value = false
            }
        }
    }
    
    // MARK: - Direction Selection Management
    // Handle goorback selection changes and reset related state
    fun selectGoorback(newGoorback: String) {
        // Early return if same value to avoid unnecessary processing
        if (_selectedGoorback.value == newGoorback) return
        
        // Set flag to indicate route is changing
        isGoorBackChanging.value = true
        
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
            isGoorBackChanging.value = false
        }
    }
    
    // MARK: - Line Number Management
    // Update available line numbers with option to preserve current line number
    private fun updateAvailableLineNumbers(shouldPreserveLineNumber: Boolean) {
        val changeLineValue = sharedPreferences.getInt(_selectedGoorback.value.changeLineKey(), 0)
        val maxLineNumber = minOf(changeLineValue + 1, 3)
        availableLineNumbers.value = (1..maxLineNumber).toList()
        
        // Reset transportation settings for lines beyond current transfer count
        for (i in (changeLineValue + 2)..4) {
            val transportationKey = _selectedGoorback.value.transportationKey(i)
            sharedPreferences.edit().putString(transportationKey, "none").apply()
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
        selectedTransportationKind.value = if (savedKindString != null) {
            savedKindString.fromString() ?: TransportationLineKind.RAILWAY
        } else {
            TransportationLineKind.RAILWAY
        }
        
        // Load operator name (consistent with other fields like line name, station names)
        val operatorNameKey = _selectedGoorback.value.operatorNameKey(currentLineIndex)
        val savedOperatorName = sharedPreferences.getString(operatorNameKey, null)
        
        if (savedOperatorName != null && savedOperatorName.isNotEmpty()) {
            operatorInput.value = savedOperatorName
            
            // Restore operator code from operator name for filtering
            val dataSource = LocalDataSource.values().firstOrNull {
                it.transportationType() == selectedTransportationKind.value.toTransportationKind() &&
                it.operatorDisplayName(getApplication()) == savedOperatorName
            }
            
            if (dataSource != null) {
                selectedOperatorCode.value = dataSource.operatorCode()
                operatorSelected.value = true
                
                // Load operator line list from SharedPreferences only for GTFS bus routes
                if (selectedTransportationKind.value == TransportationLineKind.BUS && 
                    dataSource.apiType() == ODPTAPIType.GTFS) {
                    loadOperatorLineList(_selectedGoorback.value, currentLineIndex)?.let { savedLineList ->
                        _lineSuggestions.value = savedLineList
                        showLineSuggestions.value = false // Don't show suggestions when sheet is opened
                    }
                }
                
                // Load line name and restore line object from line name for filtering
                val lineNameKey = _selectedGoorback.value.lineNameKey(currentLineIndex)
                val savedLineName = sharedPreferences.getString(lineNameKey, null)
                
                if (savedLineName != null && savedLineName.isNotEmpty()) {
                    lineInput.value = savedLineName
                    
                    // Restore line object from line name if data is available
                    if (all.isNotEmpty()) {
                        findSavedLineInData()?.let { foundLine ->
                            _selectedLine.value = foundLine
                            lineSelected.value = true
                            
                            // Load line stop list from SharedPreferences only for GTFS bus routes
                            if (selectedTransportationKind.value == TransportationLineKind.BUS && 
                                dataSource.apiType() == ODPTAPIType.GTFS) {
                                loadLineStopList(_selectedGoorback.value, currentLineIndex)?.let { savedStopList ->
                                    _lineStops.value = savedStopList
                                    // Also update lineBusStops based on loaded stops
                                    lineBusStops.value = savedStopList
                                } ?: run {
                                    // If no saved stop list, set up line stops from line data
                                    setupLineStops(foundLine)
                                }
                            } else {
                                // If not GTFS bus, set up line stops from line data
                                setupLineStops(foundLine)
                            }
                        } ?: run {
                            _selectedLine.value = null
                            lineStations.value = emptyList()
                            lineBusStops.value = emptyList()
                            _lineStops.value = emptyList()
                            lineSelected.value = false
                        }
                    } else {
                        // If all data is not loaded yet, try to load line stop list from SharedPreferences only for GTFS bus routes
                        if (selectedTransportationKind.value == TransportationLineKind.BUS && 
                            dataSource.apiType() == ODPTAPIType.GTFS) {
                            loadLineStopList(_selectedGoorback.value, currentLineIndex)?.let { savedStopList ->
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
                    lineSelected.value = false
                }
            } else {
                selectedOperatorCode.value = null
                operatorSelected.value = false
            }
        } else {
            operatorInput.value = ""
            selectedOperatorCode.value = null
            operatorSelected.value = false
        }
        
        // Load line name and restore line object from line name for filtering (when operator is not saved)
        val lineNameKey = _selectedGoorback.value.lineNameKey(currentLineIndex)
        val savedLineName = sharedPreferences.getString(lineNameKey, null)
        
        if (savedLineName != null && savedLineName.isNotEmpty() && selectedOperatorCode.value == null) {
            lineInput.value = savedLineName
            
            // Restore line object from line name if data is available
            if (all.isNotEmpty()) {
                findSavedLineInData()?.let { foundLine ->
                    _selectedLine.value = foundLine
                    lineSelected.value = true
                    setupLineStops(foundLine)
                } ?: run {
                    _selectedLine.value = null
                    lineStations.value = emptyList()
                    lineBusStops.value = emptyList()
                    _lineStops.value = emptyList()
                    lineSelected.value = false
                }
            }
        } else if (savedLineName == null || savedLineName.isEmpty()) {
            lineInput.value = ""
            _selectedLine.value = null
            lineStations.value = emptyList()
            lineBusStops.value = emptyList()
            _lineStops.value = emptyList()
            lineSelected.value = false
        }
        
        // Load line color
        val colorKey = _selectedGoorback.value.lineColorKey(currentLineIndex)
        selectedLineColor.value = sharedPreferences.getString(colorKey, null)
        
        // Load departure station name and restore station object from station name
        // Split by ":" and return first component for ODPT format
        val departureKey = _selectedGoorback.value.departStationKey(currentLineIndex)
        val savedDeparture = sharedPreferences.getString(departureKey, null)
        
        if (savedDeparture != null && savedDeparture.isNotEmpty()) {
            // Split by ":" and return first component for ODPT format
            val components = savedDeparture.split(":")
            val displayDeparture = components.firstOrNull()?.trim() ?: savedDeparture
            departureStopInput.value = displayDeparture
            
            // Restore departure station object from station name if line stops are available
            if (lineStops.isNotEmpty()) {
                val foundStop = lineStops.firstOrNull { stop ->
                    stop.name == savedDeparture ||
                    stop.name == displayDeparture ||
                    stop.title?.ja == savedDeparture ||
                    stop.title?.ja == displayDeparture ||
                    stop.title?.en == savedDeparture ||
                    stop.title?.en == displayDeparture ||
                    stop.displayName(getApplication()) == displayDeparture
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
        val arrivalKey = _selectedGoorback.value.arriveStationKey(currentLineIndex)
        val savedArrival = sharedPreferences.getString(arrivalKey, null)
        
        if (savedArrival != null && savedArrival.isNotEmpty()) {
            // Split by ":" and return first component for ODPT format
            val components = savedArrival.split(":")
            val displayArrival = components.firstOrNull()?.trim() ?: savedArrival
            arrivalStopInput.value = displayArrival
            
            // Restore arrival station object from station name if line stops are available
            if (lineStops.isNotEmpty()) {
                val foundStop = lineStops.firstOrNull { stop ->
                    stop.name == savedArrival ||
                    stop.name == displayArrival ||
                    stop.title?.ja == savedArrival ||
                    stop.title?.ja == displayArrival ||
                    stop.title?.en == savedArrival ||
                    stop.title?.en == displayArrival ||
                    stop.displayName(getApplication()) == displayArrival
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
        val savedRideTime = sharedPreferences.getInt(rideTimeKey, 0)
        _selectedRideTime.value = if (savedRideTime > 0) savedRideTime else 0
        
        // Load transfer settings
        if (_selectedLineNumber.value < 3) {
            val transportationKey = _selectedGoorback.value.transportationKey(currentLineIndex + 2)
            val savedTransportation = sharedPreferences.getString(transportationKey, null)
            selectedTransportation.value = if (savedTransportation != null && savedTransportation.isNotEmpty()) {
                savedTransportation
            } else {
                "none"
            }
            
            val transferTimeKey = _selectedGoorback.value.transferTimeKey(currentLineIndex + 2)
            val savedTransferTime = sharedPreferences.getInt(transferTimeKey, 0)
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
                line.railwayTitle?.getLocalizedName(getApplication(), fallbackTo = line.name) == lineInput.value ||
                line.busRouteEnglishName() == lineInput.value
            } else {
                line.name == lineInput.value || 
                line.railwayTitle?.getLocalizedName(getApplication(), fallbackTo = line.name) == lineInput.value
            }
        }
    }
    
    // Helper method to set up line stops after finding a line
    // Handles bus stops and updates lineStops for station selection
    private fun setupLineStops(foundLine: TransportationLine) {
        // Set bus stops for bus routes
        if (foundLine.kind == TransportationLineKind.BUS) {
            foundLine.busstopPoleOrder?.let { busstopPoleOrder ->
                lineBusStops.value = busstopPoleOrder
                
                val busStops = busstopPoleOrder.mapNotNull { busStop ->
                    if (busStop.name.isEmpty()) null
                    else TransportationStop(
                        kind = TransportationLineKind.BUS,
                        name = busStop.name,
                        code = busStop.code,
                        index = busStop.index,
                        lineCode = foundLine.code,
                        title = busStop.title,
                        note = busStop.note,
                        busstopPole = busStop.busstopPole
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
    private fun loadStationSettings() {
        val currentLineIndex = _selectedLineNumber.value - 1
        
        val departureKey = _selectedGoorback.value.departStationKey(currentLineIndex)
        sharedPreferences.getString(departureKey, null)?.let { savedDeparture ->
            // Split by ":" and return first component for ODPT format
            val components = savedDeparture.split(":")
            departureStopInput.value = components.firstOrNull()?.trim() ?: savedDeparture
        } ?: run {
            departureStopInput.value = ""
        }
        
        val arrivalKey = _selectedGoorback.value.arriveStationKey(currentLineIndex)
        sharedPreferences.getString(arrivalKey, null)?.let { savedArrival ->
            // Split by ":" and return first component for ODPT format
            val components = savedArrival.split(":")
            arrivalStopInput.value = components.firstOrNull()?.trim() ?: savedArrival
        } ?: run {
            arrivalStopInput.value = ""
        }
    }
    
    // MARK: - Get Stops for Selected Line
    // Get stops information for the selected line (unified for both railway and bus)
    fun getStopsForSelectedLine(): List<TransportationStop> {
        val selectedLine = _selectedLine.value ?: return emptyList()
        
        if (selectedLine.kind == TransportationLineKind.BUS) {
            // Handle bus routes - use lineBusStops if available, otherwise fallback to busstopPoleOrder
            if (lineBusStops.value.isNotEmpty()) {
                return lineBusStops.value.map { busStop ->
                    // Check if we need to fetch Japanese name from API
                    val hasJapaneseInNote = busStop.note?.any { it.isJapanese() } == true
                    
                    if ((!hasJapaneseInNote || busStop.note.isNullOrEmpty()) && !busStop.busstopPole.isNullOrEmpty()) {
                        // Japanese name will be fetched later in selectLine
                        // Create TransportationStop with Japanese name from title if available
                        TransportationStop(
                            kind = TransportationLineKind.BUS,
                            name = busStop.title?.ja ?: busStop.name,
                            code = busStop.code,
                            index = busStop.index,
                            lineCode = busStop.lineCode,
                            title = busStop.title,
                            busstopPole = busStop.busstopPole
                        )
                    } else {
                        // Use existing TransportationStop (already a TransportationStop)
                        busStop
                    }
                }
            } else if (selectedLine.busstopPoleOrder != null) {
                return selectedLine.busstopPoleOrder!!.map { busStop ->
                    // Check if note is empty or doesn't contain Japanese characters
                    val hasJapaneseInNote = busStop.note?.any { it.isJapanese() } == true
                    if ((!hasJapaneseInNote || busStop.note.isNullOrEmpty()) && !busStop.busstopPole.isNullOrEmpty()) {
                        // Japanese name will be fetched later in selectLine
                    }
                    
                    busStop
                }
            } else {
                // lineBusStops is empty and busstopPoleOrder is not available
                // Bus stops will be fetched in selectLine method, not here
                return emptyList()
            }
        } else {
            // Handle railway lines - get stations from data files
            // TODO: Implement loadLocalData and parseStationsByLineCode
            // For now, return empty list
            return emptyList()
        }
    }
    
    // MARK: - Search and Filtering
    // Filter railway lines based on search input with performance optimizations
    suspend fun filterLine(q: String) = withContext(Dispatchers.Main) {
        val t = q.normalizedForSearch()
        
        // Don't show suggestions if line number or direction is being changed or line is already selected
        if (isLineNumberChanging.value || isGoorBackChanging.value || lineSelected.value) return@withContext
        
        // Don't show line suggestions if operator is not selected from dropdown
        val operatorCode = selectedOperatorCode.value
        if (operatorCode == null || !operatorSelected.value) {
            _lineSuggestions.value = emptyList()
            nameCounts.value = emptyMap()
            showLineSuggestions.value = false
            return@withContext
        }
        
        // Filter by transportation kind (.railway or .bus)
        var searchData = if (selectedTransportationKind.value == TransportationLineKind.RAILWAY) {
            railwayLines.value
        } else {
            busLines.value
        }
        
        // Filter by selected operator
        searchData = searchData.filter { it.operatorCode == operatorCode }
        
        // For GTFS operators, use lines from SharedPreferences only if operator has not been changed
        val dataSource = LocalDataSource.values().firstOrNull { it.operatorCode() == operatorCode }
        if (dataSource != null && dataSource.apiType() == ODPTAPIType.GTFS && !isChangedOperator.value) {
            loadOperatorLineList(_selectedGoorback.value, _selectedLineNumber.value - 1)?.let {
                searchData = it
            }
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
                            seenLineCodes.add(line.lineCode!!)
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
                showLineSuggestions.value = isLineFieldFocused.value && _lineSuggestions.value.isNotEmpty()
                nameCounts.value = emptyMap()
            } else {
                val uniqueResults = removeDuplicates(searchData)
                _lineSuggestions.value = uniqueResults
                showLineSuggestions.value = isLineFieldFocused.value && _lineSuggestions.value.isNotEmpty()
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
                p.railwayTitle?.getLocalizedName(getApplication(), fallbackTo = p.name)?.normalizedForSearch() 
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
                        seenLineCodes.add(line.lineCode!!)
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
            showLineSuggestions.value = isLineFieldFocused.value && 
                lineDisplayName(singleLine).normalizedForSearch() != q.normalizedForSearch()
        } else {
            showLineSuggestions.value = isLineFieldFocused.value && _lineSuggestions.value.isNotEmpty()
        }
        
        nameCounts.value = _lineSuggestions.value.groupBy { lineDisplayName(it) }
            .mapValues { it.value.size }
    }
    
    // MARK: - Operator Search and Filtering
    // Filter operators based on search input and transportation kind (railway or bus)
    suspend fun filterOperators(q: String) = withContext(Dispatchers.Main) {
        val t = q.normalizedForSearch()
        
        // Don't show suggestions if line number or direction is being changed or operator is already selected
        if (isLineNumberChanging.value || isGoorBackChanging.value || operatorSelected.value) return@withContext
        
        // Get available operators filtered by transportation kind (railway or bus)
        val availableDataSources = LocalDataSource.values()
            .filter { dataSource ->
                dataSource.transportationType() == selectedTransportationKind.value.toTransportationKind() &&
                dataSource.operatorCode() != null
            }
        
        // Create array of (dataSource, operatorDisplayName) tuples to maintain enum order
        val availableOperatorsWithSource = availableDataSources.mapNotNull { dataSource ->
            dataSource.operatorCode()?.let { operatorCode ->
                Pair(dataSource, dataSource.operatorDisplayName(getApplication()))
            }
        }
        
        // Filter operators based on search query
        val filtered = if (t.isEmpty()) {
            // If query is empty, show all operators when field is focused (maintain enum order)
            if (isOperatorFieldFocused.value) {
                availableOperatorsWithSource
            } else {
                operatorSuggestions.value = emptyList()
                showOperatorSuggestions.value = false
                return@withContext
            }
        } else {
            // If query is not empty, filter operators based on search input
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
        showOperatorSuggestions.value = isOperatorFieldFocused.value && operatorSuggestions.value.isNotEmpty()
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
        showDepartureSuggestions.value = isDepartureFieldFocused.value && 
            filtered.isNotEmpty() && !departureStopSelected.value
    }
    
    // Filter candidate arrival stops based on search input
    // If departure stop is selected, only show stops after the departure stop
    fun filterArrivalStops(lineInput: String) {
        val filtered = filterStops(lineInput, excludeStop = _selectedDepartureStop.value, isDeparture = false)
        arrivalSuggestions.value = filtered
        showArrivalSuggestions.value = isArrivalFieldFocused.value && 
            filtered.isNotEmpty() && !arrivalStopSelected.value
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
                if (isDeparture) {
                    // For departure stops (bus only): only show stops before the selected arrival stop
                    filtered = filtered.filterIndexed { index, _ -> index < excludeIndex }
                } else {
                    // For arrival stops (bus only): only show stops after the selected departure stop
                    filtered = filtered.filterIndexed { index, _ -> index > excludeIndex }
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
                stop.displayName(getApplication()).contains(lineInput, ignoreCase = true)
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
        val arrivalStopName = _selectedArrivalStop.value?.title?.getLocalizedName(getApplication(), 
            fallbackTo = _selectedArrivalStop.value?.name ?: "") ?: _selectedArrivalStop.value?.name ?: ""
        val isSameAsArrival = arrivalStopName == newValue || arrivalStopInput.value == newValue
        
        if (isSameAsArrival) {
            departureStopInput.value = ""
            _selectedDepartureStop.value = null
        } else {
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
        val departureStopName = _selectedDepartureStop.value?.title?.getLocalizedName(getApplication(),
            fallbackTo = _selectedDepartureStop.value?.name ?: "") ?: _selectedDepartureStop.value?.name ?: ""
        val isSameAsDeparture = departureStopName == newValue || departureStopInput.value == newValue
        
        if (isSameAsDeparture) {
            arrivalStopInput.value = ""
            _selectedArrivalStop.value = null
        } else {
            // Filter suggestions
            filterArrivalStops(newValue)
        }
    }
    
    // Process operator input changes and trigger search/filter
    fun processOperatorInput(newValue: String) {
        // Don't reset operator selection if line number is being changed
        if (isLineNumberChanging.value) return
        
        // Trigger filtering when operatorInput changes
        viewModelScope.launch {
            filterOperators(newValue)
        }
        
        // Get current selected operator name for comparison
        val currentOperatorName = selectedOperatorCode.value?.let { operatorCode ->
            LocalDataSource.values().firstOrNull {
                it.transportationType() == selectedTransportationKind.value.toTransportationKind() &&
                it.operatorCode() == operatorCode
            }?.operatorDisplayName(getApplication())
        }
        
        // Reset operator selection and code when operatorInput changes
        if (newValue.isEmpty()) {
            // Clear operator selection when input is empty
            selectedOperatorCode.value = null
            operatorSelected.value = false
            showOperatorSuggestions.value = false
            operatorSuggestions.value = emptyList()
            
            // Re-filter lines without operator filter
            if (lineInput.value.isNotEmpty()) {
                viewModelScope.launch {
                    filterLine(lineInput.value)
                }
            }
        } else if (operatorSelected.value) {
            // Only reset selection flag if input changes to a different value
            val shouldResetSelection = newValue != currentOperatorName
            
            if (shouldResetSelection) {
                // Reset selection flag if input changes after selection
                operatorSelected.value = false
                showOperatorSuggestions.value = false
                operatorSuggestions.value = emptyList()
                selectedOperatorCode.value = null
                
                // Re-filter lines without operator filter
                if (lineInput.value.isNotEmpty()) {
                    viewModelScope.launch {
                        filterLine(lineInput.value)
                    }
                }
            }
        }
        
        // Show operator selection UI for custom operator input
        if (newValue.isNotEmpty()) {
            showOperatorSelection.value = true
        }
    }
    
    // Process line input changes and trigger search/filter
    fun processLineInput(newValue: String) {
        // Don't reset station selection if line number is being changed
        if (isLineNumberChanging.value) return
        
        // Trigger filtering when lineInput changes
        viewModelScope.launch {
            filterLine(newValue)
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
            line.busstopPoleOrder?.let { busstopPoleOrder ->
                lineBusStops.value = busstopPoleOrder
                
                // Check if any bus stops need Japanese names and fetch them once
                val needsJapaneseNames = busstopPoleOrder.any { busStop ->
                    val hasJapaneseInNote = busStop.note?.any { it.isJapanese() } == true
                    (!hasJapaneseInNote || busStop.note.isNullOrEmpty()) && !busStop.busstopPole.isNullOrEmpty()
                }
                
                if (needsJapaneseNames) {
                    viewModelScope.launch {
                        isLoadingBusStops.value = true
                        loadingMessage = "Loading bus stops...".localized(getApplication())
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
                    LocalDataSource.values().firstOrNull { it.operatorCode() == operatorCode }?.let { dataSource ->
                        if (dataSource.apiType() == ODPTAPIType.GTFS) {
                            // Clear line bus stops first to avoid showing old data while loading
                            lineBusStops.value = emptyList()
                            
                            viewModelScope.launch {
                                isLoadingBusStops.value = true
                                loadingMessage = "Loading bus stops...".localized(getApplication())
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
            // For railway lines, show loading message while loading stations
            lineBusStops.value = emptyList()
            viewModelScope.launch {
                isLoadingBusStops.value = true
                loadingMessage = "Loading stations...".localized(getApplication())
                try {
                    // Small delay to show loading message (stations are loaded synchronously)
                    kotlinx.coroutines.delay(100)
                } finally {
                    isLoadingBusStops.value = false
                    loadingMessage = null
                }
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
        _lineSuggestions.value = emptyList()
        showLineSuggestions.value = false
        lineSelected.value = false
        _selectedLine.value = null
        
        // Reset station selection
        resetStationSelection()
        
        // Clear departure and arrival station input fields
        departureStopInput.value = ""
        departureSuggestions.value = emptyList()
        showDepartureSuggestions.value = false
        departureStopSelected.value = false
        _selectedDepartureStop.value = null
        
        arrivalStopInput.value = ""
        arrivalSuggestions.value = emptyList()
        showArrivalSuggestions.value = false
        arrivalStopSelected.value = false
        _selectedArrivalStop.value = null
        
        // Reset ride time to 0 minutes
        _selectedRideTime.value = 0
        
        // Reset line color to accent (not saved to SharedPreferences)
        selectedLineColor.value = AccentString
        
        // Reset transfer settings to none
        selectedTransportation.value = "none"
        selectedTransferTime.value = 0
        
        // Hide all dropdowns and selection UIs
        showColorSelection.value = false
        
        // Reset all focus states
        isOperatorFieldFocused.value = false
        isLineFieldFocused.value = false
        isDepartureFieldFocused.value = false
        isArrivalFieldFocused.value = false
    }
    
    // Reset station selection and clear all related state
    private fun resetStationSelection() {
        _selectedLine.value = null
        showStationSelection.value = false
        lineStations.value = emptyList()
        _selectedDepartureStop.value = null
        _selectedArrivalStop.value = null
        _selectedRideTime.value = 0
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
        operatorSuggestions.value = emptyList()
        showOperatorSuggestions.value = false
        operatorSelected.value = false
        selectedOperatorCode.value = null
        
        // Load data for the new kind from cache only (no fetch, no save)
        viewModelScope.launch {
            val kind = selectedTransportationKind.value.toTransportationKind()
            val loadedLines = mutableListOf<TransportationLine>()
            
            // Load data from cache for all operators of the selected kind
            for (transportOperator in LocalDataSource.values()) {
                if (transportOperator.transportationType() != kind) continue
                if (transportOperator.apiType() == ODPTAPIType.GTFS) continue
                
                val cacheKey = transportOperator.fileName()
                val cachedData = cache.loadData(cacheKey)
                
                if (cachedData != null) {
                    try {
                        val lines = if (kind == TransportationKind.RAILWAY) {
                            ODPTParser.parseRailwayRoutes(cachedData)
                        } else {
                            ODPTParser.parseBusRoutes(cachedData)
                        }
                        loadedLines.addAll(lines)
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsLineViewModel", "Failed to parse cached data: ${e.message}")
                    }
                }
            }
            
            all = loadedLines
            allData = all
            railwayLines.value = loadedLines.filter { it.kind == TransportationLineKind.RAILWAY }
            busLines.value = loadedLines.filter { it.kind == TransportationLineKind.BUS }
            
            // Re-filter existing data if line input exists
            if (lineInput.value.isNotEmpty() && lineInput.value.trim().isNotEmpty()) {
                filterLine(lineInput.value)
            }
            
            // Re-filter operator suggestions if operator input exists
            if (operatorInput.value.isNotEmpty() && operatorInput.value.trim().isNotEmpty()) {
                filterOperators(operatorInput.value)
            }
        }
    }
    
    // MARK: - GTFS Lines Fetching
    // Fetch GTFS lines from ZIP cache for selected operator
    suspend fun fetchGTFSLinesForOperator(dataSource: LocalDataSource) = withContext(Dispatchers.Main) {
        // Clear line suggestions immediately to prevent showing old data
        _lineSuggestions.value = emptyList()
        showLineSuggestions.value = false
        isLoadingLines.value = true
        loadingMessage = "Loading lines...".localized(getApplication())
        
        try {
            // TODO: Implement fetchGTFSData in GTFSDataService
            // For now, return empty list
            val gtfsLines = emptyList<TransportationLine>()
            
            // Update busLines with fetched GTFS lines
            busLines.value = busLines.value.filter { it.operatorCode != dataSource.operatorCode() } + gtfsLines
            
            // Also update all arrays to prevent duplicate loading
            all = all.filter { it.operatorCode != dataSource.operatorCode() } + gtfsLines
            allData = all
            
            // Update lineSuggestions with fetched lines
            val filteredLines = gtfsLines.filter { it.operatorCode == dataSource.operatorCode() }
            
            // Group by lineCode (route_short_name) to avoid duplicates
            val uniqueResults = mutableListOf<TransportationLine>()
            val seenLineCodes = mutableSetOf<String>()
            val seenDisplayNames = mutableSetOf<String>()
            
            for (line in filteredLines) {
                if (!line.lineCode.isNullOrEmpty()) {
                    if (!seenLineCodes.contains(line.lineCode)) {
                        seenLineCodes.add(line.lineCode!!)
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
            android.util.Log.e("SettingsLineViewModel", "Failed to fetch GTFS lines: ${e.message}")
        } finally {
            isLoadingLines.value = false
            loadingMessage = null
        }
    }
    
    // MARK: - GTFS Bus Stops Fetching
    // Fetch bus stops for a GTFS route from GTFS data
    private suspend fun fetchGTFSStopsForRoute(routeId: String, transportOperator: LocalDataSource) = withContext(Dispatchers.IO) {
        android.util.Log.d("SettingsLineViewModel", "🔍 fetchGTFSStopsForRoute: routeId=$routeId, transportOperator=${transportOperator.name}")
        try {
            // TODO: Implement fetchGTFSStopsForRoute in GTFSDataService
            // For now, return empty list
            val stops = emptyList<TransportationStop>()
            
            withContext(Dispatchers.Main) {
                // stops are already TransportationStop objects
                lineBusStops.value = stops
                // Update lineStops to reflect the fetched bus stops
                _lineStops.value = getStopsForSelectedLine()
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsLineViewModel", "❌ Failed to fetch GTFS stops for route $routeId: ${e.message}")
        }
    }
    
    // MARK: - BusstopPole API Integration
    // Fetches Japanese names for all bus stops in the selected route
    private suspend fun fetchJapaneseNamesForAllBusStops() = withContext(Dispatchers.IO) {
        val selectedLine = _selectedLine.value ?: return@withContext
        val operatorCode = selectedLine.operatorCode ?: return@withContext
        val dataSource = LocalDataSource.values().firstOrNull { it.operatorCode() == operatorCode } ?: return@withContext
        
        // TODO: Implement fetchJapaneseNamesForAllBusStops
        // This requires ODPT API integration for BusstopPole
    }
    
    // MARK: - Operator Line List Persistence
    // Save operator line list to SharedPreferences after operator selection
    override fun saveOperatorLineList(lines: List<TransportationLine>, goorback: String, num: Int) {
        val operatorLineListKey = goorback.operatorLineListKey(num)
        // TODO: Implement JSON serialization for TransportationLine
        // For now, save as empty set
        sharedPreferences.edit().putStringSet(operatorLineListKey, emptySet()).apply()
    }
    
    // Load operator line list from SharedPreferences
    fun loadOperatorLineList(goorback: String, num: Int): List<TransportationLine>? {
        val operatorLineListKey = goorback.operatorLineListKey(num)
        // TODO: Implement JSON deserialization for TransportationLine
        // For now, return null
        return null
    }
    
    // MARK: - Line Stop List Persistence
    // Save line stop list to SharedPreferences after line selection
    override fun saveLineStopList(stops: List<TransportationStop>, goorback: String, num: Int) {
        val lineStopListKey = goorback.lineStopListKey(num)
        // TODO: Implement JSON serialization for TransportationStop
        // For now, save as empty set
        sharedPreferences.edit().putStringSet(lineStopListKey, emptySet()).apply()
    }
    
    // Load line stop list from SharedPreferences
    fun loadLineStopList(goorback: String, num: Int): List<TransportationStop>? {
        val lineStopListKey = goorback.lineStopListKey(num)
        // TODO: Implement JSON deserialization for TransportationStop
        // For now, return null
        return null
    }
    
    // MARK: - Data Persistence
    // Save all information when save button is pressed
    override suspend fun saveAllDataToUserDefaults() = withContext(Dispatchers.IO) {
        val lineIndex = _selectedLineNumber.value - 1
        
        sharedPreferences.edit().apply {
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
                val codeToSave = if (!selectedLine.lineCode.isNullOrEmpty()) {
                    selectedLine.lineCode!!
                } else {
                    "" // Save empty string when odpt:lineCode is not available
                }
                putString(lineCodeKey, codeToSave)
            } ?: run {
                // Preserve existing lineCode if selectedLine is nil
                val existingLineCode = sharedPreferences.getString(lineCodeKey, null) ?: ""
                if (existingLineCode.isEmpty()) {
                    // Try to find lineCode from saved line name using reverse lookup
                    findSavedLineInData()?.let { foundLine ->
                        val codeToSave = if (!foundLine.lineCode.isNullOrEmpty()) {
                            foundLine.lineCode!!
                        } else {
                            ""
                        }
                        putString(lineCodeKey, codeToSave)
                    }
                }
            }
            
            // Save line color
            selectedLineColor.value?.let { lineColor ->
                if (lineColor.isNotEmpty()) {
                    val lineColorKey = _selectedGoorback.value.lineColorKey(lineIndex)
                    putString(lineColorKey, lineColor)
                }
            }
            
            // Save operator name (consistent with other fields like line name, station names)
            if (operatorInput.value.trim().isNotEmpty()) {
                val operatorNameKey = _selectedGoorback.value.operatorNameKey(lineIndex)
                putString(operatorNameKey, operatorInput.value)
            }
            
            // Save transportation kind
            val lineKindKey = _selectedGoorback.value.lineKindKey(lineIndex)
            putString(lineKindKey, selectedTransportationKind.value.name)
            
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
                    val departureLineCodeKey = "${_selectedGoorback.value.departStationCodeKey(lineIndex)}_lineCode"
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
                    val arrivalLineCodeKey = "${_selectedGoorback.value.arriveStationCodeKey(lineIndex)}_lineCode"
                    putString(arrivalLineCodeKey, stationLineCode)
                }
            }
            
            // Save ride time
            val rideTimeKey = _selectedGoorback.value.rideTimeKey(lineIndex)
            putInt(rideTimeKey, _selectedRideTime.value)
            
            // Save transfer settings and calculate transfer count
            val changeLineKey = _selectedGoorback.value.changeLineKey()
            val transportationKey = _selectedGoorback.value.transportationKey(lineIndex + 2)
            val currentChangeLine = sharedPreferences.getInt(changeLineKey, 0)
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
        }.apply()
        
        // Save operator line list to SharedPreferences
        if (_lineSuggestions.value.isNotEmpty()) {
            saveOperatorLineList(_lineSuggestions.value, _selectedGoorback.value, lineIndex)
        }
        
        // Save line stop list to SharedPreferences
        if (lineStops.isNotEmpty()) {
            saveLineStopList(lineStops, _selectedGoorback.value, lineIndex)
        }
        
        // Update display
        updateDisplay()
    }
    
    // Update all line information at once
    // Synchronize UI display with current model state
    private fun updateDisplay() {
        _selectedLine.value?.let { line ->
            lineInput.value = lineDisplayName(line)
        }
        
        _selectedLine.value?.lineColor?.let { lineColor ->
            selectedLineColor.value = lineColor
        }
        
        _selectedDepartureStop.value?.let { departureStop ->
            departureStopInput.value = departureStop.displayName(getApplication())
        }
        
        _selectedArrivalStop.value?.let { arrivalStop ->
            arrivalStopInput.value = arrivalStop.displayName(getApplication())
        }
    }
    
    // MARK: - Handle Line Save
    // Common save processing for all line types
    // Saves all current settings to persistent storage
    suspend fun handleLineSave() {
        saveAllDataToUserDefaults()
        updateDisplay()
        
        // Post notification to update MainContentView (same as SwiftUI's NotificationCenter.post)
        // Use SharedPreferences to trigger OnSharedPreferenceChangeListener
        sharedPreferences.edit()
            .putLong("SettingsLineUpdated", System.currentTimeMillis())
            .apply()
    }
    
    // MARK: - Reset Selections
    // Reset all selections when direction changes
    // Clear all user selections to start fresh
    private fun resetSelections() {
        operatorInput.value = ""
        operatorSuggestions.value = emptyList()
        showOperatorSuggestions.value = false
        operatorSelected.value = false
        showOperatorSelection.value = false
        selectedOperatorCode.value = null
        lineInput.value = ""
        _selectedLine.value = null
        _lineStops.value = emptyList()
        _selectedDepartureStop.value = null
        _selectedArrivalStop.value = null
        departureStopInput.value = ""
        arrivalStopInput.value = ""
        _selectedRideTime.value = 0
        selectedLineColor.value = AccentString
        selectedTransportationKind.value = TransportationLineKind.RAILWAY
        showColorSelection.value = false
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
    
    // MARK: - Data Update
    // Perform manual data update for both railway and bus operators
    // Refreshes all transportation data from ODPT API
    suspend fun performDataUpdate() = withContext(Dispatchers.Main) {
        isLoading.value = true
        
        // TODO: Implement SharedDataManager.performRailwayUpdate() and performBusUpdate()
        // For now, reload from cache
        loadFromCache()
        
        isLoading.value = false
        lastUpdatedDisplay.value = Date().toString()
    }
}

