package com.mytimetablemaker.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.unit.Dp
import com.mytimetablemaker.ui.theme.*

// Shared UI components for consistent styling.
object CommonComponents {
    
    // Custom button with consistent styling.
    @Composable
    fun CustomButton(
        title: String,
        modifier: Modifier = Modifier,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        backgroundColor: Color = Accent,
        textColor: Color = White,
        isEnabled: Boolean = true,
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

    // Text with a simple 8-direction shadow stack.
    @Composable
    fun CustomShadowText(
        text: String,
        fontSize: androidx.compose.ui.unit.TextUnit,
        fontWeight: FontWeight,
        textColor: Color,
        shadowColor: Color,
        shadowOffset: Dp,
        modifier: Modifier = Modifier,
        maxLines: Int = 1,
        overflow: TextOverflow = TextOverflow.Ellipsis
    ) {
        val shadowOffsets = listOf(
            shadowOffset to 0.dp,
            -shadowOffset to 0.dp,
            0.dp to shadowOffset,
            0.dp to -shadowOffset,
            shadowOffset to shadowOffset,
            shadowOffset to -shadowOffset,
            -shadowOffset to shadowOffset,
            -shadowOffset to -shadowOffset
        )
        shadowOffsets.forEach { (offsetX, offsetY) ->
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = shadowColor,
                maxLines = maxLines,
                modifier = modifier.offset(x = offsetX, y = offsetY)
            )
        }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
    }
    
    // Reusable back button.
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
    
