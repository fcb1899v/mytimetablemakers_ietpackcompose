package com.mytimetablemaker.ui.timetable

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import androidx.core.view.WindowCompat
import com.mytimetablemaker.models.getTrainTypeDisplayName

// Sheet for editing timetable times with add/delete/copy functionality
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
    
    // State management
    var departureTime by remember { mutableStateOf<Int?>(null) }
    var displayDepartureTime by remember { mutableStateOf<Int?>(null) }
    var rideTime by remember { mutableStateOf<Int?>(null) }
    var selectedTrainType by remember { mutableStateOf<String?>(null) }
    var isCalendarTypeDropdownOpen by remember { mutableStateOf(false) }
    var isCopyTimeDropdownOpen by remember { mutableStateOf(false) }
    var currentHour by remember { mutableIntStateOf(hour) }
    var currentCalendarType by remember { mutableStateOf(selectedCalendarType) }
    var transportationTimes by remember { mutableStateOf<List<TransportationTime>>(emptyList()) }
    var availableOdptCalendar by remember { mutableStateOf<List<ODPTCalendarType>>(emptyList()) }
    
    val horizontalPadding = ScreenSize.settingsSheetHorizontalPadding()
    val verticalSpacing = ScreenSize.settingsSheetVerticalSpacing()
    val titleFontSize = ScreenSize.settingsTitleFontSize()
    
    // Configure status bar
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = false
            }
        }
        onDispose { }
    }
    
    // Initialize state on screen load
    LaunchedEffect(Unit) {
        // Load saved ride time as default value (using route-level key without hour identifier)
        val rideTimeKey = goorback.rideTimeKey(num)
        val savedRideTime = rideTimeKey.userDefaultsInt(sharedPreferences, 0)
        rideTime = if (savedRideTime == 0) null else savedRideTime
        
        // Load available calendar types (returns full rawValue strings like "odpt.Calendar:Weekday")
        val loadedTypesString = goorback.loadAvailableCalendarTypes(sharedPreferences, num)
        availableOdptCalendar = loadedTypesString.mapNotNull { typeString ->
            ODPTCalendarType.fromRawValue(typeString)
        }
        
        // Load initial transportation times
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
    }
    
    // Reload times when hour or calendar type changes
    LaunchedEffect(currentHour, currentCalendarType) {
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
    }
    
    // Check if departure time exists in timetable (for validation before deletion)
    fun isTimeExistsForDeletion(): Boolean {
        val departureTimeValue = departureTime ?: return false
        val timetableKey = goorback.timetableKey(currentCalendarType, num, currentHour)
        val timetableString = sharedPreferences.getString(timetableKey, null) ?: return false
        return timetableString.containsTimeInAnyFormat(departureTimeValue)
    }
    
    // Check if exact same entry exists (time, ride time, train type) to prevent duplicates
    fun isExactSameEntryExists(): Boolean {
        val departureTimeValue = departureTime ?: return false
        val rideTimeValue = rideTime ?: return false
        
        val timetableKey = goorback.timetableKey(currentCalendarType, num, currentHour)
        val timetableTrainTypeKey = goorback.timetableTrainTypeKey(currentCalendarType, num, currentHour)
        val timetableRideTimeKey = goorback.timetableRideTimeKey(currentCalendarType, num, currentHour)
        
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
        
        // Check for exact match: Railway checks time+type+ride, Bus checks time+ride only
        for ((index, time) in departureTimes.withIndex()) {
            val matchesTime = time == newTimeString || time == departureTimeValue.toString()
            val matchesRideTime = if (index < rideTimes.size) rideTimes[index] == newRideTimeString else false
            
            // Railway checks train type, bus doesn't
            val matchesType: Boolean
            if (isRailway) {
                val existingType = if (index < trainTypes.size) trainTypes[index] else ""
                matchesType = existingType == newTrainType
            } else {
                matchesType = true
            }
            
            if (matchesTime && matchesType && matchesRideTime) {
                return true
            }
        }
        
        return false
    }
    
    // Get available train types for dropdown selection
    // Returns default types + operator-specific types if available
    fun getAvailableTrainTypes(): List<String> {
        val defaultTypeList = listOf(
            DisplayTrainType.DEFAULT_LOCAL.rawValue,
            DisplayTrainType.DEFAULT_EXPRESS.rawValue,
            DisplayTrainType.DEFAULT_RAPID.rawValue,
            DisplayTrainType.DEFAULT_SPECIAL_RAPID.rawValue,
            DisplayTrainType.DEFAULT_LIMITED_EXPRESS.rawValue
        )

        // Manual input mode should always show only the five default types.
        val isLineSelected = goorback.lineSelected(sharedPreferences, num)
        val hasOperatorCode = goorback.operatorCode(sharedPreferences, num).isNotEmpty()
        val hasLineCode = goorback.lineCode(sharedPreferences, num).isNotEmpty()
        val isFetchedMode = isLineSelected && hasOperatorCode && hasLineCode
        if (!isFetchedMode) {
            return defaultTypeList
        }
        
        // First, try to get existing train types from the current line's saved data
        val existingTrainTypes = goorback
            .loadTrainTypeList(currentCalendarType, num, sharedPreferences)
            .distinct()
        
        // If no existing train types are found, use default list.
        if (existingTrainTypes.isEmpty()) {
            return defaultTypeList
        }

        // In fetched mode, show only operator-derived train types.
        val operatorTypes = existingTrainTypes
            .filterNot { trainType -> defaultTypeList.contains(trainType) }
            .distinct()

        return operatorTypes
    }
    
    // Add new train type to list if not already present
    fun updateTrainTypeList(trainType: String) {
        val trainTypeListKey = goorback.trainTypeListKey(currentCalendarType, num)
        val existingListString = sharedPreferences.getString(trainTypeListKey, null) ?: ""
        val existingList = existingListString.timetableComponents.toMutableList()
        
        if (!existingList.contains(trainType)) {
            existingList.add(trainType)
            sharedPreferences.edit {
                putString(trainTypeListKey, existingList.joinToString(" "))
            }
        }
    }
    
    // Save train type list for all hours
    // Collects all transportation times across all valid hours and saves consolidated train type list
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
        
        // Save consolidated train type list
        goorback.saveTrainTypeList(
            allTransportationTimes,
            currentCalendarType,
            num,
            sharedPreferences
        )
    }
    
    // Add or update departure time with train type and ride time, then sort all entries
    fun addTimeAndTrainTypePair(departureTime: Int, trainType: String?, rideTime: Int) {
        val timetableKey = goorback.timetableKey(currentCalendarType, num, currentHour)
        val timetableTrainTypeKey = goorback.timetableTrainTypeKey(currentCalendarType, num, currentHour)
        val timetableRideTimeKey = goorback.timetableRideTimeKey(currentCalendarType, num, currentHour)
        val routeRideTimeKey = goorback.rideTimeKey(num)
        
        // Load current timetable data
        val currentTimetableString = sharedPreferences.getString(timetableKey, null) ?: ""
        val currentTrainTypeString = sharedPreferences.getString(timetableTrainTypeKey, null) ?: ""
        val currentRideTimeString = sharedPreferences.getString(timetableRideTimeKey, null) ?: ""
        val defaultRideTime = routeRideTimeKey.userDefaultsInt(sharedPreferences, 0)
        val departureTimes = currentTimetableString.timetableComponents.toMutableList()
        val trainTypes = currentTrainTypeString.timetableComponents.toMutableList()
        val rideTimes = currentRideTimeString.timetableComponents.toMutableList()
        
        val newTimeString = departureTime.addZeroTime()
        val newTrainType = trainType ?: ""
        val newRideTimeString = rideTime.addZeroTime()
        
        // Check if time already exists
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
            // Pad arrays to match departureTimes size if shorter (when keys were empty)
            while (trainTypes.size <= index) {
                trainTypes.add("defaultLocal")
            }
            while (rideTimes.size <= index) {
                rideTimes.add(defaultRideTime.addZeroTime())
            }
            trainTypes[index] = newTrainType
            rideTimes[index] = newRideTimeString
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
    
    // Delete departure time entry and its associated train type and ride time
    // Removes the entry from all three synchronized arrays
    fun deleteTimeAndTrainTypePair(departureTime: Int) {
        val timetableKey = goorback.timetableKey(currentCalendarType, num, currentHour)
        val timetableTrainTypeKey = goorback.timetableTrainTypeKey(currentCalendarType, num, currentHour)
        val timetableRideTimeKey = goorback.timetableRideTimeKey(currentCalendarType, num, currentHour)
        val routeRideTimeKey = goorback.rideTimeKey(num)
        
        // Load current timetable data
        val currentTimetableString = sharedPreferences.getString(timetableKey, null) ?: ""
        val currentTrainTypeString = sharedPreferences.getString(timetableTrainTypeKey, null) ?: ""
        val currentRideTimeString = sharedPreferences.getString(timetableRideTimeKey, null) ?: ""
        val defaultRideTime = routeRideTimeKey.userDefaultsInt(sharedPreferences, 0)
        
        // Parse to lists
        val departureTimes = currentTimetableString.timetableComponents.toMutableList()
        val trainTypes = currentTrainTypeString.timetableComponents.toMutableList()
        val rideTimes = currentRideTimeString.timetableComponents.toMutableList()
        
        // Find and remove the time (try both single-digit and double-digit formats)
        val singleDigitTime = departureTime.toString()
        val doubleDigitTime = departureTime.addZeroTime()
        
        var indexToRemove: Int? = null
        for ((index, time) in departureTimes.withIndex()) {
            if (time == singleDigitTime || time == doubleDigitTime) {
                indexToRemove = index
                break
            }
        }
        
        // Remove entry and associated data
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
    
    // Add new time entry to timetable with validation
    fun addTime() {
        val departureTimeValue = departureTime ?: return
        val rideTimeValue = rideTime ?: return
        
        // Add triplet and sort all entries
        addTimeAndTrainTypePair(
            departureTime = departureTimeValue,
            trainType = selectedTrainType,
            rideTime = rideTimeValue
        )
        
        // Reload transportation times
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
        
        // Update global train type list
        saveTrainTypeListForAllHours()
        
        // Update display state
        displayDepartureTime = departureTimeValue
    }
    
    // Remove time entry from timetable
    fun deleteTime() {
        val departureTimeValue = departureTime ?: return
        
        deleteTimeAndTrainTypePair(departureTime = departureTimeValue)
        
        // Reload transportation times
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
        
        // Update global train type list
        saveTrainTypeListForAllHours()
        
        // Update display state
        displayDepartureTime = departureTimeValue
    }
    
    // Copy timetable data (times, train types, ride times) from another hour or route
    // Overwrites current hour's data with data from the selected source
    fun copyTime(index: Int) {
        val keyArray = goorback.choiceCopyTimeKeyArray(currentCalendarType, num, currentHour)
        if (index < 0 || index >= keyArray.size) return
        
        // Get source and destination keys for all three data arrays
        val sourceTimetableKey = keyArray[index]
        val sourceTrainTypeKey = "${sourceTimetableKey}traintype"
        val sourceRideTimeKey = "${sourceTimetableKey}ridetime"
        
        val timetableKey = goorback.timetableKey(currentCalendarType, num, currentHour)
        val timetableTrainTypeKey = goorback.timetableTrainTypeKey(currentCalendarType, num, currentHour)
        val timetableRideTimeKey = goorback.timetableRideTimeKey(currentCalendarType, num, currentHour)
        
        // Copy all timetable data from source to current hour
        val copiedTime = goorback.choiceCopyTime(currentCalendarType, num, currentHour, index, sharedPreferences)
        val copiedTrainTypes = sharedPreferences.getString(sourceTrainTypeKey, null) ?: ""
        val copiedRideTimes = sharedPreferences.getString(sourceRideTimeKey, null) ?: ""
        
        // Save copied data to current hour's keys
        sharedPreferences.edit {
            putString(timetableKey, copiedTime)
                .putString(timetableTrainTypeKey, copiedTrainTypes)
                .putString(timetableRideTimeKey, copiedRideTimes)
        }
        
        // Reload with copied data
        transportationTimes = goorback.loadTransportationTimes(
            currentCalendarType,
            num,
            currentHour,
            sharedPreferences
        )
        
        // Update global train type list
        saveTrainTypeListForAllHours()
    }
    
    // MARK: - Main Content
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Set dialog height for sheet; top bar matches SettingsLineSheet / SettingsTransferSheet
        Scaffold(
            modifier = Modifier.height(ScreenSize.settingsTimetableSheetHeight()),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(ScreenSize.settingsSheetTopBarHeight())
                        .background(White)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = ScreenSize.settingsSheetBackButtonPadding())
                    ) {
                        CommonComponents.CustomBackButton(
                            foregroundColor = Black,
                            onClick = onDismiss
                        )
                    }
                    Text(
                        text = stringResource(R.string.editTimetable),
                        fontSize = titleFontSize.value.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
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
                        .padding(horizontal = horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    // Calendar type dropdown and hour control (reduced spacing between them)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetCompactVerticalSpacing())
                    ) {
                        CalendarDropDownSection(
                            selectedCalendarType = currentCalendarType,
                            isCalendarTypeDropdownOpen = isCalendarTypeDropdownOpen,
                            onToggleDropdown = { isCalendarTypeDropdownOpen = !isCalendarTypeDropdownOpen }
                        )
                        HourControlSection(
                            hour = currentHour,
                            trainCount = goorback.getTrainTimesCounts(currentCalendarType, num, sharedPreferences)
                                .getOrElse(goorback.validHourRange(currentCalendarType, num, sharedPreferences).indexOf(currentHour)) { 0 },
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
                    }
                    
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
                            },
                            isTimeExistsForDeletion = isTimeExistsForDeletion()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(verticalSpacing))
                    
                    // Train type selection section (only for railway lines)
                    if (goorback.lineKind(sharedPreferences, num) == TransportationLineKind.RAILWAY) {
                        TrainTypeSelectSection(
                            selectedTrainType = selectedTrainType,
                            availableTrainTypes = getAvailableTrainTypes(),
                            onTrainTypeSelected = { trainType -> selectedTrainType = trainType },
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
                            isCalendarTypeDropdownOpen = false
                            isCopyTimeDropdownOpen = !isCopyTimeDropdownOpen
                        }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Dropdown options overlay (same as TimetableContentScreen: TopStart + offset)
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
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = ScreenSize.timetableCalendarMenuOffsetY())
                            .zIndex(1f)
                    )
                }
                if (isCopyTimeDropdownOpen) {
                    CopyTimeDropdownView(
                        currentHour = currentHour,
                        onCopyTime = { index ->
                            copyTime(index)
                            isCopyTimeDropdownOpen = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = ScreenSize.timetableCopyMenuOffsetY())
                            .zIndex(1f)
                    )
                }
            }
        }
    }
}

