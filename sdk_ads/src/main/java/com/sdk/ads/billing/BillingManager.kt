package com.sdk.ads.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED
import com.android.billingclient.api.BillingClient.ProductType.INAPP
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import com.android.billingclient.api.Purchase.PurchaseState.PENDING
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.sdk.ads.billing.extensions.asBillingProducts
import com.sdk.ads.billing.extensions.asBillingPurchases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.runBlocking
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

    fun launchBillingFlow(productIdsInApp: List<String>, productIdsSubs: List<String>, purchasedSku: String) {
        ensureConnection {
            queryProductDetails(productIdsInApp, productIdsSubs, purchasedSku)
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

    private fun performQueryPurchasesV5() {
        billingClient.queryPurchasesAsync(SUBS) { billingResult, list ->
            if (billingResult.responseCode == OK) {
                handlePurchases(list)
            }
        }
    }

    private fun performQueryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(SUBS)
                .build(),
        ) { billingResult, list ->
            if (billingResult.responseCode == OK) {
                handlePurchases(list)
            }
        }
    }

    private fun queryProductDetails(
        productIdsInApp: List<String>,
        productIdsSubs: List<String>,
        purchasedSku: String
    ) = runBlocking {
        val inAppFlow = getDetailsFlow(productIdsInApp, INAPP)
        val subsFlow = getDetailsFlow(productIdsSubs, SUBS)
        inAppFlow.zip(subsFlow) { inAppResult, subsResult ->
            return@zip inAppResult + subsResult
        }.collect { listProduct ->
            listProduct.firstOrNull { it.productId == purchasedSku }?.let {
                performLaunchBillingFlow(it)
            }
        }
    }

    private fun getDetailsFlow(productIds: List<String>, type: String): Flow<List<ProductDetails>> {
        val productList = productIds.map { productId ->
            Product.newBuilder()
                .setProductId(productId)
                .setProductType(type)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        return flow {
            emit(billingClient.queryProductDetails(params))
        }.map { result ->
            result.productDetailsList ?: emptyList()
        }
    }

    private fun performQuerySkuDetails(
        skus: List<String>,
        resultHandler: ((List<SkuDetails>) -> Unit)? = null
    ) {
        val params = SkuDetailsParams
            .newBuilder()
            .setSkusList(skus)
            .setType(INAPP)
            .build()
        Log.e("querySkuDetails:skus::", skus.toString() + "params:" + params.toString())
        billingClient.querySkuDetailsAsync(params) { result, detailsList ->
            Log.e(
                "querySkuDetailsAsync::",
                result.toString() + "detailsList:" + detailsList.toString()
            )
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

    private fun performLaunchBillingFlow(productDetails: ProductDetails) {
        val activity = activity.get() ?: return
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build(),
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
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
        Log.e("handlePurchases:::", list.toString())
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
        Log.e("onPurchasesUpdated:::", billingResult.responseCode.toString())
        when (billingResult.responseCode) {
            OK -> purchases?.let {
                handlePurchases(it)
            }

            USER_CANCELED -> purchaseListener?.onUserCancelBilling()

            SERVICE_DISCONNECTED -> startConnection()
            else -> queryPurchases()
        }
    }

    // endregion
}
