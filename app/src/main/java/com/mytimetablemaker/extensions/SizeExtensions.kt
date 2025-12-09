package com.mytimetablemaker.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Screen Size Extensions
// Screen dimensions and responsive sizing calculations
// Matches SwiftUI SizeExtensions structure
object ScreenSize {
    
    // MARK: - Basic Screen Properties
    @Composable
    fun screenWidth(): Float {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp.toFloat()
    }
    
    @Composable
    fun screenHeight(): Float {
        val configuration = LocalConfiguration.current
        return configuration.screenHeightDp.toFloat()
    }
    
    @Composable
    fun customWidth(): Float {
        val width = screenWidth()
        return if (width < 600) width else 600f
    }
    
    @Composable
    fun statusBarHeight(): Float {
        // TODO: Get actual status bar height from WindowInsets
        // For now, return approximate value
        return 44f // Approximate status bar height
    }
    
    // MARK: - Splash Screen
    @Composable
    fun splashTitleFontSize(): Dp = (customWidth() * 0.08f).dp
    
    @Composable
    fun splashIconSize(): Dp = (customWidth() * 0.3f).dp
    
    @Composable
    fun splashLoadingFontSize(): Dp = (customWidth() * 0.06f).dp
    
    @Composable
    fun splashLoadingSpacing(): Dp = (screenHeight() * 0.02f).dp
    
    // MARK: - Header & Navigation
    @Composable
    fun headerTopMargin(): Dp = (statusBarHeight() + 5f).dp
    
    @Composable
    fun headerHeight(): Dp = (statusBarHeight() + operationButtonWidth().value + 10f).dp
    
    @Composable
    fun headerDateFontSize(): Dp = (customWidth() / 20f).dp
    
    @Composable
    fun headerDateHeight(): Dp = (customWidth() / 30f).dp
    
    @Composable
    fun headerDateMargin(): Dp = (customWidth() / 6f).dp
    
    @Composable
    fun headerSpace(): Dp = (customWidth() / 60f).dp
    
    @Composable
    fun headerSettingsButtonSize(): Dp = (customWidth() / 16f).dp
    
    @Composable
    fun operationButtonWidth(): Dp = (customWidth() / 6f).dp
    
    @Composable
    fun operationButtonHeight(): Dp = (customWidth() / 12f).dp
    
    @Composable
    fun operationButtonMargin(): Dp = (customWidth() / 24f).dp
    
    // MARK: - Main Content Layout
    @Composable
    fun routeSingleWidth(): Dp = (customWidth() - 10f * routeSidePadding().value).dp
    
    @Composable
    fun routeDoubleWidth(): Dp = (customWidth() / 2f - 4f * routeSidePadding().value).dp
    
    @Composable
    fun routeHeight(): Dp = (screenHeight() - admobBannerHeight().value - headerHeight().value).dp
    
    @Composable
    fun routeSidePadding(): Dp = (customWidth() / 40f).dp
    
    @Composable
    fun routeBottomSpace(): Dp = (routeHeight().value / 150f).dp
    
    @Composable
    fun routeCountdownFontSize(): Dp = (customWidth() / 12f).dp
    
    @Composable
    fun routeCountdownTopSpace(): Dp {
        val rHeight = routeHeight().value
        return if (rHeight > 600) {
            ((rHeight - 600) / 20 + 5).dp
        } else {
            5.dp
        }
    }
    
    @Composable
    fun routeCountdownPadding(): Dp {
        val rHeight = routeHeight().value
        return (customWidth() / 50f + (if (rHeight > 600) ((rHeight - 600) / 10) else 0f)).dp
    }
    
    @Composable
    fun stationFontSize(): Dp = (customWidth() / 27f).dp
    
    @Composable
    fun transferHeight(): Dp = (screenHeight() * 0.036f).dp
    
    @Composable
    fun lineNameHeight(): Dp = (screenHeight() * 0.045f).dp
    
    @Composable
    fun lineFontSize(): Dp = (customWidth() / 27f).dp
    
    @Composable
    fun lineImageForegroundSize(): Dp = (customWidth() / 20f).dp
    
    @Composable
    fun lineImageForegroundPadding(): Dp = (customWidth() / 80f).dp
    
