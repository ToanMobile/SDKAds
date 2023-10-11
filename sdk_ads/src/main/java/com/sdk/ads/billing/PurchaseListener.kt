package com.sdk.ads.billing

interface PurchaseListener {

    fun onResult(purchases: List<BillingPurchase>, pending: List<BillingPurchase>)
}
