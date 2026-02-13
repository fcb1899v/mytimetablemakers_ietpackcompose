package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.R
import kotlinx.coroutines.flow.MutableStateFlow

// ViewModel for managing transfer settings data and business logic
class SettingsTransferSheetViewModel(
    application: Application,
    private val sharedPreferences: SharedPreferences
) : AndroidViewModel(application) {
    
    // Place names
    val homeInput = MutableStateFlow("")
    val officeInput = MutableStateFlow("")
    
    // Transportation methods
    val selectedHomeTransportation1 = MutableStateFlow("")
    val selectedHomeTransportation2 = MutableStateFlow("")
    val selectedOfficeTransportation1 = MutableStateFlow("")
    val selectedOfficeTransportation2 = MutableStateFlow("")
    
    // Transfer times (minutes)
    val selectedHomeTransferTime1 = MutableStateFlow(0)
    val selectedHomeTransferTime2 = MutableStateFlow(0)
    val selectedOfficeTransferTime1 = MutableStateFlow(0)
    val selectedOfficeTransferTime2 = MutableStateFlow(0)
    
    // Route 2 visibility
    val showRoute2 = MutableStateFlow(false)
    
    init {
        loadSettings()
    }
    
    // Load transfer settings from SharedPreferences
    fun loadSettings() {
        val context = getApplication<Application>()
        val notSet = context.getString(R.string.notSet)
        val savedDeparturePoint = homeKey.userDefaultsValue(sharedPreferences, "") ?: ""
        homeInput.value = if (savedDeparturePoint.isNotEmpty() && savedDeparturePoint != notSet) savedDeparturePoint else ""
        
        val savedDestination = officeKey.userDefaultsValue(sharedPreferences, "") ?: ""
        officeInput.value = if (savedDestination.isNotEmpty() && savedDestination != notSet) savedDestination else ""
        
        // Default to WALKING if empty or none
        val defaultTransportation = com.mytimetablemaker.models.TransferType.WALKING.rawValue
        selectedHomeTransportation1.value = ("back1".transportationKey(0).userDefaultsValue(sharedPreferences, "") ?: "").let { 
            if (it.isEmpty() || it == "none") defaultTransportation else it 
        }
        selectedHomeTransportation2.value = ("back2".transportationKey(0).userDefaultsValue(sharedPreferences, "") ?: "").let { 
            if (it.isEmpty() || it == "none") defaultTransportation else it 
        }
        selectedOfficeTransportation1.value = ("back1".transportationKey(1).userDefaultsValue(sharedPreferences, "") ?: "").let { 
            if (it.isEmpty() || it == "none") defaultTransportation else it 
        }
        selectedOfficeTransportation2.value = ("back2".transportationKey(1).userDefaultsValue(sharedPreferences, "") ?: "").let { 
            if (it.isEmpty() || it == "none") defaultTransportation else it 
        }
        
        selectedHomeTransferTime1.value = "back1".transferTimeKey(0).userDefaultsInt(sharedPreferences, 0)
        selectedHomeTransferTime2.value = "back2".transferTimeKey(0).userDefaultsInt(sharedPreferences, 0)
        selectedOfficeTransferTime1.value = "back1".transferTimeKey(1).userDefaultsInt(sharedPreferences, 0)
        selectedOfficeTransferTime2.value = "back2".transferTimeKey(1).userDefaultsInt(sharedPreferences, 0)
        
        val back2Route2Value = "back2".isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
        val go2Route2Value = "go2".isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
        showRoute2.value = back2Route2Value || go2Route2Value
    }
    
    // Save Route 2 visibility to both back2 and go2 keys
    fun saveRoute2Setting() {
        sharedPreferences.edit(commit = true) {
            putBoolean("back2".isShowRoute2Key(), showRoute2.value)
            putBoolean("go2".isShowRoute2Key(), showRoute2.value)
        }
    }
    
    // Validate all required form fields
    val isFormValid: Boolean
        get() {
            val basicValidation = homeInput.value.isNotEmpty() &&
                    officeInput.value.isNotEmpty() &&
                    selectedHomeTransferTime1.value > 0 &&
                    selectedOfficeTransferTime1.value > 0
            
            if (showRoute2.value) {
                return basicValidation &&
                        selectedHomeTransferTime2.value > 0 &&
                        selectedOfficeTransferTime2.value > 0
            }
            return basicValidation
        }
    
    // Save all transfer settings to SharedPreferences
    fun saveSettings() {
        if (!isFormValid) {
            return
        }
        
        sharedPreferences.edit(commit = true) {
            putString(homeKey, homeInput.value)
            putString(officeKey, officeInput.value)
            
            putString("back1".transportationKey(0), selectedHomeTransportation1.value)
            putString("back2".transportationKey(0), selectedHomeTransportation2.value)
            putString("back1".transportationKey(1), selectedOfficeTransportation1.value)
            putString("back2".transportationKey(1), selectedOfficeTransportation2.value)
            putInt("back1".transferTimeKey(0), selectedHomeTransferTime1.value)
            putInt("back2".transferTimeKey(0), selectedHomeTransferTime2.value)
            putInt("back1".transferTimeKey(1), selectedOfficeTransferTime1.value)
            putInt("back2".transferTimeKey(1), selectedOfficeTransferTime2.value)
            
            putString("go1".transportationKey(1), selectedHomeTransportation1.value)
            putString("go2".transportationKey(1), selectedHomeTransportation2.value)
            putString("go1".transportationKey(0), selectedOfficeTransportation1.value)
            putString("go2".transportationKey(0), selectedOfficeTransportation2.value)
            putInt("go1".transferTimeKey(1), selectedHomeTransferTime1.value)
            putInt("go2".transferTimeKey(1), selectedHomeTransferTime2.value)
            putInt("go1".transferTimeKey(0), selectedOfficeTransferTime1.value)
            putInt("go2".transferTimeKey(0), selectedOfficeTransferTime2.value)
        }
        
        saveRoute2Setting()
        
        // Trigger listener notification
        sharedPreferences.edit(commit = true) {
            putLong("SettingsTransferUpdated", System.currentTimeMillis())
        }
    }
}

