package com.mytimetablemaker.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import com.mytimetablemaker.R
import com.mytimetablemaker.models.ODPTCalendarType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Time formatting and calculation helpers.
// Keeps app-specific time rules together.

// Format time values with leading zeros.
fun Int.addZeroTime(): String {
    return if (this in 0..9) "0$this" else "$this"
}

// Int time conversions and calculations.

// Convert seconds to MMSS.
val Int.SStoMMSS: Int
    get() = (this / 60) * 100 + (this % 60)

// Get hour component from HHMM.
val Int.timeHH: String
    get() = (this / 100 + (this % 100) / 60).addZeroTime()

// Get minute component from HHMM.
val Int.timeMM: String
    get() = (this % 100 % 60).addZeroTime()

// Map 0-3 AM to 24-27 for timetable hours.
val Int.timetableHour: Int
    get() = if (this > 3) this else this + 24

// Remove a leading space from time strings.
val String.timeString: String
    get() = if (this.startsWith(" ")) this.substring(1) else this

// Convert HH:MM to total minutes for sorting.
val String.timeToMinutes: Int get() {
    val components = this.split(":")
    if (components.size == 2) {
        val hour = components[0].toIntOrNull()
        val minute = components[1].toIntOrNull()
        if (hour != null && minute != null) {
            return hour.timetableHour * 60 + minute
        }
    }
    return 0
}

// Date formatting (locale-aware via resources).
fun Date.formatDate(context: Context): String {
    val pattern = context.getString(R.string.dateFormat)
    val locales = context.resources.configuration.locales
    val locale = if (locales.isEmpty) Locale.getDefault() else locales.get(0)
    val formatter = SimpleDateFormat(pattern, locale)
    return formatter.format(this)
}

// Format time for display.
val Date.setTime: String get() {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(this)
}

// Convert HHMM to HH:MM string.
val Int.stringTime: String get() {
    val timeString = "${this.timeHH}:${this.timeMM}"
    return if (timeString != "27:00") timeString else "--:--"
}

// Convert HHMMSS to seconds.
val Int.HHMMSStoSS: Int
    get() = this / 10000 * 3600 + (this % 10000) / 100 * 60 + this % 100

// Subtract HHMMSS times with wrap-around.
fun Int.minusHHMMSS(time: Int): Int {
    val diff = this.HHMMSStoSS - time.HHMMSStoSS
    return if (diff < 0) (diff + 86400) % 86400 else diff
}

// Format countdown display from MMSS.
val Int.countdown: String
    get() {
        return if (this in 0..9999) "${(this / 100).addZeroTime()}:${(this % 100).addZeroTime()}" else "--:--"
    }

// Calculate countdown string from departure time.
// Uses `minusHHMMSS` and `SStoMMSS` for MM:SS.
fun Int.countdownTime(departTime: Int): String {
    return (departTime * 100).minusHHMMSS(this).SStoMMSS.countdown
}

// Convert HHMM to minutes.
val Int.HHMMtoMM: Int
    get() = this / 100 * 60 + this % 100

// Convert minutes to HHMM.
val Int.MMtoHHMM: Int
    get() = (this / 60) * 100 + (this % 60)

// Add HHMM times.
fun Int.plusHHMM(time: Int): Int {
    return (this.HHMMtoMM + time.HHMMtoMM).MMtoHHMM
}

// Subtract HHMM times with day wrap.
fun Int.minusHHMM(time: Int): Int {
    return if (this.HHMMtoMM < time.HHMMtoMM) ((this + 2400).HHMMtoMM - time.HHMMtoMM).MMtoHHMM else (this.HHMMtoMM - time.HHMMtoMM).MMtoHHMM
}

// Clamp time to 27:00.
fun Int.overTime(beforeTime: Int): Int {
    return if (beforeTime == 2700) 2700 else if (this > 2700) 2700 else this
}

