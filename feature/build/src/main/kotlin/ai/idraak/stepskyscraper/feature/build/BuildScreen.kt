package ai.idraak.stepskyscraper.feature.build

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.idraak.stepskyscraper.core.designsystem.component.RollingNumber
import ai.idraak.stepskyscraper.core.designsystem.component.SegmentBar
import ai.idraak.stepskyscraper.core.designsystem.component.SkyHeader
import ai.idraak.stepskyscraper.core.designsystem.component.SsCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconCircleButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsMessageState
import ai.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsSnackbar
import ai.idraak.stepskyscraper.core.designsystem.component.SsStatusChip
import ai.idraak.stepskyscraper.core.designsystem.component.StatusChipKind
import ai.idraak.stepskyscraper.core.designsystem.component.WeeklyRing
import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerAnimation
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerFit
import ai.idraak.stepskyscraper.core.designsystem.tower.towerActiveFloorAnchor
import ai.idraak.stepskyscraper.core.domain.model.DayProgress
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.model.WeekProgress
import ai.idraak.stepskyscraper.core.domain.usecase.CityCalculator
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.ui.component.SystemBarsEffect
import ai.idraak.stepskyscraper.core.ui.component.boldText
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.format.updatedAgo
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import ai.idraak.stepskyscraper.core.ui.tower.CosmeticVisuals
import ai.idraak.stepskyscraper.core.ui.tower.toSpec
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import ai.idraak.stepskyscraper.core.ui.R as UiR

@Serializable
data object BuildRoute

@Composable
fun BuildRoute(
    onOpenCustomise: (Int) -> Unit,
    onOpenShare: (Int) -> Unit,
    onOpenWeek: () -> Unit,
    onOpenStepHelp: () -> Unit,
    onOpenCeremony: (Int) -> Unit,
    viewModel: BuildViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalPlatformActions.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BuildEffect.OpenCustomise -> onOpenCustomise(effect.towerIndex)
                is BuildEffect.OpenShare -> onOpenShare(effect.towerIndex)
                BuildEffect.OpenWeekDetail -> onOpenWeek()
                BuildEffect.OpenStepHelp -> onOpenStepHelp()
                is BuildEffect.OpenCeremony -> onOpenCeremony(effect.towerIndex)
                BuildEffect.RequestStepAccess -> platform.requestStepAccess { viewModel.onIntent(BuildIntent.AccessResult(it)) }
                BuildEffect.InstallHealthConnect -> platform.installHealthConnect()
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(BuildIntent.Resumed) }
    BuildScreen(state, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(state: BuildUiState, onIntent: (BuildIntent) -> Unit) {
    val c = SsTheme.colors
    val pull = rememberPullToRefreshState()
    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .pullToRefresh(isRefreshing = state.syncing, state = pull, onRefresh = { onIntent(BuildIntent.Refresh) }),
    ) {
        when (val content = state.content) {
            Lce.Loading -> SsMessageState(Symbol.Apartment, stringResource(R.string.build_loading), "", Modifier.align(Alignment.Center))
            is Lce.Error -> SsMessageState(Symbol.Apartment, stringResource(R.string.build_error_title), content.message.resolve(), Modifier.align(Alignment.Center))
            is Lce.Content -> BuildContentView(content.value, state, onIntent)
        }
        val snack = state.snackbar
        LaunchedEffect(snack?.id) {
            if (snack != null) {
                delay(SsMotion.SNACKBAR_VISIBLE_MS)
                onIntent(BuildIntent.SnackbarDismissed)
            }
        }
        SsSnackbar(
            visible = snack != null,
            message = snack?.let {
                if (it.floorsAdded > 0) pluralStringResource(R.plurals.build_synced, it.floorsAdded, it.floorsAdded) else stringResource(R.string.build_synced_none)
            } ?: "",
            actionLabel = if ((snack?.floorsAdded ?: 0) > 0) stringResource(R.string.build_view) else null,
            onAction = { onIntent(BuildIntent.SnackbarAction) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = SsSpacing.l, end = SsSpacing.l, bottom = SsSpacing.m),
        )
    }
}

