package com.mytimetablemaker.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import kotlinx.coroutines.launch
import kotlin.math.max
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mytimetablemaker.ui.theme.*

// MARK: - Common Components Object
// Container for reusable UI components
object CommonComponents {
    
    // MARK: - Custom Button
    // Custom button component for consistent styling
    @Composable
    fun CustomButton(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        backgroundColor: Color = Accent,
        textColor: Color = White,
        isEnabled: Boolean = true,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            enabled = isEnabled,
            modifier = modifier
                .fillMaxWidth()
                .height(ScreenSize.settingsSheetButtonHeight()),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) backgroundColor else Gray,
                disabledContainerColor = Gray,
                contentColor = textColor,
                disabledContentColor = textColor
            ),
            shape = RoundedCornerShape(ScreenSize.settingsSheetButtonCornerRadius()),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(ScreenSize.settingsSheetIconSize()),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(ScreenSize.settingsSheetIconSpacing()))
                }
                Text(
                    text = title,
                    fontSize = ScreenSize.settingsSheetButtonFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
    
    // MARK: - Custom Back Button
    // Reusable back button component with consistent styling
    @Composable
    fun CustomBackButton(
        foregroundColor: Color = White,
        onClick: () -> Unit
    ) {
        TextButton(onClick = onClick) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = foregroundColor,
                    modifier = Modifier.size(ScreenSize.settingsHeaderIconSize())
                )
                Spacer(modifier = Modifier.width(ScreenSize.settingsHeaderIconSpace()))
                Text(
                    text = stringResource(R.string.backToHomepage),
                    fontSize = ScreenSize.settingsHeaderFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = foregroundColor
                )
            }
        }
    }
    
    // MARK: - Custom Toggle
    // Customizable toggle component with left/right text and Switch
    @Composable
    fun CustomToggle(
        isLeftSelected: Boolean,
        onToggle: (Boolean) -> Unit,
        leftText: String,
        leftColor: Color,
        rightText: String,
        rightColor: Color,
        circleColor: Color = White,
        offColor: Color = Gray
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.customToggleSpacing()),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = ScreenSize.customTogglePaddingHorizontal())
                .wrapContentWidth()
        ) {
            // Left label
            val leftLabelColor = animateColorAsState(
                targetValue = if (isLeftSelected) leftColor else offColor,
                animationSpec = tween(durationMillis = 200),
                label = "leftLabelColor"
            )
            Text(
                text = leftText,
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                fontWeight = FontWeight.Medium,
                color = leftLabelColor.value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .wrapContentWidth()
                    // Enable clicking on the text to switch to the left option
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Disable ripple effect
                    ) { onToggle(true) }
            )
            
            // Switch component
            // Material3 Switch has a default height, scale it to match customToggleHeight
            val defaultSwitchHeight = ScreenSize.customSwitchDefaultHeight()
            val targetHeight = ScreenSize.customToggleHeight()
            val scaleFactor = targetHeight.value / defaultSwitchHeight.value
            
            Box(
                modifier = Modifier.height(targetHeight),
                contentAlignment = Alignment.Center
            ) {
                Switch(
                    checked = !isLeftSelected, // Switch is checked when right is selected
                    onCheckedChange = { checked ->
                        onToggle(!checked) // Invert because Switch checked means right selected
                    },
                    modifier = Modifier.scale(scaleX = scaleFactor, scaleY = scaleFactor),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = White,
                        uncheckedTrackColor = Primary,
                    )
                )
            }
            
            // Right label
            val rightLabelColor = animateColorAsState(
                targetValue = if (isLeftSelected) offColor else rightColor,
                animationSpec = tween(durationMillis = 200),
                label = "rightLabelColor"
            )
            Text(
                text = rightText,
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                fontWeight = FontWeight.Medium,
                color = rightLabelColor.value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .wrapContentWidth()
                    // Enable clicking on the text to switch to the right option
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Disable ripple effect
                    ) { onToggle(false) }
            )
        }
    }
    
    // MARK: - Custom Rectangle Button
    // Custom rectangle button component for consistent styling
    // Used for buttons that don't need to fill the full width
    @Composable
    fun CustomRectangleButton(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        tintColor: Color = Accent,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.height(ScreenSize.settingsSheetRectangleButtonHeight()),
            colors = ButtonDefaults.buttonColors(
                containerColor = tintColor,
                contentColor = White
            ),
            contentPadding = PaddingValues(
                horizontal = ScreenSize.settingsSheetRectangleButtonPaddingHorizontal(),
                vertical = ScreenSize.settingsSheetRectangleButtonPaddingVertical()
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    if (icon != null) ScreenSize.settingsSheetIconSpacing() else 0.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                    )
                }
                Text(
                    text = title,
                    fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    // MARK: - Custom 2 Digit Picker
    // Picker component for selecting two-digit numbers with separate tens and ones place
    @Composable
    fun Custom2DigitPicker(
        value: Int,
        onValueChange: (Int) -> Unit,
        isZeroToFive: Boolean = false
    ) {
        val minValue = 0
        val maxValue = if (isZeroToFive) 59 else 99
        val tensDigit = value / 10
        val onesDigit = value % 10
        
        val tensRange = (0)..(maxValue / 10)
        val tensList = tensRange.toList()
        val onesRange = when (tensDigit) {
            0 -> { (max(0, 0))..9 }
            maxValue / 10 -> { 0..(maxValue % 10) }
            else -> { 0..9 }
        }
        val onesList = onesRange.toList()
        
        val tensSelectedIndex = tensList.indexOf(tensDigit).coerceAtLeast(0)
        val onesSelectedIndex = onesList.indexOf(onesDigit).coerceAtLeast(0)
        
        val tensListState = rememberLazyListState(initialFirstVisibleItemIndex = tensSelectedIndex.coerceIn(0, tensList.size - 1))
        val onesListState = rememberLazyListState(initialFirstVisibleItemIndex = onesSelectedIndex.coerceIn(0, onesList.size - 1))
        val coroutineScope = rememberCoroutineScope()
        
        val pickerItemPaddingVertical = ScreenSize.settingsSheetPickerItemPaddingVertical()
        val pickerHeight = ScreenSize.settingsSheetPickerSelectHeight()
        val itemHeight = pickerHeight / 3f
        val centerPadding = (pickerHeight - itemHeight) / 2f
        val textPadding = pickerItemPaddingVertical * 0.5f
        
        var tensLastSelectedIndex by remember { mutableIntStateOf(tensSelectedIndex) }
        var onesLastSelectedIndex by remember { mutableIntStateOf(onesSelectedIndex) }
        var tensIsProgrammaticScroll by remember { mutableStateOf(false) }
        var onesIsProgrammaticScroll by remember { mutableStateOf(false) }
        var tensWasUserScrolling by remember { mutableStateOf(false) }
        var onesWasUserScrolling by remember { mutableStateOf(false) }
        
        LaunchedEffect(tensSelectedIndex) {
            if (tensSelectedIndex != tensLastSelectedIndex) {
                tensLastSelectedIndex = tensSelectedIndex
                tensIsProgrammaticScroll = true
                coroutineScope.launch {
                    tensListState.animateScrollToItem(
                        index = tensSelectedIndex,
                        scrollOffset = -centerPadding.value.toInt()
                    )
                    kotlinx.coroutines.delay(100)
                    tensIsProgrammaticScroll = false
                }
            }
        }
        
        LaunchedEffect(onesSelectedIndex) {
            if (onesSelectedIndex != onesLastSelectedIndex) {
                onesLastSelectedIndex = onesSelectedIndex
                onesIsProgrammaticScroll = true
                coroutineScope.launch {
                    onesListState.animateScrollToItem(
                        index = onesSelectedIndex,
                        scrollOffset = -centerPadding.value.toInt()
                    )
                    kotlinx.coroutines.delay(100)
                    onesIsProgrammaticScroll = false
                }
            }
        }
        
        LaunchedEffect(tensListState.isScrollInProgress) {
            if (tensListState.isScrollInProgress) {
                if (!tensIsProgrammaticScroll) {
                    tensWasUserScrolling = true
                }
            } else if (tensWasUserScrolling && !tensIsProgrammaticScroll) {
                tensWasUserScrolling = false
                val layoutInfo = tensListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset / 2
                    val centerItem = visibleItems.minByOrNull { 
                        kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) 
                    }
                    val centerIndex = centerItem?.index ?: tensListState.firstVisibleItemIndex
                    
                    if (centerIndex != tensSelectedIndex && centerIndex in tensList.indices) {
                        val newTens = tensList[centerIndex]
                        val newValue = newTens * 10 + onesDigit
                        if (newValue in minValue..maxValue) {
                            onValueChange(newValue)
                        }
                    }
                }
            }
        }
        
        LaunchedEffect(onesListState.isScrollInProgress) {
            if (onesListState.isScrollInProgress) {
                if (!onesIsProgrammaticScroll) {
                    onesWasUserScrolling = true
                }
            } else if (onesWasUserScrolling && !onesIsProgrammaticScroll) {
                onesWasUserScrolling = false
                val layoutInfo = onesListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset / 2
                    val centerItem = visibleItems.minByOrNull { 
                        kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) 
                    }
                    val centerIndex = centerItem?.index ?: onesListState.firstVisibleItemIndex
                    
                    if (centerIndex != onesSelectedIndex && centerIndex in onesList.indices) {
                        val newOnes = onesList[centerIndex]
                        val newValue = tensDigit * 10 + newOnes
                        if (newValue in minValue..maxValue) {
                            onValueChange(newValue)
                        }
                    }
                }
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetPickerSpacing()),
            modifier = Modifier.height(ScreenSize.settingsSheetPickerSelectHeight())
        ) {
            // Tens digit picker
            Box(
                modifier = Modifier
                    .width(ScreenSize.settingsSheetPickerSelectWidth())
                    .height(ScreenSize.settingsSheetPickerSelectHeight())
            ) {
                LazyColumn(
                    state = tensListState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = centerPadding)
                ) {
                    items(tensList.size) { index ->
                        val item = tensList[index]
                        val isSelected = index == tensSelectedIndex
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clickable {
                                    coroutineScope.launch {
                                        tensListState.animateScrollToItem(
                                            index = index,
                                            scrollOffset = -centerPadding.value.toInt()
                                        )
                                        val newTens = tensList[index]
                                        val newValue = newTens * 10 + onesDigit
                                        if (newValue in minValue..maxValue) {
                                            onValueChange(newValue)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.toString(),
                                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Black else Gray
                            )
                        }
                    }
                }
            }
            
            // Ones digit picker
            Box(
                modifier = Modifier
                    .width(ScreenSize.settingsSheetPickerSelectWidth())
                    .height(ScreenSize.settingsSheetPickerSelectHeight())
            ) {
                LazyColumn(
                    state = onesListState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = centerPadding)
                ) {
                    items(onesList.size) { index ->
                        val item = onesList[index]
                        val isSelected = index == onesSelectedIndex
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clickable {
                                    coroutineScope.launch {
                                        onesListState.animateScrollToItem(
                                            index = index,
                                            scrollOffset = -centerPadding.value.toInt()
                                        )
                                        val newOnes = onesList[index]
                                        val newValue = tensDigit * 10 + newOnes
                                        if (newValue in minValue..maxValue) {
                                            onValueChange(newValue)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.toString(),
                                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Black else Gray
                            )
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Custom Tag
    // Small tag display component for showing metadata
    @Composable
    fun CustomTag(
        text: String,
        backgroundColor: Color? = null,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .size(ScreenSize.settingsSheetLineTagSize())
                .clip(CircleShape)
                .background(
                    backgroundColor?.copy(alpha = 0.5f) ?: Gray.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = ScreenSize.settingsLineSheetCaptionFontSize().value.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
    
    // MARK: - Custom Dropdown
    // Custom dropdown component that doesn't interfere with IME
    @Composable
    fun <T> CustomDropdown(
        items: List<T>,
        onItemSelected: (T) -> Unit,
        onDismissRequest: () -> Unit,
        itemContent: @Composable (T) -> Unit,
        modifier: Modifier = Modifier,
        maxHeight: androidx.compose.ui.unit.Dp = ScreenSize.settingsLineSheetSuggestionItemHeight() * 7f,
        backgroundColor: Color = LightGray,
        offsetX: androidx.compose.ui.unit.Dp = ScreenSize.settingsLineSheetDropdownOffsetX(),
        offsetY: androidx.compose.ui.unit.Dp = 0.dp, // Position directly below the text field
        itemHeight: androidx.compose.ui.unit.Dp = ScreenSize.settingsLineSheetSuggestionItemHeight()
    ) {
        Box(
            modifier = modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .offset(x = offsetX, y = offsetY)
                .focusable(false) // Prevent focus stealing
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .heightIn(max = maxHeight)
                    .background(backgroundColor)
                    .clip(RoundedCornerShape(ScreenSize.settingsSheetCornerRadius()))
                    .focusable(false),
                color = backgroundColor,
                shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
            ) {
                LazyColumn(
                    modifier = Modifier
                        .wrapContentWidth()
                        .focusable(false),
                    contentPadding = PaddingValues(vertical = ScreenSize.settingsSheetDropdownContentPaddingVertical())
                ) {
                    items(
                        count = items.size,
                        key = { index -> index }
                    ) { index ->
                        val item = items[index]
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(itemHeight)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onItemSelected(item)
                                }
                                .padding(horizontal = ScreenSize.settingsSheetInputPaddingHorizontal())
                                .focusable(false),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            itemContent(item)
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Custom Background
    // Custom background for input fields
    @Composable
    fun CustomBackground(
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(ScreenSize.settingsSheetCornerRadius()))
                .background(backgroundColor.copy(alpha = 0.95f))
        )
    }
    
    // MARK: - Custom Border
    // Custom border for input fields
    @Composable
    fun CustomBorder(
        borderColor: Color = MaterialTheme.colorScheme.outline,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(ScreenSize.settingsSheetCornerRadius()))
                .border(
                    width = ScreenSize.settingsSheetStrokeLineWidth(),
                    color = borderColor,
                    shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                )
        )
    }
    
    // MARK: - Custom TextField
    // Custom text field component with consistent styling
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomTextField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        modifier: Modifier = Modifier,
        focusRequester: FocusRequester? = null,
        title: String? = null,
        isCheckmarkValid: Boolean? = null,
        onFocusChanged: ((Boolean) -> Unit)? = null,
        visualTransformation: VisualTransformation = VisualTransformation.None,
        keyboardType: KeyboardType = KeyboardType.Text,
        trailingIcon: @Composable (() -> Unit)? = null
    ) {
        val inputFontSize = ScreenSize.settingsSheetInputFontSize()
        val headlineFontSize = ScreenSize.settingsSheetHeadlineFontSize()
        val horizontalSpacing = ScreenSize.settingsSheetHorizontalSpacing()
        val verticalPadding = ScreenSize.customTextFieldPaddingVertical()
        
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = headlineFontSize.value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.wrapContentWidth()
                )
            }
            
            val interactionSource = remember { MutableInteractionSource() }
            val textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = inputFontSize.value.sp,
                color = Black
            )
            
            // Monitor focus state
            var isFocused by remember { mutableStateOf(false) }
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        }
                    )
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        onFocusChanged?.invoke(focusState.isFocused)
                    },
                singleLine = true,
                textStyle = textStyle,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    autoCorrectEnabled = false
                )
            ) { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    placeholder = {
                        Text(
                            text = placeholder,
                            fontSize = inputFontSize.value.sp,
                            color = Color(0xFF9C9C9C),
                        )
                    },
                    trailingIcon = trailingIcon,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = White.copy(alpha = 0.95f),
                        unfocusedContainerColor = White.copy(alpha = 0.95f),
                        focusedTextColor = Primary,
                        unfocusedTextColor = Primary,
                        focusedPlaceholderColor = Gray,
                        unfocusedPlaceholderColor = Gray
                    ),
                    contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                        top = verticalPadding,
                        bottom = verticalPadding,
                    ),
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
            
            if (isCheckmarkValid != null) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isCheckmarkValid) Accent else Gray,
                    modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                )
            }
        }
    }
    
    // MARK: - Custom Login TextField
    // Custom text field component for login screens with password visibility toggle.
    // Uses OutlinedTextField so that visualTransformation (password mask) is applied correctly.
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomLoginTextField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        modifier: Modifier = Modifier,
        focusRequester: FocusRequester? = null,
        keyboardType: KeyboardType = KeyboardType.Text,
        onFocusChanged: ((Boolean) -> Unit)? = null,
        isPassword: Boolean = false
    ) {
        val inputFontSize = ScreenSize.settingsSheetInputFontSize()
        
        // Password visibility: false = masked (***), true = plain text
        var isPasswordVisible by remember { mutableStateOf(false) }
        
        val visualTransformation = if (isPassword && !isPasswordVisible)
            PasswordVisualTransformation() else
            VisualTransformation.None

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .then(modifier)
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                )
                .onFocusChanged { focusState ->
                    onFocusChanged?.invoke(focusState.isFocused)
                },
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = inputFontSize.value.sp,
                    lineHeight = (inputFontSize.value * 1.2f).sp,
                    color = Gray,
                )
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = inputFontSize.value.sp,
                color = Primary
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                autoCorrectEnabled = false
            ),
            visualTransformation = visualTransformation,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = Gray,
                            modifier = Modifier.size(ScreenSize.loginEyeIconSize())
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White.copy(alpha = 0.95f),
                unfocusedContainerColor = White.copy(alpha = 0.95f),
                focusedTextColor = Primary,
                unfocusedTextColor = Primary,
                focusedPlaceholderColor = Gray,
                unfocusedPlaceholderColor = Gray,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius()),
        )
    }
}
