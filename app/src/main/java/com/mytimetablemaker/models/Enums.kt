package com.mytimetablemaker.models

import android.content.Context
import com.mytimetablemaker.extensions.localized

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
            LocalDataSource.KEIO_BUS -> "keiobus"
            LocalDataSource.NISHITOKYO_BUS -> "nishitokyobus"
            LocalDataSource.KAWASAKI_BUS -> "kawasakibus"
            LocalDataSource.KAWASAKI_TSURUMI_RINKO_BUS -> "kawasakitsurumirinkobus"
            LocalDataSource.KANTO_BUS -> "kantobus"
            LocalDataSource.IZUHAKONE_BUS -> "izuhakonebus"
            LocalDataSource.KEISEI_TRANSIT_BUS -> "keiseitransitbus"
            LocalDataSource.YOKOHAMA_BUS -> "yokohamabus"
            LocalDataSource.TOEI_BUS -> "toeibus"
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
            LocalDataSource.JR_EAST -> "JR-East".localized(context)
            LocalDataSource.TOKYO_METRO -> "TokyoMetro".localized(context)
            LocalDataSource.TOEI_METRO -> "ToeiMetro".localized(context)
            LocalDataSource.TOKYU -> "Tokyu".localized(context)
            LocalDataSource.KEIKYU -> "Keikyu".localized(context)
            LocalDataSource.ODAKYU -> "Odakyu".localized(context)
            LocalDataSource.TOBU -> "Tobu".localized(context)
            LocalDataSource.SEIBU -> "Seibu".localized(context)
            LocalDataSource.SOTETSU -> "Sotetsu".localized(context)
            LocalDataSource.YOKOHAMA_METRO -> "YokohamaMetro".localized(context)
            LocalDataSource.RINKAI -> "TokyoWaterfrontAreaRapidTransit".localized(context)
            LocalDataSource.YURIKAMOME -> "Yurikamome".localized(context)
            LocalDataSource.TSUKUBA -> "MetropolitanIntercityRailway".localized(context)
            LocalDataSource.TAMA -> "TamaMonorail".localized(context)
            LocalDataSource.TOEI_BUS -> "ToeiBus".localized(context)
            LocalDataSource.YOKOHAMA_BUS -> "YokohamaBus".localized(context)
            LocalDataSource.TOKYU_BUS -> "TokyuBus".localized(context)
            LocalDataSource.SEIBU_BUS -> "SeibuBus".localized(context)
            LocalDataSource.SOTETSU_BUS -> "SotetsuBus".localized(context)
            LocalDataSource.KANACHU_BUS -> "Kanachu".localized(context)
            LocalDataSource.KOKUSAI_KOGYO -> "KokusaiKogyo".localized(context)
            LocalDataSource.KEIO_BUS -> "KeioBus".localized(context)
            LocalDataSource.NISHITOKYO_BUS -> "NishitokyoBus".localized(context)
            LocalDataSource.KAWASAKI_BUS -> "KawasakiBus".localized(context)
            LocalDataSource.KAWASAKI_TSURUMI_RINKO_BUS -> "KawasakiTsurumiRinkoBus".localized(context)
            LocalDataSource.KANTO_BUS -> "KantoBus".localized(context)
            LocalDataSource.IZUHAKONE_BUS -> "IzuhakoneBus".localized(context)
            LocalDataSource.KEISEI_TRANSIT_BUS -> "KeiseiTransitBus".localized(context)
        }
    }
    
    // MARK: - Operator Short Display Name Mapping
    // Get short display name for CustomTag (compact version)
    fun operatorShortDisplayName(context: Context): String {
        return when (this) {
            LocalDataSource.JR_EAST -> "JR-E".localized(context)
            LocalDataSource.TOKYO_METRO -> "Metro".localized(context)
            LocalDataSource.TOEI_METRO -> "Toei".localized(context)
            LocalDataSource.TOKYU -> "Tokyu".localized(context)
            LocalDataSource.KEIKYU -> "Keikyu".localized(context)
            LocalDataSource.ODAKYU -> "Odakyu".localized(context)
            LocalDataSource.TOBU -> "Tobu".localized(context)
            LocalDataSource.SEIBU -> "Seibu".localized(context)
            LocalDataSource.SOTETSU -> "Sotetsu".localized(context)
            LocalDataSource.YOKOHAMA_METRO -> "Yokohama".localized(context)
            LocalDataSource.RINKAI -> "TWR".localized(context)
            LocalDataSource.YURIKAMOME -> "Yurikamome".localized(context)
            LocalDataSource.TSUKUBA -> "MIR".localized(context)
            LocalDataSource.TAMA -> "Tama".localized(context)
            LocalDataSource.TOEI_BUS -> "Toei".localized(context)
            LocalDataSource.YOKOHAMA_BUS -> "Yokohama".localized(context)
            LocalDataSource.TOKYU_BUS -> "Tokyu".localized(context)
            LocalDataSource.SEIBU_BUS -> "Seibu".localized(context)
            LocalDataSource.SOTETSU_BUS -> "Sotetsu".localized(context)
            LocalDataSource.KANACHU_BUS -> "Kanachu".localized(context)
            LocalDataSource.KOKUSAI_KOGYO -> "KokusaiKogyo".localized(context)
            LocalDataSource.KEIO_BUS -> "Keio".localized(context)
            LocalDataSource.NISHITOKYO_BUS -> "Nishitokyo".localized(context)
            LocalDataSource.KAWASAKI_BUS -> "Kawasaki".localized(context)
            LocalDataSource.KAWASAKI_TSURUMI_RINKO_BUS -> "Rinko".localized(context)
            LocalDataSource.KANTO_BUS -> "Kanto".localized(context)
            LocalDataSource.IZUHAKONE_BUS -> "Izuhakone".localized(context)
            LocalDataSource.KEISEI_TRANSIT_BUS -> "KeiseiTransit".localized(context)
        }
    }
    
    // MARK: - ODPT Operator Code Mapping
    // Get ODPT operator code for API queries and data matching
    fun operatorCode(): String? {
        return when (this) {
            LocalDataSource.JR_EAST -> "odpt.Operator:JR-East"
            LocalDataSource.TOKYO_METRO -> "odpt.Operator:TokyoMetro"
            LocalDataSource.TOEI_METRO -> "odpt.Operator:Toei"
            LocalDataSource.TOKYU -> "odpt.Operator:Tokyu"
            LocalDataSource.KEIKYU -> "odpt.Operator:Keikyu"
            LocalDataSource.ODAKYU -> "odpt.Operator:Odakyu"
            LocalDataSource.TOBU -> "odpt.Operator:Tobu"
            LocalDataSource.SEIBU -> "odpt.Operator:Seibu"
            LocalDataSource.SOTETSU -> "odpt.Operator:Sotetsu"
            LocalDataSource.YOKOHAMA_METRO -> "odpt.Operator:YokohamaMunicipal"
            LocalDataSource.RINKAI -> "odpt.Operator:TWR"
            LocalDataSource.YURIKAMOME -> "odpt.Operator:Yurikamome"
            LocalDataSource.TSUKUBA -> "odpt.Operator:MIR"
            LocalDataSource.TAMA -> "odpt.Operator:TamaMonorail"
            LocalDataSource.TOKYU_BUS -> "odpt.Operator:TokyuBus"
            LocalDataSource.SEIBU_BUS -> "odpt.Operator:SeibuBus"
            LocalDataSource.SOTETSU_BUS -> "odpt.Operator:SotetsuBus"
            LocalDataSource.KANACHU_BUS -> "odpt.Operator:Kanachu"
            LocalDataSource.KOKUSAI_KOGYO -> "odpt.Operator:KokusaiKogyoBus"
            LocalDataSource.TOEI_BUS -> "Toei/data/ToeiBus-GTFS.zip"
            LocalDataSource.YOKOHAMA_BUS -> "YokohamaMunicipal/Bus.zip?"
            LocalDataSource.KEIO_BUS -> "KeioBus/AllLines.zip?"
            LocalDataSource.NISHITOKYO_BUS -> "TokyuBus/tokyubus_community.zip?"
            LocalDataSource.KAWASAKI_BUS -> "TransportationBureau_CityOfKawasaki/AllLines.zip?"
            LocalDataSource.KAWASAKI_TSURUMI_RINKO_BUS -> "KawasakiTsurumiRinkoBus/allrinko.zip?"
            LocalDataSource.KANTO_BUS -> "KantoBus/AllLines.zip?"
            LocalDataSource.IZUHAKONE_BUS -> "IzuhakoneBus/IZHB.zip?"
            LocalDataSource.KEISEI_TRANSIT_BUS -> "KeiseiTransitBus/AllLines.zip?"
        }
    }
    
    // MARK: - Transportation Type
    // Get transportation type (railway or bus) for data processing
    // TODO: Implement TransportationLine.Kind enum
    fun transportationType(): TransportationKind {
        return when (this) {
            LocalDataSource.JR_EAST, LocalDataSource.TOKYO_METRO, LocalDataSource.TOEI_METRO,
            LocalDataSource.TOKYU, LocalDataSource.KEIKYU, LocalDataSource.ODAKYU, LocalDataSource.TOBU,
            LocalDataSource.SEIBU, LocalDataSource.SOTETSU, LocalDataSource.YOKOHAMA_METRO,
            LocalDataSource.RINKAI, LocalDataSource.YURIKAMOME, LocalDataSource.TSUKUBA, LocalDataSource.TAMA ->
                TransportationKind.RAILWAY
            LocalDataSource.TOEI_BUS, LocalDataSource.YOKOHAMA_BUS, LocalDataSource.TOKYU_BUS,
            LocalDataSource.SEIBU_BUS, LocalDataSource.SOTETSU_BUS, LocalDataSource.KANACHU_BUS,
            LocalDataSource.KOKUSAI_KOGYO, LocalDataSource.KEIO_BUS, LocalDataSource.NISHITOKYO_BUS,
            LocalDataSource.KAWASAKI_BUS, LocalDataSource.KAWASAKI_TSURUMI_RINKO_BUS, LocalDataSource.KANTO_BUS,
            LocalDataSource.IZUHAKONE_BUS, LocalDataSource.KEISEI_TRANSIT_BUS ->
                TransportationKind.BUS
        }
    }
    
    // MARK: - API Type Determination
    // Determine the appropriate API type for this operator
    fun apiType(): ODPTAPIType {
        return when (this) {
            LocalDataSource.TOEI_METRO -> ODPTAPIType.PUBLIC_API
            LocalDataSource.TOKYO_METRO, LocalDataSource.YOKOHAMA_METRO, LocalDataSource.TSUKUBA,
            LocalDataSource.TAMA, LocalDataSource.YURIKAMOME, LocalDataSource.RINKAI,
            LocalDataSource.TOKYU_BUS, LocalDataSource.SEIBU_BUS, LocalDataSource.SOTETSU_BUS ->
                ODPTAPIType.STANDARD
            LocalDataSource.JR_EAST, LocalDataSource.TOKYU, LocalDataSource.ODAKYU, LocalDataSource.KEIKYU,
            LocalDataSource.TOBU, LocalDataSource.SEIBU, LocalDataSource.SOTETSU,
            LocalDataSource.KANACHU_BUS, LocalDataSource.KOKUSAI_KOGYO ->
                ODPTAPIType.CHALLENGE
            LocalDataSource.KEIO_BUS, LocalDataSource.NISHITOKYO_BUS, LocalDataSource.KAWASAKI_BUS,
            LocalDataSource.KAWASAKI_TSURUMI_RINKO_BUS, LocalDataSource.KANTO_BUS, LocalDataSource.IZUHAKONE_BUS,
            LocalDataSource.KEISEI_TRANSIT_BUS, LocalDataSource.YOKOHAMA_BUS, LocalDataSource.TOEI_BUS ->
                ODPTAPIType.GTFS
        }
    }
    
    // Indicates if this operator provides train timetables
    fun hasTrainTimeTable(): Boolean {
        return when (this) {
            LocalDataSource.JR_EAST, LocalDataSource.TOBU, LocalDataSource.SOTETSU,
            LocalDataSource.TOKYO_METRO, LocalDataSource.TOEI_METRO, LocalDataSource.YOKOHAMA_METRO,
            LocalDataSource.RINKAI, LocalDataSource.TSUKUBA, LocalDataSource.TAMA ->
                true
            else -> false
        }
    }
    
    // Indicates if this operator provides bus timetables
    fun hasBusTimeTable(): Boolean {
        return when (this) {
            LocalDataSource.TOEI_BUS, LocalDataSource.YOKOHAMA_BUS, LocalDataSource.TOKYU_BUS,
            LocalDataSource.SEIBU_BUS, LocalDataSource.SOTETSU_BUS, LocalDataSource.KANACHU_BUS,
            LocalDataSource.KOKUSAI_KOGYO, LocalDataSource.KEIO_BUS, LocalDataSource.NISHITOKYO_BUS,
            LocalDataSource.KAWASAKI_BUS, LocalDataSource.KAWASAKI_TSURUMI_RINKO_BUS, LocalDataSource.KANTO_BUS,
            LocalDataSource.IZUHAKONE_BUS, LocalDataSource.KEISEI_TRANSIT_BUS ->
                true
            else -> false
        }
    }
    
    // MARK: - Train Type Mapping
    // Get available train types for each operator
    fun operatorTrainType(): List<String> {
        return when (this) {
            LocalDataSource.JR_EAST -> listOf(
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
            LocalDataSource.TOKYO_METRO -> listOf(
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
            LocalDataSource.TOEI_METRO -> listOf(
                "odpt.TrainType:Toei.AccessExpress",
                "odpt.TrainType:Toei.AirportRapidLimitedExpress",
                "odpt.TrainType:Toei.CommuterLimitedExpress",
                "odpt.TrainType:Toei.Express",
                "odpt.TrainType:Toei.LimitedExpress",
                "odpt.TrainType:Toei.Local",
                "odpt.TrainType:Toei.RapidLimitedExpress",
                "odpt.TrainType:Toei.Rapid"
            )
            LocalDataSource.TOKYU -> listOf(
                "odpt.TrainType:Tokyu.CommuterLimitedExpress",
                "odpt.TrainType:Tokyu.Express",
                "odpt.TrainType:Tokyu.F-Liner",
                "odpt.TrainType:Tokyu.LimitedExpress",
                "odpt.TrainType:Tokyu.Local",
                "odpt.TrainType:Tokyu.S-TRAIN",
                "odpt.TrainType:Tokyu.SemiExpress"
            )
            LocalDataSource.KEIKYU -> listOf(
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
            LocalDataSource.ODAKYU -> listOf(
                "odpt.TrainType:Odakyu.CommuterExpress",
                "odpt.TrainType:Odakyu.CommuterSemiExpress",
                "odpt.TrainType:Odakyu.Express",
                "odpt.TrainType:Odakyu.LimitedExpress",
                "odpt.TrainType:Odakyu.Local",
                "odpt.TrainType:Odakyu.RapidExpress",
                "odpt.TrainType:Odakyu.SemiExpress"
            )
            LocalDataSource.TOBU -> listOf(
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
            LocalDataSource.SEIBU -> listOf(
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
            LocalDataSource.SOTETSU -> listOf(
                "odpt.TrainType:Sotetsu.CommuterExpress",
                "odpt.TrainType:Sotetsu.CommuterLimitedExpress",
                "odpt.TrainType:Sotetsu.Express",
                "odpt.TrainType:Sotetsu.LimitedExpress",
                "odpt.TrainType:Sotetsu.Local",
                "odpt.TrainType:Sotetsu.Rapid"
            )
            LocalDataSource.YOKOHAMA_METRO -> listOf(
                "odpt.TrainType:YokohamaMunicipal.Local",
                "odpt.TrainType:YokohamaMunicipal.Rapid"
            )
            LocalDataSource.RINKAI -> listOf(
                "odpt.TrainType:TWR.CommuterRapid",
                "odpt.TrainType:TWR.Local",
                "odpt.TrainType:TWR.Rapid"
            )
            LocalDataSource.YURIKAMOME -> listOf(
                "odpt.TrainType:Yurikamome.Local"
            )
            LocalDataSource.TSUKUBA -> listOf(
                "odpt.TrainType:MIR.CommuterRapid",
                "odpt.TrainType:MIR.Local",
                "odpt.TrainType:MIR.Rapid",
                "odpt.TrainType:MIR.SemiRapid"
            )
            LocalDataSource.TAMA -> listOf(
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
            return "Unknown".localized(context)
        }
        val components = trainType.split(".")
        val lastComponent = components.lastOrNull() ?: return "Unknown".localized(context)
        return lastComponent.localized(context)
    }
    
    // MARK: - Unified API Link Generation
    // Generate API links using clean enum-based approach
    fun apiLink(
        dataType: APIDataType,
        transportationKind: TransportationKind = TransportationKind.RAILWAY,
        odptAccessKey: String = "",
        odptChallengeKey: String = ""
    ): String {
        val operatorCode = operatorCode() ?: return ""
        if (operatorCode.isEmpty()) return ""
        
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
                if (this == LocalDataSource.TOEI_BUS) {
                    "https://api-public.odpt.org/api/v4/files/$operatorCode"
                } else {
                    // For other GTFS operators, use standard API with date and access token
                    val dateString = GTFSDates.dateFor(this) ?: return ""
                    if (dateString.isEmpty()) return ""
                    "https://api.odpt.org/api/v4/files/odpt/$operatorCode?date=$dateString&acl:consumerKey=$odptAccessKey"
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
        val allCases = values().toList()
    }
}

// MARK: - ODPT Error Types
// Custom error types for ODPT operations
sealed class ODPTError : Exception() {
    object DateExtractionFailed : ODPTError()
    data class NetworkError(val errorMessage: String) : ODPTError()
    object InvalidData : ODPTError()
    
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
        return this.debugDisplayName().localized(context)
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
            NONE -> "None".localized(context)
            WALKING -> "Walking".localized(context)
            BICYCLE -> "Bicycle".localized(context)
            CAR -> "Car".localized(context)
        }
    }
    
    companion object {
        val allCases = values().toList()
    }
}

