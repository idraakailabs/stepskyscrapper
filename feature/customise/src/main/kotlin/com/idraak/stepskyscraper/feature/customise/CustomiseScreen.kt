package com.idraak.stepskyscraper.feature.customise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idraak.stepskyscraper.core.designsystem.component.SheetHandle
import com.idraak.stepskyscraper.core.designsystem.component.SsFilterChip
import com.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import com.idraak.stepskyscraper.core.designsystem.component.SsModalSheet
import com.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import com.idraak.stepskyscraper.core.designsystem.component.SsSnackbar
import com.idraak.stepskyscraper.core.designsystem.component.SsStatusChip
import com.idraak.stepskyscraper.core.designsystem.component.SsTextButton
import com.idraak.stepskyscraper.core.designsystem.component.StatusChipKind
import com.idraak.stepskyscraper.core.designsystem.component.ssShadow
import com.idraak.stepskyscraper.core.designsystem.icon.IconSize
import com.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import com.idraak.stepskyscraper.core.designsystem.icon.Symbol
import com.idraak.stepskyscraper.core.designsystem.theme.SsElevation
import com.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import com.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import com.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.designsystem.tower.TowerAnimation
import com.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import com.idraak.stepskyscraper.core.designsystem.tower.TowerFit
import com.idraak.stepskyscraper.core.domain.achievements.AchievementCatalog
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCategory
import com.idraak.stepskyscraper.core.domain.cosmetics.NATIVE_CROWN
import com.idraak.stepskyscraper.core.domain.cosmetics.NATIVE_PALETTE
import com.idraak.stepskyscraper.core.domain.cosmetics.UnlockRule
import com.idraak.stepskyscraper.core.domain.model.TowerLook
import com.idraak.stepskyscraper.core.domain.usecase.LockState
import com.idraak.stepskyscraper.core.domain.usecase.SkyTime
import com.idraak.stepskyscraper.core.ui.R as UiR
import com.idraak.stepskyscraper.core.ui.component.SystemBarsEffect
import com.idraak.stepskyscraper.core.ui.format.Formats
import com.idraak.stepskyscraper.core.ui.mvi.Lce
import com.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import com.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import com.idraak.stepskyscraper.core.ui.tower.CosmeticVisuals
import com.idraak.stepskyscraper.core.ui.tower.toSpec
import kotlinx.coroutines.delay

@Composable
fun CustomiseRoute(onClose: () -> Unit, onOpenPaywall: () -> Unit, viewModel: CustomiseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { e ->
            when (e) {
                CustomiseEffect.Close -> onClose()
                CustomiseEffect.OpenPaywall -> onOpenPaywall()
            }
        }
    }
    CustomiseScreen(state, onClose, viewModel::onIntent)
}

