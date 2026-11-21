package ai.idraak.stepskyscraper.core.data.repository

import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.repository.LadderRepository
import ai.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Blueprint ladder from Firebase Remote Config key `tower_ladder` ("10,25,50,100,160,250").
 * The bundled default is used offline, on malformed values, and when Firebase is not configured.
 * [refresh] fetches all Remote Config values (including the `ads_enabled` switch) through [RemoteConfigSource].
 */
@Singleton
class RemoteConfigLadderRepository @Inject constructor(
    private val source: RemoteConfigSource,
) : LadderRepository {
    override val ladder: Flow<List<Int>> = source.values.map { parse(it.ladderRaw) }.distinctUntilChanged()

    override suspend fun refresh(): Outcome<Unit> = source.refresh()

    companion object {
        const val KEY = RemoteConfigSource.KEY_LADDER

        fun parse(raw: String?): List<Int> =
            BlueprintLadder.sanitize(raw?.split(',')?.mapNotNull { it.trim().toIntOrNull() })
    }
}
