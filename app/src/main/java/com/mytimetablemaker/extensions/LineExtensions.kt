package com.mytimetablemaker.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.mytimetablemaker.models.*
import com.mytimetablemaker.ui.theme.*
import java.util.*
import java.text.Normalizer
import java.util.regex.Pattern

// MARK: - App Constants
// Core application constants
val goorbackArray = listOf("back1", "go1", "back2", "go2")

// Route direction constants
val goorbackOptions = listOf("back1", "back2", "go1", "go2")

// Route direction display names
val goorbackDisplayNamesRaw = mapOf(
    "back1" to "Return Route 1",
    "back2" to "Return Route 2",
    "go1" to "Outbound Route 1",
    "go2" to "Outbound Route 2"
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
    return if (sharedPreferences.contains(this)) {
        sharedPreferences.getInt(this, defaultValue)
    } else {
        defaultValue
    }
}

fun String.userDefaultsBool(sharedPreferences: SharedPreferences, defaultValue: Boolean): Boolean {
    return if (sharedPreferences.contains(this)) {
        sharedPreferences.getBoolean(this, defaultValue)
    } else {
        defaultValue
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

fun String.departurePoint(sharedPreferences: SharedPreferences): String {
    val default = if (this.isBack()) "Office" else "Home"
    return this.departurePointKey().userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.destination(sharedPreferences: SharedPreferences): String {
    val default = if (this.isBack()) "Home" else "Office"
    return this.destinationKey().userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.departStation(sharedPreferences: SharedPreferences, num: Int): String {
    val default = "Dep. St. ${num + 1}"
    return this.departStationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.arriveStation(sharedPreferences: SharedPreferences, num: Int): String {
    val default = "Arr. St. ${num + 1}"
    return this.arriveStationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.operatorName(sharedPreferences: SharedPreferences, num: Int): String {
    return this.operatorNameKey(num).userDefaultsValue(sharedPreferences, "") ?: ""
}

fun String.lineName(sharedPreferences: SharedPreferences, num: Int): String {
    val default = "Line ${num + 1}"
    return this.lineNameKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.lineColor(sharedPreferences: SharedPreferences, num: Int): Color {
    val accentString = "#03DAC5"
    return this.lineColorKey(num).userDefaultsColor(sharedPreferences, accentString)
}

fun String.lineCode(sharedPreferences: SharedPreferences, num: Int): String {
    return this.lineCodeKey(num).userDefaultsValue(sharedPreferences, "") ?: ""
}

// Get line kind (railway or bus) for a specific line number
fun String.lineKind(sharedPreferences: SharedPreferences, num: Int): com.mytimetablemaker.models.TransportationLineKind {
    val kindString = this.lineKindKey(num).userDefaultsValue(sharedPreferences, "Railway") ?: "Railway"
    return when (kindString.uppercase()) {
        "RAILWAY", "RAIL" -> com.mytimetablemaker.models.TransportationLineKind.RAILWAY
        "BUS" -> com.mytimetablemaker.models.TransportationLineKind.BUS
        else -> com.mytimetablemaker.models.TransportationLineKind.RAILWAY
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
fun String.departStationArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).map { this.departStation(sharedPreferences, it) }
}

fun String.arriveStationArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).map { this.arriveStation(sharedPreferences, it) }
}

fun String.stationArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).flatMap { listOf(
        this.departStation(sharedPreferences, it),
        this.arriveStation(sharedPreferences, it)
    ) }
}

fun String.operatorNameArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).map { this.operatorName(sharedPreferences, it) }
}

fun String.lineNameArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).map { this.lineName(sharedPreferences, it) }
}

fun String.lineColorArray(sharedPreferences: SharedPreferences): List<Color> {
    return (0..2).map { this.lineColor(sharedPreferences, it) }
}

fun String.lineCodeArray(sharedPreferences: SharedPreferences): List<String> {
    return (0..2).map { this.lineCode(sharedPreferences, it) }
}