// Calendar type dropdown selector
@Composable
private fun CalendarDropDownSection(
    selectedCalendarType: ODPTCalendarType,
    isCalendarTypeDropdownOpen: Boolean,
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
                containerColor = LightGray,
                contentColor = LightGray
            ),
            modifier = Modifier
                .wrapContentSize()
        ) {
            Row(
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

// Hour navigation controls with train count display
@Composable
private fun HourControlSection(
    hour: Int,
    trainCount: Int = 0,
    onDecreaseHour: () -> Unit,
    onIncreaseHour: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            text = if (trainCount > 0) "$hour${stringResource(R.string.hour)} ($trainCount)" else "$hour${stringResource(R.string.hour)}",
            fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            textAlign = TextAlign.Center,
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

// Display timetable entries in a grid
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
                .padding(
                    horizontal = ScreenSize.timetableMinuteSpacing(),
                )
                .padding(
                    bottom = ScreenSize.timetableDisplayBottomSpacing(),
                ),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center,
        ) {
            items(transportationTimes) { transportationTime ->
                Row(
                    modifier = Modifier
                        .width(itemWidth.dp)
                        .height(ScreenSize.timetableNumberHeight()),
                ) {
                    val trainType = (transportationTime as? TrainTime)?.trainType
                    Text(
                        text = transportationTime.departureTime.minutesOnly,
                        fontSize = ScreenSize.timetableMinuteFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorForTrainType(trainType),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = "(${transportationTime.rideTime})",
                        fontSize = ScreenSize.timetableRideTimeFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White,
                        textAlign = TextAlign.Center,
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

// Minute picker for departure time selection
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
    val isRailway = goorback.lineKind(sharedPreferences, num) == TransportationLineKind.RAILWAY
    
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
                color = Primary,
                modifier = Modifier.wrapContentWidth()
            )
            Text(
                text = if ((departureTime ?: displayDepartureTime) == null) "-" else "${departureTime ?: displayDepartureTime} ${stringResource(R.string.min)}",
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black
            )
    
            Spacer(modifier = Modifier.weight(1f))    

            CommonComponents.Custom2DigitPicker(
                value = departureTime ?: displayDepartureTime ?: 0,
                onValueChange = onDepartureTimeChange,
                isZeroToFive = true
            )

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(ScreenSize.settingsSheetIconSize()),
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
    }
}

// Ride time input with 2-digit picker
@Composable
private fun RideTimeSelectSection(
    rideTime: Int?,
    onRideTimeChange: (Int) -> Unit,
    isTimeExistsForDeletion: Boolean
) {
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
                color = Primary,
                modifier = Modifier.wrapContentWidth()
            )
            
            Text(
                text = if (rideTime == null) "-" else "$rideTime ${stringResource(R.string.min)}",
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black
            )
                
            Spacer(modifier = Modifier.weight(1f))    

            CommonComponents.Custom2DigitPicker(
                value = rideTime ?: 0,
                onValueChange = onRideTimeChange,
                isZeroToFive = false
            )

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(ScreenSize.settingsSheetIconSize()),
                tint = when {
                    rideTime != null && !isTimeExistsForDeletion -> Accent
                    rideTime != null && isTimeExistsForDeletion -> Primary
                    else -> Red
                }
            )
        }
    }
}

