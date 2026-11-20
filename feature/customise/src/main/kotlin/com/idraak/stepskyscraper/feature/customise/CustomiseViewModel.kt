package com.idraak.stepskyscraper.feature.customise

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCategory
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticItem
import com.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.TowerLook
import com.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import com.idraak.stepskyscraper.core.domain.repository.UnlockRepository
import com.idraak.stepskyscraper.core.domain.usecase.CustomiseTowerUseCase
import com.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import com.idraak.stepskyscraper.core.domain.usecase.LockState
import com.idraak.stepskyscraper.core.domain.usecase.ResolveCosmeticLockUseCase
import com.idraak.stepskyscraper.core.domain.usecase.UnlockContext
import com.idraak.stepskyscraper.core.domain.usecase.UnlockCosmeticUseCase
import com.idraak.stepskyscraper.core.ui.mvi.Lce
import com.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import com.idraak.stepskyscraper.core.ui.mvi.UiText
import com.idraak.stepskyscraper.core.ui.mvi.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class CustomiseRoute(val towerIndex: Int)

@Immutable
data class CosmeticTile(val item: CosmeticItem, val lock: LockState)

@Immutable
data class CustomiseContent(
    val towerIndex: Int,
    val towerName: String,
    val floors: Int,
    val target: Int,
    val saved: TowerLook,
    val tiles: Map<CosmeticCategory, List<CosmeticTile>>,
    val blueprints: Int,
)

@Immutable
data class CustomiseUiState(
    val content: Lce<CustomiseContent> = Lce.Loading,
    val draft: TowerLook? = null,
    val tab: CosmeticCategory = CosmeticCategory.Facade,
    val previewing: CosmeticItem? = null,
    val explaining: CosmeticTile? = null,
    val saving: Boolean = false,
    val message: UiText? = null,
    val reskinKey: Int = 0,
) {
    /** What the tower shows right now: a 3s locked preview wins over the draft. */
    val shownLook: TowerLook? get() = draft?.let { d -> previewing?.let { CustomiseReducer.apply(d, it) } ?: d }
}

sealed interface CustomiseIntent {
    data class SelectTab(val tab: CosmeticCategory) : CustomiseIntent

    data class TapItem(val tile: CosmeticTile) : CustomiseIntent

    data object DismissExplainer : CustomiseIntent

    data class BuyWithBlueprints(val itemId: String) : CustomiseIntent

    data object OpenPass : CustomiseIntent

    data object Done : CustomiseIntent

    data object MessageShown : CustomiseIntent
}

sealed interface CustomiseEffect {
    data object Close : CustomiseEffect

    data object OpenPaywall : CustomiseEffect
}

object CustomiseReducer {
    fun apply(look: TowerLook, item: CosmeticItem): TowerLook = when (item.category) {
        CosmeticCategory.Facade -> look.copy(facadeId = item.id)
        CosmeticCategory.Palette -> look.copy(paletteId = item.id)
        CosmeticCategory.Crown -> look.copy(crownId = item.id)
        CosmeticCategory.Lights -> look.copy(lightsId = item.id)
        CosmeticCategory.Ground -> look.copy(groundId = item.id)
    }

    fun isSelected(look: TowerLook?, item: CosmeticItem): Boolean = look != null && when (item.category) {
        CosmeticCategory.Facade -> look.facadeId == item.id
        CosmeticCategory.Palette -> look.paletteId == item.id
        CosmeticCategory.Crown -> look.crownId == item.id
        CosmeticCategory.Lights -> look.lightsId == item.id
        CosmeticCategory.Ground -> look.groundId == item.id
    }

    fun tiles(context: UnlockContext, resolve: ResolveCosmeticLockUseCase): Map<CosmeticCategory, List<CosmeticTile>> =
        CosmeticCategory.entries.associateWith { cat -> CosmeticCatalog.inCategory(cat).map { CosmeticTile(it, resolve(it, context)) } }
}

