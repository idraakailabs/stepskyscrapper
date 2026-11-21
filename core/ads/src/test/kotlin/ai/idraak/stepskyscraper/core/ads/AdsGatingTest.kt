package ai.idraak.stepskyscraper.core.ads

import ai.idraak.stepskyscraper.core.domain.model.AdAvailability
import ai.idraak.stepskyscraper.core.domain.model.AdFailure
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.repository.AdsRemoteSwitch
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val LIVE_APP = "ca-app-pub-1234567890123456~1234567890"
private const val LIVE_UNIT = "ca-app-pub-1234567890123456/9876543210"

private val debugEnv = AdsEnvironment(isProdRelease = false, liveAppId = LIVE_APP, liveRewardedUnitId = LIVE_UNIT)
private val releaseEnv = AdsEnvironment(isProdRelease = true, liveAppId = LIVE_APP, liveRewardedUnitId = LIVE_UNIT)

private class FakeSwitch(initial: Boolean) : AdsRemoteSwitch {
    val value = MutableStateFlow(initial)
    var reads = 0
    override val adsEnabled: Flow<Boolean> = flow {
        reads++
        value.collect { emit(it) }
    }
}

/** Records every SDK call; never touches Google. */
private class RecordingSdk(var consented: Boolean = true, val consentAfterForm: Boolean = true) : AdSdk {
    val calls = mutableListOf<String>()
    val loadedUnits = mutableListOf<String>()
    var fill = true
    var showResult = ShowResult.Earned

    override fun canRequestAds(): Boolean = consented

    override suspend fun updateConsentInfo(host: UiHost): Boolean {
        calls += "updateConsent"
        return true
    }

    override suspend fun showConsentFormIfRequired(host: UiHost) {
        calls += "consentForm"
        consented = consentAfterForm
    }

    var privacyRequired = false

    override fun privacyOptionsRequired(): Boolean = privacyRequired

    override suspend fun showPrivacyOptionsForm(host: UiHost) {
        calls += "privacyForm"
    }

    override suspend fun initialize() {
        calls += "initialize"
    }

    override suspend fun loadRewarded(unitId: String): RewardedHandle? {
        calls += "load"
        loadedUnits += unitId
        return if (fill) object : RewardedHandle {} else null
    }

    override suspend fun show(handle: RewardedHandle, host: UiHost): ShowResult {
        calls += "show"
        return showResult
    }

    /** Anything that reaches the network (consent refresh, init, ad requests). */
    val sdkTouched get() = calls.isNotEmpty()
}

private val host = object : UiHost {}

class AdsConfigResolverTest {
    @Test
    fun `debug always uses Google test ids even when live ids are configured`() {
        assertThat(AdsConfigResolver.resolve(debugEnv)).isEqualTo(AdSetup.Test)
        assertThat(AdSetup.Test.rewardedUnitId).isEqualTo("ca-app-pub-3940256099942544/5224354917")
        assertThat(AdsConfigResolver.resolve(debugEnv.copy(liveAppId = "", liveRewardedUnitId = ""))).isEqualTo(AdSetup.Test)
    }

    @Test
    fun `release with valid live ids uses them`() {
        assertThat(AdsConfigResolver.resolve(releaseEnv)).isEqualTo(AdSetup.Live(LIVE_UNIT))
    }

    @Test
    fun `release without valid live ids is not configured`() {
        listOf(
            releaseEnv.copy(liveAppId = "", liveRewardedUnitId = ""),
            releaseEnv.copy(liveRewardedUnitId = ""),
            releaseEnv.copy(liveAppId = AdMobIds.TEST_APP_ID, liveRewardedUnitId = AdMobIds.TEST_REWARDED_UNIT_ID),
            releaseEnv.copy(liveRewardedUnitId = AdMobIds.TEST_REWARDED_UNIT_ID),
            releaseEnv.copy(liveAppId = "ca-app-pub-1234567890123456/1234567890"),
            releaseEnv.copy(liveRewardedUnitId = "not-an-id"),
        ).forEach { assertThat(AdsConfigResolver.resolve(it)).isEqualTo(AdSetup.NotConfigured) }
    }
}