    // Toggle with labels and custom segmented control (SwiftUI-style pill with sliding circle).
    @Composable
    fun CustomToggle(
        isLeftSelected: Boolean,
        onToggle: (Boolean) -> Unit,
        leftText: String,
        rightText: String,
        onColor: Color = Primary,
        offColor: Color = Gray,
        backgroundColor: Color = Primary,
        circleColor: Color = White,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.customToggleSpacing()),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = ScreenSize.customTogglePaddingHorizontal())
                .wrapContentWidth()
        ) {
            // Left label (visual only, matches SwiftUI - only center switch is tappable)
            val leftLabelColor = animateColorAsState(
                targetValue = if (isLeftSelected) onColor else offColor,
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
                modifier = Modifier.wrapContentWidth()
            )
            
            // Switch component
            val defaultSwitchHeight = ScreenSize.customSwitchDefaultHeight()
            val targetHeight = ScreenSize.customToggleHeight()
            val scaleFactor = targetHeight.value / defaultSwitchHeight.value
            Box(
                modifier = Modifier.height(targetHeight),
                contentAlignment = Alignment.Center
            ) {
                Switch(
                    checked = !isLeftSelected,
                    onCheckedChange = { checked -> onToggle(!checked) },
                    modifier = Modifier.scale(scaleX = scaleFactor, scaleY = scaleFactor),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = circleColor,
                        checkedTrackColor = backgroundColor,
                        uncheckedThumbColor = circleColor,
                        uncheckedTrackColor = backgroundColor,
                    )
                )
            }
            
            // Right label (visual only, matches SwiftUI - only center switch is tappable)
            val rightLabelColor = animateColorAsState(
                targetValue = if (isLeftSelected) offColor else onColor,
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
                modifier = Modifier.wrapContentWidth()
            )
        }
    }
    
    // Rectangle button for compact actions.
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
    
    // Two-digit picker with tens/ones columns.
    @Composable
    @OptIn(ExperimentalFoundationApi::class)
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
        
        val pickerHeight = ScreenSize.settingsSheetPickerSelectHeight()
        val itemHeight = pickerHeight / 3f
        val centerPadding = (pickerHeight - itemHeight) / 2f

        var tensLastSelectedIndex by remember { mutableIntStateOf(tensSelectedIndex) }
        var onesLastSelectedIndex by remember { mutableIntStateOf(onesSelectedIndex) }
        var tensIsProgrammaticScroll by remember { mutableStateOf(false) }
        var onesIsProgrammaticScroll by remember { mutableStateOf(false) }
        var tensWasUserScrolling by remember { mutableStateOf(false) }
        var onesWasUserScrolling by remember { mutableStateOf(false) }

        val tensFlingBehavior = rememberSnapFlingBehavior(lazyListState = tensListState)
        val onesFlingBehavior = rememberSnapFlingBehavior(lazyListState = onesListState)

        val isTensClickEnabled = !tensIsProgrammaticScroll && !tensWasUserScrolling
        val isOnesClickEnabled = !onesIsProgrammaticScroll && !onesWasUserScrolling

        val scrollToTensIndex: (Int) -> Unit = scrollToTensIndex@{ index ->
            if (index !in tensList.indices) return@scrollToTensIndex
            tensIsProgrammaticScroll = true
            coroutineScope.launch {
                tensListState.animateScrollToItem(index = index, scrollOffset = 0)
                val newValue = tensList[index] * 10 + onesDigit
                kotlinx.coroutines.delay(100)
                tensIsProgrammaticScroll = false
                if (newValue in minValue..maxValue && newValue != value) {
                    onValueChange(newValue)
                }
            }
        }
        val scrollToOnesIndex: (Int) -> Unit = scrollToOnesIndex@{ index ->
            if (index !in onesList.indices) return@scrollToOnesIndex
            onesIsProgrammaticScroll = true
            coroutineScope.launch {
                onesListState.animateScrollToItem(index = index, scrollOffset = 0)
                val newValue = tensDigit * 10 + onesList[index]
                kotlinx.coroutines.delay(100)
                onesIsProgrammaticScroll = false
                if (newValue in minValue..maxValue && newValue != value) {
                    onValueChange(newValue)
                }
            }
        }

        LaunchedEffect(tensDigit, onesList) {
            if (onesDigit !in onesList) {
                val fallbackOnes = onesList.firstOrNull() ?: return@LaunchedEffect
                onValueChange(tensDigit * 10 + fallbackOnes)
            }
        }
        
        LaunchedEffect(tensSelectedIndex) {
            if (tensSelectedIndex != tensLastSelectedIndex) {
                tensLastSelectedIndex = tensSelectedIndex
                tensIsProgrammaticScroll = true
                coroutineScope.launch {
                    tensListState.animateScrollToItem(
                        index = tensLastSelectedIndex,
                        scrollOffset = 0
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
                        index = onesLastSelectedIndex,
                        scrollOffset = 0
                    )
                    kotlinx.coroutines.delay(100)
                    onesIsProgrammaticScroll = false
                }
            }
        }
        
        LaunchedEffect(tensListState.isScrollInProgress) {
            val wasUserScrolling = tensWasUserScrolling
            val isUserScrolling = tensListState.isScrollInProgress && !tensIsProgrammaticScroll
            tensWasUserScrolling = isUserScrolling
            if (!tensListState.isScrollInProgress && wasUserScrolling && !tensIsProgrammaticScroll) {
                // Wait a little for snap fling settle, then fix value only (no extra scroll animation).
                kotlinx.coroutines.delay(80)
                if (tensListState.isScrollInProgress || tensIsProgrammaticScroll) return@LaunchedEffect
                val layoutInfo = tensListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val centerItem = visibleItems.minByOrNull { 
                        kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) 
                    }
                    val centerIndex = centerItem?.index ?: tensListState.firstVisibleItemIndex
                    
                    if (centerIndex in tensList.indices) {
                        val newTens = tensList[centerIndex]
                        val newValue = newTens * 10 + onesDigit
                        if (newValue in minValue..maxValue && newValue != value) {
                            onValueChange(newValue)
                        }
                    }
                }
            }
        }
        
        LaunchedEffect(onesListState.isScrollInProgress) {
            val wasUserScrolling = onesWasUserScrolling
            val isUserScrolling = onesListState.isScrollInProgress && !onesIsProgrammaticScroll
            onesWasUserScrolling = isUserScrolling
            if (!onesListState.isScrollInProgress && wasUserScrolling && !onesIsProgrammaticScroll) {
                // Wait a little for snap fling settle, then fix value only (no extra scroll animation).
                kotlinx.coroutines.delay(80)
                if (onesListState.isScrollInProgress || onesIsProgrammaticScroll) return@LaunchedEffect
                val layoutInfo = onesListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val centerItem = visibleItems.minByOrNull { 
                        kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) 
                    }
                    val centerIndex = centerItem?.index ?: onesListState.firstVisibleItemIndex
                    
                    if (centerIndex in onesList.indices) {
                        val newOnes = onesList[centerIndex]
                        val newValue = tensDigit * 10 + newOnes
                        if (newValue in minValue..maxValue && newValue != value) {
                            onValueChange(newValue)
                        }
                    }
                }
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetPickerDigitSpacing()),
            modifier = Modifier.height(ScreenSize.settingsSheetPickerSelectHeight())
        ) {
            // Tens digit picker
            Box(
                modifier = Modifier
                    .width(ScreenSize.settingsSheetPickerDigitColumnWidth())
                    .height(ScreenSize.settingsSheetPickerSelectHeight())
            ) {
                LazyColumn(
                    state = tensListState,
                    flingBehavior = tensFlingBehavior,
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
                                .clickable(enabled = isTensClickEnabled) {
                                    coroutineScope.launch {
                                        scrollToTensIndex(index)
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
                    .width(ScreenSize.settingsSheetPickerDigitColumnWidth())
                    .height(ScreenSize.settingsSheetPickerSelectHeight())
            ) {
                LazyColumn(
                    state = onesListState,
                    flingBehavior = onesFlingBehavior,
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
                                .clickable(enabled = isOnesClickEnabled) {
                                    coroutineScope.launch {
                                        scrollToOnesIndex(index)
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
    
    // Small tag for metadata with rounded rectangle shape.
    @Composable
    fun CustomTag(
        text: String,
        modifier: Modifier = Modifier,
        backgroundColor: Color? = null,
    ) {
        val tagSize = ScreenSize.settingsSheetLineTagSize()
        val cornerRadius = tagSize / 2f // Half of height for rounded rectangle
        
        Box(
            modifier = modifier
                .size(tagSize)
                .clip(RoundedCornerShape(cornerRadius))
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
    
    // Loading progress indicator with dark overlay.
    @Composable
    fun CustomProgressIndicator(
        text: String? = null
    ) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ScreenSize.splashLoadingSpacing())
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ScreenSize.customProgressIndicatorSize()),
                    color = White
                )
                
                if (text != null) {
                    Text(
                        text = text,
                        fontSize = ScreenSize.splashLoadingFontSize().value.sp,
                        color = White
                    )
                }
            }
        }
    }
    
    // Dropdown that avoids IME focus issues.
    @Composable
    fun <T> CustomDropdown(
        items: List<T>,
        onItemSelected: (T) -> Unit,
        itemContent: @Composable (T) -> Unit,
        modifier: Modifier = Modifier,
        maxHeight: Dp = ScreenSize.settingsLineSheetSuggestionItemHeight() * 7f,
        backgroundColor: Color = LightGray.copy(alpha = 0.85f),
        offsetX: Dp = ScreenSize.settingsLineSheetDropdownOffsetX(),
        offsetY: Dp = 0.dp,
        itemHeight: Dp = ScreenSize.settingsLineSheetSuggestionItemHeight(),
        onDismissRequest: () -> Unit = {},
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
                                    onDismissRequest()
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
    
    // Custom text field with consistent styling.
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
        isCheckmarkSelected: Boolean? = null,
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
            
            var isFocused by remember { mutableStateOf(false) }
            val placeholderColor = if (isFocused) Gray else Color(0xFF9C9C9C)
            
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
                            color = placeholderColor,
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
                    tint = if (!isCheckmarkValid) Gray else if (isCheckmarkSelected == true) Primary else Accent,
                    modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                )
            }
        }
    }
    
    // Login text field with password visibility toggle.
    // Uses OutlinedTextField for proper masking.
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
    
    // Alert dialog with consistent sizing and emphasis.
    @Composable
    fun CustomAlertDialog(
        title: String,
        alertMessage: String? = null,
        confirmButtonText: String,
        dismissButtonText: String? = null,
        onConfirmClick: () -> Unit,
        onDismissRequest: () -> Unit,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        isDestructive: Boolean = false,
        textContent: (@Composable () -> Unit)? = null
    ) {
        val titleFontSize = ScreenSize.alertDialogTitleFontSize()
        val textFontSize = ScreenSize.alertDialogTextFontSize()
        val buttonFontSize = ScreenSize.alertDialogButtonFontSize()
        val dialogCornerRadius = ScreenSize.alertDialogCornerRadius()
        val buttonCornerRadius = ScreenSize.alertDialogButtonCornerRadius()
        val elevation = ScreenSize.alertDialogElevation()

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetHorizontalSpacing())
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isDestructive) Red else Primary,
                            modifier = Modifier.size(ScreenSize.settingsSheetIconSize())
                        )
                    }
                    Text(
                        text = title,
                        fontSize = titleFontSize.value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDestructive) Red else Primary
                    )
                }
            },
            text = if (textContent != null || alertMessage != null) {
                {
                    if (textContent != null) {
                        textContent()
                    } else if (alertMessage != null) {
                        Text(
                            text = alertMessage,
                            fontSize = textFontSize.value.sp,
                            color = Black
                        )
                    }
                }
            } else null,
            confirmButton = {
                FilledTonalButton(
                    onClick = onConfirmClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isDestructive) Red else Primary,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(buttonCornerRadius)
                ) {
                    Text(
                        text = confirmButtonText,
                        fontSize = buttonFontSize.value.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = if (dismissButtonText != null) {
                {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(buttonCornerRadius),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Primary
                        ),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = dismissButtonText,
                            fontSize = buttonFontSize.value.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else null,
            containerColor = White.copy(alpha = 0.8f),
            shape = RoundedCornerShape(dialogCornerRadius),
            tonalElevation = elevation
        )
    }
}
