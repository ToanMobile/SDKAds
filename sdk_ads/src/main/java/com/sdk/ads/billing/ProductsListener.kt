package com.sdk.ads.billing

interface ProductsListener {

    fun onResult(products: List<BillingProduct>)
}