    @Composable
    fun lineImageBackgroundSize(): Dp = (customWidth() / 15f).dp
    
    @Composable
    fun lineImageBackgroundPadding(): Dp = (customWidth() / 200f).dp
    
    @Composable
    fun lineImagePadding(): Dp = (customWidth() / 300f).dp
    
    @Composable
    fun timeFontSize(): Dp = (customWidth() / 18f).dp
    
    @Composable
    fun admobBannerWidth(): Dp = (customWidth() - 100f).dp
    
    @Composable
    fun admobBannerMinWidth(): Dp = 320.dp
    
    @Composable
    fun admobBannerHeight(): Dp {
        val height = screenHeight() - headerHeight().value - 75f
        return if (height < 500) {
            50.dp
        } else {
            (height / 10f).dp
        }
    }
    
    // MARK: - Login
    @Composable
    fun loginTitleFontSize(): Dp = (customWidth() * 0.06f).dp
    
    @Composable
    fun loginButtonWidth(): Dp = (customWidth() * 0.88f).dp
    
    @Composable
    fun loginTextFieldFontSize(): Dp = (customWidth() * 0.036f).dp
    
    @Composable
    fun loginEyeIconSize(): Dp = (customWidth() * 0.042f).dp
    
    @Composable
    fun loginTitleTopMargin(): Dp = (screenHeight() * 0.12f).dp
    
    @Composable
    fun loginTitleBottomMargin(): Dp = (screenHeight() * 0.02f).dp
    
    @Composable
    fun loginTextHeight(): Dp = (screenHeight() * 0.045f).dp
    
    @Composable
    fun loginMargin(): Dp = (screenHeight() * 0.03f).dp
    
    // MARK: - Timetable
    @Composable
    fun timetableDisplayWidth(): Dp = (customWidth() * 0.90f).dp
    
    @Composable
    fun timetableHourFontSize(): Dp = (customWidth() * 0.036f).dp
    
    @Composable
    fun timetableMinuteFontSize(): Dp = (customWidth() * 0.032f).dp
    
    @Composable
    fun timetableRideTimeFontSize(): Dp = (customWidth() * 0.020f).dp
    
    @Composable
    fun timetableMinuteSpacing(): Dp = (customWidth() * 0.008f).dp
    
    @Composable
    fun timetableHorizontalSpacing(): Dp = (customWidth() * 0.04f).dp
    
    @Composable
    fun timetableWeekToggleSpacing(): Dp = (customWidth() * 0.016f).dp
    
    @Composable
    fun timetableHourFrameWidth(): Dp = (customWidth() * 0.1f).dp
    
    @Composable
    fun timetableMinuteFrameWidth(): Dp {
        val hourWidth = timetableHourFrameWidth().value
        return (customWidth() - hourWidth - 1f).dp
    }
    
    @Composable
    fun timetableNumberWidth(): Dp = (customWidth() * 0.048f).dp
    
    @Composable
    fun timetableTypeMenuWidth(): Dp = (customWidth() * 0.50f).dp
    
    @Composable
    fun timetableEditButtonWidth(): Dp = (customWidth() * 0.44f).dp
    
    @Composable
    fun timetablePickerWidth(): Dp = (customWidth() * 0.43f).dp
    
    @Composable
    fun timetableTypeMenuOffsetX(): Dp = (customWidth() * 0.00f).dp
    
    @Composable
    fun timetableNumberHeight(): Dp = (screenHeight() * 0.018f).dp
    
    @Composable
    fun timetableGridHeight(): Dp = (screenHeight() * 0.024f).dp
    
    @Composable
    fun timetableDisplayHeight(): Dp = (screenHeight() * 0.06f).dp
    
    @Composable
    fun timetableEditTitleHeight(): Dp = (screenHeight() * 0.06f).dp
    
    @Composable
    fun timetableMaxHeight(): Dp = (screenHeight() * 0.64f).dp
    
    @Composable
    fun timetableVerticalSpacing(): Dp = (screenHeight() * 0.012f).dp
    
    @Composable
    fun timetableTypeMenuPadding(): Dp = (screenHeight() * 0.16f).dp
    
    @Composable
    fun timetablePickerSpacing(): Dp = (screenHeight() * 0.02f).dp
    
    @Composable
    fun timetableTypeMenuOffsetY(): Dp = (screenHeight() * -0.045f).dp
    
