package com.mytimetablemaker.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import java.text.SimpleDateFormat
import java.util.*

// MARK: - Time Extensions
// Extensions for time formatting and calculations

// Format time values for display with leading zeros
fun Int.addZeroTime(): String {
    return if (this in 0..9) "0$this" else "$this"
}

// MARK: - Int Time Extensions
// Time format conversions and calculations

// Convert MMSS to seconds
val Int.MMSStoSS: Int
    get() = this / 100 * 60 + this % 100

// Convert seconds to MMSS
val Int.SStoMMSS: Int
    get() = (this / 60) * 100 + (this % 60)

// Convert seconds to HHMMSS
val Int.SStoHHMMSS: Int
    get() = (this / 3600) * 10000 + ((this % 3600) / 60) * 100 + (this % 60)

// Add HHMMSS time
fun Int.plusHHMMSS(time: Int): Int {
    return (this.HHMMSStoSS + time.HHMMSStoSS).SStoHHMMSS
}

// Add MMSS time
fun Int.plusMMSS(time: Int): Int {
    return (this.MMSStoSS + time.MMSStoSS).SStoMMSS
}

// Subtract MMSS time
fun Int.minusMMSS(time: Int): Int {
    return (this.MMSStoSS - time.MMSStoSS).SStoMMSS
}

// Get hour component from HHMM format
val Int.timeHH: String
    get() = (this / 100 + (this % 100) / 60).addZeroTime()

// Get minute component from HHMM format
val Int.timeMM: String
    get() = (this % 100 % 60).addZeroTime()

// Get timetable hour (0-3 AM becomes 24-27)
val Int.timetableHour: Int
    get() = if (this > 3) this else this + 24

// Remove leading space from time string
fun String.timeString(): String {
    return if (this.startsWith(" ")) this.substring(1) else this
}

// Convert HH:MM format to total minutes for sorting
fun String.timeToMinutes(): Int {
    val components = this.split(":")
    if (components.size == 2) {
        val hour = components[0].toIntOrNull()
        val minute = components[1].toIntOrNull()
        if (hour != null && minute != null) {
            // timetableHour: (hour > 3) ? hour : hour + 24
            val timetableHour = if (hour > 3) hour else hour + 24
            return timetableHour * 60 + minute
        }
    }
    return 0
}

// MARK: - Date Extensions
// Format date for display
fun Date.setDate(context: Context): String {
    val formatter = SimpleDateFormat("E, MMM d, yyyy", Locale.getDefault())
    return formatter.format(this)
}

// Format time for display
fun Date.setTime(): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(this)
}

// Convert current date to HHMMSS format integer
val Date.currentTime: Int
    get() {
        val calendar = Calendar.getInstance()
        calendar.time = this
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        return hour * 10000 + minute * 100 + second
    }

// MARK: - Int Extensions
// Convert HHMM format to HH:MM string format
fun Int.stringTime(): String {
    val timeHH = (this / 100 + (this % 100) / 60).addZeroTime()
    val timeMM = (this % 100 % 60).addZeroTime()
    val timeString = "$timeHH:$timeMM"
    return if (timeString != "27:00") timeString else "--:--"
}

// Convert HHMMSS to MMSS format
val Int.HHMMSStoMMSS: Int
    get() = (this / 10000 * 60 + (this % 10000) / 100) * 100 + this % 100

// Convert HHMMSS to seconds
val Int.HHMMSStoSS: Int
    get() = this / 10000 * 3600 + (this % 10000) / 100 * 60 + this % 100

// Subtract HHMMSS time
fun Int.minusHHMMSS(time: Int): Int {
    val diff = this.HHMMSStoSS - time.HHMMSStoSS
    return if (diff < 0) {
        (diff + 86400) % 86400
    } else {
        diff
    }
}

// Format countdown timer display from MMSS format
val Int.countdown: String
    get() {
        return if (this in 0..9999) {
            "${(this / 100).addZeroTime()}:${(this % 100).addZeroTime()}"
        } else {
            "--:--"
        }
    }

