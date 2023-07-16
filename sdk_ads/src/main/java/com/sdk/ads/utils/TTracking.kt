package com.sdk.ads.utils

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.ParametersBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ads.open.AdmobOpenResume

private val tracker get() = Firebase.analytics

// Todo bốc ra ngoài app, ko để trong module
fun logAdClicked(adType: AdType, adID: String? = null) {
    logParams("ad_click_custom") {
        val clazz = com.sdk.ads.ads.AdsSDK.getClazzOnTop()

        if (clazz != null) {
            runCatching { param("screen", clazz::class.java.simpleName) }

            val adFormat = when (adType) {
                com.sdk.ads.utils.AdType.OpenApp -> {
                    if (adID != null && adID == com.sdk.ads.ads.open.AdmobOpenResume.adUnitId) {
                        "ad_open_ads_resume"
                    } else {
                        "ad_open_ads"
                    }
                }
                com.sdk.ads.utils.AdType.Inter -> "ad_interstitial"
                com.sdk.ads.utils.AdType.Banner -> "ad_banner"
                com.sdk.ads.utils.AdType.Native -> "ad_native"
                com.sdk.ads.utils.AdType.Rewarded -> "ad_rewarded"
            }

            runCatching { param("ad_format", adFormat) }
        }
    }
}

fun logAdImpression(adTag: String) {
    AdsSDK.getClazzOnTop()?.let {
        logParams(adTag + "_impression") {
            param("screen", "$it")
        }
    }
}

fun logEvent(evenName: String) {
    val result = evenName.trim().replace("-", "_")
    Log.e("logEvent::", evenName)
    tracker.logEvent(result, null)
}

fun logScreen(screenName: String) {
    val result = screenName.trim().replace("-", "_")
    Log.e("logScreen::", result)
    tracker.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
        param(FirebaseAnalytics.Param.SCREEN_NAME, result)
    }
}

fun logParams(eventName: String, block: ParametersBuilder.() -> Unit) {
    runCatching {
        val result = eventName.trim().replace("-", "_")
        tracker.logEvent(result) { block() }
    }
}
