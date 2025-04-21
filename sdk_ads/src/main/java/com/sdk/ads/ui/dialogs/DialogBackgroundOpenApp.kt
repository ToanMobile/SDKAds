package com.sdk.ads.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.admob.ui.BaseDialog
import com.sdk.ads.databinding.DialogBackgroundOpenResumeBinding

class DialogBackgroundOpenApp(
    context: Context,
) : BaseDialog<DialogBackgroundOpenResumeBinding>(context, height = ViewGroup.LayoutParams.MATCH_PARENT) {

    override fun getWidthPercent() = 1f

    override val binding = DialogBackgroundOpenResumeBinding.inflate(LayoutInflater.from(context))

    override fun onViewReady() {
    }
}
