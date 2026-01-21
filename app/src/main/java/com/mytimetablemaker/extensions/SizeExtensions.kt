package com.mytimetablemaker.extensions

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Screen Size Extensions
// Screen dimensions and responsive sizing calculations
// Matches SwiftUI SizeExtensions structure
object ScreenSize {
    
    // MARK: - Basic Screen Properties
    // Use LocalConfiguration for actual screen size (matches SwiftUI UIScreen.main.bounds)
    // Note: LocalWindowInfo.containerSize is window/container size, not screen size
    @Composable
    fun screenWidth(): Dp = LocalConfiguration.current.screenWidthDp.dp
    @Composable
    fun screenHeight(): Dp = LocalConfiguration.current.screenHeightDp.dp
    @Composable
    fun customWidth(): Dp = if (screenWidth() < 600.dp) screenWidth() else 600.dp
    @Composable
    fun statusBarHeight(): Dp = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

    // MARK: - Splash Screen
    @Composable
    fun splashTitleFontSize(): Dp = customWidth() * 0.075f
    @Composable
    fun splashIconSize(): Dp = customWidth() * 0.3f
    @Composable
    fun splashLoadingFontSize(): Dp = customWidth() * 0.06f
    @Composable
    fun splashLoadingSpacing(): Dp = screenHeight() * 0.02f
    
    // MARK: - Header & Navigation
    @Composable
    fun headerHeight(): Dp = operationButtonWidth() + 10.dp
    @Composable
    fun headerDateFontSize(): Dp = customWidth() / 20f
    @Composable
    fun headerDateHeight(): Dp = customWidth() / 30f
    @Composable
    fun headerDateMargin(): Dp = customWidth() / 6f
    @Composable
    fun headerSpace(): Dp = customWidth() / 60f
    @Composable
    fun headerSettingsButtonSize(): Dp = customWidth() / 16f
    @Composable
    fun operationButtonWidth(): Dp = customWidth() / 6f
    @Composable
    fun operationButtonHeight(): Dp = customWidth() / 12f
    @Composable
    fun operationButtonMargin(): Dp = customWidth() / 24f
    
    // MARK: - Main Content Layout
    @Composable
    fun routeSingleWidth(): Dp = customWidth() -  routeSidePadding() * 10f
    @Composable
    fun routeDoubleWidth(): Dp = customWidth() / 2f - routeSidePadding() * 4f
    @Composable
    fun routeWidth(isShowRoute2: Boolean): Dp = if (isShowRoute2) routeDoubleWidth() else routeSingleWidth()
    @Composable
    fun routeHeight(): Dp = screenHeight() - admobBannerHeight() - statusBarHeight()
    @Composable
    fun routeSidePadding(): Dp = customWidth() * 0.02f
    @Composable
    fun routeBottomSpace(): Dp = routeHeight() / 150f
    @Composable
    fun routeCountdownFontSize(): Dp = customWidth() / 12f
    @Composable
    fun routeCountdownTopSpace(): Dp = if (routeHeight() > 600.dp) (routeHeight() - 600.dp) / 20f + 5.dp else 5.dp
    @Composable
    fun routeCountdownPadding(): Dp = customWidth() / 50f + (if (routeHeight() > 600.dp) ((routeHeight() - 600.dp) / 10) else 0.dp)
    @Composable
    fun stationFontSize(): Dp = customWidth() / 27f
    @Composable
    fun transferHeight(): Dp = screenHeight() * 0.036f
    @Composable
    fun lineNameHeight(): Dp = screenHeight() * 0.045f
    @Composable
    fun lineFontSize(): Dp = customWidth() / 27f
    @Composable
    fun lineImageForegroundSize(): Dp = customWidth() / 20f
    @Composable
    fun lineImageBackgroundSize(): Dp = customWidth() / 15f
    @Composable
    fun timeFontSize(): Dp = customWidth() / 18f

    // MARK: - AdMob Banner
    @Composable
    fun admobBannerWidth(): Dp = customWidth()
    @Composable
    fun admobBannerMinWidth(): Dp = 320.dp
    @Composable
    fun admobBannerHeight(): Dp = if ((screenHeight() - headerHeight() - 75.dp) < 500.dp) 50.dp else (screenHeight() - headerHeight() - 75.dp) / 10

    // MARK: - Login
    @Composable
    fun loginTitleFontSize(): Dp = customWidth() * 0.06f
    @Composable
    fun loginButtonWidth(): Dp = customWidth() * 0.88f
    @Composable
    fun loginTextFieldFontSize(): Dp = customWidth() * 0.036f
    @Composable
    fun loginEyeIconSize(): Dp = customWidth() * 0.042f
    @Composable
    fun loginTitleTopMargin(): Dp = screenHeight() * 0.12f
    @Composable
    fun loginTitleBottomMargin(): Dp = screenHeight() * 0.02f
    @Composable
    fun loginTextHeight(): Dp = screenHeight() * 0.045f
    @Composable
    fun loginMargin(): Dp = screenHeight() * 0.03f
    @Composable
    fun loginHeadlineFontSize(): Dp = customWidth() * 0.042f
    @Composable
    fun loginSubheadlineFontSize(): Dp = customWidth() * 0.034f
    @Composable
    fun loginCheckboxSize(): Dp = customWidth() * 0.06f
    
