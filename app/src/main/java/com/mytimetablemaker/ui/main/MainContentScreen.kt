package com.mytimetablemaker.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.models.TransportationLineKind
import com.mytimetablemaker.ui.settings.FirestoreViewModel
import com.mytimetablemaker.ui.theme.Primary
import com.mytimetablemaker.ui.theme.Gray
import com.mytimetablemaker.ui.theme.Accent
import com.mytimetablemaker.ui.common.AdMobBannerView
import android.content.SharedPreferences
import android.content.Context
import android.app.Application
import java.util.*

// MARK: - Main Content Screen
// Primary screen displaying transfer information, timetables, and navigation controls
@Composable
fun MainContentScreen(
    viewModel: MainViewModel? = null,
    firestoreViewModel: FirestoreViewModel? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLineSheet: (String, Int) -> Unit = { _, _ -> },
    onNavigateToTransferSheet: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    // Create ViewModels if not provided
    val mainViewModel = viewModel ?: remember { MainViewModel(application) }
    val firestoreVm = firestoreViewModel ?: remember { FirestoreViewModel(application) }
    
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
    // State for sheet management
    var isShowingLineSheet by remember { mutableStateOf(false) }
    var isShowingTransferSheet by remember { mutableStateOf(false) }
    var sheetGoorback by remember { mutableStateOf<String?>(null) }
    var sheetLineIndex by remember { mutableStateOf<Int?>(null) }
    
    // Use ViewModel properties directly - they are tracked by Compose automatically
    val dateLabel = mainViewModel.dateLabel
    val timeLabel = mainViewModel.timeLabel
    val isTimeStop = mainViewModel.isTimeStop
    val isBack = mainViewModel.isBack
    val goOrBack1 = mainViewModel.goOrBack1
    val goOrBack2 = mainViewModel.goOrBack2
    val changeLine1 = mainViewModel.changeLine1
    val changeLine2 = mainViewModel.changeLine2
    val stationArray1 = mainViewModel.stationArray1
    val stationArray2 = mainViewModel.stationArray2
    val lineNameArray1 = mainViewModel.lineNameArray1
    val lineNameArray2 = mainViewModel.lineNameArray2
    val lineColorArray1 = mainViewModel.lineColorArray1
    val lineColorArray2 = mainViewModel.lineColorArray2
    val transportationArray1 = mainViewModel.transportationArray1
    val transportationArray2 = mainViewModel.transportationArray2
    val transferTimeArray1 = mainViewModel.transferTimeArray1
    val transferTimeArray2 = mainViewModel.transferTimeArray2
    val lineCodeArray1 = mainViewModel.lineCodeArray1
    val lineCodeArray2 = mainViewModel.lineCodeArray2
    val lineKindArray1 = mainViewModel.lineKindArray1
    val lineKindArray2 = mainViewModel.lineKindArray2
    
    // Access mutableStateOf properties directly to ensure recomposition on state changes
    val isShowBackRoute2 = mainViewModel.isShowBackRoute2
    val isShowGoRoute2 = mainViewModel.isShowGoRoute2
    
    // Load Route 2 display settings from SharedPreferences when screen appears
    // This ensures the latest settings are loaded when the screen is displayed
    // Also reload when isBack changes (same as SwiftUI's onChange(of: myTransit.isBack))
    LaunchedEffect(Unit) {
        mainViewModel.setRoute2()
    }
    
    // Reload route 2 settings when isBack changes (same as SwiftUI)
    LaunchedEffect(isBack) {
        mainViewModel.setRoute2()
        // Also update all data when isBack changes (same as SwiftUI's onChange(of: myTransit.isBack))
        mainViewModel.updateAllDataFromUserDefaults()
    }
    
    // Listen to SharedPreferences changes and update data (same as SwiftUI's onReceive(UserDefaults.didChangeNotification))
    // Also listen for SettingsLineUpdated and SettingsTransferUpdated notifications
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            // Update data on any SharedPreferences change (same as UserDefaults.didChangeNotification)
            mainViewModel.updateAllDataFromUserDefaults()
            
            // Also check for specific notification keys (SettingsLineUpdated, SettingsTransferUpdated)
            if (key == "SettingsLineUpdated" || key == "SettingsTransferUpdated") {
                mainViewModel.updateAllDataFromUserDefaults()
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    // Ensure timer is running when MainContentScreen appears (same as SwiftUI's onAppear)
    LaunchedEffect(Unit) {
        if (isTimeStop) {
            mainViewModel.startButton()
        } else {
            mainViewModel.ensureTimerRunning()
        }
    }
    
    // Update dateLabel and timeLabel when selectDate changes (same as SwiftUI's onChange(of: myTransit.selectDate))
    LaunchedEffect(mainViewModel.selectDate) {
        mainViewModel.updateDate(mainViewModel.selectDate)
        if (isTimeStop) {
            mainViewModel.updateTime(mainViewModel.selectDate)
        }
    }
    
    // Computed properties - use directly from ViewModel
    // Calculate isShowRoute2 based on current direction (same as SwiftUI: isBack ? isShowBackRoute2 : isShowGoRoute2)
    // Use remember to ensure recomposition when dependencies change
    val isShowRoute2 = remember(isBack, isShowBackRoute2, isShowGoRoute2) {
        if (isBack) isShowBackRoute2 else isShowGoRoute2
    }
    
    val countdownTime1 = mainViewModel.countdownTime1
    val countdownTime2 = mainViewModel.countdownTime2
    val countdownColor1 = mainViewModel.countdownColor1
    val countdownColor2 = mainViewModel.countdownColor2
    val routeWidth = mainViewModel.isShowRoute2.routeWidth().value
    val timeArrayString1 = mainViewModel.timeArrayString1
    val timeArrayString2 = mainViewModel.timeArrayString2
    
    // Set status bar color to Primary
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            @Suppress("DEPRECATION")
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            // Set status bar content to light (white icons) for dark background
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = false
            }
        }
        onDispose { }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // MARK: - Status Bar Background
        // Fill status bar area with Primary color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Primary)
                .align(Alignment.TopCenter)
        )
        
        // MARK: - Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // MARK: - Header Section
            HeaderSection(
            dateLabel = dateLabel,
            timeLabel = timeLabel,
            isTimeStop = isTimeStop,
            isBack = isBack,
            onDateChange = { date -> mainViewModel.updateDate(date) },
            onTimeChange = { date -> mainViewModel.updateTime(date) },
            onBackClick = { mainViewModel.backButton() },
            onGoClick = { mainViewModel.goButton() },
            onStartClick = { mainViewModel.startButton() },
            onStopClick = { mainViewModel.stopButton() },
            onSettingsClick = onNavigateToSettings
        )
        
        // MARK: - Transfer Information Display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            // First direction route
            RouteColumn(
                goorback = goOrBack1,
                countdownTime = countdownTime1,
                countdownColor = countdownColor1,
                changeLine = changeLine1,
                stationArray = stationArray1,
                lineNameArray = lineNameArray1,
                lineColorArray = lineColorArray1,
                transportationArray = transportationArray1,
                transferTimeArray = transferTimeArray1,
                lineCodeArray = lineCodeArray1,
                lineKindArray = lineKindArray1,
                timeArrayString = timeArrayString1,
                routeWidth = routeWidth,
                sharedPreferences = sharedPreferences,
                context = context,
                mainViewModel = mainViewModel,
                onLineClick = { num ->
                    sheetGoorback = goOrBack1
                    sheetLineIndex = num
                    isShowingLineSheet = true
                },
                onTransferClick = { num ->
                    if (num < 2) {
                        isShowingTransferSheet = true
                    } else {
                        sheetGoorback = goOrBack1
                        sheetLineIndex = maxOf(num - 2, 0)
                        isShowingLineSheet = true
                    }
                }
            )
            
            // Second direction route (if enabled)
            if (isShowRoute2) {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.5.dp),
                    color = Primary
                )
                
                RouteColumn(
                    goorback = goOrBack2,
                    countdownTime = countdownTime2,
                    countdownColor = countdownColor2,
                    changeLine = changeLine2,
                    stationArray = stationArray2,
                    lineNameArray = lineNameArray2,
                    lineColorArray = lineColorArray2,
                    transportationArray = transportationArray2,
                    transferTimeArray = transferTimeArray2,
                    lineCodeArray = lineCodeArray2,
                    lineKindArray = lineKindArray2,
                    timeArrayString = timeArrayString2,
                    routeWidth = routeWidth,
                    sharedPreferences = sharedPreferences,
                    context = context,
                    mainViewModel = mainViewModel,
                    onLineClick = { num ->
                        sheetGoorback = goOrBack2
                        sheetLineIndex = num
                        isShowingLineSheet = true
                    },
                    onTransferClick = { num ->
                        if (num < 2) {
                            isShowingTransferSheet = true
                        } else {
                            sheetGoorback = goOrBack2
                            sheetLineIndex = maxOf(num - 2, 0)
                            isShowingLineSheet = true
                        }
                    }
                )
            }
        }
        
        // MARK: - Ad Banner
        AdBannerSection()
        }
    }
    
    // Handle sheet navigation using LaunchedEffect
    // Navigate to Line Settings Sheet
    LaunchedEffect(isShowingLineSheet, sheetGoorback, sheetLineIndex) {
        if (isShowingLineSheet && sheetGoorback != null && sheetLineIndex != null) {
            onNavigateToLineSheet(sheetGoorback!!, sheetLineIndex!!)
            isShowingLineSheet = false
            sheetGoorback = null
            sheetLineIndex = null
        }
    }
    
    // Navigate to Transfer Settings Sheet
    LaunchedEffect(isShowingTransferSheet) {
        if (isShowingTransferSheet) {
            onNavigateToTransferSheet()
            isShowingTransferSheet = false
        }
    }
}

