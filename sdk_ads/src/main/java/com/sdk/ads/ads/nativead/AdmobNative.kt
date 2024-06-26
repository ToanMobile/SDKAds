package com.sdk.ads.ads.nativead

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.sdk.ads.R
import com.sdk.ads.ads.AdsSDK
import com.sdk.ads.databinding.AdLoadingNativeViewBinding
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.getPaidTrackingBundle
import com.sdk.ads.utils.isNetworkAvailable

object AdmobNative {

    interface INativeLoadCallback {
        fun forNativeAd(adUnitId: String, nativeAd: NativeAd) {}
    }

    private const val TAG = "AdmobNative"

    private val natives = mutableMapOf<String, NativeAd?>()
    private val nativeWithViewGroup = mutableMapOf<String, ViewGroup?>()
    private val nativesLoading = mutableMapOf<String, INativeLoadCallback>()

    fun loadOnly(adUnitId: String) {
        if (!AdsSDK.isEnableNative) {
            return
        }

        load(adUnitId)
    }

    /**
     * @param adContainer: ViewGroup contain this Native
     * @param adUnitId AdId
     * @param nativeContentLayoutId LayoutRes for Native
     * @param forceRefresh always load new ad then fill to ViewGroup
     * @param callback callback
     */
    fun show(
        adContainer: ViewGroup,
        adUnitId: String,
        @LayoutRes nativeContentLayoutId: Int,
        forceRefresh: Boolean = false,
        callback: TAdCallback? = null,
    ) {
        if (!AdsSDK.isEnableNative) {
            adContainer.removeAllViews()
            adContainer.isVisible = false
            return
        }

        addLoadingLayout(adContainer)

        if (!adContainer.context.isNetworkAvailable()) {
            return
        }

        val nativeAd = natives[adUnitId]

        val nativeIsStillLoading = nativesLoading.contains(adUnitId)

        // Native vẫn tiếp tục loading
        if (nativeIsStillLoading) {
            nativesLoading[adUnitId] = object : INativeLoadCallback {
                override fun forNativeAd(adUnitId: String, nativeAd: NativeAd) {
                    fillNative(
                        adContainer,
                        nativeAd,
                        nativeContentLayoutId,
                        adUnitId,
                    )
                }
            }
        } else {
            // Native đang ko loading và (đang null hoặc forceRefresh) thì load lại quảng cáo
            if (nativeAd == null) {
                load(
                    adUnitId,
                    callback,
                    object : INativeLoadCallback {
                        override fun forNativeAd(adUnitId: String, nativeAd: NativeAd) {
                            fillNative(
                                adContainer,
                                nativeAd,
                                nativeContentLayoutId,
                                adUnitId,
                            )
                        }
                    },
                )
            } else {
                // Native đang ko loading  và có sẵn quảng cáo thì fill luôn
                fillNative(
                    adContainer,
                    nativeAd,
                    nativeContentLayoutId,
                    adUnitId,
                )

                // Nếu forceRefresh thì load quảng cáo mới
                if (forceRefresh) {
                    load(
                        adUnitId,
                        callback,
                        object : INativeLoadCallback {
                            override fun forNativeAd(adUnitId: String, nativeAd: NativeAd) {
                                fillNative(
                                    adContainer,
                                    nativeAd,
                                    nativeContentLayoutId,
                                    adUnitId,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun load(
        adUnitId: String,
        callback: TAdCallback? = null,
        nativeLoadCallback: INativeLoadCallback = object : INativeLoadCallback {},
    ) {
        if (!AdsSDK.app.isNetworkAvailable()) {
            return
        }

        val adLoader = AdLoader.Builder(AdsSDK.app, adUnitId)
            .forNativeAd { ad: NativeAd ->
                natives[adUnitId]?.destroy()
                natives[adUnitId] = ad
                nativesLoading[adUnitId]?.forNativeAd(adUnitId, ad)
                ad.setOnPaidEventListener { adValue ->
                    val bundle = getPaidTrackingBundle(adValue, adUnitId, "Native", ad.responseInfo)
                    AdsSDK.adCallback.onPaidValueListener(bundle)
                    callback?.onPaidValueListener(bundle)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    AdsSDK.adCallback.onAdFailedToLoad(adUnitId, AdType.Native, adError)
                    callback?.onAdFailedToLoad(adUnitId, AdType.Native, adError)
                    nativesLoading.remove(adUnitId)
                    natives[adUnitId] = null
                    runCatching { Throwable(adError.message) }
                }

                override fun onAdClicked() {
                    AdsSDK.adCallback.onAdClicked(adUnitId, AdType.Native)
                    callback?.onAdClicked(adUnitId, AdType.Native)
                }

                override fun onAdClosed() {
                    AdsSDK.adCallback.onAdClosed(adUnitId, AdType.Native)
                    callback?.onAdClosed(adUnitId, AdType.Native)
                }

                override fun onAdImpression() {
                    AdsSDK.adCallback.onAdImpression(adUnitId, AdType.Native)
                    callback?.onAdImpression(adUnitId, AdType.Native)
                }

                override fun onAdLoaded() {
                    AdsSDK.adCallback.onAdLoaded(adUnitId, AdType.Native)
                    callback?.onAdLoaded(adUnitId, AdType.Native)
                    nativesLoading.remove(adUnitId)
                }

                override fun onAdOpened() {
                    AdsSDK.adCallback.onAdOpened(adUnitId, AdType.Native)
                    callback?.onAdOpened(adUnitId, AdType.Native)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build(),
            )
            .build()
        nativesLoading[adUnitId] = nativeLoadCallback
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun fillNative(
        viewGroup: ViewGroup,
        nativeAd: NativeAd,
        @LayoutRes nativeContentLayoutId: Int,
        adUnitId: String,
    ) {
        try {
            val contentNativeView = LayoutInflater
                .from(viewGroup.context)
                .inflate(nativeContentLayoutId, null, false)

            val unifiedNativeAdView = NativeAdView(AdsSDK.app)

            unifiedNativeAdView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

            contentNativeView.parent?.let {
                (it as ViewGroup).removeView(contentNativeView)
            }

            unifiedNativeAdView.addView(contentNativeView)
            viewGroup.removeAllViews()
            populateUnifiedNativeAdView(nativeAd, unifiedNativeAdView)
            viewGroup.addView(unifiedNativeAdView)

            nativeWithViewGroup[adUnitId] = viewGroup
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        val viewGroup = adView.findViewById<ViewGroup>(R.id.ad_media)
        if (viewGroup != null) {
            val mediaView = MediaView(adView.context)
            viewGroup.addView(
                mediaView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            adView.mediaView = mediaView
        }

        try {
            val viewGroupIcon = adView.findViewById<View>(R.id.ad_app_icon)
            if (viewGroupIcon != null) {
                if (viewGroupIcon is ViewGroup) {
                    val nativeAdIcon = ImageView(adView.context)
                    viewGroupIcon.addView(
                        nativeAdIcon,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    adView.iconView = nativeAdIcon
                } else {
                    adView.iconView = viewGroupIcon
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        try {
            (adView.headlineView as TextView).text = nativeAd.headline
            if (adView.mediaView != null && nativeAd.mediaContent != null) {
                adView.mediaView!!.mediaContent = nativeAd.mediaContent!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.callToActionView != null) {
                if (adView.callToActionView != null) {
                    if (nativeAd.callToAction == null) {
                        adView.callToActionView!!.visibility = View.INVISIBLE
                    } else {
                        adView.callToActionView!!.visibility = View.VISIBLE
                        if (adView.callToActionView is Button) {
                            (adView.callToActionView as Button).text = nativeAd.callToAction
                        } else {
                            (adView.callToActionView as TextView).text = nativeAd.callToAction
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.iconView != null) {
                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.INVISIBLE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAd.icon!!.drawable,
                    )
                    adView.iconView!!.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.priceView != null) {
                if (nativeAd.price == null) {
                    adView.priceView!!.visibility = View.INVISIBLE
                } else {
                    adView.priceView!!.visibility = View.VISIBLE
                    (adView.priceView as TextView).text = nativeAd.price
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.storeView != null) {
                if (nativeAd.store == null) {
                    adView.storeView!!.visibility = View.INVISIBLE
                } else {
                    adView.storeView!!.visibility = View.VISIBLE
                    (adView.storeView as TextView).text = nativeAd.store
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.starRatingView != null) {
                if (nativeAd.starRating == null) {
                    adView.starRatingView!!.visibility = View.GONE
                } else {
                    (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                    adView.starRatingView!!.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.advertiserView != null) {
                if (nativeAd.advertiser == null) {
                    adView.advertiserView!!.visibility = View.INVISIBLE
                } else {
                    (adView.advertiserView as TextView).text = nativeAd.advertiser
                    adView.advertiserView!!.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        adView.setNativeAd(nativeAd)
    }

    private fun addLoadingLayout(viewGroup: ViewGroup) {
        val view = AdLoadingNativeViewBinding
            .inflate(LayoutInflater.from(viewGroup.context))
            .root

        viewGroup.removeAllViews()
        viewGroup.addView(view, ViewGroup.LayoutParams(-1, -1))
        view.requestLayout()
    }

    fun setEnableNative(isEnable: Boolean) {
        if (!isEnable) {
            try {
                nativeWithViewGroup.forEach { (_, viewGroup) ->
                    viewGroup?.removeAllViews()
                    viewGroup?.isVisible = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
