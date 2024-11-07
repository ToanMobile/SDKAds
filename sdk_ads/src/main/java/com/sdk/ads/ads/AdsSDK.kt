package com.sdk.ads.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.LanguageUtils
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.sdk.ads.ads.banner.AdmobBanner
import com.sdk.ads.ads.interstitial.AdmobInterResume
import com.sdk.ads.ads.nativead.AdmobNative
import com.sdk.ads.ads.open.AdmobOpenResume
import com.sdk.ads.billing.BillingManager
import com.sdk.ads.billing.BillingPurchase
import com.sdk.ads.billing.PurchaseListener
import com.sdk.ads.billing.extensions.containsAnySKU
import com.sdk.ads.consent.ConsentTracker
import com.sdk.ads.consent.GdprConsent
import com.sdk.ads.utils.ActivityActivityLifecycleCallbacks
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.adLogger
import com.sdk.ads.utils.logAdClicked
import com.sdk.ads.utils.logParams

object AdsSDK {

    internal lateinit var app: Application
    var isEnableAds = true
        private set

    var isEnableBanner = true
        private set

    var isEnableNative = true
        private set

    var isEnableInter = true
        private set

    var isEnableOpenAds = true
        private set

    var isEnableRewarded = true
        private set

    var isEnableDebugGDPR = false
        private set

    var interTimeDelayMs = 15_000
        private set

    private var autoLogPaidValueTrackingInSdk = false

    private var outsideAdCallback: TAdCallback? = null

    private var preventShowResumeAd = false
    private var purchaseSkuForRemovingAds: List<String>? = null
    private var listTestDeviceIDs: List<String>? = null
    private var adsType = AdsType.NONE
    val getAdsType get() = adsType
    val checkAdsShow get() = adsType == AdsType.SHOW_ADS
    val checkAdsFail get() = adsType == AdsType.FAIL_ADS
    val checkIsPurchase get() = adsType == AdsType.PURCHARSE
    val checkIsShowConsent get() = adsType == AdsType.SHOW_CONSENT

    val adCallback: TAdCallback = object : TAdCallback {
        override fun onAdClicked(adUnit: String, adType: AdType) {
            super.onAdClicked(adUnit, adType)
            outsideAdCallback?.onAdClicked(adUnit, adType)
            adLogger(adType, adUnit, "onAdClicked")
            logAdClicked(adType)
        }

        override fun onAdClosed(adUnit: String, adType: AdType) {
            super.onAdClosed(adUnit, adType)
            outsideAdCallback?.onAdClosed(adUnit, adType)
            adLogger(adType, adUnit, "onAdClosed")
        }

        override fun onAdDismissedFullScreenContent(adUnit: String, adType: AdType) {
            super.onAdDismissedFullScreenContent(adUnit, adType)
            outsideAdCallback?.onAdDismissedFullScreenContent(adUnit, adType)
            adLogger(adType, adUnit, "onAdDismissedFullScreenContent")
        }

        override fun onAdShowedFullScreenContent(adUnit: String, adType: AdType) {
            super.onAdShowedFullScreenContent(adUnit, adType)
            outsideAdCallback?.onAdShowedFullScreenContent(adUnit, adType)
            adLogger(adType, adUnit, "onAdShowedFullScreenContent")
        }

        override fun onAdFailedToShowFullScreenContent(adUnit: String, adType: AdType) {
            super.onAdFailedToShowFullScreenContent(adUnit, adType)
            outsideAdCallback?.onAdFailedToShowFullScreenContent(adUnit, adType)
            adLogger(adType, adUnit, "onAdFailedToShowFullScreenContent")
        }

        override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError?) {
            super.onAdFailedToLoad(adUnit, adType, error)
            outsideAdCallback?.onAdFailedToLoad(adUnit, adType, error)
            adLogger(adType, adUnit, "onAdFailedToLoad(${error?.code} - ${error?.message})")
        }

        override fun onAdImpression(adUnit: String, adType: AdType) {
            super.onAdImpression(adUnit, adType)
            outsideAdCallback?.onAdImpression(adUnit, adType)
            adLogger(adType, adUnit, "onAdImpression")
        }

        override fun onAdLoaded(adUnit: String, adType: AdType) {
            super.onAdLoaded(adUnit, adType)
            outsideAdCallback?.onAdLoaded(adUnit, adType)
            adLogger(adType, adUnit, "onAdLoaded")
        }

        override fun onAdOpened(adUnit: String, adType: AdType) {
            super.onAdOpened(adUnit, adType)
            outsideAdCallback?.onAdOpened(adUnit, adType)
            adLogger(adType, adUnit, "onAdOpened")
        }

        override fun onAdSwipeGestureClicked(adUnit: String, adType: AdType) {
            super.onAdSwipeGestureClicked(adUnit, adType)
            outsideAdCallback?.onAdSwipeGestureClicked(adUnit, adType)
            adLogger(adType, adUnit, "onAdSwipeGestureClicked")
        }