    @Composable
    fun timetableCopyMenuOffsetY(): Dp = (screenHeight() * 0.13f).dp
    
    @Composable
    fun timetableCalendarMenuOffsetY(): Dp = (screenHeight() * -0.34f).dp
    
    @Composable
    fun timetableContentViewMenuOffsetY(): Dp = (screenHeight() * 0.10f).dp
    
    @Composable
    fun timetablePickerTopPadding(): Dp = (screenHeight() * -0.036f).dp
    
    @Composable
    fun timetablePickerBottomPadding(): Dp = (screenHeight() * -0.012f).dp
    
    @Composable
    fun timetableScrollViewMaxHeight(): Dp = (screenHeight() * 0.6f).dp
    
    @Composable
    fun settingsTimetableSheetHeight(): Dp = (screenHeight() * 0.6f).dp
    
    @Composable
    fun calculateContentHeight(trainTimesCount: Int): Dp {
        val maxItemsPerRow = 10
        return if (trainTimesCount > maxItemsPerRow) {
            val rows = (trainTimesCount + maxItemsPerRow - 1) / maxItemsPerRow
            (rows.toFloat() * timetableNumberHeight().value).dp
        } else {
            timetableGridHeight()
        }
    }
    
    // MARK: - Settings
    @Composable
    fun settingsTitleFontSize(): Dp = (screenHeight() * 0.022f).dp
    
    @Composable
    fun settingsHeaderFontSize(): Dp = (screenHeight() * 0.016f).dp
    
    @Composable
    fun settingsFontSize(): Dp = (screenHeight() * 0.018f).dp
    
    // MARK: - Settings Sheet Common
    @Composable
    fun settingsSheetHorizontalPadding(): Dp = (customWidth() * 0.06f).dp
    
    @Composable
    fun settingsSheetHorizontalSpacing(): Dp = (customWidth() * 0.015f).dp
    
    @Composable
    fun settingsSheetTitleFontSize(): Dp = (customWidth() * 0.040f).dp
    
    @Composable
    fun settingsSheetHeadlineFontSize(): Dp = (customWidth() * 0.032f).dp
    
    @Composable
    fun settingsSheetInputFontSize(): Dp = (customWidth() * 0.036f).dp
    
    @Composable
    fun settingsSheetButtonFontSize(): Dp = (customWidth() * 0.040f).dp
    
    @Composable
    fun settingsSheetInputPaddingHorizontal(): Dp = (customWidth() * 0.04f).dp
    
    @Composable
    fun settingsSheetStrokeLineWidth(): Dp = (customWidth() * 0.002f).dp
    
    @Composable
    fun settingsSheetIconSize(): Dp = (customWidth() * 0.016f).dp
    
    @Composable
    fun settingsSheetPickerSelectWidth(): Dp = (customWidth() * 0.10f).dp
    
    @Composable
    fun settingsSheetPickerSpacing(): Dp = (customWidth() * -0.030f).dp
    
    @Composable
    fun settingsSheetIconSpacing(): Dp = (customWidth() * 0.02f).dp
    
    @Composable
    fun settingsSheetVerticalSpacing(): Dp = (screenHeight() * 0.012f).dp
    
    @Composable
    fun settingsSheetSaveButtonSpacing(): Dp = (screenHeight() * 0.03f).dp
    
    @Composable
    fun settingsSheetInputPaddingVertical(): Dp = (screenHeight() * 0.008f).dp
    
    @Composable
    fun settingsSheetCornerRadius(): Dp = (screenHeight() * 0.016f).dp
    
    @Composable
    fun settingsSheetPickerSelectHeight(): Dp = (screenHeight() * 0.10f).dp
    
    @Composable
    fun settingsSheetPickerDisplayHeight(): Dp = (screenHeight() * 0.022f).dp
    
    @Composable
    fun settingsSheetButtonHeight(): Dp = (screenHeight() * 0.044f).dp
    
    @Composable
    fun settingsSheetButtonCornerRadius(): Dp = (screenHeight() * 0.022f).dp
    
    // MARK: - Settings Line Sheet
    @Composable
    fun settingsLineSheetPickerPadding(): Dp = (screenHeight() * -0.032f).dp
    
