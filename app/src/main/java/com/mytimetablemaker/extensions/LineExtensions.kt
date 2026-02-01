package com.mytimetablemaker.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mytimetablemaker.models.*
import com.mytimetablemaker.ui.theme.*
import java.util.*
import java.text.Normalizer
import java.util.regex.Pattern
import androidx.core.content.edit

// MARK: - App Constants
// Core application constants
val goorbackArray = listOf("back1", "go1", "back2", "go2")

// Route direction constants
val goorbackOptions = listOf("back1", "back2", "go1", "go2")

// Route direction display names (using string resource keys)
val goorbackDisplayNamesRaw = mapOf(
    "back1" to "returnRoute1",
    "back2" to "returnRoute2",
    "go1" to "outboundRoute1",
    "go2" to "outboundRoute2"
)

// UserDefault Keys for home and office locations
const val homeKey = "departurepoint"
const val officeKey = "destination"

// MARK: - String Extensions for Route Data
// Extension for managing route-specific data and SharedPreferences operations

// Check if route is return direction
fun String.isBack(): Boolean = this == "back1" || this == "back2"

// Generate SharedPreferences keys
fun String.isShowRoute2Key(): String = "${this}route2flag"

fun String.changeLineKey(): String = "${this}changeline"

fun String.departurePointKey(): String = if (this.isBack()) "destination" else "departurepoint"

fun String.destinationKey(): String = if (this.isBack()) "departurepoint" else "destination"

fun String.departStationKey(num: Int): String = "${this}departstation${num + 1}"

fun String.arriveStationKey(num: Int): String = "${this}arrivestation${num + 1}"

fun String.departStationCodeKey(num: Int): String = "${this}departstationcode${num + 1}"

fun String.arriveStationCodeKey(num: Int): String = "${this}arrivestationcode${num + 1}"

fun String.operatorNameKey(num: Int): String = "${this}operatorname${num + 1}"

fun String.operatorCodeKey(num: Int): String = "${this}operatorcode${num + 1}"

fun String.lineNameKey(num: Int): String = "${this}linename${num + 1}"

fun String.lineSelectedKey(num: Int): String = "${this}lineSelected${num + 1}"

fun String.lineColorKey(num: Int): String = "${this}linecolor${num + 1}"

fun String.lineDirectionKey(num: Int): String = "${this}linedirection${num + 1}"

fun String.lineCodeKey(num: Int): String = "${this}linecode${num + 1}"

fun String.lineKindKey(num: Int): String = "${this}linekind${num + 1}"

fun String.rideTimeKey(num: Int): String = "${this}ridetime${num + 1}"

fun String.transportationKey(num: Int): String = if (num == 0) "${this}transporte" else "${this}transport${num}"

fun String.transferTimeKey(num: Int): String = if (num == 0) "${this}transfertimee" else "${this}transfertime${num}"

// TODO: Implement ODPTCalendarType enum and calendarTag() extension
fun String.timetableKey(calendarType: String, num: Int, hour: Int): String {
    val calendarTag = when (calendarType) {
        "weekday" -> "weekday"
        "holiday" -> "holiday"
        "saturdayHoliday" -> "weekend"
        else -> "weekday"
    }
    return "${this.lineNameKey(num)}$calendarTag${hour.addZeroTime()}"
}

fun String.timetableRideTimeKey(calendarType: String, num: Int, hour: Int): String {
    val calendarTag = when (calendarType) {
        "weekday" -> "weekday"
        "holiday" -> "holiday"
        "saturdayHoliday" -> "weekend"
        else -> "weekday"
    }
    return "${this.lineNameKey(num)}$calendarTag${hour.addZeroTime()}ridetime"
}

fun String.timetableTrainTypeKey(calendarType: String, num: Int, hour: Int): String {
    val calendarTag = when (calendarType) {
        "weekday" -> "weekday"
        "holiday" -> "holiday"
        "saturdayHoliday" -> "weekend"
        else -> "weekday"
    }
    return "${this.lineNameKey(num)}$calendarTag${hour.addZeroTime()}traintype"
}

fun String.trainTypeListKey(calendarType: String, num: Int): String {
    val calendarTag = when (calendarType) {
        "weekday" -> "weekday"
        "holiday" -> "holiday"
        "saturdayHoliday" -> "weekend"
        else -> "weekday"
    }
    return "${this.lineNameKey(num)}$calendarTag" + "traintypelist"
}

fun String.operatorLineListKey(num: Int): String = "${this}operatorlinelist${num + 1}"

fun String.lineStopListKey(num: Int): String = "${this}linestoplist${num + 1}"

// MARK: - SharedPreferences Helper Extensions
fun String.userDefaultsValue(sharedPreferences: SharedPreferences, defaultValue: String): String? {
    return if (sharedPreferences.contains(this)) {
        sharedPreferences.getString(this, defaultValue)
    } else {
        defaultValue
    }
}

fun String.userDefaultsInt(sharedPreferences: SharedPreferences, defaultValue: Int): Int {
    if (!sharedPreferences.contains(this)) return defaultValue
    return try {
        sharedPreferences.getInt(this, defaultValue)
    } catch (e: ClassCastException) {
        // Value was stored as String (e.g. from Firestore getLineInfoFirestore)
        sharedPreferences.getString(this, null)?.toIntOrNull() ?: defaultValue
    }
}

