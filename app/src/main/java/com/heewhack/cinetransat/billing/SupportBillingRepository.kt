package com.heewhack.cinetransat.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class SupportBillingUiState(
    val isConnecting: Boolean = true,
    val isPurchasing: Boolean = false,
    val tipProducts: List<ProductDetails> = emptyList(),
    val monthlyProducts: List<ProductDetails> = emptyList(),
    val activeMonthlyProductId: String? = null,
    val statusMessage: String? = null,
    val didSucceed: Boolean = false,
)

class SupportBillingRepository(
    context: Context,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(SupportBillingUiState())
    val state: StateFlow<SupportBillingUiState> = _state.asStateFlow()

    private val billingClient: BillingClient =
        BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
            )
            .build()

    fun start() {
        if (billingClient.isReady) {
            scope.launch { refreshProductsAndPurchases() }
            return
        }
        _state.update { it.copy(isConnecting = true, statusMessage = null) }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        scope.launch { refreshProductsAndPurchases() }
                    } else {
                        _state.update {
                            it.copy(
                                isConnecting = false,
                                statusMessage = result.debugMessage.ifBlank { "Billing unavailable" },
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _state.update { it.copy(isConnecting = true) }
                }
            },
        )
    }

    fun end() {
        runCatching { billingClient.endConnection() }
    }

    suspend fun refreshProductsAndPurchases() {
        _state.update { it.copy(isConnecting = true) }
        val inApp = queryProductDetails(SupportProductIds.oneTimeTips, BillingClient.ProductType.INAPP)
        val subs = queryProductDetails(SupportProductIds.monthly, BillingClient.ProductType.SUBS)
        val activeMonthly = queryActiveMonthlyProductId()
        _state.update {
            it.copy(
                isConnecting = false,
                tipProducts = SupportProductIds.oneTimeTips.mapNotNull { id -> inApp[id] },
                monthlyProducts = SupportProductIds.monthly.mapNotNull { id -> subs[id] },
                activeMonthlyProductId = activeMonthly,
            )
        }
    }

    fun tipProduct(productId: String): ProductDetails? =
        _state.value.tipProducts.firstOrNull { it.productId == productId }

    fun monthlyProduct(productId: String): ProductDetails? =
        _state.value.monthlyProducts.firstOrNull { it.productId == productId }

    fun tipProductForNominalAmount(amountText: String): ProductDetails? {
        val normalized = amountText.trim().replace(',', '.')
        val productId = SupportProductIds.tipNominalAmounts[normalized] ?: return null
        return tipProduct(productId)
    }

    fun launchPurchase(
        activity: Activity,
        details: ProductDetails,
    ) {
        val offerToken =
            details.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken
        val productParams =
            if (offerToken != null) {
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            } else {
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            }
        val flowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
        _state.update { it.copy(isPurchasing = true, statusMessage = null, didSucceed = false) }
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update {
                it.copy(
                    isPurchasing = false,
                    statusMessage = result.debugMessage.ifBlank { "Unable to start purchase" },
                )
            }
        }
    }

    fun restore() {
        scope.launch {
            _state.update { it.copy(isPurchasing = true, statusMessage = null) }
            val activeMonthly = queryActiveMonthlyProductId()
            _state.update {
                it.copy(
                    isPurchasing = false,
                    activeMonthlyProductId = activeMonthly,
                    didSucceed = activeMonthly != null,
                )
            }
        }
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        scope.launch {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases.orEmpty().forEach { handlePurchase(it) }
                    val activeMonthly = queryActiveMonthlyProductId()
                    _state.update {
                        it.copy(
                            isPurchasing = false,
                            didSucceed = true,
                            activeMonthlyProductId = activeMonthly,
                        )
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    _state.update { it.copy(isPurchasing = false) }
                }
                else -> {
                    _state.update {
                        it.copy(
                            isPurchasing = false,
                            statusMessage = result.debugMessage.ifBlank { "Purchase failed" },
                        )
                    }
                }
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val isTip = purchase.products.any { it in SupportProductIds.oneTimeTips }
        if (isTip) {
            val consumeParams =
                ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            suspendCancellableCoroutine { cont ->
                billingClient.consumeAsync(consumeParams) { _, _ ->
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        } else if (!purchase.isAcknowledged) {
            val ackParams =
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            suspendCancellableCoroutine { cont ->
                billingClient.acknowledgePurchase(ackParams) { _ ->
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    private suspend fun queryProductDetails(
        productIds: List<String>,
        productType: String,
    ): Map<String, ProductDetails> {
        if (!billingClient.isReady || productIds.isEmpty()) return emptyMap()
        val productList =
            productIds.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(productType)
                    .build()
            }
        val params =
            QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return emptyMap()
        }
        return result.productDetailsList.orEmpty().associateBy { it.productId }
    }

    private suspend fun queryActiveMonthlyProductId(): String? {
        if (!billingClient.isReady) return null
        val params =
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        val purchasesResult = billingClient.queryPurchasesAsync(params)
        if (purchasesResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return null
        }
        val active =
            purchasesResult.purchasesList.firstOrNull { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.any { it in SupportProductIds.monthly }
            }
        return active?.products?.firstOrNull { it in SupportProductIds.monthly }
    }
}

fun ProductDetails.formattedPrice(): String {
    oneTimePurchaseOfferDetails?.formattedPrice?.let { return it }
    subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.firstOrNull()
        ?.formattedPrice
        ?.let { return it }
    return productId
}
