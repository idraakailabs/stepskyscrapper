package ai.idraak.stepskyscraper.feature.me

import ai.idraak.stepskyscraper.core.designsystem.component.SsCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsLinearBar
import ai.idraak.stepskyscraper.core.designsystem.component.SsSettingsRow
import ai.idraak.stepskyscraper.core.designsystem.component.SsSwitch
import ai.idraak.stepskyscraper.core.designsystem.component.cssLineBox
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.GoalPause
import ai.idraak.stepskyscraper.core.domain.model.Goals
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.model.WeekProgress
import ai.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.PauseGoalsUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.PauseOption
import ai.idraak.stepskyscraper.core.ui.component.SystemBarsEffect
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.mvi.toUiText
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Build › week card: this week's bars, the goal and "Pause goals" (BUG-004). */
@Serializable data object WeekRoute

@Immutable
data class WeekContent(val week: WeekProgress, val goals: Goals)

@Immutable
data class WeekUiState(val content: Lce<WeekContent> = Lce.Loading, val pauseSheet: Boolean = false)

sealed interface WeekIntent {
    data object TogglePause : WeekIntent

    data object DismissPause : WeekIntent

    data class Pause(val option: PauseOption) : WeekIntent
}

@HiltViewModel
class WeekViewModel @Inject constructor(
    progress: GetTowerProgressUseCase,
    private val pause: PauseGoalsUseCase,
    private val analytics: AnalyticsTracker,
) : MviViewModel<WeekUiState, WeekIntent, Nothing>(WeekUiState()) {
    init {
        viewModelScope.launch {
            progress()
                .catch { e -> reduce { it.copy(content = Lce.Error(DomainError.Storage(e.message ?: "").toUiText())) } }
                .collect { s -> reduce { it.copy(content = Lce.Content(WeekContent(s.week, s.settings.goals))) } }
        }
    }

    override fun onIntent(intent: WeekIntent) {
        when (intent) {
            WeekIntent.TogglePause -> {
                val paused = (currentState.content as? Lce.Content)?.value?.goals?.pause != GoalPause.None
                if (paused) {
                    viewModelScope.launch { pause.resume() }
                } else {
                    reduce { it.copy(pauseSheet = true) }
                }
            }
            WeekIntent.DismissPause -> reduce { it.copy(pauseSheet = false) }
            is WeekIntent.Pause -> viewModelScope.launch {
                pause.pause(intent.option)
                analytics.track(AnalyticsEvent.GoalsPaused)
                reduce { it.copy(pauseSheet = false) }
            }
        }
    }
}

@Composable
fun WeekRoute(onBack: () -> Unit, onOpenGoals: () -> Unit, viewModel: WeekViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeekScreen(state, onBack, onOpenGoals, viewModel::onIntent)
}

@Composable
fun WeekScreen(state: WeekUiState, onBack: () -> Unit, onOpenGoals: () -> Unit, onIntent: (WeekIntent) -> Unit) {
    // Opened from Build, whose sky sets light status-bar icons: this screen sits on the light background.
    SystemBarsEffect(lightIcons = SsTheme.colors.isDark)
    SubScreen(stringResource(R.string.me_this_week), onBack) {
        (state.content as? Lce.Content)?.value?.let { WeekBody(it, onOpenGoals, onIntent) }
    }
    if (state.pauseSheet) PauseSheet({ onIntent(WeekIntent.Pause(it)) }, { onIntent(WeekIntent.DismissPause) })
}

@Composable
private fun WeekBody(content: WeekContent, onOpenGoals: () -> Unit, onIntent: (WeekIntent) -> Unit) {
    val c = SsTheme.colors
    val week = content.week
    val g = content.goals
    SsCard(padding = PaddingValues(horizontal = 18.dp, vertical = SsSpacing.l)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Formats.steps(week.floors), style = SsTheme.type.numberM, color = c.onSurface, modifier = Modifier.cssLineBox(SsTheme.type.numberM))
            Text(
                stringResource(R.string.me_of_floors, week.goalFloors),
                style = SsTheme.type.bodyM,
                color = c.bodySecondary,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }
        Box(Modifier.height(SsSpacing.s))
        SsLinearBar(week.fraction)
        Box(Modifier.height(SsSpacing.s))
        Text(
            when {
                week.paused -> stringResource(R.string.week_paused)
                week.goalReached -> stringResource(R.string.week_reached)
                else -> stringResource(R.string.week_to_go, week.floorsToGo)
            },
            style = SsTheme.type.bodyXS,
            color = c.bodySecondary,
        )
        Box(Modifier.height(14.dp))
        Bars(week.days, month = false)
    }
    Column(Modifier.background(c.surface, SsRadius.shapeCard).padding(vertical = SsSpacing.xs)) {
        SsSettingsRow(
            Symbol.PauseCircle,
            stringResource(R.string.me_pause),
            when (val p = g.pause) {
                is GoalPause.Until -> stringResource(R.string.me_pause_until, Formats.dayMonth(p.lastPausedDay))
                is GoalPause.Indefinite -> stringResource(R.string.me_pause_indefinite)
                GoalPause.None -> stringResource(R.string.me_pause_sub)
            },
            { onIntent(WeekIntent.TogglePause) },
            trailing = { SsSwitch(g.pause != GoalPause.None) },
        )
        SsSettingsRow(
            Symbol.Flag,
            stringResource(R.string.me_goals),
            stringResource(R.string.me_goals_sub, Formats.steps(g.dailyTargetSteps), g.weeklyFloors),
            onOpenGoals,
            showDivider = false,
        )
    }
}

@ScreenPreviews
@Composable
private fun WeekPreview() = PreviewSurface {
    val me = previewMe()
    val week = WeekProgress(LocalDate.of(2026, 11, 16), me.weekFloors, me.weekGoal, 3, false, me.week)
    WeekScreen(
        WeekUiState(Lce.Content(WeekContent(week, Goals(7_000, TargetPreset.Steady, GoalPause.None)))),
        {},
        {},
        {},
    )
}
