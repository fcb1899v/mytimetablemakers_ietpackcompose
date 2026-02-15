# ProGuard configuration for MyTransitMakers Timetable Maker application
# With R8 enabled, mapping file is output to app/build/outputs/mapping/release/mapping.txt
# After uploading release AAB, upload the mapping file for the same version in Play Console.

# ---------------------------------------------------------------------------
# Kotlin
# ---------------------------------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ---------------------------------------------------------------------------
# Gson (keep model classes for JSON serialization)
# ---------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.Unsafe
-keep class com.google.gson.** { *; }
-keep class com.mytimetablemaker.models.** { *; }

# ---------------------------------------------------------------------------
# OkHttp
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---------------------------------------------------------------------------
# Firebase Auth / Firestore
# ---------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ---------------------------------------------------------------------------
# Google Mobile Ads (AdMob)
# ---------------------------------------------------------------------------
-keep class com.google.android.gms.ads.** { *; }

# ---------------------------------------------------------------------------
# BuildConfig (keep token references etc.)
# ---------------------------------------------------------------------------
-keep class com.mytimetablemaker.BuildConfig { *; }

# ---------------------------------------------------------------------------
# Crash/ANR stack traces (source file and line numbers)
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
