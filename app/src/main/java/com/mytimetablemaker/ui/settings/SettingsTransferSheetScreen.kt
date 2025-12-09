package com.mytimetablemaker.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.models.TransferType
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.theme.*
import android.content.Context
import android.content.SharedPreferences
import com.mytimetablemaker.ui.common.CommonComponents.Custom2DigitPicker
import com.mytimetablemaker.ui.common.CommonComponents.CustomBackButton
import com.mytimetablemaker.ui.common.CommonComponents.CustomButton

// MARK: - Settings Transfer Sheet Screen
// Sheet view for configuring transfer time and transportation methods in settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTransferSheetScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val sharedPreferences = context.getSharedPreferences("SettingsTransferSheetViewModel", Context.MODE_PRIVATE)
    
    // Create ViewModel
    val viewModel = remember {
        SettingsTransferSheetViewModel(application, sharedPreferences)
    }
    
    // Observe ViewModel state
    val homeInput by viewModel.homeInput.collectAsState()
    val officeInput by viewModel.officeInput.collectAsState()
    val selectedHomeTransportation1 by viewModel.selectedHomeTransportation1.collectAsState()
    val selectedHomeTransportation2 by viewModel.selectedHomeTransportation2.collectAsState()
    val selectedOfficeTransportation1 by viewModel.selectedOfficeTransportation1.collectAsState()
    val selectedOfficeTransportation2 by viewModel.selectedOfficeTransportation2.collectAsState()
    val selectedHomeTransferTime1 by viewModel.selectedHomeTransferTime1.collectAsState()
    val selectedHomeTransferTime2 by viewModel.selectedHomeTransferTime2.collectAsState()
    val selectedOfficeTransferTime1 by viewModel.selectedOfficeTransferTime1.collectAsState()
    val selectedOfficeTransferTime2 by viewModel.selectedOfficeTransferTime2.collectAsState()
    val showRoute2 by viewModel.showRoute2.collectAsState()
    val isFormValid by remember { derivedStateOf { viewModel.isFormValid } }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.homeDestination),
                        fontSize = ScreenSize.settingsTitleFontSize().value.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    CustomBackButton(
                        onClick = onNavigateBack,
                        foregroundColor = Color.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding())
        ) {
            Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
            
            // Route 2 Toggle Section
            Route2ToggleSection(
                viewModel = viewModel,
                showRoute2 = showRoute2
            )
            
            Spacer(modifier = Modifier.height(ScreenSize.settingsTransferSheetVerticalSpacing()))
            
            // Header section - Setting departure point
            HeaderSection(title = stringResource(R.string.settingDeparturePoint))
            
            Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
            
            // Departure place input
            PlaceInputSection(
                title = stringResource(R.string.from),
                placeholder = stringResource(R.string.enterDeparturePoint),
                text = homeInput,
                onTextChange = { viewModel.homeInput.value = it }
            )
            
            Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
            
            // Departure transportation settings - Route 1
            RouteRow(
                routeTitle = if (showRoute2) stringResource(R.string.route1) else stringResource(R.string.route),
                selectedTransportation = selectedHomeTransportation1,
                transferTime = selectedHomeTransferTime1,
                onTransportationChanged = { viewModel.selectedHomeTransportation1.value = it },
                onTransferTimeChanged = { viewModel.selectedHomeTransferTime1.value = it }
            )
            
            // Route 2 row (only shown when showRoute2 is true)
            if (showRoute2) {
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                RouteRow(
                    routeTitle = stringResource(R.string.route2),
                    selectedTransportation = selectedHomeTransportation2,
                    transferTime = selectedHomeTransferTime2,
                    onTransportationChanged = { viewModel.selectedHomeTransportation2.value = it },
                    onTransferTimeChanged = { viewModel.selectedHomeTransferTime2.value = it }
                )
            }
            
            Spacer(modifier = Modifier.height(ScreenSize.settingsTransferSheetVerticalSpacing()))
            
            // Destination section header
            HeaderSection(title = stringResource(R.string.settingDestination))
            
            Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
            
            // Destination place input
            PlaceInputSection(
                title = stringResource(R.string.to),
                placeholder = stringResource(R.string.enterDestination),
                text = officeInput,
                onTextChange = { viewModel.officeInput.value = it }
            )
            
            Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
            
            // Destination transportation settings - Route 1
            RouteRow(
                routeTitle = if (showRoute2) stringResource(R.string.route1) else stringResource(R.string.route),
                selectedTransportation = selectedOfficeTransportation1,
                transferTime = selectedOfficeTransferTime1,
                onTransportationChanged = { viewModel.selectedOfficeTransportation1.value = it },
                onTransferTimeChanged = { viewModel.selectedOfficeTransferTime1.value = it }
            )
            
            // Route 2 row (only shown when showRoute2 is true)
            if (showRoute2) {
                Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
                
                RouteRow(
                    routeTitle = stringResource(R.string.route2),
                    selectedTransportation = selectedOfficeTransportation2,
                    transferTime = selectedOfficeTransferTime2,
                    onTransportationChanged = { viewModel.selectedOfficeTransportation2.value = it },
                    onTransferTimeChanged = { viewModel.selectedOfficeTransferTime2.value = it }
                )
            }
            
            Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
            
            // Save button
            SaveButtonSection(
                isFormValid = isFormValid,
                onSave = {
                    viewModel.saveSettings()
                    // TODO: Post notification to update MainContentView
                    onNavigateBack()
                }
            )
            
            Spacer(modifier = Modifier.height(ScreenSize.settingsSheetVerticalSpacing()))
        }
    }
    
    // Load settings on appear
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }
}

