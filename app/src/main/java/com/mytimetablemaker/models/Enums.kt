package com.mytimetablemaker.models

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mytimetablemaker.BuildConfig
import com.mytimetablemaker.R

// MARK: - GTFS Date Constants
// Hardcoded dates for GTFS data files (format: YYYYMMDD)
object GTFSDates {
    private val dates = mapOf(
        LocalDataSource.KEIO_BUS to "20251117",
        LocalDataSource.NISHITOKYO_BUS to "20251101",
        LocalDataSource.KAWASAKI_BUS to "20251201",
        LocalDataSource.KAWASAKI_TSURUMI_RINKO_BUS to "20251117",
        LocalDataSource.KANTO_BUS to "20251110",
        LocalDataSource.IZUHAKONE_BUS to "20251101",
        LocalDataSource.KEISEI_TRANSIT_BUS to "20250401",
        LocalDataSource.YOKOHAMA_BUS to "20251101",
        LocalDataSource.TOEI_BUS to "" // Toei Bus doesn't use date parameter
    )
    
    fun dateFor(operator: LocalDataSource): String? {
        return dates[operator]
    }
}

// MARK: - Custom Color Enumeration
// Defines color options for line customization
enum class CustomColor(val rawValue: String) {
    RED("RED"),              // Pure red - #E60012
    DARK_RED("DARK RED"),    // Dark red - #A22041
    ORANGE("ORANGE"),        // Orange - #FF6600
    BROWN("BROWN"),          // Brown - #8F4C38
    YELLOW("YELLOW"),        // Bright yellow - #FFD400
    BEIGE("BEIGE"),          // Beige - #C1A470
    ORIVE("ORIVE"),          // Olive green - #9FB01C
    YELLOW_GREEN("YELLOW GREEN"), // Yellow green - #9ACD32
    GREEN("GREEN"),         // Green - #009739
    DARK_GREEN("DARK GREEN"), // Dark green - #004E2E
    BLUE_GREEN("BLUE GREEN"), // Blue green - #00AC9A
    LIGHT_BLUE("LIGHT BLUE"), // Light blue - #00BFFF
    BLUE("BLUE"),           // Pure blue - #0000FF
    NAVY_BLUE("NAVY BLUE"),  // Navy blue - #003580
    PRIMARY("INDIGO"),      // Indigo - #3700B3
    LAVENDER("LAVENDER"),   // Lavender - #8F76D6
    PURPLE("PURPLE"),       // Purple - #B22C8D
    MAGENTA("MAGENTA"),     // Magenta - #E4007F
    PINK("PINK"),           // Pink - #E85298
    GRAY("GRAY"),           // Gray - #9C9C9C
    SILVER("SILVER"),       // Silver - #89A1AD
    GOLD("GOLD"),           // Gold - #C5C544
    BLACK("BLACK"),         // Black - #000000
    ACCENT("DEFAULT")       // Default accent color - #03DAC5
}

// MARK: - Data Source Definitions
// Defines available data sources for railway information
enum class LocalDataSource {
    JR_EAST,                    // JR East railway lines
    TOKYO_METRO,                // Tokyo Metro subway lines
    TOEI_METRO,                 // Toei subway lines
    YOKOHAMA_METRO,             // Yokohama Municipal Subway
    TOBU,                       // Tobu Railway
    YURIKAMOME,                 // Yurikamomey line
    SOTETSU,                    // Sotetsu Railway
    TSUKUBA,                    // Tsukuba Express
    TAMA,                       // Tama Monorail
    RINKAI,                     // Rinkai Line
    KEIKYU,                     // Keikyu railway lines
    ODAKYU,                     // Odakyu railway lines
    SEIBU,                      // Seibu Railway
    TOKYU,                      // Tokyu Railway
    TOKYU_BUS,                  // Tokyu Bus
    SEIBU_BUS,                  // Seibu Bus
    SOTETSU_BUS,                // Sotetsu Bus
    KANACHU_BUS,                // Kanachu Bus
    KOKUSAI_KOGYO,              // Kokusai Kogyo Bus
    TOEI_BUS,                   // Toei Bus
    YOKOHAMA_BUS,               // Yokohama Municipal Bus
    KEIO_BUS,                   // Keio Bus
    KANTO_BUS,                  // Kanto Bus
    NISHITOKYO_BUS,             // Nishitokyo Bus
    KAWASAKI_BUS,               // Kawasaki City Bus
    KAWASAKI_TSURUMI_RINKO_BUS, // Kawasaki Tsurumi Rinko Bus
    KEISEI_TRANSIT_BUS,         // Keisei Transit Bus
    IZUHAKONE_BUS;              // Izuhakone Bus
    
    // MARK: - GTFS File Name for Cache
    // Generate safe file name for GTFS cache keys (without special characters)
    // Used for cache key generation to avoid issues with special characters in file paths
    fun gtfsFileName(): String {
        return when (this) {
            KEIO_BUS -> "keiobus"
            NISHITOKYO_BUS -> "nishitokyobus"
            KAWASAKI_BUS -> "kawasakibus"
            KAWASAKI_TSURUMI_RINKO_BUS -> "kawasakitsurumirinkobus"
            KANTO_BUS -> "kantobus"
            IZUHAKONE_BUS -> "izuhakonebus"
            KEISEI_TRANSIT_BUS -> "keiseitransitbus"
            YOKOHAMA_BUS -> "yokohamabus"
            TOEI_BUS -> "toeibus"
            else -> {
                // For non-GTFS operators, use fileName but remove special characters
                fileName()
                    .replace("/", "_")
                    .replace("?", "_")
                    .replace(".json", "")
            }
        }
    }
    
