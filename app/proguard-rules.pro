# ProGuard configuration for MyTransitMakers Timetable Maker application
# With R8 enabled, mapping file is output to app/build/outputs/mapping/release/mapping.txt
# After uploading release AAB, upload the mapping file for the same version in Play Console.

# ---------------------------------------------------------------------------
# Kotlin (scoped: avoid -keep class kotlin.** which affects 100+ classes)
# ---------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ---------------------------------------------------------------------------
# Gson (scoped: keep models + serialization support, not entire com.google.gson.**)
# ---------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.Unsafe
-keep class com.mytimetablemaker.models.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.stream.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---------------------------------------------------------------------------
# OkHttp
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---------------------------------------------------------------------------
# Firebase / GMS (scoped by module; add more -keep if you use other Firebase features)
# ---------------------------------------------------------------------------
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.installations.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ---------------------------------------------------------------------------
# Google Mobile Ads (AdMob) – already scoped to ads
# ---------------------------------------------------------------------------
# (covered by com.google.android.gms.ads.** above)

# ---------------------------------------------------------------------------
# BuildConfig (keep token references etc.)
# ---------------------------------------------------------------------------
-keep class com.mytimetablemaker.BuildConfig { *; }

# ---------------------------------------------------------------------------
# Crash/ANR stack traces (source file and line numbers)
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
