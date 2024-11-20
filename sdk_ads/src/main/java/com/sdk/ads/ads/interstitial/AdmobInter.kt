package com.sdk.ads.ads.interstitial

import android.app.Activity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.zzc
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ui.dialogs.DialogShowLoadingAds
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.delay
import com.sdk.ads.utils.getActivityOnTop
import com.sdk.ads.utils.getAppCompatActivityOnTop
import com.sdk.ads.utils.getPaidTrackingBundle
import com.sdk.ads.utils.isNetworkAvailable
import com.sdk.ads.utils.waitActivityResumed
import com.sdk.ads.utils.waitActivityStop
import com.sdk.ads.utils.waitingResume

object AdmobInter {

    private val inters = mutableMapOf<String, InterstitialAd?>()
    private val interTimeShown = mutableMapOf<String, Long>()
    private val intersLoading = mutableListOf<String>()
    private var timeShowLoading = 1_000

    private var nextActionDuringInterShow = false
    private var timeToActionAfterShowInter = 300

    /**
     * Config when show InterAd.
     * Usually we will take the next action after Inter dismiss
     * But InterAd's onDismissFullContent is suffering from a callback bug that is several hundred milliseconds late.
     * Therefore, the screen change delay is not happening as expected.
     * This function will fix that error.
     * We will handle nextAction while InterAd starting showing
     * @param handleNextActionDuringInterShow: if set {true} => fix bug delay onDismiss of Inter
     * @param delayTimeToActionAfterShowInter: time to delay start from showInter. Recommend 0 if startActivity , 300 with navigateFragment
     */
    fun setNextWhileInterShowing(
        handleNextActionDuringInterShow: Boolean,
        delayTimeToActionAfterShowInter: Int = 300,
    ) {
        nextActionDuringInterShow = handleNextActionDuringInterShow
        timeToActionAfterShowInter = delayTimeToActionAfterShowInter
    }

