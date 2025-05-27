package com.sdk.ads.consent

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ads.AppType
import com.sdk.ads.utils.logEvent

class ConsentTracker(val context: Context) {
    private var isShowForceAgain = false
    private var isAcceptAll = false
    private var language = ""
    private val TAG = "ConsentTracker"

    val getIsAcceptAll get() = isAcceptAll

    fun updateState(isShowForceAgain: Boolean, language: String) {
        this.isShowForceAgain = isShowForceAgain
        this.language = language
    }

    fun isUserConsentValid(isTracking: Boolean = false): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isGdpr = isGDPR(prefs)
        val canShowPersonAds = canShowPersonalizedAds(prefs)
        val canShowAds = canShowAds(prefs)
        val consentValidity = if (!isGdpr) {
            true
        } else canShowPersonAds || canShowAds
        if (isTracking) {
            sendLogTracking(prefs, isGdpr, canShowPersonAds, canShowAds)
        }
        Log.d(TAG, "isUserConsentValid: $consentValidity isTracking:$isTracking, GDPR: $isGdpr, PersonAds: $canShowPersonAds, Ads: $canShowAds")
        return consentValidity
    }

    fun isRequestAdsFail(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isGdpr = isGDPR(prefs)
        val canShowPersonAds = canShowPersonalizedAds(prefs)
        val canShowAds = canShowAds(prefs)
        return (!canShowAds && !canShowPersonAds) && isGdpr
    }

    private fun isGDPR(prefs: SharedPreferences): Boolean {
        val gdpr = prefs.getInt("IABTCF_gdprApplies", 0)
        return gdpr == 1
    }

    private fun canShowAds(prefs: SharedPreferences): Boolean {
        //https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#in-app-details
        //https://support.google.com/admob/answer/9760862?hl=en&ref_topic=9756841

        val purposeConsent = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
        val vendorConsent = prefs.getString("IABTCF_VendorConsents", "") ?: ""
        val vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests", "") ?: ""
        val purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests", "") ?: ""

        val googleId = 755
        val hasGoogleVendorConsent = hasAttribute(vendorConsent, index = googleId)
        val hasGoogleVendorLI = hasAttribute(vendorLI, index = googleId)

        // Minimum required for at least non-personalized ads
        return hasConsentFor(listOf(1), purposeConsent, hasGoogleVendorConsent) && hasConsentOrLegitimateInterestFor(
            listOf(2, 7, 9, 10),
            purposeConsent,
            purposeLI,
            hasGoogleVendorConsent,
            hasGoogleVendorLI
        )
    }

    private fun canShowPersonalizedAds(prefs: SharedPreferences): Boolean {
        //https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#in-app-details
        //https://support.google.com/admob/answer/9760862?hl=en&ref_topic=9756841

        val purposeConsent = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
        val vendorConsent = prefs.getString("IABTCF_VendorConsents", "") ?: ""
        val vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests", "") ?: ""
        val purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests", "") ?: ""

        val googleId = 755
        val hasGoogleVendorConsent = hasAttribute(vendorConsent, index = googleId)
        val hasGoogleVendorLI = hasAttribute(vendorLI, index = googleId)

        return hasConsentFor(listOf(1, 3, 4), purposeConsent, hasGoogleVendorConsent) && hasConsentOrLegitimateInterestFor(
            listOf(2, 7, 9, 10),
            purposeConsent,
            purposeLI,
            hasGoogleVendorConsent,
            hasGoogleVendorLI
        )
    }

    // Check if a binary string has a "1" at position "index" (1-based)
    private fun hasAttribute(input: String, index: Int): Boolean {
        return input.length >= index && input[index - 1] == '1'
    }

    // Check if consent is given for a list of purposes
    private fun hasConsentFor(purposes: List<Int>, purposeConsent: String, hasVendorConsent: Boolean): Boolean {
        return purposes.all { p -> hasAttribute(purposeConsent, p) } && hasVendorConsent
    }

    // Check if a vendor either has consent or legitimate interest for a list of purposes
    private fun hasConsentOrLegitimateInterestFor(purposes: List<Int>, purposeConsent: String, purposeLI: String, hasVendorConsent: Boolean, hasVendorLI: Boolean): Boolean {
        return purposes.all { p ->
            (hasAttribute(purposeLI, p) && hasVendorLI) || (hasAttribute(purposeConsent, p) && hasVendorConsent)
        }
    }

    private fun sendLogTracking(prefs: SharedPreferences, isGdpr: Boolean, canShowPersonAds: Boolean, canShowAds: Boolean) {
        val purposeConsent = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
        val purposeLegitimate = prefs.getString("IABTCF_PurposeLegitimateInterests", "") ?: ""
        Log.e(TAG, "sendLogTracking: $purposeConsent")
        val consentStatus = when {
            (isGdpr && canShowPersonAds && canShowAds) || (purposeConsent.all { it == '1' }) -> {
                isAcceptAll = true
                "GDPR_acceptAll"
            }

            purposeConsent.all { it == '0' } -> "GDPR_denyAll"
            else -> "GDPR_acceptAPart"
        }
        Log.e(TAG, "consentStatus: $consentStatus")
        logEvent(eventName = consentStatus)
        logEvent(eventName = "${consentStatus}_$language")
        if (consentStatus == "GDPR_acceptAPart") {
            if (AdsSDK.appType == AppType.PDF) {
                logEvent(eventName = "GDPR_accept_${language}_$purposeConsent")
                logEvent(eventName = "GDPR_accept${purposeConsent}")
            } else {
                logEvent(eventName = "GDPR_accept_${language}_${purposeConsent}")
            }
        }
    }
}