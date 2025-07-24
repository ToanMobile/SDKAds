package com.sdk.ads.billing

abstract class BillingConnectionListener {

    abstract fun onSuccess()

    open fun onDisconnected() {}
    fun response(code: Int) {}
}
