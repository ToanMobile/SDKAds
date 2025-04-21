package com.sdk.ads.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import com.sdk.ads.databinding.DialogLoadingAdsBinding
import com.sdk.ads.ui.BaseDialog

class DialogShowLoadingAds(context: Context) : BaseDialog<DialogLoadingAdsBinding>(context) {

    override val binding: DialogLoadingAdsBinding = DialogLoadingAdsBinding.inflate(LayoutInflater.from(context))

    override fun getWidthPercent(): Float = 1f

    override fun onViewReady() {
        // Any UI setup here
    }
}