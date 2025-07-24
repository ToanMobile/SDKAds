package com.sdk.ads.consent

import android.app.Activity
import android.content.Context
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
import com.sdk.ads.utils.logger

class GdprConsent(val context: Context, private val language: String) {
    @Suppress("PrivatePropertyName")
    private val TAG = "GdprConsent"
    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)
    private var consentForm: ConsentForm? = null
    private var isShowGDPR = false

    companion object {
        private var hasShownConsentForm: Boolean = false
    }

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
                    logger("loadForm:::isConsentFormAvailable", TAG)
                    if (AdsSDK.appType == AppType.PDF) {
                        logEvent(eventName = "GDPR_formAvailable")
                    }
                    loadForm(activity, consentTracker, isShowForceAgain, consentPermit, initAds = { initAds() }, callBackFormError = callBackFormError)
                } else {
                    consentPermit(isConsentObtained(consentTracker))
                }
            },
            { formError ->
                initAds()
                logger("requestConsentInfoUpdate:formError: ${formError.message}", TAG)
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
        if (hasShownConsentForm || !activity.hasWindowFocus()) {
            logger("Consent form has already been shown. Skipping.", TAG)
            consentPermit(isConsentObtained(consentTracker))
            initAds()
            return
        }
        // Nếu form đã được tải, show lại luôn (hoặc return tùy bạn)
        if (consentForm != null) {
            logger("consentForm already loaded", TAG)
            consentPermit(isConsentObtained(consentTracker))
            initAds()
            return
        }
        logger("requestConsentInfoUpdate:loadConsentForm", TAG)
        // Loads a consent form. Must be called on the main thread.
        UserMessagingPlatform.loadConsentForm(
            context,
            { consentFormShow ->
                consentForm = consentFormShow
                logger("consentForm is required to show" + consentInformation.consentStatus.toString(), TAG)
                when (consentInformation.consentStatus) {
                    ConsentInformation.ConsentStatus.REQUIRED -> {
                        hasShownConsentForm = true
                        isShowGDPR = true
                        logger("consentForm is required to show:::${consentForm}", TAG)
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
                                logger("consentForm show ${formError.message}", TAG)
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
                                logger("loadForm:::OBTAINED", TAG)
                                consentPermit(isConsentObtained(consentTracker, isTracking = true))
                                initAds()
                            } else {
                                logger("loadForm:::!!!OBTAINED", TAG)
                                consentPermit(isConsentObtained(consentTracker))
                                initAds()
                            }
                            // Chỉ reload nếu thật sự yêu cầu
                            if (isShowForceAgain) {
                                logger("loadForm:::isShowForceAgain", TAG)
                                loadForm(activity, consentTracker, true, consentPermit, initAds, callBackFormError)
                            }
                        }
                    }

                    else -> {
                        consentPermit(isConsentObtained(consentTracker))
                    }
                }
            },
            { formError ->
                logger("loadForm Failure: ${formError.message}", TAG)
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
            logger("reUseExistingConsentForm$consentForm", TAG)
            if (AdsSDK.appType == AppType.PDF) {
                logEvent(eventName = "GDPR_showFormGDPR_$language")
                logEvent(eventName = "GDPR_showFormGDPR")
                logEvent(eventName = "GDPR_formAvailable")
            } else {
                logEvent(eventName = "GDPR_showFormGDPR_$language")
                logEvent(eventName = "GDPR_showForm")
            }
            consentForm?.show(
                activity,
            ) { formError ->
                // Log error
                if (formError != null) {
                    logger("consentForm formError ${formError.message}", TAG)
                }
                // App can start requesting ads.
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                    logger("consentForm is Obtained", TAG)
                    consentPermit(isConsentObtained(consentTracker))
                    initAds()
                }
                logger("loadForm:::2222222222", TAG)
                // Handle dismissal by reloading form.
                loadForm(activity, consentTracker, true, consentPermit, initAds, callBackFormError = callBackFormError)
            }
        } else {
            logger("Consent form not available, check internet connection.", TAG)
            consentPermit(isConsentObtained(consentTracker))
        }
    }

    /**RETURNS TRUE IF EU/UK IS TRULY OBTAINED OR NOT REQUIRED ELSE FALSE*/
    private fun isConsentObtained(consentTracker: ConsentTracker, isTracking: Boolean = false): Boolean {
        val isSendTracking = isShowGDPR && isTracking
        val obtained = consentTracker.isUserConsentValid(isSendTracking) && consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED
        val notRequired = consentInformation.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
        val isObtained = obtained || notRequired
        logger("isConsentObtained or not required: $isObtained", TAG)
        return isObtained
    }

    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    /**RESET ONLY IF TRULY REQUIRED. E.G FOR TESTING OR USER WANTS TO RESET CONSENT SETTINGS*/
    fun resetConsent() {
        hasShownConsentForm = false
        consentInformation.reset()
    }
}