@Composable
private fun BuildContentView(content: BuildContent, state: BuildUiState, onIntent: (BuildIntent) -> Unit) {
    val c = SsTheme.colors
    val tone = CosmeticVisuals.tone(content.sky)
    val chrome = SkyTokens.chrome(tone)
    SystemBarsEffect(lightIcons = chrome.lightStatusBar)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val compact = LocalDensity.current.fontScale > 1.5f
    val kitName = stringResource(CosmeticVisuals.kitName(content.kit))
    Box(Modifier.fillMaxSize()) {
        val heroBottom = top + if (compact) 300.dp else SsSpacing.sheetTop - 28.dp
        Box(Modifier.fillMaxWidth().height(heroBottom + 28.dp).background(SkyTokens.gradient(tone).brush()))
        Column(Modifier.padding(top = top)) {
            SkyHeader(
                overline = if (content.morning) {
                    stringResource(R.string.build_overline_morning, kitName)
                } else {
                    stringResource(R.string.build_overline_tower, content.towerIndex, kitName)
                },
                title = content.towerName,
                subtitle = stringResource(R.string.build_floors_of, content.floorsBuilt, content.target),
                chrome = chrome,
                actions = {
                    SsIconCircleButton(
                        Symbol.Palette, stringResource(R.string.build_customise), { onIntent(BuildIntent.OpenCustomise) },
                        container = chrome.buttonFill, tint = chrome.buttonIcon,
                    )
                    SsIconCircleButton(
                        Symbol.IosShare, stringResource(R.string.build_share), { onIntent(BuildIntent.OpenShare) },
                        container = chrome.buttonFill, tint = chrome.buttonIcon,
                    )
                },
            )
        }
        TowerHero(
            content, state,
            onIntent,
            Modifier
                .padding(top = top + if (compact) 90.dp else SsSpacing.towerTop - 28.dp)
                .padding(horizontal = SsSpacing.xl)
                .fillMaxWidth()
                .height(if (compact) SsSpacing.compactHeroHeight else SsSpacing.towerHeight),
        )
        Column(
            Modifier
                .padding(top = heroBottom)
                .fillMaxSize()
                .background(c.background, SsRadius.shapeSheet)
                .verticalScroll(rememberScrollState())
                .padding(start = SsSpacing.l, end = SsSpacing.l, top = SsSpacing.l, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(SsSpacing.m),
        ) {
            NextFloorCard(content, state.syncing)
            if (content.access == StepAccessUi.Connected) {
                WeekCard(content.week, content.weekMessage) { onIntent(BuildIntent.OpenWeek) }
            } else {
                ConnectCard(content.access) { onIntent(BuildIntent.ConnectSteps) }
            }
        }
    }
}

@Composable
private fun TowerHero(content: BuildContent, state: BuildUiState, onIntent: (BuildIntent) -> Unit, modifier: Modifier) {
    val spec = remember(content.look, content.floorsBuilt, content.target, content.sky, content.today.stepsIntoFloor) {
        content.look.toSpec(
            floors = content.floorsBuilt, target = content.target, sky = content.sky, crane = content.floorsBuilt < content.target,
            progress = content.today.stepsIntoFloor / 1_000f, seed = content.towerIndex,
        )
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val reduce = SsTheme.reduceMotion
    val scale by animateFloatAsState(if (state.zoomed) SsMotion.ZOOM_SCALE else 1f, tween(if (reduce) 0 else 300), label = "zoom")
    val anchor = remember(spec, size) {
        if (size == IntSize.Zero) null else towerActiveFloorAnchor(spec, TowerFit(), Size(size.width.toFloat(), size.height.toFloat()))
    }
    val description = stringResource(
        UiR.string.tower_description, content.towerName, content.floorsBuilt, content.target, Formats.steps(content.today.stepsToNextFloor),
    )
    val tapLabel = stringResource(R.string.build_tower_actions)
    Box(
        modifier
            .onSizeChanged { size = it }
            .clearAndSetSemantics {
                contentDescription = "$description $tapLabel"
                onClick { onIntent(BuildIntent.TowerTapped); true }
                onLongClick { onIntent(BuildIntent.TowerLongPressed); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onIntent(BuildIntent.TowerTapped) }, onLongPress = { onIntent(BuildIntent.TowerLongPressed) })
            },
    ) {
        TowerCanvas(
            spec,
            description,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    anchor?.let { transformOrigin = TransformOrigin(it.x / size.width, it.y / size.height) }
                },
            animation = content.dropFrom?.let { TowerAnimation(dropFrom = it, key = content.dropKey) } ?: TowerAnimation.None,
            mirror = state.mirrored,
        )
        if (state.zoomed && anchor != null) {
            val density = LocalDensity.current
            SsStatusChip(
                stringResource(R.string.build_zoom_label, content.nextFloorNumber, Formats.steps(content.today.stepsIntoFloor)),
                StatusChipKind.Neutral,
                Modifier.offset(x = with(density) { anchor.x.toDp() } - 80.dp, y = with(density) { anchor.y.toDp() } - 56.dp),
            )
        }
    }
}