// Calculate countdown time from departure time
fun Int.countdownTime(departTime: Int): String {
    return (departTime * 100).minusHHMMSS(this).HHMMSStoMMSS.countdown
}

// Convert HHMM to minutes
val Int.HHMMtoMM: Int
    get() = this / 100 * 60 + this % 100

// Convert minutes to HHMM
val Int.MMtoHHMM: Int
    get() = (this / 60) * 100 + (this % 60)

// Add HHMM time
fun Int.plusHHMM(time: Int): Int {
    return (this.HHMMtoMM + time.HHMMtoMM).MMtoHHMM
}

// Subtract HHMM time
fun Int.minusHHMM(time: Int): Int {
    return if (this.HHMMtoMM < time.HHMMtoMM) {
        ((this + 2400).HHMMtoMM - time.HHMMtoMM).MMtoHHMM
    } else {
        (this.HHMMtoMM - time.HHMMtoMM).MMtoHHMM
    }
}

// Limit time to 2700 (27:00)
fun Int.overTime(beforeTime: Int): Int {
    return if (beforeTime == 2700) 2700 else if (this > 2700) 2700 else this
}

// MARK: - Date Extensions (continued)
// Determine calendar type based on date with fallback to available types
fun Date.odptCalendarType(availableTypes: List<String>): String {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    
    // Check for weekday (Monday-Friday, not a holiday)
    val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
    // TODO: Implement Japanese holiday detection
    val isHoliday = false // Placeholder
    
    if (isWeekday && !isHoliday) {
        // Check for specific weekday types
        when (dayOfWeek) {
            Calendar.MONDAY -> if (availableTypes.contains("monday")) return "monday"
            Calendar.TUESDAY -> if (availableTypes.contains("tuesday")) return "tuesday"
            Calendar.WEDNESDAY -> if (availableTypes.contains("wednesday")) return "wednesday"
            Calendar.THURSDAY -> if (availableTypes.contains("thursday")) return "thursday"
            Calendar.FRIDAY -> if (availableTypes.contains("friday")) return "friday"
        }
        // Fallback to weekday
        if (availableTypes.contains("weekday")) return "weekday"
    }
    
    // Check for holiday/weekend calendar types
    if (isHoliday && availableTypes.contains("holiday")) return "holiday"
    if (dayOfWeek == Calendar.SUNDAY && availableTypes.contains("sunday")) return "sunday"
    if (dayOfWeek == Calendar.SATURDAY && availableTypes.contains("saturday")) return "saturday"
    if ((isHoliday || dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) && 
        availableTypes.contains("saturdayHoliday")) return "saturdayHoliday"
    
    // Final fallback
    return availableTypes.firstOrNull() ?: "weekday"
}

// MARK: - String Extensions
// Parse time string (HH:MM:SS) to integer format (HHMMSS)
val String.currentTime: Int
    get() {
        val components = this.split(":")
        if (components.size >= 2) {
            val hour = components[0].toIntOrNull() ?: 0
            val minute = components[1].toIntOrNull() ?: 0
            val second = if (components.size >= 3) components[2].toIntOrNull() ?: 0 else 0
            return hour * 10000 + minute * 100 + second
        }
        return 0
    }

// Load available calendar types for a route and line
fun String.loadAvailableCalendarTypes(sharedPreferences: SharedPreferences, num: Int): List<String> {
    // Check line-level cache first
    val lineCacheKey = "${this}line${num + 1}_calendarTypes"
    val cachedTypes = sharedPreferences.getStringSet(lineCacheKey, null)
    if (cachedTypes != null && cachedTypes.isNotEmpty()) {
        return cachedTypes.toList()
    }
    
    // TODO: Implement detection from actual data
    // For now, return default types
    return listOf("weekday", "saturdayHoliday")
}