class AdsGateTest {
    @Test
    fun `debug ignores Remote Config and is always on with test ids`() = runTest {
        val switch = FakeSwitch(false)
        val gate = AdsGate(debugEnv, switch)
        assertThat(gate.currentUnitId()).isEqualTo(AdMobIds.TEST_REWARDED_UNIT_ID)
        assertThat(gate.enabled.first()).isTrue()
        assertThat(switch.reads).isEqualTo(0)
    }

    @Test
    fun `release follows the remote switch and defaults off`() = runTest {
        val switch = FakeSwitch(false)
        val gate = AdsGate(releaseEnv, switch)
        assertThat(gate.currentUnitId()).isNull()
        switch.value.value = true
        assertThat(gate.currentUnitId()).isEqualTo(LIVE_UNIT)
        switch.value.value = false
        assertThat(gate.enabled.first()).isFalse()
    }

    @Test
    fun `release without live ids stays off even when the switch is on`() = runTest {
        val gate = AdsGate(releaseEnv.copy(liveRewardedUnitId = ""), FakeSwitch(true))
        assertThat(gate.currentUnitId()).isNull()
    }
}

class AdMobRewardedGatewayTest {
    private var now = 0L

    private fun gateway(env: AdsEnvironment, switch: AdsRemoteSwitch, sdk: AdSdk) = AdMobRewardedGateway(AdsGate(env, switch), sdk) { now }

    @Test
    fun `debug loads the test unit even with the remote switch off`() = runTest {
        val sdk = RecordingSdk()
        val ads = gateway(debugEnv, FakeSwitch(false), sdk)
        ads.prepare(host)
        assertThat(sdk.loadedUnits).containsExactly(AdMobIds.TEST_REWARDED_UNIT_ID)
        assertThat(ads.availability.first()).isEqualTo(AdAvailability.Ready)
    }

    @Test
    fun `release with remote switch off makes no SDK calls and reports Disabled`() = runTest {
        val sdk = RecordingSdk()
        val ads = gateway(releaseEnv, FakeSwitch(false), sdk)
        ads.prepare(host)
        val shown = ads.show(host)
        assertThat(sdk.sdkTouched).isFalse()
        assertThat(ads.availability.first()).isEqualTo(AdAvailability.Disabled)
        assertThat((shown as Outcome.Failure).error).isEqualTo(DomainError.AdUnavailable(AdFailure.Disabled))
    }

    @Test
    fun `release with Remote Config unavailable (never emits true) makes no SDK calls`() = runTest {
        // Firebase missing or fetch failed: the switch stays at its default, false.
        val sdk = RecordingSdk()
        val ads = gateway(releaseEnv, FakeSwitch(initial = false), sdk)
        repeat(3) { ads.prepare(host) }
        assertThat(sdk.sdkTouched).isFalse()
    }

    @Test
    fun `release without live ids makes no SDK calls even with the switch on`() = runTest {
        val sdk = RecordingSdk()
        val ads = gateway(releaseEnv.copy(liveAppId = "", liveRewardedUnitId = ""), FakeSwitch(true), sdk)
        ads.prepare(host)
        ads.show(host)
        assertThat(sdk.sdkTouched).isFalse()
        assertThat(ads.availability.first()).isEqualTo(AdAvailability.Disabled)
    }

    @Test
    fun `release with remote switch on requests the live unit`() = runTest {
        val sdk = RecordingSdk()
        val ads = gateway(releaseEnv, FakeSwitch(true), sdk)
        ads.prepare(host)
        assertThat(sdk.loadedUnits).containsExactly(LIVE_UNIT)
        assertThat(ads.show(host)).isEqualTo(Outcome.Success(Unit))
    }

