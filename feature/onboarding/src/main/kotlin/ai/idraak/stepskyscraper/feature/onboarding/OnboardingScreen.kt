package ai.idraak.stepskyscraper.feature.onboarding

import ai.idraak.stepskyscraper.core.designsystem.component.CountUpNumber
import ai.idraak.stepskyscraper.core.designsystem.component.PageDots
import ai.idraak.stepskyscraper.core.designsystem.component.SegmentBar
import ai.idraak.stepskyscraper.core.designsystem.component.SsBadge
import ai.idraak.stepskyscraper.core.designsystem.component.SsCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsRadioCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsTextButton
import ai.idraak.stepskyscraper.core.designsystem.component.cssLineBox
import ai.idraak.stepskyscraper.core.designsystem.component.ssShadow
import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import ai.idraak.stepskyscraper.core.designsystem.theme.SsElevation
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.designsystem.tower.Facade
import ai.idraak.stepskyscraper.core.designsystem.tower.GroundStyle
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerAnimation
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerFit
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerSpec
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerTime
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.ui.component.StyleAndNamePicker
import ai.idraak.stepskyscraper.core.ui.component.SystemBarsEffect
import ai.idraak.stepskyscraper.core.ui.component.boldText
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import ai.idraak.stepskyscraper.core.ui.tower.CosmeticVisuals
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalPlatformActions.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.RequestStepAccess -> platform.requestStepAccess { viewModel.onIntent(OnboardingIntent.AccessResult(it)) }
                OnboardingEffect.InstallHealthConnect -> platform.installHealthConnect()
                OnboardingEffect.RequestPinWidget -> viewModel.onIntent(OnboardingIntent.WidgetPinResult(platform.requestPinWidget()))
                OnboardingEffect.Finished -> onFinished()
            }
        }
    }
    // Returning from the Health Connect Play listing re-checks the source.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(OnboardingIntent.RecheckSource) }
    BackHandler(enabled = state.step != OnboardingStep.Promise && state.step != OnboardingStep.Widget) {
        viewModel.onIntent(OnboardingIntent.Back)
    }
    OnboardingScreen(state, viewModel::onIntent)
}

@Composable
fun OnboardingScreen(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val reduce = SsTheme.reduceMotion
    SystemBarsEffect(lightIcons = SsTheme.colors.isDark)
    AnimatedContent(
        targetState = state.step,
        transitionSpec = { fadeIn(tween(if (reduce) SsMotion.CROSSFADE_MS else 300)) togetherWith fadeOut(tween(SsMotion.CROSSFADE_MS)) },
        label = "onboarding",
    ) { step ->
        when (step) {
            OnboardingStep.Promise -> PromiseStep(onIntent)
            OnboardingStep.Permission -> PermissionStep(state, onIntent)
            OnboardingStep.Payoff -> PayoffStep(state, onIntent)
            OnboardingStep.Style -> StyleStep(state, onIntent)
            OnboardingStep.Target -> TargetStep(state, onIntent)
            OnboardingStep.Widget -> WidgetStep(state, onIntent)
        }
    }
}

/** Bottom block shared by every step: content, page dots and CTA, 24dp margins, 40dp from the bottom. */
@Composable
private fun BottomBlock(gap: androidx.compose.ui.unit.Dp = SsSpacing.l, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, bottom = SsSpacing.bottomCtaInset),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

@Composable
private fun Dots(index: Int) = PageDots(4, index, stringResource(R.string.onb_page, index + 1, 4))

// ---- 1a Promise --------------------------------------------------------------------------------------

