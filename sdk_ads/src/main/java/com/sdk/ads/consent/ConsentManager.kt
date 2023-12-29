//package com.sdk.ads.consent
//
//import android.app.Activity
//import android.util.Log
//import com.google.android.gms.ads.AdRequest
//import com.google.android.ump.ConsentDebugSettings
//import com.google.android.ump.ConsentInformation.ConsentStatus.NOT_REQUIRED
//import com.google.android.ump.ConsentInformation.ConsentStatus.OBTAINED
//import com.google.android.ump.ConsentInformation.ConsentStatus.REQUIRED
//import com.google.android.ump.ConsentRequestParameters
//import com.google.android.ump.UserMessagingPlatform
//import java.lang.ref.WeakReference
//
//class ConsentManager(activity: Activity) {
//
//    // region Companion Object
//
//    // endregion
//
//    // region Public Variables
//
//    val isApplicable: Boolean
//        get() = consentInformation.consentStatus == OBTAINED || consentInformation.consentStatus == REQUIRED
//
//    // endregion
//
//    // region Private Variables
//
//    private var activity: WeakReference<Activity> = WeakReference(activity)
//
//    private val consentInformation by lazy { UserMessagingPlatform.getConsentInformation(activity) }
//    private var consentInformationUpdated = false
//
//    private val consentResult: Boolean
//        get() = consentInformation.consentStatus == OBTAINED || consentInformation.consentStatus == NOT_REQUIRED
//
//    private val consentRequestParameters: ConsentRequestParameters
//        get() = consentBuilder().build()
//
//    // endregion
//
//    // region Public Methods
//
//    fun request(onConsentResult: (Boolean) -> Unit) {
//        if (consentInformationUpdated && consentResult) {
//            onConsentResult(consentResult)
//            return
//        }
//        val activity = activity.get() ?: return onConsentResult(consentResult)
//        consentInformation.requestConsentInfoUpdate(activity, consentRequestParameters, {
//            consentInformationUpdated = true
//
//            if (consentInformation.isConsentFormAvailable) {
//                loadForm(onConsentResult)
//            } else {
//                onConsentResult(consentResult)
//            }
//        }, {
//            Log.d("Consent", "requestConsentInfoUpdate.error: ${it.message}")
//            onConsentResult(consentResult)
//        })
//    }
//
//    fun reset() {
//        consentInformation.reset()
//    }
//
//    // endregion
//
//    // region Private Methods
//
//    private fun loadForm(onConsent: (Boolean) -> Unit) {
//        val activity = activity.get() ?: return
//        UserMessagingPlatform.loadConsentForm(activity, { consentForm ->
//            if (consentInformation.consentStatus == REQUIRED) {
//                consentForm.show(activity) { error ->
//                    if (error != null) {
//                        loadForm(onConsent)
//                    } else {
//                        onConsent(consentResult)
//                    }
//                }
//            } else {
//                onConsent(consentResult)
//            }
//        }, {
//            Log.d("Consent", "loadConsentForm.error: ${it.message}")
//            onConsent(consentResult)
//        })
//    }
//
//    private fun consentBuilder(): ConsentRequestParameters.Builder {
//        val builder = ConsentRequestParameters.Builder()
//        val activity = activity.get() ?: return builder
//        val debugSettings = ConsentDebugSettings.Builder(activity)
//            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//            .addTestDeviceHashedId(AdRequest.DEVICE_ID_EMULATOR)
//            .addTestDeviceHashedId("7F918B01D8AF60BA7D816B92323D4F97")
//            .setForceTesting(true)
//            .build()
//        builder.setConsentDebugSettings(debugSettings)
//        return builder
//    }
//    // endregion
//}