fun String.userDefaultsBool(sharedPreferences: SharedPreferences, defaultValue: Boolean): Boolean {
    if (!sharedPreferences.contains(this)) return defaultValue
    return try {
        sharedPreferences.getBoolean(this, defaultValue)
    } catch (e: ClassCastException) {
        sharedPreferences.getString(this, null)?.toBooleanStrictOrNull() ?: defaultValue
    }
}

fun String.userDefaultsColor(sharedPreferences: SharedPreferences, defaultValue: String): Color {
    val colorString = this.userDefaultsValue(sharedPreferences, defaultValue) ?: defaultValue
    return colorString.safeColor
}

// MARK: - Route Data Access Extensions
fun String.isShowRoute2(sharedPreferences: SharedPreferences): Boolean {
    return this.isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
}

fun String.changeLineInt(sharedPreferences: SharedPreferences): Int {
    return this.changeLineKey().userDefaultsInt(sharedPreferences, 0)
}

fun String.departurePoint(sharedPreferences: SharedPreferences, context: Context? = null): String {
    val default = if (context != null) {
        if (this.isBack()) {
            context.getString(com.mytimetablemaker.R.string.office)
        } else {
            context.getString(com.mytimetablemaker.R.string.home)
        }
    } else {
        if (this.isBack()) "Office" else "Home"
    }
    return this.departurePointKey().userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.destination(sharedPreferences: SharedPreferences, context: Context? = null): String {
    val default = if (context != null) {
        if (this.isBack()) {
            context.getString(com.mytimetablemaker.R.string.home)
        } else {
            context.getString(com.mytimetablemaker.R.string.office)
        }
    } else {
        if (this.isBack()) "Home" else "Office"
    }
    return this.destinationKey().userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.departStation(sharedPreferences: SharedPreferences, num: Int, context: Context? = null): String {
    val default = context?.let { num.departStationDefault(it) } ?: "Dep. St. ${num + 1}"
    return this.departStationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.arriveStation(sharedPreferences: SharedPreferences, num: Int, context: Context? = null): String {
    val default = context?.let { num.arriveStationDefault(it) } ?: "Arr. St. ${num + 1}"
    return this.arriveStationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.departStationCode(sharedPreferences: SharedPreferences, num: Int): String {
    return this.departStationCodeKey(num).userDefaultsValue(sharedPreferences, "") ?: ""
}

fun String.arriveStationCode(sharedPreferences: SharedPreferences, num: Int): String {
    return this.arriveStationCodeKey(num).userDefaultsValue(sharedPreferences, "") ?: ""
}

fun String.operatorName(sharedPreferences: SharedPreferences, num: Int): String {
    return this.operatorNameKey(num).userDefaultsValue(sharedPreferences, "") ?: ""
}

fun String.operatorCode(sharedPreferences: SharedPreferences, num: Int): String {
    return this.operatorCodeKey(num).userDefaultsValue(sharedPreferences, "") ?: ""
}

fun String.lineSelected(sharedPreferences: SharedPreferences, num: Int): Boolean {
    return this.lineSelectedKey(num).userDefaultsBool(sharedPreferences, false)
}

fun String.lineDirection(sharedPreferences: SharedPreferences, num: Int): String {
    return this.lineDirectionKey(num).userDefaultsValue(sharedPreferences, "") ?: ""
}

fun String.lineName(sharedPreferences: SharedPreferences, num: Int, context: Context? = null): String {
    val default = context?.let { num.lineNameDefault(it) } ?: "Line ${num + 1}"
    return this.lineNameKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.lineColor(sharedPreferences: SharedPreferences, num: Int): Color {
    val accentString = "#03DAC5"
    val key = this.lineColorKey(num)
    val colorString = key.userDefaultsValue(sharedPreferences, accentString) ?: accentString
    val color = colorString.safeColor
    android.util.Log.d("LineExtensions", "🎨 Loading line color: goorback=$this, num=$num, key=$key, colorString=$colorString, color=$color")
    return color
}

fun String.lineCode(sharedPreferences: SharedPreferences, num: Int): String {
    return this.lineCodeKey(num).userDefaultsValue(sharedPreferences, "") ?: ""
}

// Get line kind (railway or bus) for a specific line number
fun String.lineKind(sharedPreferences: SharedPreferences, num: Int): TransportationLineKind {
    val kindString = this.lineKindKey(num).userDefaultsValue(sharedPreferences, "Railway") ?: "Railway"
    return when (kindString.uppercase()) {
        "RAILWAY", "RAIL" -> TransportationLineKind.RAILWAY
        "BUS" -> TransportationLineKind.BUS
        else -> TransportationLineKind.RAILWAY
    }
}

fun String.lineColorString(sharedPreferences: SharedPreferences, num: Int): String {
    val accentString = "#03DAC5"
    return this.lineColorKey(num).userDefaultsValue(sharedPreferences, accentString) ?: accentString
}

fun String.rideTime(sharedPreferences: SharedPreferences, num: Int): Int {
    return this.rideTimeKey(num).userDefaultsInt(sharedPreferences, 0)
}

// TODO: Implement TransferType enum
fun String.transportation(sharedPreferences: SharedPreferences, num: Int): String {
    val default = "Walking" // TransferType.walking.rawValue
    return this.transportationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.transferTime(sharedPreferences: SharedPreferences, num: Int): Int {
    return this.transferTimeKey(num).userDefaultsInt(sharedPreferences, 0)
}

fun String.timetableTime(sharedPreferences: SharedPreferences, calendarType: String, num: Int, hour: Int): String {
    return this.timetableKey(calendarType, num, hour).userDefaultsValue(sharedPreferences, "") ?: ""
}

fun String.timetableRideTime(sharedPreferences: SharedPreferences, calendarType: String, num: Int, hour: Int): String {
    return this.timetableRideTimeKey(calendarType, num, hour).userDefaultsValue(sharedPreferences, "") ?: ""
}

// MARK: - Array Generation Extensions
fun String.departStationArray(sharedPreferences: SharedPreferences, context: Context? = null): List<String> {
    return (0..2).map { this.departStation(sharedPreferences, it, context) }
}

fun String.arriveStationArray(sharedPreferences: SharedPreferences, context: Context? = null): List<String> {
    return (0..2).map { this.arriveStation(sharedPreferences, it, context) }
}

fun String.stationArray(sharedPreferences: SharedPreferences, context: Context? = null): List<String> {
    return (0..2).flatMap { listOf(
        this.departStation(sharedPreferences, it, context),
        this.arriveStation(sharedPreferences, it, context)
    ) }
}

fun String.operatorNameArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).map { this.operatorName(sharedPreferences, it) }
}

fun String.lineNameArray(sharedPreferences: SharedPreferences, context: Context? = null): List<String> {
    return (0..2).map { this.lineName(sharedPreferences, it, context) }
}

fun String.lineColorArray(sharedPreferences: SharedPreferences): List<Color> {
    return (0..2).map { this.lineColor(sharedPreferences, it) }
}

fun String.lineCodeArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).map { this.lineCode(sharedPreferences, it) }
}

fun String.lineKindArray(sharedPreferences: SharedPreferences): List<TransportationLineKind> {
    return (0..2).map { this.lineKind(sharedPreferences, it) }
}

fun String.lineColorStringArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).map { this.lineColorString(sharedPreferences, it) }
}

