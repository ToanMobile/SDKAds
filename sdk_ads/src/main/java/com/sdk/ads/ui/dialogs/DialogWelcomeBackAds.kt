package com.sdk.ads.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.admob.ui.BaseDialog
import com.sdk.ads.databinding.DialogWelcomeBackResumeBinding
import com.sdk.ads.utils.delay

class DialogWelcomeBackAds(
    context: Context,
    private val onGotoApp: () -> Unit,
) : BaseDialog<DialogWelcomeBackResumeBinding>(context, height = ViewGroup.LayoutParams.MATCH_PARENT) {

    override val binding = DialogWelcomeBackResumeBinding.inflate(LayoutInflater.from(context))

    override fun onViewReady() {
        binding.btnGotoApp.setOnClickListener {
            onGotoApp.invoke()
            delay(200) {
                dismiss()
            }
        }
    }
}