@Composable
private fun PromiseStep(onIntent: (OnboardingIntent) -> Unit) {
    val c = SsTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(SkyTokens.OnboardingDawn.fadingInto(c.background, 0.78f).brush())
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(
            Modifier
                .padding(start = 40.dp, end = 40.dp, top = 36.dp)
                .fillMaxWidth()
                .weight(1f)
                .heightIn(max = 420.dp),
        ) {
            TowerCanvas(
                TowerSpec(facade = Facade.Deco, floors = 7, target = 12, progress = 0.5f, crane = true, time = TowerTime.Dawn),
                stringResource(R.string.onb_promise_tower),
                Modifier.fillMaxSize(),
                animation = TowerAnimation.Drop,
            )
        }
        BottomBlock {
            Text(stringResource(R.string.onb_promise_title), style = SsTheme.type.headlineL, color = c.onSurface, modifier = Modifier.cssLineBox(SsTheme.type.headlineL))
            Text(stringResource(R.string.onb_promise_body), style = SsTheme.type.body, color = c.bodySecondary)
            Dots(0)
            SsPrimaryButton(stringResource(R.string.onb_promise_cta), { onIntent(OnboardingIntent.Start) }, Modifier.fillMaxWidth())
        }
    }
}

// ---- 1b Permission -----------------------------------------------------------------------------------

