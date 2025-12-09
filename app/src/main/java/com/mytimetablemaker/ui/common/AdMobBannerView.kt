package com.mytimetablemaker.ui.common

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.mytimetablemaker.extensions.ScreenSize
import java.util.Properties
import java.io.File

// MARK: - AdMob Banner View
// Jetpack Compose wrapper for Google Mobile Ads banner view
@Composable
fun AdMobBannerView() {
    val context = LocalContext.current
    var viewWidth by remember { mutableStateOf(0f) }
    var lastLoadTime by remember { mutableStateOf(0L) }
    val minimumLoadInterval = 60_000L // 60 seconds in milliseconds
    
    // Get AdMob Banner Unit ID from resources or local.properties
    val adUnitID = remember {
        getAdUnitID(context)
    }
    
    // Get banner height
    val bannerHeight = ScreenSize.admobBannerHeight()
    
    // Initialize MobileAds if not already initialized
    LaunchedEffect(Unit) {
        MobileAds.initialize(context) {}
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight)
    ) {
        AndroidView(
            factory = { ctx ->
                val adView = AdView(ctx)
                adView.adUnitId = adUnitID
                adView.setAdSize(AdSize.BANNER)
                adView
            },
            update = { adView ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastLoadTime >= minimumLoadInterval) {
                    val request = AdRequest.Builder().build()
                    adView.loadAd(request)
                    lastLoadTime = currentTime
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
        )
    }
}

// MARK: - Ad Unit ID Configuration
// Get AdMob unit ID from resources or local.properties
private fun getAdUnitID(context: Context): String {
    // Method 1: Try to get from string resources (set by build.gradle.kts)
    val resourceId = context.resources.getIdentifier(
        "admob_banner_unit_id",
        "string",
        context.packageName
    )
    if (resourceId != 0) {
        val unitID = context.getString(resourceId)
        if (unitID.isNotEmpty() && unitID != "\$(ADMOB_BANNER_UNIT_ID)") {
            return unitID
        }
    }
    
    // Method 2: Try to get from local.properties
    val localPropertiesFile = File(context.filesDir.parentFile, "local.properties")
    if (localPropertiesFile.exists()) {
        val properties = Properties()
        localPropertiesFile.inputStream().use { properties.load(it) }
        val unitID = properties.getProperty("ADMOB_BANNER_UNIT_ID")
        if (!unitID.isNullOrEmpty()) {
            return unitID
        }
    }
    
    // Fallback to test unit ID
    return "ca-app-pub-3940256099942544/6300978111"
}
