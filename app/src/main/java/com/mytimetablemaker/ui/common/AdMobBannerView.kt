package com.mytimetablemaker.ui.common

import android.content.Context
import android.content.res.Resources
import android.util.Log
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

// AdMob banner wrapper for Compose.
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lastLoadTimeState = remember { mutableLongStateOf(0L) }
    var isReadyToLoad by remember { mutableStateOf(false) }
    val minimumLoadInterval = 60_000L // 60 seconds in milliseconds
    
    // Resolve the AdMob unit ID from resources or local.properties.
    val adUnitID = remember { getAdUnitID(context) }
    val bannerHeight = ScreenSize.admobBannerHeight()
    
    // Defer first ad load to avoid blocking initial composition.
    LaunchedEffect(Unit) {
        delay(500)
        isReadyToLoad = true
    }
    
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
            val lastLoadTime = lastLoadTimeState.longValue
            if (currentTime - lastLoadTime >= minimumLoadInterval) {
                val request = AdRequest.Builder().build()
                adView.loadAd(request)
                lastLoadTimeState.longValue = currentTime
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .then(modifier)
    )
}

// Ad unit ID resolution for banner ads.
private fun getAdUnitID(context: Context): String {
    // Method 1: string resource (set by build.gradle.kts).
    try {
        val unitID = context.getString(R.string.admob_banner_unit_id)
        if (unitID.isNotEmpty() && unitID != "$(ADMOB_BANNER_UNIT_ID)") {
            return unitID
        }
    } catch (e: Resources.NotFoundException) {
        Log.w("AdMobBannerView", "admob_banner_unit_id resource not found", e)
    }
    
    // Method 2: local.properties.
    val localPropertiesFile = File(context.filesDir.parentFile, "local.properties")
    if (localPropertiesFile.exists()) {
        val properties = Properties()
        try {
            localPropertiesFile.inputStream().use { properties.load(it) }
            val unitID = properties.getProperty("ADMOB_BANNER_UNIT_ID")
            if (!unitID.isNullOrEmpty()) {
                return unitID
            }
        } catch (e: Exception) {
            Log.w("AdMobBannerView", "Failed to read local.properties for ADMOB_BANNER_UNIT_ID", e)
        }
    }
    
    // Fallback to the test unit ID.
    Log.w("AdMobBannerView", "Using test AdMob banner unit ID; configure ADMOB_BANNER_UNIT_ID.")
    return "ca-app-pub-3940256099942544/6300978111"
}