@Composable
fun CustomiseScreen(state: CustomiseUiState, onClose: () -> Unit, onIntent: (CustomiseIntent) -> Unit) {
    val c = SsTheme.colors
    val content = (state.content as? Lce.Content)?.value
    val look = state.shownLook
    SystemBarsEffect(lightIcons = false)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to Color(0xFF9CC4E4),
                    0.3f to Color(0xFFC9DFEE),
                    0.4f to Color(0xFFEAF1F4),
                ),
            ),
    ) {
        if (content != null && look != null) {
            Box(Modifier.padding(top = top + SsSpacing.xs).align(Alignment.TopCenter).size(150.dp, 172.dp)) {
                TowerCanvas(
                    look.toSpec(content.floors.coerceAtLeast(1), content.target, SkyTime.Day, crane = false),
                    stringResource(R.string.cus_tower_preview, content.towerName),
                    Modifier.fillMaxSize(),
                    animation =
                    TowerAnimation(
                        dropFrom = 0,
                        staggerMs = SsMotion.RESKIN_MS / content.floors.coerceAtLeast(1),
                        key = state.reskinKey,
                    )
                        .takeIf { state.reskinKey > 0 } ?: TowerAnimation.None,
                )
                if (state.previewing != null) {
                    SsStatusChip(stringResource(R.string.cus_preview), StatusChipKind.Neutral, Modifier.align(Alignment.TopCenter))
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.735f)
                .ssShadow(SsElevation.level2, SsRadius.shapeSheet)
                .background(c.surface, SsRadius.shapeSheet)
                .padding(top = 10.dp),
        ) {
            SheetHandle(Modifier.align(Alignment.CenterHorizontally))
            Row(
                Modifier.fillMaxWidth().padding(start = SsSpacing.screenMargin, end = SsSpacing.xs, top = SsSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.cus_title),
                    style = SsTheme.type.titleBold,
                    color = c.onSurface,
                    modifier = Modifier.weight(1f),
                )
                content?.let {
                    Row(
                        Modifier.height(32.dp).background(c.background, SsRadius.shapeFull).padding(horizontal = SsSpacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SsSpacing.xs),
                    ) {
                        SsIcon(Symbol.Architecture, size = IconSize.sm, tint = c.primaryText)
                        Text(
                            stringResource(UiR.string.blueprints_count, Formats.steps(it.blueprints)),
                            style = SsTheme.type.labelSBold,
                            color = c.onSurface,
                        )
                    }
                }
                SsIconButton(Symbol.Close, stringResource(R.string.cus_close), onClose)
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = SsSpacing.screenMargin),
                horizontalArrangement = Arrangement.spacedBy(SsSpacing.s),
            ) {
                CosmeticCategory.entries.forEach { tab ->
                    SsFilterChip(stringResource(tabLabel(tab)), state.tab == tab, { onIntent(CustomiseIntent.SelectTab(tab)) })
                }
            }
            Column(
                Modifier.weight(
                    1f,
                ).verticalScroll(rememberScrollState()).padding(horizontal = SsSpacing.screenMargin, vertical = SsSpacing.m),
            ) {
                if (content != null) {
                    val tiles = content.tiles[state.tab].orEmpty()
                    when (state.tab) {
                        CosmeticCategory.Facade, CosmeticCategory.Crown -> TowerGrid(tiles, state.draft, onIntent)
                        CosmeticCategory.Palette -> PaletteGrid(tiles, state.draft, onIntent)
                        CosmeticCategory.Lights -> LightsList(tiles, state.draft, onIntent)
                        CosmeticCategory.Ground -> GroundChips(tiles, state.draft, onIntent)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(c.surface)) {
                HorizontalDivider(thickness = 1.dp, color = c.divider)
                SsPrimaryButton(
                    stringResource(R.string.cus_done),
                    { onIntent(CustomiseIntent.Done) },
                    Modifier.fillMaxWidth().windowInsetsPadding(
                        WindowInsets.navigationBars,
                    ).padding(start = SsSpacing.screenMargin, end = SsSpacing.screenMargin, top = SsSpacing.m, bottom = SsSpacing.l),
                    enabled = !state.saving,
                )
            }
        }
        state.explaining?.let { Explainer(it, content?.blueprints ?: 0, onIntent) }
        val message = state.message
        LaunchedEffect(message) {
            if (message != null) {
                delay(SsMotion.SNACKBAR_VISIBLE_MS)
                onIntent(CustomiseIntent.MessageShown)
            }
        }
        SsSnackbar(
            message != null,
            message?.resolve() ?: "",
            Modifier.align(Alignment.BottomCenter).padding(SsSpacing.l).padding(bottom = 80.dp),
            icon = Symbol.Info,
        )
    }
}

private fun tabLabel(tab: CosmeticCategory) = when (tab) {
    CosmeticCategory.Facade -> R.string.cus_tab_facade
    CosmeticCategory.Palette -> R.string.cus_tab_palette
    CosmeticCategory.Crown -> R.string.cus_tab_crown
    CosmeticCategory.Lights -> R.string.cus_tab_lights
    CosmeticCategory.Ground -> R.string.cus_tab_ground
}

/** "Build 200 floors", "400 Blueprints", "Architect Pass"… always text + icon, never colour only. */
@Composable
internal fun lockLabel(lock: LockState, short: Boolean = false): String? = when (lock) {
    LockState.Unlocked -> null
    is LockState.Progress -> when (val r = lock.rule) {
        is UnlockRule.FloorsBuilt -> if (short) {
            stringResource(
                R.string.cus_lock_floors_short,
                r.floors,
            )
        } else {
            stringResource(R.string.cus_lock_floors, r.floors)
        }
        is UnlockRule.TowersFinished -> stringResource(R.string.cus_lock_towers, r.towers)
        is UnlockRule.BuildDays -> if (short) {
            stringResource(
                R.string.cus_lock_days,
                r.days,
            )
        } else {
            stringResource(R.string.cus_lock_days_long, r.days)
        }
        is UnlockRule.ArchitectLevel -> stringResource(R.string.cus_lock_level, r.level)
        else -> null
    }
    is LockState.Price -> stringResource(R.string.cus_lock_price, lock.cost)
    LockState.Premium -> stringResource(R.string.cus_lock_pass)
    is LockState.Achievement -> stringResource(
        R.string.cus_lock_achievement,
        AchievementCatalog.definition(lock.achievementId)?.title.orEmpty(),
    )
}