    @Test
    fun `switch turned off after loading discards the ad and stops requests`() = runTest {
        val switch = FakeSwitch(true)
        val sdk = RecordingSdk()
        val ads = gateway(releaseEnv, switch, sdk)
        ads.prepare(host)
        switch.value.value = false
        val shown = ads.show(host)
        assertThat((shown as Outcome.Failure).error).isEqualTo(DomainError.AdUnavailable(AdFailure.Disabled))
        assertThat(sdk.calls).doesNotContain("show")
        assertThat(sdk.loadedUnits).hasSize(1)
    }

    @Test
    fun `prepare never shows the consent form and does not request without consent (BUG-007)`() = runTest {
        val sdk = RecordingSdk(consented = false)
        val ads = gateway(debugEnv, FakeSwitch(true), sdk)
        ads.prepare(host)
        assertThat(sdk.calls).containsExactly("updateConsent")
        assertThat(ads.availability.first()).isEqualTo(AdAvailability.Unavailable)
    }

    @Test
    fun `tap gathers consent before initialising and loading`() = runTest {
        val sdk = RecordingSdk(consented = false)
        val ads = gateway(debugEnv, FakeSwitch(true), sdk)
        assertThat(ads.show(host)).isEqualTo(Outcome.Success(Unit))
        assertThat(sdk.calls).containsExactly("updateConsent", "consentForm", "initialize", "load", "show").inOrder()
    }

    @Test
    fun `no consent after the form means no init and no request`() = runTest {
        val sdk = RecordingSdk(consented = false, consentAfterForm = false)
        val ads = gateway(debugEnv, FakeSwitch(true), sdk)
        val shown = ads.show(host)
        assertThat((shown as Outcome.Failure).error).isEqualTo(DomainError.AdUnavailable(AdFailure.NoConsent))
        assertThat(sdk.calls).containsNoneOf("initialize", "load", "show")
        assertThat(ads.availability.first()).isEqualTo(AdAvailability.ConsentRequired)
    }

    @Test
    fun `reward is granted only when the SDK reports it earned`() = runTest {
        val sdk = RecordingSdk().apply { showResult = ShowResult.Dismissed }
        val ads = gateway(debugEnv, FakeSwitch(true), sdk)
        val shown = ads.show(host)
        assertThat((shown as Outcome.Failure).error).isEqualTo(DomainError.AdUnavailable(AdFailure.Dismissed))
    }

    @Test
    fun `a preloaded ad is reused and the SDK initialises once`() = runTest {
        val sdk = RecordingSdk()
        val ads = gateway(debugEnv, FakeSwitch(true), sdk)
        ads.prepare(host)
        ads.prepare(host)
        ads.show(host)
        assertThat(sdk.calls.count { it == "load" }).isEqualTo(1)
        assertThat(sdk.calls.count { it == "initialize" }).isEqualTo(1)
    }

    @Test
    fun `an ad older than its expiry is reloaded`() = runTest {
        val sdk = RecordingSdk()
        val ads = gateway(debugEnv, FakeSwitch(true), sdk)
        ads.prepare(host)
        now += 56L * 60 * 1_000_000_000
        ads.show(host)
        assertThat(sdk.calls.count { it == "load" }).isEqualTo(2)
    }
}

class AdMobPrivacyChoicesTest {
    @Test
    fun `ads off means no entry point and no consent request`() = runTest {
        val sdk = RecordingSdk().apply { privacyRequired = true }
        val choices = AdMobPrivacyChoices(AdsGate(releaseEnv, FakeSwitch(false)), sdk)
        assertThat(choices.isRequired(host)).isFalse()
        choices.show(host)
        assertThat(sdk.sdkTouched).isFalse()
    }

    @Test
    fun `ads on follows the UMP requirement and shows the form only on request`() = runTest {
        val sdk = RecordingSdk()
        val choices = AdMobPrivacyChoices(AdsGate(releaseEnv, FakeSwitch(true)), sdk)
        assertThat(choices.isRequired(host)).isFalse()
        sdk.privacyRequired = true
        assertThat(choices.isRequired(host)).isTrue()
        assertThat(sdk.calls).containsExactly("updateConsent", "updateConsent")
        choices.show(host)
        assertThat(sdk.calls.last()).isEqualTo("privacyForm")
    }
}
