package com.mytimetablemaker.ui.main

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Train
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.models.TransportationLineKind
import com.mytimetablemaker.models.TransferType
import com.mytimetablemaker.ui.settings.FirestoreViewModel
import com.mytimetablemaker.ui.theme.*
import com.mytimetablemaker.ui.common.AdMobBannerView
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.R
import java.util.Calendar
import java.util.Date
import android.content.SharedPreferences
import android.content.Context
import android.app.Application
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val dateToDisplay = mainViewModel.displayDate ?: mainViewModel.selectDate
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
    // Read route2 display settings directly from SharedPreferences
    var isShowRoute2 by remember { mutableStateOf(false) }
    
    // Load Route 2 display settings from SharedPreferences when screen appears
    LaunchedEffect(Unit) {
        isShowRoute2 = if (isBack) {
            "back2".isShowRoute2(sharedPreferences)
        } else {
            "go2".isShowRoute2(sharedPreferences)
        }
    }
    
    // Reload route 2 settings when isBack changes
    LaunchedEffect(isBack) {
        isShowRoute2 = if (isBack) {
            "back2".isShowRoute2(sharedPreferences)
        } else {
            "go2".isShowRoute2(sharedPreferences)
        }
        // Also update all data when isBack changes (same as SwiftUI's onChange(of: myTransit.isBack))
        mainViewModel.updateAllDataFromUserDefaults()
    }
    
    // Listen to SharedPreferences changes and update data (same as SwiftUI's onReceive(UserDefaults.didChangeNotification))
    // Also listen for SettingsLineUpdated and SettingsTransferUpdated notifications
    DisposableEffect(isBack) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            // Update data on any SharedPreferences change (same as UserDefaults.didChangeNotification)
            mainViewModel.updateAllDataFromUserDefaults()
            
            // Also check for specific notification keys (SettingsLineUpdated, SettingsTransferUpdated)
            if (key == "SettingsLineUpdated" || key == "SettingsTransferUpdated") {
                mainViewModel.updateAllDataFromUserDefaults()
            }
            
            // Update isShowRoute2 when route2 flag changes
            if (key == "back2".isShowRoute2Key() || key == "go2".isShowRoute2Key()) {
                isShowRoute2 = if (isBack) {
                    "back2".isShowRoute2(sharedPreferences)
                } else {
                    "go2".isShowRoute2(sharedPreferences)
                }
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    // Ensure timer is running when MainContentScreen appears
    DisposableEffect(Unit) {
        if (isTimeStop) {
            mainViewModel.startButton()
        }
        onDispose { }
    }

    // Update dateLabel and timeLabel when selectDate changes (same as SwiftUI's onChange(of: myTransit.selectDate))
    LaunchedEffect(mainViewModel.selectDate) {
        mainViewModel.updateDate(mainViewModel.selectDate)
        if (isTimeStop) {
            mainViewModel.updateTime(mainViewModel.selectDate)
        }
    }
    
    val countdownTime1 = mainViewModel.countdownTime1
    val countdownTime2 = mainViewModel.countdownTime2
    val countdownColor1 = mainViewModel.countdownColor1
    val countdownColor2 = mainViewModel.countdownColor2
    val timeArrayString1 = mainViewModel.timeArrayString1
    val timeArrayString2 = mainViewModel.timeArrayString2

    // Set status bar icons appearance (light/dark)
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            // Set status bar content to light (white icons) for dark background (Primary color)
            controller.isAppearanceLightStatusBars = false
        }
        onDispose { }
    }
    
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Primary)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(Primary)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
        ) {
        // MARK: - Main Content
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // MARK: - Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Date and Time Display
                val context = LocalContext.current
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
                            text = dateToDisplay.formatDate(context),
                            fontSize = ScreenSize.headerDateFontSize().value.sp,
                            fontWeight = FontWeight.Normal,
                            color = White,
                            modifier = if (isTimeStop) {
                                Modifier.clickable { showDatePicker = true }
                            } else {
                                Modifier
                            }
                        )
                        
                        if (isTimeStop) {
                            Box(
                                modifier = Modifier
                                    .alpha(0.1f)
                                    .size(ScreenSize.headerDateHeight())
                                    .clickable { showDatePicker = true }
                            )
                        }
                    }
                    
                    LaunchedEffect(showDatePicker) {
                        if (showDatePicker) {
                            val datePickerDialog = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedDate = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }
                                    mainViewModel.updateDate(selectedDate.time)
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
                                color = White,
                                modifier = Modifier.clickable { showTimePicker = true }
                            )
                            
                            Box(
                                modifier = Modifier
                                    .alpha(0.1f)
                                    .size(ScreenSize.headerDateHeight())
                                    .clickable { showTimePicker = true }
                            )
                        }
                        
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
                                        mainViewModel.updateTime(newCalendar.time)
                                        showTimePicker = false
                                    },
                                    initialHour,
                                    initialMinute,
                                    true
                                )
                                timePickerDialog.show()
                            }
                        }
                    } else {
                        DisposableEffect(Unit) {
                            mainViewModel.ensureTimerRunning()
                            onDispose {
                                mainViewModel.stopTimerOnDisappear()
                            }
                        }
                        
                        Text(
                            text = timeLabel,
                            fontSize = ScreenSize.headerDateFontSize().value.sp,
                            fontWeight = FontWeight.Normal,
                            color = White
                        )
                    }
                }
                
                // Operation Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ScreenSize.operationButtonMargin()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CommonComponents.CustomButton(
                        title = stringResource(R.string.back),
                        backgroundColor = if (isBack) Accent else Gray,
                        modifier = Modifier
                            .width(ScreenSize.operationButtonWidth())
                            .height(ScreenSize.operationButtonHeight()),
                        onClick = { mainViewModel.backButton() }
                    )
                    
                    CommonComponents.CustomButton(
                        title = stringResource(R.string.go),
                        backgroundColor = if (!isBack) Accent else Gray,
                        modifier = Modifier
                            .width(ScreenSize.operationButtonWidth())
                            .height(ScreenSize.operationButtonHeight()),
                        onClick = { mainViewModel.goButton() }
                    )
                    
                    CommonComponents.CustomButton(
                        title = stringResource(R.string.start),
                        backgroundColor = if (!isTimeStop) Accent else Gray,
                        modifier = Modifier
                            .width(ScreenSize.operationButtonWidth())
                            .height(ScreenSize.operationButtonHeight()),
                        onClick = { mainViewModel.startButton() }
                    )
                    
                    CommonComponents.CustomButton(
                        title = stringResource(R.string.stop),
                        backgroundColor = if (isTimeStop) Accent else Gray,
                        modifier = Modifier
                            .width(ScreenSize.operationButtonWidth())
                            .height(ScreenSize.operationButtonHeight()),
                        onClick = { mainViewModel.stopButton() }
                    )
                    
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = White,
                            modifier = Modifier.size(ScreenSize.headerSettingsButtonSize())
                        )
                    }
                }
            }
        
            // MARK: - Transfer Information Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                if (ScreenSize.screenWidth().value > 600) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(ScreenSize.routeSidePadding()))

                // First direction route
                Column(
                    modifier = Modifier
                        .width(ScreenSize.routeWidth(isShowRoute2))
                        .padding(horizontal = ScreenSize.routeSidePadding())
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ScreenSize.routeBottomSpace())
                ) {

                    Spacer(modifier = Modifier.height(ScreenSize.routeCountdownTopSpace()))

                    // Countdown Time
                    Text(
                        text = countdownTime1,
                        fontSize = ScreenSize.routeCountdownFontSize().value.sp,
                        fontWeight = FontWeight.Medium,
                        color = countdownColor1,
                    )

                    Spacer(modifier = Modifier.height(ScreenSize.routeCountdownSpace()))

                    // Home/Office View (destination)
                    HomeOfficeView(
                        goorback = goOrBack1,
                        num = 1,
                        timeArray = timeArrayString1,
                        sharedPreferences = sharedPreferences
                    )

                    // Transfer and Station Line Views
                    for (num in 0..changeLine1) {
                        TransferView(
                            goorback = goOrBack1,
                            num = num + 1,
                            transportationArray = transportationArray1,
                            lineColorArray = lineColorArray1,
                            lineCodeArray = lineCodeArray1,
                            lineKindArray = lineKindArray1,
                            transferTimeArray = transferTimeArray1,
                            mainViewModel = mainViewModel,
                            onTransferClick = {
                                if (num + 1 < 2) {
                                    isShowingTransferSheet = true
                                } else {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        sheetGoorback = goOrBack1
                                        sheetLineIndex = maxOf((num + 1) - 2, 0)
                                        delay(10)
                                        isShowingLineSheet = true
                                    }
                                }
                            }
                        )

                        StationLineView(
                            goorback = goOrBack1,
                            num = num,
                            stationArray = stationArray1,
                            lineNameArray = lineNameArray1,
                            lineColorArray = lineColorArray1,
                            transportationArray = transportationArray1,
                            lineCodeArray = lineCodeArray1,
                            lineKindArray = lineKindArray1,
                            mainViewModel = mainViewModel,
                            onLineClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    sheetGoorback = goOrBack1
                                    sheetLineIndex = maxOf((num + 2) - 2, 0)
                                    delay(10)
                                    isShowingLineSheet = true
                                }
                            }
                        )
                    }

                    // Transfer View (arrival)
                    TransferView(
                        goorback = goOrBack1,
                        num = 0,
                        transportationArray = transportationArray1,
                        lineColorArray = lineColorArray1,
                        lineCodeArray = lineCodeArray1,
                        lineKindArray = lineKindArray1,
                        transferTimeArray = transferTimeArray1,
                        mainViewModel = mainViewModel,
                        onTransferClick = {
                            isShowingTransferSheet = true
                        }
                    )

                    // Home/Office View (departure)
                    HomeOfficeView(
                        goorback = goOrBack1,
                        num = 0,
                        timeArray = timeArrayString1,
                        sharedPreferences = sharedPreferences
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }

                // Second direction route (if enabled)
                if (isShowRoute2) {

                    Spacer(modifier = Modifier.width(ScreenSize.dividerSidePadding()))

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(ScreenSize.dividerWidth()),
                        color = Primary,
                    )

                    Spacer(modifier = Modifier.width(ScreenSize.dividerSidePadding()))

                    Column(
                        modifier = Modifier
                            .width(ScreenSize.routeWidth(isShowRoute2))
                            .padding(horizontal = ScreenSize.routeSidePadding())
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ScreenSize.routeBottomSpace())
                    ) {

                        Spacer(modifier = Modifier.height(ScreenSize.routeCountdownTopSpace()))

                        // Countdown Time
                        Text(
                            text = countdownTime2,
                            fontSize = ScreenSize.routeCountdownFontSize().value.sp,
                            fontWeight = FontWeight.Medium,
                            color = countdownColor2,
                        )

                        Spacer(modifier = Modifier.height(ScreenSize.routeCountdownSpace()))

                        // Home/Office View (destination)
                        HomeOfficeView(
                            goorback = goOrBack2,
                            num = 1,
                            timeArray = timeArrayString2,
                            sharedPreferences = sharedPreferences
                        )

                        // Transfer and Station Line Views
                        for (num in 0..changeLine2) {
                            TransferView(
                                goorback = goOrBack2,
                                num = num + 1,
                                transportationArray = transportationArray2,
                                lineColorArray = lineColorArray2,
                                lineCodeArray = lineCodeArray2,
                                lineKindArray = lineKindArray2,
                                transferTimeArray = transferTimeArray2,
                                mainViewModel = mainViewModel,
                                onTransferClick = {
                                    if (num + 1 < 2) {
                                        isShowingTransferSheet = true
                                    } else {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            sheetGoorback = goOrBack2
                                            sheetLineIndex = maxOf((num + 1) - 2, 0)
                                            delay(10)
                                            isShowingLineSheet = true
                                        }
                                    }
                                }
                            )

                            StationLineView(
                                goorback = goOrBack2,
                                num = num,
                                stationArray = stationArray2,
                                lineNameArray = lineNameArray2,
                                lineColorArray = lineColorArray2,
                                transportationArray = transportationArray2,
                                lineCodeArray = lineCodeArray2,
                                lineKindArray = lineKindArray2,
                                mainViewModel = mainViewModel,
                                onLineClick = {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        sheetGoorback = goOrBack2
                                        sheetLineIndex = maxOf((num + 2) - 2, 0)
                                        delay(10)
                                        isShowingLineSheet = true
                                    }
                                }
                            )
                        }

                        // Transfer View (arrival)
                        TransferView(
                            goorback = goOrBack2,
                            num = 0,
                            transportationArray = transportationArray2,
                            lineColorArray = lineColorArray2,
                            lineCodeArray = lineCodeArray2,
                            lineKindArray = lineKindArray2,
                            transferTimeArray = transferTimeArray2,
                            mainViewModel = mainViewModel,
                            onTransferClick = {
                                isShowingTransferSheet = true
                            }
                        )

                        // Home/Office View (departure)
                        HomeOfficeView(
                            goorback = goOrBack2,
                            num = 0,
                            timeArray = timeArrayString2,
                            sharedPreferences = sharedPreferences
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.width(ScreenSize.routeSidePadding()))

                if (ScreenSize.screenWidth().value > 600) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // MARK: - Ad Banner (画面最下部)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                AdMobBannerView(
                    modifier = Modifier
                        .width(ScreenSize.admobBannerWidth())
                        .height(ScreenSize.admobBannerHeight())
                        .then(Modifier.widthIn(min = ScreenSize.admobBannerMinWidth()))
                )
            }
        }
    }
    }
    
    // Handle sheet navigation using LaunchedEffect
    // Navigate to Line Settings Sheet
    LaunchedEffect(isShowingLineSheet, sheetGoorback, sheetLineIndex) {
        if (isShowingLineSheet && sheetGoorback != null && sheetLineIndex != null) {
            val currentGoorback = sheetGoorback!!
            val currentLineIndex = sheetLineIndex!!
            onNavigateToLineSheet(currentGoorback, currentLineIndex)
            isShowingLineSheet = false
        }
    }
    
    // Reset navigation parameters after sheet is dismissed
    // This LaunchedEffect reads the reset values, preventing the warning
    LaunchedEffect(isShowingLineSheet) {
        if (!isShowingLineSheet) {
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


// MARK: - Home Office View
// Displays station name and departure/arrival time
@Composable
private fun HomeOfficeView(
    goorback: String,
    num: Int,
    timeArray: List<String>,
    sharedPreferences: SharedPreferences
) {
    val context = LocalContext.current
    val time = if (num == 0) timeArray.getOrNull(0) ?: "" else timeArray.getOrNull(1) ?: ""
    val stationName = if (num == 0) {
        goorback.destination(sharedPreferences, context)
    } else {
        goorback.departurePoint(sharedPreferences, context)
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stationName,
            fontSize = ScreenSize.stationFontSize().value.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = time,
            fontSize = ScreenSize.timeFontSize().value.sp,
            fontWeight = FontWeight.Medium,
            color = Primary,
            maxLines = 1
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
    val context = mainViewModel.getApplication<Application>()
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
            shape = RoundedCornerShape(0.dp),
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
    val context = mainViewModel.getApplication<Application>()
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
    // Calculate timeArray for current date and time
    val currentDate = mainViewModel.selectDate
    val currentTime = mainViewModel.currentTime
    val timeArray = goorback.timeArray(sharedPreferences, currentDate, currentTime).map { it.stringTime }
    
    val departureTime = timeArray.getOrNull(2 * num + 2) ?: ""
    val arrivalTime = timeArray.getOrNull(2 * num + 3) ?: ""
    val departureStation = (stationArray.getOrNull(2 * num) ?: "").getDisplayName()
    val arrivalStation = (stationArray.getOrNull(2 * num + 1) ?: "").getDisplayName()
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
                fontWeight = FontWeight.Medium,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = departureTime,
                fontSize = ScreenSize.timeFontSize().value.sp,
                fontWeight = FontWeight.Medium,
                color = Primary,
                maxLines = 1
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
            ),
            shape = RoundedCornerShape(0.dp)
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
                
                Spacer(modifier = Modifier.width(ScreenSize.lineImageTextSpacing()))

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
                fontWeight = FontWeight.Medium,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = arrivalTime,
                fontSize = ScreenSize.timeFontSize().value.sp,
                fontWeight = FontWeight.Medium,
                color = Primary,
                maxLines = 1
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
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
    val lineColor = if (isTransfer) {
        Gray
    } else {
        // Check if lineColorArray is empty or num is out of bounds
        if (lineColorArray.isEmpty() || num >= lineColorArray.size) {
            // Fallback to SharedPreferences if array is empty
            goorback.lineColor(sharedPreferences, num)
        } else {
            val color = lineColorArray[num]
            // Check if color is Unspecified, Transparent, White, or has invalid alpha, then use fallback
            when {
                color == Color.Unspecified || 
                color == Color.Transparent || 
                color == White ||
                color.alpha == 0f -> goorback.lineColor(sharedPreferences, num)
                else -> color
            }
        }
    }
    val lineCode = if (isTransfer) {
        ""
    } else {
        lineCodeArray.getOrNull(num) ?: goorback.lineCode(sharedPreferences, num)
    }
    val transportation = if (isTransfer) {
        transportationArray.getOrNull(num) ?: ""
    } else {
        ""
    }
    val transportationKind = if (isTransfer) {
        null
    } else {
        lineKindArray.getOrNull(num) ?: goorback.lineKind(sharedPreferences, num)
    }
    
    // Determine icon based on transfer type and transportation kind
    val icon = when {
        !isTransfer && transportationKind == TransportationLineKind.BUS -> Icons.Filled.DirectionsBus
        !isTransfer -> Icons.Filled.Train
        isDirectConnection -> Icons.Filled.KeyboardDoubleArrowDown
        transportation.isNotEmpty() ->  TransferType.transferType(transportation, context).icon
        else -> Icons.Filled.KeyboardDoubleArrowDown
    }
    
    // ZStack equivalent: Rectangle (background), Image (icon), Text (lineCode)
    Box(
        modifier = Modifier
            .size(ScreenSize.lineImageBackgroundSize())
            .aspectRatio(1f)
            .background(lineColor, RoundedCornerShape(0.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Icon layer (same as SwiftUI's Image)
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ScreenSize.lineImageForegroundSize()),
            tint = White
        )
        
        // Line code text with shadow effect (overlay on icon)
        // SwiftUI uses 8 shadows (radius: 0, offset: ±0.5) for better visibility
        if (lineCode.isNotEmpty()) {
            val shadowOffset = ScreenSize.shadowOffset()
            val shadowColor = Gray.copy(alpha = 0.5f)
            val lineCodeFontSize = ScreenSize.stationFontSize().value.sp
            
            // Draw shadow layers first (behind main text)
            // Shadow layer 1: (0.5, 0)
            Text(
                text = lineCode,
                fontSize = lineCodeFontSize,
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
                fontSize = lineCodeFontSize,
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
                fontSize = lineCodeFontSize,
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
                fontSize = lineCodeFontSize,
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
                fontSize = lineCodeFontSize,
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
                fontSize = lineCodeFontSize,
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
                fontSize = lineCodeFontSize,
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
                fontSize = lineCodeFontSize,
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
                fontSize = lineCodeFontSize,
                fontWeight = FontWeight.Bold,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
// MARK: - Helper Functions
// Get display name (split by ":" and return first component for ODPT format)



