package com.sdk.ads.ads.open

import com.sdk.ads.ui.dialogs.DialogBackgroundOpenApp
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.appopen.AppOpenAd
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.getActivityOnTop
import com.sdk.ads.utils.getClazzOnTop

object AdmobOpenResume {

    internal lateinit var adUnitId: String

    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenAdShowing = false
    private var isAppOpenAdLoading = false

    fun load(id: String, callback: TAdCallback? = null) {
        adUnitId = id
        isAppOpenAdLoading = true
        AdmobOpen.load(
            adUnitId,
            callback,
            onAdLoadFailure = {
                isAppOpenAdLoading = false
            },
            onAdLoaded = {
                isAppOpenAdLoading = false
                appOpenAd = it
            },
        )
    }

    internal fun onOpenAdAppResume() {
        if (!AdsSDK.isEnableOpenAds) {
            return
        }

        if (isAppOpenAdShowing) {
            return
        }

        if (!AdmobOpenResume::adUnitId.isInitialized) {
            return
        }

        if (appOpenAd == null && !isAppOpenAdLoading) {
            AdmobOpen.load(adUnitId)
            return
        }

        val activity = AdsSDK.getActivityOnTop()

        activity ?: return

        val clazzOnTop = AdsSDK.getClazzOnTop()
        val adActivityOnTop = AdsSDK.getActivityOnTop() is AdActivity
        val containClazzOnTop = AdsSDK.clazzIgnoreAdResume.contains(AdsSDK.getClazzOnTop())
        if (clazzOnTop == null || containClazzOnTop || adActivityOnTop) {
            return
        }

        appOpenAd?.let { appOpenAd ->
            val dialog = DialogBackgroundOpenApp(AdsSDK.getActivityOnTop()!!)
            if (!dialog.isShowing) {
                dialog.show()
                AdmobOpen.show(
                    appOpenAd,
                    callback = object : TAdCallback {

                        override fun onAdImpression(adUnit: String, adType: AdType) {
                            super.onAdImpression(adUnit, adType)
                            isAppOpenAdShowing = true
                        }

                        override fun onAdFailedToShowFullScreenContent(
                            adUnit: String,
                            adType: AdType,
                        ) {
                            super.onAdFailedToShowFullScreenContent(adUnit, adType)
                            isAppOpenAdShowing = false
                            AdmobOpenResume.appOpenAd = null
                            isAppOpenAdLoading = false
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                        }

                        override fun onAdDismissedFullScreenContent(
                            adUnit: String,
                            adType: AdType,
                        ) {
                            super.onAdDismissedFullScreenContent(adUnit, adType)
                            isAppOpenAdShowing = false
                            AdmobOpenResume.appOpenAd = null
                            isAppOpenAdLoading = false
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }

                            load(adUnitId)
                        }
                    },
                )
            }
        }
    }
}
