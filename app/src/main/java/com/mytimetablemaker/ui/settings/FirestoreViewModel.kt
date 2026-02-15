package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.models.ODPTCalendarType
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

// Route direction keys for Firestore sync.
val goorbackarray = listOf("back1", "go1", "back2", "go2")

// Syncs SharedPreferences data with Firestore.
class FirestoreViewModel(application: Application) : AndroidViewModel(application) {
    
    // SharedPreferences used by the main screen.
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("MainViewModel", Application.MODE_PRIVATE)
    // Lazy init to avoid Firebase access at startup.
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // UI state for alerts and loading.
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

    // Loading message for progress indicator
    var loadingMessage by mutableStateOf("")
        private set
    
    // Dismiss Firestore result alert.
    fun dismissMessage() {
        isShowMessage = false
    }
    
    // Create Firestore document reference for the user and route.
    private fun getRef(goorback: String): com.google.firebase.firestore.DocumentReference {
        val userId = auth.currentUser?.uid ?: ""
        val userDb = firestore.collection("users").document(userId)
        return userDb.collection("goorback").document(goorback)
    }
    
    // Upload SharedPreferences data to Firestore.
    fun setFirestore(password: String) {
        if (password.isBlank()) {
            title = getApplication<Application>().getString(R.string.inputError)
            message = getApplication<Application>().getString(R.string.enterYourPassword)
            isShowMessage = true
            return
        }
        viewModelScope.launch {
            isLoading = true
            loadingMessage = getApplication<Application>().getString(R.string.savingData)
            isShowAlert = false
            isShowMessage = false
            isFirestoreSuccess = false
            title = getApplication<Application>().getString(R.string.saveDataError)
            message = getApplication<Application>().getString(R.string.dataCouldNotBeSaved)

            try {
                run { firestore; auth }
            } catch (e: Exception) {
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeSaved)
                isLoading = false
                loadingMessage = ""
                isShowMessage = true
                return@launch
            }
            try {
                val user = auth.currentUser
                if (user == null || user.email.isNullOrEmpty()) {
                    message = getApplication<Application>().getString(R.string.dataCouldNotBeSaved)
                    isLoading = false
                    loadingMessage = ""
                    isShowMessage = true
                    return@launch
                }
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()
            } catch (e: Exception) {
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeSaved)
                isLoading = false
                loadingMessage = ""
                isShowMessage = true
                return@launch
            }
            try {
                for (goorback in goorbackarray) {
                    for (linenumber in 0..2) {
                        val availableCalendarTypes = getAvailableCalendarTypesForRoute(goorback, linenumber)
                        setTimetableFirestore(goorback, linenumber, availableCalendarTypes)
                    }
                    setLineInfoFirestore(goorback)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirestoreViewModel", "Error in setFirestore", e)
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeSaved)
                isLoading = false
                loadingMessage = ""
                isShowMessage = true
            }
        }
    }
    
    // Get available calendar types for a route and line.
    private fun getAvailableCalendarTypesForRoute(goorback: String, num: Int): List<ODPTCalendarType> {
        val loadedTypes = goorback.loadAvailableCalendarTypes(sharedPreferences, num)
        val availableTypes = loadedTypes.mapNotNull { ODPTCalendarType.fromRawValue(it) }
        return availableTypes
    }
    
    // Upload timetable data for a route and line.
    private suspend fun setTimetableFirestore(goorback: String, num: Int, availableCalendarTypes: List<ODPTCalendarType>) {
        for (calendarType in availableCalendarTypes) {
            val documentName = "timetable${num + 1}${calendarType.calendarTag()}"
            val nextRef = getRef(goorback).collection("timetable").document(documentName)
            
            // Build hour data with legacy and enhanced formats.
            val hourData = mutableMapOf<String, Any>()
            
            for (hour in 4..25) {
                val hourKey = "hour${String.format(Locale.ROOT, "%02d", hour)}"
                val timetableKey = goorback.timetableKey(calendarType, num, hour)
                val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarType, num, hour)
                val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarType, num, hour)
                
                val timetableData = sharedPreferences.getString(timetableKey, "") ?: ""
                val rideTimeData = sharedPreferences.getString(timetableRideTimeKey, "") ?: ""
                val trainTypeData = sharedPreferences.getString(timetableTrainTypeKey, "") ?: ""
                
                // Legacy format for existing clients.
                hourData[hourKey] = timetableData
                
                // Enhanced format for structured data.
                hourData["${hourKey}_enhanced"] = mapOf(
                    "timetable" to timetableData,
                    "rideTime" to rideTimeData,
                    "trainType" to trainTypeData
                )
            }
            
            try {
                nextRef.set(hourData).await()
            } catch (e: Exception) {
                android.util.Log.e("FirestoreViewModel", "Error uploading timetable data", e)
            }
        }
    }
    
    // Upload line information to Firestore.
    private suspend fun setLineInfoFirestore(goorback: String) {
        val batch = firestore.batch()
        val ref = getRef(goorback)
        
        // Cache key for calendar types per line.
        fun calendarTypesCacheKey(goorback: String, num: Int) = "${goorback}line${num + 1}_calendarTypes"
        
        // Read SharedPreferences once for this batch.
        val operatorNames = goorback.operatorNameArray(sharedPreferences)
        val lineNames = goorback.lineNameArray(sharedPreferences, getApplication())
        val departStations = goorback.departStationArray(sharedPreferences, getApplication())
        val arriveStations = goorback.arriveStationArray(sharedPreferences, getApplication())
        val lineColors = goorback.lineColorStringArray(sharedPreferences)
        val lineCodes = goorback.lineCodeArray(sharedPreferences)
        val lineKinds = goorback.lineKindArray(sharedPreferences)
        val rideTimes = goorback.rideTimeArray(sharedPreferences)
        val transportations = goorback.transportationArray(sharedPreferences)
        val transferTimes = goorback.transferTimeArray(sharedPreferences)
        
        val lineData = mapOf(
            "switch" to goorback.isShowRoute2(sharedPreferences),
            "changeline" to goorback.changeLineInt(sharedPreferences).toString(),
            "departpoint" to goorback.departurePoint(sharedPreferences, getApplication()),
            "arrivalpoint" to goorback.destination(sharedPreferences, getApplication()),
            "operatorname1" to operatorNames[0],
            "operatorname2" to operatorNames[1],
            "operatorname3" to operatorNames[2],
            "linename1" to lineNames[0],
            "linename2" to lineNames[1],
            "linename3" to lineNames[2],
            "departstation1" to departStations[0],
            "departstation2" to departStations[1],
            "departstation3" to departStations[2],
            "arrivalstation1" to arriveStations[0],
            "arrivalstation2" to arriveStations[1],
            "arrivalstation3" to arriveStations[2],
            "linecolor1" to lineColors[0],
            "linecolor2" to lineColors[1],
            "linecolor3" to lineColors[2],
            "linecode1" to lineCodes[0],
            "linecode2" to lineCodes[1],
            "linecode3" to lineCodes[2],
            "linekind1" to lineKinds[0].firestoreRawValue(),
            "linekind2" to lineKinds[1].firestoreRawValue(),
            "linekind3" to lineKinds[2].firestoreRawValue(),
            "ridetime1" to rideTimes[0].toString(),
            "ridetime2" to rideTimes[1].toString(),
            "ridetime3" to rideTimes[2].toString(),
            "transportation1" to transportations[1],
            "transportation2" to transportations[2],
            "transportation3" to transportations[3],
            "transportatione" to transportations[0],
            "transittime1" to transferTimes[1].toString(),
            "transittime2" to transferTimes[2].toString(),
            "transittime3" to transferTimes[3].toString(),
            "transittimee" to transferTimes[0].toString(),
            "calendarTypes1" to (sharedPreferences.getStringSet(calendarTypesCacheKey(goorback, 0), null)?.toList() ?: emptyList()),
            "calendarTypes2" to (sharedPreferences.getStringSet(calendarTypesCacheKey(goorback, 1), null)?.toList() ?: emptyList()),
            "calendarTypes3" to (sharedPreferences.getStringSet(calendarTypesCacheKey(goorback, 2), null)?.toList() ?: emptyList())
        )
        
        batch.set(ref, lineData)
        
        try {
            batch.commit().await()
            if (goorback == "go2") {
                title = getApplication<Application>().getString(R.string.dataSavedSuccessfully)
                message = ""
                isFirestoreSuccess = true
                isLoading = false
                loadingMessage= ""
                isShowMessage = true
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreViewModel", "Error uploading line info", e)
            if (goorback == "go2") {
                isLoading = false
                loadingMessage= ""
                isShowMessage = true
            }
        }
    }
    
    // Download Firestore data into SharedPreferences.
    fun getFirestore(password: String) {
        if (password.isBlank()) {
            title = getApplication<Application>().getString(R.string.inputError)
            message = getApplication<Application>().getString(R.string.enterYourPassword)
            isShowMessage = true
            return
        }
        viewModelScope.launch {
            isLoading = true
            loadingMessage = getApplication<Application>().getString(R.string.gettingData)
            isShowAlert = false
            isShowMessage = false
            isFirestoreSuccess = false
            title = getApplication<Application>().getString(R.string.getDataError)
            message = getApplication<Application>().getString(R.string.dataCouldNotBeGot)

            try {
                run { firestore; auth }
            } catch (e: Exception) {
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                isLoading = false
                loadingMessage = ""
                isShowMessage = true
                return@launch
            }
            try {
                val user = auth.currentUser
                if (user == null || user.email.isNullOrEmpty()) {
                    message = getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                    isLoading = false
                    loadingMessage= ""
                    isShowMessage = true
                    return@launch
                }
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()
            } catch (e: Exception) {
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                isLoading = false
                loadingMessage = ""
                isShowMessage = true
                return@launch
            }
            try {
                for (goorback in goorbackarray) {
                    for (linenumber in 0..2) {
                        for (calendarType in ODPTCalendarType.allCases) {
                            getTimetableFirestore(goorback, linenumber, calendarType)
                        }
                    }
                    getLineInfoFirestore(goorback)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirestoreViewModel", "Error in getFirestore", e)
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                isLoading = false
                loadingMessage = ""
                isShowMessage = true
            }
        }
    }
    
    // Download line information into SharedPreferences.
    private suspend fun getLineInfoFirestore(goorback: String) {
        try {
            val document = getRef(goorback).get().await()
            if (document.exists() && document.data != null) {
                val data = document.data!!
                
                // Cache key for calendar types per line.
                fun calendarTypesCacheKey(goorback: String, num: Int) = "${goorback}line${num + 1}_calendarTypes"
                
                sharedPreferences.edit {
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
                        // Restore calendar types from Firestore.
                        (data["calendarTypes${num + 1}"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet()?.let { types ->
                            putStringSet(calendarTypesCacheKey(goorback, num), types)
                        }
                    }
                    
                    data["transportatione"]?.let { putString(goorback.transportationKey(0), it.toString()) }
                    data["transittimee"]?.let { putString(goorback.transferTimeKey(0), it.toString()) }
                }
                
                if (goorback == "go2") {
                    title = getApplication<Application>().getString(R.string.dataGotSuccessfully)
                    message = ""
                    isFirestoreSuccess = true
                    isLoading = false
                    loadingMessage = ""
                    isShowMessage = true
                }
            } else {
                if (goorback == "go2") {
                    message = getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                    isLoading = false
                    loadingMessage = ""
                    isShowMessage = true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreViewModel", "Error downloading line info", e)
            if (goorback == "go2") {
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                isLoading = false
                loadingMessage = ""
                isShowMessage = true
            }
            throw e
        }
    }
    
    // Download timetable data for a route and line.
    private suspend fun getTimetableFirestore(goorback: String, num: Int, calendarType: ODPTCalendarType) {
        val documentName = "timetable${num + 1}${calendarType.calendarTag()}"
        val nextRef = getRef(goorback).collection("timetable").document(documentName)
        
        try {
            val document = nextRef.get().await()
            if (document.exists() && document.data != null) {
                val data = document.data!!
                
                // Apply legacy and enhanced formats per hour.
                for (hour in 4..25) {
                    val hourKey = "hour${String.format(Locale.ROOT, "%02d", hour)}"
                    val enhancedHourKey = "${hourKey}_enhanced"
                    val timetableKey = goorback.timetableKey(calendarType, num, hour)
                    val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarType, num, hour)
                    val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarType, num, hour)
                    
                    val enhancedData = data[enhancedHourKey] as? Map<*, *>
                    if (enhancedData != null) {
                        sharedPreferences.edit {
                            putString(timetableKey, enhancedData["timetable"]?.toString() ?: "")
                            putString(timetableRideTimeKey, enhancedData["rideTime"]?.toString() ?: "")
                            putString(timetableTrainTypeKey, enhancedData["trainType"]?.toString() ?: "")
                        }
                    } else {
                        val hourDataString = data[hourKey] as? String
                        if (hourDataString != null) {
                            sharedPreferences.edit {
                                putString(timetableKey, hourDataString)
                                putString(timetableRideTimeKey, "")
                                putString(timetableTrainTypeKey, "")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreViewModel", "Error getting document for ${calendarType.debugDisplayName()}", e)
            throw e
        }
    }
}

