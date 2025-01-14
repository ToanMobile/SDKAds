package com.sdk.ads.billing

import com.android.billingclient.api.ProductDetails

data class BillingProduct(internal val skuDetails: ProductDetails) {

    val sku: String
        get() = skuDetails.productId
}