// MARK: - Header Section
// Displays date, time, and operation buttons
@Composable
private fun HeaderSection(
    dateLabel: String,
    timeLabel: String,
    isTimeStop: Boolean,
    isBack: Boolean,
    onDateChange: (Date) -> Unit,
    onTimeChange: (Date) -> Unit,
    onBackClick: () -> Unit,
    onGoClick: () -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Primary)
            .padding(horizontal = ScreenSize.headerSpace(), vertical = ScreenSize.headerSpace()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ScreenSize.headerSpace())
    ) {
        // Date and Time Display
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.headerDateMargin()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Display
            var showDatePicker by remember { mutableStateOf(false) }
            val calendar = Calendar.getInstance()
            val initialYear = calendar.get(Calendar.YEAR)
            val initialMonth = calendar.get(Calendar.MONTH)
            val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
            
            Box {
                Text(
                    text = dateLabel,
                    fontSize = ScreenSize.headerDateFontSize().value.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    modifier = if (isTimeStop) {
                        Modifier.clickable { showDatePicker = true }
                    } else {
                        Modifier
                    }
                )
                
                // DatePicker overlay (transparent, same as SwiftUI)
                if (isTimeStop) {
                    // Invisible DatePicker button (opacity 0.1, same as SwiftUI)
                    // Use android.app.DatePickerDialog instead
                    Box(
                        modifier = Modifier
                            .alpha(0.1f)
                            .size(ScreenSize.headerDateHeight())
                            .clickable { showDatePicker = true }
                    )
                }
            }
            
            // Show DatePickerDialog when clicked
            LaunchedEffect(showDatePicker) {
                if (showDatePicker) {
                    val datePickerDialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedDate = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth)
                            }
                            onDateChange(selectedDate.time)
                            showDatePicker = false
                        },
                        initialYear,
                        initialMonth,
                        initialDay
                    )
                    datePickerDialog.show()
                }
            }
            
            // Time Display
            if (isTimeStop) {
                var showTimePicker by remember { mutableStateOf(false) }
                val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
                val initialMinute = calendar.get(Calendar.MINUTE)
                
                Box {
                    Text(
                        text = timeLabel,
                        fontSize = ScreenSize.headerDateFontSize().value.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        modifier = Modifier.clickable { showTimePicker = true }
                    )
                    
                    // Invisible TimePicker button (opacity 0.1, same as SwiftUI)
                    Box(
                        modifier = Modifier
                            .alpha(0.1f)
                            .size(ScreenSize.headerDateHeight())
                            .clickable { showTimePicker = true }
                    )
                }
                
                // Show TimePickerDialog when clicked
                LaunchedEffect(showTimePicker) {
                    if (showTimePicker) {
                        val timePickerDialog = android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val newCalendar = Calendar.getInstance().apply {
                                    time = Date()
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                }
                                onTimeChange(newCalendar.time)
                                showTimePicker = false
                            },
                            initialHour,
                            initialMinute,
                            true // 24-hour format
                        )
                        timePickerDialog.show()
                    }
                }
            } else {
                Text(
                    text = timeLabel,
                    fontSize = ScreenSize.headerDateFontSize().value.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
            }
        }
        
        // Operation Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.operationButtonMargin()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            CustomButton(
                title = "Back".localized(context),
                backgroundColor = if (isBack) Accent else Gray,
                onClick = onBackClick
            )
            
            // Go Button
            CustomButton(
                title = "Go".localized(context),
                backgroundColor = if (!isBack) Accent else Gray,
                onClick = onGoClick
            )
            
            // Start Button
            CustomButton(
                title = "Start".localized(context),
                backgroundColor = if (!isTimeStop) Accent else Gray,
                onClick = onStartClick
            )
            
            // Stop Button
            CustomButton(
                title = "Stop".localized(context),
                backgroundColor = if (isTimeStop) Accent else Gray,
                onClick = onStopClick
            )
            
            // Settings Button
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(ScreenSize.headerSettingsButtonSize())
                )
            }
        }
    }
}

