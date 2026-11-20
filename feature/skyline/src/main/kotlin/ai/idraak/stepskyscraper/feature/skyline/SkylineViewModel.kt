package ai.idraak.stepskyscraper.feature.skyline

import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.model.AdAvailability
import ai.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import ai.idraak.stepskyscraper.core.domain.model.District
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.Tower
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import ai.idraak.stepskyscraper.core.domain.repository.RewardedAdGateway
import ai.idraak.stepskyscraper.core.domain.repository.TowerRepository
import ai.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import ai.idraak.stepskyscraper.core.domain.usecase.CitySnapshot
import ai.idraak.stepskyscraper.core.domain.usecase.ClaimRewardedBonusUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.CompleteTowerUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.DistrictNames
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.SkyClock
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.mvi.UiText
import ai.idraak.stepskyscraper.core.ui.mvi.toUiText
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Immutable
data class RewardCard(
    val towerIndex: Int,
    val towerName: String,
    val blueprints: Int,
    val adAvailable: Boolean,
    val claimed: Boolean,
    val premium: Boolean,
)

@Immutable
data class SkylineContent(
    val districtIndex: Int,
    val districtName: String,
    val towers: List<Tower>,
    val since: LocalDate?,
    val pinnedIndex: Int,
    val newestIndex: Int?,
    val reward: RewardCard?,
    val activeTower: Tower,
    val stepsToNext: Int,
    val segmentsFilled: Int,
    val sky: SkyTime,
    val today: LocalDate,
) {
    val floors: Int get() = towers.sumOf { it.floorsBuilt }
    val inDistrict: Int get() = towers.size
}

@Immutable
data class SkylineUiState(
    val content: Lce<SkylineContent> = Lce.Loading,
    val selectedTower: Int? = null,
    val renaming: Boolean = false,
    val adBusy: Boolean = false,
    val message: UiText? = null,
)

sealed interface SkylineIntent {
    data class SelectTower(val index: Int) : SkylineIntent

    data object CloseSheet : SkylineIntent

    data class StepTower(val delta: Int) : SkylineIntent

    data object StartRename : SkylineIntent

    data object CancelRename : SkylineIntent

    data class Rename(val name: String) : SkylineIntent

    data class PrepareAd(val host: UiHost) : SkylineIntent

    data class WatchAd(val host: UiHost) : SkylineIntent

    data object ShareSkyline : SkylineIntent

    data class ShareTower(val index: Int) : SkylineIntent

    data object OpenBuild : SkylineIntent

    data object MessageShown : SkylineIntent
}

sealed interface SkylineEffect {
    data class Share(val towerIndex: Int?) : SkylineEffect

    data object OpenBuild : SkylineEffect
}

object SkylineReducer {
    /** Towers shown: finished towers of the active district (or of the last district if it is still empty). */
    fun districtTowers(snapshot: CitySnapshot): Pair<Int, List<Tower>> {
        val finished = snapshot.city.finishedTowers
        if (finished.isEmpty()) return 1 to emptyList()
        val current = snapshot.currentDistrictIndex
        val inCurrent = finished.filter { (it.index - 1) / District.TOWERS_PER_DISTRICT + 1 == current }
        return if (inCurrent.isNotEmpty()) {
            current to inCurrent
        } else {
            val last = (finished.last().index - 1) / District.TOWERS_PER_DISTRICT + 1
            last to finished.filter { (it.index - 1) / District.TOWERS_PER_DISTRICT + 1 == last }
        }
    }

    /** The newest tower keeps its amber pin for 24h after topping out. */
    fun newest(towers: List<Tower>, today: LocalDate): Int? =
        towers.lastOrNull()?.takeIf { t -> t.toppedOutOn?.let { !it.isBefore(today.minusDays(1)) } == true }?.index

    fun step(selected: Int?, towers: List<Tower>, delta: Int): Int? {
        val idx = towers.indexOfFirst { it.index == selected }
        if (idx < 0) return selected
        return towers[(idx + delta).coerceIn(0, towers.lastIndex)].index
    }
}