// Generate time array for route based on date and current time
fun String.timeArray(
    sharedPreferences: SharedPreferences,
    date: Date,
    currentTime: Int
): List<Int> {
    val changeLineInt = this.changeLineInt(sharedPreferences)
    val transferTimeArray = this.transferTimeArray(sharedPreferences)
    val timetableArray = this.timetableArray(sharedPreferences, date)
    
    // TODO: Implement full timeArray logic
    // For now, return empty list
    return emptyList()
}

// MARK: - String Time Extensions
// Adjust hour for timetable display (0-3 AM becomes 24-27)
fun String.adjustedForTimetable(): String {
    val components = this.split(":")
    if (components.size != 2) return this
    
    val hour = components[0].toIntOrNull() ?: return this
    val minute = components[1].toIntOrNull() ?: return this
    
    // Adjust hour for timetable display (0-3 AM becomes 24-27)
    val adjustedHour = if (hour in 0..3) hour + 24 else hour
    
    return "${adjustedHour.addZeroTime()}:${minute.addZeroTime()}"
}

// MARK: - String Calendar Type Cache Extensions
// Clear cache when calendar types are updated (called from SettingsLineViewModel)
fun String.clearCalendarTypesCache(num: Int) {
    val cacheKey = "${this}_line${num}"
    // TODO: Implement cache clearing if using a cache map
    // For now, this is handled by SharedPreferences removal in SettingsTimetableViewModel
}

// MARK: - String Minutes Only Extension
// Extract minutes from time string (HH:MM format or minutes-only format)
val String.minutesOnly: String
    get() {
        // Check if string contains ":" (HH:MM format)
        if (this.contains(":")) {
            val components = this.split(":")
            if (components.size == 2) {
                val minutes = components[1]
                val minutesInt = minutes.toIntOrNull()
                if (minutesInt != null && minutesInt < 10) {
                    return String.format("%02d", minutesInt)
                }
                return minutes
            }
        }
        
        // Handle minutes-only format (e.g., "5", "05", "24")
        val minutesInt = this.toIntOrNull()
        if (minutesInt != null) {
            if (minutesInt < 10) {
                return String.format("%02d", minutesInt)
            }
            return this
        }
        
        // Fallback: return original string
        return this
    }

// MARK: - String Time Comparison Extension
// Compare two time strings (single or double digit format)
fun String.isTimeLessThan(other: String): Boolean {
    val time1 = this.toIntOrNull() ?: 0
    val time2 = other.toIntOrNull() ?: 0
    return time1 < time2
}

// MARK: - String Timetable Components Extension
// Parse space-separated timetable string into array of non-empty strings
val String.timetableComponents: List<String>
    get() = this.split(" ").filter { it.isNotEmpty() }

// MARK: - String Time Format Validation Extension
// Check if timetable string contains time in both single and double digit formats
fun String.containsTimeInAnyFormat(departureTime: Int): Boolean {
    val existingTimes = this.timetableComponents
    val singleDigitTime = departureTime.toString()
    val doubleDigitTime = departureTime.addZeroTime()
    return existingTimes.contains(singleDigitTime) || existingTimes.contains(doubleDigitTime)
}

// MARK: - Int Choice Copy Time List Extension
// Generate list of copy time options for hour selection
@Composable
fun Int.choiceCopyTimeList(context: android.content.Context): List<String> {
    // Note: hour string resource is ":00-" which matches SwiftUI's "Hour" localized string
    val hourString = context.getString(com.mytimetablemaker.R.string.hour)
    return listOf(
        "${this - 1}$hourString",
        "${this + 1}$hourString",
        context.getString(com.mytimetablemaker.R.string.otherRouteOfLine1),
        context.getString(com.mytimetablemaker.R.string.otherRouteOfLine2),
        context.getString(com.mytimetablemaker.R.string.otherRouteOfLine3)
    )
}

// MARK: - String Other Route Extension
// Get the other route identifier (back1 <-> back2, go1 <-> go2)
val String.otherroute: String
    get() {
        val lastChar = this.lastOrNull() ?: return this
        return if (lastChar == '1') {
            this.dropLast(1) + "2"
        } else {
            this.dropLast(1) + "1"
        }
    }