@Composable
private fun PermissionStep(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val c = SsTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(SsSpacing.screenMargin),
        ) {
            Box(Modifier.size(72.dp).background(c.surfaceVariant, SsRadius.shapeLg), contentAlignment = Alignment.Center) {
                SsIcon(Symbol.DirectionsWalk, size = IconSize.hero, tint = c.onSurface)
            }
            Text(stringResource(R.string.onb_perm_title), style = SsTheme.type.headlineL, color = c.onSurface, modifier = Modifier.cssLineBox(SsTheme.type.headlineL))
            Text(stringResource(R.string.onb_perm_lead), style = SsTheme.type.bodyL, color = c.onSurface)
            SsCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                val reads = if (state.usesSensor) R.string.onb_perm_reads_sensor else R.string.onb_perm_reads
                PermissionRow(Symbol.Check, R.string.onb_perm_reads_bold, reads, divider = true)
                PermissionRow(Symbol.Block, R.string.onb_perm_never_bold, R.string.onb_perm_never, divider = true)
                PermissionRow(Symbol.Smartphone, R.string.onb_perm_stays_bold, R.string.onb_perm_stays, divider = false)
            }
            val note = when (state.access) {
                AccessState.NotInstalled, AccessState.NeedsUpdate -> R.string.onb_perm_install_note
                AccessState.Denied -> R.string.onb_perm_denied_note
                else -> null
            }
            if (note != null) Text(stringResource(note), style = SsTheme.type.bodyXS, color = c.bodySecondary)
        }
        BottomBlock(gap = SsSpacing.s) {
            Box(Modifier.padding(bottom = SsSpacing.s)) { Dots(1) }
            val cta = when (state.access) {
                AccessState.NotInstalled -> R.string.onb_perm_install
                AccessState.NeedsUpdate -> R.string.onb_perm_update
                else -> R.string.onb_perm_allow
            }
            SsPrimaryButton(stringResource(cta), { onIntent(OnboardingIntent.AllowAccess) }, Modifier.fillMaxWidth())
            SsTextButton(stringResource(R.string.onb_perm_not_now), { onIntent(OnboardingIntent.NotNow) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PermissionRow(symbol: Symbol, bold: Int, rest: Int, divider: Boolean) {
    val c = SsTheme.colors
    Column {
        Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SsIcon(symbol, size = IconSize.ml, tint = c.onSurface)
            Text(
                boldText("<b>${stringResource(bold)}</b> ${stringResource(rest)}"),
                style = SsTheme.type.bodyM,
                color = c.onSurface,
            )
        }
        if (divider) HorizontalDivider(thickness = 1.dp, color = c.divider)
    }
}

// ---- 1c Payoff ---------------------------------------------------------------------------------------

@Composable
private fun PayoffStep(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val c = SsTheme.colors
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier
            .fillMaxSize()
            .background(SkyTokens.OnboardingDay.fadingInto(c.background, 0.72f).brush())
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(Modifier.padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = 28.dp)) {
            Text(
                stringResource(R.string.onb_payoff_already),
                style = SsTheme.type.labelM,
                color = SkyTokens.chrome(ai.idraak.stepskyscraper.core.designsystem.theme.SkyTone.Day).subtitle,
            )
            // Design: `align-items: baseline` — the unit sits on the number's baseline.
            Row(horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                if (state.payoffLoading) {
                    Text(stringResource(R.string.onb_payoff_loading), style = SsTheme.type.title, color = c.onSurface)
                } else {
                    CountUpNumber(
                        target = state.todaySteps,
                        style = SsTheme.type.numberXL,
                        color = c.onSurface,
                        modifier = Modifier.alignByBaseline(),
                        onTick = { if (it % 1_000 == 0 && it > 0) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) },
                    )
                    Text(
                        stringResource(R.string.onb_payoff_steps_today),
                        style = SsTheme.type.unit,
                        color = c.onSurface,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
        Box(
            Modifier
                .padding(horizontal = 56.dp)
                .padding(top = SsSpacing.l)
                .fillMaxWidth()
                .weight(1f)
                .heightIn(max = 340.dp),
        ) {
            if (!state.payoffLoading) {
                TowerCanvas(
                    TowerSpec(
                        facade = Facade.Deco,
                        floors = state.floorsToday.coerceAtMost(11),
                        target = 12,
                        progress = state.floorProgress,
                        time = TowerTime.Day,
                    ),
                    pluralStringResource(R.plurals.onb_payoff_title, state.floorsToday, state.floorsToday),
                    Modifier.fillMaxSize(),
                    animation = TowerAnimation(dropFrom = -1, key = state.todaySteps),
                    fit = TowerFit(minHeight = 190f),
                )
            }
        }
        BottomBlock {
            if (state.floorsToday == 0) {
                Text(stringResource(R.string.onb_payoff_zero_title), style = SsTheme.type.headline, color = c.onSurface, modifier = Modifier.cssLineBox(SsTheme.type.headline))
                Text(stringResource(R.string.onb_payoff_zero_body), style = SsTheme.type.bodyM, color = c.bodySecondary)
            } else {
                Text(
                    pluralStringResource(R.plurals.onb_payoff_title, state.floorsToday, state.floorsToday),
                    style = SsTheme.type.headline,
                    color = c.onSurface,
                    modifier = Modifier.cssLineBox(SsTheme.type.headline),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                SegmentBar(
                    filled = state.segmentsFilled,
                    activeFraction = 0f,
                    contentDescription = stringResource(
                        R.string.onb_payoff_next,
                        state.floorsToday + 1,
                        Formats.steps(state.stepsToNext),
                    ).replace("<b>", "").replace("</b>", ""),
                    future = c.trackFuture,
                )
                if (state.floorsToday > 0) {
                    Text(
                        boldText(
                            stringResource(R.string.onb_payoff_next, state.floorsToday + 1, Formats.steps(state.stepsToNext)),
                            c.onSurface,
                        ),
                        style = SsTheme.type.bodyM,
                        color = c.bodySecondary,
                    )
                }
            }
            Dots(2)
            SsPrimaryButton(
                stringResource(R.string.onb_payoff_cta),
                { onIntent(OnboardingIntent.ContinueFromPayoff) },
                Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---- 1d Style & name ---------------------------------------------------------------------------------

@Composable
private fun StyleStep(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val c = SsTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.onb_style_title),
                style = SsTheme.type.headlineS,
                color = c.onSurface,
                modifier = Modifier.padding(
                    start = SsSpacing.onboardingMargin,
                    end = SsSpacing.onboardingMargin,
                    top = SsSpacing.screenMargin,
                ).cssLineBox(SsTheme.type.headlineS),
            )
            StyleAndNamePicker(
                selected = state.kit,
                name = state.name,
                previewFloors = state.floorsToday.coerceIn(4, 11),
                previewTarget = 12,
                animationKey = state.restyleKey,
                onSelect = { onIntent(OnboardingIntent.SelectKit(it)) },
                onNameChange = { onIntent(OnboardingIntent.NameChanged(it)) },
                onDice = { onIntent(OnboardingIntent.RollName) },
            )
        }
        BottomBlock {
            Dots(3)
            SsPrimaryButton(
                stringResource(R.string.onb_style_cta),
                { onIntent(OnboardingIntent.ConfirmStyle) },
                Modifier.fillMaxWidth(),
                enabled = state.name.isNotBlank(),
            )
        }
    }
}

// ---- 1e Daily target ---------------------------------------------------------------------------------

@Composable
private fun TargetStep(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val c = SsTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Box(Modifier.padding(start = SsSpacing.s, top = SsSpacing.s)) {
            SsIconButton(Symbol.ArrowBack, stringResource(R.string.onb_back), { onIntent(OnboardingIntent.Back) })
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                Modifier.padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = SsSpacing.s),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.onb_target_title), style = SsTheme.type.headlineS, color = c.onSurface, modifier = Modifier.cssLineBox(SsTheme.type.headlineS))
                val body = if (state.sevenDayAverage > 0) {
                    boldText(stringResource(R.string.onb_target_body, Formats.steps(state.sevenDayAverage)), c.onSurface)
                } else {
                    boldText(stringResource(R.string.onb_target_body_no_history))
                }
                Text(body, style = SsTheme.type.bodyM, color = c.bodySecondary)
            }
            Column(
                Modifier.padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = SsSpacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(
                    TargetPreset.EasyGoing to R.string.onb_target_easy,
                    TargetPreset.Steady to R.string.onb_target_steady,
                    TargetPreset.Ambitious to R.string.onb_target_ambitious,
                ).forEach { (preset, label) ->
                    SsRadioCard(
                        title = stringResource(label),
                        subtitle = stringResource(
                            R.string.onb_target_option_sub,
                            Formats.steps(preset.dailySteps),
                            preset.dailySteps / 1_000 * 7,
                        ),
                        selected = state.selected == preset,
                        onClick = { onIntent(OnboardingIntent.SelectTarget(preset)) },
                        trailing = if (preset == state.suggested) ({ SsBadge(stringResource(R.string.onb_target_suggested)) }) else null,
                    )
                }
                val custom = state.selected == TargetPreset.Custom
                SsRadioCard(
                    title = stringResource(R.string.onb_target_custom),
                    subtitle = if (custom) {
                        stringResource(R.string.onb_target_option_sub, Formats.steps(state.customSteps), state.customSteps / 1_000 * 7)
                    } else {
                        stringResource(R.string.onb_target_custom_sub)
                    },
                    selected = custom,
                    leading = Symbol.Tune,
                    onClick = { onIntent(OnboardingIntent.SelectTarget(TargetPreset.Custom)) },
                    trailing = if (custom) {
                        {
                            Row {
                                SsIconButton(Symbol.Remove, stringResource(R.string.onb_target_less), {
                                    onIntent(OnboardingIntent.AdjustCustom(-OnboardingReducer.CUSTOM_STEP))
                                })
                                SsIconButton(Symbol.Add, stringResource(R.string.onb_target_more), {
                                    onIntent(OnboardingIntent.AdjustCustom(OnboardingReducer.CUSTOM_STEP))
                                })
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
        BottomBlock(gap = SsSpacing.m) {
            Text(
                stringResource(R.string.onb_target_pause_hint),
                style = SsTheme.type.bodyXS,
                color = c.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            SsPrimaryButton(
                stringResource(R.string.onb_target_cta, state.weeklyFloors),
                { onIntent(OnboardingIntent.ConfirmTarget) },
                Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )
        }
    }
}

// ---- 1f Add widget -----------------------------------------------------------------------------------

@Composable
private fun WidgetStep(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val c = SsTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Box(
            Modifier
                .padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = SsSpacing.onboardingMargin)
                .fillMaxWidth()
                .height(340.dp)
                .background(Brush.linearGradient(SkyTokens.WallpaperMock), SsRadius.shapeLg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.onb_widget_mock),
                style = SsTheme.type.mockLabel,
                color = SkyTokens.WallpaperMockLabel,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = SsSpacing.m),
            )
            WidgetMock(state)
        }
        Column(
            Modifier.weight(1f).padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.onb_widget_title), style = SsTheme.type.headlineS, color = c.onSurface, modifier = Modifier.cssLineBox(SsTheme.type.headlineS))
            Text(stringResource(R.string.onb_widget_body), style = SsTheme.type.bodyM, color = c.bodySecondary)
            if (state.widgetUnsupported) {
                Text(
                    stringResource(R.string.onb_widget_unsupported),
                    style = SsTheme.type.bodyXS,
                    color = c.onSurfaceVariant,
                )
            }
        }
        BottomBlock(gap = SsSpacing.s) {
            SsPrimaryButton(stringResource(R.string.onb_widget_cta), {
                onIntent(OnboardingIntent.AddWidget)
            }, Modifier.fillMaxWidth(), icon = Symbol.Widgets)
            SsTextButton(stringResource(R.string.onb_widget_later), { onIntent(OnboardingIntent.MaybeLater) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun WidgetMock(state: OnboardingUiState) {
    val c = SsTheme.colors
    Row(
        Modifier
            .size(264.dp, 124.dp)
            .ssShadow(SsElevation.widget, SsRadius.shapeLg)
            .background(c.surface, SsRadius.shapeLg),
    ) {
        Box(
            Modifier
                .size(96.dp, 124.dp)
                .background(
                    SkyTokens.CardDay.brush(),
                    androidx.compose.foundation.shape.RoundedCornerShape(topStart = SsRadius.lg, bottomStart = SsRadius.lg),
                )
                .padding(start = 6.dp, end = 6.dp, top = SsSpacing.s, bottom = SsSpacing.xs),
        ) {
            TowerCanvas(
                TowerSpec(facade = CosmeticVisuals.facade(state.kit), floors = 5, target = 12, progress = 0.6f, ground = GroundStyle.None),
                state.name,
                Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(state.name.uppercase(), style = SsTheme.type.microStrong, color = c.onSurfaceVariant, maxLines = 1)
                Text(Formats.steps(400), style = SsTheme.type.numberWidgetMock, color = c.onSurface, modifier = Modifier.cssLineBox(SsTheme.type.numberWidgetMock))
                Text(
                    stringResource(ai.idraak.stepskyscraper.core.ui.R.string.to_next_floor),
                    style = SsTheme.type.cardCaption,
                    color = c.bodySecondary,
                )
            }
            Spacer(Modifier.height(SsSpacing.xs))
            SegmentBar(6, 0f, "", height = 6.dp, gap = 2.dp, radius = 2.dp, outlineActive = false)
        }
    }
}

@ScreenPreviews
@Composable
private fun PromisePreview() = PreviewSurface { OnboardingScreen(OnboardingUiState(), {}) }

@ScreenPreviews
@Composable
private fun PermissionPreview() = PreviewSurface { OnboardingScreen(OnboardingUiState(step = OnboardingStep.Permission), {}) }

@ScreenPreviews
@Composable
private fun PayoffPreview() = PreviewSurface { OnboardingScreen(OnboardingUiState(step = OnboardingStep.Payoff, todaySteps = 4_200), {}) }

@ScreenPreviews
@Composable
private fun PayoffZeroPreview() = PreviewSurface { OnboardingScreen(OnboardingUiState(step = OnboardingStep.Payoff, todaySteps = 0), {}) }

@ScreenPreviews
@Composable
private fun StylePreview() = PreviewSurface { OnboardingScreen(OnboardingUiState(step = OnboardingStep.Style, name = "Calder Glass"), {}) }

@ScreenPreviews
@Composable
private fun TargetPreview() = PreviewSurface {
    OnboardingScreen(OnboardingUiState(step = OnboardingStep.Target, sevenDayAverage = 6_400), {})
}

@ScreenPreviews
@Composable
private fun WidgetPreview() = PreviewSurface {
    OnboardingScreen(OnboardingUiState(step = OnboardingStep.Widget, name = "Calder Glass"), {})
}
