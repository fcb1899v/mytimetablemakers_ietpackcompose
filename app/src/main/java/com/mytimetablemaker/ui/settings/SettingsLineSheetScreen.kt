package com.mytimetablemaker.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.extensions.goorbackOptions
import com.mytimetablemaker.models.*
import com.mytimetablemaker.ui.common.*
import com.mytimetablemaker.ui.theme.*
import com.mytimetablemaker.ui.common.CommonComponents
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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
    val sharedPreferences = context.getSharedPreferences("SettingsLineViewModel", Context.MODE_PRIVATE)
    
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
    
    val isAllNotEmpty by remember { derivedStateOf { viewModel.isAllNotEmpty } }
    val isAllSelected by remember { derivedStateOf { viewModel.isAllSelected } }
    
    // Status bar setup
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = false
            }
        }
        onDispose { }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.routeSettings),
                        fontSize = ScreenSize.settingsTitleFontSize().value.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    CommonComponents.CustomBackButton(
                        onClick = {
                            if (!isLoadingBusStops && !isLoadingTimetable && !isLoadingLines) {
                                onNavigateBack()
                            }
                        },
                        foregroundColor = if (isLoadingBusStops || isLoadingTimetable || isLoadingLines) Color.White else Color.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding())
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
                            viewModel.filterOperators("")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Line Number Menu
                LineNumberMenu(
                    viewModel = viewModel,
                    selectedLineNumber = selectedLineNumber,
                    selectedTransportationKind = selectedTransportationKind,
                    onTransportationKindChanged = { isRailway ->
                        viewModel.switchTransportationKind(isRailway)
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Operator Input Section
                OperatorInputSection(
                    viewModel = viewModel,
                    operatorInput = operatorInput,
                    isOperatorFieldFocused = isOperatorFieldFocused,
                    focusRequester = operatorFocusRequester,
                    onOperatorInputChanged = { newValue ->
                        viewModel.processOperatorInput(newValue)
                    },
                    onFocusChanged = { isFocused ->
                        viewModel.isOperatorFieldFocused.value = isFocused
                        if (isFocused && operatorInput.isEmpty()) {
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.filterOperators("")
                            }
                        } else if (!isFocused) {
                            viewModel.showOperatorSuggestions.value = false
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
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
                        viewModel.isLineFieldFocused.value = isFocused
                        if (isFocused && lineInput.isEmpty() && viewModel.selectedOperatorCode.value != null && operatorSelected) {
                            viewModel.lineSelected.value = false
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.filterLine("")
                            }
                        } else if (!isFocused) {
                            viewModel.showLineSuggestions.value = false
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Line Color Section
                LineColorSection(
                    viewModel = viewModel,
                    selectedLineColor = selectedLineColor,
                    selectedLine = selectedLine,
                    onColorSelectClick = {
                        viewModel.showColorSelection.value = true
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Station Header Text
                StationHeaderText(
                    viewModel = viewModel,
                    selectedTransportationKind = selectedTransportationKind
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
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
                        viewModel.isDepartureFieldFocused.value = isFocused
                        if (isFocused && departureStopInput.isEmpty() && viewModel.selectedOperatorCode.value != null && operatorSelected && lineSelected) {
                            viewModel.departureStopSelected.value = false
                            viewModel.filterDepartureStops("")
                        } else if (!isFocused) {
                            viewModel.showDepartureSuggestions.value = false
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
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
                        viewModel.isArrivalFieldFocused.value = isFocused
                        if (isFocused && arrivalStopInput.isEmpty() && viewModel.selectedOperatorCode.value != null && operatorSelected && lineSelected) {
                            viewModel.arrivalStopSelected.value = false
                            viewModel.filterArrivalStops("")
                        } else if (!isFocused) {
                            viewModel.showArrivalSuggestions.value = false
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Time Header Text
                TimeHeaderText()
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Ride Time Section
                RideTimeSection(
                    viewModel = viewModel,
                    selectedRideTime = selectedRideTime,
                    onRideTimeChanged = { newValue ->
                        viewModel._selectedRideTime.value = newValue
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Transportation Settings Section (only for lines 1 and 2)
                if (selectedLineNumber < 3) {
                    TransportationSettingsSection(
                        viewModel = viewModel,
                        selectedTransportation = selectedTransportation,
                        onTransportationChanged = { newTransportation ->
                            viewModel.selectedTransportation.value = newTransportation
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                    
                    // Transfer Time Settings Section (only when transportation is not "none")
                    if (selectedTransportation != "none") {
                        TransferTimeSettingsSection(
                            viewModel = viewModel,
                            selectedTransferTime = selectedTransferTime,
                            onTransferTimeChanged = { newValue ->
                                viewModel.selectedTransferTime.value = newValue
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                    }
                }
                
                // Save Button Section
                SaveButtonSection(
                    viewModel = viewModel,
                    isAllNotEmpty = isAllNotEmpty,
                    onSave = {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.handleLineSave()
                            onNavigateBack()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Timetable Settings Button Section
                TimetableSettingsButtonSection(
                    viewModel = viewModel,
                    isAllNotEmpty = isAllNotEmpty,
                    onTimetableSettings = {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.handleLineSave()
                            showTimetableSettings = true
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                // Timetable Auto Settings Button Section
                TimetableAutoSettingsButtonSection(
                    viewModel = viewModel,
                    isAllNotEmpty = isAllNotEmpty,
                    isAllSelected = isAllSelected,
                    onAutoGenerate = {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.handleLineSave()
                            // TODO: Implement auto generate timetable
                            showTimetableSettings = true
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
            }
            
            // Operator Suggestions View
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
                        // TODO: Handle operator selection logic
                        lineFocusRequester.requestFocus()
                        viewModel.isLineFieldFocused.value = true
                    }
                )
            }
            
            // Line Suggestions View
            if (showLineSuggestions && lineSuggestions.isNotEmpty() && !isLineNumberChanging && !lineSelected && isLineFieldFocused) {
                LineSuggestionsView(
                    viewModel = viewModel,
                    lineSuggestions = lineSuggestions,
                    onLineSelected = { line ->
                        viewModel.selectLine(line)
                        if (line.lineColor == null) {
                            viewModel.showColorSelection.value = true
                        }
                        viewModel.showLineSuggestions.value = false
                        viewModel.lineSelected.value = true
                    }
                )
            }
            
            // Color Selection Section
            if (showColorSelection || (lineInput.isNotEmpty() && selectedLineColor == null && selectedLine?.lineColor == null && !lineSelected)) {
                ColorSelectionSection(
                    viewModel = viewModel,
                    onColorSelected = { color ->
                        viewModel.setLineColor(color)
                        viewModel.showColorSelection.value = false
                        departureFocusRequester.requestFocus()
                        viewModel.isDepartureFieldFocused.value = true
                    },
                    onCancel = {
                        viewModel.showColorSelection.value = false
                    }
                )
            }
            
            // Departure Stop Suggestions View
            if (showDepartureSuggestions && departureSuggestions.isNotEmpty() && lineSelected && isDepartureFieldFocused) {
                DepartureStopSuggestionsView(
                    viewModel = viewModel,
                    departureSuggestions = departureSuggestions,
                    onStopSelected = { stop ->
                        val arrivalDisplayName = viewModel.selectedArrivalStopState.value?.displayName(context) ?: ""
                        val isSameAsArrival = arrivalDisplayName == stop.displayName(context)
                        viewModel.departureStopInput.value = if (isSameAsArrival) "" else stop.displayName(context)
                        viewModel.setSelectedDepartureStop(if (isSameAsArrival) null else stop)
                        viewModel.showDepartureSuggestions.value = false
                        viewModel.isDepartureFieldFocused.value = false
                        viewModel.departureSuggestions.value = emptyList()
                        viewModel.departureStopSelected.value = true
                    }
                )
            }
            
            // Arrival Stop Suggestions View
            if (showArrivalSuggestions && arrivalSuggestions.isNotEmpty() && lineSelected && isArrivalFieldFocused) {
                ArrivalStopSuggestionsView(
                    viewModel = viewModel,
                    arrivalSuggestions = arrivalSuggestions,
                    onStopSelected = { stop ->
                        val departureDisplayName = viewModel.selectedDepartureStopState.value?.displayName(context) ?: ""
                        val isSameAsDeparture = departureDisplayName == stop.displayName(context)
                        viewModel.arrivalStopInput.value = if (isSameAsDeparture) "" else stop.displayName(context)
                        viewModel.setSelectedArrivalStop(if (isSameAsDeparture) null else stop)
                        viewModel.showArrivalSuggestions.value = false
                        viewModel.isArrivalFieldFocused.value = false
                        viewModel.arrivalSuggestions.value = emptyList()
                        viewModel.arrivalStopSelected.value = true
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
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ScreenSize.settingsSheetVerticalSpacing()),
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
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Black,
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
            icon = Icons.Default.Close,
            tintColor = Color.Red,
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
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Black,
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
            circleColor = Color.White,
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
    onFocusChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.operatorName),
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        OutlinedTextField(
            value = operatorInput,
            onValueChange = onOperatorInputChanged,
            placeholder = {
                Text(
                    text = stringResource(R.string.enterOperatorName),
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
                )
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gray,
                unfocusedBorderColor = Gray
            )
        )
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (operatorInput.isEmpty()) Gray else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize())
        )
    }
    
    LaunchedEffect(isOperatorFieldFocused) {
        onFocusChanged(isOperatorFieldFocused)
    }
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
    onFocusChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.lineName),
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        OutlinedTextField(
            value = lineInput,
            onValueChange = onLineInputChanged,
            placeholder = {
                Text(
                    text = if (selectedTransportationKind == TransportationLineKind.RAILWAY) {
                        stringResource(R.string.enterLineName)
                    } else {
                        stringResource(R.string.enterBusRouteName)
                    },
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
                )
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gray,
                unfocusedBorderColor = Gray
            )
        )
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (lineInput.isEmpty()) Gray else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize())
        )
    }
    
    LaunchedEffect(isLineFieldFocused) {
        onFocusChanged(isLineFieldFocused)
    }
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
    val color = selectedLineColor?.let { Color(android.graphics.Color.parseColor("#$it")) }
        ?: selectedLine?.lineColor?.let { Color(android.graphics.Color.parseColor("#$it")) }
        ?: Gray
    
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
                .background(color)
                .border(
                    width = ScreenSize.settingsSheetStrokeLineWidth(),
                    color = Primary,
                    shape = CircleShape
                )
                .clickable(onClick = onColorSelectClick)
                .padding(
                    horizontal = ScreenSize.settingsLineSheetColorHorizontalPadding(),
                    vertical = ScreenSize.settingsLineSheetColorVerticalPadding()
                )
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
    
    Text(
        text = headerText + stationInfo,
        fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.padding(top = ScreenSize.settingsSheetVerticalSpacing())
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
    onFocusChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selectedTransportationKind == TransportationLineKind.BUS) {
                stringResource(R.string.departureStop)
            } else {
                stringResource(R.string.departureStation)
            },
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        OutlinedTextField(
            value = departureStopInput,
            onValueChange = onDepartureStopInputChanged,
            placeholder = {
                Text(
                    text = if (selectedTransportationKind == TransportationLineKind.BUS) {
                        stringResource(R.string.enterDepartureStop)
                    } else {
                        stringResource(R.string.enterDepartureStation)
                    },
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
                )
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gray,
                unfocusedBorderColor = Gray
            )
        )
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (departureStopInput.isEmpty()) Gray else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize())
        )
    }
    
    LaunchedEffect(isDepartureFieldFocused) {
        onFocusChanged(isDepartureFieldFocused)
    }
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
    onFocusChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selectedTransportationKind == TransportationLineKind.BUS) {
                stringResource(R.string.arrivalStop)
            } else {
                stringResource(R.string.arrivalStation)
            },
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        OutlinedTextField(
            value = arrivalStopInput,
            onValueChange = onArrivalStopInputChanged,
            placeholder = {
                Text(
                    text = if (selectedTransportationKind == TransportationLineKind.BUS) {
                        stringResource(R.string.enterArrivalStop)
                    } else {
                        stringResource(R.string.enterArrivalStation)
                    },
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
                )
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gray,
                unfocusedBorderColor = Gray
            )
        )
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (arrivalStopInput.isEmpty()) Gray else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize())
        )
    }
    
    LaunchedEffect(isArrivalFieldFocused) {
        onFocusChanged(isArrivalFieldFocused)
    }
}

// MARK: - Time Header Text
// Time header with simple text
@Composable
private fun TimeHeaderText() {
    Text(
        text = stringResource(R.string.timeSettings),
        fontSize = ScreenSize.settingsSheetTitleFontSize().value.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.padding(top = ScreenSize.settingsSheetVerticalSpacing())
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
    val hasTimetableSupport by remember { derivedStateOf { viewModel.hasTimetableSupport() } }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        
        Box(
            modifier = Modifier.weight(1f)
        ) {
            // Display current ride time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScreenSize.settingsSheetPickerDisplayHeight())
                    .padding(
                        vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                        horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                    )
                    .background(
                        color = Gray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    )
                    .border(
                        width = ScreenSize.settingsSheetStrokeLineWidth(),
                        color = Gray,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedRideTime == 0) "-" else "$selectedRideTime ${stringResource(R.string.min)}",
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                    color = Color.Black
                )
            }
            
            // Custom2DigitPicker overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .padding(end = ScreenSize.settingsSheetInputPaddingHorizontal()),
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
            tint = if (selectedRideTime == 0 && !hasTimetableSupport) Gray else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize())
        )
    }
}

