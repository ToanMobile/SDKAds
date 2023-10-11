package com.sdk.ads.billing

import com.android.billingclient.api.Purchase

data class BillingPurchase(internal val purchase: Purchase) {

    val skus: List<String>
        get() = purchase.skus
}
