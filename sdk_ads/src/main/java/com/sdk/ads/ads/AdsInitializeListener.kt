package com.sdk.ads.ads

abstract class AdsInitializeListener {

    abstract fun onInitialize()

    open fun onFail(message: String) {}
    open fun always() {}
}
