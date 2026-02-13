package com.mytimetablemaker.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.mytimetablemaker.R
import com.mytimetablemaker.models.*
import com.mytimetablemaker.ui.theme.*
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.ui.common.CommonComponents
import android.content.Context
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.core.graphics.toColorInt
import androidx.compose.runtime.collectAsState

// Line settings sheet for route configuration.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLineSheetScreen(
    goorback: String,
    lineIndex: Int,
    onNavigateBack: () -> Unit,
    onNavigateToTimetableSettings: (String, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
    // Validate route key and fall back to back1.
    val validGoorback = if (goorback.isEmpty() || !goorbackList.contains(goorback)) "back1" else goorback
    
    // Create ViewModel.
    val viewModel = remember(validGoorback, lineIndex) {
        SettingsLineViewModel(application, sharedPreferences, validGoorback, lineIndex)
    }
    
    // Local UI state.
    var showTimetableSettings by remember { mutableStateOf(false) }
    
    // Focus requesters for text fields.
    val operatorFocusRequester = remember { FocusRequester() }
    val lineFocusRequester = remember { FocusRequester() }
    val departureFocusRequester = remember { FocusRequester() }
    val arrivalFocusRequester = remember { FocusRequester() }
    
    // ViewModel state.
    val operatorInput by viewModel.operatorInput.collectAsState()
    val operatorSuggestions by viewModel.operatorSuggestions.collectAsState()
    val showOperatorSuggestions by viewModel.showOperatorSuggestions.collectAsState()
    val operatorSelected by viewModel.operatorSelected.collectAsState()
    val selectedOperatorCode by viewModel.selectedOperatorCode.collectAsState()
    val isOperatorFieldFocused by viewModel.isOperatorFieldFocused.collectAsState()
    val isLineNumberChanging by viewModel.isLineNumberChanging.collectAsState()
    
    val lineInput by viewModel.lineInput.collectAsState()
    val lineSuggestions by viewModel.lineSuggestionsState.collectAsState()
    val showLineSuggestions by viewModel.showLineSuggestions.collectAsState()
    val lineSelected by viewModel.lineSelected.collectAsState()
    val isLineFieldFocused by viewModel.isLineFieldFocused.collectAsState()
    
    val selectedLineColor by viewModel.selectedLineColor.collectAsState()
    val showColorSelection by viewModel.showColorSelection.collectAsState()
    val selectedLine by viewModel.selectedLineState.collectAsState()
    
    val departureStopInput by viewModel.departureStopInput.collectAsState()
    val arrivalStopInput by viewModel.arrivalStopInput.collectAsState()
    val showDepartureSuggestions by viewModel.showDepartureSuggestions.collectAsState()
    val departureSuggestions by viewModel.departureSuggestions.collectAsState()
    val isDepartureFieldFocused by viewModel.isDepartureFieldFocused.collectAsState()
    val showArrivalSuggestions by viewModel.showArrivalSuggestions.collectAsState()
    val arrivalSuggestions by viewModel.arrivalSuggestions.collectAsState()
    val isArrivalFieldFocused by viewModel.isArrivalFieldFocused.collectAsState()
    
    val selectedRideTime by viewModel.selectedRideTimeState.collectAsState()
    val selectedTransportation by viewModel.selectedTransportation.collectAsState()
    val selectedTransferTime by viewModel.selectedTransferTime.collectAsState()
    val selectedLineNumber by viewModel.selectedLineNumberState.collectAsState()
    val selectedGoorback by viewModel.selectedGoorbackState.collectAsState()
    val selectedTransportationKind by viewModel.selectedTransportationKind.collectAsState()
    
    val isLoadingBusStops by viewModel.isLoadingBusStops.collectAsState()
    val isLoadingTimetable = viewModel.isLoadingTimetable
    val isLoadingLines by viewModel.isLoadingLines.collectAsState()
    val loadingMessage = viewModel.loadingMessage
    
    val selectedDepartureStop by viewModel.selectedDepartureStopState.collectAsState()
    val selectedArrivalStop by viewModel.selectedArrivalStopState.collectAsState()
    
    // Track form completeness.
    val isAllNotEmpty by remember(operatorInput, lineInput, departureStopInput, arrivalStopInput, selectedRideTime) {
        derivedStateOf { viewModel.isAllNotEmpty }
    }
    
    val isAllSelected by remember(operatorSelected, selectedLine, selectedDepartureStop, selectedArrivalStop) {
        derivedStateOf {
            val result = operatorSelected &&
            selectedLine != null &&
            selectedDepartureStop != null &&
            selectedArrivalStop != null
            result
        }
    }
    
    // Status bar setup.
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
    
    val horizontalPadding = ScreenSize.settingsSheetHorizontalPadding()
    val verticalSpacing = ScreenSize.settingsSheetVerticalSpacing()
    val titleFontSize = ScreenSize.settingsTitleFontSize()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(ScreenSize.settingsSheetTopBarHeight())
                        .background(White)
                ) {
                    // Back button.
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = ScreenSize.settingsSheetBackButtonPadding())
                    ) {
                        CommonComponents.CustomBackButton(
                            onClick = {
                                if (!isLoadingBusStops && !isLoadingTimetable && !isLoadingLines) {
                                    onNavigateBack()
                                }
                            },
                            foregroundColor = if (isLoadingBusStops || isLoadingTimetable || isLoadingLines) White else Black
                        )
                    }
                    
                    // Title.
                    Text(
                        text = stringResource(R.string.routeSettings),
                        fontSize = titleFontSize.value.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    .padding(horizontal = horizontalPadding)
            ) {
                // Route header menu.
                RouteHeaderMenu(
                    viewModel = viewModel,
                    selectedGoorback = selectedGoorback,
                    onClear = {
                        operatorFocusRequester.requestFocus()
                        viewModel.isOperatorFieldFocused.value = true
                        viewModel.clearAllFormData()
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.filterOperators("", isFocused = true)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Line number menu.
                LineNumberMenu(
                    viewModel = viewModel,
                    selectedLineNumber = selectedLineNumber,
                    selectedTransportationKind = selectedTransportationKind,
                    onTransportationKindChanged = { isRailway ->
                        clearAllFocus(
                            operatorFocusRequester,
                            lineFocusRequester,
                            departureFocusRequester,
                            arrivalFocusRequester,
                            viewModel
                        )
                        viewModel.switchTransportationKind(isRailway)
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Operator input.
                OperatorInputSection(
                    operatorInput = operatorInput,
                    isOperatorSelected = selectedOperatorCode != null,
                    isOperatorFieldFocused = isOperatorFieldFocused,
                    focusRequester = operatorFocusRequester,
                    onOperatorInputChanged = { newValue ->
                        // Update input and refresh suggestions.
                        viewModel.operatorInput.value = newValue
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.processOperatorInput(newValue)
                            if (viewModel.showOperatorSuggestions.value && viewModel.operatorSuggestions.value.isNotEmpty()) {
                                operatorFocusRequester.requestFocus()
                            }
                        }
                    },
                    onFocusChanged = { isFocused ->
                        viewModel.isOperatorFieldFocused.value = isFocused
                        if (isFocused) {
                            // Reset flags so suggestions can show.
                            viewModel.operatorSelected.value = false
                            viewModel.isLineNumberChanging.value = false
                            viewModel.isGoOrBackChanging.value = false
                            
                            if (operatorInput.isEmpty()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.filterOperators("")
                                    operatorFocusRequester.requestFocus()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.filterOperators(operatorInput)
                                    operatorFocusRequester.requestFocus()
                                }
                            }
                        } else {
                            if (viewModel.showOperatorSuggestions.value && viewModel.operatorSuggestions.value.isNotEmpty()) {
                                operatorFocusRequester.requestFocus()
                            } else {
                                viewModel.showOperatorSuggestions.value = false
                            }
                        }
                    },
                    showOperatorSuggestions = showOperatorSuggestions && !isLineNumberChanging && !operatorSelected && isOperatorFieldFocused,
                    operatorSuggestions = operatorSuggestions
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Line input.
                LineInputSection(
                    lineInput = lineInput,
                    selectedTransportationKind = selectedTransportationKind,
                    isLineSelected = selectedLine != null,
                    focusRequester = lineFocusRequester,
                    onLineInputChanged = { newValue ->
                        viewModel.processLineInput(newValue)
                    },
                    onFocusChanged = { isFocused ->
                        viewModel.isLineFieldFocused.value = isFocused
                        if (isFocused) {
                            // Reset flags so suggestions can show.
                            viewModel.isLineNumberChanging.value = false
                            viewModel.isGoOrBackChanging.value = false
                            viewModel.lineSelected.value = false
                            
                            if (viewModel.selectedOperatorCode.value != null && viewModel.operatorSelected.value && lineInput.isEmpty()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.filterLine("", isFocused = true)
                                }
                            }
                        } else {
                            viewModel.showLineSuggestions.value = false
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Line color.
                LineColorSection(
                    viewModel = viewModel,
                    selectedLineColor = selectedLineColor,
                    selectedLine = selectedLine,
                    onColorSelectClick = {
                        viewModel.showColorSelection.value = true
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Station header.
                StationHeaderText(
                    viewModel = viewModel,
                    selectedTransportationKind = selectedTransportationKind
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Departure stop input.
                DepartureStopInputSection(
                    departureStopInput = departureStopInput,
                    selectedTransportationKind = selectedTransportationKind,
                    isDepartureSelected = selectedDepartureStop != null,
                    focusRequester = departureFocusRequester,
                    onDepartureStopInputChanged = { newValue ->
                        viewModel.processDepartureStopInput(newValue)
                    },
                    onFocusChanged = { isFocused ->
                        viewModel.isDepartureFieldFocused.value = isFocused
                        if (isFocused && lineSelected) {
                            viewModel.departureStopSelected.value = false
                            viewModel.filterDepartureStops(departureStopInput)
                        } else if (!isFocused) {
                            viewModel.showDepartureSuggestions.value = false
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Arrival stop input.
                ArrivalStopInputSection(
                    arrivalStopInput = arrivalStopInput,
                    selectedTransportationKind = selectedTransportationKind,
                    isArrivalSelected = selectedArrivalStop != null,
                    focusRequester = arrivalFocusRequester,
                    onArrivalStopInputChanged = { newValue ->
                        viewModel.processArrivalStopInput(newValue)
                    },
                    onFocusChanged = { isFocused ->
                        viewModel.isArrivalFieldFocused.value = isFocused
                        if (isFocused && lineSelected) {
                            viewModel.arrivalStopSelected.value = false
                            viewModel.filterArrivalStops(arrivalStopInput)
                        } else if (!isFocused) {
                            viewModel.showArrivalSuggestions.value = false
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Time header.
                TimeHeaderText()
                
                // Ride time.
                RideTimeSection(
                    viewModel = viewModel,
                    selectedRideTime = selectedRideTime,
                    isAllSelected = isAllSelected,
                    onRideTimeChanged = { newValue ->
                        viewModel.setSelectedRideTime(newValue)
                    }
                )

                // Transportation settings for lines 1 and 2.
                if (selectedLineNumber < 3) {
                    TransportationSettingsSection(
                        viewModel = viewModel,
                        selectedTransportation = selectedTransportation,
                        onTransportationChanged = { newTransportation ->
                            viewModel.selectedTransportation.value = newTransportation
                        }
                    )

                    // Transfer time settings when transportation is set.
                    if (selectedTransportation != "none") {
                        TransferTimeSettingsSection(
                            viewModel = viewModel,
                            selectedTransferTime = selectedTransferTime,
                            isAllSelected = isAllSelected,
                            onTransferTimeChanged = { newValue ->
                                viewModel.selectedTransferTime.value = newValue
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(verticalSpacing))
                    }
                }
                
                // Save action.
                SaveButtonSection(
                    isAllNotEmpty = isAllNotEmpty,
                    isAllSelected = isAllSelected,
                    onSave = {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.handleLineSave()
                            onNavigateBack()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Timetable settings action.
                TimetableSettingsButtonSection(
                    isAllNotEmpty = isAllNotEmpty,
                    isAllSelected = isAllSelected,
                    onTimetableSettings = {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.handleLineSave()
                            showTimetableSettings = true
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Auto-generate timetable action.
                TimetableAutoSettingsButtonSection(
                    isAllSelected = isAllSelected,
                    onAutoGenerate = {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                viewModel.handleLineSave()
                                viewModel.autoGenerateTimetable()
                                delay(100)
                                showTimetableSettings = true
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsLineSheetScreen", "Failed to auto-generate timetable", e)
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
            }
            
            // Operator suggestions overlay.
            if (showOperatorSuggestions && operatorSuggestions.isNotEmpty() && !isLineNumberChanging && !operatorSelected && isOperatorFieldFocused) {
                OperatorSuggestionsView(
                    operatorSuggestions = operatorSuggestions,
                    onOperatorSelected = { operatorName ->
                        viewModel.operatorInput.value = operatorName
                        viewModel.operatorSelected.value = true
                        viewModel.showOperatorSuggestions.value = false
                        viewModel.operatorSuggestions.value = emptyList()
                        
                        // Find operator code from operator name.
                        val transportationKind = when (selectedTransportationKind) {
                            TransportationLineKind.RAILWAY -> TransportationKind.RAILWAY
                            TransportationLineKind.BUS -> TransportationKind.BUS
                        }
                        val dataSource = LocalDataSource.entries.firstOrNull {
                            it.transportationType() == transportationKind &&
                            it.operatorDisplayName(viewModel.getApplication()) == operatorName
                        }
                        if (dataSource != null) {
                            val operatorCode = dataSource.operatorCode()
                            val previousOperatorCode = viewModel.selectedOperatorCode.value
                            
                            if (previousOperatorCode != operatorCode) {
                                viewModel.isChangedOperator.value = true
                            }
                            
                            viewModel.selectedOperatorCode.value = operatorCode
                            
                            viewModel.lineInput.value = ""
                            viewModel.lineSelected.value = false
                            
                            viewModel.isLineNumberChanging.value = false
                            
                            viewModel.isLineFieldFocused.value = true
                            
                            if (dataSource.apiType() == ODPTAPIType.GTFS) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        viewModel.fetchGTFSLinesForOperator(dataSource)
                                    } catch (e: Exception) {
                                        android.util.Log.e("SettingsLineSheetScreen", "Failed to fetch GTFS lines for ${dataSource.name}", e)
                                    }
                                }
                            } else {
                                viewModel.processLineInput("")
                            }
                            
                            lineFocusRequester.requestFocus()
                        }
                    },
                    onDismissRequest = {
                        viewModel.showOperatorSuggestions.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = ScreenSize.settingsLineSheetOperatorOffset())
                        .zIndex(100f)
                )
            }
            
            // Line suggestions overlay.
            if (showLineSuggestions && lineSuggestions.isNotEmpty() && !isLineNumberChanging && !lineSelected && isLineFieldFocused && operatorSelected) {
                LineSuggestionsView(
                    viewModel = viewModel,
                    lineSuggestions = lineSuggestions,
                    onLineSelected = { line ->
                        viewModel.selectLine(line)
                        viewModel.showLineSuggestions.value = false
                        viewModel.lineSelected.value = true
                        
                        // Show color selection or move to departure input.
                        if (line.lineColor == null) {
                            viewModel.showColorSelection.value = true
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(100)
                                departureFocusRequester.requestFocus()
                                viewModel.isDepartureFieldFocused.value = true
                            }
                        }
                    },
                    onDismissRequest = {
                        viewModel.showLineSuggestions.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = ScreenSize.settingsLineSheetLineOffset())
                        .zIndex(100f)
                )
            }
            
            // Departure stop suggestions overlay.
            if (showDepartureSuggestions && departureSuggestions.isNotEmpty() && lineSelected && isDepartureFieldFocused) {
                DepartureStopSuggestionsView(
                    departureSuggestions = departureSuggestions,
                    onStopSelected = { stop ->
                        viewModel.isLineNumberChanging.value = true
                        val arrivalDisplayName = viewModel.selectedArrivalStopState.value?.displayName() ?: ""
                        val isSameAsArrival = arrivalDisplayName == stop.displayName()
                        viewModel.departureStopInput.value = if (isSameAsArrival) "" else stop.displayName()
                        viewModel.setSelectedDepartureStop(if (isSameAsArrival) null else stop)
                        viewModel.showDepartureSuggestions.value = false
                        viewModel.isDepartureFieldFocused.value = false
                        viewModel.departureSuggestions.value = emptyList()
                        viewModel.departureStopSelected.value = true
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100)
                            arrivalFocusRequester.requestFocus()
                            viewModel.isArrivalFieldFocused.value = true
                            viewModel.isLineNumberChanging.value = false
                        }
                    },
                    onDismissRequest = {
                        viewModel.showDepartureSuggestions.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = ScreenSize.settingsLineSheetDepartureOffset())
                        .zIndex(100f)
                )
            }
            
            // Arrival stop suggestions overlay.
            if (showArrivalSuggestions && arrivalSuggestions.isNotEmpty() && lineSelected && isArrivalFieldFocused) {
                ArrivalStopSuggestionsView(
                    arrivalSuggestions = arrivalSuggestions,
                    onStopSelected = { stop ->
                        viewModel.isLineNumberChanging.value = true
                        val departureDisplayName = viewModel.selectedDepartureStopState.value?.displayName() ?: ""
                        val isSameAsDeparture = departureDisplayName == stop.displayName()
                        viewModel.arrivalStopInput.value = if (isSameAsDeparture) "" else stop.displayName()
                        viewModel.setSelectedArrivalStop(if (isSameAsDeparture) null else stop)
                        viewModel.showArrivalSuggestions.value = false
                        viewModel.isArrivalFieldFocused.value = false
                        viewModel.arrivalSuggestions.value = emptyList()
                        viewModel.arrivalStopSelected.value = true
                        
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100)
                            if (departureStopInput.isEmpty()) {
                                departureFocusRequester.requestFocus()
                                viewModel.isDepartureFieldFocused.value = true
                            } else if (viewModel.selectedLineNumber < 3) {
                                clearAllFocus(
                                    operatorFocusRequester,
                                    lineFocusRequester,
                                    departureFocusRequester,
                                    arrivalFocusRequester,
                                    viewModel
                                )
                            }
                            viewModel.isLineNumberChanging.value = false
                        }
                    },
                    onDismissRequest = {
                        viewModel.showArrivalSuggestions.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = ScreenSize.settingsLineSheetArrivalOffset())
                        .zIndex(100f)
                )
            }
            
            // Color selection overlay.
            if (showColorSelection || (lineInput.isNotEmpty() && selectedLineColor == null && selectedLine?.lineColor == null && !lineSelected)) {
                ColorSelectionSection(
                    onColorSelected = { color ->
                        viewModel.setLineColor(color)
                        viewModel.showColorSelection.value = false
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100)
                            departureFocusRequester.requestFocus()
                            viewModel.isDepartureFieldFocused.value = true
                        }
                    },
                    onCancel = {
                        viewModel.showColorSelection.value = false
                    }
                )
            }
            
            // Loading overlay removed from here (moved outside Scaffold)
        }
        }
        
        // Loading overlay - outside Scaffold to overlay TopBar/BottomBar
        if (isLoadingBusStops || isLoadingTimetable || isLoadingLines) {
            CommonComponents.CustomProgressIndicator(
                text = loadingMessage
            )
        }
    }
    
    // Navigate to timetable settings.
    LaunchedEffect(showTimetableSettings) {
        if (showTimetableSettings) {
            onNavigateToTimetableSettings(validGoorback, lineIndex)
            showTimetableSettings = false
        }
    }
    
    // Initialize on appear.
    LaunchedEffect(Unit) {
        viewModel.isChangedOperator.value = false
        if (viewModel.selectedGoorback != validGoorback) {
            viewModel.selectGoorback(validGoorback)
        }
    }
    
    // Clear focus when color selection opens.
    LaunchedEffect(showColorSelection) {
        if (showColorSelection) {
            clearAllFocus(
                operatorFocusRequester,
                lineFocusRequester,
                departureFocusRequester,
                arrivalFocusRequester,
                viewModel
            )
        }
    }
}

// Clear all focus from text fields.
private fun clearAllFocus(
    operatorFocusRequester: FocusRequester,
    lineFocusRequester: FocusRequester,
    departureFocusRequester: FocusRequester,
    arrivalFocusRequester: FocusRequester,
    viewModel: SettingsLineViewModel
) {
    operatorFocusRequester.freeFocus()
    lineFocusRequester.freeFocus()
    departureFocusRequester.freeFocus()
    arrivalFocusRequester.freeFocus()
    viewModel.isOperatorFieldFocused.value = false
    viewModel.isLineFieldFocused.value = false
    viewModel.isDepartureFieldFocused.value = false
    viewModel.isArrivalFieldFocused.value = false
}

// Route header with direction dropdown and clear action.
@Composable
private fun RouteHeaderMenu(
    viewModel: SettingsLineViewModel,
    selectedGoorback: String,
    onClear: () -> Unit
) {
    val goorbackDisplayNames by remember { derivedStateOf { viewModel.goorbackDisplayNames } }
    val verticalSpacing = ScreenSize.settingsSheetVerticalSpacing()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = verticalSpacing),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Direction dropdown.
        var expanded by remember { mutableStateOf(false) }
        
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goorbackDisplayNames[selectedGoorback] ?: selectedGoorback,
                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                Spacer(modifier = Modifier.width(ScreenSize.settingsSheetHorizontalSpacing()))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Black,
                    modifier = Modifier.size(ScreenSize.settingsSheetTitleFontSize())
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                goorbackList.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = goorbackDisplayNames[option] ?: option,
                                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
                            )
                        },
                        onClick = {
                            viewModel.selectGoorback(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Clear Button
        CommonComponents.CustomRectangleButton(
            title = stringResource(R.string.clear),
            icon = Icons.Default.Cancel,
            tintColor = Red,
            onClick = onClear
        )
    }
}

// Line number menu with transportation kind toggle.
@Composable
private fun LineNumberMenu(
    viewModel: SettingsLineViewModel,
    selectedLineNumber: Int,
    selectedTransportationKind: TransportationLineKind,
    onTransportationKindChanged: (Boolean) -> Unit
) {
    val availableLineNumbers by viewModel.availableLineNumbers.collectAsState()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Line number dropdown.
        var expanded by remember { mutableStateOf(false) }
        
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.line)}${selectedLineNumber}",
                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                Spacer(modifier = Modifier.width(ScreenSize.settingsSheetHorizontalSpacing()))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Black,
                    modifier = Modifier.size(ScreenSize.settingsSheetTitleFontSize())
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableLineNumbers.forEach { lineNumber ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${stringResource(R.string.line)}${lineNumber}",
                                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
                            )
                        },
                        onClick = {
                            viewModel.selectLineNumber(lineNumber)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Transportation kind toggle.
        CommonComponents.CustomToggle(
            isLeftSelected = selectedTransportationKind == TransportationLineKind.RAILWAY,
            onToggle = onTransportationKindChanged,
            leftText = stringResource(R.string.railway),
            rightText = stringResource(R.string.bus),
        )
    }
}

// Operator input section.
@Composable
private fun OperatorInputSection(
    operatorInput: String,
    isOperatorSelected: Boolean,
    isOperatorFieldFocused: Boolean,
    focusRequester: FocusRequester,
    onOperatorInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    showOperatorSuggestions: Boolean,
    operatorSuggestions: List<String>
) {
    // Keep focus when suggestions are visible.
    LaunchedEffect(showOperatorSuggestions) {
        if (showOperatorSuggestions && operatorSuggestions.isNotEmpty()) {
            if (!isOperatorFieldFocused) {
                focusRequester.requestFocus()
            }
        }
    }
    
    CommonComponents.CustomTextField(
        value = operatorInput,
        onValueChange = onOperatorInputChanged,
        placeholder = stringResource(R.string.enterOperatorName),
        modifier = Modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        title = stringResource(R.string.operatorName),
        isCheckmarkValid = operatorInput.isNotEmpty(),
        isCheckmarkSelected = isOperatorSelected,
        onFocusChanged = onFocusChanged
    )
}

// Line input section.
@Composable
private fun LineInputSection(
    lineInput: String,
    selectedTransportationKind: TransportationLineKind,
    isLineSelected: Boolean,
    focusRequester: FocusRequester,
    onLineInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    CommonComponents.CustomTextField(
        value = lineInput,
        onValueChange = onLineInputChanged,
        placeholder =  stringResource(if (selectedTransportationKind == TransportationLineKind.RAILWAY)  R.string.enterLineName else R.string.enterBusRouteName),
        modifier = Modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        title = stringResource(R.string.lineName),
        isCheckmarkValid = lineInput.isNotEmpty(),
        isCheckmarkSelected = isLineSelected,
        onFocusChanged = onFocusChanged
    )
}

// Line color selection.
@Composable
private fun LineColorSection(
    viewModel: SettingsLineViewModel,
    selectedLineColor: String?,
    selectedLine: TransportationLine?,
    onColorSelectClick: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember(context) {
        context.getSharedPreferences("SettingsLineViewModel", Context.MODE_PRIVATE)
    }
    val goorback = viewModel.goorback
    val lineIndex = viewModel.selectedLineNumber - 1
    
    // Resolve line color from inputs and saved values.
    val color = run {
        fun parseColorString(colorString: String?): Color? {
            return colorString?.takeIf { it.isNotEmpty() }?.let {
                try {
                    val hexString = if (it.startsWith("#")) it else "#$it"
                    Color(hexString.toColorInt())
                } catch (_: Exception) {
                    null
                }
            }
        }
        
        parseColorString(selectedLineColor)
            ?: parseColorString(selectedLine?.lineColor)
            ?: parseColorString(goorback.settingsLineColorString(sharedPreferences, lineIndex))
            ?: Accent
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.lineColor),
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        // Color display.
        Box(
            modifier = Modifier
                .size(ScreenSize.settingsLineSheetColorCircleSmallSize())
                .clip(CircleShape)
                .background(
                    if (color.alpha == 0f || color == Color.Unspecified || color == Color.Transparent) {
                        Accent
                    } else {
                        color
                    }
                )
                .border(
                    width = ScreenSize.settingsSheetStrokeLineWidth(),
                    color = Gray,
                    shape = CircleShape
                )
                .clickable(onClick = onColorSelectClick)
        )
        
        // Color selection button.
        CommonComponents.CustomRectangleButton(
            title = stringResource(R.string.select),
            icon = Icons.Default.Palette,
            tintColor = Primary,
            onClick = onColorSelectClick
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

// Station header with optional first/last stop.
@Composable
private fun StationHeaderText(
    viewModel: SettingsLineViewModel,
    selectedTransportationKind: TransportationLineKind
) {
    val hasSelectedLine by remember { derivedStateOf { viewModel.hasSelectedLine } }
    val hasStops by remember { derivedStateOf { viewModel.hasStops } }
    val lineStops by viewModel.lineStopsState.collectAsState()
    
    val headerText = if (selectedTransportationKind == TransportationLineKind.BUS) {
        stringResource(R.string.busStopInput)
    } else {
        stringResource(R.string.stationInput)
    }
    
    val stationInfo = if (hasSelectedLine && hasStops && lineStops.isNotEmpty()) {
        val firstStop = lineStops.firstOrNull()?.displayName() ?: ""
        val lastStop = lineStops.lastOrNull()?.displayName() ?: ""
        ": $firstStop ${stringResource(R.string.to)} $lastStop"
    } else {
        ""
    }
    
    val titleFontSize = ScreenSize.settingsSheetTitleFontSize()
    val verticalSpacing = ScreenSize.settingsSheetVerticalSpacing()
    
    Text(
        text = headerText + stationInfo,
        fontSize = titleFontSize.value.sp,
        fontWeight = FontWeight.Bold,
        color = Black,
        modifier = Modifier.padding(top = verticalSpacing)
    )
}

// Departure stop input section.
@Composable
private fun DepartureStopInputSection(
    departureStopInput: String,
    selectedTransportationKind: TransportationLineKind,
    isDepartureSelected: Boolean,
    focusRequester: FocusRequester,
    onDepartureStopInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    CommonComponents.CustomTextField(
        value = departureStopInput,
        onValueChange = onDepartureStopInputChanged,
        placeholder = if (selectedTransportationKind == TransportationLineKind.BUS) {
            stringResource(R.string.enterDepartureStop)
        } else {
            stringResource(R.string.enterDepartureStation)
        },
        modifier = Modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        title = if (selectedTransportationKind == TransportationLineKind.BUS) {
            stringResource(R.string.departureStop)
        } else {
            stringResource(R.string.departureStation)
        },
        isCheckmarkValid = departureStopInput.isNotEmpty(),
        isCheckmarkSelected = isDepartureSelected,
        onFocusChanged = onFocusChanged
    )
}

// Arrival stop input section.
@Composable
private fun ArrivalStopInputSection(
    arrivalStopInput: String,
    selectedTransportationKind: TransportationLineKind,
    isArrivalSelected: Boolean,
    focusRequester: FocusRequester,
    onArrivalStopInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    CommonComponents.CustomTextField(
        value = arrivalStopInput,
        onValueChange = onArrivalStopInputChanged,
        placeholder = if (selectedTransportationKind == TransportationLineKind.BUS) {
            stringResource(R.string.enterArrivalStop)
        } else {
            stringResource(R.string.enterArrivalStation)
        },
        modifier = Modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        title = if (selectedTransportationKind == TransportationLineKind.BUS) {
            stringResource(R.string.arrivalStop)
        } else {
            stringResource(R.string.arrivalStation)
        },
        isCheckmarkValid = arrivalStopInput.isNotEmpty(),
        isCheckmarkSelected = isArrivalSelected,
        onFocusChanged = onFocusChanged
    )
}

// Time header text.
@Composable
private fun TimeHeaderText() {
    val titleFontSize = ScreenSize.settingsSheetTitleFontSize()
    val verticalSpacing = ScreenSize.settingsSheetVerticalSpacing()
    
    Text(
        text = stringResource(R.string.timeSettings),
        fontSize = titleFontSize.value.sp,
        fontWeight = FontWeight.Bold,
        color = Black,
        modifier = Modifier.padding(top = verticalSpacing)
    )
}

// Ride time section.
@Composable
private fun RideTimeSection(
    viewModel: SettingsLineViewModel,
    selectedRideTime: Int,
    isAllSelected: Boolean,
    onRideTimeChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("SettingsLineViewModel", Context.MODE_PRIVATE)
    val hasTimetableSupport by remember { derivedStateOf { viewModel.hasTimetableSupport() } }
    val selectedGoorback by viewModel.selectedGoorbackState.collectAsState()
    val selectedLineNumber by viewModel.selectedLineNumberState.collectAsState()
    
    val currentLineIndex = selectedLineNumber - 1
    val rideTimeKey = selectedGoorback.rideTimeKey(currentLineIndex)
    val hasSavedRideTime = sharedPreferences.contains(rideTimeKey)
    
    // Use saved ride time color for the checkmark.
    val rideTimeColor = selectedGoorback.settingsRideTimeColor(sharedPreferences, currentLineIndex)
    
    val horizontalSpacing = ScreenSize.settingsSheetHorizontalSpacing()
    val headlineFontSize = ScreenSize.settingsSheetHeadlineFontSize()

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.rideTime),
            fontSize = headlineFontSize.value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        // Display current ride time.
        Text(
            text = if (selectedRideTime == 0) {
                if (hasSavedRideTime) "0" else "-"
            } else {
                "$selectedRideTime ${stringResource(R.string.min)}"
            },
            fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        CommonComponents.Custom2DigitPicker(
            value = selectedRideTime,
            onValueChange = onRideTimeChanged,
            isZeroToFive = false
        )
                
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isAllSelected) Primary else if (selectedRideTime == 0 && !hasTimetableSupport) rideTimeColor else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
        )
    }
}

// Transportation settings section.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportationSettingsSection(
    viewModel: SettingsLineViewModel,
    selectedTransportation: String,
    onTransportationChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("SettingsLineViewModel", Context.MODE_PRIVATE)
    val selectedGoorback by viewModel.selectedGoorbackState.collectAsState()
    val selectedLineNumber by viewModel.selectedLineNumberState.collectAsState()
    
    // Resolve display value from saved data when needed.
    val currentLineIndex = selectedLineNumber - 1
    val savedTransportation = selectedGoorback.settingsTransportation(sharedPreferences, currentLineIndex + 2, context)
    val notSet = stringResource(R.string.notSet)
    
    // Prefer current selection unless unset.
    val displayTransportation = if (selectedTransportation == "none" || selectedTransportation.isEmpty()) {
        if (savedTransportation != notSet) savedTransportation else selectedTransportation
    } else {
        selectedTransportation
    }
    
    val transferType = TransferType.fromRawValue(displayTransportation)
    val transferTypeDisplayName = transferType.displayName()
    
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.nextTransfer),
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(2f)
        ) {
            Box(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ScreenSize.settingsSheetCornerRadius()))
                    .background(LightGray)
                    .padding(
                        vertical = ScreenSize.customTextFieldPaddingVertical(),
                        horizontal = ScreenSize.settingsSheetHorizontalSpacing()
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
                ) {
                    Icon(
                        imageVector = transferType.icon,
                        contentDescription = null,
                        tint = Black,
                        modifier = Modifier.size(ScreenSize.settingsSheetIconSize() * 1.1f)
                    )
                    Text(
                        text = transferTypeDisplayName,
                        fontSize =  ScreenSize.settingsSheetInputFontSize().value.sp,
                        color = Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(color = LightGray.copy(alpha = 0.85f))
                    .offset(
                        y = ScreenSize.settingsLineSheetTransportationDropdownOffsetY(),
                    )
            ) {
                TransferType.entries.reversed().forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
                            ) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    tint = Black,
                                    modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                                )
                                Text(
                                    text = type.displayName(),
                                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                                    color = Primary
                                )
                            }
                        },
                        onClick = {
                            onTransportationChanged(type.rawValue)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

// Transfer time settings section.
@Composable
private fun TransferTimeSettingsSection(
    viewModel: SettingsLineViewModel,
    selectedTransferTime: Int,
    isAllSelected: Boolean,
    onTransferTimeChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("SettingsLineViewModel", Context.MODE_PRIVATE)
    val selectedGoorback by viewModel.selectedGoorbackState.collectAsState()
    val selectedLineNumber by viewModel.selectedLineNumberState.collectAsState()
    
    val currentLineIndex = selectedLineNumber - 1
    val transferTimeKey = selectedGoorback.transferTimeKey(currentLineIndex + 2)
    val hasSavedTransferTime = sharedPreferences.contains(transferTimeKey)

    val horizontalSpacing = ScreenSize.settingsSheetHorizontalSpacing()
    val headlineFontSize = ScreenSize.settingsSheetHeadlineFontSize()

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.transferTime),
            fontSize = headlineFontSize.value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )

        Text(
            text = if (selectedTransferTime == 0) {
                if (hasSavedTransferTime) "0" else "-"
            } else {
                "$selectedTransferTime ${stringResource(R.string.min)}"
            },
            fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )

        Spacer(modifier = Modifier.weight(1f))

        CommonComponents.Custom2DigitPicker(
            value = selectedTransferTime,
            onValueChange = onTransferTimeChanged,
            isZeroToFive = false
        )
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isAllSelected) Primary else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
        )
    }
}