// MARK: - Route 2 Toggle Section
// Toggle to show/hide Route 2 configuration options
@Composable
private fun Route2ToggleSection(
    viewModel: SettingsTransferSheetViewModel,
    showRoute2: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        HeaderSection(title = stringResource(R.string.anotherRoute))
        
        // Route 2 toggle
        CustomToggle(
            isLeftSelected = !showRoute2,
            onToggle = { newValue ->
                viewModel.showRoute2.value = !newValue
                viewModel.saveRoute2Setting()
            },
            leftText = stringResource(R.string.hide),
            rightText = stringResource(R.string.display),
            primaryColor = Gray,
            secondaryColor = Primary,
            circleColor = Color.White,
            modifier = Modifier.wrapContentWidth()
        )
    }
}

@Composable
fun CustomToggle(
    isLeftSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    leftText: String,
    rightText: String,
    primaryColor: Color,
    secondaryColor: Color,
    circleColor: Color,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = ScreenSize.customTogglePaddingHorizontal())
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.customToggleSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left label
        Text(
            text = leftText,
            fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
            fontWeight = FontWeight.Medium,
            color = if (isLeftSelected) primaryColor else secondaryColor,
            maxLines = 1
        )
        
        // Toggle switch
        Box(
            modifier = Modifier
                .size(
                    width = ScreenSize.customToggleWidth(),
                    height = ScreenSize.customToggleHeight()
                )
                .background(
                    color = if (isLeftSelected) primaryColor else secondaryColor,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(ScreenSize.customToggleCornerRadius())
                )
                .clickable { onToggle(!isLeftSelected) },
            contentAlignment = Alignment.Center
        ) {
            // Toggle circle
            Box(
                modifier = Modifier
                    .size(ScreenSize.customToggleCircleSize())
                    .background(
                        color = circleColor,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .offset(
                        x = if (isLeftSelected) {
                            -ScreenSize.customToggleCircleOffset()
                        } else {
                            ScreenSize.customToggleCircleOffset()
                        },
                        y = 0.dp
                    )
            )
        }
        
        // Right label
        Text(
            text = rightText,
            fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
            fontWeight = FontWeight.Medium,
            color = if (isLeftSelected) secondaryColor else primaryColor,
            maxLines = 1
        )
    }
}

// MARK: - Header Section
// Header section with title text
@Composable
private fun HeaderSection(title: String) {
    Text(
        text = title,
        fontSize = ScreenSize.settingsSheetButtonFontSize().value.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}

// MARK: - Place Input Section
// Place input section with title, text field, and validation checkmark
@Composable
private fun PlaceInputSection(
    title: String,
    placeholder: String,
    text: String,
    onTextChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp
                )
            },
            modifier = Modifier.weight(1f),
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
            tint = if (text.isEmpty()) Gray else Accent,
            modifier = Modifier.size(ScreenSize.settingsSheetInputFontSize())
        )
    }
}