@Composable
private fun NextFloorCard(content: BuildContent, syncing: Boolean) {
    val c = SsTheme.colors
    val dim = content.stale || syncing
    val numberColor = if (dim) c.onSurfaceVariant else c.onSurface
    SsCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(stringResource(R.string.build_next_floor_in), style = SsTheme.type.label, color = c.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                    RollingNumber(content.today.stepsToNextFloor, SsTheme.type.numberHero, numberColor)
                    Text(stringResource(R.string.build_steps_unit), style = SsTheme.type.unit, color = numberColor, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
            when {
                syncing -> SsStatusChip(stringResource(R.string.build_syncing), StatusChipKind.Sync, spinning = true)
                content.targetMet -> SsStatusChip(stringResource(R.string.build_target_met), StatusChipKind.TargetMet, pop = true)
            }
        }
        Box(Modifier.height(SsSpacing.m))
        SegmentBar(
            filled = content.today.segmentsFilled,
            activeFraction = content.today.activeSegmentFraction,
            contentDescription = stringResource(UiR.string.segment_bar_description, content.nextFloorNumber, Formats.steps(content.today.stepsIntoFloor)),
            dimmed = dim,
            outlineActive = !dim,
        )
        Box(Modifier.height(SsSpacing.m))
        if (content.today.steps == 0 && !dim) {
            Text(stringResource(R.string.build_fresh_day), style = SsTheme.type.bodyS, color = c.bodySecondary)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val left = if (content.targetMet) {
                    stringResource(R.string.build_steps_of_target, Formats.steps(content.today.steps), Formats.steps(content.dailyTarget))
                } else {
                    stringResource(R.string.build_steps_today, Formats.steps(content.today.steps))
                }
                Text(boldText(left, c.onSurface), style = SsTheme.type.bodyS, color = c.bodySecondary, maxLines = 1)
                val right = if (dim) updatedAgo(content.lastSyncAt, content.now) else pluralStringResource(R.plurals.build_floors_today, content.today.floors, content.today.floors)
                Text(boldText(right, c.onSurface), style = SsTheme.type.bodyS, color = c.bodySecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun WeekCard(week: WeekProgress, message: WeekMessage, onClick: () -> Unit) {
    val c = SsTheme.colors
    val subtitle = when (message) {
        is WeekMessage.ToGo -> stringResource(R.string.build_week_to_go, message.floors, pluralStringResource(R.plurals.build_days_left, message.daysLeft, message.daysLeft))
        is WeekMessage.ToppingOut -> pluralStringResource(R.plurals.build_week_topping, message.floors, message.floors)
        WeekMessage.Bonus -> stringResource(R.string.build_week_bonus)
        WeekMessage.Rest -> stringResource(R.string.build_week_rest)
        WeekMessage.Paused -> stringResource(R.string.build_week_paused)
        WeekMessage.Reached -> stringResource(R.string.build_week_reached)
        WeekMessage.NeedsHelp -> stringResource(R.string.build_week_help)
    }
    SsCard(
        padding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = SsSpacing.s, top = SsSpacing.m, bottom = SsSpacing.m),
        onClick = onClick,
        onClickLabel = stringResource(R.string.build_week_open),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            WeeklyRing(
                week.fraction, fill = if (message == WeekMessage.Paused) c.barNeutral else c.primary,
            ) { Text(stringResource(R.string.build_ring_percent, week.percent), style = SsTheme.type.ringCenter, color = c.onSurface) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.build_week_title, week.floors, week.goalFloors), style = SsTheme.type.bodyMStrong, color = c.onSurface)
                Text(subtitle, style = SsTheme.type.bodyXS, color = c.onSurfaceVariant)
            }
            Box(Modifier.padding(SsSpacing.m)) { SsIcon(Symbol.ChevronRight, size = IconSize.ml, tint = c.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ConnectCard(access: StepAccessUi, onConnect: () -> Unit) {
    val c = SsTheme.colors
    SsCard {
        Text(stringResource(R.string.build_connect_title), style = SsTheme.type.bodyMStrong, color = c.onSurface)
        Text(stringResource(R.string.build_connect_body), style = SsTheme.type.bodyXS, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = SsSpacing.m))
        val cta = when (access) {
            StepAccessUi.HcMissing -> R.string.build_hc_missing_cta
            StepAccessUi.HcNeedsUpdate -> R.string.build_hc_update_cta
            else -> R.string.build_connect_cta
        }
        SsPrimaryButton(stringResource(cta), onConnect, Modifier.fillMaxWidth(), icon = Symbol.DirectionsWalk, large = false)
    }
}

// ---- previews ----------------------------------------------------------------------------------------

internal fun previewBuildContent(
    steps: Int = 6_660,
    floors: Int = 14,
    sky: SkyTime = SkyTime.Day,
    morning: Boolean = false,
    weekFloors: Int = 35,
    message: WeekMessage = WeekMessage.ToGo(15, 3),
    stale: Boolean = false,
    access: StepAccessUi = StepAccessUi.Connected,
    paused: Boolean = false,
): BuildContent {
    val today = LocalDate.of(2026, 11, 20)
    return BuildContent(
        towerIndex = 5, towerName = "Marlow Spire", kit = ai.idraak.stepskyscraper.core.domain.model.FacadeKit.Deco,
        look = CityCalculator.defaultLook(ai.idraak.stepskyscraper.core.domain.model.FacadeKit.Deco),
        floorsBuilt = floors, target = 24, sky = sky, morning = morning,
        today = DayProgress(today, steps, steps / 1_000, steps % 1_000),
        dailyTarget = TargetPreset.Steady.dailySteps,
        week = WeekProgress(today.minusDays(4), weekFloors, 50, 3, paused, emptyList()),
        weekMessage = message, stale = stale, lastSyncAt = Instant.parse("2026-11-20T13:48:00Z"), now = Instant.parse("2026-11-20T14:00:00Z"),
        access = access, dropFrom = null, dropKey = 0,
    )
}

@ScreenPreviews
@Composable
private fun BuildDayPreview() = PreviewSurface { BuildScreen(BuildUiState(Lce.Content(previewBuildContent())), {}) }

@ScreenPreviews
@Composable
private fun BuildMorningPreview() = PreviewSurface {
    BuildScreen(BuildUiState(Lce.Content(previewBuildContent(1_240, 20, SkyTime.Dawn, true, 29, WeekMessage.ToppingOut(4)))), {})
}

@ScreenPreviews
@Composable
private fun BuildTargetMetPreview() = PreviewSurface {
    BuildScreen(BuildUiState(Lce.Content(previewBuildContent(8_420, 16, SkyTime.Dusk, weekFloors = 43, message = WeekMessage.Bonus))), {})
}

@ScreenPreviews
@Composable
private fun BuildZeroPreview() = PreviewSurface {
    BuildScreen(BuildUiState(Lce.Content(previewBuildContent(0, 11, weekFloors = 22, message = WeekMessage.Rest))), {})
}

@ScreenPreviews
@Composable
private fun BuildSyncingPreview() = PreviewSurface {
    BuildScreen(BuildUiState(Lce.Content(previewBuildContent(stale = true)), syncing = true, snackbar = BuildSnackbar(2, 1)), {})
}

@ScreenPreviews
@Composable
private fun BuildNightPreview() = PreviewSurface(dark = true) {
    BuildScreen(BuildUiState(Lce.Content(previewBuildContent(sky = SkyTime.Night))), {})
}

@ScreenPreviews
@Composable
private fun BuildNotConnectedPreview() = PreviewSurface {
    BuildScreen(BuildUiState(Lce.Content(previewBuildContent(0, 0, access = StepAccessUi.NotConnected))), {})
}

@ScreenPreviews
@Composable
private fun BuildPausedPreview() = PreviewSurface {
    BuildScreen(BuildUiState(Lce.Content(previewBuildContent(message = WeekMessage.Paused, paused = true))), {})
}
