package ai.idraak.stepskyscraper.feature.build.ceremony

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import ai.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.TowerLook
import ai.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.domain.repository.TowerRepository
import ai.idraak.stepskyscraper.core.domain.usecase.CompleteTowerUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.mvi.UiText
import ai.idraak.stepskyscraper.feature.build.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class CeremonyRoute(val towerIndex: Int)

@Immutable
data class CeremonyContent(
    val towerIndex: Int,
    val name: String,
    val look: TowerLook,
    val floors: Int,
    val buildDays: Int,
    val steps: Long,
    val blueprints: Int,
)

@Immutable
data class CeremonyUiState(val content: Lce<CeremonyContent> = Lce.Loading, val sheetVisible: Boolean = false)

sealed interface CeremonyIntent {
    data object RevealSheet : CeremonyIntent

    data object Share : CeremonyIntent

    data object StartNext : CeremonyIntent

    data object Close : CeremonyIntent
}

sealed interface CeremonyEffect {
    data class Share(val towerIndex: Int) : CeremonyEffect

    /** [showPaywall]: only after the first finished tower, once, after the ceremony is dismissed. */
    data class StartNext(val showPaywall: Boolean) : CeremonyEffect

    data class Close(val towerIndex: Int, val showPaywall: Boolean) : CeremonyEffect
}

/** Topping-out ceremony. Shown once per tower; no ads here, ever. */
@HiltViewModel
class CeremonyViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val progress: GetTowerProgressUseCase,
    private val completeTower: CompleteTowerUseCase,
    private val towers: TowerRepository,
    private val settings: SettingsRepository,
    private val analytics: AnalyticsTracker,
) : MviViewModel<CeremonyUiState, CeremonyIntent, CeremonyEffect>(CeremonyUiState()) {
    private val towerIndex = savedState.toRoute<CeremonyRoute>().towerIndex

    init {
        analytics.track(AnalyticsEvent.ToppingOutShown)
        viewModelScope.launch {
            val tower = progress().first().city.towers.firstOrNull { it.index == towerIndex && it.isComplete }
            if (tower == null) {
                reduce { it.copy(content = Lce.Error(UiText.Res(R.string.build_error_title))) }
                return@launch
            }
            val blueprints = (completeTower(tower) as? Outcome.Success)?.value ?: 0
            reduce {
                it.copy(
                    content = Lce.Content(
                        CeremonyContent(tower.index, tower.name, tower.record.look, tower.floorsBuilt, tower.buildDays, tower.steps, blueprints),
                    ),
                )
            }
        }
    }

    override fun onIntent(intent: CeremonyIntent) {
        when (intent) {
            CeremonyIntent.RevealSheet -> reduce { it.copy(sheetVisible = true) }
            CeremonyIntent.Share -> emit(CeremonyEffect.Share(towerIndex))
            CeremonyIntent.StartNext -> dismiss { emit(CeremonyEffect.StartNext(it)) }
            CeremonyIntent.Close -> dismiss { emit(CeremonyEffect.Close(towerIndex, it)) }
        }
    }

    private fun dismiss(then: (Boolean) -> Unit) = viewModelScope.launch {
        towers.markCeremonySeen(towerIndex)
        val s = progress().first()
        val showPaywall = !s.settings.paywallShownAfterFirstTower && !s.entitlement.isPremium
        if (showPaywall) settings.setPaywallShownAfterFirstTower()
        then(showPaywall)
    }
}