    // MARK: - File Name Mapping
    // Generate filename dynamically from operatorCode with transportation type included
    fun fileName(): String {
        // Extract operator name from operatorCode (remove "odpt.Operator:" prefix)
        val operatorCode = operatorCode() ?: return ""
        val operatorName = operatorCode.replace("odpt.Operator:", "")
        // Convert to lowercase and keep hyphens
        val normalizedName = operatorName.lowercase().replace(" ", "")
        // Add transportation type suffix
        return "${normalizedName}_${transportationType().rawValue.lowercase()}.json"
    }
    
    // MARK: - Operator Display Name Mapping
    // Get localized display name for operator selection UI
    fun operatorDisplayName(context: Context): String {
        return when (this) {
            JR_EAST -> context.getString(R.string.jrEast)
            TOKYO_METRO -> context.getString(R.string.tokyoMetro)
            TOEI_METRO -> context.getString(R.string.toeiMetro)
            TOKYU -> context.getString(R.string.tokyu)
            KEIKYU -> context.getString(R.string.keikyu)
            ODAKYU -> context.getString(R.string.odakyu)
            TOBU -> context.getString(R.string.tobu)
            SEIBU -> context.getString(R.string.seibu)
            SOTETSU -> context.getString(R.string.sotetsu)
            YOKOHAMA_METRO -> context.getString(R.string.yokohamaMetro)
            RINKAI -> context.getString(R.string.tokyoWaterfrontAreaRapidTransit)
            YURIKAMOME -> context.getString(R.string.yurikamome)
            TSUKUBA -> context.getString(R.string.metropolitanIntercityRailway)
            TAMA -> context.getString(R.string.tamaMonorail)
            TOEI_BUS -> context.getString(R.string.toeiBus)
            YOKOHAMA_BUS -> context.getString(R.string.yokohamaBus)
            TOKYU_BUS -> context.getString(R.string.tokyuBus)
            SEIBU_BUS -> context.getString(R.string.seibuBus)
            SOTETSU_BUS -> context.getString(R.string.sotetsuBus)
            KANACHU_BUS -> context.getString(R.string.kanachu)
            KOKUSAI_KOGYO -> context.getString(R.string.kokusaiKogyo)
            KEIO_BUS -> context.getString(R.string.keioBus)
            NISHITOKYO_BUS -> context.getString(R.string.nishitokyoBus)
            KAWASAKI_BUS -> context.getString(R.string.kawasakiBus)
            KAWASAKI_TSURUMI_RINKO_BUS -> context.getString(R.string.kawasakiTsurumiRinkoBus)
            KANTO_BUS -> context.getString(R.string.kantoBus)
            IZUHAKONE_BUS -> context.getString(R.string.izuhakoneBus)
            KEISEI_TRANSIT_BUS -> context.getString(R.string.keiseiTransitBus)
        }
    }
    
    // MARK: - Operator Short Display Name Mapping
    // Get short display name for CustomTag (compact version)
    fun operatorShortDisplayName(context: Context): String {
        return when (this) {
            JR_EAST -> context.getString(R.string.jrE)
            TOKYO_METRO -> context.getString(R.string.metro)
            TOEI_METRO -> context.getString(R.string.toei)
            TOKYU -> context.getString(R.string.tokyuShort)
            KEIKYU -> context.getString(R.string.keikyuShort)
            ODAKYU -> context.getString(R.string.odakyuShort)
            TOBU -> context.getString(R.string.tobuShort)
            SEIBU -> context.getString(R.string.seibuShort)
            SOTETSU -> context.getString(R.string.sotetsuShort)
            YOKOHAMA_METRO -> context.getString(R.string.yokohama)
            RINKAI -> context.getString(R.string.twr)
            YURIKAMOME -> context.getString(R.string.yurikamomeShort)
            TSUKUBA -> context.getString(R.string.mir)
            TAMA -> context.getString(R.string.tama)
            TOEI_BUS -> context.getString(R.string.toei)
            YOKOHAMA_BUS -> context.getString(R.string.yokohama)
            TOKYU_BUS -> context.getString(R.string.tokyuShort)
            SEIBU_BUS -> context.getString(R.string.seibuShort)
            SOTETSU_BUS -> context.getString(R.string.sotetsuShort)
            KANACHU_BUS -> context.getString(R.string.kanachuShort)
            KOKUSAI_KOGYO -> context.getString(R.string.kokusaiKogyoShort)
            KEIO_BUS -> context.getString(R.string.keio)
            NISHITOKYO_BUS -> context.getString(R.string.nishitokyo)
            KAWASAKI_BUS -> context.getString(R.string.kawasaki)
            KAWASAKI_TSURUMI_RINKO_BUS -> context.getString(R.string.rinko)
            KANTO_BUS -> context.getString(R.string.kanto)
            IZUHAKONE_BUS -> context.getString(R.string.izuhakone)
            KEISEI_TRANSIT_BUS -> context.getString(R.string.keiseiTransit)
        }
    }
    
