package ai.idraak.stepskyscraper.feature.share

import ai.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconCircleButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsSegmentedToggle
import ai.idraak.stepskyscraper.core.designsystem.component.SsSnackbar
import ai.idraak.stepskyscraper.core.designsystem.component.SsSwitchRow
import ai.idraak.stepskyscraper.core.designsystem.component.ssShadow
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsElevation
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import ai.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import ai.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import ai.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.domain.usecase.CalculateCityStatsUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.DistrictNames
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import ai.idraak.stepskyscraper.core.ui.tower.CosmeticVisuals
import ai.idraak.stepskyscraper.core.ui.tower.toSkylineTower
import ai.idraak.stepskyscraper.core.ui.tower.toSpec
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** [towerIndex] null opens on the Skyline subject. */
@Serializable
data class ShareRoute(val towerIndex: Int? = null)

@Immutable
data class ShareUiState(
    val subject: ShareSubject = ShareSubject.Tower,
    val format: ShareFormat = ShareFormat.Story,
    val showStats: Boolean = true,
    val tower: TowerCardData? = null,
    val skyline: SkylineCardData? = null,
    val kitNameRes: Int = 0,
    /** Result of the last "Save image" tap, shown as a snackbar until [ShareIntent.MessageShown] (BUG-003). */
    val saveResult: Boolean? = null,
)

sealed interface ShareIntent {
    data class Subject(val subject: ShareSubject) : ShareIntent

    data class Format(val format: ShareFormat) : ShareIntent

    data class Stats(val show: Boolean) : ShareIntent

    data object Shared : ShareIntent

    data class Saved(val success: Boolean) : ShareIntent

    data object MessageShown : ShareIntent
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    savedState: SavedStateHandle,
    progress: GetTowerProgressUseCase,
    private val settings: SettingsRepository,
    private val achievements: AchievementRepository,
    private val analytics: AnalyticsTracker,
) : MviViewModel<ShareUiState, ShareIntent, Unit>(ShareUiState()) {
    private val towerIndex = savedState.toRoute<ShareRoute>().towerIndex

    init {
        viewModelScope.launch {
            progress().collect { s ->
                val tower = s.city.towers.firstOrNull { it.index == towerIndex } ?: s.city.finishedTowers.lastOrNull() ?: s.city.activeTower
                val district = s.districts.lastOrNull()
                val towerCard = TowerCardData(
                    name = tower.name,
                    floors = tower.floorsBuilt,
                    kitName = "",
                    month = Formats.monthYear(tower.toppedOutOn ?: s.today),
                    steps = tower.steps,
                    buildDays = tower.buildDays,
                    spec = tower.toSpec(SkyTime.Dusk, crane = !tower.isComplete),
                )
                val skyline = district?.let { d ->
                    SkylineCardData(
                        d.name,
                        d.towers.map { it.toSkylineTower() },
                        d.floors,
                        d.towers.sumOf { it.steps },
                        d.foundedOn?.let(Formats::dayMonth) ?: "",
                    )
                } ?: SkylineCardData(DistrictNames.forDistrict(0), emptyList(), 0, 0, "")
                reduce {
                    it.copy(
                        subject = if (towerIndex == null && district != null) ShareSubject.Skyline else it.subject,
                        tower = towerCard,
                        skyline = skyline,
                        showStats = s.settings.shareShowStats,
                        kitNameRes = CosmeticVisuals.kitName(CosmeticCatalog.kitOf(tower.record.look.facadeId)),
                    )
                }
            }
        }
    }

    override fun onIntent(intent: ShareIntent) {
        when (intent) {
            is ShareIntent.Subject -> reduce { it.copy(subject = intent.subject) }
            is ShareIntent.Format -> reduce { it.copy(format = intent.format) }
            is ShareIntent.Stats -> viewModelScope.launch {
                reduce { it.copy(showStats = intent.show) }
                settings.setShareShowStats(intent.show)
            }
            is ShareIntent.Saved -> reduce { it.copy(saveResult = intent.success) }
            ShareIntent.MessageShown -> reduce { it.copy(saveResult = null) }
            ShareIntent.Shared -> viewModelScope.launch {
                analytics.track(AnalyticsEvent.ShareCard(currentState.subject.name, currentState.format.name))
                achievements.increment(CalculateCityStatsUseCase.COUNTER_SHARES)
            }
        }
    }
}

@Composable
fun ShareRoute(onClose: () -> Unit, viewModel: ShareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShareScreen(state, onClose, viewModel::onIntent)
}

