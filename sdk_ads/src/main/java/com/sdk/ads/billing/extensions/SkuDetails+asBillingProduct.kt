package com.sdk.ads.billing.extensions

import com.android.billingclient.api.SkuDetails
import com.sdk.ads.billing.BillingProduct

val SkuDetails.asBillingProduct: BillingProduct
    get() = BillingProduct(this)

val List<SkuDetails>.asBillingProducts: List<BillingProduct>
    get() = map { it.asBillingProduct }
