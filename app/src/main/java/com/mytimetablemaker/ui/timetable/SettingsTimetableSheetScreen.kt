package com.mytimetablemaker.ui.timetable

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.zIndex
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.models.*
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.theme.*
import androidx.core.content.edit

// Helper function to get train type display name from resource
private fun getTrainTypeDisplayName(trainType: String, context: Context): String {
    // Extract resource name from train type string (e.g., "odpt.TrainType:JR-East.ChuoSpecialRapid" -> "chuoSpecialRapid")
    val parts = trainType.split(".")
    if (parts.size >= 3) {
        val resourceName = parts[2].replaceFirstChar { it.lowercaseChar() }
        return when (resourceName) {
            "accessExpress" -> context.getString(R.string.accessExpress)
            "airportRapidLimitedExpress" -> context.getString(R.string.airportRapidLimitedExpress)
            "chuoSpecialRapid" -> context.getString(R.string.chuoSpecialRapid)
            "commuterExpress" -> context.getString(R.string.commuterExpress)
            "commuterLimitedExpress" -> context.getString(R.string.commuterLimitedExpress)
            "commuterRapid" -> context.getString(R.string.commuterRapid)
            "commuterSemiExpress" -> context.getString(R.string.commuterSemiExpress)
            "commuterSpecialRapid" -> context.getString(R.string.commuterSpecialRapid)
            "eveningWing" -> context.getString(R.string.eveningWing)
            "express" -> context.getString(R.string.express)
            "haijimaLiner" -> context.getString(R.string.haijimaLiner)
            "kawagoeLimitedExpress" -> context.getString(R.string.kawagoeLimitedExpress)
            "limitedExpress" -> context.getString(R.string.limitedExpress)
            "liner" -> context.getString(R.string.liner)
            "local" -> context.getString(R.string.local)
            "morningWing" -> context.getString(R.string.morningWing)
            "omeSpecialRapid" -> context.getString(R.string.omeSpecialRapid)
            "rapid" -> context.getString(R.string.rapid)
            "rapidExpress" -> context.getString(R.string.rapidExpress)
            "rapidLimitedExpress" -> context.getString(R.string.rapidLimitedExpress)
            "semiExpress" -> context.getString(R.string.semiExpress)
            "semiRapid" -> context.getString(R.string.semiRapid)
            "sectionExpress" -> context.getString(R.string.sectionExpress)
            "sectionSemiExpress" -> context.getString(R.string.sectionSemiExpress)
            "specialRapid" -> context.getString(R.string.specialRapid)
            "fLiner" -> context.getString(R.string.fLiner)
            "sTrain" -> context.getString(R.string.sTrain)
            "slTaiju" -> context.getString(R.string.slTaiju)
            "thLiner" -> context.getString(R.string.thLiner)
            "tjLiner" -> context.getString(R.string.tjLiner)
            else -> trainType
        }
    }
    return trainType
}

