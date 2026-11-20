package ai.idraak.stepskyscraper.feature.build.next

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import ai.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import ai.idraak.stepskyscraper.core.domain.usecase.GenerateTowerNameUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.StyleNextTowerUseCase
import ai.idraak.stepskyscraper.core.ui.component.StyleAndNamePicker
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.mvi.UiText
import ai.idraak.stepskyscraper.core.ui.mvi.toUiText
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import ai.idraak.stepskyscraper.feature.build.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object NextTowerRoute

@Immutable
data class NextTowerUiState(
    val kit: FacadeKit = FacadeKit.Deco,
    val name: String = "",
    val floors: Int = 0,
    val target: Int = 25,
    val restyleKey: Int = 0,
    val saving: Boolean = false,
    val error: UiText? = null,
)

sealed interface NextTowerIntent {
    data class SelectKit(val kit: FacadeKit) : NextTowerIntent

    data class NameChanged(val name: String) : NextTowerIntent

    data object Roll : NextTowerIntent

    data object Confirm : NextTowerIntent
}

sealed interface NextTowerEffect {
    data object Done : NextTowerEffect
}

/** "Start next tower": same style + name picker as onboarding 1d, then back to Build. */
@HiltViewModel
class NextTowerViewModel @Inject constructor(
    private val progress: GetTowerProgressUseCase,
    private val styleNext: StyleNextTowerUseCase,
    private val names: GenerateTowerNameUseCase,
    private val analytics: AnalyticsTracker,
) : MviViewModel<NextTowerUiState, NextTowerIntent, NextTowerEffect>(NextTowerUiState()) {
    init {
        viewModelScope.launch {
            val active = progress().first().city.activeTower
            reduce {
                it.copy(
                    kit = CosmeticCatalog.kitOf(active.record.look.facadeId),
                    name = active.name,
                    floors = active.floorsBuilt,
                    target = active.target,
                )
            }
        }
    }

    override fun onIntent(intent: NextTowerIntent) {
        when (intent) {
            is NextTowerIntent.SelectKit -> reduce { if (it.kit == intent.kit) it else it.copy(kit = intent.kit, restyleKey = it.restyleKey + 1) }
            is NextTowerIntent.NameChanged -> reduce { it.copy(name = intent.name) }
            NextTowerIntent.Roll -> reduce { it.copy(name = names(avoid = it.name)) }
            NextTowerIntent.Confirm -> viewModelScope.launch {
                reduce { it.copy(saving = true) }
                val result = styleNext(currentState.name, currentState.kit)
                analytics.track(AnalyticsEvent.FacadeChosen(currentState.kit))
                reduce { it.copy(saving = false, error = (result as? Outcome.Failure)?.error?.toUiText()) }
                if (result is Outcome.Success) emit(NextTowerEffect.Done)
            }
        }
    }
}

@Composable
fun NextTowerRoute(onBack: () -> Unit, onDone: () -> Unit, viewModel: NextTowerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { onDone() } }
    NextTowerScreen(state, onBack, viewModel::onIntent)
}

@Composable
fun NextTowerScreen(state: NextTowerUiState, onBack: () -> Unit, onIntent: (NextTowerIntent) -> Unit) {
    val c = SsTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Box(Modifier.padding(start = SsSpacing.s, top = SsSpacing.xs)) { SsIconButton(Symbol.ArrowBack, stringResource(R.string.next_back), onBack) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.next_title),
                style = SsTheme.type.headlineS,
                color = c.onSurface,
                modifier = Modifier.padding(horizontal = SsSpacing.onboardingMargin),
            )
            StyleAndNamePicker(
                selected = state.kit,
                name = state.name,
                previewFloors = state.floors.coerceIn(4, 11),
                previewTarget = 12,
                animationKey = state.restyleKey,
                onSelect = { onIntent(NextTowerIntent.SelectKit(it)) },
                onNameChange = { onIntent(NextTowerIntent.NameChanged(it)) },
                onDice = { onIntent(NextTowerIntent.Roll) },
            )
            state.error?.let {
                Text(it.resolve(), style = SsTheme.type.bodyXS, color = c.onSurfaceVariant, modifier = Modifier.padding(SsSpacing.onboardingMargin))
            }
        }
        SsPrimaryButton(
            stringResource(R.string.next_cta), { onIntent(NextTowerIntent.Confirm) },
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, bottom = 40.dp, top = SsSpacing.l),
            enabled = state.name.isNotBlank() && !state.saving,
        )
    }
}

@ScreenPreviews
@Composable
private fun NextTowerPreview() = PreviewSurface { NextTowerScreen(NextTowerUiState(name = "Juniper Works", floors = 2), {}, {}) }