@Composable
private fun TowerGrid(tiles: List<CosmeticTile>, draft: TowerLook?, onIntent: (CustomiseIntent) -> Unit) {
    val c = SsTheme.colors
    tiles.chunked(3).forEach { row ->
        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { tile ->
                val selected = CustomiseReducer.isSelected(draft, tile.item)
                val locked = tile.lock != LockState.Unlocked
                val label = lockLabel(tile.lock)
                val previewLook = (draft ?: return@forEach).let { CustomiseReducer.apply(it, tile.item) }
                val name = if (tile.item.id == NATIVE_CROWN) tile.item.name else tile.item.name
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected, role = Role.RadioButton) { onIntent(CustomiseIntent.TapItem(tile)) }
                        .semantics { stateDescription = label ?: "" },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val premium = tile.lock == LockState.Premium
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .background(
                                if (premium) {
                                    SsTheme.brand.passSurface
                                } else if (locked) {
                                    c.surfaceVariant
                                } else {
                                    c.background
                                },
                                SsRadius.shapeMd,
                            )
                            .then(if (selected) Modifier.border(2.dp, c.ink, SsRadius.shapeMd) else Modifier)
                            .padding(SsSpacing.s),
                    ) {
                        TowerCanvas(
                            previewLook.toSpec(6, 6, if (premium) SkyTime.Dusk else SkyTime.Day, crane = false, showGround = false),
                            name,
                            Modifier.fillMaxSize().then(if (locked && !premium) Modifier.alpha(0.35f).graphicsLayer { } else Modifier),
                            fit = TowerFit(pad = 2f),
                        )
                        when {
                            selected -> Box(
                                Modifier.align(Alignment.TopEnd).size(22.dp).background(c.ink, RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                SsIcon(Symbol.Check, size = 15.dp, tint = c.onInk)
                            }
                            premium -> SsIcon(
                                Symbol.WorkspacePremium,
                                Modifier.align(Alignment.TopEnd),
                                IconSize.sm,
                                SsTheme.brand.passAccent,
                                filled = true,
                            )
                            tile.lock is LockState.Price -> Row(
                                Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SsIcon(Symbol.Architecture, size = IconSize.sm, tint = c.primaryText)
                                Text((tile.lock as LockState.Price).cost.toString(), style = SsTheme.type.labelSBold, color = c.onSurface)
                            }
                            locked -> SsIcon(Symbol.Lock, Modifier.align(Alignment.Center), IconSize.l, c.onSurface)
                        }
                    }
                    Text(
                        name,
                        style = if (selected ||
                            locked
                        ) {
                            SsTheme.type.labelSBold
                        } else {
                            SsTheme.type.bodyXS
                        },
                        color = c.onSurface,
                        maxLines = 1,
                    )
                    if (label != null) Text(label, style = SsTheme.type.captionRegular, color = c.onSurfaceVariant, maxLines = 2)
                }
            }
            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PaletteGrid(tiles: List<CosmeticTile>, draft: TowerLook?, onIntent: (CustomiseIntent) -> Unit) {
    val c = SsTheme.colors
    Text(
        stringResource(R.string.cus_section_palette),
        style = SsTheme.type.bodyMStrong,
        color = c.onSurface,
        modifier = Modifier.padding(bottom = 10.dp),
    )
    val kit = draft?.let { CosmeticCatalog.kitOf(it.facadeId) }
    tiles.chunked(4).forEach { row ->
        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { tile ->
                val selected = CustomiseReducer.isSelected(draft, tile.item)
                val locked = tile.lock != LockState.Unlocked
                val swatch = if (tile.item.id ==
                    NATIVE_PALETTE
                ) {
                    kit?.let { CosmeticVisuals.nativeSwatch[it] }
                } else {
                    CosmeticVisuals.palettes[tile.item.id]
                }
                Column(
                    Modifier.weight(1f).selectable(selected, role = Role.RadioButton) { onIntent(CustomiseIntent.TapItem(tile)) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            Modifier
                                .size(68.dp, 56.dp)
                                .then(if (selected) Modifier.border(2.dp, c.ink, RoundedCornerShape(16.dp)).padding(4.dp) else Modifier)
                                .alpha(if (locked) 0.4f else 1f)
                                .background(Color.Transparent, RoundedCornerShape(14.dp)),
                        ) {
                            listOfNotNull(swatch?.light, swatch?.mid, swatch?.trim).forEachIndexed { i, rgb ->
                                val shape = when (i) {
                                    0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                    2 -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                                Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF000000.toInt() or rgb), shape))
                            }
                        }
                        when {
                            tile.lock == LockState.Premium -> SsIcon(
                                Symbol.WorkspacePremium,
                                size = IconSize.ml,
                                tint = c.primaryText,
                                filled = true,
                            )
                            locked -> SsIcon(Symbol.Lock, size = IconSize.ml, tint = c.onSurface)
                        }
                    }
                    Text(
                        tile.item.name,
                        style = if (selected) SsTheme.type.captionBold else SsTheme.type.captionRegular,
                        color = c.onSurface,
                        maxLines = 1,
                    )
                    lockLabel(
                        tile.lock,
                        short = true,
                    )?.let { Text(it, style = SsTheme.type.captionRegular, color = c.onSurfaceVariant, maxLines = 1) }
                }
            }
            repeat(4 - row.size) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun LightsList(tiles: List<CosmeticTile>, draft: TowerLook?, onIntent: (CustomiseIntent) -> Unit) {
    val c = SsTheme.colors
    Text(stringResource(R.string.cus_section_lights), style = SsTheme.type.bodyMStrong, color = c.onSurface)
    tiles.forEachIndexed { i, tile ->
        val selected = CustomiseReducer.isSelected(draft, tile.item)
        val locked = tile.lock != LockState.Unlocked
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .selectable(selected, role = Role.RadioButton) { onIntent(CustomiseIntent.TapItem(tile)) }
                .padding(vertical = SsSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SsSpacing.m),
        ) {
            val colors = lightSwatch(tile.item.id)
            Column(
                Modifier.size(36.dp).alpha(
                    if (locked &&
                        tile.lock != LockState.Premium
                    ) {
                        0.45f
                    } else {
                        1f
                    },
                ).background(Color(0xFF1A2647), RoundedCornerShape(10.dp)).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                colors.chunked(2).forEach { pair ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        pair.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF000000.toInt() or it))) }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(tile.item.name, style = SsTheme.type.bodyM, color = c.onSurface)
                lockLabel(tile.lock)?.let { Text(it, style = SsTheme.type.captionRegular, color = c.onSurfaceVariant) }
            }
            when {
                tile.lock == LockState.Premium -> SsIcon(Symbol.WorkspacePremium, size = IconSize.m, tint = c.primaryText, filled = true)
                locked -> SsIcon(Symbol.Lock, size = IconSize.m, tint = c.onSurfaceVariant)
                else -> SsIcon(
                    if (selected) Symbol.RadioButtonChecked else Symbol.RadioButtonUnchecked,
                    size = IconSize.ml,
                    tint = if (selected) c.onSurface else c.onSurfaceVariant,
                    filled = selected,
                )
            }
        }
        if (i < tiles.lastIndex) HorizontalDivider(thickness = 1.dp, color = c.divider)
    }
}

