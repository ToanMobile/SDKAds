package com.sdk.ads.billing

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED
import com.android.billingclient.api.BillingClient.ProductType.INAPP
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState.PENDING
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.sdk.ads.billing.extensions.asBillingPurchases
import com.sdk.ads.utils.logEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

enum class BillingStatus {
    OrderReceived, Chargeable, Charged, Pending
}

class BillingManager(activity: Activity) {

    // region Public Variables

    var purchaseListener: PurchaseListener? = null
    // endregion

    // region Private Variables

    private var activity: WeakReference<Activity> = WeakReference(activity)

    private val billingClient: BillingClient by lazy {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        BillingClient.newBuilder(activity)
            .setListener(::onPurchasesUpdated)
            .enablePendingPurchases(pendingPurchasesParams)
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

    fun launchBillingFlow(productIdsInApp: List<String>, productIdsSubs: List<String>, purchasedSku: String, resultProducts: (List<ProductDetails>) -> Unit) {
        ensureConnection {
            queryProductDetails(productIdsInApp = productIdsInApp, productIdsSubs = productIdsSubs, purchasedSku = purchasedSku, resultProducts = resultProducts)
        }
    }

    fun launchBillingFlow(productsDetails: ProductDetails) {
        ensureConnection {
            performLaunchBillingFlow(productsDetails)
        }
    }

    fun launchBillingInAppFlow(productsDetails: ProductDetails) {
        ensureConnection {
            performLaunchBillingFlow(productsDetails, onlyInApp = true)
        }
    }

    fun queryInAppProductDetails(productIdsInApp: List<String>, resultProducts: (List<ProductDetails>) -> Unit) {
        ensureConnection {
            queryProductDetails(productIdsInApp = productIdsInApp, resultProducts = resultProducts)
        }
    }

    fun queryAllProductDetails(productIdsInApp: List<String>, productIdsSubs: List<String>, resultProducts: (List<ProductDetails>) -> Unit) {
        ensureConnection {
            queryProductDetails(productIdsInApp = productIdsInApp, productIdsSubs = productIdsSubs, resultProducts = resultProducts)
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
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(INAPP) // SUBS
                .build(),
        ) { billingResult, list ->
            if (billingResult.responseCode == OK) {
                handlePurchases(list)
            }
        }
    }

    private fun queryProductDetails(
        productIdsInApp: List<String>,
        purchasedSku: String? = null,
        resultProducts: (List<ProductDetails>) -> Unit,
    ) = runBlocking {
        getDetailsFlow(productIdsInApp, INAPP).collect { listProduct ->
            resultProducts(listProduct)
            purchasedSku?.let { purchasedSku ->
                listProduct.firstOrNull { it.productId == purchasedSku }?.let {
                    performLaunchBillingFlow(it)
                }
            }
        }
    }

    private fun queryProductDetails(
        productIdsInApp: List<String>,
        productIdsSubs: List<String>,
        purchasedSku: String? = null,
        resultProducts: (List<ProductDetails>) -> Unit,
    ) = runBlocking {
        val inAppFlow = getDetailsFlow(productIdsInApp, INAPP)
        val subsFlow = getDetailsFlow(productIdsSubs, SUBS)
        inAppFlow.zip(subsFlow) { inAppResult, subsResult ->
            return@zip inAppResult + subsResult
        }.collect { listProduct ->
            resultProducts(listProduct)
            purchasedSku?.let { purchasedSku ->
                listProduct.firstOrNull { it.productId == purchasedSku }?.let {
                    performLaunchBillingFlow(it)
                }
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

    private fun performLaunchBillingFlow(productDetails: ProductDetails, onlyInApp: Boolean = false) {
        val activity = activity.get() ?: return
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
        val product = if (onlyInApp) {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        } else {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        }
        val productDetailsParamsList = listOf(product)
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

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == OK) {
                logEvent(BillingStatus.Chargeable.name)
            }
        }
    }

    private fun handlePurchases(list: List<Purchase>) {
        //Log.e("handlePurchases:::", list.toString())
        if (list.isEmpty()) {
            purchaseListener?.onResult(listOf(), listOf())
            return
        }

        val purchased = list.filter { it.purchaseState == PURCHASED }
        purchased.forEach {
            performAcknowledgePurchase(it)
        }

        val pending = list.filter { it.purchaseState == PENDING }
        if (purchased.isNotEmpty()) {
            val isCharged = purchased.filter { it.isAcknowledged }
            if (isCharged.isNotEmpty()) {
                logEvent(BillingStatus.Charged.name)
            } else {
                logEvent(BillingStatus.OrderReceived.name)
            }
        } else if (pending.isNotEmpty()) {
            logEvent(BillingStatus.Pending.name)
        }
        purchaseListener?.onResult(purchased.asBillingPurchases, pending.asBillingPurchases)
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        //Log.e("onPurchasesUpdated:::", billingResult.responseCode.toString() +"\npurchases:"+purchases)
        when (billingResult.responseCode) {
            OK, ITEM_ALREADY_OWNED -> purchases?.let {
                handlePurchases(it)
            }

            USER_CANCELED -> purchaseListener?.onUserCancelBilling()

            SERVICE_DISCONNECTED -> startConnection()
            else -> queryPurchases()
        }
    }

    // endregion
}
