package com.sdk.ads.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class GdprConsent(val context: Context) {
    @Suppress("PrivatePropertyName")
    private val TAG = "GdprConsent"
    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)
    private var consentForm: ConsentForm? = null

    /**IN PRODUCTION CALL AT ONCREATE FOR CONSENT FORM CHECK*/
    fun updateConsentInfo(
        activity: Activity,
        underAge: Boolean,
        consentTracker: ConsentTracker,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit
    ) {
        val params = ConsentRequestParameters
            .Builder()
            // .setAdMobAppId(context.getString(R.string.AdMob_App_ID))
            .setTagForUnderAgeOfConsent(underAge)
            .build()
        requestConsentInfoUpdate(
            activity = activity,
            params = params,
            consentPermit = consentPermit,
            consentTracker = consentTracker,
            initAds = { initAds() }
        )
    }

    /**ONLY TO DEBUG EU & NONE EU GEOGRAPHICS
     * EU: ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
     * NOT EU: ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA
     * DISABLED: ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED
     * requestConsentInfoUpdate() logs the hashed id when run*/
    fun updateConsentInfoWithDebugGeographics(
        activity: Activity,
        georaph: Int = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA,
        consentTracker: ConsentTracker,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit
    ) {
        val debugSettings = ConsentDebugSettings.Builder(context)
            .setDebugGeography(georaph)
            //.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(AdRequest.DEVICE_ID_EMULATOR)
            .addTestDeviceHashedId("B58902D5FBC20938E8B12C76700BD34C")
            .build()

        val params = ConsentRequestParameters
            .Builder()
            .setConsentDebugSettings(debugSettings)
            // .setAdMobAppId(context.getString(R.string.AdMob_App_ID))
            .build()
        requestConsentInfoUpdate(
            activity = activity,
            params = params,
            consentTracker = consentTracker,
            consentPermit = consentPermit,
            initAds = { initAds() }
        )
    }

    private fun requestConsentInfoUpdate(
        activity: Activity,
        params: ConsentRequestParameters,
        consentTracker: ConsentTracker,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit
    ) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { // The consent information state was updated, ready to check if a form is available.
                if (consentInformation.isConsentFormAvailable) {
                    loadForm(activity, consentTracker, consentPermit, initAds = { initAds() })
                } else {
                    consentPermit(isConsentObtained(consentTracker))
                }
            },
            { formError ->
                Log.e(TAG, "requestConsentInfoUpdate: ${formError.message}")
            }
        )
    }

    private fun loadForm(
        activity: Activity,
        consentTracker: ConsentTracker,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit
    ) { // Loads a consent form. Must be called on the main thread.
        UserMessagingPlatform.loadConsentForm(
            context,
            { _consentForm ->
                // Take form if needed later
                consentForm = _consentForm
                when (consentInformation.consentStatus) {
                    ConsentInformation.ConsentStatus.REQUIRED -> {
                        Log.e(TAG, "consentForm is required to show")
                        consentForm?.show(
                            activity,
                        ) { formError ->
                            // Log error
                            if (formError != null) {
                                Log.e(TAG, "consentForm show ${formError.message}")
                            }
                            // App can start requesting ads.
                            if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                                Log.e(TAG, "consentForm is Obtained")
                                consentPermit(isConsentObtained(consentTracker))
                                initAds()
                            }
                            // Handle dismissal by reloading form.
                            loadForm(activity, consentTracker, consentPermit, initAds)
                        }
                    }

                    else -> {
                        consentPermit(isConsentObtained(consentTracker))
                    }
                }
            },
            { formError ->
                Log.e(TAG, "loadForm Failure: ${formError.message}")
            },
        )
    }

    fun reUseExistingConsentForm(
        activity: Activity,
        consentTracker: ConsentTracker,
        consentPermit: (Boolean) -> Unit,
        initAds: () -> Unit
    ) {
        if (consentInformation.isConsentFormAvailable) {
            Log.e(TAG, "reUseExistingConsentForm")
            consentForm?.show(
                activity,
            ) { formError ->
                // Log error
                if (formError != null) {
                    Log.e(TAG, "consentForm show ${formError.message}")
                }
                // App can start requesting ads.
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                    Log.e(TAG, "consentForm is Obtained")
                    consentPermit(isConsentObtained(consentTracker))
                    initAds()
                }
                // Handle dismissal by reloading form.
                loadForm(activity, consentTracker, consentPermit, initAds)
            }
        } else {
            Log.e(TAG, "Consent form not available, check internet connection.")
            consentPermit(isConsentObtained(consentTracker))
        }
    }

    /**RETURNS TRUE IF EU/UK IS TRULY OBTAINED OR NOT REQUIRED ELSE FALSE*/
    private fun isConsentObtained(consentTracker: ConsentTracker): Boolean {
        val obtained = consentTracker.isUserConsentValid() && consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED
        val notRequired = consentInformation.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
        val isObtained = obtained || notRequired
        Log.e(TAG, "isConsentObtained or not required: $isObtained")
        return isObtained
    }

    /**RESET ONLY IF TRULY REQUIRED. E.G FOR TESTING OR USER WANTS TO RESET CONSENT SETTINGS*/
    fun resetConsent() {
        consentInformation.reset()
    }

}