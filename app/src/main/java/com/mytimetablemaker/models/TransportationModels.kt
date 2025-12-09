package com.mytimetablemaker.models

import android.content.Context
import java.util.Locale

// MARK: - Localized Title Model
// Common structure for multi-language support across all transportation entities
data class LocalizedTitle(
    val ja: String?,
    val en: String?
) {
    // Get localized name based on current language
    fun getLocalizedName(context: Context): String {
        val currentLanguage = Locale.getDefault().language
        return if (currentLanguage == "ja") {
            ja ?: en ?: ""
        } else {
            en ?: ja ?: ""
        }
    }
    
    // Get localized name with fallback to a base name
    fun getLocalizedName(context: Context, fallbackTo: String): String {
        val localizedName = getLocalizedName(context)
        return if (localizedName.isEmpty()) fallbackTo else localizedName
    }
}

// MARK: - Transportation Time Interface
// Common interface for all transportation time data
interface TransportationTime {
    val departureTime: String
    val arrivalTime: String
    val rideTime: Int
    val calculatedRideTime: Int
    val isValid: Boolean
}

// MARK: - Train Time Model
// Represents train time information with departure, arrival, and ride time data
data class TrainTime(
    override val departureTime: String,
    override val arrivalTime: String,
    val trainNumber: String? = null,
    val trainType: String? = null,
    override val rideTime: Int
) : TransportationTime {
    // Calculate ride time from departure and arrival times
    override val calculatedRideTime: Int
        get() = departureTime.calculateRideTime(arrivalTime)
    
    // Check if this train time has valid data
    override val isValid: Boolean
        get() = departureTime.isNotEmpty() && arrivalTime.isNotEmpty() && rideTime > 0
}

// MARK: - Bus Time Model
// Represents bus time information with departure, arrival, and ride time data
data class BusTime(
    override val departureTime: String,
    override val arrivalTime: String,
    val busNumber: String? = null,
    val routePattern: String? = null,
    override val rideTime: Int
) : TransportationTime {
    // Calculate ride time from departure and arrival times
    override val calculatedRideTime: Int
        get() = departureTime.calculateRideTime(arrivalTime)
    
    // Check if this bus time has valid data
    override val isValid: Boolean
        get() = departureTime.isNotEmpty() && arrivalTime.isNotEmpty() && rideTime > 0
}

// MARK: - Helper Functions
// Calculate ride time in minutes between departure and arrival times
fun String.calculateRideTime(arrivalTime: String): Int {
    val departureComponents = this.split(":")
    val arrivalComponents = arrivalTime.split(":")
    
    if (departureComponents.size != 2 || arrivalComponents.size != 2) {
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
    
    // Handle day rollover (arrival time is next day)
    val rideTimeMinutes = if (arrivalTotalMinutes >= departureTotalMinutes) {
        arrivalTotalMinutes - departureTotalMinutes
    } else {
        (24 * 60) - departureTotalMinutes + arrivalTotalMinutes
    }
    
    return rideTimeMinutes
}

// MARK: - Transportation Line Kind Enum
// Defines transportation line types
enum class TransportationLineKind {
    RAILWAY,
    BUS
}

// MARK: - Transportation Line Model
// Core data structure representing a railway and bus line or transportation route
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
    // Bus-specific properties
    val busRoute: String? = null,
    val pattern: String? = null,
    val busDirection: String? = null,
    val busstopPoleOrder: List<TransportationStop>? = null,
    val title: String? = null
)

// MARK: - Transportation Stop Model
// Unified model for both railway stations and bus stops
data class TransportationStop(
    val kind: TransportationLineKind,
    val name: String,
    val code: String? = null,
    val index: Int? = null,
    val lineCode: String? = null,
    val title: LocalizedTitle? = null,
    // Bus-specific properties
    val note: String? = null,
    val busstopPole: String? = null
) {
    // Display name with localization support
    fun displayName(context: Context): String {
        val currentLanguage = java.util.Locale.getDefault().language
        var baseName: String
        
        if (title != null) {
            val localizedName = title.getLocalizedName(context)
            baseName = if (localizedName.isNotEmpty()) {
                localizedName
            } else {
                name
            }
        } else {
            // For bus stops, try to extract English from busstopPole for English locale
            if (kind == TransportationLineKind.BUS && currentLanguage != "ja") {
                if (busstopPole != null && busstopPole.isNotEmpty()) {
                    val components = busstopPole.split(".")
                    if (components.size > 2) {
                        baseName = components[2].trim()
                    } else {
                        baseName = name
                    }
                } else {
                    baseName = name
                }
            } else {
                baseName = name
            }
        }
        
        // Split by ":" and return first component for ODPT format
        val components = baseName.split(":")
        return components.firstOrNull()?.trim() ?: baseName
    }
}
