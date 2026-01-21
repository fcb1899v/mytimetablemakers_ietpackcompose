package com.mytimetablemaker.ui.main

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.models.ODPTCalendarType
import com.mytimetablemaker.models.TransportationLineKind
import com.mytimetablemaker.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

// MARK: - Transit Data Model
// Manages transit information, timetables, and real-time updates
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    // Timer for real-time updates
    private var timerJob: Job? = null
    
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("MainViewModel", Application.MODE_PRIVATE)
    
    // Date and time state
    var selectDate by mutableStateOf(Date())
        private set
    var dateLabel by mutableStateOf("")
        private set
    var timeLabel by mutableStateOf("")
        private set
    var isTimeStop by mutableStateOf(false)
        private set
    var isBack by mutableStateOf(true)
        private set
    
    // Route visibility settings with SharedPreferences persistence
    // Match SwiftUI: separate flags for back and go routes
    var isShowBackRoute2 by mutableStateOf(false)
        private set
    var isShowGoRoute2 by mutableStateOf(false)
        private set
    
    // Computed property: returns the appropriate route2 flag based on isBack
    val isShowRoute2: Boolean
        get() = if (isBack) isShowBackRoute2 else isShowGoRoute2

    // Line change settings with SharedPreferences persistence
    var changeLine1 by mutableIntStateOf(0)
        private set
    var changeLine2 by mutableIntStateOf(0)
        private set
    
    // Route direction identifiers with SharedPreferences persistence
    var goOrBack1 by mutableStateOf("back1")
        private set
    var goOrBack2 by mutableStateOf("back2")
        private set
    
    // Line data arrays for real-time updates
    var home by mutableStateOf("")
        private set
    var office by mutableStateOf("")
        private set
    var stationArray1 by mutableStateOf<List<String>>(emptyList())
        private set
    var stationArray2 by mutableStateOf<List<String>>(emptyList())
        private set
    var lineNameArray1 by mutableStateOf<List<String>>(emptyList())
        private set
    var lineNameArray2 by mutableStateOf<List<String>>(emptyList())
        private set
    var lineColorArray1 by mutableStateOf<List<Color>>(emptyList())
        private set
    var lineColorArray2 by mutableStateOf<List<Color>>(emptyList())
        private set
    var transportationArray1 by mutableStateOf<List<String>>(emptyList())
        private set
    var transportationArray2 by mutableStateOf<List<String>>(emptyList())
        private set
    var transferTimeArray1 by mutableStateOf<List<Int>>(emptyList())
        private set
    var transferTimeArray2 by mutableStateOf<List<Int>>(emptyList())
        private set
    var rideTimeArray1 by mutableStateOf<List<Int>>(emptyList())
        private set
    var rideTimeArray2 by mutableStateOf<List<Int>>(emptyList())
        private set
    var lineCodeArray1 by mutableStateOf<List<String>>(emptyList())
        private set
    var lineCodeArray2 by mutableStateOf<List<String>>(emptyList())
        private set
    var lineKindArray1 by mutableStateOf<List<TransportationLineKind?>>(emptyList())
        private set
    var lineKindArray2 by mutableStateOf<List<TransportationLineKind?>>(emptyList())
        private set
    
    // MARK: - Initialization
    init {
        isBack = true
        isShowBackRoute2 = "back2".isShowRoute2(sharedPreferences)
        isShowGoRoute2 = "go2".isShowRoute2(sharedPreferences)
        changeLine1 = "back1".changeLineInt(sharedPreferences)
        changeLine2 = "back2".changeLineInt(sharedPreferences)
        goOrBack1 = "back1"
        goOrBack2 = "back2"
        isTimeStop = false
        selectDate = Date()
        dateLabel = selectDate.setDate
        timeLabel = selectDate.setTime
        home = "back1".departurePoint(sharedPreferences)
        office = "back1".destination(sharedPreferences)
        stationArray1 = "back1".stationArray(sharedPreferences)
        stationArray2 = "back2".stationArray(sharedPreferences)
        lineNameArray1 = "back1".lineNameArray(sharedPreferences)
        lineNameArray2 = "back2".lineNameArray(sharedPreferences)
        lineColorArray1 = "back1".lineColorArray(sharedPreferences)
        lineColorArray2 = "back2".lineColorArray(sharedPreferences)
        transportationArray1 = "back1".transportationArray(sharedPreferences)
        transportationArray2 = "back2".transportationArray(sharedPreferences)
        transferTimeArray1 = "back1".transferTimeArray(sharedPreferences)
        transferTimeArray2 = "back2".transferTimeArray(sharedPreferences)
        rideTimeArray1 = "back1".rideTimeArray(sharedPreferences)
        rideTimeArray2 = "back2".rideTimeArray(sharedPreferences)
        lineCodeArray1 = "back1".lineCodeArray(sharedPreferences)
        lineCodeArray2 = "back2".lineCodeArray(sharedPreferences)
        lineKindArray1 = "back1".lineKindArray(sharedPreferences).map { it as TransportationLineKind? }
        lineKindArray2 = "back2".lineKindArray(sharedPreferences).map { it as TransportationLineKind? }
    }
    
    // MARK: - Route Management
    // Updates route direction identifiers
    fun setGoOrBack() {
        goOrBack1 = isBack.goOrBack1()
        goOrBack2 = isBack.goOrBack2()
    }
    
    // Updates route visibility settings from SharedPreferences
    // Match SwiftUI: update both back and go route flags
    fun setRoute2() {
        isShowBackRoute2 = "back2".isShowRoute2(sharedPreferences)
        isShowGoRoute2 = "go2".isShowRoute2(sharedPreferences)
    }
    
    // Set route 2 visibility for both back and go routes
    // Used when updating from settings screens
    fun setRoute2Value(value: Boolean) {
        isShowBackRoute2 = value
        isShowGoRoute2 = value
    }
    
    // Updates line change settings based on current direction
    fun setLineData() {
        home = goOrBack1.departurePoint(sharedPreferences)
        office = goOrBack1.destination(sharedPreferences)
        changeLine1 = goOrBack1.changeLineInt(sharedPreferences)
        changeLine2 = goOrBack2.changeLineInt(sharedPreferences)
        stationArray1 = goOrBack1.stationArray(sharedPreferences)
        stationArray2 = goOrBack2.stationArray(sharedPreferences)
        lineNameArray1 = goOrBack1.lineNameArray(sharedPreferences)
        lineNameArray2 = goOrBack2.lineNameArray(sharedPreferences)
        lineColorArray1 = goOrBack1.lineColorArray(sharedPreferences)
        lineColorArray2 = goOrBack2.lineColorArray(sharedPreferences)
        rideTimeArray1 = goOrBack1.rideTimeArray(sharedPreferences)
        rideTimeArray2 = goOrBack2.rideTimeArray(sharedPreferences)
        lineCodeArray1 = goOrBack1.lineCodeArray(sharedPreferences)
        lineCodeArray2 = goOrBack2.lineCodeArray(sharedPreferences)
        lineKindArray1 = goOrBack1.lineKindArray(sharedPreferences).map { it as TransportationLineKind? }
        lineKindArray2 = goOrBack2.lineKindArray(sharedPreferences).map { it as TransportationLineKind? }
    }
    
    fun setTransferData() {
        transportationArray1 = goOrBack1.transportationArray(sharedPreferences)
        transportationArray2 = goOrBack2.transportationArray(sharedPreferences)
        transferTimeArray1 = goOrBack1.transferTimeArray(sharedPreferences)
        transferTimeArray2 = goOrBack2.transferTimeArray(sharedPreferences)
    }
    
    // MARK: - SharedPreferences Persistence
    // Save route visibility settings to SharedPreferences
    // Match SwiftUI: save both back and go route flags
    fun saveRoute2Settings() {
        sharedPreferences.edit {
            putBoolean("back2".isShowRoute2Key(), isShowBackRoute2)
            putBoolean("go2".isShowRoute2Key(), isShowGoRoute2)
        }
    }
    
    // Save line change settings to SharedPreferences
    fun saveChangeLineSettings() {
        sharedPreferences.edit {
            putInt(goOrBack1.changeLineKey(), changeLine1)
            putInt(goOrBack2.changeLineKey(), changeLine2)
        }
    }
    
    // MARK: - SharedPreferences Data Update
    // Updates all data from SharedPreferences when changes are detected
    fun updateAllDataFromUserDefaults() {
        // Update route direction identifiers
        setGoOrBack()
        // Update route 2 visibility settings
        setRoute2()
        // Update line settings
        setLineData()
        // Update transfer settings
        setTransferData()
    }
    
    // MARK: - Direction Control
    // Switches to return direction and updates line settings
    fun updateDate(date: Date) {
        selectDate = date
        dateLabel = date.setDate
    }
    
    fun updateTime(date: Date) {
        timeLabel = date.setTime
    }
    
    fun backButton() {
        isBack = true
        updateAllDataFromUserDefaults()
    }
    
    // Switches to outbound direction and updates line settings
    fun goButton() {
        isBack = false
        updateAllDataFromUserDefaults()
    }
    
    // MARK: - Timer Control
    // Starts real-time timer for updating date and time
    fun startButton() {
        // Cancel existing timer before starting new one
        timerJob?.cancel()
        isTimeStop = false
        selectDate = Date()
        
        timerJob = viewModelScope.launch {
            while (true) {
                val now = Date()
                dateLabel = now.setDate
                timeLabel = now.setTime
                delay(1000)
            }
        }
    }
    
    // Stops real-time timer
    fun stopButton() {
        isTimeStop = true
        timerJob?.cancel()
        timerJob = null
    }
    
    // MARK: - Timer State Management
    // Ensures timer is running when view appears
    fun ensureTimerRunning() {
        // Always start timer when view appears, regardless of current state
        startButton()
    }
    
    // Stops timer when view disappears
    fun stopTimerOnDisappear() {
        // Always stop timer when view disappears
        stopButton()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopButton()
    }
    
    // MARK: - Computed Properties
    // Current date and time information
    val currentTime: Int
        get() = timeLabel.currentTime
    
    // Calendar type based on current date for each line
    // Each line may have different available calendar types
    private fun currentCalendarType(route: String, lineNumber: Int): ODPTCalendarType {
        // Use line-level cache key (structure: goorback -> line -> calendar types)
        val lineCacheKey = "${route}line${lineNumber}_calendarTypes"
        var availableTypes = mutableListOf<String>()
        
        // Try to get cached calendar types for this specific line
        val cachedTypes = sharedPreferences.getStringSet(lineCacheKey, null)
        if (cachedTypes != null && cachedTypes.isNotEmpty()) {
            availableTypes = cachedTypes.toMutableList()
        }
        
        // If no line-specific cache, try to detect from actual data
        if (availableTypes.isEmpty()) {
            availableTypes = route.loadAvailableCalendarTypes(sharedPreferences, lineNumber - 1).toMutableList()
        }
        
        // Fallback to default types if no cache or data found
        if (availableTypes.isEmpty()) {
            availableTypes = mutableListOf("weekday", "holiday", "saturdayHoliday")
        }
        
        // Determine calendar type from current date
        val calendarTypeString = selectDate.odptCalendarType(availableTypes)
        // Convert string to ODPTCalendarType
        return when (calendarTypeString.lowercase()) {
            "weekday" -> ODPTCalendarType.Weekday
            "holiday" -> ODPTCalendarType.Holiday
            "saturdayholiday", "saturday_holiday" -> ODPTCalendarType.SaturdayHoliday
            "sunday" -> ODPTCalendarType.Sunday
            "monday" -> ODPTCalendarType.Monday
            "tuesday" -> ODPTCalendarType.Tuesday
            "wednesday" -> ODPTCalendarType.Wednesday
            "thursday" -> ODPTCalendarType.Thursday
            "friday" -> ODPTCalendarType.Friday
            "saturday" -> ODPTCalendarType.Saturday
            else -> ODPTCalendarType.Weekday
        }
    }
    
    // Calendar type for route 1, line 1 (first line in route 1)
    val currentCalendarType1: ODPTCalendarType
        get() = currentCalendarType(goOrBack1, 1)
    
    // Calendar type for route 2, line 1 (first line in route 2)
    val currentCalendarType2: ODPTCalendarType
        get() = currentCalendarType(goOrBack2, 1)
    
    // Timetable data for both routes using line-specific calendar types
    // Note: timetableArray returns data for all lines (0-2) in the route
    // For simplicity, we use the calendar type of the first line for all lines in the route
    // If needed, this could be made more sophisticated to use different types per line
    val timetableArray1: List<List<Int>>
        get() = goOrBack1.timetableArray(sharedPreferences, selectDate)
    
    val timetableArray2: List<List<Int>>
        get() = goOrBack2.timetableArray(sharedPreferences, selectDate)
    
    // Current time-based schedule information using line-specific calendar types
    val timeArray1: List<Int>
        get() = goOrBack1.timeArray(sharedPreferences, selectDate, currentTime)
    
    val timeArray2: List<Int>
        get() = goOrBack2.timeArray(sharedPreferences, selectDate, currentTime)
    
    val timeArrayString1: List<String>
        get() = timeArray1.map { it.stringTime }
    
    val timeArrayString2: List<String>
        get() = timeArray2.map { it.stringTime }
    
    // Countdown information for next trains
    val countdownTime1: String
        get() = if (timeArray1.size > 1) currentTime.countdownTime(timeArray1[1]) else ""
    
    val countdownTime2: String
        get() = if (timeArray2.size > 1) currentTime.countdownTime(timeArray2[1]) else ""
    
    val countdownColor1: Color
        get() = if (timeArray1.size > 1) currentTime.countdownColor(timeArray1[1]) else Gray
    
    val countdownColor2: Color
        get() = if (timeArray2.size > 1) currentTime.countdownColor(timeArray2[1]) else Gray
}

// MARK: - Boolean Extensions
// Extensions for boolean values to provide route and weekday information