        override fun onPaidValueListener(bundle: Bundle) {
            super.onPaidValueListener(bundle)
            outsideAdCallback?.onPaidValueListener(bundle)

            if (autoLogPaidValueTrackingInSdk) {
                logParams("AdValue") {
                    bundle.keySet().forEach { key ->
                        val value = bundle.getString(key)
                        if (!value.isNullOrBlank()) {
                            param(key, value)
                        }
                    }
                }
            }
        }
    }

    val activities = mutableSetOf<Activity>()

    val clazzIgnoreAdResume = mutableListOf<Class<*>>()

    private val applicationStateObserver = object : DefaultLifecycleObserver {

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            if (preventShowResumeAd) {
                preventShowResumeAd = false
                return
            }
            AdmobInterResume.onInterAppResume()
            AdmobOpenResume.onOpenAdAppResume()
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
        }
    }

    private val activityLifecycleCallbacks = object : ActivityActivityLifecycleCallbacks() {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
            super.onActivityCreated(activity, bundle)
            activities.add(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            super.onActivityResumed(activity)
            activities.add(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            super.onActivityDestroyed(activity)
            activities.remove(activity)
        }
    }

    fun init(application: Application): AdsSDK {
        app = application
        ProcessLifecycleOwner.get().lifecycle.addObserver(applicationStateObserver)
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        return this
    }

    fun setAdCallback(callback: TAdCallback): AdsSDK {
        outsideAdCallback = callback
        return this
    }

    fun setIgnoreAdResume(vararg clazz: Class<*>): AdsSDK {
        clazzIgnoreAdResume.clear()
        clazzIgnoreAdResume.add(AdActivity::class.java)
        clazzIgnoreAdResume.addAll(clazz)
        return this
    }

    fun preventShowResumeAdNextTime() {
        preventShowResumeAd = true
    }

    fun setEnableBanner(isEnable: Boolean) {
        isEnableBanner = isEnable
        AdmobBanner.setEnableBanner(isEnable)
    }

    fun setEnableNative(isEnable: Boolean) {
        isEnableNative = isEnable
        AdmobNative.setEnableNative(isEnable)
    }

    fun setEnableInter(isEnable: Boolean) {
        isEnableInter = isEnable
    }

    fun setEnableOpenAds(isEnable: Boolean) {
        isEnableOpenAds = isEnable
    }

    fun setEnableRewarded(isEnable: Boolean) {
        isEnableRewarded = isEnable
    }

    fun setEnableDebugGDPR(isEnable: Boolean) {
        isEnableDebugGDPR = isEnable
    }

    fun setTimeInterDelayMs(timeDelayMs: Int) {
        interTimeDelayMs = timeDelayMs
    }

    fun setAutoTrackingPaidValueInSdk(useInSDK: Boolean) {
        autoLogPaidValueTrackingInSdk = useInSDK
    }

    internal fun defaultAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    // UMP
    fun initialize(activity: Activity, listener: AdsInitializeListener) {
        Log.e("initialize:::", "initialize")
        setDebugConfiguration()
        if (!isEnableAds) {
            listener.onFail("Ads is not allowed.")
            listener.always()
            return
        }
        val skus = purchaseSkuForRemovingAds ?: listOf()
        if (skus.isNotEmpty()) {
            performQueryPurchases(activity, listener)
        } else {
            performConsent(activity, listener)
        }
    }

    private fun performQueryPurchases(activity: Activity, listener: AdsInitializeListener) {
        val billingManager = BillingManager(activity)
        billingManager.purchaseListener = object : PurchaseListener {
            override fun onResult(purchases: List<BillingPurchase>, pending: List<BillingPurchase>) {
                val skus = purchaseSkuForRemovingAds ?: listOf()
                //Log.e("performQueryPurchases:", "purchases:$purchases skus=$skus")
                if (!purchases.containsAnySKU(skus)) {
                    //Log.e("performQueryPurchases:", "ok")
                    listener.onPurchase(isPurchase = false)
                    performConsent(activity = activity, listener = listener)
                    adsType = AdsType.NONE
                } else {
                    //Log.e("performQueryPurchases:", "There are some purchases for removing ads.")
                    listener.onFail("There are some purchases for removing ads.")
                    listener.onPurchase(isPurchase = true)
                    listener.always()
                    adsType = AdsType.PURCHARSE
                }
            }

            override fun onUserCancelBilling() {
                Log.e("performQueryPurchases:", "onUserCancelBilling")
                listener.onPurchase(isPurchase = false)
            }
        }

        billingManager.queryPurchases()
    }

    private fun performInitializeAds(activity: Activity, listener: AdsInitializeListener) {
        MobileAds.initialize(activity) {
            val isInitialized = it.adapterStatusMap.entries.any { entry -> entry.value.initializationState.name == "READY" }
            if (isInitialized) {
                Log.e("performInitializeAds:::", "AdsType.SHOW_ADS")
                MobileAds.setAppMuted(true)
                listener.onInitialize()
            } else {
                Log.e("performInitializeAds:::", "AdsType.FAIL_ADS")
                val first = it.adapterStatusMap.entries.firstOrNull()?.value
                listener.onFail(first?.description ?: first?.initializationState?.name ?: "Ads initialization fail.")
            }
            listener.always()
        }
    }

    fun setPurchaseSku(purchasedSkuRemovingAds: List<String>): AdsSDK {
        this.purchaseSkuForRemovingAds = purchasedSkuRemovingAds
        return this
    }

    fun setDeviceTest(listTestDeviceIDs: List<String>): AdsSDK {
        this.listTestDeviceIDs = listTestDeviceIDs
        return this
    }

    private fun setDebugConfiguration() {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listTestDeviceIDs)
                .build(),
        )
    }

    private fun performConsent(activity: Activity, listener: AdsInitializeListener) {
        //performInitializeAds(activity, listener)
        //return
        adsType = AdsType.SHOW_CONSENT
        val language = LanguageUtils.getSystemLanguage().language
        val consentTracker = ConsentTracker(activity)
        val gdprConsent = GdprConsent(activity, language)
        consentTracker.updateState(isShowForceAgain = false, language = language)
        if (isEnableDebugGDPR) {
            //resetConsent(gdprConsent)
            gdprConsent.updateConsentInfoWithDebugGeoGraphics(
                activity = activity,
                consentPermit = {
                    adsType = if (it) AdsType.SHOW_ADS else AdsType.FAIL_ADS
                },
                isShowForceAgain = false,
                consentTracker = consentTracker,
                hashDeviceIdTest = listTestDeviceIDs,
                initAds = {
                    performInitializeAds(activity, listener)
                },
                callBackFormError = {
                    listener.formError(it)
                })
        } else {
            gdprConsent.updateConsentInfo(activity = activity, underAge = false, consentPermit = {
                adsType = if (it) AdsType.SHOW_ADS else AdsType.FAIL_ADS
            }, consentTracker = consentTracker, isShowForceAgain = false, initAds = {
                performInitializeAds(activity, listener)
            },
                callBackFormError = {
                    listener.formError(it)
                })
        }
        Log.e("isUserConsentValid:::", "User data consent couldn't be requested.")
        if (consentTracker.isUserConsentValid()) {
            //performInitializeAds(activity, listener)
        }
        if (consentTracker.isRequestAdsFail()) {
            forceReShowGDPR(activity, gdprConsent, consentTracker, language, listener)
            //reUseExistingConsentForm(activity, gdprConsent, consentTracker, listener)
        }
    }

    private fun forceReShowGDPR(activity: Activity, gdprConsent: GdprConsent, consentTracker: ConsentTracker, language: String, listener: AdsInitializeListener) {
        try {
            adsType = AdsType.SHOW_CONSENT
            Log.e("isUserConsentValid:::", "canRequestAds:${gdprConsent.canRequestAds()}")
            gdprConsent.resetConsent()
            consentTracker.updateState(isShowForceAgain = true, language = language)
            if (isEnableDebugGDPR) {
                gdprConsent.updateConsentInfoWithDebugGeoGraphics(
                    activity = activity,
                    consentPermit = {
                        adsType = if (it) AdsType.SHOW_ADS else AdsType.FAIL_ADS
                    },
                    consentTracker = consentTracker,
                    isShowForceAgain = true,
                    hashDeviceIdTest = listTestDeviceIDs,
                    initAds = {
                        performInitializeAds(activity, listener)
                    },
                    callBackFormError = {
                        listener.formError(it)
                    })
            } else {
                gdprConsent.updateConsentInfo(activity = activity, underAge = false, consentPermit = {
                    adsType = if (it) AdsType.SHOW_ADS else AdsType.FAIL_ADS
                }, consentTracker = consentTracker, isShowForceAgain = true, initAds = {
                    performInitializeAds(activity, listener)
                },
                    callBackFormError = {
                        listener.formError(it)
                    })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun reUseExistingConsentForm(activity: Activity, gdprConsent: GdprConsent, consentTracker: ConsentTracker, listener: AdsInitializeListener) {
        try {
            Log.e("reUseConsentForm:", "reUseExistingConsentForm")
            gdprConsent.reUseExistingConsentForm(
                activity = activity,
                consentPermit = {
                    adsType = if (it) AdsType.SHOW_ADS else AdsType.FAIL_ADS
                },
                consentTracker = consentTracker,
                initAds = {
                    performInitializeAds(activity, listener)
                },
                callBackFormError = {
                    listener.formError(it)
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetConsent(gdprConsent: GdprConsent) {
        try {
            gdprConsent.resetConsent()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
