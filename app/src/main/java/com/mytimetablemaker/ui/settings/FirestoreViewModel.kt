package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.models.ODPTCalendarType
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// MARK: - App Constants
// Array of route direction identifiers
val goorbackarray = listOf("back1", "go1", "back2", "go2")

// MARK: - Firestore Data Manager
// Handles data synchronization between SharedPreferences and Firestore database
class FirestoreViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("FirestoreViewModel", Application.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // MARK: - Published Properties
    // Alert messages, visibility states, and loading indicators for UI updates
    var title by mutableStateOf("")
        private set
    var message by mutableStateOf("")
        private set
    var isShowAlert by mutableStateOf(false)
        private set
    var isShowMessage by mutableStateOf(false)
        private set
    var isFirestoreSuccess by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    
    // MARK: - Firestore Reference Helper
    // Creates Firestore document reference for current user and route
    private fun getRef(goorback: String): com.google.firebase.firestore.DocumentReference {
        val userId = auth.currentUser?.uid ?: ""
        val userDb = firestore.collection("users").document(userId)
        return userDb.collection("goorback").document(goorback)
    }
    
    // MARK: - Data Upload
    // Uploads all SharedPreferences data to Firestore server
    fun setFirestore() {
        viewModelScope.launch {
            isLoading = true
            isShowAlert = false
            isShowMessage = false
            isFirestoreSuccess = false
            title = getApplication<Application>().getString(R.string.saveDataError)
            message = getApplication<Application>().getString(R.string.dataCouldNotBeSaved)
            
            try {
                // Process each route (go/back) and upload timetable and line information
                goorbackarray.forEach { goorback ->
                    (0..2).forEach { linenumber ->
                        // Get available calendar types for this route and line
                        val availableCalendarTypes = getAvailableCalendarTypesForRoute(goorback, linenumber)
                        setTimetableFirestore(goorback, linenumber, availableCalendarTypes)
                    }
                    setLineInfoFirestore(goorback)
                }
            } catch (e: Exception) {
                println("❌ Error in setFirestore: ${e.message}")
                isLoading = false
                isShowMessage = true
            }
        }
    }
    
    // MARK: - Available Calendar Types Detection
    // Get available calendar types for specific route and line
    private fun getAvailableCalendarTypesForRoute(goorback: String, num: Int): List<ODPTCalendarType> {
        val availableTypes = mutableListOf<ODPTCalendarType>()
        
        // Check which calendar types have data in SharedPreferences
        for (calendarType in ODPTCalendarType.allCases) {
            var hasData = false
            for (hour in 4..25) {
                val timetableKey = goorback.timetableKey(calendarType.calendarTag(), num, hour)
                if (sharedPreferences.getString(timetableKey, null) != null) {
                    hasData = true
                    break
                }
            }
            if (hasData) {
                availableTypes.add(calendarType)
            }
        }
        
        // Fallback to basic types if no data found
        if (availableTypes.isEmpty()) {
            availableTypes.add(ODPTCalendarType.Weekday)
            availableTypes.add(ODPTCalendarType.SaturdayHoliday)
        }
        
        println("📅 Available calendar types for $goorback line $num: ${availableTypes.map { it.debugDisplayName() }}")
        return availableTypes
    }
    
    // MARK: - Timetable Upload
    // Uploads timetable data for specific route, line, and available calendar types to Firestore
    private suspend fun setTimetableFirestore(goorback: String, num: Int, availableCalendarTypes: List<ODPTCalendarType>) {
        for (calendarType in availableCalendarTypes) {
            val documentName = "timetable${num + 1}${calendarType.calendarTag()}"
            val nextRef = getRef(goorback).collection("timetable").document(documentName)
            
            // Create comprehensive hour data with backward compatibility
            val hourData = mutableMapOf<String, Any>()
            
            for (hour in 4..25) {
                val hourKey = "hour${String.format("%02d", hour)}"
                val calendarTag = calendarType.calendarTag()
                val timetableKey = goorback.timetableKey(calendarTag, num, hour)
                val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarTag, num, hour)
                val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarTag, num, hour)
                
                // Get data from SharedPreferences
                val timetableData = sharedPreferences.getString(timetableKey, "") ?: ""
                val rideTimeData = sharedPreferences.getString(timetableRideTimeKey, "") ?: ""
                val trainTypeData = sharedPreferences.getString(timetableTrainTypeKey, "") ?: ""
                
                // Save in both formats for backward compatibility
                // Legacy format (string) - for existing clients
                hourData[hourKey] = timetableData
                
                // New format (dictionary) - for enhanced data
                hourData["${hourKey}_enhanced"] = mapOf(
                    "timetable" to timetableData,
                    "rideTime" to rideTimeData,
                    "trainType" to trainTypeData
                )
            }
            
            try {
                nextRef.set(hourData).await()
                println("✅ Uploaded timetable data to Firestore for ${calendarType.debugDisplayName()}")
            } catch (e: Exception) {
                println("❌ Error uploading timetable data: ${e.message}")
            }
        }
    }
    
    // MARK: - Line Information Upload
    // Uploads line information (stations, colors, times) to Firestore
    private suspend fun setLineInfoFirestore(goorback: String) {
        val batch = firestore.batch()
        val ref = getRef(goorback)
        
        val lineData = mapOf(
            "switch" to goorback.isShowRoute2(sharedPreferences),
            "changeline" to goorback.changeLineInt(sharedPreferences).toString(),
            "departpoint" to goorback.departurePoint(sharedPreferences),
            "arrivalpoint" to goorback.destination(sharedPreferences),
            "operatorname1" to goorback.operatorNameArray(sharedPreferences)[0],
            "operatorname2" to goorback.operatorNameArray(sharedPreferences)[1],
            "operatorname3" to goorback.operatorNameArray(sharedPreferences)[2],
            "linename1" to goorback.lineNameArray(sharedPreferences)[0],
            "linename2" to goorback.lineNameArray(sharedPreferences)[1],
            "linename3" to goorback.lineNameArray(sharedPreferences)[2],
            "departstation1" to goorback.departStationArray(sharedPreferences)[0],
            "departstation2" to goorback.departStationArray(sharedPreferences)[1],
            "departstation3" to goorback.departStationArray(sharedPreferences)[2],
            "arrivalstation1" to goorback.arriveStationArray(sharedPreferences)[0],
            "arrivalstation2" to goorback.arriveStationArray(sharedPreferences)[1],
            "arrivalstation3" to goorback.arriveStationArray(sharedPreferences)[2],
            "linecolor1" to goorback.lineColorStringArray(sharedPreferences)[0],
            "linecolor2" to goorback.lineColorStringArray(sharedPreferences)[1],
            "linecolor3" to goorback.lineColorStringArray(sharedPreferences)[2],
            "linecode1" to goorback.lineCodeArray(sharedPreferences)[0],
            "linecode2" to goorback.lineCodeArray(sharedPreferences)[1],
            "linecode3" to goorback.lineCodeArray(sharedPreferences)[2],
            "linekind1" to goorback.lineKindArray(sharedPreferences)[0],
            "linekind2" to goorback.lineKindArray(sharedPreferences)[1],
            "linekind3" to goorback.lineKindArray(sharedPreferences)[2],
            "ridetime1" to goorback.rideTimeArray(sharedPreferences)[0].toString(),
            "ridetime2" to goorback.rideTimeArray(sharedPreferences)[1].toString(),
            "ridetime3" to goorback.rideTimeArray(sharedPreferences)[2].toString(),
            "transportation1" to goorback.transportationArray(sharedPreferences)[1],
            "transportation2" to goorback.transportationArray(sharedPreferences)[2],
            "transportation3" to goorback.transportationArray(sharedPreferences)[3],
            "transportatione" to goorback.transportationArray(sharedPreferences)[0],
            "transittime1" to goorback.transferTimeArray(sharedPreferences)[1].toString(),
            "transittime2" to goorback.transferTimeArray(sharedPreferences)[2].toString(),
            "transittime3" to goorback.transferTimeArray(sharedPreferences)[3].toString(),
            "transittimee" to goorback.transferTimeArray(sharedPreferences)[0].toString()
        )
        
        batch.set(ref, lineData)
        
        // Commit batch operation and handle completion/error callbacks
        try {
            batch.commit().await()
            if (goorback == "go2") {
                title = getApplication<Application>().getString(R.string.dataSavedSuccessfully)
                message = ""
                isFirestoreSuccess = true
                isLoading = false
                isShowMessage = true
            }
        } catch (e: Exception) {
            println("❌ Error uploading line info: ${e.message}")
            if (goorback == "go2") {
                isLoading = false
                isShowMessage = true
            }
        }
    }
    
    // MARK: - Data Download
    // Downloads all data from Firestore server to SharedPreferences
    fun getFirestore() {
        viewModelScope.launch {
            isLoading = true
            isShowAlert = false
            isShowMessage = false
            isFirestoreSuccess = false
            title = getApplication<Application>().getString(R.string.getDataError)
            message = getApplication<Application>().getString(R.string.dataCouldNotBeGot)
            
            try {
                // Process each route (go/back) and download timetable and line information
                goorbackarray.forEach { goorback ->
                    (0..2).forEach { linenumber ->
                        // Get available calendar types for this route and line
                        val availableCalendarTypes = getAvailableCalendarTypesForRoute(goorback, linenumber)
                        availableCalendarTypes.forEach { calendarType ->
                            getTimetableFirestore(goorback, linenumber, calendarType)
                        }
                    }
                    getLineInfoFirestore(goorback)
                }
            } catch (e: Exception) {
                println("❌ Error in getFirestore: ${e.message}")
                isLoading = false
                isShowMessage = true
            }
        }
    }
    
    // MARK: - Line Information Download
    // Downloads line information from Firestore and saves to SharedPreferences
    private suspend fun getLineInfoFirestore(goorback: String) {
        try {
            val document = getRef(goorback).get().await()
            if (document.exists() && document.data != null) {
                val data = document.data!!
                
                sharedPreferences.edit().apply {
                    data["switch"]?.let { putBoolean(goorback.isShowRoute2Key(), it as? Boolean ?: false) }
                    data["changeline"]?.let { putString(goorback.changeLineKey(), it.toString()) }
                    data["departpoint"]?.let { putString(goorback.departurePointKey(), it.toString()) }
                    data["arrivalpoint"]?.let { putString(goorback.destinationKey(), it.toString()) }
                    
                    for (num in 0..2) {
                        data["operatorname${num + 1}"]?.let { putString(goorback.operatorNameKey(num), it.toString()) }
                        data["departstation${num + 1}"]?.let { putString(goorback.departStationKey(num), it.toString()) }
                        data["arrivalstation${num + 1}"]?.let { putString(goorback.arriveStationKey(num), it.toString()) }
                        data["linename${num + 1}"]?.let { putString(goorback.lineNameKey(num), it.toString()) }
                        data["linecolor${num + 1}"]?.let { putString(goorback.lineColorKey(num), it.toString()) }
                        data["linecode${num + 1}"]?.let { putString(goorback.lineCodeKey(num), it.toString()) }
                        data["linekind${num + 1}"]?.let { putString(goorback.lineKindKey(num), it.toString()) }
                        data["ridetime${num + 1}"]?.let { putString(goorback.rideTimeKey(num), it.toString()) }
                        data["transportation${num + 1}"]?.let { putString(goorback.transportationKey(num + 1), it.toString()) }
                        data["transittime${num + 1}"]?.let { putString(goorback.transferTimeKey(num + 1), it.toString()) }
                    }
                    
                    data["transportatione"]?.let { putString(goorback.transportationKey(0), it.toString()) }
                    data["transittimee"]?.let { putString(goorback.transferTimeKey(0), it.toString()) }
                    apply()
                }
                
                if (goorback == "go2") {
                    title = getApplication<Application>().getString(R.string.dataGotSuccessfully)
                    message = ""
                    isFirestoreSuccess = true
                    isLoading = false
                    isShowMessage = true
                }
            } else {
                if (goorback == "go2") {
                    isLoading = false
                    isShowMessage = true
                }
            }
        } catch (e: Exception) {
            println("❌ Error downloading line info: ${e.message}")
            if (goorback == "go2") {
                isLoading = false
                isShowMessage = true
            }
        }
    }
    
    // MARK: - Timetable Download
    // Downloads timetable data for specific route, line, and calendar type from Firestore
    private suspend fun getTimetableFirestore(goorback: String, num: Int, calendarType: ODPTCalendarType) {
        val documentName = "timetable${num + 1}${calendarType.calendarTag()}"
        val nextRef = getRef(goorback).collection("timetable").document(documentName)
        
        try {
            val document = nextRef.get().await()
            if (document.exists() && document.data != null) {
                val data = document.data!!
                
                // Set data from Firestore for all hours with backward compatibility
                for (hour in 4..25) {
                    val hourKey = "hour${String.format("%02d", hour)}"
                    val enhancedHourKey = "${hourKey}_enhanced"
                    val calendarTag = calendarType.calendarTag()
                    val timetableKey = goorback.timetableKey(calendarTag, num, hour)
                    val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarTag, num, hour)
                    val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarTag, num, hour)
                    
                    // Try to load enhanced format first (new format)
                    val enhancedData = data[enhancedHourKey] as? Map<*, *>
                    if (enhancedData != null) {
                        // New format with structured data
                        sharedPreferences.edit().apply {
                            putString(timetableKey, enhancedData["timetable"]?.toString() ?: "")
                            putString(timetableRideTimeKey, enhancedData["rideTime"]?.toString() ?: "")
                            putString(timetableTrainTypeKey, enhancedData["trainType"]?.toString() ?: "")
                            apply()
                        }
                    } else {
                        // Legacy format - fallback to old string-only format for backward compatibility
                        val hourDataString = data[hourKey] as? String
                        if (hourDataString != null) {
                            sharedPreferences.edit().apply {
                                putString(timetableKey, hourDataString)
                                // Clear ride time and train type for legacy data
                                putString(timetableRideTimeKey, "")
                                putString(timetableTrainTypeKey, "")
                                apply()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Error getting document for ${calendarType.debugDisplayName()}: ${e.message}")
        }
    }
}

// MARK: - ODPTCalendarType Extensions
// Extensions for calendar type utilities
fun ODPTCalendarType.calendarTag(): String {
    // For .specific types, extract identifier from rawValue for unique key
    if (this is ODPTCalendarType.Specific) {
        val components = this.rawValue.split(".")
        val lastComponent = components.lastOrNull() ?: return "weekday"
        return lastComponent.lowercase()
    }
    
    // For standard types, use display type tag
    val displayType = this.displayCalendarType()
    return when (displayType) {
        is ODPTCalendarType.SaturdayHoliday -> "weekend"
        else -> displayType.debugDisplayName().lowercase()
    }
}

fun ODPTCalendarType.debugDisplayName(): String {
    val displayType = this.displayCalendarType()
    return when (displayType) {
        is ODPTCalendarType.Weekday -> "Weekday"
        is ODPTCalendarType.Holiday -> "Holiday"
        is ODPTCalendarType.SaturdayHoliday -> "Saturday/Holiday"
        is ODPTCalendarType.Sunday -> "Sunday"
        is ODPTCalendarType.Monday -> "Monday"
        is ODPTCalendarType.Tuesday -> "Tuesday"
        is ODPTCalendarType.Wednesday -> "Wednesday"
        is ODPTCalendarType.Thursday -> "Thursday"
        is ODPTCalendarType.Friday -> "Friday"
        is ODPTCalendarType.Saturday -> "Saturday"
        is ODPTCalendarType.Specific -> "Specific"
    }
}

fun ODPTCalendarType.displayCalendarType(): ODPTCalendarType {
    // Convert .specific calendar types to standard types for display
    if (this is ODPTCalendarType.Specific) {
        val components = this.rawValue.split(".")
        val lastComponent = components.lastOrNull()
        
        if (lastComponent != null) {
            // Check if last component is a day type name
            when (lastComponent) {
                "Weekday" -> return ODPTCalendarType.Weekday
                "Saturday" -> return ODPTCalendarType.Saturday
                "Holiday" -> return ODPTCalendarType.Holiday
                else -> {
                    // Handle identifier patterns (e.g., "odpt.Calendar:Specific.Toei.81-170" or "21_7")
                    val partsByDash = lastComponent.split("-")
                    val partsByUnderscore = lastComponent.split("_")
                    val lastPart = if (partsByDash.size > 1) {
                        partsByDash.lastOrNull() ?: ""
                    } else if (partsByUnderscore.size > 1) {
                        partsByUnderscore.lastOrNull() ?: ""
                    } else {
                        lastComponent
                    }
                    
                    return when (lastPart) {
                        "100", "109" -> ODPTCalendarType.Holiday
                        "160" -> ODPTCalendarType.Saturday
                        "170", "179" -> ODPTCalendarType.Weekday
                        else -> ODPTCalendarType.Weekday  // Fallback to weekday
                    }
                }
            }
        }
        return ODPTCalendarType.Weekday  // Default fallback
    }
    return this
}

