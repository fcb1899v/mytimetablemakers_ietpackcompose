package com.mytimetablemaker.ui.theme

import androidx.compose.ui.graphics.Color
import com.mytimetablemaker.models.CustomColor
import androidx.core.graphics.toColorInt

// MARK: - App Theme Colors
// Application-wide color constants
// These colors are derived from CustomColor enum to maintain consistency

// Primary theme color (Indigo - #3700B3)
val Primary = CustomColor.PRIMARY.color()

// Accent color (Default - #03DAC5)
val Accent = CustomColor.ACCENT.color()

// Standard colors
val Red = CustomColor.RED.color()
val Yellow = CustomColor.YELLOW.color()
val Gray = CustomColor.GRAY.color()
val LightGray = Color(0.95f, 0.95f, 0.97f, 0.95f)
val YellowGreen = CustomColor.YELLOW_GREEN.color()
val Orange = CustomColor.ORANGE.color()
val Pink = CustomColor.PINK.color()
val LightBlue = CustomColor.LIGHT_BLUE.color()
val White = Color.White
val Black = Color.Black

// Hex string values for color storage
const val AccentString = "#03DAC5"

// MARK: - CustomColor Extension
// Convert CustomColor enum to Color object
fun CustomColor.color(): Color {
    return Color(this.rgb().toColorInt())
}

// Get RGB hex string value for CustomColor
fun CustomColor.rgb(): String {
    return when (this) {
        CustomColor.RED -> "#E60012"
        CustomColor.DARK_RED -> "#A22041"
        CustomColor.ORANGE -> "#F58220"
        CustomColor.BROWN -> "#8F4C38"
        CustomColor.YELLOW -> "#FFD400"
        CustomColor.BEIGE -> "#C1A470"
        CustomColor.YELLOW_GREEN -> "#9ACD32"
        CustomColor.OLIVE -> "#9FB01C"
        CustomColor.GREEN -> "#009739"
        CustomColor.DARK_GREEN -> "#004E2E"
        CustomColor.BLUE_GREEN -> "#00AC9A"
        CustomColor.LIGHT_BLUE -> "#00BFFF"
        CustomColor.BLUE -> "#0000FF"
        CustomColor.NAVY_BLUE -> "#003580"
        CustomColor.PRIMARY -> "#3700B3"
        CustomColor.LAVENDER -> "#8F76D6"
        CustomColor.PURPLE -> "#B22C8D"
        CustomColor.MAGENTA -> "#E4007F"
        CustomColor.PINK -> "#E85298"
        CustomColor.GRAY -> "#9C9C9C"
        CustomColor.SILVER -> "#89A1AD"
        CustomColor.GOLD -> "#C5C544"
        CustomColor.BLACK -> "#000000"
        CustomColor.ACCENT -> "#03DAC5"
    }
}