// MARK: - Custom Button
// Reusable button component with customizable title and background color
@Composable
private fun CustomButton(
    title: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(ScreenSize.operationButtonWidth())
            .height(ScreenSize.operationButtonHeight()),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        )
    ) {
        Text(
            text = title,
            fontSize = ScreenSize.stationFontSize().value.sp,
            color = Color.White
        )
    }
}

// MARK: - Route Column
// Displays route information for one direction
@Composable
private fun RouteColumn(
    goorback: String,
    countdownTime: String,
    countdownColor: Color,
    changeLine: Int,
    stationArray: List<String>,
    lineNameArray: List<String>,
    lineColorArray: List<Color>,
    transportationArray: List<String>,
    transferTimeArray: List<Int>,
    lineCodeArray: List<String>,
    lineKindArray: List<TransportationLineKind?>,
    timeArrayString: List<String>,
    routeWidth: Float,
    sharedPreferences: SharedPreferences,
    context: Context,
    mainViewModel: MainViewModel,
    onLineClick: (Int) -> Unit,
    onTransferClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .width(routeWidth.dp)
            .padding(horizontal = ScreenSize.routeSidePadding())
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ScreenSize.routeBottomSpace())
    ) {
        Spacer(modifier = Modifier.height(ScreenSize.routeCountdownTopSpace()))
        
        // Countdown Time
        Text(
            text = countdownTime,
            fontSize = ScreenSize.routeCountdownFontSize().value.sp,
            fontWeight = FontWeight.Normal,
            color = countdownColor,
            modifier = Modifier.padding(vertical = ScreenSize.routeCountdownPadding())
        )
        
        // Home/Office View (destination)
        HomeOfficeView(
            goorback = goorback,
            num = 1,
            timeArray = timeArrayString,
            sharedPreferences = sharedPreferences
        )
        
        // Transfer and Station Line Views
        for (num in 0..changeLine) {
            TransferView(
                goorback = goorback,
                num = num + 1,
                transportationArray = transportationArray,
                lineColorArray = lineColorArray,
                lineCodeArray = lineCodeArray,
                lineKindArray = lineKindArray,
                transferTimeArray = transferTimeArray,
                mainViewModel = mainViewModel,
                onTransferClick = { onTransferClick(num + 1) }
            )
            
            StationLineView(
                goorback = goorback,
                num = num,
                stationArray = stationArray,
                lineNameArray = lineNameArray,
                lineColorArray = lineColorArray,
                transportationArray = transportationArray,
                lineCodeArray = lineCodeArray,
                lineKindArray = lineKindArray,
                mainViewModel = mainViewModel,
                onLineClick = { onLineClick(num) }
            )
        }
        
        // Transfer View (arrival)
        TransferView(
            goorback = goorback,
            num = 0,
            transportationArray = transportationArray,
            lineColorArray = lineColorArray,
            lineCodeArray = lineCodeArray,
            lineKindArray = lineKindArray,
            transferTimeArray = transferTimeArray,
            mainViewModel = mainViewModel,
            onTransferClick = { onTransferClick(0) }
        )
        
        // Home/Office View (departure)
        HomeOfficeView(
            goorback = goorback,
            num = 0,
            timeArray = timeArrayString,
            sharedPreferences = sharedPreferences
        )
        
        Spacer(modifier = Modifier.height(ScreenSize.routeCountdownTopSpace()))
    }
}

