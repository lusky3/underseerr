package app.lusk.underseerr.domain.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Android implementation of [BillingManager] using Google Play Billing Library.
 */
class AndroidBillingManager(
    private val context: Context,
    private val activityProvider: () -> Activity?
) : BillingManager, PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _purchaseDetails = kotlinx.coroutines.flow.MutableSharedFlow<PurchaseDetails>()
    override val purchaseDetails = _purchaseDetails.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun startConnection() {
        if (billingClient.isReady) return

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        _isSubscribed.value = isSubscribed("premium_subscription")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to the billing service
            }
        })
    }

    override suspend fun purchaseProduct(productId: String, basePlanId: String?): Result<Unit> = withContext(Dispatchers.Main) {
        val activity = activityProvider() ?: return@withContext Result.failure(Exception("No active activity found"))

        if (!billingClient.isReady) {
            return@withContext Result.failure(Exception("Billing client not ready"))
        }

        // 1. Query Product Details
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val productDetailsResult = billingClient.queryProductDetails(queryProductDetailsParams)

        if (productDetailsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK || 
            productDetailsResult.productDetailsList.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("Product not found: ${productDetailsResult.billingResult.debugMessage}"))
        }

        val productDetails = productDetailsResult.productDetailsList!![0]
        
        // 2. Launch Billing Flow - Find the specific base plan or take the first one
        val offer = if (basePlanId != null) {
            productDetails.subscriptionOfferDetails?.find { it.basePlanId == basePlanId }
                ?: productDetails.subscriptionOfferDetails?.firstOrNull()
        } else {
            productDetails.subscriptionOfferDetails?.firstOrNull()
        }

        val offerToken = offer?.offerToken ?: ""
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Billing flow failed: ${billingResult.debugMessage}"))
        }
    }

    override suspend fun isSubscribed(productId: String): Boolean {
        if (!billingClient.isReady) return false

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val purchasesResult = billingClient.queryPurchasesAsync(params)
        
        return purchasesResult.purchasesList.any { purchase ->
            purchase.products.contains(productId) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Emit for server-side verification
            scope.launch {
                _purchaseDetails.emit(
                    PurchaseDetails(
                        productId = purchase.products.firstOrNull() ?: "",
                        purchaseToken = purchase.purchaseToken,
                        packageName = context.packageName
                    )
                )
            }

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isSubscribed.value = true
                    }
                }
            } else {
                _isSubscribed.value = true
            }
        }
    }
}