@Composable
fun ShareScreen(state: ShareUiState, onClose: () -> Unit, onIntent: (ShareIntent) -> Unit) {
    val c = SsTheme.colors
    val platform = LocalPlatformActions.current
    val scope = rememberCoroutineScope()
    val kitName = if (state.kitNameRes != 0) stringResource(state.kitNameRes) else ""
    val tower = state.tower?.copy(kitName = kitName)
    val exportLayer = rememberGraphicsLayer()
    val caption = when (state.subject) {
        ShareSubject.Tower -> stringResource(R.string.share_caption_tower, tower?.name.orEmpty(), tower?.floors ?: 0)
        ShareSubject.Skyline -> stringResource(
            R.string.share_caption_skyline,
            state.skyline?.name.orEmpty(),
            state.skyline?.towers?.size ?: 0,
            state.skyline?.floors ?: 0,
        )
    }
    Box(Modifier.fillMaxSize().background(c.surfaceVariant)) {
        // Off-screen exporter: the card laid out at exactly 1080 px wide, recorded into a graphics layer.
        val density = LocalDensity.current
        val exportW = with(density) {
            (
                if (state.format ==
                    ShareFormat.Story
                ) {
                    ShareCardSize.EXPORT_STORY_W
                } else {
                    ShareCardSize.EXPORT_SQUARE
                }
                ).toDp()
        }
        val exportH = with(density) {
            (
                if (state.format ==
                    ShareFormat.Story
                ) {
                    ShareCardSize.EXPORT_STORY_H
                } else {
                    ShareCardSize.EXPORT_SQUARE
                }
                ).toDp()
        }
        Box(Modifier.graphicsLayer { alpha = 0f }) {
            ShareCard(
                state.subject,
                state.format,
                state.showStats,
                tower,
                state.skyline,
                Modifier.size(exportW, exportH).drawWithContent {
                    exportLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(exportLayer)
                },
            )
        }
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.padding(start = SsSpacing.s, top = SsSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                SsIconButton(Symbol.Close, stringResource(R.string.share_close), onClose)
                Text(stringResource(R.string.share_title), style = SsTheme.type.titleBold, color = c.onSurface)
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = SsSpacing.xs), contentAlignment = Alignment.TopCenter) {
                val previewDesc = stringResource(R.string.card_preview)
                Crossfade(
                    Triple(state.subject, state.format, state.showStats),
                    animationSpec = tween(SsMotion.CROSSFADE_MS),
                    label = "card",
                ) { (subject, format, stats) ->
                    val size = if (format == ShareFormat.Story) Modifier.size(198.dp, 352.dp) else Modifier.size(280.dp, 280.dp)
                    ShareCard(
                        subject,
                        format,
                        stats,
                        tower,
                        state.skyline,
                        size.ssShadow(SsElevation.widget, SsRadius.shapeTileCard).clip(SsRadius.shapeTileCard).semantics {
                            contentDescription =
                                "$previewDesc. $caption"
                        },
                    )
                }
                val saved = state.saveResult
                LaunchedEffect(saved) {
                    if (saved != null) {
                        delay(SsMotion.SNACKBAR_VISIBLE_MS)
                        onIntent(ShareIntent.MessageShown)
                    }
                }
                // Keep the last result while the snackbar slides out, so the text doesn't blank mid-animation.
                val lastSaved = remember { mutableStateOf(true) }
                if (saved != null) lastSaved.value = saved
                SsSnackbar(
                    saved != null,
                    stringResource(if (lastSaved.value) R.string.share_saved else R.string.share_save_failed),
                    Modifier.align(Alignment.BottomCenter).padding(SsSpacing.l),
                    icon = if (lastSaved.value) Symbol.Download else Symbol.Info,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, SsRadius.shapeSheet)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = SsSpacing.screenMargin, end = SsSpacing.screenMargin, top = SsSpacing.l, bottom = SsSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(SsSpacing.m),
            ) {
                SsSegmentedToggle(
                    listOf(stringResource(R.string.share_tower), stringResource(R.string.share_skyline)),
                    state.subject.ordinal,
                    { onIntent(ShareIntent.Subject(ShareSubject.entries[it])) },
                    Modifier.fillMaxWidth(),
                )
                SsSegmentedToggle(
                    listOf(stringResource(R.string.share_story), stringResource(R.string.share_square)),
                    state.format.ordinal,
                    { onIntent(ShareIntent.Format(ShareFormat.entries[it])) },
                    Modifier.fillMaxWidth(),
                )
                SsSwitchRow(stringResource(R.string.share_stats), state.showStats, { onIntent(ShareIntent.Stats(it)) })
                Row(horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                    SsIconCircleButton(
                        Symbol.Download,
                        stringResource(R.string.share_save),
                        {
                            scope.launch {
                                platform.saveImage(exportLayer.toImageBitmap()) { onIntent(ShareIntent.Saved(it)) }
                            }
                        },
                        size = SsSpacing.primaryButtonHeight,
                    )
                    SsPrimaryButton(
                        stringResource(R.string.share_image),
                        {
                            scope.launch {
                                platform.shareImage(exportLayer.toImageBitmap(), caption)
                                onIntent(ShareIntent.Shared)
                            }
                        },
                        Modifier.weight(1f),
                        icon = Symbol.IosShare,
                    )
                }
            }
        }
    }
}

@ScreenPreviews
@Composable
private fun SharePreview() = PreviewSurface {
    val tower = TowerCardData(
        "Marlow Spire",
        24,
        "Art Deco",
        "NOV 2026",
        24_310,
        11,
        ai.idraak.stepskyscraper.core.designsystem.tower.TowerSpec(floors = 24, target = 24),
    )
    ShareScreen(ShareUiState(tower = tower, skyline = SkylineCardData("Juniper Quarter", emptyList(), 100, 100_840, "14 Aug")), {}, {})
}
