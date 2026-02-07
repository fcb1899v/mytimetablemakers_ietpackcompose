package com.mytimetablemaker.ui.common

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.R
import kotlinx.coroutines.delay
import java.io.File
import java.util.Properties

// MARK: - AdMob Banner View
// Jetpack Compose wrapper for Google Mobile Ads banner view
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastLoadTime by remember { mutableLongStateOf(0L) }
    var isReadyToLoad by remember { mutableStateOf(false) }
    val minimumLoadInterval = 60_000L // 60 seconds in milliseconds
    
    // Get AdMob Banner Unit ID from resources or local.properties
    val adUnitID = remember { getAdUnitID(context) }
    val bannerHeight = ScreenSize.admobBannerHeight()
    
    // Defer first ad load to avoid blocking main thread during initial composition
    LaunchedEffect(Unit) {
        delay(500)
        isReadyToLoad = true
    }
    
    Box(
        modifier = modifier
            .then(Modifier.height(bannerHeight))
    ) {
        AndroidView(
            factory = { ctx ->
                val adView = AdView(ctx)
                adView.adUnitId = adUnitID
                adView.setAdSize(AdSize.BANNER)
                adView
            },
            update = { adView ->
                if (!isReadyToLoad) return@AndroidView
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
                .then(modifier)
        )
    }
}

// MARK: - Ad Unit ID Configuration
// Get AdMob unit ID from resources or local.properties
private fun getAdUnitID(context: Context): String {
    // Method 1: Try to get from string resources (set by build.gradle.kts)
    try {
        val unitID = context.getString(R.string.admob_banner_unit_id)
        if (unitID.isNotEmpty() && unitID != "$(ADMOB_BANNER_UNIT_ID)") {
            return unitID
        }
    } catch (_: Exception) {
        // Resource not found, continue to next method
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
