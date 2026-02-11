package com.mytimetablemaker.models

import com.mytimetablemaker.extensions.selectLocalizedName
import com.mytimetablemaker.extensions.busStopEnglishName
import java.util.Locale

// Localized title for multi-language display.
data class LocalizedTitle(
    val ja: String?,
    val en: String?
) {
    // Resolve localized name with a fallback.
    fun getLocalizedName(fallbackTo: String = ""): String {
        val currentLanguage = Locale.getDefault().language
        val localized = currentLanguage.selectLocalizedName(ja, en)
        return localized.ifEmpty { fallbackTo }
    }
}

// Common interface for transportation time data.
interface TransportationTime {
    val departureTime: String
    val arrivalTime: String
    val rideTime: Int
    val calculatedRideTime: Int
    val isValid: Boolean
}

// Train time record with departure/arrival and ride time.
data class TrainTime(
    override val departureTime: String,
    override val arrivalTime: String,
    val trainNumber: String? = null,
    val trainType: String? = null,
    override val rideTime: Int
) : TransportationTime {
    // Calculate ride time from departure and arrival.
    override val calculatedRideTime: Int
        get() = departureTime.calculateRideTime(arrivalTime)
    
    // Check if this train time is valid.
    override val isValid: Boolean
        get() = departureTime.isNotEmpty() && arrivalTime.isNotEmpty() && rideTime > 0
}

// Bus time record with departure/arrival and ride time.
data class BusTime(
    override val departureTime: String,
    override val arrivalTime: String,
    val busNumber: String? = null,
    val routePattern: String? = null,
    override val rideTime: Int
) : TransportationTime {
    // Calculate ride time from departure and arrival.
    override val calculatedRideTime: Int
        get() = departureTime.calculateRideTime(arrivalTime)
    
    // Check if this bus time is valid.
    override val isValid: Boolean
        get() = departureTime.isNotEmpty() && arrivalTime.isNotEmpty() && rideTime > 0
}

// Calculate ride time in minutes between departure and arrival.
fun String.calculateRideTime(arrivalTime: String): Int {
    val departureComponents = this.trim().split(":")
    val arrivalComponents = arrivalTime.trim().split(":")
    
    if (departureComponents.size < 2 || arrivalComponents.size < 2) {
        return 0
    }
    
    val departureHour = departureComponents[0].toIntOrNull()
    val departureMinute = departureComponents[1].toIntOrNull()
    val arrivalHour = arrivalComponents[0].toIntOrNull()
    val arrivalMinute = arrivalComponents[1].toIntOrNull()
    
    if (departureHour == null || departureMinute == null || 
        arrivalHour == null || arrivalMinute == null) {
        return 0
    }
    
    val departureTotalMinutes = departureHour * 60 + departureMinute
    val arrivalTotalMinutes = arrivalHour * 60 + arrivalMinute
    
    // Handle day rollover to next day.
    val rideTimeMinutes = if (arrivalTotalMinutes >= departureTotalMinutes) arrivalTotalMinutes - departureTotalMinutes else (24 * 60) - departureTotalMinutes + arrivalTotalMinutes
    
    return rideTimeMinutes
}

// Transportation line types.
enum class TransportationLineKind {
    RAILWAY,
    BUS;

    /** String value for Firestore storage (e.g. "Railway", "Bus"). */
    fun firestoreRawValue(): String = when (this) {
        RAILWAY -> "Railway"
        BUS -> "Bus"
    }
}

// Transportation line model for rail and bus routes.
data class TransportationLine(
    val id: String,
    val kind: TransportationLineKind,
    val name: String,
    val code: String,
    val operatorCode: String? = null,
    val lineColor: String? = null,
    val startStation: String? = null,
    val endStation: String? = null,
    val destinationStation: String? = null,
    val railwayTitle: LocalizedTitle? = null,
    val lineCode: String? = null,
    val lineDirection: String? = null,
    val ascendingRailDirection: String? = null,
    val descendingRailDirection: String? = null,
    // Railway-specific properties
    val stationOrder: List<TransportationStop>? = null,
    // Bus-specific properties
    val busRoute: String? = null,
    val pattern: String? = null,
    val busDirection: String? = null,
    val busStopPoleOrder: List<TransportationStop>? = null,
    val title: String? = null
) {
    // Get localized display name with locale-aware fallback.
    fun displayName(): String {
        val currentLanguage = Locale.getDefault().language

        if (kind == TransportationLineKind.BUS) {
            // Bus line display with locale-aware precedence.
            val titleJa = railwayTitle?.ja?.trim().orEmpty()
            val titleEn = railwayTitle?.en?.trim().orEmpty()
            val nameValue = name.trim()
            val titleValue = title?.trim().orEmpty()

            val displayName: String = if (currentLanguage == "ja") {
                when {
                    titleJa.isNotEmpty() -> titleJa
                    titleValue.isNotEmpty() -> titleValue
                    nameValue.isNotEmpty() -> nameValue
                    titleEn.isNotEmpty() -> titleEn
                    else -> nameValue
                }
            } else {
                when {
                    titleEn.isNotEmpty() -> titleEn
                    nameValue.isNotEmpty() -> nameValue
                    titleValue.isNotEmpty() -> titleValue
                    titleJa.isNotEmpty() -> titleJa
                    else -> nameValue
                }
            }
            
            // Fix trailing "行行" to "行".
            return if (displayName.endsWith("行行")) displayName.dropLast(1) else displayName
        } else {
            // Railway line: use railwayTitle with fallback to name.
            return railwayTitle?.getLocalizedName(fallbackTo = name) ?: name
        }
    }
}

// Transportation stop model for stations and bus stops.
data class TransportationStop(
    val kind: TransportationLineKind,
    val name: String,
    val code: String? = null,
    val index: Int? = null,
    val lineCode: String? = null,
    val title: LocalizedTitle? = null,
    // Bus-specific properties
    val note: String? = null,
    val busStopPole: String? = null
) {
    // Display name with localization and bus stop English fallback.
    fun displayName(): String {
        val currentLanguage = Locale.getDefault().language
        val titleJa = title?.ja?.trim().orEmpty()
        val titleEn = title?.en?.trim().orEmpty()
        val noteValue = note?.trim().orEmpty()
        val nameValue = name.trim()
        
        // Use busStopEnglishName for English bus stop names.
        val englishFromPole = busStopPole?.busStopEnglishName().orEmpty()

        val baseName = if (currentLanguage == "ja") {
            when {
                titleJa.isNotEmpty() -> titleJa
                noteValue.isNotEmpty() -> noteValue
                nameValue.isNotEmpty() -> nameValue
                titleEn.isNotEmpty() -> titleEn
                englishFromPole.isNotEmpty() -> englishFromPole
                else -> ""
            }
        } else {
            when {
                titleEn.isNotEmpty() -> titleEn
                englishFromPole.isNotEmpty() -> englishFromPole
                nameValue.isNotEmpty() -> nameValue
                titleJa.isNotEmpty() -> titleJa
                noteValue.isNotEmpty() -> noteValue
                else -> ""
            }
        }

        // Split by ":" and return the first component for ODPT format.
        return baseName.split(":").firstOrNull()?.trim() ?: baseName
    }

    // Cleaned name for matching (bus stops prefer note).
    fun cleanedName(): String =
        if (kind == TransportationLineKind.BUS && !note.isNullOrBlank()) note.trim() else name
}
