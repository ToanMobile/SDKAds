package com.sdk.ads.billing.extensions

import com.android.billingclient.api.ProductDetails
import com.sdk.ads.billing.BillingProduct

val ProductDetails.asBillingProduct: BillingProduct
    get() = BillingProduct(this)

val List<ProductDetails>.asBillingProducts: List<BillingProduct>
    get() = map { it.asBillingProduct }