    // MARK: - ODPT Operator Code Mapping
    // Get ODPT operator code for API queries and data matching
    fun operatorCode(): String? {
        return when (this) {
            JR_EAST -> "odpt.Operator:JR-East"
            TOKYO_METRO -> "odpt.Operator:TokyoMetro"
            TOEI_METRO -> "odpt.Operator:Toei"
            TOKYU -> "odpt.Operator:Tokyu"
            KEIKYU -> "odpt.Operator:Keikyu"
            ODAKYU -> "odpt.Operator:Odakyu"
            TOBU -> "odpt.Operator:Tobu"
            SEIBU -> "odpt.Operator:Seibu"
            SOTETSU -> "odpt.Operator:Sotetsu"
            YOKOHAMA_METRO -> "odpt.Operator:YokohamaMunicipal"
            RINKAI -> "odpt.Operator:TWR"
            YURIKAMOME -> "odpt.Operator:Yurikamome"
            TSUKUBA -> "odpt.Operator:MIR"
            TAMA -> "odpt.Operator:TamaMonorail"
            TOKYU_BUS -> "odpt.Operator:TokyuBus"
            SEIBU_BUS -> "odpt.Operator:SeibuBus"
            SOTETSU_BUS -> "odpt.Operator:SotetsuBus"
            KANACHU_BUS -> "odpt.Operator:Kanachu"
            KOKUSAI_KOGYO -> "odpt.Operator:KokusaiKogyoBus"
            TOEI_BUS -> "Toei/data/ToeiBus-GTFS.zip"
            YOKOHAMA_BUS -> "YokohamaMunicipal/Bus.zip?"
            KEIO_BUS -> "KeioBus/AllLines.zip?"
            NISHITOKYO_BUS -> "TokyuBus/tokyubus_community.zip?"
            KAWASAKI_BUS -> "TransportationBureau_CityOfKawasaki/AllLines.zip?"
            KAWASAKI_TSURUMI_RINKO_BUS -> "KawasakiTsurumiRinkoBus/allrinko.zip?"
            KANTO_BUS -> "KantoBus/AllLines.zip?"
            IZUHAKONE_BUS -> "IzuhakoneBus/IZHB.zip?"
            KEISEI_TRANSIT_BUS -> "KeiseiTransitBus/AllLines.zip?"
        }
    }
    
    // MARK: - Transportation Type
    // Get transportation type (railway or bus) for data processing
    // TODO: Implement TransportationLine.Kind enum
    fun transportationType(): TransportationKind {
        return when (this) {
            JR_EAST, TOKYO_METRO, TOEI_METRO,
            TOKYU, KEIKYU, ODAKYU, TOBU,
            SEIBU, SOTETSU, YOKOHAMA_METRO,
            RINKAI, YURIKAMOME, TSUKUBA, TAMA ->
                TransportationKind.RAILWAY
            TOEI_BUS, YOKOHAMA_BUS, TOKYU_BUS,
            SEIBU_BUS, SOTETSU_BUS, KANACHU_BUS,
            KOKUSAI_KOGYO, KEIO_BUS, NISHITOKYO_BUS,
            KAWASAKI_BUS, KAWASAKI_TSURUMI_RINKO_BUS, KANTO_BUS,
            IZUHAKONE_BUS, KEISEI_TRANSIT_BUS ->
                TransportationKind.BUS
        }
    }
    
    // MARK: - API Type Determination
    // Determine the appropriate API type for this operator
    fun apiType(): ODPTAPIType {
        return when (this) {
            TOEI_METRO -> ODPTAPIType.PUBLIC_API
            TOKYO_METRO, YOKOHAMA_METRO, TSUKUBA,
            TAMA, YURIKAMOME, RINKAI,
            TOKYU_BUS, SEIBU_BUS, SOTETSU_BUS ->
                ODPTAPIType.STANDARD
            JR_EAST, TOKYU, ODAKYU, KEIKYU,
            TOBU, SEIBU, SOTETSU,
            KANACHU_BUS, KOKUSAI_KOGYO ->
                ODPTAPIType.CHALLENGE
            KEIO_BUS, NISHITOKYO_BUS, KAWASAKI_BUS,
            KAWASAKI_TSURUMI_RINKO_BUS, KANTO_BUS, IZUHAKONE_BUS,
            KEISEI_TRANSIT_BUS, YOKOHAMA_BUS, TOEI_BUS ->
                ODPTAPIType.GTFS
        }
    }
    
    // Indicates if this operator provides train timetables
    fun hasTrainTimeTable(): Boolean {
        return when (this) {
            JR_EAST, TOBU, SOTETSU,
            TOKYO_METRO, TOEI_METRO, YOKOHAMA_METRO,
            RINKAI, TSUKUBA, TAMA ->
                true
            else -> false
        }
    }
    
    // Indicates if this operator provides bus timetables
    fun hasBusTimeTable(): Boolean {
        return when (this) {
            TOEI_BUS, YOKOHAMA_BUS, TOKYU_BUS,
            SEIBU_BUS, SOTETSU_BUS, KANACHU_BUS,
            KOKUSAI_KOGYO, KEIO_BUS, NISHITOKYO_BUS,
            KAWASAKI_BUS, KAWASAKI_TSURUMI_RINKO_BUS, KANTO_BUS,
            IZUHAKONE_BUS, KEISEI_TRANSIT_BUS ->
                true
            else -> false
        }
    }
    
