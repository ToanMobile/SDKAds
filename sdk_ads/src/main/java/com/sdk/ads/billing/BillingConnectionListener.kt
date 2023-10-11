package com.sdk.ads.billing

abstract class BillingConnectionListener {

    abstract fun onSuccess()

    fun onDisconnected() {}
    fun response(code: Int) {}
}
