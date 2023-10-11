package com.sdk.ads.billing

import com.android.billingclient.api.SkuDetails

data class BillingProduct(internal val skuDetails: SkuDetails) {

    val sku: String
        get() = skuDetails.sku
}
