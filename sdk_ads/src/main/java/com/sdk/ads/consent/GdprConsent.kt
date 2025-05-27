package com.sdk.ads.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ads.AppType
import com.sdk.ads.utils.logEvent

class GdprConsent(val context: Context, private val language: String) {
    @Suppress("PrivatePropertyName")
    private val TAG = "GdprConsent"
    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)
    private var consentForm: ConsentForm? = null
    private var isShowGDPR = false
    private var isFormLoading = false

    /**IN PRODUCTION CALL AT ONCREATE FOR CONSENT FORM CHECK*/
    fun updateConsentInfo(
        activity: Activity,
        underAge: Boolean,
        consentTracker: ConsentTracker,
        isShowForceAgain: Boolean = false,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit,
        callBackFormError: (FormError?) -> Unit
    ) {
        isShowGDPR = false
        val params = ConsentRequestParameters
            .Builder()
            .setTagForUnderAgeOfConsent(underAge)
            .build()
        requestConsentInfoUpdate(
            activity = activity,
            params = params,
            consentPermit = consentPermit,
            isShowForceAgain = isShowForceAgain,
            consentTracker = consentTracker,
            initAds = { initAds() },
            callBackFormError = callBackFormError,
        )
    }

    /**ONLY TO DEBUG EU & NONE EU GEOGRAPHICS
     * EU: ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
     * NOT EU: ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA
     * DISABLED: ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED
     * requestConsentInfoUpdate() logs the hashed id when run*/
    fun updateConsentInfoWithDebugGeoGraphics(
        activity: Activity,
        geoGraph: Int = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA,
        consentTracker: ConsentTracker,
        isShowForceAgain: Boolean = false,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit,
        hashDeviceIdTest: List<String>?,
        callBackFormError: (FormError?) -> Unit
    ) {
        val debugSetting = ConsentDebugSettings.Builder(context)
            .setDebugGeography(geoGraph)
            .addTestDeviceHashedId(AdRequest.DEVICE_ID_EMULATOR)
        hashDeviceIdTest?.forEach {
            debugSetting.addTestDeviceHashedId(it)
        }
        val debugSettings = debugSetting.build()
        val params = ConsentRequestParameters
            .Builder()
            .setConsentDebugSettings(debugSettings)
            .build()
        requestConsentInfoUpdate(
            activity = activity,
            params = params,
            consentTracker = consentTracker,
            isShowForceAgain = isShowForceAgain,
            consentPermit = consentPermit,
            initAds = { initAds() },
            callBackFormError = callBackFormError
        )
    }

    private fun requestConsentInfoUpdate(
        activity: Activity,
        params: ConsentRequestParameters,
        consentTracker: ConsentTracker,
        isShowForceAgain: Boolean = false,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit,
        callBackFormError: (FormError?) -> Unit
    ) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { // The consent information state was updated, ready to check if a form is available.
                if (consentInformation.isConsentFormAvailable) {
                    loadForm(activity, consentTracker, isShowForceAgain, consentPermit, initAds = { initAds() }, callBackFormError = callBackFormError)
                } else {
                    consentPermit(isConsentObtained(consentTracker))
                }
            },
            { formError ->
                initAds()
                Log.e(TAG, "requestConsentInfoUpdate:formError: ${formError.message}")
            }
        )
    }

    private fun loadForm(
        activity: Activity,
        consentTracker: ConsentTracker,
        isShowForceAgain: Boolean = false,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit,
        callBackFormError: (FormError?) -> Unit
    ) {
        if (isFormLoading) {
            Log.e(TAG, "loadForm already in progress")
            return
        }
        // Nếu form đã được tải, show lại luôn (hoặc return tùy bạn)
        if (consentForm != null) {
            Log.e(TAG, "consentForm already loaded")
            consentPermit(isConsentObtained(consentTracker))
            initAds()
            return
        }
        isFormLoading = true
        Log.e(TAG, "requestConsentInfoUpdate:loadConsentForm")
        // Loads a consent form. Must be called on the main thread.
        UserMessagingPlatform.loadConsentForm(
            context,
            { consentFormShow ->
                isFormLoading = false
                consentForm = consentFormShow
                Log.e(TAG, "consentForm is required to show" + consentInformation.consentStatus.toString())
                when (consentInformation.consentStatus) {
                    ConsentInformation.ConsentStatus.REQUIRED -> {
                        isShowGDPR = true
                        Log.e(TAG, "consentForm is required to show:::${consentForm}")
                        if (AdsSDK.appType == AppType.PDF) {
                            logEvent(eventName = "GDPR_showFormGDPR_$language")
                            logEvent(eventName = "GDPR_showFormGDPR")
                        } else {
                            logEvent(eventName = "GDPR_showFormGDPR_$language")
                            logEvent(eventName = "GDPR_showForm")
                        }
                        consentForm?.show(
                            activity,
                        ) { formError ->
                            // Log error
                            if (formError != null) {
                                Log.e(TAG, "consentForm show ${formError.message}")
                                if (AdsSDK.appType == AppType.PDF) {
                                    logEvent(eventName = "GDPR_formError_${formError.errorCode}_${formError.message}")
                                    logEvent(eventName = "GDPR_formError_${formError.errorCode}_${formError.message}_$language")
                                } else {
                                    logEvent(eventName = "GDPR_formError_${formError.errorCode}_${formError.message}_$language")
                                }
                                callBackFormError(formError)
                            }
                            // App can start requesting ads.
                            if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                                Log.e(TAG, "consentForm is Obtained")
                                consentPermit(isConsentObtained(consentTracker, isTracking = true))
                                initAds()
                            }
                            Log.e(TAG, "consentForm is required to show${consentForm}")
                            // Handle dismissal by reloading form.
                            loadForm(activity, consentTracker, isShowForceAgain, consentPermit, initAds, callBackFormError = callBackFormError)
                        }
                    }

                    else -> {
                        consentPermit(isConsentObtained(consentTracker))
                    }
                }
            },
            { formError ->
                isFormLoading = false
                Log.e(TAG, "loadForm Failure: ${formError.message}")
                if (AdsSDK.appType == AppType.PDF) {
                    logEvent(eventName = "GDPR_formError_${formError.errorCode}_${formError.message}")
                    logEvent(eventName = "GDPR_formError_${formError.errorCode}_${formError.message}_$language")
                } else {
                    logEvent(eventName = "GDPR_formError_${formError.errorCode}_${formError.message}_$language")
                }
                callBackFormError(formError)
            },
        )
    }

    fun reUseExistingConsentForm(
        activity: Activity,
        consentTracker: ConsentTracker,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit,
        callBackFormError: (FormError?) -> Unit
    ) {
        resetConsent()
        if (consentInformation.isConsentFormAvailable) {
            isShowGDPR = true
            Log.e(TAG, "reUseExistingConsentForm$consentForm")
            if (AdsSDK.appType == AppType.PDF) {
                logEvent(eventName = "GDPR_showFormGDPR_$language")
                logEvent(eventName = "GDPR_showFormGDPR")
            } else {
                logEvent(eventName = "GDPR_showFormGDPR_$language")
                logEvent(eventName = "GDPR_showForm")
            }
            consentForm?.show(
                activity,
            ) { formError ->
                // Log error
                if (formError != null) {
                    Log.e(TAG, "consentForm formError ${formError.message}")
                }
                // App can start requesting ads.
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                    Log.d(TAG, "consentForm is Obtained")
                    consentPermit(isConsentObtained(consentTracker))
                    initAds()
                }
                // Handle dismissal by reloading form.
                loadForm(activity, consentTracker, true, consentPermit, initAds, callBackFormError = callBackFormError)
            }
        } else {
            Log.e(TAG, "Consent form not available, check internet connection.")
            consentPermit(isConsentObtained(consentTracker))
        }
    }

    /**RETURNS TRUE IF EU/UK IS TRULY OBTAINED OR NOT REQUIRED ELSE FALSE*/
    private fun isConsentObtained(consentTracker: ConsentTracker, isTracking: Boolean = false): Boolean {
        val isSendTracking = isShowGDPR && isTracking
        val obtained = consentTracker.isUserConsentValid(isSendTracking) && consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED
        val notRequired = consentInformation.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
        val isObtained = obtained || notRequired
        Log.e(TAG, "isConsentObtained or not required: $isObtained")
        return isObtained
    }

    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    /**RESET ONLY IF TRULY REQUIRED. E.G FOR TESTING OR USER WANTS TO RESET CONSENT SETTINGS*/
    fun resetConsent() {
        consentInformation.reset()
    }
}