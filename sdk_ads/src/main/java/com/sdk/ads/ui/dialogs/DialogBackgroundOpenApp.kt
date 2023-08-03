package com.sdk.ads.ui.dialogs

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import com.admob.ui.BaseDialog
import com.sdk.ads.databinding.DialogBackgroundOpenResumeBinding
import com.sdk.ads.utils.screenWidth

class DialogBackgroundOpenApp(
    context: Context,
) : BaseDialog<DialogBackgroundOpenResumeBinding>(context) {

    override fun getWidthPercent() = 1f

    override val binding = DialogBackgroundOpenResumeBinding.inflate(LayoutInflater.from(context))

    override fun onViewReady() {
    }

    override fun show() {
        super.show()
        val width = screenWidth * getWidthPercent()
        window?.setLayout(width.toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setGravity(Gravity.CENTER)
    }
}
