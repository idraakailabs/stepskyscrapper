package ai.idraak.stepskyscraper.feature.build.ceremony

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.idraak.stepskyscraper.core.designsystem.component.AutoSizeName
import ai.idraak.stepskyscraper.core.designsystem.component.rememberCountUp
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsMessageState
import ai.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsTonalButton
import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.designsystem.tower.CrownMode
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerAnimation
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.usecase.CityCalculator
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.ui.component.SystemBarsEffect
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.platform.awaitNotificationPermission
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import ai.idraak.stepskyscraper.core.ui.tower.toSpec
import ai.idraak.stepskyscraper.feature.build.R
import kotlinx.coroutines.delay

@Composable
fun CeremonyRoute(
    onShare: (Int) -> Unit,
    onStartNext: (showPaywall: Boolean) -> Unit,
    onClose: (towerIndex: Int, showPaywall: Boolean) -> Unit,
    viewModel: CeremonyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalPlatformActions.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { e ->
            when (e) {
                is CeremonyEffect.Share -> onShare(e.towerIndex)
                is CeremonyEffect.StartNext -> {
                    if (e.askNotifications) platform.awaitNotificationPermission()
                    onStartNext(e.showPaywall)
                }
                is CeremonyEffect.Close -> {
                    if (e.askNotifications) platform.awaitNotificationPermission()
                    onClose(e.towerIndex, e.showPaywall)
                }
            }
        }
    }
    val loaded = state.content is Lce.Content
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        // 0.0s crown lift + medium haptic on contact, 1.6s sheet rises (tap anywhere skips).
        delay(SsMotion.CROWN_LIFT_DELAY_MS + SsMotion.CROWN_LIFT_MS.toLong())
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        platform.playCeremonySound()
        delay(SsMotion.CEREMONY_SHEET_AT_MS - SsMotion.CROWN_LIFT_DELAY_MS - SsMotion.CROWN_LIFT_MS)
        viewModel.onIntent(CeremonyIntent.RevealSheet)
    }
    CeremonyScreen(state, viewModel::onIntent)
}

@Composable
fun CeremonyScreen(state: CeremonyUiState, onIntent: (CeremonyIntent) -> Unit) {
    val c = SsTheme.colors
    val dark = c.isDark
    SystemBarsEffect(lightIcons = true)
    val sky = if (dark) SkyTokens.Night else SkyTokens.Dusk
    val content = (state.content as? Lce.Content)?.value
    if (content == null) {
        Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.Center) {
            if (state.content is Lce.Error) SsMessageState(Symbol.Apartment, stringResource(R.string.build_error_title), "")
        }
        return
    }
    val skip = stringResource(R.string.ceremony_skip)
    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = skip) { onIntent(CeremonyIntent.RevealSheet) },
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.7f).background(sky.brush()))
        var beamsOn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(SsMotion.CEREMONY_WINDOWS_AT_MS)
            beamsOn = true
        }
        val beam by animateFloatAsState(if (beamsOn || state.sheetVisible) 1f else 0f, tween(600), label = "beam")
        val beamColor = if (dark) SkyTokens.BeamDark.copy(alpha = 0.22f) else SkyTokens.BeamLight.copy(alpha = 0.28f)
        listOf(-14f to Alignment.TopStart, 14f to Alignment.TopEnd).forEach { (rot, align) ->
            Box(
                Modifier
                    .align(align)
                    .padding(top = 120.dp)
                    .padding(horizontal = 70.dp)
                    .size(60.dp, 440.dp)
                    .graphicsLayer {
                        rotationZ = rot
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        alpha = beam
                    }
                    .background(Brush.verticalGradient(listOf(Color.Transparent, beamColor))),
            )
        }
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Box(Modifier.fillMaxWidth().padding(end = SsSpacing.s, top = SsSpacing.xs), contentAlignment = Alignment.TopEnd) {
                SsIconButton(Symbol.Close, stringResource(R.string.ceremony_close), { onIntent(CeremonyIntent.Close) }, tint = Color(0xFFFBFAF7))
            }
        }
        TowerCanvas(
            content.look.toSpec(content.floors, content.floors, if (dark) SkyTime.Night else SkyTime.Dusk, crane = true, crownMode = CrownMode.Lift),
            stringResource(R.string.ceremony_tower, content.name),
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .offset(x = 30.dp, y = 48.dp)
                .size(300.dp, 480.dp),
            animation = TowerAnimation.Ceremony,
        )
        AnimatedVisibility(
            visible = state.sheetVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(SsMotion.SHEET_MS, easing = SsMotion.EmphasizedDecelerate)) { it } + fadeIn(),
        ) {
            CeremonySheet(content, onIntent)
        }
    }
}

@Composable
private fun CeremonySheet(content: CeremonyContent, onIntent: (CeremonyIntent) -> Unit) {
    val c = SsTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.background, SsRadius.shapeSheet)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = 22.dp, bottom = SsSpacing.xl)
            .semantics(mergeDescendants = false) {},
        verticalArrangement = Arrangement.spacedBy(SsSpacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.ceremony_topped_out).uppercase(), style = SsTheme.type.overlineWide, color = c.primaryText)
            AutoSizeName(content.name, SsTheme.type.towerNameCeremony, c.onSurface)
            Text(
                stringResource(R.string.ceremony_stats, content.floors, content.buildDays, Formats.steps(content.steps)),
                style = SsTheme.type.bodyS, color = c.bodySecondary,
            )
        }
        Row(
            Modifier.fillMaxWidth().background(c.surface, SsRadius.shapeMd).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SsIcon(Symbol.Architecture, size = IconSize.ml, tint = c.primaryText)
            // Design: one sentence, "+120 Blueprints" bold, wrapping as a whole; the number counts up.
            val shown = rememberCountUp(content.blueprints, durationMs = BLUEPRINT_COUNT_MS)
            val amount = stringResource(R.string.ceremony_blueprints_amount, Formats.steps(shown))
            val rest = stringResource(R.string.ceremony_blueprints_rest)
            val spoken = stringResource(R.string.ceremony_blueprints_amount, Formats.steps(content.blueprints)) + " " + rest
            Text(
                buildAnnotatedString {
                    withStyle(SsTheme.type.bodySStrong.toSpanStyle()) { append(amount) }
                    append(" ")
                    append(rest)
                },
                style = SsTheme.type.bodyS,
                color = c.onSurface,
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
            SsPrimaryButton(stringResource(R.string.ceremony_share), { onIntent(CeremonyIntent.Share) }, Modifier.weight(1f), icon = Symbol.IosShare, large = false)
            SsTonalButton(
                stringResource(R.string.ceremony_next), { onIntent(CeremonyIntent.StartNext) }, Modifier.weight(1.3f),
                strong = true, height = SsSpacing.primaryButtonHeight,
            )
        }
        Box(Modifier.height(0.dp))
    }
}

private fun previewCeremony() = CeremonyContent(5, "Marlow Spire", CityCalculator.defaultLook(FacadeKit.Deco), 24, 11, 24_310, 120)

@ScreenPreviews
@Composable
private fun CeremonyPreview() = PreviewSurface { CeremonyScreen(CeremonyUiState(Lce.Content(previewCeremony()), sheetVisible = true), {}) }

private const val BLUEPRINT_COUNT_MS = 800