// MARK: - Settings Timetable Sheet Screen
// Sheet for editing timetable times with add/delete/copy functionality
// Matches SwiftUI SettingsTimetableSheet structure
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTimetableSheetScreen(
    goorback: String,
    selectedCalendarType: ODPTCalendarType,
    num: Int,
    hour: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
    // State variables
    var departureTime by remember { mutableStateOf<Int?>(null) }
    var displayDepartureTime by remember { mutableStateOf<Int?>(null) }
    var rideTime by remember { mutableStateOf<Int?>(null) }
    var selectedTrainType by remember { mutableStateOf<String?>(null) }
    var isTrainTypeDropdownOpen by remember { mutableStateOf(false) }
    var isCalendarTypeDropdownOpen by remember { mutableStateOf(false) }
    var isCopyTimeDropdownOpen by remember { mutableStateOf(false) }
    var currentHour by remember { mutableIntStateOf(hour) }
    var currentCalendarType by remember { mutableStateOf(selectedCalendarType) }
    var transportationTimes by remember { mutableStateOf<List<TransportationTime>>(emptyList()) }
    var availableOdptCalendar by remember { mutableStateOf<List<ODPTCalendarType>>(emptyList()) }
    
    // Initialize state
    LaunchedEffect(Unit) {
        // Load saved ride time as default value (using route-level key without hour)
        val rideTimeKey = goorback.rideTimeKey(num)
        val savedRideTime = sharedPreferences.getInt(rideTimeKey, 0)
        rideTime = if (savedRideTime == 0) null else savedRideTime
        
        // Load available calendar types
        val loadedTypesString = goorback.loadAvailableCalendarTypes(sharedPreferences, num)
        availableOdptCalendar = loadedTypesString.mapNotNull { typeString ->
            ODPTCalendarType.fromRawValue("odpt.Calendar:$typeString")
        }
        
        // Load transportation times
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
    }
    
    // Update transportation times when hour or calendar type changes
    LaunchedEffect(currentHour, currentCalendarType) {
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
    }
    
    // MARK: - Helper Functions
    // Check if the selected departure time exists in the timetable (for deletion)
    fun isTimeExistsForDeletion(): Boolean {
        val departureTimeValue = departureTime ?: return false
        val calendarTag = currentCalendarType.calendarTag()
        val timetableKey = goorback.timetableKey(calendarTag, num, currentHour)
        val timetableString = sharedPreferences.getString(timetableKey, null) ?: return false
        return timetableString.containsTimeInAnyFormat(departureTimeValue)
    }
    
    // Check if the exact same entry (departure time, ride time, and train type) already exists
    fun isExactSameEntryExists(): Boolean {
        val departureTimeValue = departureTime ?: return false
        val rideTimeValue = rideTime ?: return false
        
        val calendarTag = currentCalendarType.calendarTag()
        val timetableKey = goorback.timetableKey(calendarTag, num, currentHour)
        val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarTag, num, currentHour)
        val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarTag, num, currentHour)
        
        val currentTimetableString = sharedPreferences.getString(timetableKey, null) ?: ""
        val currentTrainTypeString = sharedPreferences.getString(timetableTrainTypeKey, null) ?: ""
        val currentRideTimeString = sharedPreferences.getString(timetableRideTimeKey, null) ?: ""
        
        val departureTimes = currentTimetableString.timetableComponents
        val trainTypes = currentTrainTypeString.timetableComponents
        val rideTimes = currentRideTimeString.timetableComponents
        
        val newTimeString = departureTimeValue.addZeroTime()
        val newTrainType = selectedTrainType ?: ""
        val newRideTimeString = rideTimeValue.addZeroTime()
        
        val isRailway = goorback.lineKind(sharedPreferences, num) == TransportationLineKind.RAILWAY
        
        // Check if exact same entry exists
        for ((index, time) in departureTimes.withIndex()) {
            val matchesTime = time == newTimeString || time == departureTimeValue.toString()
            val matchesRideTime = if (index < rideTimes.size) rideTimes[index] == newRideTimeString else false
            
            // For railway, also check train type; for bus, skip train type check
            val matchesType: Boolean
            if (isRailway) {
                val existingType = if (index < trainTypes.size) trainTypes[index] else ""
                matchesType = existingType == newTrainType
            } else {
                matchesType = true // Bus doesn't have train type
            }
            
            if (matchesTime && matchesType && matchesRideTime) {
                return true
            }
        }
        
        return false
    }
    
    // Get available train types for selection
    fun getAvailableTrainTypes(): List<String> {
        val defaultTypeList = listOf(
            DisplayTrainType.DEFAULT_LOCAL.rawValue,
            DisplayTrainType.DEFAULT_EXPRESS.rawValue,
            DisplayTrainType.DEFAULT_RAPID.rawValue,
            DisplayTrainType.DEFAULT_SPECIAL_RAPID.rawValue,
            DisplayTrainType.DEFAULT_LIMITED_EXPRESS.rawValue
        )
        
        // First, try to get existing train types from the current line
        val existingTrainTypes = goorback.loadTrainTypeList(currentCalendarType, num, sharedPreferences)
        
        // If no existing train types are found, return default list
        if (existingTrainTypes.isEmpty()) {
            return defaultTypeList
        }
        
        // Check if existingTrainTypes contains only default types
        val hasOnlyDefaultTypes = existingTrainTypes.all { trainType ->
            defaultTypeList.contains(trainType)
        }
        
        // If existingTrainTypes has only default types, return defaultTypeList
        // Otherwise, return existingTrainTypes (which includes custom types)
        return if (hasOnlyDefaultTypes) defaultTypeList else existingTrainTypes
    }
    
    // Update train type list
    fun updateTrainTypeList(trainType: String) {
        val calendarTag = currentCalendarType.calendarTag()
        val trainTypeListKey = goorback.trainTypeListKey(calendarTag, num)
        val existingListString = sharedPreferences.getString(trainTypeListKey, null) ?: ""
        val existingList = existingListString.timetableComponents.toMutableList()
        
        // Add new train type if not already in the list
        if (!existingList.contains(trainType)) {
            existingList.add(trainType)
            sharedPreferences.edit {
                putString(trainTypeListKey, existingList.joinToString(" "))
            }
        }
    }
    
    // Save train type list for all hours
    fun saveTrainTypeListForAllHours() {
        val validHours = goorback.validHourRange(currentCalendarType, num, sharedPreferences)
        val allTransportationTimes = mutableListOf<TransportationTime>()
        
        // Collect all transportation times from all valid hours
        for (hourValue in validHours) {
            val times = goorback.loadTransportationTimes(
                currentCalendarType,
                num,
                hourValue,
                sharedPreferences
            )
            allTransportationTimes.addAll(times)
        }
        
        // Save train type list
        goorback.saveTrainTypeList(
            allTransportationTimes,
            currentCalendarType,
            num,
            sharedPreferences
        )
    }
    
    // Add time and train type pair
    fun addTimeAndTrainTypePair(departureTime: Int, trainType: String?, rideTime: Int) {
        val calendarTag = currentCalendarType.calendarTag()
        val timetableKey = goorback.timetableKey(calendarTag, num, currentHour)
        val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarTag, num, currentHour)
        val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarTag, num, currentHour)
        val routeRideTimeKey = goorback.rideTimeKey(num)
        
        // Get current data
        val currentTimetableString = sharedPreferences.getString(timetableKey, null) ?: ""
        val currentTrainTypeString = sharedPreferences.getString(timetableTrainTypeKey, null) ?: ""
        val currentRideTimeString = sharedPreferences.getString(timetableRideTimeKey, null) ?: ""
        val defaultRideTime = sharedPreferences.getInt(routeRideTimeKey, 0)
        
        // Convert to arrays
        val departureTimes = currentTimetableString.timetableComponents.toMutableList()
        val trainTypes = currentTrainTypeString.timetableComponents.toMutableList()
        val rideTimes = currentRideTimeString.timetableComponents.toMutableList()
        
        val newTimeString = departureTime.addZeroTime()
        val newTrainType = trainType ?: ""
        val newRideTimeString = rideTime.addZeroTime()
        
        // Check if the same time already exists
        var existingIndex: Int? = null
        for ((index, time) in departureTimes.withIndex()) {
            if (time == newTimeString || time == departureTime.toString()) {
                existingIndex = index
                break
            }
        }
        
        if (existingIndex != null) {
            // Overwrite existing time with new train type and ride time
            val index = existingIndex
            if (index < trainTypes.size) {
                trainTypes[index] = newTrainType
            } else {
                trainTypes.add(newTrainType)
            }
            if (index < rideTimes.size) {
                rideTimes[index] = newRideTimeString
            } else {
                rideTimes.add(newRideTimeString)
            }
        } else {
            // Add new time, train type, and ride time
            departureTimes.add(newTimeString)
            trainTypes.add(newTrainType)
            rideTimes.add(newRideTimeString)
        }
        
        // Create triplets and sort by departure time
        val timeTypeRidePairs = mutableListOf<Triple<String, String, String>>()
        for (i in departureTimes.indices) {
            val type = if (i < trainTypes.size) trainTypes[i] else "defaultLocal"
            val rideTimeStr = if (i < rideTimes.size) rideTimes[i] else defaultRideTime.addZeroTime()
            timeTypeRidePairs.add(Triple(departureTimes[i], type, rideTimeStr))
        }
        
        // Sort triplets by time (convert to Int for proper sorting)
        // Matches SwiftUI: timeTypeRidePairs.sort { pair1.time.isTimeLessThan(pair2.time) }
        timeTypeRidePairs.sortWith { (time1, _, _), (time2, _, _) ->
            if (time1.isTimeLessThan(time2)) -1 else 1
        }
        
        // Extract sorted arrays
        val sortedTimes = timeTypeRidePairs.map { it.first }
        val sortedTypes = timeTypeRidePairs.map { it.second }
        val sortedRideTimes = timeTypeRidePairs.map { it.third }
        
        // Save to SharedPreferences
        sharedPreferences.edit {
            putString(timetableKey, sortedTimes.joinToString(" "))
                .putString(timetableTrainTypeKey, sortedTypes.joinToString(" "))
                .putString(timetableRideTimeKey, sortedRideTimes.joinToString(" "))
        }
        
        // Update train type list if new type was added
        if (trainType != null) {
            updateTrainTypeList(trainType)
        }
    }
    
    // Delete time and train type pair
    fun deleteTimeAndTrainTypePair(departureTime: Int) {
        val calendarTag = currentCalendarType.calendarTag()
        val timetableKey = goorback.timetableKey(calendarTag, num, currentHour)
        val timetableTrainTypeKey = goorback.timetableTrainTypeKey(calendarTag, num, currentHour)
        val timetableRideTimeKey = goorback.timetableRideTimeKey(calendarTag, num, currentHour)
        val routeRideTimeKey = goorback.rideTimeKey(num)
        
        // Get current data
        val currentTimetableString = sharedPreferences.getString(timetableKey, null) ?: ""
        val currentTrainTypeString = sharedPreferences.getString(timetableTrainTypeKey, null) ?: ""
        val currentRideTimeString = sharedPreferences.getString(timetableRideTimeKey, null) ?: ""
        val defaultRideTime = sharedPreferences.getInt(routeRideTimeKey, 0)
        
        // Convert to arrays
        val departureTimes = currentTimetableString.timetableComponents.toMutableList()
        val trainTypes = currentTrainTypeString.timetableComponents.toMutableList()
        val rideTimes = currentRideTimeString.timetableComponents.toMutableList()
        
        // Find and remove the time (try both single and double digit formats)
        val singleDigitTime = departureTime.toString()
        val doubleDigitTime = departureTime.addZeroTime()
        
        var indexToRemove: Int? = null
        for ((index, time) in departureTimes.withIndex()) {
            if (time == singleDigitTime || time == doubleDigitTime) {
                indexToRemove = index
                break
            }
        }
        
        // Remove the time, corresponding train type, and ride time
        if (indexToRemove != null) {
            val index = indexToRemove
            departureTimes.removeAt(index)
            if (index < trainTypes.size) {
                trainTypes.removeAt(index)
            }
            if (index < rideTimes.size) {
                rideTimes.removeAt(index)
            }
        }
        
        // Create triplets and sort by departure time
        val timeTypeRidePairs = mutableListOf<Triple<String, String, String>>()
        for (i in departureTimes.indices) {
            val type = if (i < trainTypes.size) trainTypes[i] else "defaultLocal"
            val rideTimeStr = if (i < rideTimes.size) rideTimes[i] else defaultRideTime.addZeroTime()
            timeTypeRidePairs.add(Triple(departureTimes[i], type, rideTimeStr))
        }
        
        // Sort triplets by time (convert to Int for proper sorting)
        // Matches SwiftUI: timeTypeRidePairs.sort { pair1.time.isTimeLessThan(pair2.time) }
        timeTypeRidePairs.sortWith { (time1, _, _), (time2, _, _) ->
            if (time1.isTimeLessThan(time2)) -1 else 1
        }
        
        // Extract sorted arrays
        val sortedTimes = timeTypeRidePairs.map { it.first }
        val sortedTypes = timeTypeRidePairs.map { it.second }
        val sortedRideTimes = timeTypeRidePairs.map { it.third }
        
        // Save to SharedPreferences
        sharedPreferences.edit {
            putString(timetableKey, sortedTimes.joinToString(" "))
                .putString(timetableTrainTypeKey, sortedTypes.joinToString(" "))
                .putString(timetableRideTimeKey, sortedRideTimes.joinToString(" "))
        }
    }
    
    // Add or update time entry in timetable
    fun addTime() {
        val departureTimeValue = departureTime ?: return
        val rideTimeValue = rideTime ?: return
        
        // Add time, train type, and ride time as a triplet, then sort all together
        addTimeAndTrainTypePair(
            departureTime = departureTimeValue,
            trainType = selectedTrainType,
            rideTime = rideTimeValue
        )
        
        // Update transportationTimes array with fresh data from SharedPreferences
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
        
        // Save train type list for all hours
        saveTrainTypeListForAllHours()
        
        // Save display value
        displayDepartureTime = departureTimeValue
    }
    
    // Delete time entry from timetable
    fun deleteTime() {
        val departureTimeValue = departureTime ?: return
        
        deleteTimeAndTrainTypePair(departureTime = departureTimeValue)
        
        // Update transportationTimes array with fresh data from SharedPreferences
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
        
        // Save train type list for all hours
        saveTrainTypeListForAllHours()
        
        // Save display value
        displayDepartureTime = departureTimeValue
    }
    
    // Copy timetable times from another hour or route
    fun copyTime(index: Int) {
        val calendarTag = currentCalendarType.calendarTag()
        val timetableKey = goorback.timetableKey(calendarTag, num, currentHour)
        val copiedTime = goorback.choiceCopyTime(
            currentCalendarType,
            num,
            currentHour,
            index,
            sharedPreferences
        )
        
        sharedPreferences.edit {
            putString(timetableKey, copiedTime)
        }
        
        // Update transportationTimes when time is copied
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
        
        // Save train type list for all hours
        saveTrainTypeListForAllHours()
    }
    
    // MARK: - Main Content
    // Matches SwiftUI: .presentationDetents([.height(screen.settingsTimetableSheetHeight)])
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Set dialog height to match SwiftUI presentationDetents
        Scaffold(
            modifier = Modifier.height(ScreenSize.settingsTimetableSheetHeight()),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.editTimetable),
                            fontSize = ScreenSize.settingsTitleFontSize().value.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    },
                    navigationIcon = {
                        CommonComponents.CustomBackButton(
                            foregroundColor = Black,
                            onClick = onDismiss
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(White)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding()),
                    verticalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetVerticalSpacing())
                ) {
                    // Calendar type dropdown section
                    CalendarDropDownSection(
                        selectedCalendarType = currentCalendarType,
                        availableOdptCalendar = availableOdptCalendar,
                        isCalendarTypeDropdownOpen = isCalendarTypeDropdownOpen,
                        onCalendarTypeSelected = { calendarType ->
                            currentCalendarType = calendarType
                            isCalendarTypeDropdownOpen = false
                            transportationTimes = goorback.loadTransportationTimes(
                                calendarType,
                                num,
                                currentHour,
                                sharedPreferences
                            )
                        },
                        onToggleDropdown = { isCalendarTypeDropdownOpen = !isCalendarTypeDropdownOpen }
                    )
                    
                    // Hour control section
                    HourControlSection(
                        hour = currentHour,
                        onDecreaseHour = {
                            if (currentHour > 4) {
                                currentHour -= 1
                                transportationTimes = goorback.loadTransportationTimes(
                                    currentCalendarType,
                                    num,
                                    currentHour,
                                    sharedPreferences
                                )
                            }
                        },
                        onIncreaseHour = {
                            if (currentHour < 24) {
                                currentHour += 1
                                transportationTimes = goorback.loadTransportationTimes(
                                    currentCalendarType,
                                    num,
                                    currentHour,
                                    sharedPreferences
                                )
                            }
                        }
                    )
                    
                    // Timetable display section
                    TimetableDisplaySection(
                        transportationTimes = transportationTimes
                    )
                    
                    // Departure time and ride time selection sections
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ScreenSize.settingsSheetVerticalSpacing()),
                        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
                    ) {
                        DepartureTimeSelectSection(
                            departureTime = departureTime,
                            displayDepartureTime = displayDepartureTime,
                            onDepartureTimeChange = { time ->
                                departureTime = time
                                displayDepartureTime = time
                                isTrainTypeDropdownOpen = false
                            },
                            isTimeExistsForDeletion = isTimeExistsForDeletion(),
                            selectedTrainType = selectedTrainType,
                            goorback = goorback,
                            num = num,
                            sharedPreferences = sharedPreferences
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        RideTimeSelectSection(
                            rideTime = rideTime,
                            onRideTimeChange = { time ->
                                rideTime = if (time == 0) null else time
                                isTrainTypeDropdownOpen = false
                            },
                            isTimeExistsForDeletion = isTimeExistsForDeletion()
                        )
                    }
                    
                    // Train type selection section (only for railway lines)
                    if (goorback.lineKind(sharedPreferences, num) == TransportationLineKind.RAILWAY) {
                        TrainTypeSelectSection(
                            selectedTrainType = selectedTrainType,
                            availableTrainTypes = getAvailableTrainTypes(),
                            isTrainTypeDropdownOpen = isTrainTypeDropdownOpen,
                            onTrainTypeSelected = { trainType ->
                                selectedTrainType = trainType
                                isTrainTypeDropdownOpen = false
                            },
                            onToggleDropdown = { isTrainTypeDropdownOpen = !isTrainTypeDropdownOpen },
                            isTimeExistsForDeletion = isTimeExistsForDeletion()
                        )
                    }
                    
                    // Add/Update and Delete buttons section
                    AddDeleteButtonSection(
                        isTimeExistsForDeletion = isTimeExistsForDeletion(),
                        rideTime = rideTime,
                        departureTime = departureTime,
                        selectedTrainType = selectedTrainType,
                        goorback = goorback,
                        num = num,
                        sharedPreferences = sharedPreferences,
                        isExactSameEntryExists = isExactSameEntryExists(),
                        onAddTime = { addTime() },
                        onDeleteTime = { deleteTime() }
                    )
                    
                    // Copy time button section
                    CopyTimeButtonSection(
                        isCopyTimeDropdownOpen = isCopyTimeDropdownOpen,
                        onToggleDropdown = {
                            isTrainTypeDropdownOpen = false
                            isCalendarTypeDropdownOpen = false
                            isCopyTimeDropdownOpen = !isCopyTimeDropdownOpen
                        },
                        hour = currentHour,
                        onCopyTime = { index -> copyTime(index) }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Dropdown options overlay
                if (isCalendarTypeDropdownOpen) {
                    CalendarTypeDropdownView(
                        availableCalendarTypes = availableOdptCalendar,
                        onCalendarTypeSelected = { calendarType ->
                            currentCalendarType = calendarType
                            isCalendarTypeDropdownOpen = false
                            transportationTimes = goorback.loadTransportationTimes(
                                calendarType,
                                num,
                                currentHour,
                                sharedPreferences
                            )
                        },
                        onDismiss = { isCalendarTypeDropdownOpen = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = ScreenSize.timetableTypeMenuOffsetX(),
                                y = ScreenSize.timetableCalendarMenuOffsetY()
                            )
                            .zIndex(1f)
                    )
                }
                
                if (isTrainTypeDropdownOpen) {
                    TrainTypeDropdownView(
                        availableTrainTypes = getAvailableTrainTypes(),
                        onTrainTypeSelected = { trainType ->
                            selectedTrainType = trainType
                            isTrainTypeDropdownOpen = false
                        },
                        onDismiss = { isTrainTypeDropdownOpen = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = ScreenSize.timetableTypeMenuOffsetX(),
                                y = ScreenSize.timetableTypeMenuOffsetY()
                            )
                            .zIndex(1f)
                    )
                }
                
                if (isCopyTimeDropdownOpen) {
                    CopyTimeDropdownView(
                        hour = currentHour,
                        onCopyTime = { index ->
                            copyTime(index)
                            isCopyTimeDropdownOpen = false
                        },
                        onDismiss = { isCopyTimeDropdownOpen = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = ScreenSize.timetableTypeMenuOffsetX(),
                                y = ScreenSize.timetableCopyMenuOffsetY()
                            )
                            .zIndex(1f)
                    )
                }
            }
        }
    }
}