    @Composable
    fun settingsLineSheetShadowRadius(): Dp = (screenHeight() * 0.006f).dp
    
    @Composable
    fun settingsLineSheetTitleSpacing(): Dp = (screenHeight() * 0.012f).dp
    
    @Composable
    fun settingsLineSheetGridSpacing(): Dp = (screenHeight() * 0.02f).dp
    
    @Composable
    fun settingsLineSheetColorVerticalPadding(): Dp = (screenHeight() * 0.008f).dp
    
    @Composable
    fun settingsLineSheetSuggestionItemHeight(): Dp = (screenHeight() * 0.046f).dp
    
    @Composable
    fun settingsLineSheetMaxSuggestionHeight(): Dp = (screenHeight() * 0.280f).dp
    
    @Composable
    fun settingsLineSheetStopMaxSuggestionHeight(): Dp = (screenHeight() * 0.234f).dp
    
    @Composable
    fun settingsLineSheetTagPaddingVertical(): Dp = (screenHeight() * 0.003f).dp
    
    @Composable
    fun settingsLineSheetSuggestionSpacing(): Dp = (screenHeight() * 0.002f).dp
    
    @Composable
    fun settingsLineSheetSuggestionPaddingVertical(): Dp = (screenHeight() * 0.004f).dp
    
    @Composable
    fun settingsLineSheetOperatorOffset(): Dp = (screenHeight() * 0.13f).dp
    
    @Composable
    fun settingsLineSheetLineOffset(): Dp = (screenHeight() * 0.18f).dp
    
    @Composable
    fun settingsLineSheetColorOffset(): Dp = (screenHeight() * 0.25f).dp
    
    @Composable
    fun settingsLineSheetDepartureOffset(): Dp = (screenHeight() * 0.32f).dp
    
    @Composable
    fun settingsLineSheetArrivalOffset(): Dp = (screenHeight() * 0.37f).dp
    
    @Composable
    fun settingsLineSheetCaptionFontSize(): Dp = (customWidth() * 0.024f).dp
    
    @Composable
    fun settingsLineSheetColorSettingWidth(): Dp = (customWidth() * 0.80f).dp
    
    @Composable
    fun settingsLineSheetColorHorizontalPadding(): Dp = (customWidth() * 0.04f).dp
    
    @Composable
    fun settingsLineSheetColorCircleSize(): Dp = (customWidth() * 0.08f).dp
    
    @Composable
    fun settingsLineSheetColorCircleSmallSize(): Dp = (customWidth() * 0.04f).dp
    
    @Composable
    fun settingsLineSheetTagPaddingHorizontal(): Dp = (customWidth() * 0.006f).dp
    
    // MARK: - Settings Transfer Sheet
    @Composable
    fun settingsTransferSheetVerticalSpacing(): Dp = (screenHeight() * 0.02f).dp
    
    @Composable
    fun settingsTransferSheetCheckmarkSpacing(): Dp = (screenHeight() * 0.009f).dp
    
    @Composable
    fun settingsTransferSheetPickerWidth(): Dp = (customWidth() * 0.28f).dp
    
    @Composable
    fun settingsTransferSheetPaddingLeft(): Dp = (customWidth() * 0.03f).dp
    
    // MARK: - Components
    @Composable
    fun customToggleSpacing(): Dp = (screenHeight() * 0.006f).dp
    
    @Composable
    fun customToggleCornerRadius(): Dp = (screenHeight() * 0.026f).dp
    
    @Composable
    fun customToggleWidth(): Dp = (screenHeight() * 0.051f).dp
    
    @Composable
    fun customToggleHeight(): Dp = (screenHeight() * 0.029f).dp
    
    @Composable
    fun customToggleCircleSize(): Dp = (screenHeight() * 0.022f).dp
    
    @Composable
    fun customToggleCircleOffset(): Dp = (screenHeight() * 0.011f).dp
    
    @Composable
    fun customTogglePaddingHorizontal(): Dp = (customWidth() * 0.02f).dp
}

// MARK: - Boolean Extension for Route Width
// Extension function for route width calculation based on boolean value
@Composable
fun Boolean.routeWidth(): Dp {
    return if (this) {
        ScreenSize.routeDoubleWidth()
    } else {
        ScreenSize.routeSingleWidth()
    }
}

