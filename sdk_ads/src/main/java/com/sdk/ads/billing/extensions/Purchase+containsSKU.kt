package com.sdk.ads.billing.extensions

import com.android.billingclient.api.Purchase
import com.sdk.ads.billing.BillingPurchase

@JvmName("skusPurchase")
fun List<Purchase>.skus(): List<String> {
    val list = arrayListOf<String>()
    forEach { list + it.skus }
    return list
}

@JvmName("skusBillingPurchase")
fun List<BillingPurchase>.skus(): List<String> {
    val list = arrayListOf<String>()
    forEach { list + it.skus }
    return list
}

@JvmName("containsPurchase")
fun List<Purchase>.contains(sku: String): Boolean {
    return skus().contains(sku)
}

@JvmName("containsAnyPurchase")
fun List<Purchase>.containsAny(skus: List<String>): Boolean {
    return skus().any { skus.contains(it) }
}

@JvmName("containsBillingPurchase")
fun List<BillingPurchase>.contains(sku: String): Boolean {
    return skus().contains(sku)
}

@JvmName("containsAnyBillingPurchase")
fun List<BillingPurchase>.containsAny(skus: List<String>): Boolean {
    return skus().any { skus.contains(it) }
}
