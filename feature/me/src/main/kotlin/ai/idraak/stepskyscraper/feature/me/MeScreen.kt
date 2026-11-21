package ai.idraak.stepskyscraper.feature.me

import ai.idraak.stepskyscraper.core.designsystem.component.AchievementBadge
import ai.idraak.stepskyscraper.core.designsystem.component.NextFloorPill
import ai.idraak.stepskyscraper.core.designsystem.component.SsCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsLinearBar
import ai.idraak.stepskyscraper.core.designsystem.component.SsMessageState
import ai.idraak.stepskyscraper.core.designsystem.component.SsModalSheet
import ai.idraak.stepskyscraper.core.designsystem.component.SsSegmentedToggle
import ai.idraak.stepskyscraper.core.designsystem.component.SsSettingsRow
import ai.idraak.stepskyscraper.core.designsystem.component.SsSwitch
import ai.idraak.stepskyscraper.core.designsystem.component.SsTextButton
import ai.idraak.stepskyscraper.core.designsystem.component.cssLineBox
import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.model.Appearance
import ai.idraak.stepskyscraper.core.domain.model.GoalPause
import ai.idraak.stepskyscraper.core.domain.model.WeekDayBar
import ai.idraak.stepskyscraper.core.domain.usecase.PauseOption
import ai.idraak.stepskyscraper.core.ui.R as UiR
import ai.idraak.stepskyscraper.core.ui.component.SystemBarsEffect
import ai.idraak.stepskyscraper.core.ui.component.boldText
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.format.updatedAgoLower
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import ai.idraak.stepskyscraper.core.ui.platform.rememberNotificationsAllowed
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Navigation callbacks out of the Me tab. */
data class MeNavigation(
    val goals: () -> Unit,
    val achievements: () -> Unit,
    val stepSource: () -> Unit,
    val settings: () -> Unit,
    val help: () -> Unit,
    val paywall: () -> Unit,
)

@Composable
fun MeRoute(nav: MeNavigation, viewModel: MeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshSource() }
    MeScreen(state, nav, viewModel::onIntent)
}

@Composable
fun MeScreen(state: MeUiState, nav: MeNavigation, onIntent: (MeIntent) -> Unit) {
    val c = SsTheme.colors
    SystemBarsEffect(lightIcons = c.isDark)
    Box(Modifier.fillMaxSize().background(c.background)) {
        when (val content = state.content) {
            Lce.Loading -> Unit
            is Lce.Error -> SsMessageState(Symbol.Person, content.message.resolve(), "", Modifier.align(Alignment.Center))
            is Lce.Content -> MeContentView(content.value, state, nav, onIntent)
        }
    }
    if (state.pauseSheet) PauseSheet({ onIntent(MeIntent.Pause(it)) }, { onIntent(MeIntent.DismissPause) })
}

@Composable
private fun MeContentView(content: MeContent, state: MeUiState, nav: MeNavigation, onIntent: (MeIntent) -> Unit) {
    val c = SsTheme.colors
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = SsSpacing.screenMargin, end = SsSpacing.s, top = SsSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.me_title), style = SsTheme.type.headline, color = c.onSurface, modifier = Modifier.weight(1f).cssLineBox(SsTheme.type.headline))
            SsIconButton(Symbol.Settings, stringResource(R.string.me_settings), nav.settings)
        }
        NextFloorPill(
            content.stepsToNext,
            content.segmentsFilled,
            stringResource(UiR.string.to_next_floor),
            stringResource(UiR.string.next_floor_in_steps, Formats.steps(content.stepsToNext)),
            Modifier.padding(horizontal = SsSpacing.screenMargin),
        )
        Column(
            Modifier.padding(start = SsSpacing.l, end = SsSpacing.l, top = SsSpacing.m, bottom = SsSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(SsSpacing.m),
        ) {
            LevelCard(content)
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                StatCard(stringResource(R.string.me_build_days), content.buildDays, true, Modifier.weight(1.3f))
                StatCard(stringResource(R.string.me_floors), content.floors, false, Modifier.weight(1f))
                StatCard(stringResource(R.string.me_towers), content.towers, false, Modifier.weight(1f))
            }
            FloorsChart(content, state.showMonth) { onIntent(MeIntent.ShowMonth(it)) }
            LastWeeks(content)
            AchievementsCard(content, nav.achievements)
            PassCard(content, nav.paywall)
            SettingsList(content, nav, onIntent)
        }
    }
}

