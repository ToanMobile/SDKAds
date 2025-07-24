package com.sdk.ads.ads.nativead

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
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
import com.sdk.ads.utils.logger

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
        @LayoutRes nativeContentLayoutId: Int? = null,
        forceRefresh: Boolean = false,
        callback: TAdCallback? = null,
    ) {
        if (!AdsSDK.isEnableNative) {
            adContainer.removeAllViews()
            adContainer.isVisible = false
            return
        }
        if (!adContainer.context.isNetworkAvailable()) {
            logger("No internet connection", TAG)
            val adError = LoadAdError(0, "No Fill", "com.google.android.gms.ads", AdError(0, "No Fill", "com.google.android.gms.ads"), null)
            AdsSDK.adCallback.onAdFailedToLoad(adUnitId, AdType.Native, adError)
            callback?.onAdFailedToLoad(adUnitId, AdType.Native, adError)
            nativesLoading.remove(adUnitId)
            natives[adUnitId] = null
            return
        }
        addLoadingLayout(adContainer)
        val nativeAd = natives[adUnitId]
        val nativeIsStillLoading = nativesLoading.contains(adUnitId)
        // Native vẫn tiếp tục loading
        if (nativeIsStillLoading) {
            nativesLoading[adUnitId] = object : INativeLoadCallback {
                override fun forNativeAd(adUnitId: String, nativeAd: NativeAd) {
                    fillNative(
                        adContainer,
                        nativeAd,
                        adUnitId,
                        nativeContentLayoutId,
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
                                adUnitId,
                                nativeContentLayoutId,
                            )
                        }
                    },
                )
            } else {
                // Native đang ko loading  và có sẵn quảng cáo thì fill luôn
                fillNative(
                    adContainer,
                    nativeAd,
                    adUnitId,
                    nativeContentLayoutId,
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
                                    adUnitId,
                                    nativeContentLayoutId,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
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
                if (ad.mediaContent?.hasVideoContent() == true) {
                    logger("Native ad contains video content.", TAG)
                } else {
                    logger("Native ad is image-only.", TAG)
                }
                ad.mediaContent?.videoController?.apply {
                    videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                        override fun onVideoEnd() {
                            super.onVideoEnd()
                            logger("Video ended for $adUnitId", TAG)
                        }

                        override fun onVideoStart() {
                            super.onVideoStart()
                            logger("Video started for $adUnitId", TAG)
                        }
                    }
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    AdsSDK.adCallback.onAdFailedToLoad(adUnitId, AdType.Native, adError)
                    callback?.onAdFailedToLoad(adUnitId, AdType.Native, adError)
                    nativesLoading.remove(adUnitId)
                    natives[adUnitId] = null
                    logger("Native Ad Failed to Load: $adError", TAG)
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
        adUnitId: String,
        @LayoutRes nativeContentLayoutId: Int? = null,
    ) {
        try {
            val context = viewGroup.context
            val layout = if (nativeAd.mediaContent?.hasVideoContent() == true) {
                R.layout.ad_sdk_native_view_video
            } else {
                nativeContentLayoutId ?: R.layout.ad_sdk_native_view_image
            }
            val contentNativeView = LayoutInflater.from(context).inflate(layout, null, false)
            // NativeAdView dùng đúng context gốc
            val nativeAdView = NativeAdView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(contentNativeView)
            }
            // Clear view cũ & add mới
            viewGroup.removeAllViews()
            viewGroup.addView(nativeAdView)
            // Gán dữ liệu vào các view bên trong NativeAdView
            populateUnifiedNativeAdView(nativeAd, nativeAdView)
            // Lưu lại mapping
            nativeWithViewGroup[adUnitId] = viewGroup
            // Cleanup khi view bị detach
            viewGroup.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewDetachedFromWindow(v: View) {
                    nativeAd.destroy()
                    natives.remove(adUnitId)
                    nativeWithViewGroup.remove(adUnitId)
                    v.removeOnAttachStateChangeListener(this)
                    logger("NativeAd for [$adUnitId] destroyed on detach", TAG)
                }

                override fun onViewAttachedToWindow(v: View) {
                    // No-op
                }
            })
        } catch (e: Exception) {
            logger("Error in fillNative: ${e.message}", TAG)
        }
    }

    private fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Bắt buộc gắn các view có ID khớp với layout
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon) // bạn có thể bổ sung nếu cần
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // Gán nội dung vào các view, kiểm tra null
        (adView.headlineView as? TextView)?.text = nativeAd.headline

        val mediaContent = nativeAd.mediaContent
        if (mediaContent?.hasVideoContent() == true) {
            adView.mediaView = adView.findViewById(R.id.ad_media)
            adView.mediaView?.mediaContent = mediaContent
        } else {
            try {
                val image = nativeAd.images.firstOrNull()?.drawable
                if (image != null) {
                    val mediaView = adView.findViewById<ImageView>(R.id.ad_media)
                    mediaView.setImageDrawable(image)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        (adView.bodyView as? TextView)?.apply {
            text = nativeAd.body
            visibility = if (nativeAd.body != null) View.VISIBLE else View.INVISIBLE
        }

        (adView.callToActionView)?.apply {
            visibility = if (nativeAd.callToAction != null) View.VISIBLE else View.INVISIBLE
            when (this) {
                is Button -> text = nativeAd.callToAction
                is TextView -> text = nativeAd.callToAction
            }
        }

        (adView.iconView as? ImageView)?.apply {
            setImageDrawable(nativeAd.icon?.drawable)
            visibility = if (nativeAd.icon != null) View.VISIBLE else View.GONE
        }

        (adView.priceView as? TextView)?.apply {
            text = nativeAd.price
            visibility = if (nativeAd.price != null) View.VISIBLE else View.GONE
        }

        (adView.storeView as? TextView)?.apply {
            text = nativeAd.store
            visibility = if (nativeAd.store != null) View.VISIBLE else View.GONE
        }

        (adView.starRatingView as? RatingBar)?.apply {
            rating = nativeAd.starRating?.toFloat() ?: 0f
            visibility = if (nativeAd.starRating != null) View.VISIBLE else View.GONE
        }

        (adView.advertiserView as? TextView)?.apply {
            text = nativeAd.advertiser
            visibility = if (nativeAd.advertiser != null) View.VISIBLE else View.GONE
        }

        logger(TAG, "Native ad populated: ${nativeAd.headline}")
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