// MARK: - Home Office View
// Displays station name and departure/arrival time
@Composable
private fun HomeOfficeView(
    goorback: String,
    num: Int,
    timeArray: List<String>,
    sharedPreferences: SharedPreferences
) {
    val time = if (num == 0) timeArray.getOrNull(0) ?: "" else timeArray.getOrNull(1) ?: ""
    val stationName = if (num == 0) {
        goorback.destination(sharedPreferences)
    } else {
        goorback.departurePoint(sharedPreferences)
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stationName,
            fontSize = ScreenSize.stationFontSize().value.sp,
            maxLines = 1
        )
        
        Text(
            text = time,
            fontSize = ScreenSize.timeFontSize().value.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

// MARK: - Transfer View
// Displays transfer icon and handles transfer selection
@Composable
private fun TransferView(
    goorback: String,
    num: Int,
    transportationArray: List<String>,
    lineColorArray: List<Color>,
    lineCodeArray: List<String>,
    lineKindArray: List<TransportationLineKind?>,
    transferTimeArray: List<Int>,
    mainViewModel: MainViewModel,
    onTransferClick: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
    // Calculate timeArray for isDirectConnection check
    val currentDate = mainViewModel.selectDate
    val currentTime = mainViewModel.currentTime
    val timeArray = goorback.timeArray(sharedPreferences, currentDate, currentTime)
    
    // Determine if this is a direct connection (0 minutes transfer with same arrival/departure time)
    val isDirectConnection = transferTimeArray.getOrNull(num) == 0 && num > 0 && run {
        val prevIndex = if (num == 1) 1 else 2 * (num - 2) + 3
        val nextIndex = if (num == 1) 2 else 2 * (num - 2) + 4
        prevIndex < timeArray.size && nextIndex < timeArray.size && 
        timeArray[prevIndex] == timeArray[nextIndex]
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ScreenSize.transferHeight()),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onTransferClick,
            modifier = Modifier.size(ScreenSize.lineImageBackgroundSize()),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LineTimeImage(
                    goorback = goorback,
                    num = num,
                    isTransfer = true,
                    isDirectConnection = isDirectConnection,
                    transportationArray = transportationArray,
                    lineColorArray = lineColorArray,
                    lineCodeArray = lineCodeArray,
                    lineKindArray = lineKindArray
                )
            }
        }
    }
}

