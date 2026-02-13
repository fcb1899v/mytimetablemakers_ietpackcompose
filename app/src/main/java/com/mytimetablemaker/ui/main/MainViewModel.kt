package com.mytimetablemaker.ui.main

import android.app.Application
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.models.TransportationLineKind
import com.mytimetablemaker.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

// Manages transit state, timetables, and real-time updates.
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    // Timer for live date/time updates.
    private var timerJob: Job? = null
    
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("MainViewModel", Application.MODE_PRIVATE)
    
    // Date/time state used by the UI.
    var selectDate by mutableStateOf(Date())
        private set
    // Updated per second when running; null when stopped.
    var displayDate by mutableStateOf<Date?>(null)
        private set
    var dateLabel by mutableStateOf("")
        private set
    var timeLabel by mutableStateOf("")
        private set
    var isTimeStop by mutableStateOf(false)
        private set
    var isBack by mutableStateOf(true)
        private set
    
    // Route2 visibility flags for back and go.
    var isShowBackRoute2 by mutableStateOf(false)
        private set
    var isShowGoRoute2 by mutableStateOf(false)
        private set
    
    // Route2 visibility for the active direction.
    val isShowRoute2: Boolean
        get() = if (isBack) isShowBackRoute2 else isShowGoRoute2

    // Transfer count settings.
    var changeLine1 by mutableIntStateOf(0)
        private set
    var changeLine2 by mutableIntStateOf(0)
        private set
    
    // Route direction keys.
    var goOrBack1 by mutableStateOf("back1")
        private set
    var goOrBack2 by mutableStateOf("back2")
        private set
    
    // Route data arrays.
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
    
    // Initialize default state.
    init {
        isBack = true
        goOrBack1 = "back1"
        goOrBack2 = "back2"
        isTimeStop = false
        selectDate = Date()
        displayDate = null
        Handler(Looper.getMainLooper()).post { loadInitialDataFromPreferences() }
    }

    private fun loadInitialDataFromPreferences() {
        val appContext = getApplication<Application>()
        isShowBackRoute2 = "back2".isShowRoute2(sharedPreferences)
        isShowGoRoute2 = "go2".isShowRoute2(sharedPreferences)
        changeLine1 = "back1".changeLineInt(sharedPreferences)
        changeLine2 = "back2".changeLineInt(sharedPreferences)
        dateLabel = selectDate.formatDate(getApplication())
        timeLabel = selectDate.setTime
        home = "back1".departurePoint(sharedPreferences, appContext)
        office = "back1".destination(sharedPreferences, appContext)
        stationArray1 = "back1".stationArray(sharedPreferences, appContext)
        stationArray2 = "back2".stationArray(sharedPreferences, appContext)
        lineNameArray1 = "back1".lineNameArray(sharedPreferences, appContext)
        lineNameArray2 = "back2".lineNameArray(sharedPreferences, appContext)
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
    
    // Update route direction identifiers.
    fun setGoOrBack() {
        goOrBack1 = isBack.goOrBack1()
        goOrBack2 = isBack.goOrBack2()
    }
    
    // Refresh route2 visibility from SharedPreferences.
    fun setRoute2() {
        isShowBackRoute2 = "back2".isShowRoute2(sharedPreferences)
        isShowGoRoute2 = "go2".isShowRoute2(sharedPreferences)
    }
    
    // Set route2 visibility for both directions.
    fun setRoute2Value(value: Boolean) {
        isShowBackRoute2 = value
        isShowGoRoute2 = value
    }
    
    // Refresh line data for the active direction.
    fun setLineData() {
        val appContext = getApplication<Application>()
        home = goOrBack1.departurePoint(sharedPreferences, appContext)
        office = goOrBack1.destination(sharedPreferences, appContext)
        changeLine1 = goOrBack1.changeLineInt(sharedPreferences)
        changeLine2 = goOrBack2.changeLineInt(sharedPreferences)
        stationArray1 = goOrBack1.stationArray(sharedPreferences, appContext)
        stationArray2 = goOrBack2.stationArray(sharedPreferences, appContext)
        lineNameArray1 = goOrBack1.lineNameArray(sharedPreferences, appContext)
        lineNameArray2 = goOrBack2.lineNameArray(sharedPreferences, appContext)
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
    
    // Refresh all cached values from SharedPreferences.
    fun updateAllDataFromUserDefaults() {
        setGoOrBack()
        setRoute2()
        setLineData()
        setTransferData()
    }
    
    // Update date shown in UI.
    fun updateDate(date: Date) {
        selectDate = date
        displayDate = null
        dateLabel = date.formatDate(getApplication())
    }
    
    // Update time label from the given date.
    fun updateTime(date: Date) {
        timeLabel = date.setTime
    }
    
    // Switch to back direction.
    fun backButton() {
        isBack = true
        updateAllDataFromUserDefaults()
    }
    
    // Switch to go direction.
    fun goButton() {
        isBack = false
        updateAllDataFromUserDefaults()
    }
    
    // Start live time updates.
    fun startButton() {
        timerJob?.cancel()
        isTimeStop = false
        selectDate = Date()
        
        timerJob = viewModelScope.launch {
            while (true) {
                val now = Date()
                displayDate = now
                dateLabel = now.formatDate(getApplication())
                timeLabel = now.setTime
                delay(1000)
            }
        }
    }
    
    // Stop live time updates.
    fun stopButton() {
        isTimeStop = true
        displayDate = null
        timerJob?.cancel()
        timerJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopButton()
    }
    
    // Current time derived from the label.
    val currentTime: Int
        get() = timeLabel.currentTime
    
    // Time arrays based on line calendars.
    val timeArray1: List<Int>
        get() = goOrBack1.timeArray(sharedPreferences, selectDate, currentTime)
    
    val timeArray2: List<Int>
        get() = goOrBack2.timeArray(sharedPreferences, selectDate, currentTime)
    
    val timeArrayString1: List<String>
        get() = timeArray1.map { it.stringTime }
    
    val timeArrayString2: List<String>
        get() = timeArray2.map { it.stringTime }
    
    // Countdown values for the next trains.
    val countdownTime1: String
        get() = if (timeArray1.size > 1) currentTime.countdownTime(timeArray1[1]) else ""
    
    val countdownTime2: String
        get() = if (timeArray2.size > 1) currentTime.countdownTime(timeArray2[1]) else ""
    
    val countdownColor1: Color
        get() = if (timeArray1.size > 1) currentTime.countdownColor(timeArray1[1]) else Gray
    
    val countdownColor2: Color
        get() = if (timeArray2.size > 1) currentTime.countdownColor(timeArray2[1]) else Gray
}