@Composable
private fun LevelCard(content: MeContent) {
    val c = SsTheme.colors
    val level = content.level
    SsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).background(SsTheme.brand.passSurface, SsRadius.shapeMd), contentAlignment = Alignment.Center) {
                Text(
                    level.level.toString(),
                    style = SsTheme.type.numberXS.copy(fontSize = SsTheme.type.headlineS.fontSize),
                    color = SsTheme.brand.onPassSurface,
                    modifier = Modifier.cssLineBox(SsTheme.type.numberXS.copy(fontSize = SsTheme.type.headlineS.fontSize)),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.me_level_overline, level.level).uppercase(),
                    style = SsTheme.type.overline,
                    color = c.onSurfaceVariant,
                )
                Text(level.title, style = SsTheme.type.sheetTitle, color = c.onSurface)
            }
        }
        Box(Modifier.height(SsSpacing.m))
        SsLinearBar(level.fraction)
        Box(Modifier.height(SsSpacing.m))
        val next = level.nextLevelAtFloors
        Text(
            if (next != null && level.nextTitle != null) {
                boldText(
                    stringResource(R.string.me_level_next, level.totalFloors, next, level.level + 1, level.nextTitle.orEmpty()),
                    c.onSurface,
                )
            } else {
                boldText(stringResource(R.string.me_level_max))
            },
            style = SsTheme.type.bodyXS,
            color = c.bodySecondary,
        )
    }
}

@Composable
private fun StatCard(label: String, value: Int, large: Boolean, modifier: Modifier) {
    val c = SsTheme.colors
    Column(
        modifier.background(
            c.surface,
            SsRadius.shapeCard,
        ).padding(horizontal = SsSpacing.l, vertical = 14.dp).semantics(mergeDescendants = true) {
        },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = SsTheme.type.captionStrong, color = c.onSurfaceVariant, maxLines = 1)
        val numberStyle = if (large) SsTheme.type.numberStat else SsTheme.type.numberS
        Text(Formats.steps(value), style = numberStyle, color = c.onSurface, maxLines = 1, modifier = Modifier.cssLineBox(numberStyle))
    }
}

@Composable
private fun FloorsChart(content: MeContent, month: Boolean, onToggle: (Boolean) -> Unit) {
    val c = SsTheme.colors
    SsCard(padding = PaddingValues(horizontal = 18.dp, vertical = SsSpacing.l)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.me_floors_built),
                style = SsTheme.type.bodyStrong,
                color = c.onSurface,
                modifier = Modifier.weight(1f),
            )
            SsSegmentedToggle(listOf(stringResource(R.string.me_week), stringResource(R.string.me_month)), if (month) 1 else 0, {
                onToggle(
                    it == 1,
                )
            }, large = false)
        }
        Box(Modifier.height(14.dp))
        val bars = if (month) content.month else content.week
        val total = bars.sumOf { it.floors }
        Text(
            stringResource(if (month) R.string.me_this_month else R.string.me_this_week),
            style = SsTheme.type.bodyXS,
            color = c.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Formats.steps(total), style = SsTheme.type.numberM, color = c.onSurface, modifier = Modifier.cssLineBox(SsTheme.type.numberM))
            Text(
                if (month) stringResource(R.string.me_floors_month) else stringResource(R.string.me_of_floors, content.weekGoal),
                style = SsTheme.type.bodyM,
                color = c.bodySecondary,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }
        Box(Modifier.height(14.dp))
        Bars(bars, month)
    }
}