fun String.rideTimeArray(sharedPreferences: SharedPreferences): List<Int> {
    return (0..2).map { this.rideTime(sharedPreferences, it) }
}

fun String.transportationArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..3).map { this.transportation(sharedPreferences, it) }
}

fun String.transferTimeArray(sharedPreferences: SharedPreferences): List<Int> {
    return (0..3).map { this.transferTime(sharedPreferences, it) }
}

// MARK: - Settings View Data Access
fun String.settingsDeparturePoint(sharedPreferences: SharedPreferences, context: Context): String {
    val default = context.getString(com.mytimetablemaker.R.string.notSet)
    return this.departurePointKey().userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsDestination(sharedPreferences: SharedPreferences, context: Context): String {
    val default = context.getString(com.mytimetablemaker.R.string.notSet)
    return this.destinationKey().userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsDepartStation(sharedPreferences: SharedPreferences, num: Int, context: Context): String {
    val default = context.getString(com.mytimetablemaker.R.string.notSet)
    return this.departStationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsArriveStation(sharedPreferences: SharedPreferences, num: Int, context: Context): String {
    val default = context.getString(com.mytimetablemaker.R.string.notSet)
    return this.arriveStationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsLineName(sharedPreferences: SharedPreferences, num: Int, context: Context): String {
    val default = context.getString(com.mytimetablemaker.R.string.notSet)
    return this.lineNameKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsLineColorString(sharedPreferences: SharedPreferences, num: Int): String {
    val grayString = "#9C9C9C"
    return this.lineColorKey(num).userDefaultsValue(sharedPreferences, grayString) ?: grayString
}

fun String.settingsRideTime(sharedPreferences: SharedPreferences, num: Int, context: Context): String {
    val rideTime = this.rideTime(sharedPreferences, num)
    val notSet = context.getString(com.mytimetablemaker.R.string.notSet)
    val minText = context.getString(com.mytimetablemaker.R.string.minBrackets)
    return if (rideTime == 0) {
        notSet
    } else {
        "$rideTime$minText"
    }
}

fun String.settingsRideTimeColor(sharedPreferences: SharedPreferences, num: Int): Color {
    val rideTime = this.rideTime(sharedPreferences, num)
    return if (rideTime == 0) {
        Gray
    } else {
        this.lineColorArray(sharedPreferences)[num]
    }
}

fun String.settingsTransportation(sharedPreferences: SharedPreferences, num: Int, context: Context): String {
    val default = context.getString(com.mytimetablemaker.R.string.notSet)
    return this.transportationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsTransferTime(sharedPreferences: SharedPreferences, num: Int, context: Context): String {
    val transferTime = this.transferTime(sharedPreferences, num)
    val notSet = context.getString(com.mytimetablemaker.R.string.notSet)
    val minText = context.getString(com.mytimetablemaker.R.string.minBrackets)
    return if (transferTime == 0) {
        notSet
    } else {
        "$transferTime$minText"
    }
}

// MARK: - Boolean Extensions for Route Direction
fun Boolean.goOrBack1(): String = if (this) "back1" else "go1"

fun Boolean.goOrBack2(): String = if (this) "back2" else "go2"

// MARK: - Int Extensions for Default Names
fun Int.departStationDefault(context: Context): String {
    val prefix = context.getString(com.mytimetablemaker.R.string.depSt)
    return "$prefix${this + 1}"
}

fun Int.arriveStationDefault(context: Context): String {
    val prefix = context.getString(com.mytimetablemaker.R.string.arrSt)
    return "$prefix${this + 1}"
}

fun Int.lineNameDefault(context: Context): String {
    val prefix = context.getString(com.mytimetablemaker.R.string.line)
    return "$prefix${this + 1}"
}

// MARK: - String Localization Extensions
// REMOVED: String.localized() function has been removed.
// Use context.getString(R.string.xxx) directly instead of String.localized(context).

// Normalize string for search (convert to lowercase, handle fullwidth/halfwidth)
fun String.normalizedForSearch(): String {
    var s = this.trim()
    // Convert fullwidth to halfwidth
    s = Normalizer.normalize(s, Normalizer.Form.NFKC)
    return s.lowercase(Locale.getDefault())
}

// Extract the last component from ODPT identifiers
// Example: odpt:Operator:JR-East → JR-East
fun String.odptTail(): String {
    val components = this.split(":")
    return components.lastOrNull() ?: this
}

// MARK: - Bus English Name Extraction
// Extract English names from ODPT bus identifiers (only for English locale)

// Extract English name from bus route identifier
// Example: "odpt.Busroute:Toei.Mon33" → "Mon33"
fun String.busRouteEnglishName(): String? {
    val currentLanguage = Locale.getDefault().language
    if (currentLanguage == "ja") return null
    
    val parts = this.split(".")
    if (parts.size >= 3 && parts[0] == "odpt" && parts[1].startsWith("Busroute:")) {
        val routeCode = parts[2]
        val englishPattern = Pattern.compile("[A-Za-z0-9]")
        if (englishPattern.matcher(routeCode).find()) {
            return routeCode
        }
    }
    return null
}

// Extract English name from bus stop pole identifier
// Example: "odpt.BusstopPole:Toei.KameidoStation.369.7" → "KameidoStation"
fun String.busStopEnglishName(): String? {
    val currentLanguage = Locale.getDefault().language
    if (currentLanguage == "ja") return null
    
    val components = this.split(".")
    if (components.size >= 3) {
        return components[2]
    }
    return null
}

// MARK: - Character Extensions
// Check if the character is a Japanese character (Hiragana, Katakana, or Kanji)
fun Char.isJapanese(): Boolean {
    val codePoint = this.code
    return (codePoint in 0x3040..0x309F) || // Hiragana
           (codePoint in 0x30A0..0x30FF) || // Katakana
           (codePoint in 0x4E00..0x9FAF)    // Kanji
}

// Check if string contains Japanese characters
fun String.containsJapanese(): Boolean {
    return this.any { it.isJapanese() }
}

// MARK: - Helper Functions
// Get display name (split by ":" and return first component for ODPT format)
fun String.getDisplayName(): String {
    val components = this.split(":")
    return components.firstOrNull()?.trim() ?: this
}

// MARK: - Timetable Array Generation
// Note: otherRoute() extension is now in TimeExtensions.kt
// Note: addZeroTime() extension is now in TimeExtensions.kt
// Generate timetable arrays for all lines (0-2)
// Each line uses its own target calendar type based on date and available calendar types
fun String.timetableArray(
    sharedPreferences: SharedPreferences,
    date: Date
): List<List<Int>> {
    return (0..2).map { num ->
        // Get target calendar type based on date and available calendar types for this line
        val availableTypes = this.loadAvailableCalendarTypes(sharedPreferences, num)
        val targetCalendarType = date.odptCalendarType(availableTypes)
        
        (4..25).flatMap { hour ->
            timetableTime(sharedPreferences, targetCalendarType, num, hour)
                .timeString
                .split(" ")
                .mapNotNull { it.toIntOrNull() }
                .map { it + hour * 100 }
                .filter { it in 0..<2700 }
        }.sorted()
    }
}

// MARK: - Get Ride Time
// Get ride time for a specific departure time
// Uses timetableRideTime if available, otherwise falls back to input rideTime
// Determines calendar type based on date and available calendar types for the line
fun String.getRideTime(
    sharedPreferences: SharedPreferences,
    date: Date,
    departTime: Int,
    num: Int
): Int {
    // Get target calendar type based on date and available calendar types
    val availableTypes = this.loadAvailableCalendarTypes(sharedPreferences, num)
    val calendarTypeString = date.odptCalendarType(availableTypes)
    
    val hour = departTime / 100
    val minutesInHour = departTime % 100
    
    // Try to get timetableRideTime for this hour
    val rideTimeKey = this.timetableRideTimeKey(calendarTypeString, num, hour)
    val rideTimeString = sharedPreferences.getString(rideTimeKey, null)
    
    if (!rideTimeString.isNullOrEmpty()) {
        val rideTimes = rideTimeString.split(" ").mapNotNull { it.toIntOrNull() }
        
        // Find corresponding ride time by matching departure time
        val timetableKey = this.timetableKey(calendarTypeString, num, hour)
        val timetableString = sharedPreferences.getString(timetableKey, null)
        
        if (!timetableString.isNullOrEmpty()) {
            val departureTimes = timetableString.split(" ").mapNotNull { it.toIntOrNull() }
            val index = departureTimes.indexOf(minutesInHour)
            
            if (index >= 0 && index < rideTimes.size) {
                return rideTimes[index]
            }
            
            // If exact match not found, use first available ride time
            if (rideTimes.isNotEmpty()) {
                return rideTimes.first()
            }
        }
    }
    
    // Fallback to default ride time for this line
    return this.rideTimeArray(sharedPreferences)[num]
}

// MARK: - Valid Hour Range
// Note: timeArray() and validHourRange() extensions are now in TimeExtensions.kt

// MARK: - Bus Stop Title Generation
// Generate LocalizedTitle for bus stops from note and busstopPole
fun generateBusStopTitle(note: String, busstopPole: String): LocalizedTitle? {
    val japaneseName: String? = if (note.isEmpty()) null else note.trim()
    val englishName: String?
    
    if (busstopPole.isNotEmpty()) {
        val components = busstopPole.split(".")
        englishName = if (components.size > 2) {
            components[2].trim()
        } else {
            null
        }
    } else {
        englishName = null
    }
    
    return if (japaneseName != null || englishName != null) {
        LocalizedTitle(ja = japaneseName, en = englishName)
    } else {
        null
    }
}

// MARK: - Localization Helper
// Select localized name based on language code
fun String.selectLocalizedName(ja: String?, en: String?): String {
    return if (this == "ja") {
        ja ?: en ?: ""
    } else {
        en ?: ja ?: ""
    }
}

// MARK: - ODPT Dictionary Extensions
// Extract ODPT-specific data from dictionary structures

// Extract destination station from ODPT data
// Handles both String and List<String> formats
fun Map<String, Any>.odptDestinationStation(): String? {
    return when (val dest = this["odpt:destinationStation"]) {
        is String -> dest
        is List<*> -> dest.firstOrNull() as? String
        else -> null
    }
}

// Extract line color from ODPT data
// Supports both odpt:color (local) and odpt:lineColor (API) fields
fun Map<String, Any>.odptLineColor(): String? {
    return (this["odpt:lineColor"] as? String) ?: (this["odpt:color"] as? String)
}

// Extract LocalizedTitle from ODPT railway title dictionary
@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.odptRailwayTitle(): LocalizedTitle? {
    val railwayTitleDict = this["odpt:railwayTitle"] as? Map<String, String> ?: return null
    return LocalizedTitle(
        ja = railwayTitleDict["ja"],
        en = railwayTitleDict["en"]
    )
}

// MARK: - Time String Processing
// Note: timeString() extension is now in TimeExtensions.kt

// MARK: - Station Timetable Data Structure
// Data class for station timetable data (equivalent to Swift tuple)
data class StationTimetableData(
    val trainNumber: String,
    val departureTime: String,
    val destinationStation: String,
    val trainType: String
)

// MARK: - Station Timetable Data Extensions
// Extensions for processing arrays of station timetable data
fun List<StationTimetableData>.trainTypeList(): List<String> {
    return this.mapNotNull { record ->
        val trainType = record.trainType
        if (trainType.isEmpty()) return@mapNotNull null
        
        // Extract the actual train type name from ODPT format
        // Example: "odpt.TrainType:JR-East.Local" -> "Local"
        if (trainType.contains(".")) {
            val components = trainType.split(".")
            components.lastOrNull() ?: trainType
        } else {
            trainType
        }
    }.distinct().sorted()
}

fun List<StationTimetableData>.filteredBy(trainType: String): List<StationTimetableData> {
    return this.filter { record ->
        val recordTrainType = record.trainType
        if (recordTrainType.isEmpty()) return@filter false
        
        // Extract the actual train type name
        val actualTrainType = if (recordTrainType.contains(".")) {
            val components = recordTrainType.split(".")
            components.lastOrNull() ?: recordTrainType
        } else {
            recordTrainType
        }
        
        actualTrainType == trainType
    }
}

// MARK: - TransportationTime Array Extensions
// Extensions for processing arrays of TransportationTime
fun List<TransportationTime>.mergeAndSortTransportationTimes(): List<TransportationTime> {
    // Use Set to remove exact duplicates (based on departureTime and arrivalTime)
    val seenTimes = mutableSetOf<String>()
    val uniqueTimes = mutableListOf<TransportationTime>()
    
    for (time in this) {
        val timeKey = "${time.departureTime}-${time.arrivalTime}"
        if (!seenTimes.contains(timeKey)) {
            seenTimes.add(timeKey)
            uniqueTimes.add(time)
        }
    }
    
    // Sort by departure time
    return uniqueTimes.sortedWith(compareBy<TransportationTime> { time ->
        time.departureTime.timeToMinutes
    }.thenBy { time ->
        time.arrivalTime.timeToMinutes
    })
}

// MARK: - TransportationTime Saving Methods
// Save TransportationTime objects for a specific hour
fun String.saveTransportationTimes(
    transportationTimes: List<TransportationTime>,
    calendarType: ODPTCalendarType,
    num: Int,
    hour: Int,
    sharedPreferences: SharedPreferences
) {
    val calendarTag = calendarType.calendarTag()
    val timetableKey = this.timetableKey(calendarTag, num, hour)
    val timetableRideTimeKey = this.timetableRideTimeKey(calendarTag, num, hour)
    val timetableTrainTypeKey = this.timetableTrainTypeKey(calendarTag, num, hour)
    
    // Clear existing data (always remove to ensure clean state)
    sharedPreferences.edit {
        remove(timetableKey)
            .remove(timetableRideTimeKey)
            .remove(timetableTrainTypeKey)
    }
    
    if (transportationTimes.isEmpty()) {
        android.util.Log.w("LineExtensions", "No TransportationTime objects to save for hour $hour")
        return
    }
    
    // Prepare data arrays
    val departureTimes = mutableListOf<String>()
    val rideTimes = mutableListOf<String>()
    val trainTypes = mutableListOf<String>()
    
    for (transportationTime in transportationTimes) {
        // Convert HH:MM format to minutes format for consistency with manual editing
        val departureTimeInMinutes = convertHHMMToMinutes(transportationTime.departureTime)
        departureTimes.add(departureTimeInMinutes.toString())
        rideTimes.add(transportationTime.rideTime.toString())
        
        // Use trainType if available (only for TrainTime), otherwise empty string
        val trainType = if (transportationTime is TrainTime) {
            transportationTime.trainType ?: ""
        } else {
            ""
        }
        trainTypes.add(trainType)
    }
    
    // Save to SharedPreferences
    val timetableString = departureTimes.joinToString(" ")
    val timetableRideTimeString = rideTimes.joinToString(" ")
    val timetableTrainTypeString = trainTypes.joinToString(" ")
    
    sharedPreferences.edit {
        putString(timetableKey, timetableString)
            .putString(timetableRideTimeKey, timetableRideTimeString)
            .putString(timetableTrainTypeKey, timetableTrainTypeString)
    }
}

// MARK: - Time Format Conversion
// Convert HH:MM format to minutes within the hour
private fun convertHHMMToMinutes(timeString: String): Int {
    val components = timeString.split(":")
    if (components.size == 2) {
        val minute = components[1].toIntOrNull()
        if (minute != null) {
            return minute  // Return only minutes within the hour
        }
    }
    return 0
}

// MARK: - Save Train Type List
// Save unique train types list for the entire timetable
fun String.saveTrainTypeList(
    transportationTimes: List<TransportationTime>,
    calendarType: ODPTCalendarType,
    num: Int,
    sharedPreferences: SharedPreferences
) {
    val calendarTag = calendarType.calendarTag()
    val trainTypeListKey = this.trainTypeListKey(calendarTag, num)
    
    // Extract all train types from all transportation times
    val allTrainTypes = transportationTimes.mapNotNull { transportationTime ->
        if (transportationTime is TrainTime) {
            transportationTime.trainType
        } else {
            null // Bus doesn't have trainType
        }
    }.mapNotNull { trainType ->
        if (trainType.isEmpty()) {
            null
        } else {
            val components = trainType.split(".")
            components.lastOrNull() ?: trainType
        }
    }
    
    // Remove duplicates and sort
    val uniqueTrainTypes = allTrainTypes.distinct().sorted()
    val trainTypeListString = uniqueTrainTypes.joinToString(" ")
    
    sharedPreferences.edit {
        putString(trainTypeListKey, trainTypeListString)
    }
    
    android.util.Log.d("LineExtensions", "Train type list saved: $uniqueTrainTypes")
}

// MARK: - TransportationLineKind Extensions
// Extension for String to convert to TransportationLineKind
fun String.fromString(): TransportationLineKind? {
    return when (this.uppercase()) {
        "RAILWAY", "RAIL" -> TransportationLineKind.RAILWAY
        "BUS" -> TransportationLineKind.BUS
        else -> null
    }
}

// MARK: - TransportationLine Extensions
// Extension for TransportationLine to extract English name from bus route
fun TransportationLine.busRouteEnglishName(): String? {
    val busRoute = this.busRoute ?: return null
    return busRoute.busRouteEnglishName()
}

// MARK: - Timetable Content Extensions
// Extensions for timetable content view functionality

// Load transportation times for a specific calendar type, line number, and hour
fun String.loadTransportationTimes(
    calendarType: ODPTCalendarType,
    num: Int,
    hour: Int,
    sharedPreferences: SharedPreferences
): List<TransportationTime> {
    val calendarTag = calendarType.calendarTag()
    val timetableKey = this.timetableKey(calendarTag, num, hour)
    val timetableRideTimeKey = this.timetableRideTimeKey(calendarTag, num, hour)
    val timetableTrainTypeKey = this.timetableTrainTypeKey(calendarTag, num, hour)
    
    val timetableString = sharedPreferences.getString(timetableKey, null) ?: return emptyList()
    
    val departureTimes = timetableString.split(" ").filter { it.isNotEmpty() }
    val rideTimeString = this.timetableRideTime(sharedPreferences, calendarTag, num, hour)
    val rideTimes = if (rideTimeString.isNotEmpty()) {
        rideTimeString.split(" ").mapNotNull { it.toIntOrNull() }
    } else {
        emptyList()
    }
    val trainTypes = sharedPreferences.getString(timetableTrainTypeKey, null)
        ?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()
    
    val routeRideTimeKey = this.rideTimeKey(num)
    val defaultRideTime = routeRideTimeKey.userDefaultsInt(sharedPreferences, 0)
    
    val transportationTimes = mutableListOf<TransportationTime>()
    for ((index, departureTimeString) in departureTimes.withIndex()) {
        val rideTime = if (index < rideTimes.size) rideTimes[index] else defaultRideTime
        val trainType = if (index < trainTypes.size && trainTypes[index].isNotEmpty()) trainTypes[index] else null
        
        if (trainType == null) {
            // Bus data (no trainType)
            val busTime = BusTime(
                departureTime = departureTimeString,
                arrivalTime = "",
                busNumber = null,
                routePattern = null,
                rideTime = rideTime
            )
            transportationTimes.add(busTime)
        } else {
            // Train data (has trainType)
            val trainTime = TrainTime(
                departureTime = departureTimeString,
                arrivalTime = "",
                trainNumber = null,
                trainType = trainType,
                rideTime = rideTime
            )
            transportationTimes.add(trainTime)
        }
    }
    return transportationTimes
}

// Calculate valid hour range for timetable data
fun String.validHourRange(
    calendarType: ODPTCalendarType,
    num: Int,
    sharedPreferences: SharedPreferences
): List<Int> {
    val allHours = (4..25).toList()
    val calendarTag = calendarType.calendarTag()
    
    // Find hours with transportation times
    val hoursWithData = allHours.filter { hour ->
        val times = this.loadTransportationTimes(calendarType, num, hour, sharedPreferences)
        times.isNotEmpty()
    }
    
    // Return range from first to last hour with data
    if (hoursWithData.isEmpty()) {
        return emptyList()
    }
    
    val firstHour = hoursWithData.minOrNull() ?: return emptyList()
    val lastHour = hoursWithData.maxOrNull() ?: return emptyList()
    
    return (firstHour..lastHour).toList()
}

// Check if timetable data exists for the specified calendar type and line
fun String.hasTimetableDataForType(
    calendarType: ODPTCalendarType,
    num: Int,
    sharedPreferences: SharedPreferences
): Boolean {
    val calendarTag = calendarType.calendarTag()
    android.util.Log.d("LineExtensions", "hasTimetableDataForType: goorback=$this, calendarType=${calendarType.rawValue}, calendarTag=$calendarTag, num=$num")
    
    // Check all hours (4-25) to see if data exists
    var foundKeys = mutableListOf<String>()
    for (hour in 4..25) {
        val key = this.timetableKey(calendarTag, num, hour)
        if (sharedPreferences.contains(key)) {
            foundKeys.add(key)
            android.util.Log.d("LineExtensions", "Found timetable key: $key")
        }
    }
    
    if (foundKeys.isEmpty()) {
        // Log first few expected keys for debugging
        android.util.Log.d("LineExtensions", "No timetable data found. Expected keys (first 3): ${(4..6).map { this.timetableKey(calendarTag, num, it) }}")
    }
    
    return foundKeys.isNotEmpty()
}

// Get train times count for each hour in the valid range
fun String.getTrainTimesCounts(
    calendarType: ODPTCalendarType,
    num: Int,
    sharedPreferences: SharedPreferences
): List<Int> {
    val hours = this.validHourRange(calendarType, num, sharedPreferences)
    val counts = mutableListOf<Int>()
    for (hour in hours) {
        val transportationTimes = this.loadTransportationTimes(calendarType, num, hour, sharedPreferences)
        counts.add(transportationTimes.size)
    }
    return counts
}

// Load train type list for a specific calendar type and line
fun String.loadTrainTypeList(
    calendarType: ODPTCalendarType,
    num: Int,
    sharedPreferences: SharedPreferences
): List<String> {
    val calendarTag = calendarType.calendarTag()
    val trainTypeListKey = this.trainTypeListKey(calendarTag, num)
    
    // First, try to load from trainTypeListKey
    val trainTypeListString = sharedPreferences.getString(trainTypeListKey, null)
    if (!trainTypeListString.isNullOrEmpty()) {
        val trainTypes = trainTypeListString.split(" ").filter { it.isNotEmpty() }.distinct()
        // Sort by color priority
        return trainTypes.sortedWith(compareBy<String> { trainType ->
            colorForTrainType(trainType).priorityValue
        }.thenBy { it })
    }
    
    // If trainTypeListKey doesn't exist, collect from individual hour data
    val validHours = this.validHourRange(calendarType, num, sharedPreferences)
    val allTransportationTimes = mutableListOf<TransportationTime>()
    for (hour in validHours) {
        val times = this.loadTransportationTimes(calendarType, num, hour, sharedPreferences)
        allTransportationTimes.addAll(times)
    }
    
    // Extract train types from transportation times
    val allTrainTypes = allTransportationTimes.mapNotNull { transportationTime ->
        if (transportationTime is TrainTime) {
            transportationTime.trainType
        } else {
            null // Bus doesn't have trainType
        }
    }.mapNotNull { trainType ->
        if (trainType.isEmpty()) {
            null
        } else {
            val components = trainType.split(".")
            components.lastOrNull() ?: trainType
        }
    }
    
    // Remove duplicates and sort
    val uniqueTrainTypes = allTrainTypes.distinct().sortedWith(compareBy<String> { trainType ->
        colorForTrainType(trainType).priorityValue
    }.thenBy { it })
    
    // Save the collected train types to trainTypeListKey for future use
    if (uniqueTrainTypes.isNotEmpty()) {
        val trainTypeListString = uniqueTrainTypes.joinToString(" ")
        sharedPreferences.edit {
            putString(trainTypeListKey, trainTypeListString)
        }
    }
    
    android.util.Log.d("LineExtensions", "Train types loaded: ${uniqueTrainTypes.size} types")
    return uniqueTrainTypes
}

// MARK: - Choice Copy Time Key Array
// Generate array of keys for copying timetable from other hours or routes
fun String.choiceCopyTimeKeyArray(
    calendarType: ODPTCalendarType,
    num: Int,
    hour: Int
): List<String> {
    val calendarTag = calendarType.calendarTag()
    val oppositeCalendarTag = if (calendarTag == "weekday") "holiday" else "weekday"
    // Use otherroute extension function
    val otherroute = this.otherRoute
    
    return listOf(
        "${this.lineNameKey(num)}$calendarTag${(hour - 1).addZeroTime()}",
        "${this.lineNameKey(num)}$calendarTag${(hour + 1).addZeroTime()}",
        "${this.lineNameKey(num)}$oppositeCalendarTag${hour.addZeroTime()}",
        "${otherroute.lineNameKey(0)}$calendarTag${hour.addZeroTime()}",
        "${otherroute.lineNameKey(1)}$calendarTag${hour.addZeroTime()}",
        "${otherroute.lineNameKey(2)}$calendarTag${hour.addZeroTime()}"
    )
}

// MARK: - Choice Copy Time
// Get timetable string from another hour or route for copying
fun String.choiceCopyTime(
    calendarType: ODPTCalendarType,
    num: Int,
    hour: Int,
    index: Int,
    sharedPreferences: SharedPreferences
): String {
    val keyArray = this.choiceCopyTimeKeyArray(calendarType, num, hour)
    if (index < 0 || index >= keyArray.size) {
        return ""
    }
    return sharedPreferences.getString(keyArray[index], null) ?: ""
}

// MARK: - Operator Line List Persistence
// Save operator line list to SharedPreferences after operator selection
fun String.saveOperatorLineList(
    lines: List<TransportationLine>,
    num: Int,
    sharedPreferences: SharedPreferences
) {
    val operatorLineListKey = this.operatorLineListKey(num)
    val gson = Gson()
    val json = gson.toJson(lines)
    sharedPreferences.edit {
        putString(operatorLineListKey, json)
    }
}

// Load operator line list from SharedPreferences
fun String.loadOperatorLineList(
    num: Int,
    sharedPreferences: SharedPreferences
): List<TransportationLine>? {
    val operatorLineListKey = this.operatorLineListKey(num)
    val json = sharedPreferences.getString(operatorLineListKey, null) ?: return null
    
    return try {
        val gson = Gson()
        val type = object : TypeToken<List<TransportationLine>>() {}.type
        gson.fromJson<List<TransportationLine>>(json, type)
    } catch (e: Exception) {
        null
    }
}

// MARK: - Line Stop List Persistence
// Save line stop list to SharedPreferences after line selection
fun String.saveLineStopList(
    stops: List<TransportationStop>,
    num: Int,
    sharedPreferences: SharedPreferences
) {
    val lineStopListKey = this.lineStopListKey(num)
    val gson = Gson()
    val json = gson.toJson(stops)
    sharedPreferences.edit {
        putString(lineStopListKey, json)
    }
}

// Load line stop list from SharedPreferences
fun String.loadLineStopList(
    num: Int,
    sharedPreferences: SharedPreferences
): List<TransportationStop>? {
    val lineStopListKey = this.lineStopListKey(num)
    val json = sharedPreferences.getString(lineStopListKey, null) ?: return null
    
    return try {
        val gson = Gson()
        val type = object : TypeToken<List<TransportationStop>>() {}.type
        gson.fromJson<List<TransportationStop>>(json, type)
    } catch (e: Exception) {
        null
    }
}

