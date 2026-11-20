package com.idraak.stepskyscraper.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.idraak.stepskyscraper.core.datastore.EntitlementCacheStore
import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.model.BillingFailure
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.domain.model.Entitlement
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.PassPlan
import com.idraak.stepskyscraper.core.domain.model.PlanOffer
import com.idraak.stepskyscraper.core.domain.model.UiHost
import com.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Play Console product ids (see docs/play-console-checklist.md). */
object PassProducts {
    const val SUBSCRIPTION = "architect_pass"
    const val BASE_MONTHLY = "monthly"
    const val BASE_ANNUAL = "annual"
    const val LIFETIME = "architect_pass_lifetime"
}

/**
 * Google Play Billing for Architect Pass (monthly, annual with 7-day trial, lifetime). Purchases are
 * acknowledged; the resulting entitlement is cached offline in DataStore and re-verified on each refresh.
 */
@Singleton
class PlayBillingRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val cache: EntitlementCacheStore,
    private val clock: AppClock,
) : EntitlementRepository {
    private val scope = CoroutineScope(SupervisorJob())
    private val connectMutex = Mutex()
    private val products = MutableStateFlow<Map<PassPlan, ProductDetails>>(emptyMap())
    private val offersState = MutableStateFlow<List<PlanOffer>>(emptyList())
    private var pendingPurchase: CompletableDeferred<Outcome<Unit>>? = null
    private var lastPurchasedPlan: PassPlan? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> scope.launch { onPurchasesUpdated(result, purchases.orEmpty()) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override val entitlement: Flow<Entitlement> = cache.cached

    override val offers: Flow<List<PlanOffer>> = offersState.asStateFlow()

    private suspend fun connect(): Boolean = connectMutex.withLock {
        if (client.isReady) return true
        suspendCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }

    override suspend fun refresh(): Outcome<Unit> {
        if (!connect()) return Outcome.Failure(DomainError.Billing(BillingFailure.ServiceUnavailable))
        loadProducts()
        return when (val result = queryEntitlement()) {
            is Outcome.Success -> {
                cache.save(result.value)
                Outcome.Success(Unit)
            }
            is Outcome.Failure -> result
        }
    }

    private suspend fun loadProducts() {
        val subs = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                listOf(product(PassProducts.SUBSCRIPTION, BillingClient.ProductType.SUBS)),
            ).build(),
        )
        val inapp = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                listOf(product(PassProducts.LIFETIME, BillingClient.ProductType.INAPP)),
            ).build(),
        )
        val details = subs.productDetailsList.orEmpty() + inapp.productDetailsList.orEmpty()
        val map = mutableMapOf<PassPlan, ProductDetails>()
        details.forEach { d ->
            when (d.productId) {
                PassProducts.SUBSCRIPTION -> {
                    map[PassPlan.Monthly] = d
                    map[PassPlan.Annual] = d
                }
                PassProducts.LIFETIME -> map[PassPlan.Lifetime] = d
            }
        }
        products.value = map
        offersState.value = PassPlan.entries.mapNotNull { plan -> map[plan]?.let { toOffer(plan, it) } }
    }

    private fun product(id: String, type: String) = QueryProductDetailsParams.Product.newBuilder().setProductId(
        id,
    ).setProductType(type).build()

    private fun subscriptionOffer(details: ProductDetails, plan: PassPlan): ProductDetails.SubscriptionOfferDetails? {
        val basePlan = if (plan == PassPlan.Monthly) PassProducts.BASE_MONTHLY else PassProducts.BASE_ANNUAL
        val candidates = details.subscriptionOfferDetails.orEmpty().filter { it.basePlanId == basePlan }
        // Prefer the free-trial offer for annual when the user is eligible (Play only returns eligible offers).
        return candidates.firstOrNull { it.offerId != null && plan == PassPlan.Annual } ?: candidates.firstOrNull { it.offerId == null }
    }

    private fun toOffer(plan: PassPlan, details: ProductDetails): PlanOffer? = if (plan == PassPlan.Lifetime) {
        details.oneTimePurchaseOfferDetails?.let { PlanOffer(plan, it.formattedPrice, it.priceAmountMicros, it.priceCurrencyCode, 0) }
    } else {
        subscriptionOffer(details, plan)?.let { offer ->
            val phases = offer.pricingPhases.pricingPhaseList
            val paid = phases.last()
            val trial = phases.firstOrNull { it.priceAmountMicros == 0L }
            val trialDays = trial?.billingPeriod?.let(::periodDays) ?: 0
            PlanOffer(plan, paid.formattedPrice, paid.priceAmountMicros, paid.priceCurrencyCode, trialDays)
        }
    }

    private fun periodDays(iso: String): Int = runCatching {
        java.time.Period.parse(iso).let { it.days + it.months * 30 + it.years * 365 }
    }.getOrDefault(0)

    override suspend fun purchase(plan: PassPlan, host: UiHost): Outcome<Unit> {
        val activity = host as? Activity ?: return Outcome.Failure(DomainError.Billing(BillingFailure.Developer, "No activity"))
        if (!connect()) return Outcome.Failure(DomainError.Billing(BillingFailure.ServiceUnavailable))
        if (products.value.isEmpty()) loadProducts()
        val details = products.value[plan] ?: return Outcome.Failure(DomainError.Billing(BillingFailure.ItemUnavailable))
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).apply {
            if (plan != PassPlan.Lifetime) {
                val offer = subscriptionOffer(details, plan) ?: return Outcome.Failure(DomainError.Billing(BillingFailure.ItemUnavailable))
                setOfferToken(offer.offerToken)
            }
        }.build()
        val deferred = CompletableDeferred<Outcome<Unit>>()
        pendingPurchase = deferred
        lastPurchasedPlan = plan
        val launch = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            return Outcome.Failure(DomainError.Billing(mapFailure(launch.responseCode), launch.debugMessage))
        }
        return deferred.await()
    }

    private suspend fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>) {
        val outcome: Outcome<Unit> = when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.forEach { acknowledge(it) }
                queryEntitlement().let { e -> if (e is Outcome.Success) cache.save(e.value) }
                if (purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    Outcome.Failure(DomainError.Billing(BillingFailure.Pending))
                } else {
                    Outcome.Success(Unit)
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                refresh()
                Outcome.Success(Unit)
            }
            else -> Outcome.Failure(DomainError.Billing(mapFailure(result.responseCode), result.debugMessage))
        }
        pendingPurchase?.complete(outcome)
        pendingPurchase = null
    }

    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
        }
    }

    private suspend fun queryEntitlement(): Outcome<Entitlement> {
        val subs = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        val inapp = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        if (subs.billingResult.responseCode != BillingClient.BillingResponseCode.OK ||
            inapp.billingResult.responseCode != BillingClient.BillingResponseCode.OK
        ) {
            return Outcome.Failure(DomainError.Billing(mapFailure(subs.billingResult.responseCode)))
        }
        val owned = (subs.purchasesList + inapp.purchasesList).filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        owned.forEach { acknowledge(it) }
        val lifetime = owned.any { PassProducts.LIFETIME in it.products }
        val sub = owned.firstOrNull { PassProducts.SUBSCRIPTION in it.products }
        val plan = when {
            lifetime -> PassPlan.Lifetime
            // Play does not report the base plan on the device; keep the plan bought here or cached earlier.
            sub != null -> lastPurchasedPlan ?: cache.cached.first().plan?.takeIf { it != PassPlan.Lifetime } ?: PassPlan.Annual
            else -> null
        }
        return Outcome.Success(Entitlement(isPremium = plan != null, plan = plan, verifiedAt = clock.now()))
    }

    override suspend fun restore(): Outcome<Entitlement> {
        if (!connect()) return Outcome.Failure(DomainError.Billing(BillingFailure.ServiceUnavailable))
        val result = queryEntitlement()
        if (result is Outcome.Success) cache.save(result.value)
        return result
    }

    private fun mapFailure(code: Int): BillingFailure = when (code) {
        BillingClient.BillingResponseCode.USER_CANCELED -> BillingFailure.UserCancelled
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.NETWORK_ERROR,
        -> BillingFailure.ServiceUnavailable
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingFailure.ItemUnavailable
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingFailure.AlreadyOwned
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> BillingFailure.Developer
        else -> BillingFailure.Unknown
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds
    abstract fun entitlements(impl: PlayBillingRepository): EntitlementRepository
}
