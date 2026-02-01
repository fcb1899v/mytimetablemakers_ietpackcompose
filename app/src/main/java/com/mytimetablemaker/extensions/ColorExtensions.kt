package com.mytimetablemaker.extensions

import androidx.compose.ui.graphics.Color
import com.mytimetablemaker.models.CustomColor
import com.mytimetablemaker.models.DisplayTrainType
import com.mytimetablemaker.models.ODPTCalendarType
import com.mytimetablemaker.ui.theme.*
import androidx.core.graphics.toColorInt

// MARK: - Color Extensions
// Extensions for color management and hex color support

// MARK: - Hex Color Initializer
// Initialize color from hex integer value
fun Color(hex: Int, opacity: Float = 1.0f): Color {
    val red = ((hex and 0xff0000) shr 16) / 255.0f
    val green = ((hex and 0xff00) shr 8) / 255.0f
    val blue = (hex and 0xff) / 255.0f
    return Color(red, green, blue, opacity)
}

// Initialize color from hex string value
fun Color(hex: String): Color? {
    val cleanString = hex.replace("#", "").trim()
    val hexValue = cleanString.toIntOrNull(16) ?: return null
    return Color(hexValue)
}

// MARK: - String Color Extensions
// Extensions for string-based color operations

// Safely convert hex string to Color with fallback
val String.safeColor: Color
    get() {
        return try {
            val colorString = if (this.startsWith("#")) this else "#$this"
            Color(colorString.toColorInt())
        } catch (e: Exception) {
            // Fallback to default gray color if parsing fails
            Gray
        }
    }

// MARK: - Integer Color Extensions
// Extensions for countdown color calculations

// Calculate color based on countdown time relative to departure time
// minusHHMMSS returns seconds; convert to MMSS with SStoMMSS for countdownColor ranges
fun Int.countdownColor(departTime: Int): Color {
    return (departTime * 100).minusHHMMSS(this).SStoMMSS.countdownColor
}

// Determine color based on countdown time ranges
val Int.countdownColor: Color
    get() = when {
        this % 2 == 1 -> Gray
        this in 1000..9999 -> Accent
        this in 500..999 -> Yellow
        this in 0..499 -> Red
        else -> Gray
    }

// MARK: - Train Type Color Management
// Extensions for train type color mapping and management

// Convert train type string to appropriate color
fun colorForTrainType(trainType: String?): Color {
    if (trainType == null) {
        return White
    }
    
    val components = trainType.split(".")
    val lastComponent = components.lastOrNull() ?: return White
    
    val displayTrainType = try {
        DisplayTrainType.valueOf(lastComponent.uppercase().replace("-", "_"))
    } catch (e: IllegalArgumentException) {
        // Try to find by rawValue
        DisplayTrainType.entries.find { it.rawValue == lastComponent } ?: return White
    }
    
    return displayTrainType.color
}

// Priority mapping for sorting train types by color order
val colorPriority: Map<Color, Int> = mapOf(
    White to 0,
    YellowGreen to 1,
    Yellow to 2,
    Orange to 3,
    Pink to 4,
    LightBlue to 5
)

// Get priority value for color sorting, returns high value for unknown colors
val Color.priorityValue: Int
    get() = colorPriority[this] ?: 999

// MARK: - Display Train Type Extensions
// Extensions for DisplayTrainType enum to provide color mapping

// Map train types to colors
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

// MARK: - CustomColor Extensions
// Extensions for CustomColor enum to provide RGB color values

// Convert RGB string to Color object
val CustomColor.color: Color
    get() = run {
        val rgbString = this.RGB
        val hexString = if (rgbString.startsWith("#")) rgbString else "#$rgbString"
        
        try {
            val parsedColor = Color(hexString.toColorInt())
            if (parsedColor.alpha > 0f && parsedColor != Color.Unspecified && parsedColor != Color.Transparent) {
                parsedColor
            } else {
                // Fallback to gray color directly using hex value to avoid circular reference
                Color(0xFF9C9C9C)
            }
        } catch (e: Exception) {
            // Fallback to gray color directly using hex value to avoid circular reference
            Color(0xFF9C9C9C)
        }
    }

// Hex color values for each custom color
val CustomColor.RGB: String
    get() = when (this) {
        CustomColor.RED -> "#E60012"
        CustomColor.DARK_RED -> "#A22041"
        CustomColor.ORANGE -> "#F58220"
        CustomColor.BROWN -> "#8F4C38"
        CustomColor.YELLOW -> "#FFD400"
        CustomColor.BEIGE -> "#C1A470"
        CustomColor.YELLOW_GREEN -> "#9ACD32"
        CustomColor.ORIVE -> "#9FB01C"
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

// Resource name for localization
// Converts rawValue to string resource name format (camelCase)
val CustomColor.resourceName: String
    get() = when (this) {
        CustomColor.PRIMARY -> "indigo"
        CustomColor.ACCENT -> "defaultColor"
        else -> {
            // Convert "DARK RED" to "darkRed", "YELLOW GREEN" to "yellowGreen", etc.
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

// MARK: - ODPT Calendar Type Color Extensions
// Color extensions for ODPT calendar types

// Returns primary color for calendar labels (weekend = red, weekday = white)
// Used for quick visual distinction between weekend and weekday
// For .specific types, converts to display type first
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

// Returns secondary text color for calendar labels (weekend = red, weekday = black)
// Keeps contrast readable against calendarColor background
// For .specific types, converts to display type first
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
