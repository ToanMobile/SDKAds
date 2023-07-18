package com.sdk.ads.ads.banner

import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.ads.AdsSDK.isEnableBanner
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.adaptiveBannerSize
import com.sdk.ads.utils.addLoadingView
import com.sdk.ads.utils.getPaidTrackingBundle
import com.sdk.ads.utils.isNetworkAvailable

object AdmobBanner {

    private const val TAG = "AdmobBanner"

    private val banners = mutableMapOf<String, AdView?>()

    /**
     * @param adContainer: ViewGroup contain this Ad
     * @param adUnitId AdId
     * @param forceRefresh always load new ad then fill to ViewGroup
     * @param callback callback
     */
    fun showAdaptive(
        adContainer: ViewGroup,
        adUnitId: String,
        forceRefresh: Boolean = false,
        callback: TAdCallback? = null,
    ) = show(
        adContainer,
        adUnitId,
        BannerAdSize.BannerAdaptive,
        forceRefresh,
        callback,
    )

    /**
     * @param adContainer: ViewGroup contain this Ad
     * @param adUnitId AdId
     * @param forceRefresh always load new ad then fill to ViewGroup
     * @param callback callback
     */
    fun show300x250(
        adContainer: ViewGroup,
        adUnitId: String,
        forceRefresh: Boolean = false,
        callback: TAdCallback? = null,
    ) = show(
        adContainer,
        adUnitId,
        BannerAdSize.Banner300x250,
        forceRefresh,
        callback,
    )

    /**
     * Each position show be Unique AdUnitID
     * @param adContainer: ViewGroup contain this Ad
     * @param adUnitId AdId
     * @param showOnBottom: Show on Top or Bottom
     * @param forceRefresh always load new ad then fill to ViewGroup
     * @param callback callback
     */
    fun showCollapsible(
        adContainer: ViewGroup,
        adUnitId: String,
        showOnBottom: Boolean = true,
        forceRefresh: Boolean = false,
        callback: TAdCallback? = null,
    ) = show(
        adContainer,
        adUnitId,
        if (showOnBottom) BannerAdSize.BannerCollapsibleBottom else BannerAdSize.BannerCollapsibleTop,
        forceRefresh,
        callback,
    )

    private fun show(
        adContainer: ViewGroup,
        adUnitId: String,
        bannerType: BannerAdSize,
        forceRefresh: Boolean = false,
        callback: TAdCallback? = null,
    ) {
        if (!isEnableBanner) {
            adContainer.removeAllViews()
            adContainer.isVisible = false
            return
        }

        val adSize = getAdSize(bannerType)
        addLoadingLayout(adContainer, adSize)

        if (!adContainer.context.isNetworkAvailable()) {
            return
        }

        val adView = banners[adUnitId]

        if (adView == null || forceRefresh) {
            val context =
                if (bannerType == BannerAdSize.BannerCollapsibleBottom || bannerType == BannerAdSize.BannerCollapsibleTop) {
                    adContainer.context
                } else
                    AdsSDK.app

            AdView(context).let {
                it.adUnitId = adUnitId
                it.setAdSize(adSize)
                it.setAdCallback(it, callback) { addExistBanner(adContainer, it) }
                it.loadAd(getAdRequest(bannerType))
            }
        }

        if (adView != null) {
            addExistBanner(adContainer, adView)
            adView.setAdCallback(adView, callback) {
                addExistBanner(adContainer, adView)
            }
            return
        }
    }

    private fun addExistBanner(adContainer: ViewGroup, bannerView: AdView) {
        adContainer.removeAllViews()
        if (bannerView.parent is ViewGroup && bannerView.parent != null) {
            (bannerView.parent as ViewGroup).removeAllViews()
        }

        adContainer.addView(bannerView)
    }

    private fun addLoadingLayout(adContainer: ViewGroup, adSize: AdSize) {
        val lp = adContainer.layoutParams
        lp.width = adSize.getWidthInPixels(adContainer.context)
        lp.height = adSize.getHeightInPixels(adContainer.context)
        adContainer.layoutParams = lp
        adContainer.requestLayout()
        adContainer.addLoadingView()
    }

    private fun getAdSize(bannerAdSize: BannerAdSize): AdSize {
        return when (bannerAdSize) {
            BannerAdSize.BannerAdaptive -> adaptiveBannerSize
            BannerAdSize.BannerCollapsibleTop -> adaptiveBannerSize
            BannerAdSize.BannerCollapsibleBottom -> adaptiveBannerSize
            BannerAdSize.Banner300x250 -> AdSize.MEDIUM_RECTANGLE
        }
    }

    private fun getAdRequest(bannerAdSize: BannerAdSize): AdRequest {
        val adRequestBuilder = AdRequest.Builder()
        val extras = Bundle()

        if (bannerAdSize == BannerAdSize.BannerCollapsibleTop) {
            extras.putString("collapsible", "top")
            adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
        }

        if (bannerAdSize == BannerAdSize.BannerCollapsibleBottom) {
            extras.putString("collapsible", "bottom")
            adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
        }

        return adRequestBuilder.build()
    }

    private fun AdView.setAdCallback(
        adView: AdView,
        tAdCallback: TAdCallback?,
        onAdLoaded: () -> Unit,
    ) {
        adListener = object : AdListener() {
            override fun onAdClicked() {
                com.sdk.ads.ads.AdsSDK.adCallback.onAdClicked(adUnitId, com.sdk.ads.utils.AdType.Banner)
                tAdCallback?.onAdClicked(adUnitId, com.sdk.ads.utils.AdType.Banner)
            }

            override fun onAdClosed() {
                com.sdk.ads.ads.AdsSDK.adCallback.onAdClosed(adUnitId, com.sdk.ads.utils.AdType.Banner)
                tAdCallback?.onAdClosed(adUnitId, com.sdk.ads.utils.AdType.Banner)
            }

            override fun onAdFailedToLoad(var1: LoadAdError) {
                banners[adView.adUnitId] = null
                com.sdk.ads.ads.AdsSDK.adCallback.onAdFailedToLoad(adUnitId, com.sdk.ads.utils.AdType.Banner, var1)
                tAdCallback?.onAdFailedToLoad(adUnitId, com.sdk.ads.utils.AdType.Banner, var1)
                runCatching { Throwable(var1.message) }
            }

            override fun onAdImpression() {
                com.sdk.ads.ads.AdsSDK.adCallback.onAdImpression(adUnitId, com.sdk.ads.utils.AdType.Banner)
                tAdCallback?.onAdImpression(adUnitId, com.sdk.ads.utils.AdType.Banner)
            }

            override fun onAdLoaded() {
                com.sdk.ads.ads.AdsSDK.adCallback.onAdLoaded(adUnitId, com.sdk.ads.utils.AdType.Banner)
                tAdCallback?.onAdLoaded(adUnitId, com.sdk.ads.utils.AdType.Banner)

                adView.setOnPaidEventListener { adValue ->
                    val bundle = getPaidTrackingBundle(adValue, adUnitId, "Banner", adView.responseInfo)
                    com.sdk.ads.ads.AdsSDK.adCallback.onPaidValueListener(bundle)
                    tAdCallback?.onPaidValueListener(bundle)
                }

                banners[adView.adUnitId] = adView

                onAdLoaded.invoke()
            }
        }
    }

    fun setEnableBanner(isEnable: Boolean) {
        if (!isEnable) {
            try {
                banners.forEach { (_, adView) ->
                    val viewGroup = adView?.parent as? ViewGroup
                    viewGroup?.removeAllViews()
                    viewGroup?.isVisible = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
