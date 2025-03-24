package com.sdk.ads.utils

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ParametersBuilder
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.sdk.ads.ads.AdsSDK

private val tracker get() = Firebase.analytics

fun logAdClicked(adType: AdType, adID: String? = null) {
    if (!AdsSDK.isEnableAds || !AdsSDK.isEnableTracking) return
    logParams("ad_click_custom") {
        val clazz = AdsSDK.getClazzOnTop()

        if (clazz != null) {
            runCatching { param("screen", clazz::class.java.simpleName) }

            val adFormat = when (adType) {
                AdType.OpenApp -> {
                    if (adID != null && adID == com.sdk.ads.ads.open.AdmobOpenResume.adUnitId) {
                        "ad_open_ads_resume"
                    } else {
                        "ad_open_ads"
                    }
                }

                AdType.Inter -> "ad_interstitial"
                AdType.Banner -> "ad_banner"
                AdType.Native -> "ad_native"
                AdType.Rewarded -> "ad_rewarded"
            }

            runCatching { param("ad_format", adFormat) }
        }
    }
}

fun logAdImpression(adTag: String) {
    if (!AdsSDK.isEnableAds || !AdsSDK.isEnableTracking) return
    AdsSDK.getClazzOnTop()?.let {
        logParams(adTag + "_impression") {
            param("screen", "$it")
        }
    }
}

fun logEvent(evenName: String) {
    if (!AdsSDK.isEnableAds || !AdsSDK.isEnableTracking) return
    var result = removeEmojis(evenName.trim())
        .replace("\\s+".toRegex(), "_")
        .replace("[^a-zA-Z0-9_\\p{IsArabic}]+".toRegex(), "_")
    if (result.length > 40) {
        result = result.substring(0, 40)
    }
    Log.e("Tracking:::", result)
    tracker.logEvent(result, null)
}

fun logProperty(evenName: String, data: String?) {
    if (!AdsSDK.isEnableAds || !AdsSDK.isEnableTracking) return
    val result = evenName.trim().replace("-", "_")
    Log.e("logProperty::", evenName)
    tracker.setUserProperty(result, data)
}

fun logScreen(screenName: String) {
    if (!AdsSDK.isEnableAds || !AdsSDK.isEnableTracking) return
    val result = screenName.trim().replace("-", "_")
    Log.e("logScreen::", result)
    tracker.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
        param(FirebaseAnalytics.Param.SCREEN_NAME, result)
    }
}

fun logParams(eventName: String, block: ParametersBuilder.() -> Unit) {
    if (!AdsSDK.isEnableAds || !AdsSDK.isEnableTracking) return
    runCatching {
        val result = eventName.trim().replace("-", "_")
        tracker.logEvent(result) { block() }
    }
}

fun logNote(eventName: String, noteTitle: String, note: String) {
    if (!AdsSDK.isEnableAds || !AdsSDK.isEnableTracking) return
    logParams(eventName) {
        param(noteTitle, note)
    }
}

private fun removeEmojis(text: String): String {
    val emojiRegex = "[\\p{So}\\p{Cn}]".toRegex()  // Unicode Symbol & Private Use Area
    return text.replace(emojiRegex, "")
}