    // MARK: - Train Type Mapping
    // Get available train types for each operator
    fun operatorTrainType(): List<String> {
        return when (this) {
            JR_EAST -> listOf(
                "odpt.TrainType:JR-East.ChuoSpecialRapid",
                "odpt.TrainType:JR-East.CommuterRapid",
                "odpt.TrainType:JR-East.CommuterSpecialRapid",
                "odpt.TrainType:JR-East.Express",
                "odpt.TrainType:JR-East.LimitedExpress",
                "odpt.TrainType:JR-East.Liner",
                "odpt.TrainType:JR-East.Local",
                "odpt.TrainType:JR-East.OmeSpecialRapid",
                "odpt.TrainType:JR-East.Rapid",
                "odpt.TrainType:JR-East.SpecialRapid"
            )
            TOKYO_METRO -> listOf(
                "odpt.TrainType:TokyoMetro.CommuterExpress",
                "odpt.TrainType:TokyoMetro.CommuterLimitedExpress",
                "odpt.TrainType:TokyoMetro.CommuterRapid",
                "odpt.TrainType:TokyoMetro.Express",
                "odpt.TrainType:TokyoMetro.F-Liner",
                "odpt.TrainType:TokyoMetro.LimitedExpress",
                "odpt.TrainType:TokyoMetro.Local",
                "odpt.TrainType:TokyoMetro.RapidExpress",
                "odpt.TrainType:TokyoMetro.Rapid",
                "odpt.TrainType:TokyoMetro.S-TRAIN",
                "odpt.TrainType:TokyoMetro.SemiExpress",
                "odpt.TrainType:TokyoMetro.TH-LINER"
            )
            TOEI_METRO -> listOf(
                "odpt.TrainType:Toei.AccessExpress",
                "odpt.TrainType:Toei.AirportRapidLimitedExpress",
                "odpt.TrainType:Toei.CommuterLimitedExpress",
                "odpt.TrainType:Toei.Express",
                "odpt.TrainType:Toei.LimitedExpress",
                "odpt.TrainType:Toei.Local",
                "odpt.TrainType:Toei.RapidLimitedExpress",
                "odpt.TrainType:Toei.Rapid"
            )
            TOKYU -> listOf(
                "odpt.TrainType:Tokyu.CommuterLimitedExpress",
                "odpt.TrainType:Tokyu.Express",
                "odpt.TrainType:Tokyu.F-Liner",
                "odpt.TrainType:Tokyu.LimitedExpress",
                "odpt.TrainType:Tokyu.Local",
                "odpt.TrainType:Tokyu.S-TRAIN",
                "odpt.TrainType:Tokyu.SemiExpress"
            )
            KEIKYU -> listOf(
                "odpt.TrainType:Keikyu.AccessExpress",
                "odpt.TrainType:Keikyu.AirportRapidLimitedExpress",
                "odpt.TrainType:Keikyu.CommuterLimitedExpress",
                "odpt.TrainType:Keikyu.EveningWing",
                "odpt.TrainType:Keikyu.Express",
                "odpt.TrainType:Keikyu.LimitedExpress",
                "odpt.TrainType:Keikyu.Local",
                "odpt.TrainType:Keikyu.MorningWing",
                "odpt.TrainType:Keikyu.RapidLimitedExpress",
                "odpt.TrainType:Keikyu.Rapid"
            )
            ODAKYU -> listOf(
                "odpt.TrainType:Odakyu.CommuterExpress",
                "odpt.TrainType:Odakyu.CommuterSemiExpress",
                "odpt.TrainType:Odakyu.Express",
                "odpt.TrainType:Odakyu.LimitedExpress",
                "odpt.TrainType:Odakyu.Local",
                "odpt.TrainType:Odakyu.RapidExpress",
                "odpt.TrainType:Odakyu.SemiExpress"
            )
            TOBU -> listOf(
                "odpt.TrainType:Tobu.Express",
                "odpt.TrainType:Tobu.F-Liner",
                "odpt.TrainType:Tobu.KawagoeLimitedExpress",
                "odpt.TrainType:Tobu.LimitedExpress",
                "odpt.TrainType:Tobu.Local",
                "odpt.TrainType:Tobu.RapidExpress",
                "odpt.TrainType:Tobu.Rapid",
                "odpt.TrainType:Tobu.SL-Taiju",
                "odpt.TrainType:Tobu.SectionExpress",
                "odpt.TrainType:Tobu.SectionSemiExpress",
                "odpt.TrainType:Tobu.SemiExpress",
                "odpt.TrainType:Tobu.TH-LINER",
                "odpt.TrainType:Tobu.TJ-Liner"
            )
            SEIBU -> listOf(
                "odpt.TrainType:Seibu.CommuterExpress",
                "odpt.TrainType:Seibu.CommuterSemiExpress",
                "odpt.TrainType:Seibu.Express",
                "odpt.TrainType:Seibu.F-Liner",
                "odpt.TrainType:Seibu.HaijimaLiner",
                "odpt.TrainType:Seibu.LimitedExpress",
                "odpt.TrainType:Seibu.Local",
                "odpt.TrainType:Seibu.RapidExpress",
                "odpt.TrainType:Seibu.Rapid",
                "odpt.TrainType:Seibu.S-TRAIN",
                "odpt.TrainType:Seibu.SemiExpress"
            )
            SOTETSU -> listOf(
                "odpt.TrainType:Sotetsu.CommuterExpress",
                "odpt.TrainType:Sotetsu.CommuterLimitedExpress",
                "odpt.TrainType:Sotetsu.Express",
                "odpt.TrainType:Sotetsu.LimitedExpress",
                "odpt.TrainType:Sotetsu.Local",
                "odpt.TrainType:Sotetsu.Rapid"
            )
            YOKOHAMA_METRO -> listOf(
                "odpt.TrainType:YokohamaMunicipal.Local",
                "odpt.TrainType:YokohamaMunicipal.Rapid"
            )
            RINKAI -> listOf(
                "odpt.TrainType:TWR.CommuterRapid",
                "odpt.TrainType:TWR.Local",
                "odpt.TrainType:TWR.Rapid"
            )
            YURIKAMOME -> listOf(
                "odpt.TrainType:Yurikamome.Local"
            )
            TSUKUBA -> listOf(
                "odpt.TrainType:MIR.CommuterRapid",
                "odpt.TrainType:MIR.Local",
                "odpt.TrainType:MIR.Rapid",
                "odpt.TrainType:MIR.SemiRapid"
            )
            TAMA -> listOf(
                "odpt.TrainType:TamaMonorail.Local"
            )
            // Bus operators don't have train types
            else -> emptyList()
        }
    }
    
