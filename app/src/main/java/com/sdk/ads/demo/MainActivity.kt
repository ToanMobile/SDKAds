package com.sdk.ads.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.LoadAdError
import com.sdk.ads.ads.AdsInitializeListener
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ads.interstitial.AdmobInterSplash
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AdsSDK.initialize(activity = this, listener = object : AdsInitializeListener() {
            override fun onInitialize() {
                logger("AdsSDK:::", "onInitialize")
            }

            override fun onFail(message: String) {
                super.onFail(message)
                logger("AdsSDK:::", "onFail:$message")
            }

            override fun onPurchase(isPurchase: Boolean) {
                super.onPurchase(isPurchase)
                logger("AdsSDK:::", "onPurchase")
            }

            override fun always() {
                super.always()
                logger("AdsSDK:::", "always")
            }
        })
        findViewById<TextView>(R.id.txtText).setOnClickListener {
            com.sdk.ads.utils.logEvent("txtTextClick")
            AdmobInterSplash.show(adUnitId = "ca-app-pub-2428922951355303/8012376174", timeout = 3000, nextAction = {
                logger("callbackADS", "Ads")
            })
        }

        com.sdk.ads.ads.nativead.AdmobNative.show(
            findViewById(R.id.layoutAdsNative),
            adUnitId = "ca-app-pub-3940256099942544/2247696110", // "ca-app-pub-2428922951355303/6635076633",
            nativeContentLayoutId = R.layout.layout_ads_native,
            forceRefresh = false,
            callback = object : TAdCallback {
                override fun onAdLoaded(adUnit: String, adType: AdType) {
                    super.onAdLoaded(adUnit, adType)
                    logger("callbackADS", "onAdLoaded")
                }

                override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError?) {
                    super.onAdFailedToLoad(adUnit, adType, error)
                    logger("callbackADS", "onAdFailedToLoad")
                }

                override fun onAdFailedToShowFullScreenContent(adUnit: String, adType: AdType) {
                    super.onAdFailedToShowFullScreenContent(adUnit, adType)
                    logger("callbackADS", "onAdFailedToShowFullScreenContent")
                }
            },
        )

        com.sdk.ads.ads.banner.AdmobBanner.showAdaptive(
            findViewById(R.id.viewTop),
            adUnitId = "ca-app-pub-3940256099942544/6300978111",
            forceRefresh = false,
            callback = object : TAdCallback {
                override fun onAdLoaded(adUnit: String, adType: AdType) {
                    super.onAdLoaded(adUnit, adType)
                    logger("onAdLoaded::", adType.toString())
                    com.sdk.ads.utils.logScreen(this::class.java.simpleName)
                }

                override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError?) {
                    super.onAdFailedToLoad(adUnit, adType, error)
                    logger("onAdFailedToLoad::", adType.toString())
                }

                override fun onAdOpened(adUnit: String, adType: AdType) {
                    super.onAdOpened(adUnit, adType)
                    logger("onAdLoaded::", adType.toString())
                }
            },
        )

        com.sdk.ads.ads.banner.AdmobBanner.showCollapsible(
            findViewById(R.id.viewBottom),
            adUnitId = "ca-app-pub-3940256099942544/8388050270",
            forceRefresh = false,
            callback = object : TAdCallback {
                override fun onAdLoaded(adUnit: String, adType: AdType) {
                    super.onAdLoaded(adUnit, adType)
                    logger("onAdLoaded::", adType.toString())
                    com.sdk.ads.utils.logScreen(this::class.java.simpleName)
                }

                override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError?) {
                    super.onAdFailedToLoad(adUnit, adType, error)
                    logger("onAdFailedToLoad::", adType.toString())
                }

                override fun onAdOpened(adUnit: String, adType: AdType) {
                    super.onAdOpened(adUnit, adType)
                    logger("onAdLoaded::", adType.toString())
                }
            },
        )
        /*com.sdk.ads.ads.banner.AdmobBanner.show300x250(
            findViewById(R.id.viewBottom),
            adUnitId = "ca-app-pub-3940256099942544/2014213617",
            forceRefresh = true,
            callback = object : com.sdk.ads.utils.TAdCallback {
                override fun onAdLoaded(adUnit: String, adType: com.sdk.ads.utils.AdType) {
                    super.onAdLoaded(adUnit, adType)
                    com.sdk.ads.utils.logScreen(this::class.java.simpleName)
                }

                override fun onAdClicked(adUnit: String, adType: com.sdk.ads.utils.AdType) {
                    super.onAdClicked(adUnit, adType)
                }

                override fun onAdFailedToShowFullScreenContent(adUnit: String, adType: AdType) {
                    super.onAdFailedToShowFullScreenContent(adUnit, adType)
                }

                override fun onAdClosed(adUnit: String, adType: AdType) {
                    super.onAdClosed(adUnit, adType)
                }

                override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: com.google.android.gms.ads.LoadAdError) {
                    super.onAdFailedToLoad(adUnit, adType, error)
                }
            },
        )*/
    }
}