@HiltViewModel
class CustomiseViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val progress: GetTowerProgressUseCase,
    private val unlocks: UnlockRepository,
    private val resolve: ResolveCosmeticLockUseCase,
    private val customise: CustomiseTowerUseCase,
    private val unlock: UnlockCosmeticUseCase,
    private val analytics: AnalyticsTracker,
) : MviViewModel<CustomiseUiState, CustomiseIntent, CustomiseEffect>(CustomiseUiState()) {
    private val towerIndex = savedState.toRoute<CustomiseRoute>().towerIndex
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            combine(progress(), unlocks.purchasedItemIds) { s, purchased -> s to purchased }
                .catch { e ->
                    reduce {
                        it.copy(
                            content = Lce.Error(
                                com.idraak.stepskyscraper.core.domain.model.DomainError.Storage(
                                    e.message ?: "",
                                ).toUiText(),
                            ),
                        )
                    }
                }
                .collect { (s, purchased) ->
                    val tower = s.city.towers.firstOrNull { it.index == towerIndex } ?: s.city.activeTower
                    val context = UnlockContext(
                        s.stats,
                        s.entitlement.isPremium,
                        purchased,
                        s.achievements.filter { it.isEarned }.map { it.definition.id }.toSet(),
                        s.blueprints,
                    )
                    reduce {
                        it.copy(
                            content = Lce.Content(
                                CustomiseContent(
                                    tower.index,
                                    tower.name,
                                    tower.floorsBuilt,
                                    tower.target,
                                    tower.record.look,
                                    CustomiseReducer.tiles(context, resolve),
                                    s.blueprints,
                                ),
                            ),
                            draft = it.draft ?: tower.record.look,
                        )
                    }
                }
        }
    }

    override fun onIntent(intent: CustomiseIntent) {
        when (intent) {
            is CustomiseIntent.SelectTab -> reduce { it.copy(tab = intent.tab) }
            is CustomiseIntent.TapItem -> tap(intent.tile)
            CustomiseIntent.DismissExplainer -> reduce { it.copy(explaining = null, previewing = null) }
            is CustomiseIntent.BuyWithBlueprints -> viewModelScope.launch {
                when (val r = unlock(intent.itemId)) {
                    is Outcome.Success -> {
                        analytics.track(AnalyticsEvent.CosmeticUnlocked(CosmeticCatalog.item(intent.itemId)?.category?.name.orEmpty()))
                        val item = CosmeticCatalog.item(intent.itemId)
                        reduce { s ->
                            s.copy(
                                explaining = null,
                                previewing = null,
                                draft =
                                item?.let { i -> s.draft?.let { CustomiseReducer.apply(it, i) } } ?: s.draft,
                            )
                        }
                    }
                    is Outcome.Failure -> reduce { it.copy(message = r.error.toUiText()) }
                }
            }
            CustomiseIntent.OpenPass -> {
                reduce { it.copy(explaining = null, previewing = null) }
                emit(CustomiseEffect.OpenPaywall)
            }
            CustomiseIntent.Done -> save()
            CustomiseIntent.MessageShown -> reduce { it.copy(message = null) }
        }
    }

    private fun tap(tile: CosmeticTile) {
        if (tile.lock == LockState.Unlocked) {
            reduce { s ->
                val next = s.draft?.let { CustomiseReducer.apply(it, tile.item) }
                s.copy(draft = next, reskinKey = if (tile.item.category == CosmeticCategory.Facade) s.reskinKey + 1 else s.reskinKey)
            }
            return
        }
        // Locked: preview it on the tower for 3s with a "Preview" label, then explain how to unlock it.
        previewJob?.cancel()
        reduce { it.copy(previewing = tile.item) }
        previewJob = viewModelScope.launch {
            delay(PREVIEW_MS)
            if (tile.lock == LockState.Premium) {
                reduce { it.copy(previewing = null) }
                emit(CustomiseEffect.OpenPaywall)
            } else {
                reduce { it.copy(explaining = tile) }
            }
        }
    }

    private fun save() {
        val draft = currentState.draft ?: return emit(CustomiseEffect.Close)
        reduce { it.copy(saving = true) }
        viewModelScope.launch {
            val result = customise(towerIndex, draft)
            reduce { it.copy(saving = false, message = (result as? Outcome.Failure)?.error?.toUiText()) }
            if (result is Outcome.Success) emit(CustomiseEffect.Close)
        }
    }

    private companion object {
        const val PREVIEW_MS = 3_000L
    }
}