    // MARK: - Timetable
    @Composable
    fun timetableDisplayWidth(): Dp = customWidth() * 0.90f
    @Composable
    fun timetableHourFontSize(): Dp = customWidth() * 0.036f
    @Composable
    fun timetableMinuteFontSize(): Dp = customWidth() * 0.032f
    @Composable
    fun timetableRideTimeFontSize(): Dp = customWidth() * 0.020f
    @Composable
    fun timetableMinuteSpacing(): Dp = customWidth() * 0.008f
    @Composable
    fun timetableHorizontalSpacing(): Dp = customWidth() * 0.04f
    @Composable
    fun timetableHourFrameWidth(): Dp = customWidth() * 0.1f
    @Composable
    fun timetableMinuteFrameWidth(): Dp = customWidth() - timetableHourFrameWidth() - 1.0.dp
    @Composable
    fun timetableTypeMenuWidth(): Dp = customWidth() * 0.50f
    @Composable
    fun timetableEditButtonWidth(): Dp = customWidth() * 0.44f
    @Composable
    fun timetablePickerWidth(): Dp = customWidth() * 0.43f
    @Composable
    fun timetableTypeMenuOffsetX(): Dp = customWidth() * 0.00f
    @Composable
    fun timetableNumberHeight(): Dp = screenHeight() * 0.018f
    @Composable
    fun timetableGridHeight(): Dp = screenHeight() * 0.024f
    @Composable
    fun timetableDisplayHeight(): Dp = screenHeight() * 0.06f
    @Composable
    fun timetableMaxHeight(): Dp = screenHeight() * 0.64f
    @Composable
    fun timetableVerticalSpacing(): Dp = screenHeight() * 0.012f
    @Composable
    fun timetableTypeMenuOffsetY(): Dp = screenHeight() * 0.000f
    @Composable
    fun timetableCopyMenuOffsetY(): Dp = screenHeight() * 0.13f
    @Composable
    fun timetableCalendarMenuOffsetY(): Dp = screenHeight() * 0.00f
    @Composable
    fun timetableContentViewMenuOffsetY(): Dp = screenHeight() * 0.10f
    @Composable
    fun timetablePickerTopPadding(): Dp = screenHeight() * 0.000f
    @Composable
    fun timetablePickerBottomPadding(): Dp = screenHeight() * 0.000f
    @Composable
    fun settingsTimetableSheetHeight(): Dp = screenHeight() * 0.6f
    @Composable
    fun calculateContentHeight(trainTimesCount: Int): Dp =
        if (trainTimesCount > 10) timetableNumberHeight() * (trainTimesCount + 9) / 10 else timetableGridHeight()

    // MARK: - Settings
    @Composable
    fun settingsTitleFontSize(): Dp = screenHeight() * 0.020f
    @Composable
    fun settingsHeaderFontSize(): Dp = screenHeight() * 0.016f
    @Composable
    fun settingsFontSize(): Dp = screenHeight() * 0.018f
    
    // MARK: - Settings Sheet Common
    @Composable
    fun settingsSheetVerticalSpacing(): Dp = screenHeight() * 0.012f
    @Composable
    fun settingsSheetHorizontalPadding(): Dp = customWidth() * 0.06f
    @Composable
    fun settingsSheetHorizontalSpacing(): Dp = customWidth() * 0.02f
    @Composable
    fun settingsSheetTitleFontSize(): Dp = customWidth() * 0.038f
    @Composable
    fun settingsSheetHeadlineFontSize(): Dp = customWidth() * 0.030f
    @Composable
    fun settingsSheetInputFontSize(): Dp = customWidth() * 0.034f
    @Composable
    fun settingsSheetButtonFontSize(): Dp = customWidth() * 0.036f
    @Composable
    fun settingsSheetInputPaddingHorizontal(): Dp = customWidth() * 0.04f
    @Composable
    fun settingsSheetStrokeLineWidth(): Dp = customWidth() * 0.002f
    @Composable
    fun settingsSheetLineTagSize(): Dp = customWidth() * 0.05f
    @Composable
    fun settingsSheetIconSize(): Dp = customWidth() * 0.045f
    @Composable
    fun settingsSheetIconSpacing(): Dp = customWidth() * 0.016f
    @Composable
    fun settingsSheetRectangleButtonPaddingHorizontal(): Dp = customWidth() * 0.030f
    @Composable
    fun settingsSheetRectangleButtonPaddingVertical(): Dp = screenHeight() * 0.00f
    @Composable
    fun settingsSheetRectangleButtonHeight(): Dp = screenHeight() * 0.036f
    @Composable
    fun settingsSheetPickerSelectWidth(): Dp = customWidth() * 0.08f
    @Composable
    fun settingsSheetPickerSpacing(): Dp = customWidth() * 0.01f
    @Composable
    fun settingsSheetInputPaddingVertical(): Dp = screenHeight() * 0.008f
    @Composable
    fun settingsSheetCornerRadius(): Dp = screenHeight() * 0.015f
    @Composable
    fun settingsSheetPickerSelectHeight(): Dp = screenHeight() * 0.075f
    @Composable
    fun settingsSheetPickerDisplayHeight(): Dp = screenHeight() * 0.022f
    @Composable
    fun settingsSheetPickerItemPaddingVertical(): Dp = screenHeight() * 0.004f
    @Composable
    fun settingsSheetButtonHeight(): Dp = screenHeight() * 0.044f
    @Composable
    fun settingsSheetButtonCornerRadius(): Dp = screenHeight() * 0.022f