fun String.lineKindArray(sharedPreferences: SharedPreferences): List<com.mytimetablemaker.models.TransportationLineKind> {
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
fun String.settingsDeparturePoint(sharedPreferences: SharedPreferences): String {
    val default = "Not set"
    return this.departurePointKey().userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsDestination(sharedPreferences: SharedPreferences): String {
    val default = "Not set"
    return this.destinationKey().userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsDepartStation(sharedPreferences: SharedPreferences, num: Int): String {
    val default = "Not set"
    return this.departStationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsArriveStation(sharedPreferences: SharedPreferences, num: Int): String {
    val default = "Not set"
    return this.arriveStationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsLineName(sharedPreferences: SharedPreferences, num: Int): String {
    val default = "Not set"
    return this.lineNameKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsLineColor(sharedPreferences: SharedPreferences, num: Int): Color {
    val grayString = "#9C9C9C"
    return this.lineColorKey(num).userDefaultsColor(sharedPreferences, grayString)
}

fun String.settingsLineColorString(sharedPreferences: SharedPreferences, num: Int): String {
    val grayString = "#9C9C9C"
    return this.lineColorKey(num).userDefaultsValue(sharedPreferences, grayString) ?: grayString
}

fun String.settingsRideTime(sharedPreferences: SharedPreferences, num: Int): String {
    val rideTime = this.rideTime(sharedPreferences, num)
    return if (rideTime == 0) {
        "Not set"
    } else {
        "$rideTime [min]"
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

fun String.settingsTransportation(sharedPreferences: SharedPreferences, num: Int): String {
    val default = "Not set"
    return this.transportationKey(num).userDefaultsValue(sharedPreferences, default) ?: default
}

fun String.settingsTransferTime(sharedPreferences: SharedPreferences, num: Int): String {
    val transferTime = this.transferTime(sharedPreferences, num)
    return if (transferTime == 0) {
        "Not set"
    } else {
        "$transferTime [min]"
    }
}

// MARK: - Label Generation
fun String.departurePointLabel(): String {
    return if (this.isBack()) "Destination" else "Departure place"
}

fun String.destinationLabel(): String {
    return if (this.isBack()) "Departure place" else "Destination"
}

fun String.stationLabelArray(sharedPreferences: SharedPreferences): List<String> {
    val labels = mutableListOf<String>()
    labels.add(this.departurePointLabel())
    labels.add(this.destinationLabel())
    (0..2).forEach { i ->
        labels.add(i.departStationDefault())
        labels.add(i.arriveStationDefault())
    }
    return labels
}

fun String.transferDepartNum(sharedPreferences: SharedPreferences, num: Int): Int {
    return if (num == 0) this.changeLineInt(sharedPreferences) else num - 2
}

fun String.transferDepartStation(sharedPreferences: SharedPreferences, num: Int): String {
    return if (num == 1) {
        this.departurePoint(sharedPreferences)
    } else {
        this.arriveStation(sharedPreferences, this.transferDepartNum(sharedPreferences, num))
    }
}

fun String.transferArriveStation(sharedPreferences: SharedPreferences, num: Int): String {
    return if (num == 0) {
        this.destination(sharedPreferences)
    } else {
        this.departStation(sharedPreferences, num - 1)
    }
}

fun String.transferFromDepartStation(sharedPreferences: SharedPreferences, num: Int): String {
    return "From ${this.transferDepartStation(sharedPreferences, num)} to "
}

fun String.transferToArriveStation(sharedPreferences: SharedPreferences, num: Int): String {
    return "To ${this.transferArriveStation(sharedPreferences, num)}"
}

fun String.transportationLabel(sharedPreferences: SharedPreferences, num: Int): String {
    return if (num == 1) {
        this.transferFromDepartStation(sharedPreferences, num)
    } else {
        this.transferToArriveStation(sharedPreferences, num)
    }
}

// MARK: - Boolean Extensions for Route Direction
fun Boolean.goOrBack1(): String = if (this) "back1" else "go1"

fun Boolean.goOrBack2(): String = if (this) "back2" else "go2"

// MARK: - Int Extensions for Default Names
fun Int.departStationDefault(): String = "Dep. St. ${this + 1}"

fun Int.arriveStationDefault(): String = "Arr. St. ${this + 1}"

fun Int.lineNameDefault(): String = "Line ${this + 1}"

// MARK: - String Localization Extensions
// Multi-language support for string localization
fun String.localized(context: Context): String {
    val resId = context.resources.getIdentifier(this, "string", context.packageName)
    return if (resId != 0) {
        context.getString(resId)
    } else {
        this
    }
}

// Check if string contains hiragana characters
fun String.containsHiragana(): Boolean {
    val pattern = Pattern.compile("[ぁ-ん]")
    return pattern.matcher(this).find()
}

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
// TODO: Implement full timetableArray with ODPTCalendarType support
fun String.timetableArray(
    sharedPreferences: SharedPreferences,
    date: Date,
    calendarType: String = "weekday"
): List<List<Int>> {
    return (0..2).map { num ->
        (4..25).flatMap { hour ->
            timetableTime(sharedPreferences, calendarType, num, hour)
                .timeString()
                .split(" ")
                .mapNotNull { it.toIntOrNull() }
                .map { it + hour * 100 }
                .filter { it >= 0 && it < 2700 }
        }.sorted()
    }
}

// MARK: - Valid Hour Range
// Note: timeArray() and validHourRange() extensions are now in TimeExtensions.kt

// MARK: - Bus Stop Title Generation
// Generate LocalizedTitle for bus stops from note and busstopPole
fun generateBusStopTitle(note: String, busstopPole: String): com.mytimetablemaker.models.LocalizedTitle? {
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
        com.mytimetablemaker.models.LocalizedTitle(ja = japaneseName, en = englishName)
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
    val dest = this["odpt:destinationStation"]
    return when (dest) {
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
fun Map<String, Any>.odptRailwayTitle(): com.mytimetablemaker.models.LocalizedTitle? {
    val railwayTitleDict = this["odpt:railwayTitle"] as? Map<String, String> ?: return null
    return com.mytimetablemaker.models.LocalizedTitle(
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
fun List<com.mytimetablemaker.models.TransportationTime>.mergeAndSortTransportationTimes(): List<com.mytimetablemaker.models.TransportationTime> {
    // Use Set to remove exact duplicates (based on departureTime and arrivalTime)
    val seenTimes = mutableSetOf<String>()
    val uniqueTimes = mutableListOf<com.mytimetablemaker.models.TransportationTime>()
    
    for (time in this) {
        val timeKey = "${time.departureTime}-${time.arrivalTime}"
        if (!seenTimes.contains(timeKey)) {
            seenTimes.add(timeKey)
            uniqueTimes.add(time)
        }
    }
    
    // Sort by departure time
    return uniqueTimes.sortedWith(compareBy<com.mytimetablemaker.models.TransportationTime> { time ->
        time.departureTime.timeToMinutes()
    }.thenBy { time ->
        time.arrivalTime.timeToMinutes()
    })
}

// MARK: - TransportationTime Saving Methods
// Save TransportationTime objects for a specific hour
fun String.saveTransportationTimes(
    transportationTimes: List<com.mytimetablemaker.models.TransportationTime>,
    calendarType: com.mytimetablemaker.models.ODPTCalendarType,
    num: Int,
    hour: Int,
    sharedPreferences: SharedPreferences
) {
    val calendarTag = calendarType.calendarTag()
    val timetableKey = this.timetableKey(calendarTag, num, hour)
    val timetableRideTimeKey = this.timetableRideTimeKey(calendarTag, num, hour)
    val timetableTrainTypeKey = this.timetableTrainTypeKey(calendarTag, num, hour)
    
    // Clear existing data (always remove to ensure clean state)
    sharedPreferences.edit()
        .remove(timetableKey)
        .remove(timetableRideTimeKey)
        .remove(timetableTrainTypeKey)
        .apply()
    
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
        val trainType = if (transportationTime is com.mytimetablemaker.models.TrainTime) {
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
    
    sharedPreferences.edit()
        .putString(timetableKey, timetableString)
        .putString(timetableRideTimeKey, timetableRideTimeString)
        .putString(timetableTrainTypeKey, timetableTrainTypeString)
        .apply()
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
    transportationTimes: List<com.mytimetablemaker.models.TransportationTime>,
    calendarType: com.mytimetablemaker.models.ODPTCalendarType,
    num: Int,
    sharedPreferences: SharedPreferences
) {
    val calendarTag = calendarType.calendarTag()
    val trainTypeListKey = this.trainTypeListKey(calendarTag, num)
    
    // Extract all train types from all transportation times
    val allTrainTypes = transportationTimes.mapNotNull { transportationTime ->
        if (transportationTime is com.mytimetablemaker.models.TrainTime) {
            transportationTime.trainType
        } else {
            null // Bus doesn't have trainType
        }
    }.mapNotNull { trainType ->
        if (trainType.isNullOrEmpty()) {
            null
        } else {
            val components = trainType.split(".")
            components.lastOrNull() ?: trainType
        }
    }
    
    // Remove duplicates and sort
    val uniqueTrainTypes = allTrainTypes.distinct().sorted()
    val trainTypeListString = uniqueTrainTypes.joinToString(" ")
    
    sharedPreferences.edit()
        .putString(trainTypeListKey, trainTypeListString)
        .apply()
    
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
    calendarType: com.mytimetablemaker.models.ODPTCalendarType,
    num: Int,
    hour: Int,
    sharedPreferences: SharedPreferences
): List<com.mytimetablemaker.models.TransportationTime> {
    val calendarTag = calendarType.calendarTag()
    val timetableKey = this.timetableKey(calendarTag, num, hour)
    val timetableRideTimeKey = this.timetableRideTimeKey(calendarTag, num, hour)
    val timetableTrainTypeKey = this.timetableTrainTypeKey(calendarTag, num, hour)
    
    val timetableString = sharedPreferences.getString(timetableKey, null) ?: return emptyList()
    
    val departureTimes = timetableString.split(" ").filter { it.isNotEmpty() }
    val rideTimes = sharedPreferences.getString(timetableRideTimeKey, null)
        ?.split(" ")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    val trainTypes = sharedPreferences.getString(timetableTrainTypeKey, null)
        ?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()
    
    val routeRideTimeKey = this.rideTimeKey(num)
    val defaultRideTime = sharedPreferences.getInt(routeRideTimeKey, 0)
    
    val transportationTimes = mutableListOf<com.mytimetablemaker.models.TransportationTime>()
    for ((index, departureTimeString) in departureTimes.withIndex()) {
        val rideTime = if (index < rideTimes.size) rideTimes[index] else defaultRideTime
        val trainType = if (index < trainTypes.size && trainTypes[index].isNotEmpty()) trainTypes[index] else null
        
        if (trainType == null) {
            // Bus data (no trainType)
            val busTime = com.mytimetablemaker.models.BusTime(
                departureTime = departureTimeString,
                arrivalTime = "",
                busNumber = null,
                routePattern = null,
                rideTime = rideTime
            )
            transportationTimes.add(busTime)
        } else {
            // Train data (has trainType)
            val trainTime = com.mytimetablemaker.models.TrainTime(
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
    calendarType: com.mytimetablemaker.models.ODPTCalendarType,
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
    calendarType: com.mytimetablemaker.models.ODPTCalendarType,
    num: Int,
    sharedPreferences: SharedPreferences
): Boolean {
    val calendarTag = calendarType.calendarTag()
    
    // Check all hours (4-25) to see if data exists
    for (hour in 4..25) {
        val key = this.timetableKey(calendarTag, num, hour)
        if (sharedPreferences.contains(key)) {
            return true
        }
    }
    return false
}

// Load train type list for a specific calendar type and line
fun String.loadTrainTypeList(
    calendarType: com.mytimetablemaker.models.ODPTCalendarType,
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
            com.mytimetablemaker.extensions.colorForTrainType(trainType).priorityValue
        }.thenBy { it })
    }
    
    // If trainTypeListKey doesn't exist, collect from individual hour data
    val validHours = this.validHourRange(calendarType, num, sharedPreferences)
    val allTransportationTimes = mutableListOf<com.mytimetablemaker.models.TransportationTime>()
    for (hour in validHours) {
        val times = this.loadTransportationTimes(calendarType, num, hour, sharedPreferences)
        allTransportationTimes.addAll(times)
    }
    
    // Extract train types from transportation times
    val allTrainTypes = allTransportationTimes.mapNotNull { transportationTime ->
        if (transportationTime is com.mytimetablemaker.models.TrainTime) {
            transportationTime.trainType
        } else {
            null // Bus doesn't have trainType
        }
    }.mapNotNull { trainType ->
        if (trainType.isNullOrEmpty()) {
            null
        } else {
            val components = trainType.split(".")
            components.lastOrNull() ?: trainType
        }
    }
    
    // Remove duplicates and sort
    val uniqueTrainTypes = allTrainTypes.distinct().sortedWith(compareBy<String> { trainType ->
        com.mytimetablemaker.extensions.colorForTrainType(trainType).priorityValue
    }.thenBy { it })
    
    // Save the collected train types to trainTypeListKey for future use
    if (uniqueTrainTypes.isNotEmpty()) {
        val trainTypeListString = uniqueTrainTypes.joinToString(" ")
        sharedPreferences.edit()
            .putString(trainTypeListKey, trainTypeListString)
            .apply()
    }
    
    android.util.Log.d("LineExtensions", "Train types loaded: ${uniqueTrainTypes.size} types")
    return uniqueTrainTypes
}

// MARK: - Choice Copy Time Key Array
// Generate array of keys for copying timetable from other hours or routes
fun String.choiceCopyTimeKeyArray(
    calendarType: com.mytimetablemaker.models.ODPTCalendarType,
    num: Int,
    hour: Int
): List<String> {
    val calendarTag = calendarType.calendarTag()
    val oppositeCalendarTag = if (calendarTag == "weekday") "holiday" else "weekday"
    val otherroute = if (this == "back1" || this == "back2") {
        if (this == "back1") "go1" else "go2"
    } else {
        if (this == "go1") "back1" else "back2"
    }
    
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
    calendarType: com.mytimetablemaker.models.ODPTCalendarType,
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