    // MARK: - Train Type Helper Methods
    // Get display name for a specific train type using localization
    fun getDisplayName(context: Context, trainType: String?): String {
        if (trainType == null) {
            return context.getString(R.string.dash)
        }
        // Extract resource name from train type string (e.g., "odpt.TrainType:JR-East.ChuoSpecialRapid" -> "chuoSpecialRapid")
        val parts = trainType.split(".")
        if (parts.size >= 3) {
            val resourceName = parts[2].replaceFirstChar { it.lowercaseChar() }
            return when (resourceName) {
                "accessExpress" -> context.getString(R.string.accessExpress)
                "airportRapidLimitedExpress" -> context.getString(R.string.airportRapidLimitedExpress)
                "chuoSpecialRapid" -> context.getString(R.string.chuoSpecialRapid)
                "commuterExpress" -> context.getString(R.string.commuterExpress)
                "commuterLimitedExpress" -> context.getString(R.string.commuterLimitedExpress)
                "commuterRapid" -> context.getString(R.string.commuterRapid)
                "commuterSemiExpress" -> context.getString(R.string.commuterSemiExpress)
                "commuterSpecialRapid" -> context.getString(R.string.commuterSpecialRapid)
                "eveningWing" -> context.getString(R.string.eveningWing)
                "express" -> context.getString(R.string.express)
                "haijimaLiner" -> context.getString(R.string.haijimaLiner)
                "kawagoeLimitedExpress" -> context.getString(R.string.kawagoeLimitedExpress)
                "limitedExpress" -> context.getString(R.string.limitedExpress)
                "liner" -> context.getString(R.string.liner)
                "local" -> context.getString(R.string.local)
                "morningWing" -> context.getString(R.string.morningWing)
                "omeSpecialRapid" -> context.getString(R.string.omeSpecialRapid)
                "rapid" -> context.getString(R.string.rapid)
                "rapidExpress" -> context.getString(R.string.rapidExpress)
                "rapidLimitedExpress" -> context.getString(R.string.rapidLimitedExpress)
                "semiExpress" -> context.getString(R.string.semiExpress)
                "semiRapid" -> context.getString(R.string.semiRapid)
                "sectionExpress" -> context.getString(R.string.sectionExpress)
                "sectionSemiExpress" -> context.getString(R.string.sectionSemiExpress)
                "specialRapid" -> context.getString(R.string.specialRapid)
                "fLiner" -> context.getString(R.string.fLiner)
                "sTrain" -> context.getString(R.string.sTrain)
                "slTaiju" -> context.getString(R.string.slTaiju)
                "thLiner" -> context.getString(R.string.thLiner)
                "tjLiner" -> context.getString(R.string.tjLiner)
                else -> trainType
            }
        }
        return trainType
    }
    
    // MARK: - Unified API Link Generation
    // Generate API links using clean enum-based approach
    // Reads ODPT access tokens directly from BuildConfig (environment variables)
    fun apiLink(
        dataType: APIDataType,
        transportationKind: TransportationKind = TransportationKind.RAILWAY
    ): String {
        val operatorCode = operatorCode() ?: return ""
        if (operatorCode.isEmpty()) return ""
        
        // Read ODPT access tokens from BuildConfig (environment variables)
        val odptAccessKey = BuildConfig.ODPT_ACCESS_TOKEN
        val odptChallengeKey = BuildConfig.ODPT_CHALLENGE_TOKEN
        
        // Generate ODPT API URL for non-GTFS operators
        val odptDataType = if (transportationKind == TransportationKind.RAILWAY) {
            dataType.railwayOdpTDataType()
        } else {
            dataType.busOdpTDataType()
        }
        
        return when (apiType()) {
            ODPTAPIType.PUBLIC_API -> {
                "https://api-public.odpt.org/api/v4/${odptDataType.apiEndpoint()}?odpt:operator=$operatorCode"
            }
            ODPTAPIType.STANDARD -> {
                "https://api.odpt.org/api/v4/${odptDataType.apiEndpoint()}?odpt:operator=$operatorCode&acl:consumerKey=$odptAccessKey"
            }
            ODPTAPIType.CHALLENGE -> {
                "https://api-challenge.odpt.org/api/v4/${odptDataType.apiEndpoint()}?odpt:operator=$operatorCode&acl:consumerKey=$odptChallengeKey"
            }
            ODPTAPIType.GTFS -> {
                // Special handling for Toei Bus (uses public API, no access token needed)
                if (this == TOEI_BUS) {
                    "https://api-public.odpt.org/api/v4/files/$operatorCode"
                } else {
                    // For other GTFS operators, use standard API with date and access token
                    // Remove trailing '?' from operatorCode if present (some operatorCodes have '?' suffix)
                    val cleanOperatorCode = operatorCode.trimEnd('?')
                    val dateString = GTFSDates.dateFor(this) ?: return ""
                    if (dateString.isEmpty()) return ""
                    "https://api.odpt.org/api/v4/files/odpt/$cleanOperatorCode?date=$dateString&acl:consumerKey=$odptAccessKey"
                }
            }
        }
    }
}

// MARK: - Transportation Kind Enum
// Temporary enum for transportation type (will be replaced by TransportationLine.Kind)
enum class TransportationKind(val rawValue: String) {
    RAILWAY("Railway"),
    BUS("Bus")
}

// MARK: - API Data Type Enum
// Defines the type of data to request from the API
enum class APIDataType {
    LINE,              // Railway line or bus route information
    TIMETABLE,         // Train timetable data
    STOP_TIMETABLE,    // Station timetable data
    STOP;              // Bus stop pole data
    