// Determine calendar type with fallback to available types.
// Returns ODPTCalendarType directly.
fun Date.odptCalendarType(fallbackTo: List<ODPTCalendarType>): ODPTCalendarType {
    // Check whether a type or its display type is available.
    fun isAvailable(type: ODPTCalendarType): Boolean {
        return fallbackTo.contains(type) || fallbackTo.any { it.displayCalendarType() == type }
    }
    
    // Find the matching .specific type for a display type.
    fun findSpecificType(displayType: ODPTCalendarType): ODPTCalendarType? {
        return fallbackTo.firstOrNull { it.displayCalendarType() == displayType }
    }
    
    // TODO: Implement Japanese holiday detection.
    val isHoliday = false // Placeholder.
    val calendar = Calendar.getInstance()
    calendar.time = this
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    
    // Weekday check (Mon-Fri, not a holiday).
    val isWeekdayDate = !isHoliday && dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.SATURDAY
    
    // Prioritize weekday calendar types on weekdays.
    if (isWeekdayDate) {
        // Check for specific weekday types first.
        if (dayOfWeek == Calendar.MONDAY && isAvailable(ODPTCalendarType.Monday)) {
            return findSpecificType(ODPTCalendarType.Monday) ?: ODPTCalendarType.Monday
        }
        if (dayOfWeek == Calendar.TUESDAY && isAvailable(ODPTCalendarType.Tuesday)) {
            return findSpecificType(ODPTCalendarType.Tuesday) ?: ODPTCalendarType.Tuesday
        }
        if (dayOfWeek == Calendar.WEDNESDAY && isAvailable(ODPTCalendarType.Wednesday)) {
            return findSpecificType(ODPTCalendarType.Wednesday) ?: ODPTCalendarType.Wednesday
        }
        if (dayOfWeek == Calendar.THURSDAY && isAvailable(ODPTCalendarType.Thursday)) {
            return findSpecificType(ODPTCalendarType.Thursday) ?: ODPTCalendarType.Thursday
        }
        if (dayOfWeek == Calendar.FRIDAY && isAvailable(ODPTCalendarType.Friday)) {
            return findSpecificType(ODPTCalendarType.Friday) ?: ODPTCalendarType.Friday
        }
        // Fall back to weekday if available.
        if (isAvailable(ODPTCalendarType.Weekday)) {
            return findSpecificType(ODPTCalendarType.Weekday) ?: ODPTCalendarType.Weekday
        }
    }
    
    // Check holiday/weekend calendar types.
    if (isHoliday && isAvailable(ODPTCalendarType.Holiday)) {
        return findSpecificType(ODPTCalendarType.Holiday) ?: ODPTCalendarType.Holiday
    }
    if (dayOfWeek == Calendar.SUNDAY && isAvailable(ODPTCalendarType.Sunday)) {
        return findSpecificType(ODPTCalendarType.Sunday) ?: ODPTCalendarType.Sunday
    }
    if (dayOfWeek == Calendar.SATURDAY && isAvailable(ODPTCalendarType.Saturday)) {
        return findSpecificType(ODPTCalendarType.Saturday) ?: ODPTCalendarType.Saturday
    }
    if ((isHoliday || dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) && 
        isAvailable(ODPTCalendarType.SaturdayHoliday)) {
        return findSpecificType(ODPTCalendarType.SaturdayHoliday) ?: ODPTCalendarType.SaturdayHoliday
    }
    
    // Final fallback: weekday if available, else first available type.
    if (isAvailable(ODPTCalendarType.Weekday)) {
        return findSpecificType(ODPTCalendarType.Weekday) ?: ODPTCalendarType.Weekday
    }
    
    // If weekday is unavailable, use the first available type.
    return fallbackTo.firstOrNull() ?: ODPTCalendarType.Weekday
}

// Check if availableTypes contains a calendar type (short form or rawValue).
private fun List<String>.hasCalendarType(shortForm: String, rawValueSuffix: String): Boolean =
    any { it.equals(shortForm, ignoreCase = true) || it.endsWith(":$rawValueSuffix") }

// Legacy version: returns String for backward compatibility.
fun Date.odptCalendarType(availableTypes: List<String>): String {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    
    // Check for weekday (Mon-Fri, not a holiday).
    val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
    // TODO: Implement Japanese holiday detection.
    val isHoliday = false // Placeholder.
    
    if (isWeekday && !isHoliday) {
        // Check for specific weekday types.
        when (dayOfWeek) {
            Calendar.MONDAY -> if (availableTypes.hasCalendarType("monday", "Monday")) return "monday"
            Calendar.TUESDAY -> if (availableTypes.hasCalendarType("tuesday", "Tuesday")) return "tuesday"
            Calendar.WEDNESDAY -> if (availableTypes.hasCalendarType("wednesday", "Wednesday")) return "wednesday"
            Calendar.THURSDAY -> if (availableTypes.hasCalendarType("thursday", "Thursday")) return "thursday"
            Calendar.FRIDAY -> if (availableTypes.hasCalendarType("friday", "Friday")) return "friday"
        }
        // Fall back to weekday.
        if (availableTypes.hasCalendarType("weekday", "Weekday")) return "weekday"
    }
    
    // Check holiday/weekend calendar types.
    // For 2-type fallback, weekend/holiday -> saturdayHoliday.
    if ((isHoliday || dayOfWeek == Calendar.SUNDAY) && availableTypes.hasCalendarType("holiday", "Holiday")) return "holiday"
    if (dayOfWeek == Calendar.SUNDAY && availableTypes.hasCalendarType("sunday", "Sunday")) return "sunday"
    if (dayOfWeek == Calendar.SATURDAY && availableTypes.hasCalendarType("saturday", "Saturday")) return "saturday"
    if ((isHoliday || dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) &&
        availableTypes.hasCalendarType("saturdayHoliday", "SaturdayHoliday")) return "saturdayHoliday"
    
    // Final fallback.
    return availableTypes.firstOrNull() ?: "weekday"
}