@HiltViewModel
class SkylineViewModel @Inject constructor(
    private val progress: GetTowerProgressUseCase,
    private val towersRepo: TowerRepository,
    private val ads: RewardedAdGateway,
    private val claim: ClaimRewardedBonusUseCase,
    private val wallet: ai.idraak.stepskyscraper.core.domain.repository.WalletRepository,
    private val analytics: AnalyticsTracker,
    private val clock: AppClock,
) : MviViewModel<SkylineUiState, SkylineIntent, SkylineEffect>(SkylineUiState()) {
    init {
        viewModelScope.launch {
            combine(progress(), ads.availability) { s, a -> s to a }
                .catch { e ->
                    reduce {
                        it.copy(
                            content = Lce.Error(
                                ai.idraak.stepskyscraper.core.domain.model.DomainError.Storage(
                                    e.message ?: "",
                                ).toUiText(),
                            ),
                        )
                    }
                }
                .collect { (s, a) ->
                    val built = build(s, a)
                    reduce { it.copy(content = Lce.Content(built)) }
                }
        }
    }

    private suspend fun build(s: CitySnapshot, ad: AdAvailability): SkylineContent {
        val (district, towers) = SkylineReducer.districtTowers(s)
        val newest = SkylineReducer.newest(towers, s.today)
        val last = towers.lastOrNull()
        val reward = last?.let { t ->
            val claimed = t.record.rewardedAdClaimed || wallet.hasEntry(CompleteTowerUseCase.adKey(t.index))
            RewardCard(
                t.index,
                t.name,
                BlueprintLadder.blueprintsFor(t.target),
                ad == AdAvailability.Ready,
                claimed,
                s.entitlement.isPremium,
            )
        }?.takeIf { newest != null }
        return SkylineContent(
            districtIndex = district,
            districtName = DistrictNames.forDistrict(district - 1),
            towers = towers,
            since = towers.firstOrNull()?.startedOn,
            pinnedIndex = towers.indexOfFirst { it.index == (currentState.selectedTower ?: newest) },
            newestIndex = newest,
            reward = reward,
            activeTower = s.city.activeTower,
            stepsToNext = s.todayProgress.stepsToNextFloor,
            segmentsFilled = s.todayProgress.segmentsFilled,
            sky = SkyClock.phaseAt(clock.localNow().toLocalTime()).dominant,
            today = s.today,
        )
    }

    override fun onIntent(intent: SkylineIntent) {
        when (intent) {
            is SkylineIntent.SelectTower -> select(intent.index)
            SkylineIntent.CloseSheet -> select(null)
            is SkylineIntent.StepTower -> {
                val towers = (currentState.content as? Lce.Content)?.value?.towers.orEmpty()
                select(SkylineReducer.step(currentState.selectedTower, towers, intent.delta))
            }
            SkylineIntent.StartRename -> reduce { it.copy(renaming = true) }
            SkylineIntent.CancelRename -> reduce { it.copy(renaming = false) }
            is SkylineIntent.Rename -> viewModelScope.launch {
                currentState.selectedTower?.let { towersRepo.rename(it, intent.name.trim().take(18)) }
                reduce { it.copy(renaming = false) }
            }
            is SkylineIntent.PrepareAd -> viewModelScope.launch {
                analytics.track(AnalyticsEvent.RewardedAdOffered)
                ads.prepare(intent.host)
            }
            is SkylineIntent.WatchAd -> watchAd(intent.host)
            SkylineIntent.ShareSkyline -> emit(SkylineEffect.Share(null))
            is SkylineIntent.ShareTower -> emit(SkylineEffect.Share(intent.index))
            SkylineIntent.OpenBuild -> emit(SkylineEffect.OpenBuild)
            SkylineIntent.MessageShown -> reduce { it.copy(message = null) }
        }
    }

    private fun select(index: Int?) = reduce { state ->
        val content = (state.content as? Lce.Content)?.value
        state.copy(
            selectedTower = index,
            content =
            content?.let { c -> Lce.Content(c.copy(pinnedIndex = c.towers.indexOfFirst { it.index == (index ?: c.newestIndex) })) }
                ?: state.content,
        )
    }

    private fun watchAd(host: UiHost) {
        val reward = (currentState.content as? Lce.Content)?.value?.reward ?: return
        if (currentState.adBusy) return
        reduce { it.copy(adBusy = true) }
        viewModelScope.launch {
            val result = claim(reward.towerIndex, host)
            if (result is Outcome.Success) analytics.track(AnalyticsEvent.RewardedAdCompleted)
            reduce {
                it.copy(
                    adBusy = false,
                    message = when (result) {
                        is Outcome.Success -> UiText.Res(R.string.sky_ad_done)
                        is Outcome.Failure -> result.error.toUiText()
                    },
                )
            }
            ads.prepare(host)
        }
    }
}
