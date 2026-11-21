package ai.idraak.stepskyscraper.core.ads

import ai.idraak.stepskyscraper.core.domain.model.UiHost
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** An ad loaded by [AdSdk.loadRewarded]; opaque outside the SDK wrapper. */
interface RewardedHandle

enum class ShowResult { Earned, Dismissed, Failed }

/**
 * Thin seam over the Google Mobile Ads SDK and UMP so the gating logic in [AdMobRewardedGateway] can be unit
 * tested without the SDK. Every call that reaches Google is listed here; nothing else in the app calls it.
 */
interface AdSdk {
    /** UMP: whether consent (from this or an earlier session) allows ad requests. */
    fun canRequestAds(): Boolean

    /** UMP `requestConsentInfoUpdate`: refreshes the consent status. Never shows UI. */
    suspend fun updateConsentInfo(host: UiHost): Boolean

    /** UMP `loadAndShowConsentFormIfRequired`: may show the consent form. Only after a user tap. */
    suspend fun showConsentFormIfRequired(host: UiHost)

    /** UMP: whether the user must be offered a way to change consent (`PrivacyOptionsRequirementStatus.REQUIRED`). */
    fun privacyOptionsRequired(): Boolean

    /** UMP `showPrivacyOptionsForm`. Only after a user tap. */
    suspend fun showPrivacyOptionsForm(host: UiHost)

    /** `MobileAds.initialize`, only after consent allows ad requests. */
    suspend fun initialize()

    /** One ad request. Carries no custom targeting or keywords (no step data reaches AdMob). */
    suspend fun loadRewarded(unitId: String): RewardedHandle?

    /** Shows the ad; [ShowResult.Earned] only if `onUserEarnedReward` fired before dismissal. */
    suspend fun show(handle: RewardedHandle, host: UiHost): ShowResult
}

@Singleton
class GoogleAdSdk @Inject constructor(
    @ApplicationContext private val context: Context,
    private val env: AdsEnvironment,
) : AdSdk {
    private val consent: ConsentInformation by lazy { UserMessagingPlatform.getConsentInformation(context) }

    private class Handle(val ad: RewardedAd) : RewardedHandle

    override fun canRequestAds(): Boolean = consent.canRequestAds()

    override suspend fun updateConsentInfo(host: UiHost): Boolean {
        val activity = host as? Activity ?: return false
        val params = ConsentRequestParameters.Builder().apply {
            debugSettings()?.let { setConsentDebugSettings(it) }
        }.build()
        return suspendCancellableCoroutine { cont ->
            consent.requestConsentInfoUpdate(
                activity,
                params,
                { if (cont.isActive) cont.resume(true) },
                { error ->
                    AdsLog.i("consent update failed code=${error.errorCode}")
                    if (cont.isActive) cont.resume(false)
                },
            )
        }
    }

    override suspend fun showConsentFormIfRequired(host: UiHost) {
        val activity = host as? Activity ?: return
        AdsLog.i("consent form if required (after tap)")
        suspendCancellableCoroutine { cont ->
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) AdsLog.i("consent form error code=${error.errorCode}")
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    override fun privacyOptionsRequired(): Boolean =
        consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override suspend fun showPrivacyOptionsForm(host: UiHost) {
        val activity = host as? Activity ?: return
        AdsLog.i("privacy options form (after tap)")
        suspendCancellableCoroutine { cont ->
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) AdsLog.i("privacy options form error code=${error.errorCode}")
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    override suspend fun initialize() {
        if (!env.isProdRelease && env.testDeviceHashedId.isNotBlank()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(listOf(env.testDeviceHashedId)).build(),
            )
        }
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont -> MobileAds.initialize(context) { if (cont.isActive) cont.resume(Unit) } }
        }
    }

    override suspend fun loadRewarded(unitId: String): RewardedHandle? = suspendCancellableCoroutine { cont ->
        RewardedAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    if (cont.isActive) cont.resume(Handle(loaded))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdsLog.i("load failed code=${error.code}")
                    if (cont.isActive) cont.resume(null)
                }
            },
        )
    }

    override suspend fun show(handle: RewardedHandle, host: UiHost): ShowResult {
        val activity = host as? Activity ?: return ShowResult.Failed
        val ad = (handle as? Handle)?.ad ?: return ShowResult.Failed
        var earned = false
        return suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (cont.isActive) cont.resume(if (earned) ShowResult.Earned else ShowResult.Dismissed)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    AdsLog.i("show failed code=${error.code}")
                    if (cont.isActive) cont.resume(ShowResult.Failed)
                }
            }
            ad.show(activity) {
                earned = true
                AdsLog.i("onUserEarnedReward")
            }
        }
    }

    /** Debug builds only: UMP debug geography / test device so the consent form can be verified. */
    private fun debugSettings(): ConsentDebugSettings? {
        if (env.isProdRelease) return null
        if (!env.umpDebugGeographyEea && env.testDeviceHashedId.isBlank()) return null
        return ConsentDebugSettings.Builder(context).apply {
            if (env.umpDebugGeographyEea) setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            if (env.testDeviceHashedId.isNotBlank()) addTestDeviceHashedId(env.testDeviceHashedId)
        }.build()
    }
}