// Save button for line configuration.
@Composable
private fun SaveButtonSection(
    isAllNotEmpty: Boolean,
    isAllSelected: Boolean,
    onSave: () -> Unit
) {
    val isEnabled = isAllNotEmpty || isAllSelected
    CommonComponents.CustomButton(
        title = stringResource(R.string.inputSave),
        onClick = onSave,
        icon = Icons.Filled.Save,
        backgroundColor = if (isEnabled) Accent else Gray,
        isEnabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ScreenSize.settingsSheetVerticalSpacing())
    )
}

// Button to open manual timetable settings.
@Composable
private fun TimetableSettingsButtonSection(
    isAllNotEmpty: Boolean,
    isAllSelected: Boolean,
    onTimetableSettings: () -> Unit
) {
    val isEnabled = isAllNotEmpty || isAllSelected
    CommonComponents.CustomButton(
        title = stringResource(R.string.timetableSettings),
        onClick = onTimetableSettings,
        icon = Icons.Default.Schedule,
        backgroundColor = if (isEnabled) Accent else Gray,
        isEnabled = isEnabled,
        modifier = Modifier.fillMaxWidth()
    )
}

// Button to auto-generate timetable data.
@Composable
private fun TimetableAutoSettingsButtonSection(
    isAllSelected: Boolean,
    onAutoGenerate: () -> Unit
) {
    CommonComponents.CustomButton(
        title = stringResource(R.string.autoGenerateTimetable),
        onClick = onAutoGenerate,
        icon = Icons.Default.Schedule,
        backgroundColor = if (isAllSelected) Primary else Gray,
        isEnabled = isAllSelected,
        modifier = Modifier.fillMaxWidth()
    )
}