// Parse time string (HH:MM:SS) into HHMMSS int.
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

// Detect available calendar types from timetable data.
fun String.detectAvailableCalendarTypesFromData(
    num: Int,
    sharedPreferences: SharedPreferences
): List<ODPTCalendarType> {
    val detectedTypes = mutableSetOf<ODPTCalendarType>()
    
    // Check all possible calendar types.
    val allCalendarTypes = ODPTCalendarType.allCases
    
    for (calendarType in allCalendarTypes) {
        if (this.hasTimetableDataForType(calendarType, num, sharedPreferences)) {
            detectedTypes.add(calendarType)
        }
    }
    
    // Also check cached calendar types, including .specific cases.
    val lineCacheKey = "${this}line${num + 1}_calendarTypes"
    val cachedTypes = sharedPreferences.getStringSet(lineCacheKey, null)
    if (cachedTypes != null) {
        for (cachedTypeString in cachedTypes) {
            val cachedCalendarType = ODPTCalendarType.fromRawValue(cachedTypeString)
            if (cachedCalendarType != null) {
                // For .specific types, check this line's data only.
                if (cachedCalendarType is ODPTCalendarType.Specific) {
                    if (this.hasTimetableDataForType(cachedCalendarType, num, sharedPreferences)) {
                        detectedTypes.add(cachedCalendarType)
                        // Add displayCalendarType so it appears in the dropdown.
                        val displayType = cachedCalendarType.displayCalendarType()
                        if (displayType != cachedCalendarType && !detectedTypes.contains(displayType)) {
                            // Check data for the display type as well.
                            if (this.hasTimetableDataForType(displayType, num, sharedPreferences)) {
                                detectedTypes.add(displayType)
                            }
                        }
                    }
                } else {
                    // For non-specific types, add if data exists.
                    if (this.hasTimetableDataForType(cachedCalendarType, num, sharedPreferences)) {
                        detectedTypes.add(cachedCalendarType)
                    }
                }
            }
        }
    }
    
    return detectedTypes.sortedBy { it.rawValue }
}

// Fallback when no cache: weekday and SaturdayHoliday.
private val FALLBACK_CALENDAR_TYPES = listOf(
    "odpt.Calendar:Weekday",
    "odpt.Calendar:SaturdayHoliday"
)

fun String.loadAvailableCalendarTypes(sharedPreferences: SharedPreferences, num: Int): List<String> {
    // Line-level cache: calendar types from ODPT/GTFS fetch.
    // Empty cache falls back to weekday and saturdayHoliday.
    val lineCacheKey = "${this}line${num + 1}_calendarTypes"
    val cachedTypes = sharedPreferences.getStringSet(lineCacheKey, null)
    if (!cachedTypes.isNullOrEmpty()) {
        val cachedCalendarTypes = cachedTypes.mapNotNull { ODPTCalendarType.fromRawValue(it) }
        if (cachedCalendarTypes.isNotEmpty()) {
            return cachedCalendarTypes.map { it.rawValue }
        }
    }

    // Cache empty: use standard types.
    return FALLBACK_CALENDAR_TYPES
}

