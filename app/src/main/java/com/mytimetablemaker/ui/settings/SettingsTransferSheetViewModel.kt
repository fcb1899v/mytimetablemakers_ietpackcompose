package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.R
import kotlinx.coroutines.flow.MutableStateFlow

// MARK: - Settings Transfer Sheet ViewModel
// View model for managing transfer settings data and business logic
class SettingsTransferSheetViewModel(
    application: Application,
    private val sharedPreferences: SharedPreferences
) : AndroidViewModel(application) {
    
    // MARK: - Published Properties
    // Observable properties that trigger UI updates when changed
    
    // Departure point and destination place names
    val homeInput = MutableStateFlow("")
    val officeInput = MutableStateFlow("")
    
    // Transportation methods for each route
    val selectedHomeTransportation1 = MutableStateFlow("")
    val selectedHomeTransportation2 = MutableStateFlow("")
    val selectedOfficeTransportation1 = MutableStateFlow("")
    val selectedOfficeTransportation2 = MutableStateFlow("")
    
    // Transfer times in minutes for each route
    val selectedHomeTransferTime1 = MutableStateFlow(0)
    val selectedHomeTransferTime2 = MutableStateFlow(0)
    val selectedOfficeTransferTime1 = MutableStateFlow(0)
    val selectedOfficeTransferTime2 = MutableStateFlow(0)
    
    // Visibility control
    val showRoute2 = MutableStateFlow(false)
    
    // MARK: - Initialization
    // Initialize view model with values from SharedPreferences
    init {
        loadSettings()
    }
    
    // MARK: - Data Loading
    // Load saved settings from persistent storage
    fun loadSettings() {
        val context = getApplication<Application>()
        // Load from unified keys (homeKey = "departurepoint", officeKey = "destination")
        // All routes use the same keys, and the direction logic is handled by departurePoint()/destination() functions
        val notSet = context.getString(R.string.notSet)
        val savedDeparturePoint = homeKey.userDefaultsValue(sharedPreferences, "") ?: ""
        homeInput.value = if (savedDeparturePoint.isNotEmpty() && savedDeparturePoint != notSet) savedDeparturePoint else ""
        
        val savedDestination = officeKey.userDefaultsValue(sharedPreferences, "") ?: ""
        officeInput.value = if (savedDestination.isNotEmpty() && savedDestination != notSet) savedDestination else ""
        // Load transportation methods with default value "walking" if empty or "none"
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
        
        // Load transfer times with default value 0
        selectedHomeTransferTime1.value = "back1".transferTimeKey(0).userDefaultsInt(sharedPreferences, 0)
        selectedHomeTransferTime2.value = "back2".transferTimeKey(0).userDefaultsInt(sharedPreferences, 0)
        selectedOfficeTransferTime1.value = "back1".transferTimeKey(1).userDefaultsInt(sharedPreferences, 0)
        selectedOfficeTransferTime2.value = "back2".transferTimeKey(1).userDefaultsInt(sharedPreferences, 0)
        
        // Load Route 2 visibility flag from SharedPreferences
        // Check both back2 and go2 keys
        val back2Route2Value = "back2".isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
        val go2Route2Value = "go2".isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
        
        // Use the same value for both routes (as per requirement)
        showRoute2.value = back2Route2Value || go2Route2Value
    }
    
    // MARK: - Route 2 Setting Management
    // Save Route 2 visibility setting to SharedPreferences immediately
    // Save to both back2 and go2 keys
    fun saveRoute2Setting() {
        sharedPreferences.edit(commit = true) {
            putBoolean("back2".isShowRoute2Key(), showRoute2.value)
            putBoolean("go2".isShowRoute2Key(), showRoute2.value)
        }
    }
    
    // MARK: - Validation
    // Check if all required fields are filled for saving
    val isFormValid: Boolean
        get() {
            // Validate departure point, destination, and transfer times
            val basicValidation = homeInput.value.isNotEmpty() &&
                    officeInput.value.isNotEmpty() &&
                    selectedHomeTransferTime1.value > 0 &&
                    selectedOfficeTransferTime1.value > 0
            
            // If Route 2 is shown, also validate Route 2 fields
            if (showRoute2.value) {
                return basicValidation &&
                        selectedHomeTransferTime2.value > 0 &&
                        selectedOfficeTransferTime2.value > 0
            }
            return basicValidation
        }
    
    // MARK: - Data Saving
    // Save current settings to persistent storage with validation
    fun saveSettings() {
        // Validate form before saving
        if (!isFormValid) {
            return
        }
        
        // Save all settings using commit() to ensure synchronous save
        // All routes (back1, back2, go1, go2) use the same keys for departure point and destination
        // - homeInput is saved to "departurepoint" (homeKey)
        // - officeInput is saved to "destination" (officeKey)
        // The departurePoint() and destination() functions handle the direction logic:
        // - For back routes: departurePoint() reads from "destination", destination() reads from "departurepoint"
        // - For go routes: departurePoint() reads from "departurepoint", destination() reads from "destination"
        sharedPreferences.edit(commit = true) {
            // Save departure point and destination using unified keys
            putString(homeKey, homeInput.value)  // "departurepoint"
            putString(officeKey, officeInput.value)  // "destination"
            
            putString("back1".transportationKey(0), selectedHomeTransportation1.value)
            putString("back2".transportationKey(0), selectedHomeTransportation2.value)
            putString("back1".transportationKey(1), selectedOfficeTransportation1.value)
            putString("back2".transportationKey(1), selectedOfficeTransportation2.value)
            putInt("back1".transferTimeKey(0), selectedHomeTransferTime1.value)
            putInt("back2".transferTimeKey(0), selectedHomeTransferTime2.value)
            putInt("back1".transferTimeKey(1), selectedOfficeTransferTime1.value)
            putInt("back2".transferTimeKey(1), selectedOfficeTransferTime2.value)
            
            // Save for go routes (mirror back routes)
            putString("go1".transportationKey(1), selectedHomeTransportation1.value)
            putString("go2".transportationKey(1), selectedHomeTransportation2.value)
            putString("go1".transportationKey(0), selectedOfficeTransportation1.value)
            putString("go2".transportationKey(0), selectedOfficeTransportation2.value)
            putInt("go1".transferTimeKey(1), selectedHomeTransferTime1.value)
            putInt("go2".transferTimeKey(1), selectedHomeTransferTime2.value)
            putInt("go1".transferTimeKey(0), selectedOfficeTransferTime1.value)
            putInt("go2".transferTimeKey(0), selectedOfficeTransferTime2.value)
        }
        
        // Update Route 2 visibility settings
        saveRoute2Setting()
        
        // Post notification to update MainContentView
        // Use SharedPreferences to trigger OnSharedPreferenceChangeListener
        // Use commit() to ensure the notification is sent immediately
        sharedPreferences.edit(commit = true) {
            putLong("SettingsTransferUpdated", System.currentTimeMillis())
        }
    }
}