// Operator suggestions dropdown.
@Composable
private fun OperatorSuggestionsView(
    modifier: Modifier = Modifier,
    operatorSuggestions: List<String>,
    onOperatorSelected: (String) -> Unit,
    onDismissRequest: () -> Unit = {},
) {
    CommonComponents.CustomDropdown(
        items = operatorSuggestions,
        onItemSelected = { operatorName -> onOperatorSelected(operatorName) },
        onDismissRequest = onDismissRequest,
        itemContent = { operatorName ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
            ) {
                // Transportation kind tag.
                CommonComponents.CustomTag(
                    text = "",
                    backgroundColor = Gray
                )                
                Text(
                    text = operatorName,
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                    color = Primary,
                    maxLines = 1
                )
            }
        },
        modifier = modifier
    )
}

// Line suggestions dropdown.
@Composable
private fun LineSuggestionsView(
    modifier: Modifier = Modifier,
    viewModel: SettingsLineViewModel,
    lineSuggestions: List<TransportationLine>,
    onLineSelected: (TransportationLine) -> Unit,
    onDismissRequest: () -> Unit = {},
) {
    val uniqueLines = lineSuggestions.distinctBy { it.name }
    
    CommonComponents.CustomDropdown(
        items = uniqueLines,
        onItemSelected = { line -> onLineSelected(line) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        itemContent = { line ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
            ) {
                // Line code tag.
                val tagColor = line.lineColor?.safeColor
                CommonComponents.CustomTag(
                    text = line.lineCode ?: "",
                    backgroundColor = tagColor
                )
                
                // Line name.
                Text(
                    text = viewModel.lineDisplayName(line),
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                    color = Primary,
                    maxLines = 1
                )
            }
        }
    )
}