    /**
     * Step1: Không có mạng => Không load
     * Step2: Có sẵn quảng cáo => Không load
     * Step3: Đang loading rồi => Không load
     */
    fun load(adUnitId: String, callback: TAdCallback? = null) {
        if (!AdsSDK.isEnableInter) {
            return
        }

        if (!AdsSDK.app.isNetworkAvailable()) {
            callback?.onAdFailedToLoad(adUnitId, AdType.Inter, null)
            return
        }

        if (inters[adUnitId] != null) {
            return
        }

        if (intersLoading.contains(adUnitId)) {
            return
        }

        intersLoading.add(adUnitId)

        InterstitialAd.load(
            AdsSDK.app,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdsSDK.adCallback.onAdFailedToLoad(adUnitId, AdType.Inter, error)
                    callback?.onAdFailedToLoad(adUnitId, AdType.Inter, error)
                    intersLoading.remove(adUnitId)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    inters[adUnitId] = interstitialAd
                    AdsSDK.adCallback.onAdLoaded(adUnitId, AdType.Inter)
                    callback?.onAdLoaded(adUnitId, AdType.Inter)
                    intersLoading.remove(adUnitId)
                }
            },
        )
    }

    fun checkShowInterCondition(adUnitId: String, isForceShow: Boolean): Boolean {
        if (!AdsSDK.app.isNetworkAvailable()) {
            return false
        }

        if (AdsSDK.getActivityOnTop() == null) {
            return false
        }

        if (inters[adUnitId] == null) {
            return false
        }

        if (!checkTimeShowValid(adUnitId, isForceShow)) {
            return false
        }

        return true
    }

    /**
     * @param adUnitId
     * @param showLoadingInter: show DialogLoading 1s before show Inter
     * @param forceShow: try to force show InterAd, ignore interTimeDelayMs config
     * @param loadAfterDismiss: handle new load after InterAd dismiss
     * @param loadIfNotAvailable: try to load if InterAd not available yet
     * @param callback: callback
     * @param nextAction: callback for your work, always call whether the InterAd display is successful or not
     */
    fun show(
        adUnitId: String,
        showLoadingInter: Boolean = true,
        forceShow: Boolean = false,
        loadAfterDismiss: Boolean = true,
        loadIfNotAvailable: Boolean = true,
        callback: TAdCallback? = null,
        nextAction: () -> Unit,
    ) {
        if (!AdsSDK.isEnableInter) {
            nextAction.invoke()
            return
        }

        // Không có mạng => Không show
        if (!AdsSDK.app.isNetworkAvailable()) {
            nextAction.invoke()
            return
        }

        val interAd = inters[adUnitId]
        val currActivity = AdsSDK.getActivityOnTop()

        if (interAd == null) {
            nextAction.invoke()
            if (loadIfNotAvailable && !intersLoading.contains(adUnitId)) {
                load(adUnitId, callback)
            }
            return
        }

        if (!checkTimeShowValid(adUnitId, forceShow)) {
            nextAction.invoke()
            return
        }

        if (currActivity == null) {
            nextAction.invoke()
        } else {
            if (showLoadingInter) {
                showLoadingBeforeInter {
                    invokeShowInter(interAd, currActivity, loadAfterDismiss, callback, nextAction)
                }
            } else {
                invokeShowInter(interAd, currActivity, loadAfterDismiss, callback, nextAction)
            }
        }
    }

    /**
     * Kiểm tra khoảng thời gian giữa 2 lần show, nếu là forceShow hoặc thời gian đó lớn lơn interTimeDelayMs thì là hợp lệ
     *
     */
    private fun checkTimeShowValid(adUnitId: String, isForceShow: Boolean): Boolean {
        if (isForceShow) {
            interTimeShown[adUnitId] = 0
            return true
        }

        val lastTimeShow = interTimeShown[adUnitId] ?: 0

        return System.currentTimeMillis() - lastTimeShow > AdsSDK.interTimeDelayMs
    }

    private fun invokeShowInter(
        interstitialAd: InterstitialAd,
        activity: Activity,
        loadAfterDismiss: Boolean,
        callback: TAdCallback? = null,
        nextAction: () -> Unit,
    ) {
        val adUnitId = interstitialAd.adUnitId
        interstitialAd.setOnPaidEventListener { adValue ->
            val bundle = getPaidTrackingBundle(adValue, adUnitId, "Inter", interstitialAd.responseInfo)
            AdsSDK.adCallback.onPaidValueListener(bundle)
            callback?.onPaidValueListener(bundle)
        }
        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                AdsSDK.adCallback.onAdClicked(adUnitId, AdType.Inter)
                callback?.onAdClicked(adUnitId, AdType.Inter)
            }

            override fun onAdDismissedFullScreenContent() {
                AdsSDK.adCallback.onAdDismissedFullScreenContent(adUnitId, AdType.Inter)
                callback?.onAdDismissedFullScreenContent(adUnitId, AdType.Inter)

                interTimeShown[adUnitId] = System.currentTimeMillis()

                if (!nextActionDuringInterShow) {
                    nextAction.invoke()
                }

                if (loadAfterDismiss) {
                    load(adUnitId, callback)
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdsSDK.adCallback.onAdDismissedFullScreenContent(adUnitId, AdType.Inter)
                callback?.onAdDismissedFullScreenContent(adUnitId, AdType.Inter)
                inters.remove(adUnitId)
                if (loadAfterDismiss) {
                    load(adUnitId, callback)
                }
            }

            override fun onAdImpression() {
                AdsSDK.adCallback.onAdImpression(adUnitId, AdType.Inter)
                callback?.onAdImpression(adUnitId, AdType.Inter)
                inters.remove(adUnitId)
            }

            override fun onAdShowedFullScreenContent() {
                AdsSDK.adCallback.onAdShowedFullScreenContent(adUnitId, AdType.Inter)
                callback?.onAdShowedFullScreenContent(adUnitId, AdType.Inter)
            }
        }

        if (nextActionDuringInterShow) {
            delay(timeToActionAfterShowInter) {
                nextAction.invoke()
            }
        }
        interstitialAd.show(activity)
    }

    /**
     * Show loading trước khi show Inter
     * 1. Nếu activity null => next
     * 2. Nếu activity đang resume => show dialog / trong lúc dialog đang show mà activity bị Stop => ẩn dialog
     * 3. Nếu activity không resume => đợi resume thì next
     * 4. Khi show dialog, chờ sau timeShowLoading(1000ms) thì kiểm tra
     * 5.   Nếu Activity đang resume => ẩn dialog/ next action
     * 6.   Nếu Activity không resume => đợi resume thì ẩn dialog / nextAction
     */
    fun showLoadingBeforeInter(nextAction: () -> Unit) {
        val topActivity = AdsSDK.getAppCompatActivityOnTop()

        if (topActivity == null) {
            nextAction.invoke()
            return
        }

        val dialog = DialogShowLoadingAds(topActivity)

        if (topActivity.lifecycle.currentState == Lifecycle.State.RESUMED) {
            if (!dialog.isShowing) {
                dialog.show()

                topActivity.waitActivityStop {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }
                delay(timeShowLoading) {
                    if (topActivity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                        nextAction.invoke()
                    } else {
                        topActivity.waitActivityResumed {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            nextAction.invoke()
                        }
                    }
                }
            }
        } else {
            topActivity.waitingResume {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                nextAction.invoke()
            }
        }
    }

    fun checkAdsShow(adUnitId: String): Boolean {
        return inters[adUnitId] != null || intersLoading.contains(adUnitId)
    }
}
