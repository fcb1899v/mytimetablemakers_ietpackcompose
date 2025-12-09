package com.mytimetablemaker.ui.timetable

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.models.*
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.theme.*
import java.util.*

// MARK: - Timetable Content Screen
// Main timetable editing screen with grid view
// Matches SwiftUI TimetableContentView structure
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableContentScreen(
    goorback: String,
    num: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    val density = LocalDensity.current
    
    // Selected calendar type for filtering timetable data
    var selectedCalendarType by remember { mutableStateOf<ODPTCalendarType>(ODPTCalendarType.Weekday) }
    
    // Available calendar types loaded from cache
    var availableCalendarTypes by remember { mutableStateOf<List<ODPTCalendarType>>(emptyList()) }
    
    // Controls visibility of calendar type dropdown menu
    var isCalendarTypeDropdownOpen by remember { mutableStateOf(false) }
    
    // State for timetable grid views (hour-based sheet presentation)
    // Store the hour that should show the sheet, or null if no sheet should be shown
    var showingTimetableSheetHour by remember { mutableStateOf<Int?>(null) }
    
    // Valid hours for current calendar type
    var validHours by remember { mutableStateOf<List<Int>>(emptyList()) }
    
    // Scroll view height for dynamic sizing
    var scrollViewHeight by remember { mutableStateOf(0.dp) }
    
    // MARK: - Helper Functions
    // Get display name (split by ":" and return first component for ODPT format)
    fun getDisplayName(name: String): String {
        val components = name.split(":")
        return components.firstOrNull()?.trim() ?: name
    }
    
    // Update valid hours for current calendar type
    fun updateValidHours() {
        validHours = goorback.validHourRange(selectedCalendarType, num, sharedPreferences)
    }
    
    // Sort calendar types according to enum definition order
    fun sortCalendarTypesByEnumOrder(types: List<ODPTCalendarType>): List<ODPTCalendarType> {
        val enumOrder = listOf(
            ODPTCalendarType.Weekday,
            ODPTCalendarType.Holiday,
            ODPTCalendarType.SaturdayHoliday,
            ODPTCalendarType.Sunday,
            ODPTCalendarType.Monday,
            ODPTCalendarType.Tuesday,
            ODPTCalendarType.Wednesday,
            ODPTCalendarType.Thursday,
            ODPTCalendarType.Friday,
            ODPTCalendarType.Saturday
        )
        
        // Separate specific types and regular types
        val specificTypes = types.filterIsInstance<ODPTCalendarType.Specific>()
        val regularTypes = types.filter { it !is ODPTCalendarType.Specific }
        
        // Sort regular types by enum order
        val sortedRegularTypes = regularTypes.sortedBy { type ->
            val index = enumOrder.indexOf(type)
            if (index >= 0) index else Int.MAX_VALUE
        }
        
        // Append specific types at the end (sorted alphabetically by rawValue)
        val sortedSpecificTypes = specificTypes.sortedBy { it.rawValue }
        
        return sortedRegularTypes + sortedSpecificTypes
    }
    
    // Rebuild train type list from existing timetable data if not exists
    fun rebuildTrainTypeListIfNeeded() {
        val existingTrainTypes = goorback.loadTrainTypeList(selectedCalendarType, num, sharedPreferences)
        if (existingTrainTypes.isNotEmpty()) {
            return
        }
        
        val validHoursList = goorback.validHourRange(selectedCalendarType, num, sharedPreferences)
        val allTransportationTimes = mutableListOf<TransportationTime>()
        
        // Collect all transportation times from all valid hours
        for (hour in validHoursList) {
            val times = goorback.loadTransportationTimes(selectedCalendarType, num, hour, sharedPreferences)
            allTransportationTimes.addAll(times)
        }
        
        // Save train type list if we have data
        if (allTransportationTimes.isNotEmpty()) {
            goorback.saveTrainTypeList(allTransportationTimes, selectedCalendarType, num, sharedPreferences)
        }
    }
    
    // MARK: - Initialization
    LaunchedEffect(Unit) {
        // Load available calendar types from cache or use default
        val loadedTypesString = goorback.loadAvailableCalendarTypes(sharedPreferences, num)
        val loadedTypes = loadedTypesString.mapNotNull { typeString ->
            ODPTCalendarType.fromRawValue("odpt.Calendar:$typeString")
        }
        
        // Sort calendar types according to enum definition order
        availableCalendarTypes = sortCalendarTypesByEnumOrder(loadedTypes)
        
        // Set selectedCalendarType based on current date with fallback to available types
        val currentDate = Date()
        val currentCalendarTypeString = currentDate.odptCalendarType(availableCalendarTypes.map { it.rawValue })
        val currentCalendarType = ODPTCalendarType.fromRawValue("odpt.Calendar:$currentCalendarTypeString")
        selectedCalendarType = currentCalendarType ?: availableCalendarTypes.firstOrNull() ?: ODPTCalendarType.Weekday
        
        // Check if data exists for the selected calendar type, if not, try other available types
        if (!goorback.hasTimetableDataForType(selectedCalendarType, num, sharedPreferences)) {
            for (calendarType in availableCalendarTypes) {
                if (goorback.hasTimetableDataForType(calendarType, num, sharedPreferences)) {
                    selectedCalendarType = calendarType
                    break
                }
            }
        }
        
        // Initialize valid hours
        updateValidHours()
        
        // Rebuild train type list from existing data if not exists
        rebuildTrainTypeListIfNeeded()
    }
    
    // Update valid hours when calendar type changes
    LaunchedEffect(selectedCalendarType) {
        updateValidHours()
        rebuildTrainTypeListIfNeeded()
    }
    
    // MARK: - Main Content
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.timetableRidingTime),
                        fontSize = ScreenSize.settingsTitleFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    CommonComponents.CustomBackButton(
                        onClick = onNavigateBack,
                        foregroundColor = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Primary)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ScreenSize.timetableVerticalSpacing())
            ) {
                // MARK: - Header Section
                // Operator & Line Name Display
                Text(
                    text = "${goorback.operatorNameArray(sharedPreferences)[num]} : ${goorback.lineNameArray(sharedPreferences)[num]}",
                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(start = ScreenSize.timetableHorizontalSpacing())
                )
                
                val stationArray = goorback.stationArray(sharedPreferences)
                Text(
                    text = "${getDisplayName(stationArray[2 * num])}${stringResource(R.string.toSpace)}${getDisplayName(stationArray[2 * num + 1])}",
                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(start = ScreenSize.timetableHorizontalSpacing())
                )
                
                // MARK: - Timetable Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Top border
                    Box(
                        modifier = Modifier
                            .width(ScreenSize.customWidth().dp)
                            .height(1.dp)
                            .background(Color.White)
                    )
                    
                    // Calendar type selector
                    Row(
                        modifier = Modifier
                            .width(ScreenSize.customWidth().dp)
                            .height(ScreenSize.timetableGridHeight())
                            .background(Color.Black.copy(alpha = 0.5f)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = { isCalendarTypeDropdownOpen = !isCalendarTypeDropdownOpen },
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCalendarType.displayName(context),
                                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = selectedCalendarType.calendarColor
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ScreenSize.settingsSheetTitleFontSize())
                                        .graphicsLayer {
                                            rotationZ = if (isCalendarTypeDropdownOpen) 180f else 0f
                                        },
                                    tint = selectedCalendarType.calendarColor
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                    
                    // Bottom border
                    Box(
                        modifier = Modifier
                            .width(ScreenSize.customWidth().dp)
                            .height(1.dp)
                            .background(Color.White)
                    )
                    
                    // Timetable grid scroll view
                    LazyColumn(
                        modifier = Modifier
                            .width(ScreenSize.customWidth().dp)
                            .height(
                                if (scrollViewHeight > 0.dp) {
                                    minOf(scrollViewHeight, ScreenSize.timetableMaxHeight())
                                } else {
                                    ScreenSize.timetableMaxHeight()
                                }
                            ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(validHours.size) { index ->
                            val hour = validHours[index]
                            TimetableGridView(
                                goorback = goorback,
                                num = num,
                                hour = hour,
                                selectedCalendarType = selectedCalendarType,
                                sharedPreferences = sharedPreferences,
                                onShowSheet = { hourValue ->
                                    showingTimetableSheetHour = hourValue
                                }
                            )
                            
                            // Divider
                            if (index < validHours.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(ScreenSize.customWidth().dp)
                                        .height(1.dp)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                    
                    // Show color legend only when there are 2 or more train types
                    val trainTypes = goorback.loadTrainTypeList(selectedCalendarType, num, sharedPreferences)
                    if (trainTypes.size > 1) {
                        ColorLegendView(
                            trainTypes = trainTypes,
                            modifier = Modifier.padding(top = ScreenSize.timetableVerticalSpacing())
                        )
                    }
                    
                    // Check if timetable data exists
                    if (validHours.isEmpty()) {
                        // Show register button when no timetable data exists
                        RegisterTimetableButton(
                            goorback = goorback,
                            num = num,
                            hour = 18,
                            selectedCalendarType = selectedCalendarType,
                            onShowSheet = { hourValue ->
                                showingTimetableSheetHour = hourValue
                            }
                        )
                    }
                }
            }
            
            // Dropdown options overlay
            if (isCalendarTypeDropdownOpen) {
                CalendarTypeDropdownView(
                    availableCalendarTypes = availableCalendarTypes,
                    onCalendarTypeSelected = { calendarType ->
                        selectedCalendarType = calendarType
                        isCalendarTypeDropdownOpen = false
                        updateValidHours()
                        rebuildTrainTypeListIfNeeded()
                    },
                    onDismiss = { isCalendarTypeDropdownOpen = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = ScreenSize.timetableTypeMenuOffsetX(),
                            y = ScreenSize.timetableContentViewMenuOffsetY()
                        )
                        .zIndex(1f)
                )
            }
        }
    }
    
    // Show timetable sheet for the selected hour
    showingTimetableSheetHour?.let { hour ->
        SettingsTimetableSheetScreen(
            goorback = goorback,
            selectedCalendarType = selectedCalendarType,
            num = num,
            hour = hour,
            onDismiss = {
                showingTimetableSheetHour = null
            }
        )
    }
}

// MARK: - Timetable Grid View
// Individual grid cell for editing timetable times
@Composable
private fun TimetableGridView(
    goorback: String,
    num: Int,
    hour: Int,
    selectedCalendarType: ODPTCalendarType,
    sharedPreferences: SharedPreferences,
    onShowSheet: (Int) -> Unit
) {
    val context = LocalContext.current
    
    // Load transportation times for this hour
    val transportationTimes = goorback.loadTransportationTimes(
        selectedCalendarType,
        num,
        hour,
        sharedPreferences
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // MARK: - Hour Display
        Box(
            modifier = Modifier
                .width(ScreenSize.timetableHourFrameWidth())
                .height(ScreenSize.calculateContentHeight(transportationTimes.size))
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = hour.addZeroTime(),
                    fontSize = ScreenSize.timetableHourFontSize().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentSize(Alignment.Center)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
        }
        
        // MARK: - Time Edit Button
        Button(
            onClick = { onShowSheet(hour) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Unspecified
            ),
            modifier = Modifier
                .width(ScreenSize.timetableMinuteFrameWidth())
                .height(ScreenSize.calculateContentHeight(transportationTimes.size))
        ) {
            TimetableGridContent(
                transportationTimes = transportationTimes,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(ScreenSize.calculateContentHeight(transportationTimes.size))
                .background(Color.White)
        )
    }
}

// MARK: - Grid Content View
// Train times display grid with proper wrapping
@Composable
private fun TimetableGridContent(
    transportationTimes: List<TransportationTime>,
    modifier: Modifier = Modifier
) {
    // Calculate grid layout: 10 items per row with fixed width
    val availableWidth = ScreenSize.timetableMinuteFrameWidth().value - (ScreenSize.timetableMinuteSpacing().value * 2)
    val itemsPerRow = 10
    val itemWidth = availableWidth / itemsPerRow
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(itemsPerRow),
        modifier = modifier
            .width(ScreenSize.timetableMinuteFrameWidth())
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
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

// MARK: - Calendar Type Dropdown View
// Dropdown menu for selecting calendar type
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
                color = Color.White,
                shape = RoundedCornerShape(4.dp)
            )
            .border(1.dp, Gray, RoundedCornerShape(4.dp)),
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
                    modifier = Modifier.height(1.dp),
                    color = Gray
                )
            }
        }
    }
}