// MARK: - Calendar Dropdown Section
@Composable
private fun CalendarDropDownSection(
    selectedCalendarType: ODPTCalendarType,
    availableOdptCalendar: List<ODPTCalendarType>,
    isCalendarTypeDropdownOpen: Boolean,
    onCalendarTypeSelected: (ODPTCalendarType) -> Unit,
    onToggleDropdown: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onToggleDropdown,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Unspecified
            ),
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = ScreenSize.settingsSheetInputPaddingHorizontal(),
                        vertical = ScreenSize.settingsSheetInputPaddingVertical()
                    )
                    .background(
                        color = Gray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    ),
                horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCalendarType.displayName(context),
                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = selectedCalendarType.calendarSubColor
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(ScreenSize.settingsSheetTitleFontSize())
                        .graphicsLayer {
                            rotationZ = if (isCalendarTypeDropdownOpen) 180f else 0f
                        },
                    tint = selectedCalendarType.calendarSubColor
                )
            }
        }
    }
}

// MARK: - Hour Control Section
@Composable
private fun HourControlSection(
    hour: Int,
    onDecreaseHour: () -> Unit,
    onIncreaseHour: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ScreenSize.settingsSheetVerticalSpacing()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecreaseHour,
            enabled = hour > 4
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = if (hour > 4) Primary else Gray,
                modifier = Modifier.size(ScreenSize.settingsSheetTitleFontSize())
            )
        }
        
        Text(
            text = "$hour${stringResource(R.string.hour)}",
            fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier.width(ScreenSize.timetableEditButtonWidth())
        )
        
        IconButton(
            onClick = onIncreaseHour,
            enabled = hour < 24
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (hour < 24) Primary else Gray,
                modifier = Modifier.size(ScreenSize.settingsSheetTitleFontSize())
            )
        }
    }
}

