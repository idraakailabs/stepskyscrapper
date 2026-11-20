package ai.idraak.stepskyscraper.core.ads

import ai.idraak.stepskyscraper.core.domain.model.AdAvailability
import ai.idraak.stepskyscraper.core.domain.model.AdFailure
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.repository.RewardedAdGateway
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The single opt-in rewarded ad. Consent is gathered with UMP before the SDK is initialised or any ad is
 * requested; the ad request carries no custom targeting (step data never reaches AdMob). Only the Skyline
 * reward card calls [prepare]; Build, onboarding and the topping-out ceremony never do.
 */
@Singleton
class AdMobRewardedGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : RewardedAdGateway {
    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val initialized = AtomicBoolean(false)
    private val state = MutableStateFlow(AdAvailability.Unavailable)
    private var ad: RewardedAd? = null

    override val availability: Flow<AdAvailability> = state.asStateFlow()

    override suspend fun prepare(host: UiHost) {
        val activity = host as? Activity ?: return
        if (!gatherConsent(activity)) {
            state.value = AdAvailability.ConsentRequired
            return
        }
        if (initialized.compareAndSet(false, true)) {
            suspendCancellableCoroutine { cont -> MobileAds.initialize(context) { cont.resume(Unit) } }
        }
        load()
    }

    private suspend fun gatherConsent(activity: Activity): Boolean {
        val updated = suspendCancellableCoroutine { cont ->
            consent.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                { cont.resume(true) },
                { cont.resume(false) },
            )
        }
        if (updated) {
            suspendCancellableCoroutine { cont ->
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { cont.resume(Unit) }
            }
        }
        return consent.canRequestAds()
    }

    private suspend fun load() {
        if (ad != null) {
            state.value = AdAvailability.Ready
            return
        }
        state.value = AdAvailability.Loading
        ad = suspendCancellableCoroutine { cont ->
            RewardedAd.load(
                context,
                BuildConfig.REWARDED_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(loaded: RewardedAd) {
                        cont.resume(loaded)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        cont.resume(null)
                    }
                },
            )
        }
        state.value = if (ad != null) AdAvailability.Ready else AdAvailability.Unavailable
    }

    override suspend fun show(host: UiHost): Outcome<Unit> {
        val activity = host as? Activity ?: return Outcome.Failure(DomainError.AdUnavailable(AdFailure.NotLoaded))
        if (!consent.canRequestAds()) return Outcome.Failure(DomainError.AdUnavailable(AdFailure.NoConsent))
        val current = ad ?: return Outcome.Failure(DomainError.AdUnavailable(AdFailure.NotLoaded))
        ad = null
        state.value = AdAvailability.Unavailable
        var earned = false
        val result = suspendCancellableCoroutine { cont ->
            current.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (cont.isActive) {
                        cont.resume(if (earned) Outcome.Success(Unit) else Outcome.Failure(DomainError.AdUnavailable(AdFailure.Dismissed)))
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (cont.isActive) cont.resume(Outcome.Failure(DomainError.AdUnavailable(AdFailure.NoFill)))
                }
            }
            current.show(activity) { earned = true }
        }
        return result
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {
    @Binds
    abstract fun rewarded(impl: AdMobRewardedGateway): RewardedAdGateway
}
