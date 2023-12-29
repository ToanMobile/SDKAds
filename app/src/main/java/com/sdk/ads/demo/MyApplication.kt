package com.sdk.ads.demo

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.LoadAdError
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val ads = AdsSDK.init(this)
            .setAdCallback(object : TAdCallback {
                override fun onAdClicked(adUnit: String, adType: AdType) {
                    super.onAdClicked(adUnit, adType)
                    Log.e("onAdClicked::", "adUnit=$adUnit adType=$adType")
                }

                override fun onAdClosed(adUnit: String, adType: AdType) {
                    super.onAdClosed(adUnit, adType)
                    Log.e("onAdClosed::", "adUnit=$adUnit adType=$adType")
                }

                override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError) {
                    super.onAdFailedToLoad(adUnit, adType, error)
                    Log.e("onAdFailedToLoad::", "adUnit=$adUnit adType=$adType")
                }

                override fun onAdFailedToShowFullScreenContent(adUnit: String, adType: AdType) {
                    super.onAdFailedToShowFullScreenContent(adUnit, adType)
                    Log.e("onAdFailedToShowFullScreenContent::", "adUnit=$adUnit adType=$adType")
                }

                override fun onAdLoaded(adUnit: String, adType: AdType) {
                    super.onAdLoaded(adUnit, adType)
                    Log.e("onAdLoaded::", "adUnit=$adUnit adType=$adType")
                }
            }) // Set global callback for all AdType/AdUnit
            .setIgnoreAdResume(SplashActivity::class.java) // Ingore show AdResume in these classes (All fragments and Activities is Accepted)
        // ads.setEnableOpenAds(false)
        ads.setEnableDebugGDPR(true)
        ads.setEnableRewarded(false)
    }
}