// MARK: - Color Legend View
// Displays color legend for train types
@Composable
private fun ColorLegendView(
    trainTypes: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Filter out empty train types
    val validTrainTypes = trainTypes.filter { it.isNotEmpty() }
    
    // Group train types by color
    val groupedByColor = validTrainTypes.groupBy { trainType ->
        colorForTrainType(trainType)
    }
    
    // Convert to array of (color, trainTypes) tuples
    val colorGroups = groupedByColor.map { (color, types) ->
        Pair(color, types)
    }.sortedBy { (color, _) ->
        color.priorityValue
    }
    
    // TODO: Implement row-based layout for color legend
    // For now, display in a simple column
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenSize.timetableHorizontalSpacing()),
        verticalArrangement = Arrangement.spacedBy(ScreenSize.timetableVerticalSpacing()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        colorGroups.forEach { (color, types) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(ScreenSize.timetableHorizontalSpacing()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize()),
                    tint = color
                )
                
                val displayTexts = types.map { trainType ->
                    val components = trainType.split(".")
                    components.lastOrNull() ?: trainType
                }
                
                val separator = if (java.util.Locale.getDefault().language == "ja") "・" else ", "
                val combinedText = displayTexts.joinToString(separator)
                
                Text(
                    text = combinedText,
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
}

// MARK: - Register Timetable Button
// Button to register timetable when no data exists
@Composable
private fun RegisterTimetableButton(
    goorback: String,
    num: Int,
    hour: Int,
    selectedCalendarType: ODPTCalendarType,
    onShowSheet: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ScreenSize.timetableVerticalSpacing()),
        horizontalArrangement = Arrangement.Center
    ) {
        CommonComponents.CustomButton(
            title = stringResource(R.string.registerTimetable),
            icon = Icons.Default.Add,
            backgroundColor = Accent,
            textColor = Color.White,
            onClick = { onShowSheet(hour) }
        )
    }
}