// MARK: - Train Type Select Section
// Design matches TransportationSettingsSection (ExposedDropdownMenuBox); colors kept (Primary, White, colorForTrainType)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainTypeSelectSection(
    selectedTrainType: String?,
    availableTrainTypes: List<String>,
    onTrainTypeSelected: (String) -> Unit,
    isTimeExistsForDeletion: Boolean
) {
    val context = LocalContext.current
    val displayText = selectedTrainType?.let { getTrainTypeDisplayName(it, context) } ?: stringResource(R.string.dash)
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.selectType),
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val verticalPadding = ScreenSize.customTextFieldPaddingVertical()
            val inputFontSize = ScreenSize.settingsSheetInputFontSize()
            val textStyle = TextStyle(
                fontSize = inputFontSize.value.sp,
                color = White
            )

            BasicTextField(
                value = displayText,
                onValueChange = { },
                enabled = false,
                singleLine = true,
                textStyle = textStyle,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrectEnabled = false
                ),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
                    .heightIn(max = inputFontSize * 2.5f)
            ) { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = displayText,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = {
                        Text(
                            text = "",
                            fontSize = inputFontSize.value.sp,
                            color = Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = null,
                            tint = selectedTrainType?.let { colorForTrainType(it) } ?: White,
                            modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Primary,
                        unfocusedContainerColor = Primary,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedPlaceholderColor = Gray,
                        unfocusedPlaceholderColor = Gray,
                        focusedTrailingIconColor = White,
                        unfocusedTrailingIconColor = White
                    ),
                    contentPadding = PaddingValues(vertical = verticalPadding),
                ) {
                    TextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Primary,
                            unfocusedContainerColor = Primary
                        ),
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    )
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(color = Primary.copy(alpha = 0.85f))
                    .offset(y = ScreenSize.settingsLineSheetTransportationDropdownOffsetY())
            ) {
                availableTrainTypes.forEach { trainType ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = null,
                                    tint = colorForTrainType(trainType),
                                    modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                                )
                                Text(
                                    text = getTrainTypeDisplayName(trainType, context),
                                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = White
                                )
                            }
                        },
                        onClick = {
                            onTrainTypeSelected(trainType)
                            expanded = false
                        }
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(ScreenSize.settingsSheetIconSize()),
            tint = when {
                selectedTrainType != null && !isTimeExistsForDeletion -> Accent
                selectedTrainType != null && isTimeExistsForDeletion -> Primary
                else -> Red
            }
        )
    }
}

// Add and delete buttons with validation
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
    
    val addButtonColor = if (isTimeExistsForDeletion) if (isEnabled) Primary else Gray else Accent
    
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

// Copy time from other hours button
@Composable
private fun CopyTimeButtonSection(
    isCopyTimeDropdownOpen: Boolean,
    onToggleDropdown: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onToggleDropdown,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Unspecified
            ),
            contentPadding = PaddingValues(0.dp),
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
            horizontalArrangement = Arrangement.Center,
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
                color = White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = ScreenSize.settingsSheetIconSpacing())
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
}

// Calendar type selection dropdown dialog
@Composable
private fun CalendarTypeDropdownView(
    availableCalendarTypes: List<ODPTCalendarType>,
    onCalendarTypeSelected: (ODPTCalendarType) -> Unit,
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



// Copy time source selection dropdown dialog
@Composable
private fun CopyTimeDropdownView(
    currentHour: Int,
    onCopyTime: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val startIndex = if (currentHour == 4) 1 else 0
    val items = currentHour.choiceCopyTimeList(context)
    
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
            if (!(currentHour == 25 && index == 1)) {
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

