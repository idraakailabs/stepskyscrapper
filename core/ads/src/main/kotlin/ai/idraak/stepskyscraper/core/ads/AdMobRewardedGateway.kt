package ai.idraak.stepskyscraper.core.ads

import ai.idraak.stepskyscraper.core.domain.model.AdAvailability
import ai.idraak.stepskyscraper.core.domain.model.AdFailure
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.repository.RewardedAdGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single opt-in rewarded ad (doubles a finished tower's Blueprints; never floors).
 *
 * - Every SDK call is behind [AdsGate]: while ads are disabled there is no consent request, no SDK init and no
 *   ad request, and [availability] reports [AdAvailability.Disabled] so the UI hides the offer.
 * - [prepare] (reward card visible) never shows the consent form (BUG-007): it refreshes the consent status and
 *   preloads only if consent already allows requests.
 * - [show] (user tap) gathers consent first, initialises the SDK after consent, loads if needed, and succeeds
 *   only when the SDK reported `onUserEarnedReward`.
 * - Requests carry no custom targeting. Only the Skyline reward card calls this; Build, onboarding and the
 *   topping-out ceremony never do.
 */
@Singleton
class AdMobRewardedGateway internal constructor(
    private val gate: AdsGate,
    private val sdk: AdSdk,
    private val nanoTime: () -> Long,
) : RewardedAdGateway {
    @Inject constructor(gate: AdsGate, sdk: AdSdk) : this(gate, sdk, System::nanoTime)

    private val state = MutableStateFlow(AdAvailability.Unavailable)
    private val mutex = Mutex()
    private var initialized = false
    private var ad: RewardedHandle? = null
    private var loadedAt = 0L

    override val availability: Flow<AdAvailability> =
        combine(gate.enabled, state) { on, s -> if (on) s else AdAvailability.Disabled }.distinctUntilChanged()

    override suspend fun prepare(host: UiHost): Unit = mutex.withLock {
        val unit = gate.currentUnitId()
        if (unit == null) {
            discard()
            AdsLog.i("prepare skipped: ads disabled")
            return@withLock
        }
        if (cachedAd() != null) {
            state.value = AdAvailability.Ready
            return@withLock
        }
        sdk.updateConsentInfo(host)
        if (!sdk.canRequestAds()) {
            // No form here: consent is gathered only when the user taps the ad button (BUG-007).
            AdsLog.i("prepare: consent not given yet; waiting for tap")
            state.value = AdAvailability.Unavailable
            return@withLock
        }
        ensureInitialized()
        load(unit)
    }

    override suspend fun show(host: UiHost): Outcome<Unit> = mutex.withLock {
        val unit = gate.currentUnitId() ?: return@withLock disabled()
        if (!sdk.canRequestAds()) {
            sdk.updateConsentInfo(host)
            sdk.showConsentFormIfRequired(host)
            if (!sdk.canRequestAds()) {
                AdsLog.i("show: consent does not allow ad requests")
                state.value = AdAvailability.ConsentRequired
                return@withLock Outcome.Failure(DomainError.AdUnavailable(AdFailure.NoConsent))
            }
        }
        ensureInitialized()
        val current = cachedAd() ?: load(unit)
            ?: return@withLock Outcome.Failure(DomainError.AdUnavailable(AdFailure.NoFill))
        // The switch may have flipped while loading or while the consent form was open.
        if (gate.currentUnitId() == null) return@withLock disabled()
        ad = null
        state.value = AdAvailability.Unavailable
        AdsLog.i("show")
        when (sdk.show(current, host)) {
            ShowResult.Earned -> Outcome.Success(Unit)
            ShowResult.Dismissed -> Outcome.Failure(DomainError.AdUnavailable(AdFailure.Dismissed))
            ShowResult.Failed -> Outcome.Failure(DomainError.AdUnavailable(AdFailure.NoFill))
        }
    }

    private fun disabled(): Outcome<Unit> {
        discard()
        AdsLog.i("show refused: ads disabled")
        return Outcome.Failure(DomainError.AdUnavailable(AdFailure.Disabled))
    }

    private fun discard() {
        ad = null
        state.value = AdAvailability.Unavailable
    }

    /** A loaded ad younger than [AD_TTL_NANOS]; rewarded ads expire an hour after loading. */
    private fun cachedAd(): RewardedHandle? {
        val current = ad ?: return null
        if (nanoTime() - loadedAt < AD_TTL_NANOS) return current
        ad = null
        return null
    }

    private suspend fun ensureInitialized() {
        if (initialized) return
        AdsLog.i("MobileAds.initialize (after consent)")
        sdk.initialize()
        initialized = true
    }

    private suspend fun load(unit: String): RewardedHandle? {
        state.value = AdAvailability.Loading
        AdsLog.i("load request unit=$unit")
        val loaded = sdk.loadRewarded(unit)
        ad = loaded
        loadedAt = nanoTime()
        state.value = if (loaded != null) AdAvailability.Ready else AdAvailability.Unavailable
        AdsLog.i(if (loaded != null) "loaded" else "no ad loaded")
        return loaded
    }

    private companion object {
        const val AD_TTL_NANOS = 55L * 60 * 1_000_000_000
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {
    @Binds
    abstract fun rewarded(impl: AdMobRewardedGateway): RewardedAdGateway

    @Binds
    abstract fun sdk(impl: GoogleAdSdk): AdSdk
}
