package com.sdk.ads.demo

import android.app.Application
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.utils.TAdCallback

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val ads = AdsSDK.init(this)
            .setAdCallback(object : TAdCallback {
            }) // Set global callback for all AdType/AdUnit
            .setIgnoreAdResume(SplashActivity::class.java) // Ingore show AdResume in these classes (All fragments and Activities is Accepted)
        // ads.setEnableOpenAds(false)
        ads.setEnableRewarded(false)
    }
}
