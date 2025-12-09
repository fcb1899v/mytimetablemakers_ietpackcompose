package com.mytimetablemaker.extensions

import androidx.compose.ui.graphics.Color
import com.mytimetablemaker.models.DisplayTrainType
import com.mytimetablemaker.models.ODPTCalendarType
import com.mytimetablemaker.ui.theme.*

// MARK: - Color Extensions
// Extensions for color management and hex color support
// Matches SwiftUI ColorExtensions structure

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

// Convert hex color string to integer value
val String.colorInt: Int
    get() {
        val cleanString = this.replace("#", "").trim()
        if (cleanString.isEmpty()) {
            return 0xAAAAAA // Default gray color
        }
        return cleanString.toIntOrNull(16) ?: 0xAAAAAA
    }

// Safely convert hex string to Color with fallback
val String.safeColor: Color
    get() = Color(this.colorInt)

// MARK: - Integer Color Extensions
// Extensions for countdown color calculations

// Calculate color based on countdown time relative to departure time
fun Int.countdownColor(departTime: Int): Color {
    return (departTime * 100).minusHHMMSS(this).HHMMSStoMMSS.countdownColor
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
        DisplayTrainType.values().find { it.rawValue == lastComponent } ?: return White
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