private fun lightSwatch(id: String): List<Int> = when (id) {
    "lights_office" -> listOf(0xEAF2F8, 0x3A4466, 0xEAF2F8, 0xEAF2F8)
    "lights_moonlit" -> listOf(0xC9DBF5, 0xC9DBF5, 0x3A4466, 0xC9DBF5)
    "lights_festival" -> listOf(0xF6B7C8, 0xA8D8C8, 0xFFD98A, 0xB8C4F2)
    "lights_twinkle" -> listOf(0xFFD98A, 0xFFE9B8, 0x3A4466, 0xFFD98A)
    else -> listOf(0xFFD98A, 0xFFD98A, 0xFFD98A, 0x3A4466)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroundChips(tiles: List<CosmeticTile>, draft: TowerLook?, onIntent: (CustomiseIntent) -> Unit) {
    val c = SsTheme.colors
    Text(
        stringResource(R.string.cus_section_ground),
        style = SsTheme.type.bodyMStrong,
        color = c.onSurface,
        modifier = Modifier.padding(bottom = 10.dp),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
        tiles.forEach { tile ->
            val selected = CustomiseReducer.isSelected(draft, tile.item)
            val label = lockLabel(tile.lock, short = true)
            when (tile.lock) {
                LockState.Unlocked -> SsFilterChip(tile.item.name, selected, { onIntent(CustomiseIntent.TapItem(tile)) }, height = 40.dp)
                LockState.Premium -> SsFilterChip(
                    tile.item.name,
                    false,
                    { onIntent(CustomiseIntent.TapItem(tile)) },
                    height = 40.dp,
                    leading = Symbol.WorkspacePremium,
                    leadingFilled = true,
                    container = SsTheme.brand.passSurface,
                    content = SsTheme.brand.passAccent,
                )
                else -> SsFilterChip(
                    "${tile.item.name} · $label",
                    false,
                    { onIntent(CustomiseIntent.TapItem(tile)) },
                    height = 40.dp,
                    leading = Symbol.Lock,
                    container = c.surfaceVariant,
                    content = c.bodySecondary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Explainer(tile: CosmeticTile, balance: Int, onIntent: (CustomiseIntent) -> Unit) {
    val c = SsTheme.colors
    SsModalSheet(onDismiss = { onIntent(CustomiseIntent.DismissExplainer) }) {
        Column(
            Modifier.padding(start = SsSpacing.screenMargin, end = SsSpacing.screenMargin, bottom = 40.dp, top = SsSpacing.s),
            verticalArrangement = Arrangement.spacedBy(SsSpacing.m),
        ) {
            Text(tile.item.name, style = SsTheme.type.sheetTitle, color = c.onSurface)
            val body = when (val l = tile.lock) {
                is LockState.Progress -> stringResource(R.string.cus_explain_progress, l.current, l.required)
                is LockState.Price -> if (l.affordable) {
                    stringResource(
                        R.string.cus_explain_price,
                        l.cost,
                        balance,
                    )
                } else {
                    stringResource(R.string.cus_explain_price_short, l.cost, balance)
                }
                is LockState.Achievement -> AchievementCatalog.definition(l.achievementId).let {
                    stringResource(R.string.cus_explain_achievement, it?.title.orEmpty(), it?.description.orEmpty())
                }
                LockState.Premium -> stringResource(R.string.cus_explain_pass)
                LockState.Unlocked -> ""
            }
            lockLabel(tile.lock)?.let { SsStatusChip(it, if (tile.lock == LockState.Premium) StatusChipKind.Pass else StatusChipKind.Lock) }
            Text(body, style = SsTheme.type.bodyS, color = c.bodySecondary)
            when (val l = tile.lock) {
                is LockState.Price -> SsPrimaryButton(
                    stringResource(R.string.cus_unlock_for, l.cost),
                    { onIntent(CustomiseIntent.BuyWithBlueprints(tile.item.id)) },
                    Modifier.fillMaxWidth(),
                    enabled = l.affordable,
                    icon = Symbol.Architecture,
                )
                LockState.Premium -> SsPrimaryButton(
                    stringResource(R.string.cus_see_pass),
                    { onIntent(CustomiseIntent.OpenPass) },
                    Modifier.fillMaxWidth(),
                )
                else -> SsTextButton(
                    stringResource(R.string.cus_ok),
                    { onIntent(CustomiseIntent.DismissExplainer) },
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

internal fun previewCustomise(tab: CosmeticCategory): CustomiseUiState {
    val look = com.idraak.stepskyscraper.core.domain.usecase.CityCalculator.defaultLook(
        com.idraak.stepskyscraper.core.domain.model.FacadeKit.Deco,
    )
    val context = com.idraak.stepskyscraper.core.domain.usecase.UnlockContext(
        com.idraak.stepskyscraper.core.domain.achievements.CityStats.Empty.copy(
            totalFloors = 100,
            towersFinished = 5,
            buildDays = 38,
            architectLevel = 7,
        ),
        isPremium = false,
        purchased = emptySet(),
        earnedAchievements = setOf("five_towers"),
        blueprintBalance = 340,
    )
    val tiles = CustomiseReducer.tiles(context, com.idraak.stepskyscraper.core.domain.usecase.ResolveCosmeticLockUseCase())
    return CustomiseUiState(Lce.Content(CustomiseContent(5, "Marlow Spire", 14, 24, look, tiles, 340)), draft = look, tab = tab)
}

@ScreenPreviews
@Composable
private fun CustomiseFacadePreview() = PreviewSurface { CustomiseScreen(previewCustomise(CosmeticCategory.Facade), {}, {}) }

@ScreenPreviews
@Composable
private fun CustomisePalettePreview() = PreviewSurface { CustomiseScreen(previewCustomise(CosmeticCategory.Palette), {}, {}) }

@ScreenPreviews
@Composable
private fun CustomiseLightsPreview() = PreviewSurface { CustomiseScreen(previewCustomise(CosmeticCategory.Lights), {}, {}) }