    // MARK: - Settings Line Sheet
    @Composable
    fun settingsLineSheetPickerPadding(): Dp = screenHeight() * 0.000f
    @Composable
    fun settingsLineSheetGridSpacing(): Dp = screenHeight() * 0.01f
    @Composable
    fun settingsLineSheetSuggestionItemHeight(): Dp = screenHeight() * 0.046f
    @Composable
    fun settingsLineSheetTagPaddingVertical(): Dp = screenHeight() * 0.001f
    @Composable
    fun settingsLineSheetDropdownOffsetX(): Dp = customWidth() * 0.16f
    @Composable
    fun settingsLineSheetDropdownOffsetY(): Dp = screenHeight() * 0.005f
    @Composable
    fun settingsLineSheetOperatorOffset(): Dp = screenHeight() * 0.13f
    @Composable
    fun settingsLineSheetLineOffset(): Dp = screenHeight() * 0.18f
    @Composable
    fun settingsLineSheetDepartureOffset(): Dp = screenHeight() * 0.32f
    @Composable
    fun settingsLineSheetArrivalOffset(): Dp = screenHeight() * 0.37f
    @Composable
    fun settingsLineSheetTransportationDropdownOffsetY(): Dp = screenHeight() * 0.01f
    @Composable
    fun settingsLineSheetCaptionFontSize(): Dp = customWidth() * 0.024f
    @Composable
    fun settingsLineSheetColorSettingWidth(): Dp = customWidth() * 1.0f
    @Composable
    fun settingsLineSheetColorHorizontalPadding(): Dp = customWidth() * 0.09f
    @Composable
    fun settingsLineSheetColorCircleSize(): Dp = customWidth() * 0.09f
    @Composable
    fun settingsLineSheetColorCircleSmallSize(): Dp = customWidth() * 0.05f
    @Composable
    fun settingsLineSheetTagPaddingHorizontal(): Dp = customWidth() * 0.01f
    
    // MARK: - Settings Transfer Sheet
    @Composable
    fun settingsTransferSheetVerticalSpacing(): Dp = screenHeight() * 0.02f
    @Composable
    fun settingsTransferSheetCheckmarkSpacing(): Dp = screenHeight() * 0.009f
    @Composable
    fun settingsTransferSheetPickerWidth(): Dp = customWidth() * 0.28f
    @Composable
    fun settingsTransferSheetPaddingLeft(): Dp = customWidth() * 0.03f
    
    // MARK: - Custom Component
    @Composable
    fun customToggleSpacing(): Dp = screenHeight() * 0.006f
    @Composable
    fun customToggleCornerRadius(): Dp = screenHeight() * 0.026f
    @Composable
    fun customToggleWidth(): Dp = screenHeight() * 0.051f
    @Composable
    fun customToggleHeight(): Dp = screenHeight() * 0.018f
    @Composable
    fun customToggleCircleSize(): Dp = screenHeight() * 0.022f
    @Composable
    fun customToggleCircleOffset(): Dp = screenHeight() * 0.1f
    @Composable
    fun customTogglePaddingHorizontal(): Dp = customWidth() * 0.02f
    @Composable
    fun customTextFieldPaddingVertical(): Dp = customWidth() * 0.012f
    @Composable
    fun progressIndicatorSize(): Dp = customWidth() * 0.1f

    
    // MARK: - Common UI Elements
    @Composable
    fun dividerWidth(): Dp = 1.5.dp
    @Composable
    fun shadowOffset(): Dp = 0.5.dp
    @Composable
    fun borderWidth(): Dp = 1.dp
}