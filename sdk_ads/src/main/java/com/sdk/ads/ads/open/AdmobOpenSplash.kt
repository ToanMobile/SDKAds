package com.sdk.ads.ads.open

import android.os.CountDownTimer
import com.google.android.gms.ads.LoadAdError
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.getAppCompatActivityOnTop
import com.sdk.ads.utils.onNextActionWhenResume
import com.sdk.ads.utils.waitActivityResumed

object AdmobOpenSplash {

    private var timer: CountDownTimer? = null

    /**
     * @param adUnitId: adUnitId
     * @param timeout: timeout to wait ad show
     * @param onAdLoaded: callback when adLoaded => for update UI or something
     * @param nextAction: callback for your work, general handle nextActivity or nextFragment
     */
    fun show(
        adUnitId: String,
        timeout: Long = 15000,
        onAdLoaded: () -> Unit = {},
        nextAction: () -> Unit = {},
    ) {
        if (!AdsSDK.isEnableOpenAds) {
            onAdLoaded.invoke()
            nextAction.invoke()
            return
        }

        val callback = object : TAdCallback {

            override fun onAdLoaded(adUnit: String, adType: AdType) {
                super.onAdLoaded(adUnit, adType)
                onAdLoaded.invoke()
            }

            override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError?) {
                super.onAdFailedToLoad(adUnit, adType, error)
                timer?.cancel()
                onNextActionWhenResume(nextAction)
            }

            override fun onAdFailedToShowFullScreenContent(adUnit: String, adType: AdType) {
                super.onAdFailedToShowFullScreenContent(adUnit, adType)
                timer?.cancel()
                onNextActionWhenResume(nextAction)
            }

            override fun onAdDismissedFullScreenContent(adUnit: String, adType: AdType) {
                super.onAdDismissedFullScreenContent(adUnit, adType)
                timer?.cancel()
                onNextActionWhenResume(nextAction)
            }
        }

        // var adOpenAd: AppOpenAd? = null

        AdmobOpen.load(
            adUnitId,
            callback,
            onAdLoaded = {
                if (!AdsSDK.isEnableOpenAds) {
                    timer?.cancel()
                    onAdLoaded.invoke()
                    nextAction.invoke()
                    return@load
                }

                timer?.cancel()
                onNextActionWhenResume {
                    AdsSDK.getAppCompatActivityOnTop()?.waitActivityResumed {
                        AdmobOpen.show(it, callback)
                    }
                }
            },
        )

        /* timer?.cancel()
         timer = object : CountDownTimer(timeout, 200) {
             override fun onTick(millisUntilFinished: Long) {
                 if (!AdsSDK.isEnableOpenAds) {
                     timer?.cancel()
                     onAdLoaded.invoke()
                     nextAction.invoke()
                     return
                 }

                 adOpenAd?.let { appOpenAd ->
                     timer?.cancel()
                     onNextActionWhenResume {
                         AdsSDK.getAppCompatActivityOnTop()?.waitActivityResumed {
                             AdmobOpen.show(appOpenAd, callback)
                         }
                     }
                 }
             }

             override fun onFinish() {
                 timer?.cancel()
                 onNextActionWhenResume(nextAction)
             }
         }.start()*/
    }
}
