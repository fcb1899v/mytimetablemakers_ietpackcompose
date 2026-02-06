package com.mytimetablemaker

import android.app.Application
import com.google.android.gms.ads.MobileAds

/**
 * Application class for early initialization of SDKs.
 * MobileAds.initialize() is called here to prevent ANR when first ad is shown.
 */
class MyTransitMakersApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob early to avoid blocking main thread when MainContentScreen loads
        MobileAds.initialize(this) {}
    }
}
