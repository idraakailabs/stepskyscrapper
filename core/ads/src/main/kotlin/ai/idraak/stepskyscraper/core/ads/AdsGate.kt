package ai.idraak.stepskyscraper.core.ads

import ai.idraak.stepskyscraper.core.domain.repository.AdsRemoteSwitch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Decides whether ads may be requested right now and with which unit id:
 * - [AdSetup.Test] (every debug variant): always on, test unit id, Remote Config never read.
 * - [AdSetup.Live] (Prod Release): on only while Remote Config `ads_enabled` is true (default false).
 * - [AdSetup.NotConfigured]: always off.
 */
@Singleton
class AdsGate @Inject constructor(
    env: AdsEnvironment,
    remote: AdsRemoteSwitch,
) {
    val setup: AdSetup = AdsConfigResolver.resolve(env)

    /** The unit id to request while enabled, or null when ads are off. */
    val rewardedUnitId: Flow<String?> = when (val s = setup) {
        AdSetup.Test -> flowOf(AdSetup.Test.rewardedUnitId)
        is AdSetup.Live -> remote.adsEnabled.map { on -> if (on) s.rewardedUnitId else null }
        AdSetup.NotConfigured -> flowOf(null)
    }.distinctUntilChanged().onEach { unit -> log(unit) }

    val enabled: Flow<Boolean> = rewardedUnitId.map { it != null }.distinctUntilChanged()

    suspend fun currentUnitId(): String? = rewardedUnitId.first()

    @Volatile private var lastLog: String? = null

    /** One line per change of state, not per read. */
    private fun log(unit: String?) {
        val mode = when (setup) {
            AdSetup.Test -> "Test"
            is AdSetup.Live -> "Live"
            AdSetup.NotConfigured -> "NotConfigured"
        }
        val ids = when (setup) {
            AdSetup.Test -> "test"
            is AdSetup.Live -> "live"
            AdSetup.NotConfigured -> "none"
        }
        val line = "gate mode=$mode enabled=${unit != null} ids=$ids unit=${unit ?: "-"}"
        if (line == lastLog) return
        lastLog = line
        AdsLog.i(line)
    }
}