// MARK: - Timetable Display Section
@Composable
private fun TimetableDisplaySection(
    transportationTimes: List<TransportationTime>
) {
    if (transportationTimes.isNotEmpty()) {
        val availableWidth = ScreenSize.timetableDisplayWidth().value - (ScreenSize.timetableMinuteSpacing().value * 2)
        val itemsPerRow = 10
        val itemWidth = availableWidth / itemsPerRow
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(itemsPerRow),
            modifier = Modifier
                .width(ScreenSize.timetableDisplayWidth())
                .height(ScreenSize.timetableDisplayHeight())
                .background(Primary)
                .padding(horizontal = ScreenSize.timetableMinuteSpacing()),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(transportationTimes) { index, transportationTime ->
                Row(
                    modifier = Modifier
                        .width(itemWidth.dp)
                        .height(ScreenSize.timetableNumberHeight()),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val trainType = (transportationTime as? TrainTime)?.trainType
                    Text(
                        text = transportationTime.departureTime.minutesOnly,
                        fontSize = ScreenSize.timetableMinuteFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorForTrainType(trainType),
                        maxLines = 1
                    )
                    Text(
                        text = "(${transportationTime.rideTime})",
                        fontSize = ScreenSize.timetableRideTimeFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White,
                        maxLines = 1
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .width(ScreenSize.timetableDisplayWidth())
                .height(ScreenSize.timetableDisplayHeight())
                .background(Primary)
                .padding(horizontal = ScreenSize.timetableHorizontalSpacing())
        )
    }
}

// MARK: - Departure Time Select Section
@Composable
private fun DepartureTimeSelectSection(
    departureTime: Int?,
    displayDepartureTime: Int?,
    onDepartureTimeChange: (Int) -> Unit,
    isTimeExistsForDeletion: Boolean,
    selectedTrainType: String?,
    goorback: String,
    num: Int,
    sharedPreferences: SharedPreferences
) {
    val context = LocalContext.current
    val isRailway = goorback.lineKind(sharedPreferences, num) == TransportationLineKind.RAILWAY
    
    // Matches SwiftUI: .padding(.top, screen.timetablePickerTopPadding)
    //                  .padding(.bottom, screen.timetablePickerBottomPadding)
    Column(
        modifier = Modifier
            .width(ScreenSize.timetablePickerWidth())
            .padding(
                top = ScreenSize.timetablePickerTopPadding(),
                bottom = ScreenSize.timetablePickerBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetVerticalSpacing())
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.departureTime),
                fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
            
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(ScreenSize.settingsSheetHeadlineFontSize()),
                tint = when {
                    departureTime == null -> Gray
                    !isRailway && !isTimeExistsForDeletion -> Accent
                    !isRailway && isTimeExistsForDeletion -> Primary
                    selectedTrainType != null && !isTimeExistsForDeletion -> Accent
                    selectedTrainType != null && isTimeExistsForDeletion -> Primary
                    isTimeExistsForDeletion -> Red
                    else -> Gray
                }
            )
        }
        
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScreenSize.settingsSheetPickerDisplayHeight())
                    .padding(
                        vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                        horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                    )
                    .background(
                        color = White,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    )
                    .border(
                        width = ScreenSize.borderWidth(),
                        color = Gray,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if ((departureTime ?: displayDepartureTime) == null) {
                        "-"
                    } else {
                        "${departureTime ?: displayDepartureTime}${stringResource(R.string.min)}"
                    },
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                    color = Black
                )
            }
            
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = ScreenSize.settingsSheetInputPaddingHorizontal())
            ) {
                CommonComponents.Custom2DigitPicker(
                    value = departureTime ?: displayDepartureTime ?: 0,
                    onValueChange = onDepartureTimeChange,
                    isZeroToFive = true
                )
            }
        }
    }
}

