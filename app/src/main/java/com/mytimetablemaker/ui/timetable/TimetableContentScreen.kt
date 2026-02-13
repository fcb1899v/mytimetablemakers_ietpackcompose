package com.mytimetablemaker.ui.timetable

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

// Main timetable editing screen with calendar type selection and hourly grid view
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableContentScreen(
    goorback: String,
    num: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
    // Calendar type and available types
    var selectedCalendarType by remember { mutableStateOf<ODPTCalendarType>(ODPTCalendarType.Weekday) }
    var availableCalendarTypes by remember { mutableStateOf<List<ODPTCalendarType>>(emptyList()) }
    var isCalendarTypeDropdownOpen by remember { mutableStateOf(false) }
    
    // Timetable sheet state (hour-based editing)
    var showingTimetableSheetHour by remember { mutableStateOf<Int?>(null) }
    var validHours by remember { mutableStateOf<List<Int>>(emptyList()) }
    
    // Refresh trigger for grid update after sheet dismissal
    var timetableRefreshTrigger by remember { mutableIntStateOf(0) }
    
    // Update valid hours for current or specified calendar type
    fun updateValidHours() {
        validHours = goorback.validHourRange(selectedCalendarType, num, sharedPreferences)
    }

    // Update valid hours for specified calendar type (for dropdown selection)
    fun updateValidHours(newCalendarType: ODPTCalendarType) {
        validHours = goorback.validHourRange(newCalendarType, num, sharedPreferences)
    }
    
    // Sort calendar types: weekday, holiday, saturday/sunday, weekdays, then specific types alphabetically
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
    
    // Rebuild train type list from timetable data if not exists
    fun rebuildTrainTypeListIfNeeded() {
        // Check if train type list already exists
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
    
    // Initialize calendar types and valid hours on first load
    LaunchedEffect(Unit) {
        // Load available calendar types (already full rawValue format)
        val loadedTypesString = goorback.loadAvailableCalendarTypes(sharedPreferences, num)
        val loadedTypes = loadedTypesString.mapNotNull { typeString ->
            ODPTCalendarType.fromRawValue(typeString)
        }
        
        // Sort and set selected calendar type based on current date
        availableCalendarTypes = sortCalendarTypesByEnumOrder(loadedTypes)
        val currentDate = Date()
        selectedCalendarType = currentDate.odptCalendarType(fallbackTo = availableCalendarTypes)
        
        // If no data for selected type, try other available types
        if (!goorback.hasTimetableDataForType(selectedCalendarType, num, sharedPreferences)) {
            for (calendarType in availableCalendarTypes) {
                if (goorback.hasTimetableDataForType(calendarType, num, sharedPreferences)) {
                    selectedCalendarType = calendarType
                    break
                }
            }
        }
        
        updateValidHours()
        rebuildTrainTypeListIfNeeded()
    }
    
    // Update when calendar type changes
    LaunchedEffect(selectedCalendarType) {
        updateValidHours()
        rebuildTrainTypeListIfNeeded()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.timetableRidingTime),
                        fontSize = ScreenSize.settingsTitleFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                },
                navigationIcon = {
                    CommonComponents.CustomBackButton(
                        onClick = onNavigateBack,
                        foregroundColor = White
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
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(ScreenSize.timetableVerticalSpacing())
            ) {
                // Operator & line name display
                Text(
                    text = "${goorback.operatorNameArray(sharedPreferences)[num]} : ${goorback.lineNameArray(sharedPreferences, context)[num]}",
                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White,
                    modifier = Modifier.padding(start = ScreenSize.timetableHorizontalSpacing())
                )
                
                val stationArray = goorback.stationArray(sharedPreferences, context)
                Text(
                    text = "${stationArray[2 * num].getDisplayName()}${stringResource(R.string.toSpace)}${stationArray[2 * num + 1].getDisplayName()}",
                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White,
                    modifier = Modifier.padding(start = ScreenSize.timetableHorizontalSpacing())
                )
                
                // Timetable grid with hourly columns
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Top border
                    Box(
                        modifier = Modifier
                            .width(ScreenSize.customWidth())
                            .height(ScreenSize.borderWidth())
                            .background(White)
                    )
                    
                    // Calendar type selector
                    Row(
                        modifier = Modifier
                            .width(ScreenSize.customWidth())
                            .height(ScreenSize.timetableGridHeaderHeight())
                            .background(Black.copy(alpha = 0.5f))
                            .clickable(
                                role = androidx.compose.ui.semantics.Role.Button,
                                onClick = { isCalendarTypeDropdownOpen = !isCalendarTypeDropdownOpen }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(ScreenSize.borderWidth())
                                .fillMaxHeight()
                                .background(White)
                        )

                        Spacer(modifier = Modifier.weight(1f))

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

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .width(ScreenSize.borderWidth())
                                .fillMaxHeight()
                                .background(White)
                        )
                    }
                    
                    // Bottom border
                    Box(
                        modifier = Modifier
                            .width(ScreenSize.customWidth())
                            .height(ScreenSize.borderWidth())
                            .background(White)
                    )
                    
                    // Timetable grid scroll view (timetableRefreshTrigger forces refresh when sheet is dismissed)
                    key(selectedCalendarType.rawValue, timetableRefreshTrigger) {
                        Column(
                            modifier = Modifier
                                .width(ScreenSize.customWidth())
                                .heightIn(max = ScreenSize.timetableMaxHeight())
                                .verticalScroll(rememberScrollState())
                                .background(Black.copy(alpha = 0.25f)),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            validHours.forEach { hour ->
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

                                Box(
                                    modifier = Modifier
                                        .width(ScreenSize.customWidth())
                                        .height(ScreenSize.borderWidth())
                                        .background(White)
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
                        updateValidHours(calendarType)
                        rebuildTrainTypeListIfNeeded()
                        isCalendarTypeDropdownOpen = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = ScreenSize.timetableContentViewMenuOffsetY())
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
                updateValidHours()
                timetableRefreshTrigger++
            }
        )
    }
}

// Individual grid cell for editing timetable times at specified hour
@Composable
private fun TimetableGridView(
    goorback: String,
    num: Int,
    hour: Int,
    selectedCalendarType: ODPTCalendarType,
    sharedPreferences: SharedPreferences,
    onShowSheet: (Int) -> Unit
) {
    val transportationTimes = goorback.loadTransportationTimes(
        selectedCalendarType,
        num,
        hour,
        sharedPreferences
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Hour display box
        Box(
            modifier = Modifier
                .width(ScreenSize.timetableHourFrameWidth())
                .height(ScreenSize.calculateContentHeight(transportationTimes.size))
                .background(Black.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(ScreenSize.borderWidth())
                        .fillMaxHeight()
                        .background(White)
                )

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

                Box(
                    modifier = Modifier
                        .width(ScreenSize.borderWidth())
                        .fillMaxHeight()
                        .background(White)
                )
            }
        }
        
        // Time edit button with grid content
        Button(
            onClick = { onShowSheet(hour) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Unspecified
            ),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(0.dp),
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
                .width(ScreenSize.borderWidth())
                .height(ScreenSize.calculateContentHeight(transportationTimes.size))
                .background(White)
        )
    }
}

