package com.sdk.ads.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import com.sdk.ads.databinding.DialogBackgroundOpenResumeBinding
import com.sdk.ads.ui.BaseDialog

class DialogBackgroundOpenApp(
    context: Context,
) : BaseDialog<DialogBackgroundOpenResumeBinding>(context) {

    override fun getWidthPercent() = 1f

    override val binding = DialogBackgroundOpenResumeBinding.inflate(LayoutInflater.from(context))

    override fun onViewReady() {
    }
}
