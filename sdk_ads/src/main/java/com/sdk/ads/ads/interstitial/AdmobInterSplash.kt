package com.sdk.ads.ads.interstitial

import android.Manifest
import android.os.CountDownTimer
import androidx.annotation.RequiresPermission
import com.google.android.gms.ads.LoadAdError
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ads.interstitial.AdmobInter.showLoadingBeforeInter
import com.sdk.ads.ui.dialogs.DialogShowLoadingAds
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.getAppCompatActivityOnTop
import com.sdk.ads.utils.logger
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
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun show(
        adUnitId: String,
        isForceShowNow: Boolean = false, // Show liền ads
        isShowLoading: Boolean = false, // Show loading ads
        isDelayNextAds: Boolean = true, // Time delay ads
        autoNextActionDuringInterShow: Boolean = true,
        delayTimeToActionAfterShowInter: Int = 300,
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
            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onAdLoaded(adUnit: String, adType: AdType) {
                super.onAdLoaded(adUnit, adType)
                adLoaded()
                AdmobInter.dismissLoading(dialogShowLoadingAds)
                if (isForceShowNow) {
                    showAds(adUnitId, isDelayNextAds, autoNextActionDuringInterShow, delayTimeToActionAfterShowInter, this, nextAction)
                }
            }

            override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError?) {
                super.onAdFailedToLoad(adUnit, adType, error)
                AdmobInter.dismissLoading(dialogShowLoadingAds)
                logger("AdmobInterSplash::onAdFailedToLoad, error:$error")
                timer?.cancel()
                onNextActionWhenResume(nextAction)
            }

            override fun onAdFailedToShowFullScreenContent(adUnit: String, adType: AdType) {
                super.onAdFailedToShowFullScreenContent(adUnit, adType)
                logger("AdmobInterSplash::onAdFailedToShowFullScreenContent")
                AdmobInter.dismissLoading(dialogShowLoadingAds)
                timer?.cancel()
                onNextActionWhenResume(nextAction)
            }
        }

        AdmobInter.load(adUnitId, callback)
        if (!isForceShowNow) {
            timer?.cancel()
            timer = object : CountDownTimer(timeout, 1000) {
                @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                override fun onTick(millisUntilFinished: Long) {
                    showAds(adUnitId, isDelayNextAds, autoNextActionDuringInterShow, delayTimeToActionAfterShowInter, callback, nextAction)
                }

                override fun onFinish() {
                    timer?.cancel()
                    onNextActionWhenResume(nextAction)
                }
            }.start()
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun showAds(
        adUnitId: String,
        isDelayNextAds: Boolean = true,
        autoNextActionDuringInterShow: Boolean,
        delayTimeToActionAfterShowInter: Int,
        callback: TAdCallback,
        nextAction: () -> Unit,
    ) {
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
                            forceShow = true,
                            loadAfterDismiss = false,
                            loadIfNotAvailable = false,
                            autoNextActionDuringInterShow = autoNextActionDuringInterShow,
                            delayTimeToActionAfterShowInter = delayTimeToActionAfterShowInter,
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
