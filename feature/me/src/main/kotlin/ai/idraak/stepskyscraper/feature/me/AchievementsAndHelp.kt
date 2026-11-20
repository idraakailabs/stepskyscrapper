package ai.idraak.stepskyscraper.feature.me

import ai.idraak.stepskyscraper.core.designsystem.component.AchievementBadge
import ai.idraak.stepskyscraper.core.designsystem.component.HelpStepCard
import ai.idraak.stepskyscraper.core.designsystem.component.RewardChip
import ai.idraak.stepskyscraper.core.designsystem.component.SsCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsInlineAction
import ai.idraak.stepskyscraper.core.designsystem.component.SsLinearBar
import ai.idraak.stepskyscraper.core.designsystem.component.SsSectionLabel
import ai.idraak.stepskyscraper.core.designsystem.component.SsTextButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsTonalButton
import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.achievements.AchievementProgress
import ai.idraak.stepskyscraper.core.domain.achievements.AchievementReward
import ai.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import ai.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import ai.idraak.stepskyscraper.core.domain.repository.StepSource
import ai.idraak.stepskyscraper.core.domain.repository.SyncStatusRepository
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.SyncStepsUseCase
import ai.idraak.stepskyscraper.core.ui.component.boldText
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.mvi.UiText
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object AchievementsRoute

@Serializable data object StepHelpRoute

// ---- 7a Achievements -----------------------------------------------------------------------------------

@Immutable
data class AchievementsUiState(val items: List<AchievementProgress> = emptyList(), val today: LocalDate = LocalDate.MIN) {
    val earned: List<AchievementProgress> get() = items.filter { it.isEarned }.sortedByDescending { it.earnedOn }
    val inProgress: List<AchievementProgress> get() = items.filter { !it.isEarned }.sortedByDescending { it.fraction }
}

@HiltViewModel
class AchievementsViewModel @Inject constructor(progress: GetTowerProgressUseCase) :
    MviViewModel<AchievementsUiState, Unit, Unit>(AchievementsUiState()) {
    init {
        viewModelScope.launch { progress().collect { s -> reduce { AchievementsUiState(s.achievements, s.today) } } }
    }

    override fun onIntent(intent: Unit) = Unit
}

@Composable
fun AchievementsRoute(onBack: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AchievementsScreen(state, onBack)
}

@Composable
fun AchievementsScreen(state: AchievementsUiState, onBack: () -> Unit) {
    SubScreen(
        stringResource(R.string.ach_title),
        onBack,
        trailing = stringResource(R.string.ach_count, state.earned.size, state.items.size),
    ) {
        if (state.inProgress.isNotEmpty()) {
            SsSectionLabel(
                stringResource(R.string.ach_in_progress),
                Modifier.padding(start = SsSpacing.xs, top = SsSpacing.xs),
            )
        }
        state.inProgress.forEach { AchievementRow(it, state.today) }
        if (state.earned.isNotEmpty()) {
            SsSectionLabel(
                stringResource(R.string.ach_earned),
                Modifier.padding(start = SsSpacing.xs, top = SsSpacing.s),
            )
        }
        state.earned.forEach { AchievementRow(it, state.today) }
    }
}

@Composable
private fun AchievementRow(a: AchievementProgress, today: LocalDate) {
    val c = SsTheme.colors
    val reward = when (val r = a.definition.reward) {
        is AchievementReward.Blueprints -> stringResource(R.string.ach_blueprints_reward, r.amount)
        is AchievementReward.Cosmetic -> r.label
    }
    Row(
        Modifier.fillMaxWidth().background(
            c.surface,
            SsRadius.shapeCard,
        ).padding(horizontal = SsSpacing.l, vertical = 14.dp).semantics(mergeDescendants = true) {
        },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = if (a.isEarned) Alignment.CenterVertically else Alignment.Top,
    ) {
        val justEarned = a.earnedOn?.let { !it.isBefore(today.minusDays(7)) } == true
        AchievementBadge(Symbol.named(a.definition.icon), a.isEarned, size = 52.dp, radius = SsRadius.md, flip = a.earnedOn == today)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (a.isEarned) SsSpacing.xs else 6.dp)) {
            Text(a.definition.title, style = SsTheme.type.bodyMStrong, color = c.onSurface)
            Text(a.definition.description, style = SsTheme.type.bodyXS, color = c.bodySecondary)
            val earnedOn = a.earnedOn
            if (earnedOn != null) {
                val date = if (earnedOn == today) stringResource(R.string.ach_today) else Formats.dayMonth(earnedOn)
                Text(stringResource(R.string.ach_earned_on, date, reward), style = SsTheme.type.captionRegular, color = c.onSurfaceVariant)
                if (justEarned &&
                    a.definition.reward is AchievementReward.Cosmetic
                ) {
                    RewardChip(stringResource(R.string.ach_equip), symbol = Symbol.Palette)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                    SsLinearBar(a.fraction, Modifier.weight(1f), height = 6.dp, radius = 3.dp)
                    Text(
                        stringResource(R.string.ach_progress, a.progress, a.definition.target),
                        style = SsTheme.type.captionStrong,
                        color = c.onSurface,
                    )
                }
                RewardChip(reward)
            }
        }
    }
}

