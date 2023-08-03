package com.sdk.ads.ads.interstitial

import android.os.CountDownTimer
import com.google.android.gms.ads.LoadAdError
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.getAppCompatActivityOnTop
import com.sdk.ads.utils.onNextActionWhenResume
import com.sdk.ads.utils.waitActivityResumed

object AdmobInterSplash {

    private var timer: CountDownTimer? = null

    /**
     * @param adUnitId: adUnit
     * @param timeout: timeout to wait ad show
     * @param nextAction
     */
    fun show(
        adUnitId: String,
        isForceShowNow: Boolean = true,
        isDelayNextAds: Boolean = true,
        isShowLoading: Boolean = true,
        timeout: Long = 15000,
        nextAction: () -> Unit,
    ) {
        if (!AdsSDK.isEnableInter) {
            nextAction.invoke()
            return
        }

        val callback = object : TAdCallback {
            override fun onAdLoaded(adUnit: String, adType: AdType) {
                super.onAdLoaded(adUnit, adType)
                if (isForceShowNow) {
                    showAds(adUnitId, isShowLoading, isDelayNextAds, this, nextAction)
                }
            }

            override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError) {
                super.onAdFailedToLoad(adUnit, adType, error)
                timer?.cancel()
                onNextActionWhenResume(nextAction)
            }

            override fun onAdFailedToShowFullScreenContent(adUnit: String, adType: AdType) {
                super.onAdFailedToShowFullScreenContent(adUnit, adType)
                timer?.cancel()
                onNextActionWhenResume(nextAction)
            }
        }

        AdmobInter.load(adUnitId, callback)
        if (!isForceShowNow) {
            timer?.cancel()
            timer = object : CountDownTimer(timeout, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    showAds(adUnitId, isShowLoading, isDelayNextAds, callback, nextAction)
                }

                override fun onFinish() {
                    timer?.cancel()
                    onNextActionWhenResume(nextAction)
                }
            }.start()
        }
    }

    private fun showAds(adUnitId: String, isShowLoading: Boolean = false, isDelayNextAds: Boolean = true, callback: TAdCallback, nextAction: () -> Unit) {
        try {
            if (!AdsSDK.isEnableInter) {
                timer?.cancel()
                nextAction.invoke()
                return
            }
            if (AdmobInter.checkShowInterCondition(adUnitId, !isDelayNextAds)) {
                timer?.cancel()
                onNextActionWhenResume {
                    AdsSDK.getAppCompatActivityOnTop()?.waitActivityResumed {
                        AdmobInter.show(
                            adUnitId = adUnitId,
                            showLoadingInter = isShowLoading,
                            forceShow = true,
                            loadAfterDismiss = false,
                            loadIfNotAvailable = false,
                            callback = callback,
                            nextAction = nextAction,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            timer?.cancel()
            onNextActionWhenResume(nextAction)
        }
    }
}
