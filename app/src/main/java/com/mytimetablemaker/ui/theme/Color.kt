package com.mytimetablemaker.ui.theme

import androidx.compose.ui.graphics.Color
import com.mytimetablemaker.models.CustomColor
import androidx.core.graphics.toColorInt

// Application theme colors derived from CustomColor enum

// Theme colors
val Primary = CustomColor.PRIMARY.color()
val Accent = CustomColor.ACCENT.color()

// Standard palette
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

const val AccentString = "#03DAC5"

// Convert CustomColor enum to Compose Color
fun CustomColor.color(): Color {
    return Color(this.rgb().toColorInt())
}

// Get hex color string for CustomColor enum
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