// ---- 10a Step-source help -----------------------------------------------------------------------------

enum class HelpStatus { Granted, Denied, Missing, NeedsUpdate, Sensor }

@Immutable
data class StepHelpUiState(
    val lastData: Instant? = null,
    val zone: ZoneId = ZoneId.systemDefault(),
    val status: HelpStatus = HelpStatus.Granted,
    val samsungFirst: Boolean = false,
    val checking: Boolean = false,
    val result: UiText? = null,
    val lastSync: Instant? = null,
)

sealed interface StepHelpIntent {
    data object CheckAgain : StepHelpIntent

    data object Refresh : StepHelpIntent
}

@HiltViewModel
class StepHelpViewModel @Inject constructor(
    private val source: StepSource,
    private val syncStatus: SyncStatusRepository,
    private val sync: SyncStepsUseCase,
    private val analytics: AnalyticsTracker,
    private val clock: AppClock,
) : MviViewModel<StepHelpUiState, StepHelpIntent, Unit>(StepHelpUiState()) {
    init {
        analytics.track(AnalyticsEvent.StepHelpOpened)
        viewModelScope.launch {
            syncStatus.status.collect { s ->
                reduce {
                    it.copy(
                        lastData = s.lastDataChangeAt,
                        lastSync = s.lastSuccessAt,
                        zone = clock.zone(),
                    )
                }
            }
        }
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        val status = when (val st = source.status()) {
            StepSourceStatus.HealthConnectReady -> HelpStatus.Granted
            StepSourceStatus.HealthConnectPermissionMissing, StepSourceStatus.Unavailable -> HelpStatus.Denied
            StepSourceStatus.HealthConnectNotInstalled -> HelpStatus.Missing
            StepSourceStatus.HealthConnectNeedsUpdate -> HelpStatus.NeedsUpdate
            is StepSourceStatus.Sensor -> if (st.permissionGranted) HelpStatus.Sensor else HelpStatus.Denied
        }
        reduce { it.copy(status = status) }
    }

    fun setSamsungInstalled(installed: Boolean) = reduce { it.copy(samsungFirst = installed) }

    override fun onIntent(intent: StepHelpIntent) {
        when (intent) {
            StepHelpIntent.Refresh -> refresh()
            StepHelpIntent.CheckAgain -> viewModelScope.launch {
                reduce { it.copy(checking = true, result = null) }
                val before = syncStatus.status.first().lastDataChangeAt
                val r = sync()
                refresh()
                val after = syncStatus.status.first().lastDataChangeAt
                val steps = (r as? Outcome.Success)?.value?.todaySteps ?: 0
                reduce {
                    it.copy(
                        checking = false,
                        result = if (r is Outcome.Success &&
                            (steps > 0 || after != before)
                        ) {
                            UiText.Res(R.string.help_result_ok, listOf(Formats.steps(steps)))
                        } else {
                            UiText.Res(R.string.help_result_none)
                        },
                    )
                }
            }
        }
    }

    /** Diagnostic report: sync times and source status only, never step data. */
    fun diagnostic(): String = with(currentState) {
        "Step Skyscraper diagnostic\nSource status: $status\nLast successful sync: ${lastSync ?: "never"}\nLast new data: ${lastData ?: "never"}\nGenerated: ${clock.now()}"
    }
}

@Composable
fun StepHelpRoute(onBack: () -> Unit, viewModel: StepHelpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalPlatformActions.current
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.setSamsungInstalled(platform.isSamsungHealthInstalled()) }
    androidx.lifecycle.compose.LifecycleEventEffect(
        androidx.lifecycle.Lifecycle.Event.ON_RESUME,
    ) { viewModel.onIntent(StepHelpIntent.Refresh) }
    StepHelpScreen(state, onBack, viewModel::onIntent) { platform.sendDiagnostic(viewModel.diagnostic()) }
}

