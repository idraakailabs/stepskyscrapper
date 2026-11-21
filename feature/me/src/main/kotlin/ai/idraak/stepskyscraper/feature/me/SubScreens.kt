package ai.idraak.stepskyscraper.feature.me

import ai.idraak.stepskyscraper.core.designsystem.component.SsBadge
import ai.idraak.stepskyscraper.core.designsystem.component.SsCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsRadioCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsSectionLabel
import ai.idraak.stepskyscraper.core.designsystem.component.SsSwitchRow
import ai.idraak.stepskyscraper.core.designsystem.component.SsTextButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsTonalButton
import ai.idraak.stepskyscraper.core.designsystem.component.cssLineBox
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.model.Appearance
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.model.TargetSuggestion
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.model.UserSettings
import ai.idraak.stepskyscraper.core.domain.repository.AdPrivacyChoices
import ai.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.domain.repository.StepsRepository
import ai.idraak.stepskyscraper.core.domain.usecase.SuggestDailyTargetUseCase
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.mvi.UiText
import ai.idraak.stepskyscraper.core.ui.mvi.toUiText
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.platform.LocalUiHost
import ai.idraak.stepskyscraper.core.ui.platform.rememberNotificationsAllowed
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object GoalsRoute

@Serializable data object SettingsRoute

@Serializable data object PrivacyRoute

/** Top bar used by every Me sub-screen: back + title (+ optional trailing text). */
@Composable
internal fun SubScreen(
    title: String,
    onBack: () -> Unit,
    trailing: String? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = SsTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(start = SsSpacing.s, end = SsSpacing.m, top = SsSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SsIconButton(Symbol.ArrowBack, stringResource(R.string.me_back), onBack)
            Text(title, style = SsTheme.type.screenTitle, color = c.onSurface, modifier = Modifier.weight(1f).semantics { heading() }.cssLineBox(SsTheme.type.screenTitle))
            if (trailing != null) Text(trailing, style = SsTheme.type.bodyXS, color = c.onSurfaceVariant)
        }
        Column(
            Modifier.weight(
                1f,
            ).verticalScroll(
                rememberScrollState(),
            ).padding(start = SsSpacing.l, end = SsSpacing.l, top = SsSpacing.s, bottom = SsSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
        if (bottom != null) {
            Box(
                Modifier.windowInsetsPadding(
                    WindowInsets.navigationBars,
                ).padding(start = SsSpacing.screenMargin, end = SsSpacing.screenMargin, bottom = 28.dp, top = SsSpacing.s),
            ) {
                bottom()
            }
        }
    }
}

// ---- Goals ---------------------------------------------------------------------------------------------

@Immutable
data class GoalsUiState(
    val selected: TargetPreset = TargetPreset.Steady,
    val custom: Int = 7_000,
    val suggestion: TargetSuggestion? = null,
    val offerRaise: Boolean = false,
    val saving: Boolean = false,
) {
    val daily: Int get() = if (selected == TargetPreset.Custom) custom else selected.dailySteps
}

sealed interface GoalsIntent {
    data class Select(val preset: TargetPreset) : GoalsIntent

    data class Adjust(val delta: Int) : GoalsIntent

    data object ApplySuggestion : GoalsIntent

    data object Save : GoalsIntent
}

/** Me › Goals: daily target (weekly goal derived) plus the adaptive raise suggestion. */
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val steps: StepsRepository,
    private val suggest: SuggestDailyTargetUseCase,
    private val clock: AppClock,
) : MviViewModel<GoalsUiState, GoalsIntent, Unit>(GoalsUiState()) {
    init {
        viewModelScope.launch {
            val goals = settings.settings.first().goals
            val today = clock.today()
            val history = (steps.days(today.minusDays(7), today.minusDays(1)) as? Outcome.Success)?.value.orEmpty()
            val s = suggest(history, today)
            reduce {
                it.copy(
                    selected = goals.preset,
                    custom = goals.dailyTargetSteps,
                    suggestion = s,
                    offerRaise = suggest.shouldOfferRaise(goals, s, today),
                )
            }
        }
    }

    override fun onIntent(intent: GoalsIntent) {
        when (intent) {
            is GoalsIntent.Select -> reduce { it.copy(selected = intent.preset) }
            is GoalsIntent.Adjust -> reduce {
                it.copy(
                    selected = TargetPreset.Custom,
                    custom = suggest.normalizeCustom(
                        it.custom + intent.delta,
                    ),
                )
            }
            GoalsIntent.ApplySuggestion -> reduce { s ->
                val target = s.suggestion?.suggestedDailySteps ?: s.daily
                val preset =
                    TargetPreset.entries.firstOrNull { it != TargetPreset.Custom && it.dailySteps == target } ?: TargetPreset.Custom
                s.copy(selected = preset, custom = target, offerRaise = false)
            }
            GoalsIntent.Save -> viewModelScope.launch {
                reduce { it.copy(saving = true) }
                settings.setDailyTarget(currentState.daily, currentState.selected)
                reduce { it.copy(saving = false) }
                emit(Unit)
            }
        }
    }
}

