package com.admob.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import com.admob.ui.BaseDialog
import com.sdk.ads.databinding.DialogLoadingInterBinding

class DialogShowLoadingAds(context: Context) : BaseDialog<DialogLoadingInterBinding>(context) {
    override val binding = DialogLoadingInterBinding.inflate(LayoutInflater.from(context))
    override fun onViewReady() {}
}
