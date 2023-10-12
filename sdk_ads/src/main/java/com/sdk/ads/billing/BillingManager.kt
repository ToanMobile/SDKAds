package com.sdk.ads.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.Purchase.PurchaseState.PENDING
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import com.sdk.ads.billing.extensions.asBillingProducts
import com.sdk.ads.billing.extensions.asBillingPurchases
import java.lang.ref.WeakReference

class BillingManager(activity: Activity) {

    // region Public Variables

    var purchaseListener: PurchaseListener? = null
    private var productsListener: ProductsListener? = null

    // endregion

    // region Private Variables

    private var activity: WeakReference<Activity> = WeakReference(activity)

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(activity)
            .setListener(::onPurchasesUpdated)
            .enablePendingPurchases()
            .build()
    }

    // endregion

    // region Public Methods

    @JvmOverloads
    fun startConnection(listener: BillingConnectionListener? = null) {
        if (billingClient.isReady) {
            listener?.onSuccess()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                listener?.response(billingResult.responseCode)

                when (billingResult.responseCode) {
                    OK -> listener?.onSuccess()
                }
            }

            override fun onBillingServiceDisconnected() {
                listener?.onDisconnected()
            }
        })
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun queryPurchases() {
        ensureConnection {
            performQueryPurchases()
        }
    }

    fun querySkuDetails(skus: List<String>) {
        ensureConnection {
            performQuerySkuDetails(skus)
        }
    }

    fun launchBillingFlow(product: BillingProduct) {
        ensureConnection {
            performLaunchBillingFlow(product.skuDetails)
        }
    }

    @JvmName("launchBillingFlowSku")
    fun launchBillingFlow(sku: String) {
        ensureConnection {
            performQuerySkuDetails(listOf(sku)) { skuDetails ->
                Log.e("launchBillingFlow::", skuDetails.toString())
                skuDetails.firstOrNull()?.let {
                    performLaunchBillingFlow(it)
                }
            }
        }
    }

    fun consume(purchase: BillingPurchase) {
        ensureConnection {
            performConsume(purchase.purchase)
        }
    }

    // endregion

    // region Private Methods

    private fun ensureConnection(onSuccess: () -> Unit) {
        startConnection(object : BillingConnectionListener() {
            override fun onSuccess() {
                onSuccess()
            }
        })
    }

    private fun performQueryPurchases() {
        billingClient.queryPurchasesAsync(INAPP) { billingResult, list ->
            if (billingResult.responseCode == OK) {
                handlePurchases(list)
            }
        }
    }

    private fun performQuerySkuDetails(skus: List<String>, resultHandler: ((List<SkuDetails>) -> Unit)? = null) {
        val params = SkuDetailsParams
            .newBuilder()
            .setSkusList(skus)
            .setType(INAPP)
            .build()
        Log.e("querySkuDetails:skus::", skus.toString() + "params:" + params.toString())
        billingClient.querySkuDetailsAsync(params) { result, detailsList ->
            Log.e("querySkuDetailsAsync::", result.toString() + "detailsList:" + detailsList.toString())
            when (result.responseCode) {
                OK -> {
                    val list = detailsList ?: listOf()
                    resultHandler?.invoke(list)
                    productsListener?.onResult(list.asBillingProducts)
                }

                SERVICE_DISCONNECTED -> querySkuDetails(skus)
                else -> {
                    Log.e("querySkuDetailsError::", result.toString())
                    // TODO: Handle other errors
                }
            }
        }
    }

    private fun performLaunchBillingFlow(productDetails: SkuDetails) {
        val activity = activity.get() ?: return

        val flowParams = BillingFlowParams
            .newBuilder()
            .setSkuDetails(productDetails)
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun performConsume(purchase: Purchase) {
        // TODO: Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, outToken ->
            if (billingResult.responseCode == OK) {
                performQueryPurchases()
            }
        }
    }

    private fun performAcknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            return
        }

        val params = AcknowledgePurchaseParams
            .newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) {}
    }

    private fun handlePurchases(list: List<Purchase>) {
        if (list.isEmpty()) {
            purchaseListener?.onResult(listOf(), listOf())
            return
        }

        val purchased = list.filter { it.purchaseState == PURCHASED }
        purchased.forEach {
            performAcknowledgePurchase(it)
        }

        val pending = list.filter { it.purchaseState == PENDING }

        purchaseListener?.onResult(purchased.asBillingPurchases, pending.asBillingPurchases)
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            OK -> purchases?.let {
                handlePurchases(it)
            }

            SERVICE_DISCONNECTED -> startConnection()
            else -> queryPurchases()
        }
    }

    // endregion
}