// Train times display grid with 10 columns per row
@Composable
private fun TimetableGridContent(
    transportationTimes: List<TransportationTime>,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = ScreenSize.timetableGridContentPaddingHorizontal()
    val topPadding = ScreenSize.timetableGridContentPaddingTop()
    val bottomPadding = ScreenSize.timetableGridContentPaddingBottom()
    val contentWidth = ScreenSize.timetableMinuteFrameWidth() - horizontalPadding * 2
    val itemsPerRow = 10
    val itemWidth = contentWidth.value / itemsPerRow

    LazyVerticalGrid(
        columns = GridCells.Fixed(itemsPerRow),
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = topPadding,
            bottom = bottomPadding
        ),
        modifier = modifier
            .width(ScreenSize.timetableMinuteFrameWidth())
            .fillMaxSize(),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "(${transportationTime.rideTime})",
                    fontSize = ScreenSize.timetableRideTimeFontSize().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Dropdown menu for selecting calendar type (weekday/holiday/etc.)
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
            .border(ScreenSize.borderWidth(), Gray),
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

// Displays color legend for train types (horizontal flow layout, wraps at customWidth)
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
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenSize.timetableHorizontalSpacing()),
        contentAlignment = Alignment.Center
    ) {
        // FlowRow: each color group (bullet + label) is one block; wrap by block when width is exceeded
        FlowRow(
            modifier = Modifier.widthIn(max = ScreenSize.customWidth()),
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.timetableHorizontalSpacing(), Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(ScreenSize.timetableVerticalSpacing(), Alignment.CenterVertically),
            maxItemsInEachRow = Int.MAX_VALUE
        ) {
            colorGroups.forEach { (color, types) ->
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight(),
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
                        getTrainTypeDisplayName(trainType, context)
                    }.distinct()
                    val separator = if (Locale.getDefault().language == "ja") "・" else ", "
                    val combinedText = displayTexts.joinToString(separator)
                    Text(
                        text = combinedText,
                        fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// Button to trigger timetable generation from API when no data exists
@Composable
private fun RegisterTimetableButton(
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
            textColor = White,
            onClick = { onShowSheet(18) }
        )
    }
}
