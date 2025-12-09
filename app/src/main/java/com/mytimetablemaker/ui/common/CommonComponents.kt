package com.mytimetablemaker.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import kotlinx.coroutines.launch
import kotlin.math.max

// MARK: - Common Components Object
// Container for reusable UI components
object CommonComponents {
    
    // MARK: - Custom Button
    // Custom button component for consistent styling
    // Matches SwiftUI CustomButton implementation
    @Composable
    fun CustomButton(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        backgroundColor: Color = com.mytimetablemaker.ui.theme.Accent,
        textColor: Color = Color.White,
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
                containerColor = if (isEnabled) backgroundColor else com.mytimetablemaker.ui.theme.Gray,
                contentColor = textColor
            ),
            shape = RoundedCornerShape(ScreenSize.settingsSheetButtonCornerRadius())
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    if (icon != null) ScreenSize.settingsSheetIconSpacing() else 0.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(ScreenSize.settingsSheetButtonFontSize()),
                        tint = textColor
                    )
                }
                Text(
                    text = title,
                    fontSize = ScreenSize.settingsSheetButtonFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
    
    // MARK: - Custom Back Button
    // Reusable back button component with consistent styling
    @Composable
    fun CustomBackButton(
        foregroundColor: Color = Color.White,
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
                    modifier = Modifier.size(ScreenSize.settingsHeaderFontSize().value.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
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
    // Customizable toggle component with left/right text and colors
    @Composable
    fun CustomToggle(
        isLeftSelected: Boolean,
        onToggle: (Boolean) -> Unit,
        leftText: String,
        leftColor: Color,
        rightText: String,
        rightColor: Color,
        circleColor: Color = Color.White,
        offColor: Color = Color.Gray
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.customToggleSpacing()),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = ScreenSize.customTogglePaddingHorizontal())
        ) {
            // Left label
            Text(
                text = leftText,
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                fontWeight = FontWeight.Medium,
                color = if (isLeftSelected) leftColor else offColor,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            
            // Toggle switch
            Box(
                modifier = Modifier
                    .width(ScreenSize.customToggleWidth())
                    .height(ScreenSize.customToggleHeight())
                    .clip(RoundedCornerShape(ScreenSize.customToggleCornerRadius()))
                    .background(if (isLeftSelected) leftColor else rightColor)
                    .clickable { onToggle(!isLeftSelected) }
            ) {
                Box(
                    modifier = Modifier
                        .size(ScreenSize.customToggleCircleSize())
                        .clip(CircleShape)
                        .background(circleColor)
                        .align(Alignment.Center)
                        .offset(x = if (isLeftSelected) -ScreenSize.customToggleCircleOffset() else ScreenSize.customToggleCircleOffset())
                )
            }
            
            // Right label
            Text(
                text = rightText,
                fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                fontWeight = FontWeight.Medium,
                color = if (isLeftSelected) offColor else rightColor,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
    
    // MARK: - Custom Rectangle Button
    // Custom rectangle button component for consistent styling
    @Composable
    fun CustomRectangleButton(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        tintColor: Color = com.mytimetablemaker.ui.theme.Accent,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = tintColor,
                contentColor = Color.White
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
        
        val tensRange = (minValue / 10)..(maxValue / 10)
        val tensList = tensRange.toList()
        val onesRange = if (tensDigit == minValue / 10) {
            (max(0, minValue % 10))..9
        } else if (tensDigit == maxValue / 10) {
            0..(maxValue % 10)
        } else {
            0..9
        }
        val onesList = onesRange.toList()
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(ScreenSize.settingsSheetPickerSpacing()),
            modifier = Modifier.height(ScreenSize.settingsSheetPickerSelectHeight())
        ) {
            // Tens digit picker
            WheelPicker(
                items = tensList,
                selectedIndex = tensList.indexOf(tensDigit).coerceAtLeast(0),
                onSelectedIndexChange = { index ->
                    val newTens = tensList[index]
                    val newValue = newTens * 10 + onesDigit
                    if (newValue >= minValue && newValue <= maxValue) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.width(ScreenSize.settingsSheetPickerSelectWidth())
            )
            
            // Ones digit picker
            WheelPicker(
                items = onesList,
                selectedIndex = onesList.indexOf(onesDigit).coerceAtLeast(0),
                onSelectedIndexChange = { index ->
                    val newOnes = onesList[index]
                    val newValue = tensDigit * 10 + newOnes
                    if (newValue >= minValue && newValue <= maxValue) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.width(ScreenSize.settingsSheetPickerSelectWidth())
            )
        }
    }
    
    // MARK: - Wheel Picker Helper
    // Helper composable for wheel-style picker
    @Composable
    private fun WheelPicker(
        items: List<Int>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.size - 1))
        val coroutineScope = rememberCoroutineScope()
        
        LaunchedEffect(selectedIndex) {
            if (selectedIndex != listState.firstVisibleItemIndex) {
                coroutineScope.launch {
                    listState.animateScrollToItem(selectedIndex)
                }
            }
        }
        
        Box(
            modifier = modifier
                .height(ScreenSize.settingsSheetPickerSelectHeight())
                .background(Color.White)
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    val isSelected = index == selectedIndex
                    
                    Text(
                        text = item.toString(),
                        fontSize = ScreenSize.settingsSheetInputFontSize().value.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectedIndexChange(index)
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}