// MARK: - Station Line View
// Displays station line information with departure and arrival times
@Composable
private fun StationLineView(
    goorback: String,
    num: Int,
    stationArray: List<String>,
    lineNameArray: List<String>,
    lineColorArray: List<Color>,
    transportationArray: List<String>,
    lineCodeArray: List<String>,
    lineKindArray: List<TransportationLineKind?>,
    mainViewModel: MainViewModel,
    onLineClick: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
    // Calculate timeArray for current date and time
    val currentDate = mainViewModel.selectDate
    val currentTime = mainViewModel.currentTime
    val timeArray = goorback.timeArray(sharedPreferences, currentDate, currentTime).map { it.stringTime() }
    
    val departureTime = timeArray.getOrNull(2 * num + 2) ?: ""
    val arrivalTime = timeArray.getOrNull(2 * num + 3) ?: ""
    val departureStation = getDisplayName(stationArray.getOrNull(2 * num) ?: "")
    val arrivalStation = getDisplayName(stationArray.getOrNull(2 * num + 1) ?: "")
    val lineName = lineNameArray.getOrNull(num) ?: ""
    val lineColor = lineColorArray.getOrNull(num) ?: Accent
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Departure Station and Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = departureStation,
                fontSize = ScreenSize.stationFontSize().value.sp,
                maxLines = 1
            )
            
            Text(
                text = departureTime,
                fontSize = ScreenSize.timeFontSize().value.sp,
                fontWeight = FontWeight.Normal
            )
        }
        
        // Line Name Button
        Button(
            onClick = onLineClick,
            modifier = Modifier
                .height(ScreenSize.lineNameHeight()),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                LineTimeImage(
                    goorback = goorback,
                    num = num,
                    isTransfer = false,
                    isDirectConnection = false,
                    transportationArray = transportationArray,
                    lineColorArray = lineColorArray,
                    lineCodeArray = lineCodeArray,
                    lineKindArray = lineKindArray
                )
                
                Text(
                    text = lineName,
                    fontSize = ScreenSize.lineFontSize().value.sp,
                    color = lineColor,
                    maxLines = 2
                )
            }
        }
        
        // Arrival Station and Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = arrivalStation,
                fontSize = ScreenSize.stationFontSize().value.sp,
                maxLines = 1
            )
            
            Text(
                text = arrivalTime,
                fontSize = ScreenSize.timeFontSize().value.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// MARK: - Line Time Image
// Displays line icon with color and code
@Composable
private fun LineTimeImage(
    goorback: String,
    num: Int,
    isTransfer: Boolean,
    isDirectConnection: Boolean,
    transportationArray: List<String>,
    lineColorArray: List<Color>,
    lineCodeArray: List<String>,
    lineKindArray: List<TransportationLineKind?>
) {
    val lineColor = if (isTransfer) {
        Gray
    } else {
        // Check if lineColorArray is empty or num is out of bounds
        if (lineColorArray.isEmpty() || num >= lineColorArray.size) {
            Accent
        } else {
            val color = lineColorArray[num]
            // Check if color is Unspecified, Transparent, or White, then use Accent
            when {
                color == Color.Unspecified || color == Color.Transparent || color == Color.White -> Accent
                else -> color
            }
        }
    }
    val lineCode = if (isTransfer) "" else lineCodeArray.getOrNull(num) ?: ""
    val transportation = if (isTransfer) transportationArray.getOrNull(num) ?: "" else ""
    val transportationKind = if (isTransfer) null else lineKindArray.getOrNull(num)
    
    // Determine icon based on transfer type and transportation kind
    val icon = when {
        !isTransfer && transportationKind == TransportationLineKind.BUS -> Icons.Filled.DirectionsBus
        !isTransfer -> Icons.Filled.Train
        isDirectConnection -> Icons.Filled.KeyboardArrowDown
        transportation.isNotEmpty() -> transferTypeIcon(transportation)
        else -> Icons.AutoMirrored.Filled.DirectionsWalk
    }
    
    // ZStack equivalent: Rectangle (background), Image (icon), Text (lineCode)
    Box(
        modifier = Modifier
            .size(ScreenSize.lineImageBackgroundSize())
            .background(lineColor),
        contentAlignment = Alignment.Center
    ) {
        // Icon layer (same as SwiftUI's Image)
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ScreenSize.lineImageForegroundSize()),
            tint = Color.White
        )
        
        // Line code text with shadow effect (overlay on icon)
        // SwiftUI uses 8 shadows (radius: 0, offset: ±0.5) for better visibility
        if (lineCode.isNotEmpty()) {
            val shadowOffset = 0.5.dp
            val shadowColor = Color.Gray.copy(alpha = 0.5f)
            
            // Draw shadow layers first (behind main text)
            // Shadow layer 1: (0.5, 0)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = shadowColor,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = shadowOffset, y = 0.dp)
            )
            // Shadow layer 2: (-0.5, 0)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = shadowColor,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = -shadowOffset, y = 0.dp)
            )
            // Shadow layer 3: (0, 0.5)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = shadowColor,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 0.dp, y = shadowOffset)
            )
            // Shadow layer 4: (0, -0.5)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = shadowColor,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 0.dp, y = -shadowOffset)
            )
            // Shadow layer 5: (0.5, 0.5)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = shadowColor,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = shadowOffset, y = shadowOffset)
            )
            // Shadow layer 6: (0.5, -0.5)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = shadowColor,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = shadowOffset, y = -shadowOffset)
            )
            // Shadow layer 7: (-0.5, 0.5)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = shadowColor,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = -shadowOffset, y = shadowOffset)
            )
            // Shadow layer 8: (-0.5, -0.5)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = shadowColor,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = -shadowOffset, y = -shadowOffset)
            )
            
            // Main text (on top of shadows, drawn last)
            Text(
                text = lineCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// MARK: - Helper Functions
// Get display name (split by ":" and return first component for ODPT format)
private fun getDisplayName(name: String): String {
    val components = name.split(":")
    return components.firstOrNull()?.trim() ?: name
}

// Get transfer type icon
@Composable
private fun transferTypeIcon(transportation: String): androidx.compose.ui.graphics.vector.ImageVector {
    val context = LocalContext.current
    val transportationLower = transportation.lowercase()
    return when (transportationLower) {
        "walking", "walking".localized(context) -> Icons.AutoMirrored.Filled.DirectionsWalk
        "bicycle", "bicycle".localized(context) -> Icons.AutoMirrored.Filled.DirectionsBike
        "car", "car".localized(context) -> Icons.Filled.DirectionsCar
        else -> Icons.AutoMirrored.Filled.DirectionsWalk
    }
}

// MARK: - Ad Banner Section
// Displays advertisement banner
@Composable
private fun AdBannerSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ScreenSize.admobBannerHeight())
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        AdMobBannerView()
    }
}