    // Maps APIDataType to ODPTDataType for railway context
    // Note: .STOP is not used for railway (stations are included in odpt:Railway data)
    fun railwayOdpTDataType(): ODPTDataType {
        return when (this) {
            LINE -> ODPTDataType.RAILWAY
            TIMETABLE -> ODPTDataType.TRAIN_TIMETABLE
            STOP_TIMETABLE -> ODPTDataType.STATION_TIMETABLE
            STOP -> ODPTDataType.RAILWAY // Not used for railway, fallback to railway
        }
    }
    
    // Maps APIDataType to ODPTDataType for bus context
    fun busOdpTDataType(): ODPTDataType {
        return when (this) {
            LINE -> ODPTDataType.BUS_ROUTE_PATTERN
            TIMETABLE -> ODPTDataType.BUS_TIMETABLE
            STOP_TIMETABLE -> ODPTDataType.BUS_TIMETABLE
            STOP -> ODPTDataType.BUSSTOP_POLE
        }
    }
}

// MARK: - Display Train Type Enum
// Common train type categories for color mapping
enum class DisplayTrainType(val rawValue: String) {
    // MARK: - Default Train Types
    DEFAULT_LOCAL("defaultLocal"),
    DEFAULT_EXPRESS("defaultExpress"),
    DEFAULT_RAPID("defaultRapid"),
    DEFAULT_SPECIAL_RAPID("defaultSpecialRapid"),
    DEFAULT_LIMITED_EXPRESS("defaultLimitedExpress"),
    
    // MARK: - Standard Train Types
    LOCAL("Local"),
    RAPID("Rapid"),
    SEMI_EXPRESS("SemiExpress"),
    EXPRESS("Express"),
    COMMUTER_EXPRESS("CommuterExpress"),
    COMMUTER_SEMI_EXPRESS("CommuterSemiExpress"),
    COMMUTER_RAPID("CommuterRapid"),
    COMMUTER_LIMITED_EXPRESS("CommuterLimitedExpress"),
    RAPID_EXPRESS("RapidExpress"),
    RAPID_LIMITED_EXPRESS("RapidLimitedExpress"),
    LIMITED_EXPRESS("LimitedExpress"),
    ACCESS_EXPRESS("AccessExpress"),
    AIRPORT_RAPID_LIMITED_EXPRESS("AirportRapidLimitedExpress"),
    KAWAGOE_LIMITED_EXPRESS("KawagoeLimitedExpress"),
    SPECIAL_RAPID("SpecialRapid"),
    COMMUTER_SPECIAL_RAPID("CommuterSpecialRapid"),
    CHUO_SPECIAL_RAPID("ChuoSpecialRapid"),
    OME_SPECIAL_RAPID("OmeSpecialRapid"),
    SECTION_EXPRESS("SectionExpress"),
    SECTION_SEMI_EXPRESS("SectionSemiExpress"),
    SEMI_RAPID("SemiRapid"),
    LINER("Liner"),
    F_LINER("FLiner"),
    TH_LINER("ThLiner"),
    TJ_LINER("TjLiner"),
    HAIJIMA_LINER("HaijimaLiner"),
    S_TRAIN("STrain"),
    SL_TAIJU("SlTaiju"),
    EVENING_WING("EveningWing"),
    MORNING_WING("MorningWing"),
    UNKNOWN("Unknown");
    
    companion object {
        val allCases = entries
    }
    
    // Localized display name for UI (ODPT train type string resolution uses rawValue match)
    fun displayName(context: Context): String = when (this) {
        DEFAULT_LOCAL -> context.getString(R.string.local)
        DEFAULT_EXPRESS -> context.getString(R.string.express)
        DEFAULT_RAPID -> context.getString(R.string.rapid)
        DEFAULT_SPECIAL_RAPID -> context.getString(R.string.specialRapid)
        DEFAULT_LIMITED_EXPRESS -> context.getString(R.string.limitedExpress)
        LOCAL -> context.getString(R.string.local)
        RAPID -> context.getString(R.string.rapid)
        SEMI_EXPRESS -> context.getString(R.string.semiExpress)
        EXPRESS -> context.getString(R.string.express)
        COMMUTER_EXPRESS -> context.getString(R.string.commuterExpress)
        COMMUTER_SEMI_EXPRESS -> context.getString(R.string.commuterSemiExpress)
        COMMUTER_RAPID -> context.getString(R.string.commuterRapid)
        COMMUTER_LIMITED_EXPRESS -> context.getString(R.string.commuterLimitedExpress)
        RAPID_EXPRESS -> context.getString(R.string.rapidExpress)
        RAPID_LIMITED_EXPRESS -> context.getString(R.string.rapidLimitedExpress)
        LIMITED_EXPRESS -> context.getString(R.string.limitedExpress)
        ACCESS_EXPRESS -> context.getString(R.string.accessExpress)
        AIRPORT_RAPID_LIMITED_EXPRESS -> context.getString(R.string.airportRapidLimitedExpress)
        KAWAGOE_LIMITED_EXPRESS -> context.getString(R.string.kawagoeLimitedExpress)
        SPECIAL_RAPID -> context.getString(R.string.specialRapid)
        COMMUTER_SPECIAL_RAPID -> context.getString(R.string.commuterSpecialRapid)
        CHUO_SPECIAL_RAPID -> context.getString(R.string.chuoSpecialRapid)
        OME_SPECIAL_RAPID -> context.getString(R.string.omeSpecialRapid)
        SECTION_EXPRESS -> context.getString(R.string.sectionExpress)
        SECTION_SEMI_EXPRESS -> context.getString(R.string.sectionSemiExpress)
        SEMI_RAPID -> context.getString(R.string.semiRapid)
        LINER -> context.getString(R.string.liner)
        F_LINER -> context.getString(R.string.fLiner)
        TH_LINER -> context.getString(R.string.thLiner)
        TJ_LINER -> context.getString(R.string.tjLiner)
        HAIJIMA_LINER -> context.getString(R.string.haijimaLiner)
        S_TRAIN -> context.getString(R.string.sTrain)
        SL_TAIJU -> context.getString(R.string.slTaiju)
        EVENING_WING -> context.getString(R.string.eveningWing)
        MORNING_WING -> context.getString(R.string.morningWing)
        UNKNOWN -> rawValue
    }
}

