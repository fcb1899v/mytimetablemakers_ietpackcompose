package com.mytimetablemaker.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.models.TransferType
import com.mytimetablemaker.models.displayName
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.theme.*
import android.content.Context
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import com.mytimetablemaker.ui.common.CommonComponents.Custom2DigitPicker
import com.mytimetablemaker.ui.common.CommonComponents.CustomBackButton

// MARK: - Settings Transfer Sheet Screen
// Sheet view for configuring transfer time and transportation methods in settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTransferSheetScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    // Use the same SharedPreferences instance as MainViewModel to ensure listeners work
    val sharedPreferences = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
    
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
    
    // Calculate form validity based on all required fields
    val isFormValid by remember(
        homeInput,
        officeInput,
        selectedHomeTransferTime1,
        selectedOfficeTransferTime1,
        selectedHomeTransferTime2,
        selectedOfficeTransferTime2,
        showRoute2
    ) {
        derivedStateOf {
            // Validate departure point, destination, and transfer times
            val basicValidation = homeInput.isNotEmpty() &&
                    officeInput.isNotEmpty() &&
                    selectedHomeTransferTime1 > 0 &&
                    selectedOfficeTransferTime1 > 0
            
            // If Route 2 is shown, also validate Route 2 fields
            if (showRoute2) {
                basicValidation &&
                        selectedHomeTransferTime2 > 0 &&
                        selectedOfficeTransferTime2 > 0
            } else {
                basicValidation
            }
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
                    CustomBackButton(
                        onClick = onNavigateBack,
                        foregroundColor = Black
                    )
                }
                
                // Title centered on screen
                Text(
                    text = stringResource(R.string.homeDestination),
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
                    .padding(horizontal = horizontalPadding)
            ) {
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Route 2 Toggle Section
                Route2ToggleSection(
                    viewModel = viewModel,
                    showRoute2 = showRoute2
                )

                Spacer(modifier = Modifier.height(verticalSpacing))

                // Header section - Setting departure point
                HeaderSection(title = stringResource(R.string.settingDeparturePoint))
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Departure place input
                PlaceInputSection(
                    title = stringResource(R.string.from),
                    placeholder = stringResource(R.string.enterDeparturePoint),
                    text = homeInput,
                    onTextChange = { viewModel.homeInput.value = it }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Departure transportation settings - Route 1
                TransportationSettingsSection(
                    routeTitle = if (showRoute2) stringResource(R.string.route1) else stringResource(R.string.route),
                    selectedTransportation = selectedHomeTransportation1,
                    onTransportationChanged = { viewModel.selectedHomeTransportation1.value = it }
                )
                
                // Transfer Time Settings Section (only when transportation is not "none")
                TransferTimeSettingsSection(
                    transferTime = selectedHomeTransferTime1,
                    onTransferTimeChanged = { viewModel.selectedHomeTransferTime1.value = it }
                )

                // Route 2 row (only shown when showRoute2 is true)
                if (showRoute2) {
                    TransportationSettingsSection(
                        routeTitle = stringResource(R.string.route2),
                        selectedTransportation = selectedHomeTransportation2,
                        onTransportationChanged = { viewModel.selectedHomeTransportation2.value = it }
                    )
                    
                    // Transfer Time Settings Section (only when transportation is not "none")
                    TransferTimeSettingsSection(
                        transferTime = selectedHomeTransferTime2,
                        onTransferTimeChanged = { viewModel.selectedHomeTransferTime2.value = it }
                    )
                }

                // Destination section header
                HeaderSection(title = stringResource(R.string.settingDestination))
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Destination place input
                PlaceInputSection(
                    title = stringResource(R.string.to),
                    placeholder = stringResource(R.string.enterDestination),
                    text = officeInput,
                    onTextChange = { viewModel.officeInput.value = it }
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Destination transportation settings - Route 1
                TransportationSettingsSection(
                    routeTitle = if (showRoute2) stringResource(R.string.route1) else stringResource(R.string.route),
                    selectedTransportation = selectedOfficeTransportation1,
                    onTransportationChanged = { viewModel.selectedOfficeTransportation1.value = it }
                )
                
                // Transfer Time Settings Section (only when transportation is not "none")
                TransferTimeSettingsSection(
                    transferTime = selectedOfficeTransferTime1,
                    onTransferTimeChanged = { viewModel.selectedOfficeTransferTime1.value = it }
                )

                // Route 2 row (only shown when showRoute2 is true)
                if (showRoute2) {
                    TransportationSettingsSection(
                        routeTitle = stringResource(R.string.route2),
                        selectedTransportation = selectedOfficeTransportation2,
                        onTransportationChanged = { viewModel.selectedOfficeTransportation2.value = it }
                    )
                    
                    // Transfer Time Settings Section (only when transportation is not "none")
                    TransferTimeSettingsSection(
                        transferTime = selectedOfficeTransferTime2,
                        onTransferTimeChanged = { viewModel.selectedOfficeTransferTime2.value = it }
                    )
                }

                // Save button
                //          NotificationCenter.default.post(name: NSNotification.Name("SettingsTransferUpdated"), object: nil)
                //          dismiss()
                // Note: saveSettings() already posts the notification via SharedPreferences
                SaveButtonSection(
                    isFormValid = isFormValid,
                    onSave = {
                        viewModel.saveSettings()
                        // Notification is posted in saveSettings() via SharedPreferences
                        // MainContentScreen listens for "SettingsTransferUpdated" key changes
                        onNavigateBack()
                    }
                )
            }
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
        // Save only when save button is pressed, not immediately on toggle change
        CommonComponents.CustomToggle(
            isLeftSelected = !showRoute2,
            onToggle = { newValue ->
                viewModel.showRoute2.value = !newValue
            },
            leftText = stringResource(R.string.hide),
            rightText = stringResource(R.string.display),
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
        color = Black
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
    CommonComponents.CustomTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
        focusRequester = null,
        title = title,
        isCheckmarkValid = text.isNotEmpty()
    )
}

// MARK: - Transportation Settings Section
// Section for selecting transportation method for next transfer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportationSettingsSection(
    routeTitle: String,
    selectedTransportation: String,
    onTransportationChanged: (String) -> Unit
) {
    // Ensure selectedTransportation is not "none" or empty, default to "walking"
    val effectiveTransportation = remember(selectedTransportation) {
        if (selectedTransportation.isEmpty() || selectedTransportation == "none") {
            TransferType.WALKING.rawValue
        } else {
            selectedTransportation
        }
    }
    
    // Update parent if needed
    LaunchedEffect(effectiveTransportation) {
        if (effectiveTransportation != selectedTransportation) {
            onTransportationChanged(effectiveTransportation)
        }
    }
    
    val transferType = getTransferType(effectiveTransportation)
    val transferTypeDisplayName = transferType.displayName()
    
    var expanded by remember { mutableStateOf(false) }
    
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
                    .background(color = LightGray.copy(alpha = 0.85f))
                    .offset(
                        y = ScreenSize.settingsLineSheetTransportationDropdownOffsetY(),
                    )
            ) {
                TransferType.entries.reversed().filter { it != TransferType.NONE }.forEach { type ->
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
    transferTime: Int,
    onTransferTimeChanged: (Int) -> Unit
) {
    val pickerPadding = ScreenSize.settingsLineSheetPickerPadding()
    val horizontalSpacing = ScreenSize.settingsSheetHorizontalSpacing()
    val headlineFontSize = ScreenSize.settingsSheetHeadlineFontSize()
    val paddingHorizontal = ScreenSize.settingsSheetInputPaddingHorizontal()
    
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
            text = if (transferTime == 0) "-" else "$transferTime ${stringResource(R.string.min)}",
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
                Custom2DigitPicker(
                    value = transferTime,
                    onValueChange = onTransferTimeChanged,
                    isZeroToFive = false
                )
            }
        }
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (transferTime > 0) Accent else Gray,
            modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
        )
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
    val found = TransferType.entries.find { it.rawValue == value }
    // If value is "none" or empty, default to WALKING
    return if (found == null || found == TransferType.NONE) TransferType.WALKING else found
}


