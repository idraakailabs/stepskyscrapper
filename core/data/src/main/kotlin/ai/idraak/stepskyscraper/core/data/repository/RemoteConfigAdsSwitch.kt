package ai.idraak.stepskyscraper.core.data.repository

import ai.idraak.stepskyscraper.core.domain.repository.AdsRemoteSwitch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Remote Config boolean `ads_enabled` (default false). False until a fetch activates true, and always false
 * without Firebase. Only the Prod Release build consults it; see `core:ads` `AdsGate`.
 */
@Singleton
class RemoteConfigAdsSwitch @Inject constructor(
    source: RemoteConfigSource,
) : AdsRemoteSwitch {
    override val adsEnabled: Flow<Boolean> = source.values.map { it.adsEnabled }.distinctUntilChanged()
}