// MARK: - Ride Time Select Section
@Composable
private fun RideTimeSelectSection(
    rideTime: Int?,
    onRideTimeChange: (Int) -> Unit,
    isTimeExistsForDeletion: Boolean
) {
    val context = LocalContext.current
    
    // Matches SwiftUI: .padding(.top, screen.timetablePickerTopPadding)
    //                  .padding(.bottom, screen.timetablePickerBottomPadding)
    Column(
        modifier = Modifier
            .width(ScreenSize.timetablePickerWidth())
            .padding(
                top = ScreenSize.timetablePickerTopPadding(),
                bottom = ScreenSize.timetablePickerBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetVerticalSpacing())
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.rideTime),
                fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
            
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(ScreenSize.settingsSheetHeadlineFontSize()),
                tint = when {
                    rideTime != null && !isTimeExistsForDeletion -> Accent
                    rideTime != null && isTimeExistsForDeletion -> Primary
                    else -> Red
                }
            )
        }
        
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScreenSize.settingsSheetPickerDisplayHeight())
                    .padding(
                        vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                        horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                    )
                    .background(
                        color = White,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    )
                    .border(
                        width = ScreenSize.borderWidth(),
                        color = Gray,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (rideTime == null) {
                        "-"
                    } else {
                        "$rideTime${stringResource(R.string.min)}"
                    },
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                    color = Black
                )
            }
            
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = ScreenSize.settingsSheetInputPaddingHorizontal())
            ) {
                CommonComponents.Custom2DigitPicker(
                    value = rideTime ?: 0,
                    onValueChange = onRideTimeChange,
                    isZeroToFive = false
                )
            }
        }
    }
}

