package ai.idraak.stepskyscraper.core.billing

import ai.idraak.stepskyscraper.core.datastore.EntitlementCacheStore
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.model.BillingFailure
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Entitlement
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.PassPlan
import ai.idraak.stepskyscraper.core.domain.model.PlanOffer
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Play Console product ids and the RevenueCat ids that group them (see docs/play-console-checklist.md). */
object PassProducts {
    const val SUBSCRIPTION = "architect_pass"
    const val BASE_MONTHLY = "monthly"
    const val BASE_ANNUAL = "annual"
    const val LIFETIME = "architect_pass_lifetime"

    /** RevenueCat entitlement unlocked by every plan. */
    const val ENTITLEMENT = "architect_pass"
}

/** Facts from the app's build config (`REVENUECAT_API_KEY`); a blank key switches purchases off. */
data class BillingEnvironment(val revenueCatApiKey: String)

/**
 * Architect Pass through RevenueCat (monthly, annual with 7-day trial, lifetime; the "default" offering).
 * RevenueCat talks to Google Play Billing, acknowledges purchases and validates them; the resulting
 * entitlement is cached offline in DataStore and re-verified on each refresh. Only an anonymous RevenueCat
 * user id and purchase data leave the device, never step data.
 */
@Singleton
class RevenueCatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val environment: BillingEnvironment,
    private val cache: EntitlementCacheStore,
    private val clock: AppClock,
) : EntitlementRepository {
    private val configureMutex = Mutex()
    private val packages = MutableStateFlow<Map<PassPlan, Package>>(emptyMap())
    private val offersState = MutableStateFlow<List<PlanOffer>>(emptyList())

    override val entitlement: Flow<Entitlement> = cache.cached

    override val offers: Flow<List<PlanOffer>> = offersState.asStateFlow()

    /** Configures the SDK once; false when no key is set for this build. */
    private suspend fun ready(): Boolean = configureMutex.withLock {
        if (environment.revenueCatApiKey.isBlank()) return false
        if (!Purchases.isConfigured) {
            Purchases.configure(PurchasesConfiguration.Builder(context, environment.revenueCatApiKey).build())
        }
        true
    }

    override suspend fun refresh(): Outcome<Unit> {
        if (!ready()) return unavailable()
        // Offers and entitlement are independent: a store/product problem must not hide an active Pass.
        val offersError = try {
            loadOfferings()
            null
        } catch (e: PurchasesException) {
            Log.w(TAG, "Offerings unavailable: ${e.error.code}")
            e.error
        }
        return try {
            cache.save(toEntitlement(Purchases.sharedInstance.awaitCustomerInfo()))
            offersError?.let(::failure) ?: Outcome.Success(Unit)
        } catch (e: PurchasesException) {
            failure(e.error)
        }
    }

    private suspend fun loadOfferings() {
        val current = Purchases.sharedInstance.awaitOfferings().current
        val map = buildMap {
            current?.monthly?.let { put(PassPlan.Monthly, it) }
            current?.annual?.let { put(PassPlan.Annual, it) }
            current?.lifetime?.let { put(PassPlan.Lifetime, it) }
        }
        packages.value = map
        offersState.value = PassPlan.entries.mapNotNull { plan -> map[plan]?.let { toOffer(plan, it) } }
    }

    private fun toOffer(plan: PassPlan, pkg: Package): PlanOffer {
        val product = pkg.product
        // The default option prefers the free-trial offer when Play says the user is eligible.
        val option = product.defaultOption
        val paid = option?.fullPricePhase?.price ?: product.price
        val trialDays = option?.freePhase?.billingPeriod?.iso8601?.let(::periodDays) ?: 0
        return PlanOffer(plan, paid.formatted, paid.amountMicros, paid.currencyCode, if (plan == PassPlan.Lifetime) 0 else trialDays)
    }

    private fun periodDays(iso: String): Int = runCatching {
        java.time.Period.parse(iso).let { it.days + it.months * 30 + it.years * 365 }
    }.getOrDefault(0)

    override suspend fun purchase(plan: PassPlan, host: UiHost): Outcome<Unit> {
        val activity = host as? Activity ?: return Outcome.Failure(DomainError.Billing(BillingFailure.Developer, "No activity"))
        if (!ready()) return unavailable()
        return try {
            if (packages.value.isEmpty()) loadOfferings()
            val pkg = packages.value[plan] ?: return Outcome.Failure(DomainError.Billing(BillingFailure.ItemUnavailable))
            val result = Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, pkg).build())
            cache.save(toEntitlement(result.customerInfo))
            Outcome.Success(Unit)
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) Outcome.Failure(DomainError.Billing(BillingFailure.UserCancelled)) else failure(e.error)
        } catch (e: PurchasesException) {
            failure(e.error)
        }
    }

    override suspend fun restore(): Outcome<Entitlement> {
        if (!ready()) return unavailable()
        return try {
            val entitlement = toEntitlement(Purchases.sharedInstance.awaitRestore())
            cache.save(entitlement)
            Outcome.Success(entitlement)
        } catch (e: PurchasesException) {
            failure(e.error)
        }
    }

    private fun toEntitlement(info: CustomerInfo): Entitlement {
        val active = info.entitlements[PassProducts.ENTITLEMENT]?.takeIf { it.isActive }
        val plan = active?.let {
            when {
                it.productIdentifier == PassProducts.LIFETIME -> PassPlan.Lifetime
                it.productPlanIdentifier == PassProducts.BASE_MONTHLY || it.productIdentifier.endsWith(":${PassProducts.BASE_MONTHLY}") ->
                    PassPlan.Monthly
                else -> PassPlan.Annual
            }
        }
        return Entitlement(isPremium = plan != null, plan = plan, verifiedAt = clock.now())
    }

    private fun unavailable(): Outcome.Failure {
        Log.w(TAG, "RevenueCat key not configured for this build; purchases are off")
        return Outcome.Failure(DomainError.Billing(BillingFailure.ServiceUnavailable))
    }

    private fun failure(error: PurchasesError): Outcome.Failure = Outcome.Failure(
        DomainError.Billing(
            when (error.code) {
                PurchasesErrorCode.PurchaseCancelledError -> BillingFailure.UserCancelled
                PurchasesErrorCode.NetworkError,
                PurchasesErrorCode.StoreProblemError,
                PurchasesErrorCode.PurchaseNotAllowedError,
                -> BillingFailure.ServiceUnavailable
                PurchasesErrorCode.ProductNotAvailableForPurchaseError -> BillingFailure.ItemUnavailable
                PurchasesErrorCode.ProductAlreadyPurchasedError -> BillingFailure.AlreadyOwned
                PurchasesErrorCode.PaymentPendingError -> BillingFailure.Pending
                PurchasesErrorCode.ConfigurationError,
                PurchasesErrorCode.InvalidCredentialsError,
                -> BillingFailure.Developer
                else -> BillingFailure.Unknown
            },
            error.message,
        ),
    )

    private companion object {
        const val TAG = "StepSkyBilling"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds
    abstract fun entitlements(impl: RevenueCatRepository): EntitlementRepository
}
