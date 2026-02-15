package com.mytimetablemaker.extensions

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.mytimetablemaker.models.CustomColor
import com.mytimetablemaker.models.DisplayTrainType
import com.mytimetablemaker.models.ODPTCalendarType
import com.mytimetablemaker.ui.theme.*
import androidx.core.graphics.toColorInt

// Color utilities and mappings.
// Includes hex parsing and domain-specific colors.

// Create a Color from a hex int.
fun Color(hex: Int, opacity: Float = 1.0f): Color {
    val red = ((hex and 0xff0000) shr 16) / 255.0f
    val green = ((hex and 0xff00) shr 8) / 255.0f
    val blue = (hex and 0xff) / 255.0f
    return Color(red, green, blue, opacity)
}

// Create a Color from a hex string, or null if invalid.
fun Color(hex: String): Color? {
    val cleanString = hex.replace("#", "").trim()
    val hexValue = cleanString.toIntOrNull(16) ?: return null
    return Color(hexValue)
}

// String-based color helpers.

// Safe hex parsing with gray fallback on errors.
val String.safeColor: Color
    get() {
        return try {
            val colorString = if (this.startsWith("#")) this else "#$this"
            Color(colorString.toColorInt())
        } catch (e: Exception) {
            Log.d("ColorExtensions", "Failed to parse color '$this': ${e.message}", e)
            Gray
        }
    }

// Countdown color helpers.

// Determine countdown color from departure vs current time.
// Uses `minusHHMMSS` and `SStoMMSS` to align ranges.
fun Int.countdownColor(departTime: Int): Color {
    return (departTime * 100).minusHHMMSS(this).SStoMMSS.countdownColor
}

// Map countdown ranges to colors.
val Int.countdownColor: Color
    get() = when {
        this % 2 == 1 -> Gray
        this in 1000..9999 -> Accent
        this in 100..499 -> Yellow
        this in 0..99 -> Red
        else -> Gray
    }

// Train type color mapping.

// Convert train type string to display color.
fun colorForTrainType(trainType: String?): Color {
    if (trainType == null) {
        return White
    }
    
    val components = trainType.split(".")
    val lastComponent = components.lastOrNull() ?: return White
    
    val displayTrainType = try {
        DisplayTrainType.valueOf(lastComponent.uppercase().replace("-", "_"))
    } catch (e: IllegalArgumentException) {
        Log.d("ColorExtensions", "DisplayTrainType.valueOf failed for '$lastComponent': ${e.message}. Trying rawValue lookup.")
        DisplayTrainType.entries.find { it.rawValue == lastComponent } ?: return White
    }
    
    return displayTrainType.color
}

// Priority order for sorting colors.
val colorPriority: Map<Color, Int> = mapOf(
    White to 0,
    YellowGreen to 1,
    Yellow to 2,
    Orange to 3,
    Pink to 4,
    LightBlue to 5
)

// Color sort key with a high fallback value.
val Color.priorityValue: Int
    get() = colorPriority[this] ?: 999

// Color mapping for `DisplayTrainType`.