// MARK: - Train Type Select Section
@Composable
private fun TrainTypeSelectSection(
    selectedTrainType: String?,
    availableTrainTypes: List<String>,
    isTrainTypeDropdownOpen: Boolean,
    onTrainTypeSelected: (String) -> Unit,
    onToggleDropdown: () -> Unit,
    isTimeExistsForDeletion: Boolean
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.selectType),
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary
        )
        
        Button(
            onClick = onToggleDropdown,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Unspecified
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScreenSize.settingsSheetPickerDisplayHeight())
                    .padding(
                        vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                        horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                    )
                    .background(
                        color = Primary,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    )
                    .border(
                        width = ScreenSize.borderWidth(),
                        color = Gray,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTrainType != null) {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = null,
                            modifier = Modifier.size(ScreenSize.settingsSheetIconSize()),
                            tint = colorForTrainType(selectedTrainType)
                        )
                    }
                    Text(
                        text = selectedTrainType?.let { trainType ->
                            getTrainTypeDisplayName(trainType, context)
                        } ?: stringResource(R.string.dash),
                        fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White,
                        maxLines = 1
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(ScreenSize.settingsSheetInputFontSize())
                        .graphicsLayer {
                            rotationZ = if (isTrainTypeDropdownOpen) 180f else 0f
                        },
                    tint = White
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(ScreenSize.settingsSheetHeadlineFontSize()),
            tint = when {
                selectedTrainType != null && !isTimeExistsForDeletion -> Accent
                selectedTrainType != null && isTimeExistsForDeletion -> Primary
                else -> Red
            }
        )
    }
}