// Generate time array for a route based on date and current time.
fun String.timeArray(
    sharedPreferences: SharedPreferences,
    date: Date,
    currentTime: Int
): List<Int> {
    val changeLineInt = this.changeLineInt(sharedPreferences)
    val transferTimeArray = this.transferTimeArray(sharedPreferences)
    val timetableArray = this.timetableArray(sharedPreferences, date)
    val rideTimeArray = this.rideTimeArray(sharedPreferences)
    
    // Depart time of line 1.
    val currentTimeHHMM = currentTime / 100
    val firstDepartTime = timetableArray[0].firstOrNull { 
        it > currentTimeHHMM.plusHHMM(transferTimeArray[1]) 
    } ?: 2700
    
    val timeArray = mutableListOf(firstDepartTime)
    
    // Arrive time of line 1.
    val rideTime1 = this.getRideTime(sharedPreferences, date, firstDepartTime, 0, rideTimeArray)
    val arriveTime1 = firstDepartTime.plusHHMM(rideTime1).overTime(firstDepartTime)
    timeArray.add(arriveTime1)
    
    // Depart time from depart point.
    val departPointTime = firstDepartTime.minusHHMM(transferTimeArray[1]).overTime(firstDepartTime)
    timeArray.add(0, departPointTime)
    
    if (changeLineInt > 0) {
        for (i in 1..changeLineInt) {
            // Depart time of line i.
            val lineDepartTime = timetableArray[i].firstOrNull { 
                it >= timeArray[2 * i].plusHHMM(transferTimeArray[i + 1]) 
            } ?: 2700
            timeArray.add(lineDepartTime)
            
            // Arrive time of line i.
            val rideTimeI = this.getRideTime(sharedPreferences, date, lineDepartTime, i, rideTimeArray)
            val arriveTimeI = lineDepartTime.plusHHMM(rideTimeI).overTime(lineDepartTime)
            timeArray.add(arriveTimeI)
        }
    }
    
    // Arrive time to destination (insert at index 0).
    val lastArrivalTime = timeArray.last()
    val destinationTime = lastArrivalTime.plusHHMM(transferTimeArray[0]).overTime(lastArrivalTime)
    timeArray.add(0, destinationTime)
    
    return timeArray
}

// Adjust hours for timetable display (0-3 AM -> 24-27).
fun String.adjustedForTimetable(): String {
    val components = this.split(":")
    if (components.size != 2) return this
    
    val hour = components[0].toIntOrNull() ?: return this
    val minute = components[1].toIntOrNull() ?: return this
    
    val adjustedHour = hour.timetableHour
    return "${adjustedHour.addZeroTime()}:${minute.addZeroTime()}"
}

// Extract minutes from time string (HH:MM or minutes-only).
val String.minutesOnly: String
    get() {
        // Check for HH:MM format.
        if (this.contains(":")) {
            val components = this.split(":")
            if (components.size == 2) {
                val minutes = components[1]
                val minutesInt = minutes.toIntOrNull()
                if (minutesInt != null && minutesInt < 10) {
                    return String.format(Locale.ROOT, "%02d", minutesInt)
                }
                return minutes
            }
        }
        
        // Handle minutes-only format (e.g., "5", "05", "24").
        val minutesInt = this.toIntOrNull()
        if (minutesInt != null) {
            if (minutesInt < 10) {
                return String.format(Locale.ROOT, "%02d", minutesInt)
            }
            return this
        }
        
        // Fallback: return original string.
        return this
    }

// Extract minutes as Int from time string.
val String.minutesOnlyInt: Int
    get() = this.minutesOnly.toIntOrNull() ?: 0

// Compare two time strings (single-digit or double-digit).
fun String.isTimeLessThan(other: String): Boolean {
    val time1 = this.toIntOrNull() ?: 0
    val time2 = other.toIntOrNull() ?: 0
    return time1 < time2
}

// Parse space-separated timetable strings.
val String.timetableComponents: List<String>
    get() = this.split(" ").filter { it.isNotEmpty() }

// Check if timetable contains time in single-digit or double-digit format.
fun String.containsTimeInAnyFormat(departureTime: Int): Boolean {
    val existingTimes = this.timetableComponents
    val singleDigitTime = departureTime.toString()
    val doubleDigitTime = departureTime.addZeroTime()
    return existingTimes.contains(singleDigitTime) || existingTimes.contains(doubleDigitTime)
}

// Generate copy-time options for hour selection.
@Composable
fun Int.choiceCopyTimeList(context: Context): List<String> {
    // Note: hour string resource is ":00-".
    val hourString = context.getString(R.string.hour)
    return listOf(
        "${this - 1}$hourString",
        "${this + 1}$hourString",
        context.getString(R.string.otherRouteOfLine1),
        context.getString(R.string.otherRouteOfLine2),
        context.getString(R.string.otherRouteOfLine3)
    )
}

// Get the other route identifier (back1 <-> back2, go1 <-> go2).
val String.otherRoute: String get() {
    val lastChar = this.lastOrNull() ?: return this
    return this.dropLast(1) + (if (lastChar == '1') "2" else "1")
}