@Composable
fun StepHelpScreen(state: StepHelpUiState, onBack: () -> Unit, onIntent: (StepHelpIntent) -> Unit, onDiagnostic: () -> Unit) {
    val c = SsTheme.colors
    val platform = LocalPlatformActions.current
    SubScreen(
        stringResource(R.string.help_title),
        onBack,
        bottom = {
            SsTonalButton(
                stringResource(if (state.checking) R.string.help_checking else R.string.help_check_again),
                { onIntent(StepHelpIntent.CheckAgain) },
                Modifier.fillMaxWidth(),
                icon = Symbol.Sync,
                strong = true,
                enabled = !state.checking,
            )
        },
    ) {
        SsCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = SsSpacing.l, vertical = 14.dp)) {
            val last = state.lastData
            Text(
                if (last !=
                    null
                ) {
                    boldText(stringResource(R.string.help_last_data, Formats.weekdayDateTime(last, state.zone)), c.onSurface)
                } else {
                    boldText(stringResource(R.string.help_no_data_yet))
                },
                style = SsTheme.type.bodyS,
                color = c.onSurface,
            )
            Row(
                Modifier.padding(top = SsSpacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SsSpacing.xs),
            ) {
                SsIcon(Symbol.Info, size = IconSize.s, tint = c.onInfoContainer)
                Text(
                    stringResource(
                        when (state.status) {
                            HelpStatus.Granted -> R.string.help_status_granted
                            HelpStatus.Denied -> R.string.help_status_denied
                            HelpStatus.Missing -> R.string.help_status_missing
                            HelpStatus.NeedsUpdate -> R.string.help_status_update
                            HelpStatus.Sensor -> R.string.help_status_sensor
                        },
                    ),
                    style = SsTheme.type.captionStrong,
                    color = c.onInfoContainer,
                )
            }
            state.result?.let {
                Text(it.resolve(), style = SsTheme.type.bodyXS, color = c.bodySecondary, modifier = Modifier.padding(top = SsSpacing.s))
            }
        }
        val hc: @Composable (Int) -> Unit = { n ->
            val missing = state.status == HelpStatus.Missing || state.status == HelpStatus.NeedsUpdate
            HelpStepCard(
                n,
                stringResource(R.string.help_hc_title),
                body = {
                    Text(
                        boldText(stringResource(R.string.help_hc_body), c.onSurface),
                        style = SsTheme.type.bodyXS,
                        color = c.bodySecondary,
                    )
                },
                action = {
                    if (missing) {
                        SsInlineAction(stringResource(R.string.help_hc_install), platform::installHealthConnect)
                    } else {
                        SsInlineAction(stringResource(R.string.help_hc_action), platform::openHealthConnect)
                    }
                },
            )
        }
        val samsung: @Composable (Int) -> Unit = { n ->
            HelpStepCard(
                n,
                stringResource(R.string.help_samsung_title),
                body = {
                    Text(
                        boldText(stringResource(R.string.help_samsung_body), c.onSurface),
                        style = SsTheme.type.bodyXS,
                        color = c.bodySecondary,
                    )
                },
                action = { SsInlineAction(stringResource(R.string.help_samsung_action), platform::openSamsungHealth) },
            )
        }
        // Steps reorder by likelihood: Samsung first when Samsung Health is installed.
        if (state.samsungFirst) {
            samsung(1)
            hc(2)
        } else {
            hc(1)
            samsung(2)
        }
        HelpStepCard(
            3,
            stringResource(R.string.help_battery_title),
            body = {
                Text(
                    boldText(stringResource(R.string.help_battery_body), c.onSurface),
                    style = SsTheme.type.bodyXS,
                    color = c.bodySecondary,
                )
            },
            action = { SsInlineAction(stringResource(R.string.help_battery_action), platform::openBatterySettings) },
        )
        SsTextButton(stringResource(R.string.help_still_stuck), onDiagnostic, Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.help_diag_note),
            style = SsTheme.type.captionRegular,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@ScreenPreviews
@Composable
private fun AchievementsPreview() = PreviewSurface {
    val today = LocalDate.of(2026, 11, 20)
    val items = ai.idraak.stepskyscraper.core.domain.achievements.AchievementCatalog.all.mapIndexed { i, d ->
        AchievementProgress(d, if (i % 3 == 0) d.target else d.target / 2, if (i % 3 == 0) today.minusDays(i.toLong()) else null)
    }
    AchievementsScreen(AchievementsUiState(items, today), {})
}

@ScreenPreviews
@Composable
private fun StepHelpPreview() = PreviewSurface {
    StepHelpScreen(StepHelpUiState(lastData = Instant.parse("2026-11-18T21:40:00Z"), zone = ZoneId.of("UTC")), {}, {}, {})
}