// Color picker overlay.
@Composable
private fun ColorSelectionSection(
    onColorSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val lineColors = CustomColor.entries

    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .width(ScreenSize.settingsLineSheetColorSettingWidth())
                .align(Alignment.Center)
                .offset(y = ScreenSize.settingsLineSheetDropdownOffsetY())
                .padding(horizontal = ScreenSize.settingsLineSheetColorHorizontalPadding())
                .background(
                    color = LightGray,
                    shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                )
                .border(
                    width = ScreenSize.settingsSheetStrokeLineWidth(),
                    color = Gray,
                    shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                )
                .zIndex(100f),
        ) {
            // Color selection header.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = ScreenSize.settingsSheetHorizontalPadding(),
                        vertical = ScreenSize.settingsSheetVerticalSpacing()
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.selectLineColor),
                    fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Black
                )
                
                // Cancel button.
                TextButton(onClick = onCancel) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Black
                    )
                }
            }
            
            // Color selection grid.
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding())
                    .padding(bottom = ScreenSize.settingsSheetVerticalSpacing()),
                horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsLineSheetGridSpacing()),
                verticalArrangement = Arrangement.spacedBy(ScreenSize.settingsLineSheetGridSpacing())
            ) {
                items(lineColors) { customColor ->
                    // Resolve color and stored RGB.
                    val color = customColor.color
                    val rgbString = customColor.RGB
                    // Localized color name.
                    val colorName = when (customColor.resourceName) {
                        "red" -> stringResource(R.string.red)
                        "darkRed" -> stringResource(R.string.darkRed)
                        "orange" -> stringResource(R.string.orange)
                        "brown" -> stringResource(R.string.brown)
                        "yellow" -> stringResource(R.string.yellow)
                        "beige" -> stringResource(R.string.beige)
                        "yellowGreen" -> stringResource(R.string.yellowGreen)
                        "olive" -> stringResource(R.string.olive)
                        "green" -> stringResource(R.string.green)
                        "darkGreen" -> stringResource(R.string.darkGreen)
                        "blueGreen" -> stringResource(R.string.blueGreen)
                        "lightBlue" -> stringResource(R.string.lightBlue)
                        "blue" -> stringResource(R.string.blue)
                        "navyBlue" -> stringResource(R.string.navyBlue)
                        "indigo" -> stringResource(R.string.indigo)
                        "purple" -> stringResource(R.string.purple)
                        "magenta" -> stringResource(R.string.magenta)
                        "lavender" -> stringResource(R.string.lavender)
                        "pink" -> stringResource(R.string.pink)
                        "gold" -> stringResource(R.string.gold)
                        "silver" -> stringResource(R.string.silver)
                        "gray" -> stringResource(R.string.gray)
                        "black" -> stringResource(R.string.black)
                        "defaultColor" -> stringResource(R.string.defaultColor)
                        else -> customColor.resourceName
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onColorSelected(rgbString) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Color circle with border.
                        Box(
                            modifier = Modifier
                                .size(ScreenSize.settingsLineSheetColorCircleSize())
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = ScreenSize.settingsSheetStrokeLineWidth(),
                                        color = Gray,
                                        shape = CircleShape
                                    )
                            )
                        }
                        
                        // Color name label.
                        Text(
                            text = colorName,
                            fontSize = ScreenSize.settingsLineSheetCaptionFontSize().value.sp,
                            maxLines = 1,
                            color = Black
                        )
                    }
                }
            }
        }
    }
}

// Departure stop suggestions dropdown.
@Composable
private fun DepartureStopSuggestionsView(
    modifier: Modifier = Modifier,
    departureSuggestions: List<TransportationStop>,
    onStopSelected: (TransportationStop) -> Unit,
    onDismissRequest: () -> Unit = {},
) {
    CommonComponents.CustomDropdown(
        items = departureSuggestions,
        onItemSelected = { stop -> onStopSelected(stop) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        itemContent = { stop ->
            Text(
                text = stop.displayName(),
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                color = Primary,
                maxLines = 1
            )
        }
    )
}

// Arrival stop suggestions dropdown.
@Composable
private fun ArrivalStopSuggestionsView(
    modifier: Modifier = Modifier,
    arrivalSuggestions: List<TransportationStop>,
    onStopSelected: (TransportationStop) -> Unit,
    onDismissRequest: () -> Unit = {},
) {
    CommonComponents.CustomDropdown(
        items = arrivalSuggestions,
        onItemSelected = { stop -> onStopSelected(stop) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        itemContent = { stop ->
            Text(
                text = stop.displayName(),
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                color = Primary,
                maxLines = 1
            )
        }
    )
}