// MARK: - Transportation Settings Section
// Section for selecting transportation method for next transfer
@Composable
private fun TransportationSettingsSection(
    viewModel: SettingsLineViewModel,
    selectedTransportation: String,
    onTransportationChanged: (String) -> Unit
) {
    val transferType = getTransferType(selectedTransportation)
    val transferTypeDisplayName = getTransferTypeDisplayName(transferType)
    
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
        
        var expanded by remember { mutableStateOf(false) }
        
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                        horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                    )
                    .background(
                        color = Gray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    )
                    .border(
                        width = ScreenSize.settingsSheetStrokeLineWidth(),
                        color = Gray,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    )
                    .clickable { expanded = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = transferType.icon,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                    )
                    Text(
                        text = transferTypeDisplayName,
                        fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                        color = Color.Black
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TransferType.values().reversed().forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                                )
                                Text(
                                    text = getTransferTypeDisplayName(type),
                                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                                    color = Color.Black
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.transferTime),
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        Box(
            modifier = Modifier.weight(1f)
        ) {
            // Display current transfer time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScreenSize.settingsSheetPickerDisplayHeight())
                    .padding(
                        vertical = ScreenSize.settingsSheetInputPaddingVertical(),
                        horizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
                    )
                    .background(
                        color = Gray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    )
                    .border(
                        width = ScreenSize.settingsSheetStrokeLineWidth(),
                        color = Gray,
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTransferTime == 0) "-" else "$selectedTransferTime ${stringResource(R.string.min)}",
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                    color = Color.Black
                )
            }
            
            // Custom2DigitPicker overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .padding(end = ScreenSize.settingsSheetInputPaddingHorizontal()),
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
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize())
        )
    }
}