@Composable
internal fun Bars(bars: List<WeekDayBar>, month: Boolean) {
    val c = SsTheme.colors
    val max = (bars.maxOfOrNull { it.floors } ?: 0).coerceAtLeast(1)
    val chartHeight = 88.dp
    Row(
        // At least the design's 120dp; the tallest day needs 124dp (value + 88dp bar + label), so don't clip it.
        Modifier.fillMaxWidth().heightIn(min = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(if (month) 2.dp else SsSpacing.s),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            val today = stringResource(R.string.me_today)
            Column(
                Modifier.weight(1f).clearAndSetSemantics {
                    contentDescription =
                        "${Formats.weekdayDayMonth(bar.date)}: ${bar.floors}"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SsSpacing.xs, Alignment.Bottom),
            ) {
                if (!bar.isFuture && !month) {
                    Text(
                        bar.floors.toString(),
                        style = if (bar.isToday) SsTheme.type.microBold else SsTheme.type.microStrong,
                        color = c.onSurface,
                    )
                }
                val h: Dp = if (bar.isFuture) 8.dp else (chartHeight * (bar.floors.toFloat() / max)).coerceAtLeast(4.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(h)
                        .then(
                            if (bar.isFuture) {
                                Modifier.border(1.dp, c.barNeutral, RoundedCornerShape(6.dp))
                            } else {
                                Modifier.background(
                                    if (bar.isToday) c.primary else c.barNeutral,
                                    RoundedCornerShape(if (month) 2.dp else 6.dp),
                                )
                            },
                        ),
                )
                if (!month) {
                    Text(
                        if (bar.isToday) today else Formats.weekdayInitial(bar.date),
                        style = if (bar.isToday) SsTheme.type.microBold else SsTheme.type.micro,
                        color = if (bar.isToday) c.onSurface else c.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LastWeeks(content: MeContent) {
    val c = SsTheme.colors
    SsCard(padding = PaddingValues(horizontal = 18.dp, vertical = SsSpacing.l)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                stringResource(R.string.me_last_weeks),
                style = SsTheme.type.bodyStrong,
                color = c.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.me_goal_per_week, content.weekGoal), style = SsTheme.type.bodyXS, color = c.onSurfaceVariant)
        }
        Box(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
            content.lastWeeks.forEach { w ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        Formats.dayMonth(w.start),
                        style = if (w.isCurrent) SsTheme.type.labelSBold else SsTheme.type.bodyXS,
                        color = if (w.isCurrent) c.onSurface else c.onSurfaceVariant,
                        modifier = Modifier.width(56.dp),
                    )
                    SsLinearBar(
                        if (w.goalFloors ==
                            0
                        ) {
                            0f
                        } else {
                            w.floors.toFloat() / w.goalFloors
                        },
                        Modifier.weight(1f),
                        height = 14.dp,
                        radius = 5.dp,
                        fill = if (w.isCurrent) c.primary else c.barNeutral,
                    )
                    Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterEnd) {
                        if (w.paused && !w.isCurrent) {
                            SsIcon(
                                Symbol.Pause,
                                size = IconSize.s,
                                tint = c.onSurfaceVariant,
                                contentDescription = stringResource(R.string.me_paused_week_cd, w.floors),
                            )
                        } else {
                            Text(w.floors.toString(), style = SsTheme.type.labelSBold, color = c.onSurface, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
        Box(Modifier.height(14.dp))
        Text(stringResource(R.string.me_paused_weeks), style = SsTheme.type.captionRegular, color = c.onSurfaceVariant)
    }
}

@Composable
private fun AchievementsCard(content: MeContent, onSeeAll: () -> Unit) {
    val c = SsTheme.colors
    SsCard(padding = PaddingValues(horizontal = 18.dp, vertical = SsSpacing.l)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.me_achievements),
                style = SsTheme.type.bodyStrong,
                color = c.onSurface,
                modifier = Modifier.weight(1f),
            )
            SsTextButton(stringResource(R.string.me_see_all, content.achievements.size), onSeeAll, strong = true)
        }
        Box(Modifier.height(SsSpacing.xs))
        content.achievements.take(6).chunked(3).forEach { row ->
            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a ->
                    Column(
                        Modifier.weight(1f).semantics(mergeDescendants = true) {
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AchievementBadge(Symbol.named(a.definition.icon), a.isEarned)
                        Text(
                            a.definition.title,
                            style = if (a.isEarned) SsTheme.type.captionStrong else SsTheme.type.captionRegular,
                            color = c.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        if (!a.isEarned) {
                            Text(
                                stringResource(R.string.ach_progress, a.progress, a.definition.target),
                                style = SsTheme.type.captionRegular,
                                color = c.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassCard(content: MeContent, onClick: () -> Unit) {
    val brand = SsTheme.brand
    Row(
        Modifier
            .fillMaxWidth()
            .background(brand.passSurface, SsRadius.shapeCard)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SsSpacing.screenMargin, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SsIcon(Symbol.WorkspacePremium, size = IconSize.xxl, tint = brand.passAccent, filled = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.me_pass_title), style = SsTheme.type.bodyStrong, color = brand.onPassSurface)
            Text(
                if (content.premium) {
                    stringResource(
                        R.string.me_pass_active,
                    )
                } else {
                    stringResource(R.string.me_pass_body, content.monthlyPrice)
                },
                style = SsTheme.type.bodyXS,
                color = brand.passSubtle,
            )
        }
        SsIcon(Symbol.ChevronRight, size = IconSize.ml, tint = brand.passSubtle)
    }
}

@Composable
private fun SettingsList(content: MeContent, nav: MeNavigation, onIntent: (MeIntent) -> Unit) {
    val c = SsTheme.colors
    val g = content.goals
    Column(Modifier.background(c.surface, SsRadius.shapeCard).padding(vertical = SsSpacing.xs)) {
        SsSettingsRow(
            Symbol.Flag,
            stringResource(R.string.me_goals),
            stringResource(R.string.me_goals_sub, Formats.steps(g.dailyTargetSteps), g.weeklyFloors),
            nav.goals,
        )
        val paused = g.pause != GoalPause.None
        SsSettingsRow(
            Symbol.PauseCircle,
            stringResource(R.string.me_pause),
            when (val p = g.pause) {
                is GoalPause.Until -> stringResource(R.string.me_pause_until, Formats.dayMonth(p.lastPausedDay))
                is GoalPause.Indefinite -> stringResource(R.string.me_pause_indefinite)
                GoalPause.None -> stringResource(R.string.me_pause_sub)
            },
            { onIntent(MeIntent.TogglePause) },
            trailing = { SsSwitch(paused) },
        )
        val ago = updatedAgoLower(content.lastSync, content.now)
        SsSettingsRow(
            Symbol.DirectionsWalk,
            stringResource(R.string.me_step_source),
            when (content.source) {
                SourceKind.HealthConnect -> stringResource(R.string.me_source_hc, ago)
                SourceKind.Sensor -> stringResource(R.string.me_source_sensor, ago)
                SourceKind.Off -> stringResource(R.string.me_source_off)
            },
            nav.stepSource,
        )
        // "Off" whenever the OS blocks delivery, whatever the stored preference (BUG-001).
        val notify = (content.settings.notifyToppingOut || content.settings.notifyWeeklySummary) && rememberNotificationsAllowed()
        SsSettingsRow(
            Symbol.Notifications,
            stringResource(R.string.me_notifications),
            if (notify) stringResource(R.string.me_notifications_sub) else stringResource(R.string.me_notifications_off),
            nav.settings,
        )
        SsSettingsRow(Symbol.Contrast, stringResource(R.string.me_appearance), appearanceLabel(content.settings.appearance), nav.settings)
        SsSettingsRow(Symbol.Help, stringResource(R.string.me_help), stringResource(R.string.me_help_sub), nav.help, showDivider = false)
    }
}

@Composable
internal fun appearanceLabel(a: Appearance) = when (a) {
    Appearance.FollowSystem -> stringResource(R.string.me_appearance_system)
    Appearance.Light -> stringResource(R.string.me_appearance_light)
    Appearance.Dark -> stringResource(R.string.me_appearance_dark)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PauseSheet(onPick: (PauseOption) -> Unit, onDismiss: () -> Unit) {
    val c = SsTheme.colors
    SsModalSheet(onDismiss = onDismiss) {
        Column(
            Modifier.padding(start = SsSpacing.screenMargin, end = SsSpacing.screenMargin, top = SsSpacing.s, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(SsSpacing.m),
        ) {
            Text(stringResource(R.string.pause_title), style = SsTheme.type.sheetTitle, color = c.onSurface)
            Text(stringResource(R.string.pause_body), style = SsTheme.type.bodyS, color = c.bodySecondary)
            listOf(
                PauseOption.ThreeDays to R.string.pause_3_days,
                PauseOption.OneWeek to R.string.pause_week,
                PauseOption.UntilResumed to R.string.pause_indefinite,
            ).forEach { (option, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(c.background, SsRadius.shapeMd)
                        .clickable(role = Role.Button) { onPick(option) }
                        .padding(horizontal = SsSpacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SsSpacing.m),
                ) {
                    SsIcon(Symbol.PauseCircle, size = IconSize.ml, tint = c.onSurface)
                    Text(stringResource(label), style = SsTheme.type.bodyM, color = c.onSurface)
                }
            }
        }
    }
}

internal fun previewMe(): MeContent {
    val today = java.time.LocalDate.of(2026, 11, 20)
    val monday = today.minusDays(4)
    val week = listOf(7, 9, 3, 10, 6, 0, 0).mapIndexed { i, f -> WeekDayBar(monday.plusDays(i.toLong()), f, i == 4, i > 4) }
    val defs = ai.idraak.stepskyscraper.core.domain.achievements.AchievementCatalog.all
    val achievements = defs.mapIndexed { i, d ->
        ai.idraak.stepskyscraper.core.domain.achievements.AchievementProgress(
            d,
            if (i <
                4
            ) {
                d.target
            } else {
                d.target / 2
            },
            if (i < 4) today else null,
        )
    }
    return MeContent(
        340, 6, ai.idraak.stepskyscraper.core.domain.usecase.CalculateArchitectLevelUseCase()(100), 38, 100, 5, week, 35, 50, week,
        listOf(44, 52, 15, 35).mapIndexed { i, f ->
            ai.idraak.stepskyscraper.core.domain.model.PeriodTotal(monday.minusWeeks(3L - i), f, 50, i == 2, i == 3)
        },
        MeReducer.highlight(achievements), false, "$2.99",
        ai.idraak.stepskyscraper.core.domain.model.Goals(
            7_000,
            ai.idraak.stepskyscraper.core.domain.model.TargetPreset.Steady,
            GoalPause.None,
        ),
        ai.idraak.stepskyscraper.core.domain.model.UserSettings.Default, SourceKind.HealthConnect,
        java.time.Instant.parse("2026-11-20T13:56:00Z"), java.time.Instant.parse("2026-11-20T14:00:00Z"),
    )
}

private val previewNav = MeNavigation({}, {}, {}, {}, {}, {})

@ScreenPreviews
@Composable
private fun MePreview() = PreviewSurface { MeScreen(MeUiState(Lce.Content(previewMe())), previewNav, {}) }