// MARK: - Add Delete Button Section
@Composable
private fun AddDeleteButtonSection(
    isTimeExistsForDeletion: Boolean,
    rideTime: Int?,
    departureTime: Int?,
    selectedTrainType: String?,
    goorback: String,
    num: Int,
    sharedPreferences: SharedPreferences,
    isExactSameEntryExists: Boolean,
    onAddTime: () -> Unit,
    onDeleteTime: () -> Unit
) {
    val isRailway = goorback.lineKind(sharedPreferences, num) == TransportationLineKind.RAILWAY
    val isEnabled = rideTime != null &&
            departureTime != null &&
            (isRailway && selectedTrainType != null || !isRailway) &&
            !isExactSameEntryExists
    
    val addButtonColor = if (isTimeExistsForDeletion) {
        if (isEnabled) Primary else Gray
    } else {
        Accent
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ScreenSize.settingsSheetVerticalSpacing()),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
    ) {
        CommonComponents.CustomButton(
            title = if (isTimeExistsForDeletion) stringResource(R.string.update) else stringResource(R.string.add),
            icon = if (isTimeExistsForDeletion) Icons.Default.Refresh else Icons.Default.Add,
            backgroundColor = addButtonColor,
            textColor = White,
            isEnabled = isEnabled,
            modifier = Modifier.width(ScreenSize.timetableEditButtonWidth()),
            onClick = onAddTime
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        CommonComponents.CustomButton(
            title = stringResource(R.string.delete),
            icon = Icons.Default.Remove,
            backgroundColor = if (departureTime != null && isTimeExistsForDeletion) Red else Gray,
            textColor = White,
            isEnabled = departureTime != null && isTimeExistsForDeletion,
            modifier = Modifier.width(ScreenSize.timetableEditButtonWidth()),
            onClick = onDeleteTime
        )
    }
}