// MARK: - Save Button Section
// Save button for storing line configuration data
@Composable
private fun SaveButtonSection(
    viewModel: SettingsLineViewModel,
    isAllNotEmpty: Boolean,
    onSave: () -> Unit
) {
    CommonComponents.CustomButton(
        title = stringResource(R.string.inputSave),
        onClick = onSave,
        icon = Icons.Default.Save,
        backgroundColor = Accent,
        isEnabled = isAllNotEmpty,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ScreenSize.settingsSheetVerticalSpacing())
    )
}

// MARK: - Timetable Settings Button Section
// Button to open manual timetable configuration settings
@Composable
private fun TimetableSettingsButtonSection(
    viewModel: SettingsLineViewModel,
    isAllNotEmpty: Boolean,
    onTimetableSettings: () -> Unit
) {
    CommonComponents.CustomButton(
        title = stringResource(R.string.timetableSettings),
        onClick = onTimetableSettings,
        icon = if (isAllNotEmpty) Icons.Default.Schedule else Icons.Default.AccessTime,
        backgroundColor = if (isAllNotEmpty) Accent else Gray,
        isEnabled = isAllNotEmpty,
        modifier = Modifier.fillMaxWidth()
    )
}

// MARK: - Timetable Auto Settings Button Section
// Button to automatically generate timetable data
@Composable
private fun TimetableAutoSettingsButtonSection(
    viewModel: SettingsLineViewModel,
    isAllNotEmpty: Boolean,
    isAllSelected: Boolean,
    onAutoGenerate: () -> Unit
) {
    CommonComponents.CustomButton(
        title = stringResource(R.string.autoGenerateTimetable),
        onClick = onAutoGenerate,
        icon = if (isAllNotEmpty && isAllSelected) Icons.Default.Schedule else Icons.Default.AccessTime,
        backgroundColor = if (isAllNotEmpty && isAllSelected) Primary else Gray,
        isEnabled = isAllNotEmpty && isAllSelected,
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
    onOperatorSelected: (String) -> Unit
) {
    // TODO: Implement operator suggestions view
}

// MARK: - Line Suggestions View
// Dropdown list showing suggested lines
@Composable
private fun LineSuggestionsView(
    viewModel: SettingsLineViewModel,
    lineSuggestions: List<TransportationLine>,
    onLineSelected: (TransportationLine) -> Unit
) {
    // TODO: Implement line suggestions view
}

// MARK: - Color Selection Section
// Color picker overlay for selecting line colors
@Composable
private fun ColorSelectionSection(
    viewModel: SettingsLineViewModel,
    onColorSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    // TODO: Implement color selection section
}

// MARK: - Departure Stop Suggestions View
// Dropdown list showing suggested departure stops
@Composable
private fun DepartureStopSuggestionsView(
    viewModel: SettingsLineViewModel,
    departureSuggestions: List<TransportationStop>,
    onStopSelected: (TransportationStop) -> Unit
) {
    // TODO: Implement departure stop suggestions view
}

// MARK: - Arrival Stop Suggestions View
// Dropdown list showing suggested arrival stops
@Composable
private fun ArrivalStopSuggestionsView(
    viewModel: SettingsLineViewModel,
    arrivalSuggestions: List<TransportationStop>,
    onStopSelected: (TransportationStop) -> Unit
) {
    // TODO: Implement arrival stop suggestions view
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
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ScreenSize.splashLoadingSpacing())
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(ScreenSize.splashIconSize())
            )
            
            if (message != null) {
                Text(
                    text = message,
                    fontSize = ScreenSize.splashLoadingFontSize().value.sp,
                    color = Color.White
                )
            }
        }
    }
}

// MARK: - Helper Functions
// Get transfer type from string value
private fun getTransferType(value: String): TransferType {
    return TransferType.values().find { it.rawValue == value } ?: TransferType.NONE
}

// TransferType extension for icon and display name
private val TransferType.icon: androidx.compose.ui.graphics.vector.ImageVector
    get() = when (this) {
        TransferType.NONE -> Icons.Default.Close
        TransferType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
        TransferType.BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
        TransferType.CAR -> Icons.Default.DirectionsCar
    }

@Composable
private fun getTransferTypeDisplayName(type: TransferType): String {
    return when (type) {
        TransferType.NONE -> stringResource(R.string.none)
        TransferType.WALKING -> stringResource(R.string.walking)
        TransferType.BICYCLE -> stringResource(R.string.bicycle)
        TransferType.CAR -> stringResource(R.string.car)
    }
}

