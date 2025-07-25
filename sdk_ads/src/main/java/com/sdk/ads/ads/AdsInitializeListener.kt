package com.sdk.ads.ads

import com.google.android.ump.FormError

abstract class AdsInitializeListener {

    abstract fun onInitialize()

    abstract fun onGDPRDone(isAccept: Boolean)

    open fun onFail(message: String) {}

    open fun always() {}

    open fun formError(formError: FormError?) {}

    open fun onPurchase(isPurchase: Boolean) {}

    open fun onAcceptGDPR(isAccept: Boolean) {}
}