// MARK: - Copy Time Button Section
@Composable
private fun CopyTimeButtonSection(
    isCopyTimeDropdownOpen: Boolean,
    onToggleDropdown: () -> Unit,
    hour: Int,
    onCopyTime: (Int) -> Unit
) {
    Button(
        onClick = onToggleDropdown,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Unspecified
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ScreenSize.settingsSheetVerticalSpacing())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ScreenSize.settingsSheetButtonHeight())
                .background(
                    color = Accent,
                    shape = RoundedCornerShape(ScreenSize.settingsSheetButtonCornerRadius())
                )
                .padding(horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()),
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetIconSpacing()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(ScreenSize.settingsSheetButtonFontSize()),
                tint = White
            )
            Text(
                text = stringResource(R.string.copyingYourTimetable),
                fontSize = ScreenSize.settingsSheetButtonFontSize().value.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(ScreenSize.settingsSheetInputFontSize())
                    .graphicsLayer {
                        rotationZ = if (isCopyTimeDropdownOpen) 180f else 0f
                    },
                tint = White
            )
        }
    }
}

// MARK: - Calendar Type Dropdown View
@Composable
private fun CalendarTypeDropdownView(
    availableCalendarTypes: List<ODPTCalendarType>,
    onCalendarTypeSelected: (ODPTCalendarType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .width(ScreenSize.timetableTypeMenuWidth())
            .background(
                color = White,
            )
            .border(
                width = ScreenSize.borderWidth(),
                color = Gray,
            ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        availableCalendarTypes.forEachIndexed { index, calendarType ->
            Button(
                onClick = {
                    onCalendarTypeSelected(calendarType)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Unspecified
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                            horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = calendarType.displayName(context),
                        fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = calendarType.calendarSubColor,
                        maxLines = 1
                    )
                }
            }
            
            if (index < availableCalendarTypes.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.height(ScreenSize.borderWidth()),
                    color = Gray
                )
            }
        }
    }
}

// MARK: - Train Type Dropdown View
@Composable
private fun TrainTypeDropdownView(
    availableTrainTypes: List<String>,
    onTrainTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .width(ScreenSize.timetableTypeMenuWidth())
            .background(
                color = Primary,
            )
            .border(
                width = ScreenSize.borderWidth(),
                color = White,
            ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        availableTrainTypes.forEachIndexed { index, trainType ->
            Button(
                onClick = {
                    onTrainTypeSelected(trainType)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Unspecified
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                            horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                        ),
                    horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Train,
                        contentDescription = null,
                        modifier = Modifier.size(ScreenSize.settingsSheetIconSize()),
                        tint = colorForTrainType(trainType)
                    )
                    Text(
                        text = getTrainTypeDisplayName(trainType, context),
                        fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White,
                        maxLines = 1
                    )
                }
            }
            
            if (index < availableTrainTypes.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.height(ScreenSize.borderWidth()),
                    color = White
                )
            }
        }
    }
}

// MARK: - Copy Time Dropdown View
@Composable
private fun CopyTimeDropdownView(
    hour: Int,
    onCopyTime: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val startIndex = if (hour == 4) 1 else 0
    val items = hour.choiceCopyTimeList(context)
    
    Column(
        modifier = modifier
            .width(ScreenSize.timetableTypeMenuWidth())
            .background(
                color = Accent,
            )
            .border(
                width = ScreenSize.borderWidth(),
                color = White,
            ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        (startIndex until items.size).forEach { index ->
            if (!(hour == 25 && index == 1)) {
                Button(
                    onClick = {
                        onCopyTime(index)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Unspecified
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                                horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = items[index],
                            fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = White,
                            maxLines = 1
                        )
                    }
                }
                
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.height(ScreenSize.borderWidth()),
                        color = White
                    )
                }
            }
        }
    }
}

