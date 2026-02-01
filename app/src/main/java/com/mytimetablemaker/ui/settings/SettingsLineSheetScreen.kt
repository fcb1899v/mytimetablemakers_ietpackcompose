package com.mytimetablemaker.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
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
import androidx.compose.ui.unit.dp
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

// MARK: - Settings Line Sheet Screen
// Sheet view for configuring railway lines and bus routes in settings
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
    
    // Validate goorback value and use default if invalid
    val validGoorback = if (goorback.isEmpty() || !goorbackOptions.contains(goorback)) "back1" else goorback
    
    // Create ViewModel
    val viewModel = remember(validGoorback, lineIndex) {
        SettingsLineViewModel(application, sharedPreferences, validGoorback, lineIndex)
    }
    
    // Local state for UI
    var showTimetableSettings by remember { mutableStateOf(false) }
    
    // Focus requesters for text fields
    val operatorFocusRequester = remember { FocusRequester() }
    val lineFocusRequester = remember { FocusRequester() }
    val departureFocusRequester = remember { FocusRequester() }
    val arrivalFocusRequester = remember { FocusRequester() }
    
    // Observe ViewModel state
    val operatorInput by viewModel.operatorInput.collectAsState()
    val operatorSuggestions by viewModel.operatorSuggestions.collectAsState()
    val showOperatorSuggestions by viewModel.showOperatorSuggestions.collectAsState()
    val operatorSelected by viewModel.operatorSelected.collectAsState()
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
    
    // Single source of truth: use ViewModel.isAllNotEmpty (four inputs + ride time > 0); recompose when any input or ride time changes
    val isAllNotEmpty by remember(operatorInput, lineInput, departureStopInput, arrivalStopInput, selectedRideTime) {
        derivedStateOf { viewModel.isAllNotEmpty }
    }
    
    val isAllSelected by remember(operatorSelected, selectedLine, selectedDepartureStop, selectedArrivalStop) {
        derivedStateOf {
            val result = operatorSelected &&
            selectedLine != null &&
            selectedDepartureStop != null &&
            selectedArrivalStop != null
            
            android.util.Log.d("SettingsLineSheetScreen", "isAllSelected: operatorSelected=$operatorSelected, selectedLine=${selectedLine != null}, selectedDepartureStop=${selectedDepartureStop != null}, selectedArrivalStop=${selectedArrivalStop != null}, result=$result")
            
            result
        }
    }
    
    // Status bar setup
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
    
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(ScreenSize.settingsSheetTopBarHeight())
                    .background(White)
            ) {
                // Back button aligned to the left
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
                
                // Title centered on screen
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
                // Route Header Menu
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
                
                // Line Number Menu
                LineNumberMenu(
                    viewModel = viewModel,
                    selectedLineNumber = selectedLineNumber,
                    selectedTransportationKind = selectedTransportationKind,
                    onTransportationKindChanged = { isRailway ->
                        viewModel.switchTransportationKind(isRailway)
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Operator Input Section
                OperatorInputSection(
                    viewModel = viewModel,
                    operatorInput = operatorInput,
                    isOperatorFieldFocused = isOperatorFieldFocused,
                    focusRequester = operatorFocusRequester,
                    onOperatorInputChanged = { newValue ->
                        // Update value immediately to ensure input works correctly
                        viewModel.operatorInput.value = newValue
                        // Process input and trigger filtering
                        // Request focus back to text field after filtering completes
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.processOperatorInput(newValue)
                            // After filterOperators completes, check if dropdown should be shown
                            // If dropdown is shown, ensure focus is on text field
                            if (viewModel.showOperatorSuggestions.value && viewModel.operatorSuggestions.value.isNotEmpty()) {
                                android.util.Log.d("SettingsLineSheetScreen", "onOperatorInputChanged: Dropdown should be shown, requesting focus")
                                operatorFocusRequester.requestFocus()
                            }
                        }
                    },
                    onFocusChanged = { isFocused ->
                        android.util.Log.d("SettingsLineSheetScreen", "onFocusChanged: isFocused=$isFocused, operatorInput='$operatorInput', operatorSelected=${viewModel.operatorSelected.value}, isLineNumberChanging=${viewModel.isLineNumberChanging.value}, isGoorBackChanging=${viewModel.isGoorBackChanging.value}, showOperatorSuggestions=${viewModel.showOperatorSuggestions.value}")
                        viewModel.isOperatorFieldFocused.value = isFocused
                        if (isFocused) {
                            // Show all operators when field is focused and operator input is empty
                            // Reset operatorSelected, isLineNumberChanging, and isGoorBackChanging to allow showing all operators when field is focused
                            // This is necessary because filterOperators returns early if operatorSelected, isLineNumberChanging, or isGoorBackChanging is true
                            viewModel.operatorSelected.value = false
                            viewModel.isLineNumberChanging.value = false
                            viewModel.isGoorBackChanging.value = false
                            android.util.Log.d("SettingsLineSheetScreen", "onFocusChanged: Reset operatorSelected, isLineNumberChanging, and isGoorBackChanging to false")
                            
                            if (operatorInput.isEmpty()) {
                                android.util.Log.d("SettingsLineSheetScreen", "onFocusChanged: Calling filterOperators(\"\")")
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.filterOperators("")
                                    // Request focus back to text field after filtering to show dropdown
                                    operatorFocusRequester.requestFocus()
                                }
                            } else {
                                android.util.Log.d("SettingsLineSheetScreen", "onFocusChanged: operatorInput is not empty, calling filterOperators with current input")
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.filterOperators(operatorInput)
                                    // Request focus back to text field after filtering to show dropdown
                                    operatorFocusRequester.requestFocus()
                                }
                            }
                        } else {
                            // If dropdown is showing, try to keep focus on text field
                            if (viewModel.showOperatorSuggestions.value && viewModel.operatorSuggestions.value.isNotEmpty()) {
                                android.util.Log.d("SettingsLineSheetScreen", "onFocusChanged: Focus lost but dropdown is showing, requesting focus back immediately")
                                // Request focus immediately when lost (no delay needed)
                                operatorFocusRequester.requestFocus()
                            } else {
                                // Hide suggestions when field loses focus
                                viewModel.showOperatorSuggestions.value = false
                            }
                        }
                    },
                    showOperatorSuggestions = showOperatorSuggestions && !isLineNumberChanging && !operatorSelected && isOperatorFieldFocused,
                    operatorSuggestions = operatorSuggestions,
                    selectedTransportationKind = selectedTransportationKind,
                    onOperatorSelected = { operatorName ->
                        viewModel.operatorInput.value = operatorName
                        viewModel.operatorSelected.value = true
                        viewModel.showOperatorSuggestions.value = false
                        viewModel.operatorSuggestions.value = emptyList()
                        
                        // Find operator code from operator name
                        // Convert TransportationLineKind to TransportationKind
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
                            
                            // Check if operator has changed from saved value
                            if (previousOperatorCode != operatorCode) {
                                viewModel.isChangedOperator.value = true
                            }
                            
                            viewModel.selectedOperatorCode.value = operatorCode
                            android.util.Log.d("SettingsLineSheetScreen", "onOperatorSelected: Found operatorCode=$operatorCode for operatorName=$operatorName")
                            
                            // Clear line input when operator is selected
                            viewModel.lineInput.value = ""
                            viewModel.lineSelected.value = false
                            
                            // Reset lineSelected and isLineNumberChanging to allow showing line suggestions
                            viewModel.isLineNumberChanging.value = false
                            
                            // Set focus state before calling processLineInput or fetchGTFSLinesForOperator
                            viewModel.isLineFieldFocused.value = true
                            
                            // For GTFS operators, fetch lines from ZIP cache
                            if (dataSource.apiType() == com.mytimetablemaker.models.ODPTAPIType.GTFS) {
                                android.util.Log.d("SettingsLineSheetScreen", "onOperatorSelected: GTFS operator detected, calling fetchGTFSLinesForOperator for ${dataSource.name}, operatorCode=$operatorCode")
                                // Fetch GTFS lines asynchronously
                                // fetchGTFSLinesForOperator will update lineSuggestions when complete
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        viewModel.fetchGTFSLinesForOperator(dataSource)
                                        android.util.Log.d("SettingsLineSheetScreen", "onOperatorSelected: fetchGTFSLinesForOperator completed for ${dataSource.name}")
                                    } catch (e: Exception) {
                                        android.util.Log.e("SettingsLineSheetScreen", "onOperatorSelected: fetchGTFSLinesForOperator failed for ${dataSource.name}: ${e.message}", e)
                                    }
                                }
                            } else {
                                // For non-GTFS operators, use filterLine to show line suggestions
                                // processLineInput will clear selectedLine internally
                                viewModel.processLineInput("")
                            }
                            
                            // Request focus on line input field
                            lineFocusRequester.requestFocus()
                        } else {
                            android.util.Log.d("SettingsLineSheetScreen", "onOperatorSelected: Could not find operatorCode for operatorName=$operatorName")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Line Input Section
                LineInputSection(
                    viewModel = viewModel,
                    lineInput = lineInput,
                    isLineFieldFocused = isLineFieldFocused,
                    selectedTransportationKind = selectedTransportationKind,
                    focusRequester = lineFocusRequester,
                    onLineInputChanged = { newValue ->
                        viewModel.processLineInput(newValue)
                    },
                    onFocusChanged = { isFocused ->
                        android.util.Log.d("SettingsLineSheetScreen", "LineInput onFocusChanged: isFocused=$isFocused, lineInput='$lineInput', operatorSelected=${viewModel.operatorSelected.value}, selectedOperatorCode=${viewModel.selectedOperatorCode.value}, isLineNumberChanging=${viewModel.isLineNumberChanging.value}, isGoorBackChanging=${viewModel.isGoorBackChanging.value}, lineSelected=${viewModel.lineSelected.value}")
                        viewModel.isLineFieldFocused.value = isFocused
                        if (isFocused) {
                            // Show line suggestions when field is focused, operator is selected, and line input is empty
                            // Reset isLineNumberChanging, isGoorBackChanging, and lineSelected to allow showing suggestions when field is focused
                            viewModel.isLineNumberChanging.value = false
                            viewModel.isGoorBackChanging.value = false
                            viewModel.lineSelected.value = false
                            android.util.Log.d("SettingsLineSheetScreen", "LineInput onFocusChanged: After reset - isLineNumberChanging=${viewModel.isLineNumberChanging.value}, isGoorBackChanging=${viewModel.isGoorBackChanging.value}, lineSelected=${viewModel.lineSelected.value}")
                            
                            // Show all lines when field is focused and operator is selected
                            if (viewModel.selectedOperatorCode.value != null && viewModel.operatorSelected.value && lineInput.isEmpty()) {
                                android.util.Log.d("SettingsLineSheetScreen", "LineInput onFocusChanged: Calling filterLine(\"\") with empty input, isFocused=true")
                                CoroutineScope(Dispatchers.Main).launch {
                                    viewModel.filterLine("", isFocused = true)
                                }
                            } else {
                                android.util.Log.d("SettingsLineSheetScreen", "LineInput onFocusChanged: Conditions not met - selectedOperatorCode=${viewModel.selectedOperatorCode.value}, operatorSelected=${viewModel.operatorSelected.value}, lineInput='$lineInput'")
                            }
                        } else {
                            // Hide suggestions when field loses focus
                            viewModel.showLineSuggestions.value = false
                        }
                    },
                    showLineSuggestions = showLineSuggestions && !isLineNumberChanging && !lineSelected && isLineFieldFocused && viewModel.operatorSelected.collectAsState().value,
                    lineSuggestions = lineSuggestions,
                    onLineSelected = { line ->
                        viewModel.selectLine(line)
                        viewModel.showLineSuggestions.value = false
                        viewModel.lineSelected.value = true
                        
                        // If line has no color, show color selection
                        // Otherwise, focus on departure station field
                        if (line.lineColor == null) {
                            viewModel.showColorSelection.value = true
                        } else {
                            // Focus on departure station field when line has color
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(100) // Small delay to ensure line selection is processed
                                departureFocusRequester.requestFocus()
                                viewModel.isDepartureFieldFocused.value = true
                            }
                        }
                    },
                    operatorSelected = operatorSelected
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Line Color Section
                LineColorSection(
                    viewModel = viewModel,
                    selectedLineColor = selectedLineColor,
                    selectedLine = selectedLine,
                    onColorSelectClick = {
                        viewModel.showColorSelection.value = true
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Station Header Text
                StationHeaderText(
                    viewModel = viewModel,
                    selectedTransportationKind = selectedTransportationKind
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Departure Stop Input Section
                DepartureStopInputSection(
                    viewModel = viewModel,
                    departureStopInput = departureStopInput,
                    isDepartureFieldFocused = isDepartureFieldFocused,
                    selectedTransportationKind = selectedTransportationKind,
                    focusRequester = departureFocusRequester,
                    onDepartureStopInputChanged = { newValue ->
                        viewModel.processDepartureStopInput(newValue)
                    },
                    onFocusChanged = { isFocused ->
                        android.util.Log.d("SettingsLineSheetScreen", "DepartureStop onFocusChanged: isFocused=$isFocused, departureStopInput='$departureStopInput', operatorSelected=$operatorSelected, lineSelected=$lineSelected, _lineStops=${viewModel.lineStopsState.value.size}")
                        viewModel.isDepartureFieldFocused.value = isFocused
                        if (isFocused && lineSelected) {
                            // Reset selection flag to allow suggestions to show
                            viewModel.departureStopSelected.value = false
                            // Filter and show all stations when field is focused
                            viewModel.filterDepartureStops(departureStopInput)
                        } else if (!isFocused) {
                            viewModel.showDepartureSuggestions.value = false
                        }
                    },
                    showDepartureSuggestions = showDepartureSuggestions && lineSelected && isDepartureFieldFocused,
                    departureSuggestions = departureSuggestions,
                    onStopSelected = { stop ->
                        viewModel.isLineNumberChanging.value = true
                        
                        val arrivalDisplayName = viewModel.selectedArrivalStopState.value?.displayName(context) ?: ""
                        val isSameAsArrival = arrivalDisplayName == stop.displayName(context)
                        viewModel.departureStopInput.value = if (isSameAsArrival) "" else stop.displayName(context)
                        viewModel.setSelectedDepartureStop(if (isSameAsArrival) null else stop)
                        viewModel.showDepartureSuggestions.value = false
                        viewModel.isDepartureFieldFocused.value = false
                        viewModel.departureSuggestions.value = emptyList()
                        viewModel.departureStopSelected.value = true
                        
                        // Focus on arrival station field after departure station selection
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100) // Small delay to ensure departure station selection is processed
                            arrivalFocusRequester.requestFocus()
                            viewModel.isArrivalFieldFocused.value = true
                            viewModel.isLineNumberChanging.value = false
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Arrival Stop Input Section
                ArrivalStopInputSection(
                    viewModel = viewModel,
                    arrivalStopInput = arrivalStopInput,
                    isArrivalFieldFocused = isArrivalFieldFocused,
                    selectedTransportationKind = selectedTransportationKind,
                    focusRequester = arrivalFocusRequester,
                    onArrivalStopInputChanged = { newValue ->
                        viewModel.processArrivalStopInput(newValue)
                    },
                    onFocusChanged = { isFocused ->
                        android.util.Log.d("SettingsLineSheetScreen", "ArrivalStop onFocusChanged: isFocused=$isFocused, arrivalStopInput='$arrivalStopInput', operatorSelected=$operatorSelected, lineSelected=$lineSelected, _lineStops=${viewModel.lineStopsState.value.size}")
                        viewModel.isArrivalFieldFocused.value = isFocused
                        if (isFocused && lineSelected) {
                            // Reset selection flag to allow suggestions to show
                            viewModel.arrivalStopSelected.value = false
                            // Filter and show all stations when field is focused
                            viewModel.filterArrivalStops(arrivalStopInput)
                        } else if (!isFocused) {
                            viewModel.showArrivalSuggestions.value = false
                        }
                    },
                    showArrivalSuggestions = showArrivalSuggestions && lineSelected && isArrivalFieldFocused,
                    arrivalSuggestions = arrivalSuggestions,
                    onStopSelected = { stop ->
                        viewModel.isLineNumberChanging.value = true
                        
                        val departureDisplayName = viewModel.selectedDepartureStopState.value?.displayName(context) ?: ""
                        val isSameAsDeparture = departureDisplayName == stop.displayName(context)
                        viewModel.arrivalStopInput.value = if (isSameAsDeparture) "" else stop.displayName(context)
                        viewModel.setSelectedArrivalStop(if (isSameAsDeparture) null else stop)
                        viewModel.showArrivalSuggestions.value = false
                        viewModel.isArrivalFieldFocused.value = false
                        viewModel.arrivalSuggestions.value = emptyList()
                        viewModel.arrivalStopSelected.value = true
                        
                        // If departure station is not input, focus on departure station field
                        // Otherwise, clear all focus (only for lines 1 and 2)
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100) // Small delay to ensure arrival station selection is processed
                            if (departureStopInput.isEmpty()) {
                                // Focus on departure station field if it's empty
                                departureFocusRequester.requestFocus()
                                viewModel.isDepartureFieldFocused.value = true
                            } else if (viewModel.selectedLineNumber < 3) {
                                // Clear all focus only for lines 1 and 2
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
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Time Header Text
                TimeHeaderText()
                
                // Ride Time Section
                RideTimeSection(
                    viewModel = viewModel,
                    selectedRideTime = selectedRideTime,
                    onRideTimeChanged = { newValue ->
                        viewModel._selectedRideTime.value = newValue
                    }
                )

                // Transportation Settings Section (only for lines 1 and 2)
                if (selectedLineNumber < 3) {
                    TransportationSettingsSection(
                        viewModel = viewModel,
                        selectedTransportation = selectedTransportation,
                        onTransportationChanged = { newTransportation ->
                            viewModel.selectedTransportation.value = newTransportation
                        }
                    )

                    // Transfer Time Settings Section (only when transportation is not "none")
                    if (selectedTransportation != "none") {
                        TransferTimeSettingsSection(
                            viewModel = viewModel,
                            selectedTransferTime = selectedTransferTime,
                            onTransferTimeChanged = { newValue ->
                                viewModel.selectedTransferTime.value = newValue
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(verticalSpacing))
                    }
                }
                
                // Save Button Section (active when isAllNotEmpty || isAllSelected)
                SaveButtonSection(
                    viewModel = viewModel,
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
                
                // Timetable Settings Button Section (active when isAllNotEmpty || isAllSelected)
                TimetableSettingsButtonSection(
                    viewModel = viewModel,
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
                
                // Timetable Auto Settings Button Section (active when isAllSelected only)
                TimetableAutoSettingsButtonSection(
                    viewModel = viewModel,
                    isAllSelected = isAllSelected,
                    onAutoGenerate = {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                // Save line data first
                                viewModel.handleLineSave()
                                // Auto-generate timetable and wait for completion
                                viewModel.autoGenerateTimetable()
                                // Small delay to ensure data is saved to SharedPreferences
                                delay(100)
                                // Navigate to timetable content screen after generation
                                showTimetableSettings = true
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsLineSheetScreen", "Failed to auto-generate timetable: ${e.message}", e)
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
            }
            
            // MARK: - Operator Suggestions (ZStack layer)
            // Display operator dropdown in ZStack layer
            if (showOperatorSuggestions && operatorSuggestions.isNotEmpty() && !isLineNumberChanging && !operatorSelected && isOperatorFieldFocused) {
                OperatorSuggestionsView(
                    viewModel = viewModel,
                    operatorSuggestions = operatorSuggestions,
                    selectedTransportationKind = selectedTransportationKind,
                    onOperatorSelected = { operatorName ->
                        viewModel.operatorInput.value = operatorName
                        viewModel.operatorSelected.value = true
                        viewModel.showOperatorSuggestions.value = false
                        viewModel.operatorSuggestions.value = emptyList()
                        
                        // Find operator code from operator name
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
                            
                            // Check if operator has changed from saved value
                            if (previousOperatorCode != operatorCode) {
                                viewModel.isChangedOperator.value = true
                            }
                            
                            viewModel.selectedOperatorCode.value = operatorCode
                            android.util.Log.d("SettingsLineSheetScreen", "OperatorSuggestionsView onOperatorSelected: Found operatorCode=$operatorCode for operatorName=$operatorName")
                            
                            // Clear line input when operator is selected
                            viewModel.lineInput.value = ""
                            viewModel.lineSelected.value = false
                            
                            // Reset lineSelected and isLineNumberChanging to allow showing line suggestions
                            viewModel.isLineNumberChanging.value = false
                            
                            // Set focus state before calling processLineInput or fetchGTFSLinesForOperator
                            viewModel.isLineFieldFocused.value = true
                            
                            // For GTFS operators, fetch lines from ZIP cache
                            if (dataSource.apiType() == com.mytimetablemaker.models.ODPTAPIType.GTFS) {
                                android.util.Log.d("SettingsLineSheetScreen", "OperatorSuggestionsView onOperatorSelected: GTFS operator detected, calling fetchGTFSLinesForOperator for ${dataSource.name}, operatorCode=$operatorCode")
                                // Fetch GTFS lines asynchronously
                                // fetchGTFSLinesForOperator will update lineSuggestions when complete
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        viewModel.fetchGTFSLinesForOperator(dataSource)
                                        android.util.Log.d("SettingsLineSheetScreen", "OperatorSuggestionsView onOperatorSelected: fetchGTFSLinesForOperator completed for ${dataSource.name}")
                                    } catch (e: Exception) {
                                        android.util.Log.e("SettingsLineSheetScreen", "OperatorSuggestionsView onOperatorSelected: fetchGTFSLinesForOperator failed for ${dataSource.name}: ${e.message}", e)
                                    }
                                }
                            } else {
                                // For non-GTFS operators, use filterLine to show line suggestions
                                // processLineInput will clear selectedLine internally
                                viewModel.processLineInput("")
                            }
                            
                            // Request focus on line input field
                            lineFocusRequester.requestFocus()
                        } else {
                            android.util.Log.d("SettingsLineSheetScreen", "OperatorSuggestionsView onOperatorSelected: Could not find operatorCode for operatorName=$operatorName")
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
            
            // MARK: - Line Suggestions (ZStack layer)
            // Display line dropdown in ZStack layer
            if (showLineSuggestions && lineSuggestions.isNotEmpty() && !isLineNumberChanging && !lineSelected && isLineFieldFocused && operatorSelected) {
                LineSuggestionsView(
                    viewModel = viewModel,
                    lineSuggestions = lineSuggestions,
                    onLineSelected = { line ->
                        viewModel.selectLine(line)
                        viewModel.showLineSuggestions.value = false
                        viewModel.lineSelected.value = true
                        
                        // If line has no color, show color selection
                        // Otherwise, focus on departure station field
                        if (line.lineColor == null) {
                            viewModel.showColorSelection.value = true
                        } else {
                            // Focus on departure station field when line has color
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(100) // Small delay to ensure line selection is processed
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
            
            // MARK: - Departure Stop Suggestions (ZStack layer)
            // Display departure stop dropdown in ZStack layer
            if (showDepartureSuggestions && departureSuggestions.isNotEmpty() && lineSelected && isDepartureFieldFocused) {
                DepartureStopSuggestionsView(
                    viewModel = viewModel,
                    departureSuggestions = departureSuggestions,
                    onStopSelected = { stop ->
                        viewModel.isLineNumberChanging.value = true
                        val arrivalDisplayName = viewModel.selectedArrivalStopState.value?.displayName(context) ?: ""
                        val isSameAsArrival = arrivalDisplayName == stop.displayName(context)
                        viewModel.departureStopInput.value = if (isSameAsArrival) "" else stop.displayName(context)
                        viewModel.setSelectedDepartureStop(if (isSameAsArrival) null else stop)
                        viewModel.showDepartureSuggestions.value = false
                        viewModel.isDepartureFieldFocused.value = false
                        viewModel.departureSuggestions.value = emptyList()
                        viewModel.departureStopSelected.value = true
                        // Focus on arrival station field after departure station selection
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100) // Small delay to ensure departure station selection is processed
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
            
            // MARK: - Arrival Stop Suggestions (ZStack layer)
            // Display arrival stop dropdown in ZStack layer
            if (showArrivalSuggestions && arrivalSuggestions.isNotEmpty() && lineSelected && isArrivalFieldFocused) {
                ArrivalStopSuggestionsView(
                    viewModel = viewModel,
                    arrivalSuggestions = arrivalSuggestions,
                    onStopSelected = { stop ->
                        viewModel.isLineNumberChanging.value = true
                        val departureDisplayName = viewModel.selectedDepartureStopState.value?.displayName(context) ?: ""
                        val isSameAsDeparture = departureDisplayName == stop.displayName(context)
                        viewModel.arrivalStopInput.value = if (isSameAsDeparture) "" else stop.displayName(context)
                        viewModel.setSelectedArrivalStop(if (isSameAsDeparture) null else stop)
                        viewModel.showArrivalSuggestions.value = false
                        viewModel.isArrivalFieldFocused.value = false
                        viewModel.arrivalSuggestions.value = emptyList()
                        viewModel.arrivalStopSelected.value = true
                        
                        // If departure station is not input, focus on departure station field
                        // Otherwise, clear all focus (only for lines 1 and 2)
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100) // Small delay to ensure arrival station selection is processed
                            if (departureStopInput.isEmpty()) {
                                // Focus on departure station field if it's empty
                                departureFocusRequester.requestFocus()
                                viewModel.isDepartureFieldFocused.value = true
                            } else if (viewModel.selectedLineNumber < 3) {
                                // Clear all focus only for lines 1 and 2
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
            
            // Color Selection Section
            if (showColorSelection || (lineInput.isNotEmpty() && selectedLineColor == null && selectedLine?.lineColor == null && !lineSelected)) {
                ColorSelectionSection(
                    viewModel = viewModel,
                    onColorSelected = { color ->
                        viewModel.setLineColor(color)
                        viewModel.showColorSelection.value = false
                        // Focus on departure station field after color selection
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100) // Small delay to ensure color selection is processed
                            departureFocusRequester.requestFocus()
                            viewModel.isDepartureFieldFocused.value = true
                        }
                    },
                    onCancel = {
                        viewModel.showColorSelection.value = false
                    }
                )
            }
            
            
            // Loading Overlay
            if (isLoadingBusStops || isLoadingTimetable || isLoadingLines) {
                LoadingOverlay(
                    message = loadingMessage
                )
            }
        }
    }
    
    // Navigate to timetable settings when showTimetableSettings is true
    LaunchedEffect(showTimetableSettings) {
        if (showTimetableSettings) {
            onNavigateToTimetableSettings(validGoorback, lineIndex)
            showTimetableSettings = false
        }
    }
    
    // Initialize on appear
    LaunchedEffect(Unit) {
        viewModel.isChangedOperator.value = false
        if (viewModel.selectedGoorback != validGoorback) {
            viewModel.selectGoorback(validGoorback)
        }
    }
    
    //             if isShowing { clearAllFocus() }
    //         }
    // Clear all focus when color selection is opened
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

// MARK: - Helper Functions
// Clear all focus from text fields
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

// MARK: - Route Header Menu
// Main header view with direction selection dropdown and clear button
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
        // Direction selection dropdown
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
                goorbackOptions.forEach { option ->
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

// MARK: - Line Number Menu
// Line number selection menu with transportation kind toggle
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
        // Line number selection dropdown
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
        
        // Transportation Kind Toggle
        CommonComponents.CustomToggle(
            isLeftSelected = selectedTransportationKind == TransportationLineKind.RAILWAY,
            onToggle = onTransportationKindChanged,
            leftText = stringResource(R.string.railway),
            leftColor = Primary,
            rightText = stringResource(R.string.bus),
            rightColor = Primary,
            circleColor = White,
            offColor = Gray
        )
    }
}

// MARK: - Operator Input Section
// Section for inputting operator name
@Composable
private fun OperatorInputSection(
    viewModel: SettingsLineViewModel,
    operatorInput: String,
    isOperatorFieldFocused: Boolean,
    focusRequester: FocusRequester,
    onOperatorInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    showOperatorSuggestions: Boolean,
    operatorSuggestions: List<String>,
    selectedTransportationKind: TransportationLineKind,
    onOperatorSelected: (String) -> Unit
) {
    // When dropdown is shown, ensure focus is maintained
    // IME visibility is now handled by CustomTextField itself
    LaunchedEffect(showOperatorSuggestions) {
        if (showOperatorSuggestions && operatorSuggestions.isNotEmpty()) {
            val isCurrentlyFocused = viewModel.isOperatorFieldFocused.value
            if (!isCurrentlyFocused) {
                android.util.Log.d("SettingsLineSheetScreen", "OperatorInputSection: Focus lost while dropdown is showing, requesting focus")
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
        onFocusChanged = onFocusChanged
    )
}

// MARK: - Line Input Section
// Section for inputting line name
@Composable
private fun LineInputSection(
    viewModel: SettingsLineViewModel,
    lineInput: String,
    isLineFieldFocused: Boolean,
    selectedTransportationKind: TransportationLineKind,
    focusRequester: FocusRequester,
    onLineInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    showLineSuggestions: Boolean,
    lineSuggestions: List<TransportationLine>,
    onLineSelected: (TransportationLine) -> Unit,
    operatorSelected: Boolean // Added operatorSelected parameter
) {
    CommonComponents.CustomTextField(
        value = lineInput,
        onValueChange = onLineInputChanged,
        placeholder = if (selectedTransportationKind == TransportationLineKind.RAILWAY) {
            stringResource(R.string.enterLineName)
        } else {
            stringResource(R.string.enterBusRouteName)
        },
        modifier = Modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        title = stringResource(R.string.lineName),
        isCheckmarkValid = lineInput.isNotEmpty(),
        onFocusChanged = onFocusChanged
    )
}

// MARK: - Line Color Section
// Section for displaying and selecting line colors
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
    
    // Use settingsLineColorString function to get color
    val color = run {
        fun parseColorString(colorString: String?): Color? {
            return colorString?.takeIf { it.isNotEmpty() }?.let {
                try {
                    val hexString = if (it.startsWith("#")) it else "#$it"
                    Color(hexString.toColorInt())
                } catch (e: Exception) {
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
        
        // Color display circle
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
        
        // Color Selection Button
        CommonComponents.CustomRectangleButton(
            title = stringResource(R.string.select),
            icon = Icons.Default.Palette,
            tintColor = Primary,
            onClick = onColorSelectClick
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

// MARK: - Station Header Text
// Station header with dynamic station information
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
        val context = LocalContext.current
        val firstStop = lineStops.firstOrNull()?.displayName(context) ?: ""
        val lastStop = lineStops.lastOrNull()?.displayName(context) ?: ""
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

// MARK: - Departure Stop Input Section
// Section for inputting departure station information
@Composable
private fun DepartureStopInputSection(
    viewModel: SettingsLineViewModel,
    departureStopInput: String,
    isDepartureFieldFocused: Boolean,
    selectedTransportationKind: TransportationLineKind,
    focusRequester: FocusRequester,
    onDepartureStopInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    showDepartureSuggestions: Boolean,
    departureSuggestions: List<TransportationStop>,
    onStopSelected: (TransportationStop) -> Unit
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
        onFocusChanged = onFocusChanged
    )
}

// MARK: - Arrival Stop Input Section
// Section for inputting arrival station information
@Composable
private fun ArrivalStopInputSection(
    viewModel: SettingsLineViewModel,
    arrivalStopInput: String,
    isArrivalFieldFocused: Boolean,
    selectedTransportationKind: TransportationLineKind,
    focusRequester: FocusRequester,
    onArrivalStopInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    showArrivalSuggestions: Boolean,
    arrivalSuggestions: List<TransportationStop>,
    onStopSelected: (TransportationStop) -> Unit
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
        onFocusChanged = onFocusChanged
    )
}

// MARK: - Time Header Text
// Time header with simple text
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

// MARK: - Ride Time Section
// Section for configuring travel time between stations
@Composable
private fun RideTimeSection(
    viewModel: SettingsLineViewModel,
    selectedRideTime: Int,
    onRideTimeChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("SettingsLineViewModel", Context.MODE_PRIVATE)
    val hasTimetableSupport by remember { derivedStateOf { viewModel.hasTimetableSupport() } }
    val selectedGoorback by viewModel.selectedGoorbackState.collectAsState()
    val selectedLineNumber by viewModel.selectedLineNumberState.collectAsState()
    
    // Use settingsRideTime function for display text
    val currentLineIndex = selectedLineNumber - 1
    val rideTimeText = selectedGoorback.settingsRideTime(sharedPreferences, currentLineIndex, context)
    
    // Use settingsRideTimeColor function for checkmark color
    val rideTimeColor = selectedGoorback.settingsRideTimeColor(sharedPreferences, currentLineIndex)
    
    val pickerPadding = ScreenSize.settingsLineSheetPickerPadding()
    val horizontalSpacing = ScreenSize.settingsSheetHorizontalSpacing()
    val headlineFontSize = ScreenSize.settingsSheetHeadlineFontSize()
    val pickerDisplayHeight = ScreenSize.settingsSheetPickerDisplayHeight()
    val paddingVertical = ScreenSize.settingsSheetInputPaddingVertical()
    val paddingHorizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
    val strokeLineWidth = ScreenSize.settingsSheetStrokeLineWidth()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = pickerPadding),
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

        // Display current ride time
        Text(
            text = if (selectedRideTime == 0) "-" else "$selectedRideTime ${stringResource(R.string.min)}",
            fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            // Custom2DigitPicker overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .padding(end = paddingHorizontal),
                horizontalArrangement = Arrangement.End
            ) {
                CommonComponents.Custom2DigitPicker(
                    value = selectedRideTime,
                    onValueChange = onRideTimeChanged,
                    isZeroToFive = false
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (selectedRideTime == 0 && !hasTimetableSupport) rideTimeColor else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize() * 1.2f)
        )
    }
}

// MARK: - Transportation Settings Section
// Section for selecting transportation method for next transfer
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
    
    // Use settingsTransportation function for display text (if not set)
    val currentLineIndex = selectedLineNumber - 1
    val savedTransportation = selectedGoorback.settingsTransportation(sharedPreferences, currentLineIndex + 2, context)
    val notSet = stringResource(R.string.notSet)
    
    // Use saved transportation if current is "none" or empty, otherwise use current
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
            modifier = Modifier.weight(1f)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val verticalPadding = ScreenSize.customTextFieldPaddingVertical()
            val inputFontSize = ScreenSize.settingsSheetInputFontSize()
            val textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = inputFontSize.value.sp,
                color = Black
            )
            
            BasicTextField(
                value = transferTypeDisplayName,
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
                    value = transferTypeDisplayName,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = {
                        Text(
                            text = "",
                            fontSize = inputFontSize.value.sp,
                            color = Gray,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = transferType.icon,
                            contentDescription = null,
                            tint = Black,
                            modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LightGray,
                        unfocusedContainerColor = LightGray,
                        focusedTextColor = Primary,
                        unfocusedTextColor = Primary,
                        focusedPlaceholderColor = Gray,
                        unfocusedPlaceholderColor = Gray
                    ),
                    contentPadding = PaddingValues(vertical = verticalPadding),
                ) {
                    TextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = LightGray,
                            unfocusedContainerColor = LightGray,
                        ),
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius()),
                    )
                }
            }
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(color = LightGray)
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

// MARK: - Transfer Time Settings Section
// Section for configuring transfer time
@Composable
private fun TransferTimeSettingsSection(
    viewModel: SettingsLineViewModel,
    selectedTransferTime: Int,
    onTransferTimeChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("SettingsLineViewModel", Context.MODE_PRIVATE)
    val selectedGoorback by viewModel.selectedGoorbackState.collectAsState()
    val selectedLineNumber by viewModel.selectedLineNumberState.collectAsState()
    
    // Use settingsTransferTime function for display text
    val currentLineIndex = selectedLineNumber - 1
    val transferTimeText = selectedGoorback.settingsTransferTime(sharedPreferences, currentLineIndex + 2, context)
    
    val pickerPadding = ScreenSize.settingsLineSheetPickerPadding()
    val horizontalSpacing = ScreenSize.settingsSheetHorizontalSpacing()
    val headlineFontSize = ScreenSize.settingsSheetHeadlineFontSize()
    val pickerDisplayHeight = ScreenSize.settingsSheetPickerDisplayHeight()
    val paddingVertical = ScreenSize.settingsSheetInputPaddingVertical()
    val paddingHorizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
    val strokeLineWidth = ScreenSize.settingsSheetStrokeLineWidth()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = pickerPadding),
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
            text = if (selectedTransferTime == 0) "-" else "$selectedTransferTime ${stringResource(R.string.min)}",
            fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            // Custom2DigitPicker overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .padding(end = paddingHorizontal),
                horizontalArrangement = Arrangement.End
            ) {
                CommonComponents.Custom2DigitPicker(
                    value = selectedTransferTime,
                    onValueChange = onTransferTimeChanged,
                    isZeroToFive = false
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize() * 1.2f)
        )
    }
}

// MARK: - Save Button Section
// Save button for storing line configuration data (active when isAllNotEmpty || isAllSelected)
@Composable
private fun SaveButtonSection(
    viewModel: SettingsLineViewModel,
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

// MARK: - Timetable Settings Button Section
// Button to open manual timetable configuration settings (active when isAllNotEmpty || isAllSelected)
@Composable
private fun TimetableSettingsButtonSection(
    viewModel: SettingsLineViewModel,
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

// MARK: - Timetable Auto Settings Button Section
// Button to automatically generate timetable data (active when isAllSelected only)
@Composable
private fun TimetableAutoSettingsButtonSection(
    viewModel: SettingsLineViewModel,
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

// MARK: - Operator Suggestions View
// Dropdown list showing suggested operators
@Composable
private fun OperatorSuggestionsView(
    viewModel: SettingsLineViewModel,
    operatorSuggestions: List<String>,
    selectedTransportationKind: TransportationLineKind,
    onOperatorSelected: (String) -> Unit,
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    CommonComponents.CustomDropdown(
        items = operatorSuggestions,
        onItemSelected = { operatorName -> onOperatorSelected(operatorName) },
        onDismissRequest = onDismissRequest,
        itemContent = { operatorName ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
            ) {
                // Transportation kind tag
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

// MARK: - Line Suggestions View
// Dropdown list showing suggested lines
@Composable
private fun LineSuggestionsView(
    viewModel: SettingsLineViewModel,
    lineSuggestions: List<TransportationLine>,
    onLineSelected: (TransportationLine) -> Unit,
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                // Display lineCode using CustomTag
                // If lineCode is empty, show CustomTag with empty text
                val tagColor = line.lineColor?.safeColor
                CommonComponents.CustomTag(
                    text = line.lineCode ?: "",
                    backgroundColor = tagColor
                )
                
                // Display line name
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

// MARK: - Color Selection Section
// Color picker overlay for selecting line colors
@Composable
private fun ColorSelectionSection(
    viewModel: SettingsLineViewModel,
    onColorSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    // Use CustomColor.allCases
    val lineColors = CustomColor.entries
    
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .width(ScreenSize.settingsLineSheetColorSettingWidth())
                .align(Alignment.Center)
                .offset(
                    x = ScreenSize.settingsLineSheetDropdownOffsetX(),
                    y = ScreenSize.settingsLineSheetDropdownOffsetY(),
                )
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
            // Color Selection Header
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
                
                // Cancel button
                TextButton(onClick = onCancel) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Black
                    )
                }
            }
            
            // Color Selection Grid
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
                    // Use ColorExtensions.kt's color property
                    // This property uses android.graphics.Color.parseColor for reliable color parsing
                    val color = customColor.color
                    // Use ColorExtensions.kt's RGB property for storing color string
                    val rgbString = customColor.RGB
                    // Use CustomColor.resourceName for localization
                    val colorName = when (customColor.resourceName) {
                        "red" -> stringResource(R.string.red)
                        "darkRed" -> stringResource(R.string.darkRed)
                        "orange" -> stringResource(R.string.orange)
                        "brown" -> stringResource(R.string.brown)
                        "yellow" -> stringResource(R.string.yellow)
                        "beige" -> stringResource(R.string.beige)
                        "yellowGreen" -> stringResource(R.string.yellowGreen)
                        "orive" -> stringResource(R.string.orive)
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
                        // Color circle with border
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
                        
                        // Color name label (localized)
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

// MARK: - Departure Stop Suggestions View
// Dropdown list showing suggested departure stops
@Composable
private fun DepartureStopSuggestionsView(
    viewModel: SettingsLineViewModel,
    departureSuggestions: List<TransportationStop>,
    onStopSelected: (TransportationStop) -> Unit,
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    CommonComponents.CustomDropdown(
        items = departureSuggestions,
        onItemSelected = { stop -> onStopSelected(stop) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        itemContent = { stop ->
            Text(
                text = stop.displayName(context),
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                color = Primary,
                maxLines = 1
            )
        }
    )
}

// MARK: - Arrival Stop Suggestions View
// Dropdown list showing suggested arrival stops
@Composable
private fun ArrivalStopSuggestionsView(
    viewModel: SettingsLineViewModel,
    arrivalSuggestions: List<TransportationStop>,
    onStopSelected: (TransportationStop) -> Unit,
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    CommonComponents.CustomDropdown(
        items = arrivalSuggestions,
        onItemSelected = { stop -> onStopSelected(stop) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        itemContent = { stop ->
            Text(
                text = stop.displayName(context),
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                color = Primary,
                maxLines = 1
            )
        }
    )
}

// MARK: - Loading Overlay
// Dark overlay with progress bar when loading
@Composable
private fun LoadingOverlay(
    message: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ScreenSize.splashLoadingSpacing())
        ) {
            CircularProgressIndicator(
                color = White,
                modifier = Modifier.size(ScreenSize.splashIconSize())
            )
            
            if (message != null) {
                Text(
                    text = message,
                    fontSize = ScreenSize.splashLoadingFontSize().value.sp,
                    color = White
                )
            }
        }
    }
}


