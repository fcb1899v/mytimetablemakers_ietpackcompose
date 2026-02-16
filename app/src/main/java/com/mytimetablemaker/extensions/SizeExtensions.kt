package com.mytimetablemaker.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Screen dimensions and responsive sizing utilities.
// Centralizes size rules for layouts.
object ScreenSize {
    
    // Basic screen properties from the current window container.
    @Composable
    fun screenWidth(): Dp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    @Composable
    fun screenHeight(): Dp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }
    @Composable
    fun customWidth(): Dp = if (screenWidth() < 600.dp) screenWidth() else 600.dp
//    @Composable
//    fun statusBarHeight(): Dp = with(LocalDensity.current) {
//        WindowInsets.statusBars.getTop(this).toDp()
//    }

    // Splash screen sizing.
    @Composable
    fun splashTitleFontSize(): Dp = customWidth() * 0.075f
    @Composable
    fun splashIconSize(): Dp = customWidth() * 0.3f
    @Composable
    fun splashLoadingFontSize(): Dp = customWidth() * 0.05f
    @Composable
    fun splashLoadingSpacing(): Dp = screenHeight() * 0.02f
    
    // Header and navigation sizing. Use screenHeight() so header content scales on tablet portrait.
    @Composable
    fun headerHeight(): Dp = screenHeight() * 0.084f
    @Composable
    fun headerDateFontSize(): Dp = screenHeight() * 0.024f
    @Composable
    fun headerDateHeight(): Dp = screenHeight() * 0.024f
    @Composable
    fun headerDateMargin(): Dp = customWidth() * 0.1f
    @Composable
    fun headerSettingsButtonSize(): Dp = screenHeight() * 0.03f
    @Composable
    fun headerVerticalSpacing(): Dp = screenHeight() * 0.001f
    @Composable
    fun operationButtonWidth(): Dp = customWidth() / 6f
    @Composable
    fun operationButtonHeight(): Dp = screenHeight() * 0.036f
    @Composable
    fun operationButtonFontSize(): Dp = screenHeight() * 0.016f
    @Composable
    fun operationButtonCornerRadius(): Dp = screenHeight() * 0.018f
    @Composable
    fun operationButtonMargin(): Dp = customWidth() / 24f
    
    // Main content layout sizing.
    @Composable
    fun routeSingleWidth(): Dp = customWidth() * 0.7f
    @Composable
    fun routeDoubleWidth(): Dp = customWidth() * 0.45f
    @Composable
    fun routeWidth(isShowRoute2: Boolean): Dp = if (isShowRoute2) routeDoubleWidth() else routeSingleWidth()
    @Composable
    fun routeHeight(): Dp = screenHeight() - admobBannerHeight() - headerHeight()
    @Composable
    fun routeSidePadding(): Dp = customWidth() * 0.025f
    @Composable
    fun dividerSidePadding(): Dp = customWidth() * 0.015f
    @Composable
    fun routeBottomSpace(): Dp = routeHeight() / 150f
    @Composable
    fun routeCountdownFontSize(): Dp = customWidth() / 12f
    @Composable
    fun routeCountdownTopSpace(): Dp = screenHeight() * 0.03f
    @Composable
    fun routeCountdownSpace(): Dp = screenHeight() * 0.012f
    @Composable
    fun stationFontSize(): Dp = customWidth() * 0.036f
    @Composable
    fun transferHeight(): Dp = screenHeight() * 0.036f
    @Composable
    fun lineNameHeight(): Dp = screenHeight() * 0.056f
    @Composable
    fun lineFontSize(): Dp = customWidth() * 0.032f
    @Composable
    fun lineImageForegroundSize(): Dp = customWidth() / 20f
    @Composable
    fun lineImageBackgroundSize(): Dp = customWidth() / 15f
    @Composable
    fun lineImageTextSpacing(): Dp = customWidth() * 0.01f
    @Composable
    fun timeFontSize(): Dp = customWidth() * 0.056f

    // AdMob banner sizing.
    @Composable
    fun admobBannerWidth(): Dp = customWidth()
    @Composable
    fun admobBannerMinWidth(): Dp = 320.dp
    @Composable
    fun admobBannerHeight(): Dp = if (screenHeight() * 0.05f < 50.dp) 50.dp else screenHeight() * 0.05f

    // Login screen sizing.
    @Composable
    fun loginTitleFontSize(): Dp = customWidth() * 0.06f
    @Composable
    fun loginButtonWidth(): Dp = customWidth() * 0.88f
    @Composable
    fun loginEyeIconSize(): Dp = customWidth() * 0.042f
    @Composable
    fun loginTitleTopMargin(): Dp = screenHeight() * 0.02f
    @Composable
    fun loginTitleBottomMargin(): Dp = screenHeight() * 0.02f
    @Composable
    fun loginTextHeight(): Dp = screenHeight() * 0.06f
    @Composable
    fun loginMargin(): Dp = screenHeight() * 0.03f
    @Composable
    fun loginHeadlineFontSize(): Dp = customWidth() * 0.042f
    @Composable
    fun loginSubheadlineFontSize(): Dp = customWidth() * 0.034f
    @Composable
    fun loginCheckboxSize(): Dp = customWidth() * 0.06f
    @Composable
    fun loginSpacingSmall(): Dp = screenHeight() * 0.01f

    // Timetable sizing.
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
    fun timetableGridContentPaddingHorizontal(): Dp = customWidth() * 0.012f
    @Composable
    fun timetableGridContentPaddingTop(): Dp = screenHeight() * 0.002f
    @Composable
    fun timetableGridContentPaddingBottom(): Dp = screenHeight() * 0.005f
    @Composable
    fun timetableHorizontalSpacing(): Dp = customWidth() * 0.02f
    @Composable
    fun timetableHourFrameWidth(): Dp = customWidth() * 0.08f
    @Composable
    fun timetableMinuteFrameWidth(): Dp = customWidth() - timetableHourFrameWidth() - borderWidth()
    @Composable
    fun timetableTypeMenuWidth(): Dp = customWidth() * 0.50f
    @Composable
    fun timetableEditButtonWidth(): Dp = customWidth() * 0.44f
    @Composable
    fun timetablePickerWidth(): Dp = customWidth() * 0.43f
    @Composable
    fun timetableNumberHeight(): Dp = screenHeight() * 0.02f
    @Composable
    fun timetableGridHeaderHeight(): Dp = screenHeight() * 0.030f
    @Composable
    fun calculateContentHeight(trainTimesCount: Int): Dp {
        val itemsPerRow = 10
        val addRowCount = (trainTimesCount - 1) / itemsPerRow
        return screenHeight() * 0.026f + timetableNumberHeight() * addRowCount
    }
    @Composable
    fun timetableDisplayHeight(): Dp = screenHeight() * 0.07f
    @Composable
    fun timetableDisplayBottomSpacing(): Dp = screenHeight() * 0.006f
    @Composable
    fun timetableMaxHeight(): Dp = screenHeight() * 0.64f
    @Composable
    fun timetableVerticalSpacing(): Dp = screenHeight() * 0.012f
    @Composable
    fun timetableCopyMenuOffsetY(): Dp = screenHeight() * 0.18f  // SettingsTimetableSheet: copy time dropdown
    @Composable
    fun timetableCalendarMenuOffsetY(): Dp = screenHeight() * 0.06f  // SettingsTimetableSheet: calendar type dropdown
    @Composable
    fun timetableContentViewMenuOffsetY(): Dp = screenHeight() * 0.10f  // TimetableContentScreen: calendar type dropdown
    @Composable
    fun timetablePickerTopPadding(): Dp = screenHeight() * 0.000f
    @Composable
    fun timetablePickerBottomPadding(): Dp = screenHeight() * 0.000f

    // Settings screen sizing.
    @Composable
    fun settingsTitleFontSize(): Dp = screenHeight() * 0.022f
    @Composable
    fun settingsHeaderFontSize(): Dp = screenHeight() * 0.016f
    @Composable
    fun settingsHeaderPadding(): Dp = screenHeight() * 0.01f
    @Composable
    fun settingsHeaderIconSize(): Dp = screenHeight() * 0.020f
    @Composable
    fun settingsHeaderIconSpace(): Dp = customWidth() * 0.007f
    @Composable
    fun settingsFontSize(): Dp = screenHeight() * 0.018f
    
    // Common settings sheet sizing.
    @Composable
    fun settingsSheetVerticalSpacing(): Dp = screenHeight() * 0.012f
    @Composable
    fun settingsSheetCompactVerticalSpacing(): Dp = 0.dp  // No gap (Button contentPadding = 0)
    @Composable
    fun settingsSheetHorizontalPadding(): Dp = customWidth() * 0.06f
    @Composable
    fun settingsSheetHorizontalSpacing(): Dp = customWidth() * 0.02f
    @Composable
    fun settingsSheetTitleFontSize(): Dp = customWidth() * 0.036f
    @Composable
    fun settingsSheetHeadlineFontSize(): Dp = customWidth() * 0.030f
    @Composable
    fun settingsSheetInputFontSize(): Dp = customWidth() * 0.036f
    @Composable
    fun settingsSheetButtonFontSize(): Dp = screenHeight() * 0.018f
    @Composable
    fun settingsSheetInputPaddingHorizontal(): Dp = customWidth() * 0.04f
    @Composable
    fun settingsSheetStrokeLineWidth(): Dp = customWidth() * 0.002f
    @Composable
    fun settingsSheetLineTagSize(): Dp = customWidth() * 0.05f
    @Composable
    fun settingsSheetIconSize(): Dp = customWidth() * 0.040f
    @Composable
    fun settingsSheetIconSpacing(): Dp = customWidth() * 0.016f
    @Composable
    fun settingsSheetRectangleButtonPaddingHorizontal(): Dp = customWidth() * 0.030f
    @Composable
    fun settingsSheetRectangleButtonPaddingVertical(): Dp = screenHeight() * 0.00f
    @Composable
    fun settingsSheetRectangleButtonHeight(): Dp = screenHeight() * 0.036f
    @Composable
    fun settingsSheetPickerDigitColumnWidth(): Dp = customWidth() * 0.04f 
    @Composable
    fun settingsSheetPickerDigitSpacing(): Dp = customWidth() * 0.02f  
    @Composable
    fun settingsSheetInputPaddingVertical(): Dp = screenHeight() * 0.002f
    @Composable
    fun settingsSheetCornerRadius(): Dp = screenHeight() * 0.015f
    @Composable
    fun settingsSheetPickerSelectHeight(): Dp = screenHeight() * 0.075f
    @Composable
    fun settingsSheetButtonHeight(): Dp = screenHeight() * 0.044f
    @Composable
    fun settingsSheetButtonCornerRadius(): Dp = screenHeight() * 0.022f
    @Composable
    fun settingsSheetTopBarHeight(): Dp = screenHeight() * 0.06f
    @Composable
    fun settingsSheetBackButtonPadding(): Dp = customWidth() * 0.02f
    @Composable
    fun settingsSheetDropdownContentPaddingVertical(): Dp = screenHeight() * 0.005f
    @Composable
    fun settingsTimetableSheetHeight(): Dp = screenHeight() * 0.6f

    // Settings line sheet sizing.
    @Composable
    fun settingsLineSheetPickerPadding(): Dp = screenHeight() * 0.000f
    @Composable
    fun settingsLineSheetGridSpacing(): Dp = customWidth() * 0.013f
    @Composable
    fun settingsLineSheetSuggestionItemHeight(): Dp = screenHeight() * 0.046f
    @Composable
    fun settingsLineSheetDropdownOffsetX(): Dp = customWidth() * 0.16f
    @Composable
    fun settingsLineSheetDropdownOffsetY(): Dp = screenHeight() * 0.24f
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

    // Alert dialog sizing.
    @Composable
    fun alertDialogTitleFontSize(): Dp = customWidth() * 0.045f
    @Composable
    fun alertDialogTextFontSize(): Dp = customWidth() * 0.036f
    @Composable
    fun alertDialogButtonFontSize(): Dp = customWidth() * 0.033f
    @Composable
    fun alertDialogCornerRadius(): Dp = customWidth() * 0.06f
    @Composable
    fun alertDialogButtonCornerRadius(): Dp = customWidth() * 0.04f
    @Composable
    fun alertDialogElevation(): Dp = customWidth() * 0.015f

    // Custom component sizing.
    @Composable
    fun customToggleSpacing(): Dp = customWidth() * 0.008f
    @Composable
    fun customToggleHeight(): Dp = screenHeight() * 0.03f
    @Composable
    fun customTogglePaddingHorizontal(): Dp = customWidth() * 0.02f
    @Composable
    fun customTextFieldPaddingVertical(): Dp = screenHeight() * 0.009f
    @Composable
    fun customSwitchDefaultHeight(): Dp = screenHeight() * 0.037f
    @Composable
    fun customProgressIndicatorSize(): Dp = customWidth() * 0.1f

    // Common UI element sizes.
    @Composable
    fun dividerWidth(): Dp = customWidth() * 0.003f
    @Composable
    fun shadowOffset(): Dp = customWidth() * 0.0015f
    @Composable
    fun borderWidth(): Dp = customWidth() * 0.002f
}