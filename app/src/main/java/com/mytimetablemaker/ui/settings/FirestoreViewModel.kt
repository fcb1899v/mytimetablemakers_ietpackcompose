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

// MARK: - App Constants
// Array of route direction identifiers
val goorbackarray = listOf("back1", "go1", "back2", "go2")

// MARK: - Firestore Data Manager
// Handles data synchronization between SharedPreferences and Firestore database
class FirestoreViewModel(application: Application) : AndroidViewModel(application) {
    
    // Use same SharedPreferences as MainViewModel so save/get sync with app data
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("MainViewModel", Application.MODE_PRIVATE)
    // Lazy init so Firebase is not touched at ViewModel creation (avoids crash on DEVELOPER_ERROR at app start)
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

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
    
    // Dismiss Firestore result alert (called from UI when user taps OK)
    fun dismissMessage() {
        isShowMessage = false
    }
    
    // MARK: - Firestore Reference Helper
    // Creates Firestore document reference for current user and route
    private fun getRef(goorback: String): com.google.firebase.firestore.DocumentReference {
        val userId = auth.currentUser?.uid ?: ""
        val userDb = firestore.collection("users").document(userId)
        return userDb.collection("goorback").document(goorback)
    }
    
    // MARK: - Data Upload
    // Uploads all SharedPreferences data to Firestore server (requires password for re-authentication)
    fun setFirestore(password: String) {
        if (password.isBlank()) {
            title = getApplication<Application>().getString(R.string.inputError)
            message = getApplication<Application>().getString(R.string.enterYourPassword)
            isShowMessage = true
            return
        }
        viewModelScope.launch {
            isLoading = true
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
                isShowMessage = true
                return@launch
            }
            try {
                val user = auth.currentUser
                if (user == null || user.email.isNullOrEmpty()) {
                    message = getApplication<Application>().getString(R.string.dataCouldNotBeSaved)
                    isLoading = false
                    isShowMessage = true
                    return@launch
                }
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()
            } catch (e: Exception) {
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeSaved)
                isLoading = false
                isShowMessage = true
                return@launch
            }
            try {
                // Process each route (go/back) and upload timetable and line information (sequential await)
                for (goorback in goorbackarray) {
                    for (linenumber in 0..2) {
                        val availableCalendarTypes = getAvailableCalendarTypesForRoute(goorback, linenumber)
                        setTimetableFirestore(goorback, linenumber, availableCalendarTypes)
                    }
                    setLineInfoFirestore(goorback)
                }
            } catch (e: Exception) {
                println("❌ Error in setFirestore: ${e.message}")
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeSaved)
                isLoading = false
                isShowMessage = true
            }
        }
    }
    
    // MARK: - Available Calendar Types Detection
    // Get available calendar types for specific route and line (from cache or fallback to 3 standard types)
    private fun getAvailableCalendarTypesForRoute(goorback: String, num: Int): List<ODPTCalendarType> {
        val loadedTypes = goorback.loadAvailableCalendarTypes(sharedPreferences, num)
        val availableTypes = loadedTypes.mapNotNull { ODPTCalendarType.fromRawValue(it) }
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
                val timetableKey = goorback.timetableKey(calendarType, num, hour)
                val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarType, num, hour)
                val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarType, num, hour)
                
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
        
        // Calendar types cache key per line (saved when ODPT API fetches types; empty = show 3 standard types)
        fun calendarTypesCacheKey(goorback: String, num: Int) = "${goorback}line${num + 1}_calendarTypes"
        
        val lineData = mapOf(
            "switch" to goorback.isShowRoute2(sharedPreferences),
            "changeline" to goorback.changeLineInt(sharedPreferences).toString(),
            "departpoint" to goorback.departurePoint(sharedPreferences, getApplication()),
            "arrivalpoint" to goorback.destination(sharedPreferences, getApplication()),
            "operatorname1" to goorback.operatorNameArray(sharedPreferences)[0],
            "operatorname2" to goorback.operatorNameArray(sharedPreferences)[1],
            "operatorname3" to goorback.operatorNameArray(sharedPreferences)[2],
            "linename1" to goorback.lineNameArray(sharedPreferences, getApplication())[0],
            "linename2" to goorback.lineNameArray(sharedPreferences, getApplication())[1],
            "linename3" to goorback.lineNameArray(sharedPreferences, getApplication())[2],
            "departstation1" to goorback.departStationArray(sharedPreferences, getApplication())[0],
            "departstation2" to goorback.departStationArray(sharedPreferences, getApplication())[1],
            "departstation3" to goorback.departStationArray(sharedPreferences, getApplication())[2],
            "arrivalstation1" to goorback.arriveStationArray(sharedPreferences, getApplication())[0],
            "arrivalstation2" to goorback.arriveStationArray(sharedPreferences, getApplication())[1],
            "arrivalstation3" to goorback.arriveStationArray(sharedPreferences, getApplication())[2],
            "linecolor1" to goorback.lineColorStringArray(sharedPreferences)[0],
            "linecolor2" to goorback.lineColorStringArray(sharedPreferences)[1],
            "linecolor3" to goorback.lineColorStringArray(sharedPreferences)[2],
            "linecode1" to goorback.lineCodeArray(sharedPreferences)[0],
            "linecode2" to goorback.lineCodeArray(sharedPreferences)[1],
            "linecode3" to goorback.lineCodeArray(sharedPreferences)[2],
            "linekind1" to goorback.lineKindArray(sharedPreferences)[0].firestoreRawValue(),
            "linekind2" to goorback.lineKindArray(sharedPreferences)[1].firestoreRawValue(),
            "linekind3" to goorback.lineKindArray(sharedPreferences)[2].firestoreRawValue(),
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
            "transittimee" to goorback.transferTimeArray(sharedPreferences)[0].toString(),
            "calendarTypes1" to (sharedPreferences.getStringSet(calendarTypesCacheKey(goorback, 0), null)?.toList() ?: emptyList()),
            "calendarTypes2" to (sharedPreferences.getStringSet(calendarTypesCacheKey(goorback, 1), null)?.toList() ?: emptyList()),
            "calendarTypes3" to (sharedPreferences.getStringSet(calendarTypesCacheKey(goorback, 2), null)?.toList() ?: emptyList())
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
    // Downloads all data from Firestore server to SharedPreferences (requires password for re-authentication)
    fun getFirestore(password: String) {
        if (password.isBlank()) {
            title = getApplication<Application>().getString(R.string.inputError)
            message = getApplication<Application>().getString(R.string.enterYourPassword)
            isShowMessage = true
            return
        }
        viewModelScope.launch {
            isLoading = true
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
                isShowMessage = true
                return@launch
            }
            try {
                val user = auth.currentUser
                if (user == null || user.email.isNullOrEmpty()) {
                    message = getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                    isLoading = false
                    isShowMessage = true
                    return@launch
                }
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()
            } catch (e: Exception) {
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                isLoading = false
                isShowMessage = true
                return@launch
            }
            try {
                // Process each route (go/back) and download timetable and line information (sequential await)
                // Use all calendar types on download so we fetch everything on server (new device may have no local data yet)
                for (goorback in goorbackarray) {
                    for (linenumber in 0..2) {
                        for (calendarType in ODPTCalendarType.allCases) {
                            getTimetableFirestore(goorback, linenumber, calendarType)
                        }
                    }
                    getLineInfoFirestore(goorback)
                }
            } catch (e: Exception) {
                println("❌ Error in getFirestore: ${e.message}")
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeGot)
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
                
                // Calendar types cache key per line
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
                        // Restore calendar types from Firestore (list from ODPT API; empty = show 3 standard types)
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
                    isShowMessage = true
                }
            } else {
                if (goorback == "go2") {
                    message = getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                    isLoading = false
                    isShowMessage = true
                }
            }
        } catch (e: Exception) {
            println("❌ Error downloading line info: ${e.message}")
            if (goorback == "go2") {
                message = e.message ?: getApplication<Application>().getString(R.string.dataCouldNotBeGot)
                isLoading = false
                isShowMessage = true
            }
            throw e
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
                    val timetableKey = goorback.timetableKey(calendarType, num, hour)
                    val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarType, num, hour)
                    val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarType, num, hour)
                    
                    // Try to load enhanced format first (new format)
                    val enhancedData = data[enhancedHourKey] as? Map<*, *>
                    if (enhancedData != null) {
                        // New format with structured data
                        sharedPreferences.edit {
                            putString(timetableKey, enhancedData["timetable"]?.toString() ?: "")
                            putString(timetableRideTimeKey, enhancedData["rideTime"]?.toString() ?: "")
                            putString(timetableTrainTypeKey, enhancedData["trainType"]?.toString() ?: "")
                        }
                    } else {
                        // Legacy format - fallback to old string-only format for backward compatibility
                        val hourDataString = data[hourKey] as? String
                        if (hourDataString != null) {
                            sharedPreferences.edit {
                                putString(timetableKey, hourDataString)
                                // Clear ride time and train type for legacy data
                                putString(timetableRideTimeKey, "")
                                putString(timetableTrainTypeKey, "")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Error getting document for ${calendarType.debugDisplayName()}: ${e.message}")
            throw e
        }
    }
}