// MARK: - ODPT Error Types
// Custom error types for ODPT operations
sealed class ODPTError : Exception() {
    class DateExtractionFailed : ODPTError() {
        private fun readResolve(): Any = DateExtractionFailed()
    }

    data class NetworkError(val errorMessage: String) : ODPTError()
    class InvalidData : ODPTError() {
        private fun readResolve(): Any = InvalidData()
    }

    override val message: String?
        get() = when (this) {
            is DateExtractionFailed -> "Failed to extract date from API response"
            is NetworkError -> "Network error: ${this.errorMessage}"
            is InvalidData -> "Invalid data structure"
        }
}

// MARK: - ODPT Calendar Type Enumeration
// Defines calendar types used in ODPT API for timetable scheduling
sealed class ODPTCalendarType {
    abstract val rawValue: String
    
    object Weekday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Weekday"
    }
    object Holiday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Holiday"
    }
    object SaturdayHoliday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:SaturdayHoliday"
    }
    object Sunday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Sunday"
    }
    object Monday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Monday"
    }
    object Tuesday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Tuesday"
    }
    object Wednesday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Wednesday"
    }
    object Thursday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Thursday"
    }
    object Friday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Friday"
    }
    object Saturday : ODPTCalendarType() {
        override val rawValue = "odpt.Calendar:Saturday"
    }
    data class Specific(val value: String) : ODPTCalendarType() {
        override val rawValue = value
    }
    
    companion object {
        fun fromRawValue(rawValue: String): ODPTCalendarType? {
            return when (rawValue) {
                "odpt.Calendar:Weekday" -> Weekday
                "odpt.Calendar:Holiday" -> Holiday
                "odpt.Calendar:SaturdayHoliday" -> SaturdayHoliday
                "odpt.Calendar:Sunday" -> Sunday
                "odpt.Calendar:Monday" -> Monday
                "odpt.Calendar:Tuesday" -> Tuesday
                "odpt.Calendar:Wednesday" -> Wednesday
                "odpt.Calendar:Thursday" -> Thursday
                "odpt.Calendar:Friday" -> Friday
                "odpt.Calendar:Saturday" -> Saturday
                else -> {
                    // Handle special calendar types (keep original rawValue for API calls)
                    if (rawValue.startsWith("odpt.Calendar:Specific.")) {
                        Specific(rawValue)
                    } else {
                        null // Unknown calendar type
                    }
                }
            }
        }
        
        val allCases = listOf(
            Weekday, Holiday, SaturdayHoliday, Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
        )
    }
    
    // MARK: - Display Calendar Type
    // Convert .specific calendar types to standard types for display
    // API calls use original rawValue, but display uses converted types
    fun displayCalendarType(): ODPTCalendarType {
        return when (this) {
            is Specific -> {
                // Check for suffix patterns (e.g., "odpt.Calendar:Specific.YokohamaMunicipal.01_1.Weekday")
                val components = this.value.split(".")
                val lastComponent = components.lastOrNull()
                if (lastComponent != null) {
                    // Check if last component is a day type name
                    when (lastComponent) {
                        "Weekday" -> Weekday
                        "Saturday" -> Saturday
                        "Holiday" -> Holiday
                        else -> {
                            // Handle identifier patterns (e.g., "odpt.Calendar:Specific.Toei.81-170" or "21_7")
                            // Extract identifier and check last part after "-" or "_"
                            val identifier = lastComponent
                            val partsByDash = identifier.split("-")
                            val partsByUnderscore = identifier.split("_")
                            val lastPart = if (partsByDash.size > 1) {
                                partsByDash.lastOrNull() ?: ""
                            } else if (partsByUnderscore.size > 1) {
                                partsByUnderscore.lastOrNull() ?: ""
                            } else {
                                identifier
                            }
                            
                            when (lastPart) {
                                "100", "109" -> Holiday
                                "160" -> Saturday
                                "170", "179" -> Weekday
                                else -> Weekday  // Fallback to weekday
                            }
                        }
                    }
                } else {
                    Weekday  // Default fallback
                }
            }
            else -> this
        }
    }
    
    // MARK: - Base Display Name
    // Base English name for each calendar type
    fun debugDisplayName(): String {
        val displayType = this.displayCalendarType()
        return when (displayType) {
            is Weekday -> "Weekday"
            is Holiday -> "Holiday"
            is SaturdayHoliday -> "Saturday/Holiday"
            is Sunday -> "Sunday"
            is Monday -> "Monday"
            is Tuesday -> "Tuesday"
            is Wednesday -> "Wednesday"
            is Thursday -> "Thursday"
            is Friday -> "Friday"
            is Saturday -> "Saturday"
            is Specific -> "Specific"
        }
    }
    
    // MARK: - Display Name
    // Localized display name for each calendar type
    // For .specific types, shows only the display type (identifier not shown for cleaner UI)
    fun displayName(context: Context): String {
        val displayType = this.displayCalendarType()
        return when (displayType) {
            is Weekday -> context.getString(R.string.weekday)
            is Holiday -> context.getString(R.string.holiday)
            is SaturdayHoliday -> context.getString(R.string.saturdayHoliday)
            is Sunday -> context.getString(R.string.sunday)
            is Monday -> context.getString(R.string.monday)
            is Tuesday -> context.getString(R.string.tuesday)
            is Wednesday -> context.getString(R.string.wednesday)
            is Thursday -> context.getString(R.string.thursday)
            is Friday -> context.getString(R.string.friday)
            is Saturday -> context.getString(R.string.saturday)
            is Specific -> context.getString(R.string.dash) // Specific types show dash
        }
    }
    
    // MARK: - Calendar Tag
    // Get calendar tag for UserDefaults keys
    // For .specific types, use identifier to ensure unique keys and prevent data overwriting
    fun calendarTag(): String {
        // Extract identifier from .specific rawValue for unique key
        if (this is Specific) {
            val components = this.value.split(".")
            val lastComponent = components.lastOrNull()
            if (lastComponent != null) {
                return lastComponent.lowercase()
            }
        }
        // For standard types, use display type tag
        val displayType = this.displayCalendarType()
        return if (displayType is SaturdayHoliday) {
            "weekend"
        } else {
            displayType.debugDisplayName().lowercase()
        }
    }
}

