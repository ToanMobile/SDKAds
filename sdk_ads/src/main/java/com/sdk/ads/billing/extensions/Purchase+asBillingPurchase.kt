package com.sdk.ads.billing.extensions

import com.android.billingclient.api.Purchase
import com.sdk.ads.billing.BillingPurchase

val Purchase.asBillingPurchase: BillingPurchase
    get() = BillingPurchase(this)

val List<Purchase>.asBillingPurchases: List<BillingPurchase>
    get() = map { it.asBillingPurchase }
