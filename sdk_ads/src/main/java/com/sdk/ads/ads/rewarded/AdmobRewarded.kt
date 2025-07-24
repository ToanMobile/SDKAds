package com.sdk.ads.ads.rewarded

import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ui.dialogs.DialogShowLoadingAds
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.getActivityOnTop
import com.sdk.ads.utils.getAppCompatActivityOnTop
import com.sdk.ads.utils.getClazzOnTop
import com.sdk.ads.utils.getPaidTrackingBundle
import com.sdk.ads.utils.logger
import com.sdk.ads.utils.waitActivityResumed

object AdmobRewarded {

    /**
     * @param activity: Show on this activity
     * @param adUnitId: adUnitId
     * @param callBack
     * @param onUserEarnedReward
     * @param onFailureUserNotEarn
     */
    fun show(
        adUnitId: String,
        isShowDefaultLoadingDialog: Boolean = true,
        callBack: TAdCallback? = null,
        onFailureUserNotEarn: () -> Unit = {},
        onUserEarnedReward: () -> Unit,
    ) {
        if (!AdsSDK.isEnableRewarded) {
            onUserEarnedReward.invoke()
            return
        }
        val activity = AdsSDK.getAppCompatActivityOnTop()
        activity ?: return
        val clazzOnTop = AdsSDK.getClazzOnTop()
        val adActivityOnTop = AdsSDK.getActivityOnTop() is AdActivity
        val containClazzOnTop = AdsSDK.clazzIgnoreAdResume.contains(AdsSDK.getClazzOnTop())
        if (clazzOnTop == null || containClazzOnTop || adActivityOnTop) {
            return
        }
        var dialog: DialogShowLoadingAds? = null

        if (isShowDefaultLoadingDialog) {
            dialog = DialogShowLoadingAds(activity).apply { show() }
        }

        RewardedAd.load(
            AdsSDK.app,
            adUnitId,
            AdsSDK.defaultAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    logger("ThoNH-1", "onAdFailedToLoad")
                    super.onAdFailedToLoad(error)
                    AdsSDK.adCallback.onAdFailedToLoad(adUnitId, AdType.Rewarded, error)
                    callBack?.onAdFailedToLoad(adUnitId, AdType.Rewarded, error)
                    onFailureUserNotEarn.invoke()
                    dialog?.dismiss()
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    logger("ThoNH-1", "onAdLoaded")
                    super.onAdLoaded(rewardedAd)
                    AdsSDK.adCallback.onAdLoaded(adUnitId, AdType.Rewarded)
                    callBack?.onAdLoaded(adUnitId, AdType.Rewarded)

                    rewardedAd.setOnPaidEventListener { adValue ->
                        val bundle = getPaidTrackingBundle(adValue, adUnitId, "Rewarded", rewardedAd.responseInfo)
                        AdsSDK.adCallback.onPaidValueListener(bundle)
                        callBack?.onPaidValueListener(bundle)
                    }

                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            logger("ThoNH-1", "onAdClicked")
                            super.onAdClicked()
                            AdsSDK.adCallback.onAdClicked(adUnitId, AdType.Rewarded)
                            callBack?.onAdClicked(adUnitId, AdType.Rewarded)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            logger("ThoNH-1", "onAdDismissedFullScreenContent")
                            super.onAdDismissedFullScreenContent()
                            AdsSDK.adCallback.onAdDismissedFullScreenContent(adUnitId, AdType.Rewarded)
                            callBack?.onAdDismissedFullScreenContent(adUnitId, AdType.Rewarded)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            logger("ThoNH-1", "onAdFailedToShowFullScreenContent")
                            super.onAdFailedToShowFullScreenContent(error)
                            AdsSDK.adCallback.onAdFailedToShowFullScreenContent(adUnitId, AdType.Rewarded)
                            callBack?.onAdFailedToShowFullScreenContent(adUnitId, AdType.Rewarded)
                            onFailureUserNotEarn.invoke()
                        }

                        override fun onAdImpression() {
                            logger("ThoNH-1", "onAdImpression")
                            super.onAdImpression()
                            AdsSDK.adCallback.onAdImpression(adUnitId, AdType.Rewarded)
                            callBack?.onAdImpression(adUnitId, AdType.Rewarded)
                        }

                        override fun onAdShowedFullScreenContent() {
                            logger("ThoNH-1", "onAdShowedFullScreenContent")
                            super.onAdShowedFullScreenContent()
                            AdsSDK.adCallback.onAdShowedFullScreenContent(adUnitId, AdType.Rewarded)
                            callBack?.onAdShowedFullScreenContent(adUnitId, AdType.Rewarded)
                        }
                    }

                    activity.waitActivityResumed {
                        dialog?.dismiss()
                        rewardedAd.show(activity) { _ ->
                            logger("ThoNH-1", "onUserEarnedReward")
                            onUserEarnedReward.invoke()
                        }
                    }
                }
            },
        )
    }
}