// MARK: - ODPT Data Type Enum
// Enumeration for different ODPT data types with associated values
enum class ODPTDataType {
    RAILWAY,
    TRAIN_TIMETABLE,
    STATION_TIMETABLE,
    BUS_ROUTE_PATTERN,
    BUS_TIMETABLE,
    BUSSTOP_POLE;
    
    // MARK: - API Endpoint
    fun apiEndpoint(): String {
        return when (this) {
            RAILWAY -> "odpt:Railway"
            TRAIN_TIMETABLE -> "odpt:TrainTimetable"
            STATION_TIMETABLE -> "odpt:StationTimetable"
            BUS_ROUTE_PATTERN -> "odpt:BusroutePattern"
            BUS_TIMETABLE -> "odpt:BusTimetable"
            BUSSTOP_POLE -> "odpt:BusstopPole"
        }
    }
}

// MARK: - ODPT API Type Enum
// Enumeration for different ODPT API endpoints
enum class ODPTAPIType {
    STANDARD,    // Standard API with access key
    PUBLIC_API,  // Public API without access key
    CHALLENGE,   // Challenge API with challenge key
    GTFS         // No API (Use GTFS Data)
}

// Get localized display name for ODPT train type string (e.g. "odpt.TrainType:JR-East.ChuoSpecialRapid" or short form "ChuoSpecialRapid")
fun getTrainTypeDisplayName(trainType: String, context: Context): String {
    val resourceName = if (trainType.contains(".")) {
        val parts = trainType.split(".")
        if (parts.size >= 3) parts[2].replaceFirstChar { it.lowercaseChar() } else trainType.replaceFirstChar { it.lowercaseChar() }
    } else {
        trainType.replaceFirstChar { it.lowercaseChar() }
    }
    val displayType = DisplayTrainType.entries.firstOrNull { it.rawValue.replaceFirstChar { c -> c.lowercaseChar() } == resourceName }
    return displayType?.displayName(context) ?: trainType
}

// MARK: - Transfer Type Enumeration
// Enumeration of available transportation methods for transfer
enum class TransferType(val rawValue: String) {
    CAR("car"),           // Car transportation
    BICYCLE("bicycle"),   // Bicycle transportation
    WALKING("walking"),   // Walking between stations
    NONE("none");         // No transfer required
    
    // MARK: - Transportation Method Display Name
    // Localized display name for each transportation method
    fun transportationDisplayName(context: Context): String {
        return when (this) {
            NONE -> context.getString(R.string.none)
            WALKING -> context.getString(R.string.walking)
            BICYCLE -> context.getString(R.string.bicycle)
            CAR -> context.getString(R.string.car)
        }
    }
    
    // MARK: - Icon Property
    // Icon for each transportation method
    val icon: ImageVector
        get() = when (this) {
            NONE -> Icons.Filled.Cancel
            WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
            BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
            CAR -> Icons.Filled.DirectionsCar
        }
    
    companion object {
        val allCases = entries
        
        // MARK: - Transfer Type Helper Function
        // Helper function to convert string labels to TransferType enum values
        fun transferType(label: String, context: Context): TransferType {
            val labelLower = label.lowercase()
            val noneLocalized = context.getString(R.string.none).lowercase()
            val walkingLocalized = context.getString(R.string.walking).lowercase()
            val bicycleLocalized = context.getString(R.string.bicycle).lowercase()
            val carLocalized = context.getString(R.string.car).lowercase()
            
            return when (labelLower) {
                "none", noneLocalized -> NONE
                "walking", walkingLocalized -> WALKING
                "bicycle", bicycleLocalized -> BICYCLE
                "car", carLocalized -> CAR
                else -> WALKING // Default to walking instead of none
            }
        }
        
        // MARK: - Transfer Type from Raw Value
        // Helper function to get TransferType from raw value string
        fun fromRawValue(value: String): TransferType {
            return entries.find { it.rawValue == value } ?: NONE
        }
    }
}

// MARK: - TransferType Composable Extension
// Composable extension function for getting localized display name
@Composable
fun TransferType.displayName(): String {
    return when (this) {
        TransferType.NONE -> stringResource(R.string.none)
        TransferType.WALKING -> stringResource(R.string.walking)
        TransferType.BICYCLE -> stringResource(R.string.bicycle)
        TransferType.CAR -> stringResource(R.string.car)
    }
}