// MARK: - Route Row
// Route row component with transportation method, time selector, and checkmark
@Composable
private fun RouteRow(
    routeTitle: String,
    selectedTransportation: String,
    transferTime: Int,
    onTransportationChanged: (String) -> Unit,
    onTransferTimeChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = routeTitle,
            fontSize = ScreenSize.settingsSheetHeadlineFontSize().value.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.wrapContentWidth()
        )
        
        // Transportation method selector
        TransportationMethodSelector(
            selectedTransportation = selectedTransportation,
            onTransportationChanged = onTransportationChanged
        )
        
        // Time selector
        TimeSelector(
            selectedTime = transferTime,
            onTimeChanged = onTransferTimeChanged
        )
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (transferTime > 0) Accent else Gray,
            modifier = Modifier
                .size(ScreenSize.settingsSheetInputFontSize())
                .padding(top = ScreenSize.settingsTransferSheetCheckmarkSpacing())
        )
    }
}

// MARK: - Transportation Method Selector
// Menu-based selector for choosing transfer transportation type
@Composable
private fun TransportationMethodSelector(
    selectedTransportation: String,
    onTransportationChanged: (String) -> Unit
) {
    val transferType = getTransferType(selectedTransportation)
    val transferTypeDisplayName = getTransferTypeDisplayName(transferType)
    
    var expanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .width(ScreenSize.settingsTransferSheetPickerWidth())
            .height(ScreenSize.settingsSheetPickerDisplayHeight())
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
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
                horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetIconSpacing())
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
            TransferType.values().filter { it != TransferType.NONE }.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetIconSpacing())
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
}

// MARK: - Time Selector
// Time selector component using Custom2DigitPicker
@Composable
private fun TimeSelector(
    selectedTime: Int,
    onTimeChanged: (Int) -> Unit
) {
    Box {
        // Display current time value
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ScreenSize.settingsSheetPickerDisplayHeight())
                .padding(
                    top = ScreenSize.settingsSheetInputPaddingVertical(),
                    bottom = ScreenSize.settingsSheetInputPaddingVertical(),
                    start = ScreenSize.settingsTransferSheetPaddingLeft()
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
                text = if (selectedTime == 0) "-" else "$selectedTime ${stringResource(R.string.min)}",
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
            Custom2DigitPicker(
                value = selectedTime,
                onValueChange = onTimeChanged,
                isZeroToFive = false
            )
        }
    }
}

// MARK: - Save Button Section
// Button to save all transfer settings
@Composable
private fun SaveButtonSection(
    isFormValid: Boolean,
    onSave: () -> Unit
) {
    // Convert icon name to ImageVector
    val saveIcon = Icons.Filled.Save
    
    CommonComponents.CustomButton(
        title = stringResource(R.string.save),
        icon = saveIcon,
        backgroundColor = Accent,
        isEnabled = isFormValid,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ScreenSize.settingsSheetVerticalSpacing()),
        onClick = onSave
    )
}


// MARK: - Helper Functions
// Get transfer type from string value
private fun getTransferType(value: String): TransferType {
    return TransferType.values().find { it.rawValue == value } ?: TransferType.NONE
}

// Get transfer type display name
@Composable
private fun getTransferTypeDisplayName(type: TransferType): String {
    return when (type) {
        TransferType.NONE -> stringResource(R.string.none)
        TransferType.WALKING -> stringResource(R.string.walking)
        TransferType.BICYCLE -> stringResource(R.string.bicycle)
        TransferType.CAR -> stringResource(R.string.car)
    }
}

// TransferType extension for icon
private val TransferType.icon: androidx.compose.ui.graphics.vector.ImageVector
    get() = when (this) {
        TransferType.NONE -> Icons.Default.Close
        TransferType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
        TransferType.BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
        TransferType.CAR -> Icons.Default.DirectionsCar
    }

