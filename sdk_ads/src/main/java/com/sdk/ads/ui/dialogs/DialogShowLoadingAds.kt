package com.sdk.ads.ui.dialogs

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import com.admob.ui.BaseDialog
import com.sdk.ads.databinding.DialogLoadingAdsBinding
import com.sdk.ads.utils.screenWidth

class DialogShowLoadingAds(context: Context) : BaseDialog<DialogLoadingAdsBinding>(context) {

    override val binding: DialogLoadingAdsBinding = DialogLoadingAdsBinding.inflate(LayoutInflater.from(context))

    override fun getWidthPercent(): Float = 1f

    override fun onViewReady() {
        // Any UI setup here
    }

    override fun show() {
        super.show()
        window?.apply {
            val dialogWidth = (screenWidth * getWidthPercent()).toInt()
            setLayout(dialogWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.CENTER)
        }
        Log.d("DialogShowLoadingAds", "Dialog shown")
    }
}