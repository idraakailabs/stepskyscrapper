package com.idraak.stepskyscraper.core.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import com.idraak.stepskyscraper.core.designsystem.icon.IconSize
import com.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import com.idraak.stepskyscraper.core.designsystem.icon.Symbol
import com.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import com.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import com.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.designsystem.tower.GroundStyle
import com.idraak.stepskyscraper.core.designsystem.tower.TowerAnimation
import com.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import com.idraak.stepskyscraper.core.designsystem.tower.TowerFit
import com.idraak.stepskyscraper.core.designsystem.tower.TowerSpec
import com.idraak.stepskyscraper.core.domain.model.FacadeKit
import com.idraak.stepskyscraper.core.domain.usecase.GenerateTowerNameUseCase
import com.idraak.stepskyscraper.core.ui.R
import com.idraak.stepskyscraper.core.ui.tower.CosmeticVisuals

/** Sets status-bar icon colour for the sky behind it (light icons on dusk / night). */
@Composable
fun SystemBarsEffect(lightIcons: Boolean) {
    val view = LocalView.current
    val activity = LocalActivity.current ?: return
    // Each screen states what it needs; no restore on dispose (the next screen's effect may already have run).
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !lightIcons
    }
}

/** A small tower thumbnail in a rounded tile (cards, chips, pickers). */
@Composable
fun TowerThumb(
    spec: TowerSpec,
    description: String,
    modifier: Modifier = Modifier,
    tile: androidx.compose.ui.graphics.Color = SsTheme.colors.surfaceVariant,
    radius: Dp = SsRadius.sm,
    padding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(6.dp, 6.dp, 6.dp, 2.dp),
    pad: Float = 6f,
) {
    Box(modifier.background(tile, RoundedCornerShape(radius)).padding(padding)) {
        TowerCanvas(spec, description, Modifier.fillMaxSize(), fit = TowerFit(pad = pad))
    }
}

/**
 * Body of "Pick a style for your first tower" (1d) and "Start next tower": preview card, 5 style tiles
 * (selected = 2dp ink ring + check badge), tower-name field in the display face with a dice button.
 */
@Composable
fun StyleAndNamePicker(
    selected: FacadeKit,
    name: String,
    previewFloors: Int,
    previewTarget: Int,
    animationKey: Any,
    onSelect: (FacadeKit) -> Unit,
    onNameChange: (String) -> Unit,
    onDice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SsTheme.colors
    val previewSpec = TowerSpec(
        facade = CosmeticVisuals.facade(selected),
        floors = previewFloors,
        target = previewTarget,
        ground = if (selected == FacadeKit.Eco) GroundStyle.Park else GroundStyle.Plaza,
    )
    Column(modifier) {
        Box(
            Modifier
                .padding(horizontal = SsSpacing.onboardingMargin)
                .padding(top = SsSpacing.l)
                .fillMaxWidth()
                .height(250.dp)
                .background(SkyTokens.CardDay.brush(), SsRadius.shapeLg)
                .padding(start = 40.dp, end = 40.dp, top = SsSpacing.l, bottom = SsSpacing.s),
        ) {
            TowerCanvas(
                previewSpec,
                stringResource(R.string.style_preview_description, stringResource(CosmeticVisuals.kitName(selected))),
                Modifier.fillMaxSize(),
                animation = TowerAnimation(dropFrom = -1, staggerMs = 60, key = animationKey),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = SsSpacing.l),
            horizontalArrangement = Arrangement.spacedBy(SsSpacing.s),
        ) {
            FacadeKit.entries.forEach { kit ->
                val isSelected = kit == selected
                val label = stringResource(CosmeticVisuals.kitShortName(kit))
                Column(
                    Modifier
                        .width(72.dp)
                        .selectable(isSelected, role = Role.RadioButton) { onSelect(kit) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(72.dp, 80.dp)) {
                        TowerThumb(
                            TowerSpec(facade = CosmeticVisuals.facade(kit), floors = 6, target = 6, ground = GroundStyle.None),
                            label,
                            Modifier
                                .fillMaxSize()
                                .then(if (isSelected) Modifier.border(2.dp, c.ink, SsRadius.shapeMd) else Modifier),
                            tile = if (isSelected) c.surface else c.surfaceVariant,
                            radius = SsRadius.md,
                            padding = androidx.compose.foundation.layout.PaddingValues(6.dp),
                        )
                        if (isSelected) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(6.dp, (-6).dp)
                                    .size(22.dp)
                                    .background(c.ink, RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center,
                            ) { SsIcon(Symbol.Check, size = 15.dp, tint = c.onInk) }
                        }
                    }
                    Text(label, style = if (isSelected) SsTheme.type.captionBold else SsTheme.type.caption, color = c.onSurface)
                }
            }
        }
        Column(
            Modifier.padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = SsSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.tower_name_label), style = SsTheme.type.labelS, color = c.onSurfaceVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(c.surface, SsRadius.shapeMd)
                    .border(2.dp, c.ink, SsRadius.shapeMd)
                    .padding(start = SsSpacing.l, end = SsSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SsSpacing.s),
            ) {
                val fieldLabel = stringResource(R.string.tower_name_label)
                BasicTextField(
                    value = name.uppercase(),
                    onValueChange = { onNameChange(it.take(GenerateTowerNameUseCase.MAX_LENGTH)) },
                    singleLine = true,
                    textStyle = SsTheme.type.towerNameField.copy(color = c.onSurface),
                    cursorBrush = SolidColor(c.onSurface),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f).semantics { contentDescription = fieldLabel },
                )
                SsIconButton(Symbol.Casino, stringResource(R.string.suggest_name), onDice, iconSize = IconSize.ml)
            }
            Text(stringResource(R.string.rename_any_time), style = SsTheme.type.captionRegular, color = c.onSurfaceVariant)
        }
    }
}