// Map train categories to theme colors.
val DisplayTrainType.color: Color
    get() = when (this) {
        // Local trains - White
        DisplayTrainType.DEFAULT_LOCAL,
        DisplayTrainType.LOCAL,
        DisplayTrainType.UNKNOWN -> White
        
        // Express trains - Yellow Green
        DisplayTrainType.DEFAULT_EXPRESS,
        DisplayTrainType.EXPRESS,
        DisplayTrainType.SEMI_EXPRESS,
        DisplayTrainType.SECTION_EXPRESS,
        DisplayTrainType.SECTION_SEMI_EXPRESS,
        DisplayTrainType.COMMUTER_EXPRESS,
        DisplayTrainType.COMMUTER_SEMI_EXPRESS -> YellowGreen
        
        // Rapid trains - Yellow
        DisplayTrainType.DEFAULT_RAPID,
        DisplayTrainType.RAPID,
        DisplayTrainType.RAPID_EXPRESS,
        DisplayTrainType.SEMI_RAPID,
        DisplayTrainType.COMMUTER_RAPID -> Yellow
        
        // Special Rapid trains - Orange
        DisplayTrainType.DEFAULT_SPECIAL_RAPID,
        DisplayTrainType.SPECIAL_RAPID,
        DisplayTrainType.COMMUTER_SPECIAL_RAPID,
        DisplayTrainType.CHUO_SPECIAL_RAPID,
        DisplayTrainType.OME_SPECIAL_RAPID,
        DisplayTrainType.ACCESS_EXPRESS,
        DisplayTrainType.AIRPORT_RAPID_LIMITED_EXPRESS,
        DisplayTrainType.KAWAGOE_LIMITED_EXPRESS,
        DisplayTrainType.F_LINER,
        DisplayTrainType.RAPID_LIMITED_EXPRESS -> Orange
        
        // Limited Express trains - Pink
        DisplayTrainType.DEFAULT_LIMITED_EXPRESS,
        DisplayTrainType.LIMITED_EXPRESS,
        DisplayTrainType.COMMUTER_LIMITED_EXPRESS -> Pink
        
        // Paid trains - Light Blue
        DisplayTrainType.LINER,
        DisplayTrainType.TH_LINER,
        DisplayTrainType.TJ_LINER,
        DisplayTrainType.HAIJIMA_LINER,
        DisplayTrainType.S_TRAIN,
        DisplayTrainType.SL_TAIJU,
        DisplayTrainType.EVENING_WING,
        DisplayTrainType.MORNING_WING -> LightBlue
    }

// `CustomColor` parsing and metadata.

// Parse RGB string into a safe Color.
val CustomColor.color: Color
    get() = run {
        val rgbString = this.RGB
        val hexString = if (rgbString.startsWith("#")) rgbString else "#$rgbString"
        
        try {
            val parsedColor = Color(hexString.toColorInt())
            if (parsedColor.alpha > 0f && parsedColor != Color.Unspecified && parsedColor != Color.Transparent) {
                parsedColor
            } else {
                // Gray fallback avoids circular reference.
                Color(0xFF9C9C9C)
            }
        } catch (e: Exception) {
            Log.d("ColorExtensions", "Failed to parse CustomColor $this rgb=$rgbString: ${e.message}", e)
            Color(0xFF9C9C9C)
        }
    }

// Hex values for each `CustomColor`.
val CustomColor.RGB: String
    get() = when (this) {
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

// Resource name for localization (camelCase from raw value).
val CustomColor.resourceName: String
    get() = when (this) {
        CustomColor.PRIMARY -> "indigo"
        CustomColor.ACCENT -> "defaultColor"
        else -> {
            // Convert words into camelCase (e.g., "DARK RED" -> "darkRed").
            val words = this.rawValue.lowercase().split(" ")
            words[0] + words.drop(1).joinToString("") {
                it.replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(java.util.Locale.getDefault())
                    } else {
                        char.toString()
                    }
                }
            }
        }
    }

// ODPT calendar color helpers.

// Primary label color by day type (weekend red, weekday white).
// Converts .specific to display type first.
val ODPTCalendarType.calendarColor: Color
    get() {
        val displayType = this.displayCalendarType()
        return when (displayType) {
            is ODPTCalendarType.Holiday,
            is ODPTCalendarType.Sunday,
            is ODPTCalendarType.Saturday,
            is ODPTCalendarType.SaturdayHoliday -> Red
            is ODPTCalendarType.Monday,
            is ODPTCalendarType.Tuesday,
            is ODPTCalendarType.Wednesday,
            is ODPTCalendarType.Thursday,
            is ODPTCalendarType.Friday,
            is ODPTCalendarType.Weekday -> White
            is ODPTCalendarType.Specific -> White // Fallback
        }
    }

// Secondary text color by day type (weekend red, weekday black).
// Converts .specific to display type first.
val ODPTCalendarType.calendarSubColor: Color
    get() {
        val displayType = this.displayCalendarType()
        return when (displayType) {
            is ODPTCalendarType.Holiday,
            is ODPTCalendarType.Sunday,
            is ODPTCalendarType.Saturday,
            is ODPTCalendarType.SaturdayHoliday -> Red
            is ODPTCalendarType.Monday,
            is ODPTCalendarType.Tuesday,
            is ODPTCalendarType.Wednesday,
            is ODPTCalendarType.Thursday,
            is ODPTCalendarType.Friday,
            is ODPTCalendarType.Weekday -> Black
            is ODPTCalendarType.Specific -> Black // Fallback
        }
    }
