package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mytimetablemaker.extensions.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        homeInput.value = sharedPreferences.getString(homeKey, "") ?: ""
        officeInput.value = sharedPreferences.getString(officeKey, "") ?: ""
        selectedHomeTransportation1.value = "back1".transportationKey(0).userDefaultsValue(sharedPreferences, "") ?: ""
        selectedHomeTransportation2.value = "back2".transportationKey(0).userDefaultsValue(sharedPreferences, "") ?: ""
        selectedOfficeTransportation1.value = "back1".transportationKey(1).userDefaultsValue(sharedPreferences, "") ?: ""
        selectedOfficeTransportation2.value = "back2".transportationKey(1).userDefaultsValue(sharedPreferences, "") ?: ""
        
        // Load transfer times with default value 0
        selectedHomeTransferTime1.value = "back1".transferTimeKey(0).userDefaultsInt(sharedPreferences, 0)
        selectedHomeTransferTime2.value = "back2".transferTimeKey(0).userDefaultsInt(sharedPreferences, 0)
        selectedOfficeTransferTime1.value = "back1".transferTimeKey(1).userDefaultsInt(sharedPreferences, 0)
        selectedOfficeTransferTime2.value = "back2".transferTimeKey(1).userDefaultsInt(sharedPreferences, 0)
        
        // Load Route 2 visibility flag from SharedPreferences
        val back2Route2Value = "back2".isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
        val go2Route2Value = "go2".isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
        
        // Use the same value for both routes (as per requirement)
        showRoute2.value = back2Route2Value || go2Route2Value
    }
    
    // MARK: - Route 2 Setting Management
    // Save Route 2 visibility setting to SharedPreferences immediately
    fun saveRoute2Setting() {
        sharedPreferences.edit().apply {
            putBoolean("back2".isShowRoute2Key(), showRoute2.value)
            putBoolean("go2".isShowRoute2Key(), showRoute2.value)
            apply()
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
        
        sharedPreferences.edit().apply {
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
            
            // Save for go routes (mirror back routes)
            putString("go1".transportationKey(1), selectedHomeTransportation1.value)
            putString("go2".transportationKey(1), selectedHomeTransportation2.value)
            putString("go1".transportationKey(0), selectedOfficeTransportation1.value)
            putString("go2".transportationKey(0), selectedOfficeTransportation2.value)
            putInt("go1".transferTimeKey(1), selectedHomeTransferTime1.value)
            putInt("go2".transferTimeKey(1), selectedHomeTransferTime2.value)
            putInt("go1".transferTimeKey(0), selectedOfficeTransferTime1.value)
            putInt("go2".transferTimeKey(0), selectedOfficeTransferTime2.value)
            
            apply()
        }
        
        // Update Route 2 visibility settings
        saveRoute2Setting()
        
        // Post notification to update MainContentView (same as SwiftUI's NotificationCenter.post)
        // Use SharedPreferences to trigger OnSharedPreferenceChangeListener
        sharedPreferences.edit()
            .putLong("SettingsTransferUpdated", System.currentTimeMillis())
            .apply()
    }
}

