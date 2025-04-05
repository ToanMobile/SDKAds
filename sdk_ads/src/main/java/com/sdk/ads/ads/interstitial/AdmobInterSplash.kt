package com.sdk.ads.ads.interstitial

import android.os.CountDownTimer
import android.util.Log
import com.google.android.gms.ads.LoadAdError
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ads.interstitial.AdmobInter.dismissLoading
import com.sdk.ads.ads.interstitial.AdmobInter.showLoadingBeforeInter
import com.sdk.ads.ui.dialogs.DialogShowLoadingAds
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.getAppCompatActivityOnTop
import com.sdk.ads.utils.onNextActionWhenResume
import com.sdk.ads.utils.waitActivityResumed

object AdmobInterSplash {

    private var timer: CountDownTimer? = null

    fun cancelAds() {
        timer?.cancel()
    }

    fun checkAdsShow(adUnitId: String): Boolean = AdmobInter.checkAdsShow(adUnitId = adUnitId)

    /**
     * @param adUnitId: adUnit
     * @param timeout: timeout to wait ad show
     * @param nextAction
     */
    fun show(
        adUnitId: String,
        isForceShowNow: Boolean = false,
        isShowLoading: Boolean = false,
        isDelayNextAds: Boolean = true,
        timeout: Long = 30000,
        nextAction: () -> Unit,
        adLoaded: () -> Unit = {},
    ) {
        var dialogShowLoadingAds: DialogShowLoadingAds? = null
        if (!AdsSDK.isEnableInter) {
            nextAction.invoke()
            return
        }
        if (isForceShowNow && isShowLoading) {
            dialogShowLoadingAds = showLoadingBeforeInter()
        }
        val callback = object : TAdCallback {
            override fun onAdLoaded(adUnit: String, adType: AdType) {
                super.onAdLoaded(adUnit, adType)
                adLoaded()
                dismissLoading(dialogShowLoadingAds)
                if (isForceShowNow) {
                    showAds(adUnitId, isShowLoading, isDelayNextAds, this, nextAction)
                }
            }

            override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError?) {
                super.onAdFailedToLoad(adUnit, adType, error)
                Log.e("onAdFailedToLoad::", "error:$error")
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
            Log.d("AdmobInterSplash:", AdmobInter.checkShowInterCondition(adUnitId, !isDelayNextAds).toString())
            if (AdmobInter.checkShowInterCondition(adUnitId, !isDelayNextAds)) {
                timer?.cancel()
                onNextActionWhenResume {
                    AdsSDK.getAppCompatActivityOnTop()?.waitActivityResumed {
                        Log.d("AdmobInterSplash:", "waitActivityResumed")
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
