package com.sdk.ads.ads.interstitial

import com.sdk.ads.utils.getActivityOnTop
import com.sdk.ads.utils.getAppCompatActivityOnTop
import com.sdk.ads.utils.getClazzOnTop
import com.admob.ui.dialogs.DialogWelcomeBackAds
import com.sdk.ads.utils.waitActivityResumed
import com.sdk.ads.utils.waitActivityStop
import com.google.android.gms.ads.AdActivity
import com.sdk.ads.ads.AdsSDK

object AdmobInterResume {

    private lateinit var adUnitId: String

    fun load(id: String) {
        adUnitId = id
        AdmobInter.load(adUnitId, null)
    }

    internal fun onInterAppResume(nextAction: () -> Unit = {}) {
        if (!AdsSDK.isEnableInter) {
            nextAction.invoke()
            return
        }

        if (!AdmobInterResume::adUnitId.isInitialized) {
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

        if (!AdmobInter.checkShowInterCondition(adUnitId, true)) {
            return
        }

        val dialog = DialogWelcomeBackAds(activity) {
            if (AdmobInter.checkShowInterCondition(adUnitId, true)) {
                AdmobInter.show(
                    adUnitId = adUnitId,
                    showLoadingInter = false,
                    forceShow = true,
                    loadAfterDismiss = true,
                    loadIfNotAvailable = true,
                    callback = null,
                    nextAction = nextAction,
                )
            }
        }

        activity.waitActivityStop {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }

        activity.waitActivityResumed {
            dialog.show()
        }
    }
}
