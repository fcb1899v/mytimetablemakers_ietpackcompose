import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Load local.properties for environment variables
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.mytimetablemaker"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.mytimetablemaker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Get AdMob App ID from local.properties or use test ID as fallback
        val admobAppId = localProperties.getProperty("ADMOB_APP_ID") 
            ?: "ca-app-pub-3940256099942544~3347511713"
        
        manifestPlaceholders["admob_app_id"] = admobAppId
    }

    buildTypes {
        debug {
            // Use test Ad Unit ID for debug builds
            resValue("string", "admob_banner_unit_id", "ca-app-pub-3940256099942544/6300978111")
        }
        
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Get AdMob Banner Unit ID from local.properties or use test ID as fallback
            val admobBannerUnitId = localProperties.getProperty("ADMOB_BANNER_UNIT_ID") 
                ?: "ca-app-pub-3940256099942544/6300978111"
            
            // Set as resource value for release builds
            resValue("string", "admob_banner_unit_id", admobBannerUnitId)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Firebase - Use BOM to manage versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    
    // Google Mobile Ads
    implementation(libs.play.services.ads)
    
    // Gson for JSON parsing
    implementation(libs.gson)
    
    // OkHttp for HTTP requests
    implementation(libs.okhttp)
        
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}