@Composable
fun GoalsRoute(onBack: () -> Unit, viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { onBack() } }
    GoalsScreen(state, onBack, viewModel::onIntent)
}

@Composable
fun GoalsScreen(state: GoalsUiState, onBack: () -> Unit, onIntent: (GoalsIntent) -> Unit) {
    val c = SsTheme.colors
    SubScreen(
        stringResource(R.string.goals_title),
        onBack,
        bottom = {
            SsPrimaryButton(
                stringResource(R.string.goals_save),
                { onIntent(GoalsIntent.Save) },
                Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )
        },
    ) {
        Text(stringResource(R.string.goals_weekly, state.daily / 1_000 * 7), style = SsTheme.type.title, color = c.onSurface)
        val s = state.suggestion
        if (state.offerRaise && s != null) {
            SsCard {
                Text(
                    stringResource(R.string.goals_suggestion, Formats.steps(s.sevenDayAverage), Formats.steps(s.suggestedDailySteps)),
                    style = SsTheme.type.bodyS,
                    color = c.onSurface,
                )
                SsTonalButton(stringResource(R.string.goals_apply_suggestion, Formats.steps(s.suggestedDailySteps)), {
                    onIntent(GoalsIntent.ApplySuggestion)
                }, Modifier.padding(top = SsSpacing.m))
            }
        }
        SsSectionLabel(stringResource(R.string.goals_daily))
        listOf(TargetPreset.EasyGoing, TargetPreset.Steady, TargetPreset.Ambitious).forEach { p ->
            SsRadioCard(
                title = stringResource(presetLabel(p)),
                subtitle = stringResource(R.string.me_goals_sub, Formats.steps(p.dailySteps), p.dailySteps / 1_000 * 7),
                selected = state.selected == p,
                onClick = { onIntent(GoalsIntent.Select(p)) },
                trailing = if (s?.suggestedPreset == p) ({ SsBadge(stringResource(R.string.goals_suggested)) }) else null,
            )
        }
        val custom = state.selected == TargetPreset.Custom
        SsRadioCard(
            title = stringResource(presetLabel(TargetPreset.Custom)),
            subtitle = stringResource(R.string.me_goals_sub, Formats.steps(state.custom), state.custom / 1_000 * 7),
            selected = custom,
            leading = Symbol.Tune,
            onClick = { onIntent(GoalsIntent.Select(TargetPreset.Custom)) },
            trailing = {
                Row {
                    SsIconButton(
                        Symbol.Remove,
                        stringResource(R.string.goals_less),
                        { onIntent(GoalsIntent.Adjust(-SuggestDailyTargetUseCase.CUSTOM_STEP)) },
                    )
                    SsIconButton(
                        Symbol.Add,
                        stringResource(R.string.goals_more),
                        { onIntent(GoalsIntent.Adjust(SuggestDailyTargetUseCase.CUSTOM_STEP)) },
                    )
                }
            },
        )
    }
}

private fun presetLabel(p: TargetPreset) = when (p) {
    TargetPreset.EasyGoing -> R.string.goals_easy
    TargetPreset.Steady -> R.string.goals_steady
    TargetPreset.Ambitious -> R.string.goals_ambitious
    TargetPreset.Custom -> R.string.goals_custom
}

// ---- Settings (appearance, notifications, sound, purchases) -------------------------------------------

@Immutable
data class SettingsUiState(
    val settings: UserSettings = UserSettings.Default,
    val premium: Boolean = false,
    val message: UiText? = null,
    val version: String = "",
)

sealed interface SettingsIntent {
    data class SetAppearance(val appearance: Appearance) : SettingsIntent

    data class SetToppingOut(val on: Boolean) : SettingsIntent

    data class SetWeekly(val on: Boolean) : SettingsIntent

    data class SetSound(val on: Boolean) : SettingsIntent

    data object Restore : SettingsIntent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val entitlements: EntitlementRepository,
) : MviViewModel<SettingsUiState, SettingsIntent, Unit>(SettingsUiState()) {
    init {
        viewModelScope.launch { settings.settings.collect { s -> reduce { it.copy(settings = s) } } }
        viewModelScope.launch { entitlements.entitlement.collect { e -> reduce { it.copy(premium = e.isPremium) } } }
    }

    override fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            val s = currentState.settings
            when (intent) {
                is SettingsIntent.SetAppearance -> settings.setAppearance(intent.appearance)
                is SettingsIntent.SetToppingOut -> settings.setNotifications(intent.on, s.notifyWeeklySummary)
                is SettingsIntent.SetWeekly -> settings.setNotifications(s.notifyToppingOut, intent.on)
                is SettingsIntent.SetSound -> settings.setCeremonySound(intent.on)
                SettingsIntent.Restore -> {
                    val r = entitlements.restore()
                    reduce {
                        it.copy(
                            message = when (r) {
                                is Outcome.Success -> UiText.Res(
                                    if (r.value.isPremium) R.string.settings_restored else R.string.settings_restored_none,
                                )
                                is Outcome.Failure -> r.error.toUiText()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    versionName: String,
    onOpenGallery: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(state.copy(version = versionName), onBack, viewModel::onIntent, onOpenGallery)
}

@Composable
fun SettingsScreen(state: SettingsUiState, onBack: () -> Unit, onIntent: (SettingsIntent) -> Unit, onOpenGallery: (() -> Unit)? = null) {
    val c = SsTheme.colors
    val platform = LocalPlatformActions.current
    SubScreen(stringResource(R.string.settings_title), onBack) {
        SsSectionLabel(stringResource(R.string.settings_appearance))
        Appearance.entries.forEach { a ->
            SsRadioCard(appearanceLabel(a), "", state.settings.appearance == a, { onIntent(SettingsIntent.SetAppearance(a)) })
        }
        SsSectionLabel(stringResource(R.string.me_notifications))
        // A switch reads "on" only when notifications can actually be delivered (BUG-001).
        val allowed = rememberNotificationsAllowed()
        SsCard {
            SsSwitchRow(stringResource(R.string.settings_notify_top), state.settings.notifyToppingOut && allowed, { on ->
                if (on) {
                    platform.requestNotificationPermission { granted ->
                        onIntent(
                            SettingsIntent.SetToppingOut(granted),
                        )
                    }
                } else {
                    onIntent(SettingsIntent.SetToppingOut(false))
                }
            })
            SsSwitchRow(stringResource(R.string.settings_notify_week), state.settings.notifyWeeklySummary && allowed, { on ->
                if (on) {
                    platform.requestNotificationPermission { granted ->
                        onIntent(
                            SettingsIntent.SetWeekly(granted),
                        )
                    }
                } else {
                    onIntent(SettingsIntent.SetWeekly(false))
                }
            })
            SsSwitchRow(stringResource(R.string.settings_sound), state.settings.ceremonySound, { onIntent(SettingsIntent.SetSound(it)) })
        }
        SsSectionLabel(stringResource(R.string.settings_purchases))
        SsTonalButton(stringResource(R.string.settings_restore), { onIntent(SettingsIntent.Restore) }, Modifier.fillMaxWidth())
        if (state.premium) SsTextButton(stringResource(R.string.settings_manage), platform::openPlaySubscriptions, Modifier.fillMaxWidth())
        state.message?.let { Text(it.resolve(), style = SsTheme.type.bodyXS, color = c.onSurfaceVariant) }
        Text(stringResource(R.string.settings_version, state.version), style = SsTheme.type.captionRegular, color = c.onSurfaceVariant)
        if (onOpenGallery != null) SsTextButton(stringResource(R.string.settings_gallery), onOpenGallery, Modifier.fillMaxWidth())
    }
}

// ---- Help & privacy ----------------------------------------------------------------------------------

@Immutable
data class PrivacyUiState(val adPrivacyChoices: Boolean = false)

sealed interface PrivacyIntent {
    /** Screen shown: check whether UMP requires the "Ad privacy choices" entry point. */
    data class Check(val host: UiHost) : PrivacyIntent

    data class OpenAdPrivacyChoices(val host: UiHost) : PrivacyIntent
}

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val adPrivacy: AdPrivacyChoices,
) : MviViewModel<PrivacyUiState, PrivacyIntent, Unit>(PrivacyUiState()) {
    override fun onIntent(intent: PrivacyIntent) {
        viewModelScope.launch {
            when (intent) {
                is PrivacyIntent.Check -> {
                    val required = adPrivacy.isRequired(intent.host)
                    reduce { it.copy(adPrivacyChoices = required) }
                }
                is PrivacyIntent.OpenAdPrivacyChoices -> {
                    adPrivacy.show(intent.host)
                    val required = adPrivacy.isRequired(intent.host)
                    reduce { it.copy(adPrivacyChoices = required) }
                }
            }
        }
    }
}

@Composable
fun PrivacyRoute(onBack: () -> Unit, onStepHelp: (() -> Unit)?, viewModel: PrivacyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val host = LocalUiHost.current
    LaunchedEffect(Unit) { viewModel.onIntent(PrivacyIntent.Check(host)) }
    PrivacyScreen(state, onBack, onStepHelp, onAdPrivacyChoices = { viewModel.onIntent(PrivacyIntent.OpenAdPrivacyChoices(host)) })
}

@Composable
fun PrivacyScreen(
    state: PrivacyUiState,
    onBack: () -> Unit,
    /** null when opened from Health Connect (standalone rationale screen, no app navigation). */
    onStepHelp: (() -> Unit)?,
    onAdPrivacyChoices: () -> Unit,
) {
    val c = SsTheme.colors
    val platform = LocalPlatformActions.current
    SubScreen(stringResource(R.string.privacy_title), onBack) {
        Text(stringResource(R.string.privacy_lead), style = SsTheme.type.title, color = c.onSurface)
        listOf(R.string.privacy_p1, R.string.privacy_p2, R.string.privacy_p3, R.string.privacy_p4).forEach {
            Text(stringResource(it), style = SsTheme.type.bodyS, color = c.bodySecondary)
        }
        if (onStepHelp != null) {
            SsTonalButton(stringResource(R.string.privacy_step_help), onStepHelp, Modifier.fillMaxWidth(), icon = Symbol.DirectionsWalk)
        }
        SsTextButton(stringResource(R.string.privacy_manage_access), platform::openHealthConnect, Modifier.fillMaxWidth())
        // UMP privacy options: shown only while ads are enabled and Google's consent message requires it.
        if (state.adPrivacyChoices) {
            SsTextButton(stringResource(R.string.privacy_ad_choices), onAdPrivacyChoices, Modifier.fillMaxWidth())
        }
        if (platform.hasPrivacyPolicy) {
            SsTextButton(
                stringResource(R.string.privacy_policy),
                platform::openPrivacyPolicy,
                Modifier.fillMaxWidth(),
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun GoalsPreview() = PreviewSurface {
    GoalsScreen(GoalsUiState(suggestion = TargetSuggestion(9_100, 10_000, TargetPreset.Ambitious), offerRaise = true), {}, {})
}

@ScreenPreviews
@Composable
private fun SettingsPreview() = PreviewSurface { SettingsScreen(SettingsUiState(version = "1.0.0"), {}, {}) }

@ScreenPreviews
@Composable
private fun PrivacyPreview() = PreviewSurface { PrivacyScreen(PrivacyUiState(), {}, {}, {}) }
