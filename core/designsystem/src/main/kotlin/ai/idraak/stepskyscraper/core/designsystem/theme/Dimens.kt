package ai.idraak.stepskyscraper.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Spacing scale 4 · 8 · 12 · 16 · 24 · 32 · 48 and the layout constants from the design. */
object SsSpacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    val screenMargin = 20.dp
    val cardPadding = 20.dp
    val sectionGap = 24.dp
    val onboardingMargin = 24.dp
    val sheetInset = 16.dp
    val bottomCtaInset = 40.dp

    /** Minimum interactive size. */
    val touchTarget = 48.dp
    val primaryButtonHeight = 52.dp
    val buttonHeight = 48.dp
    val chipHeight = 36.dp
    val statusChipHeight = 32.dp
    val navHeight = 80.dp
    val navPillWidth = 64.dp
    val navPillHeight = 32.dp
    val handleWidth = 32.dp
    val handleHeight = 4.dp

    /** Design phone frame metrics (360 × 800) used to place sky-hero elements. */
    val heroHeight = 440.dp
    val skylineHeroHeight = 420.dp
    val towerTop = 118.dp
    val towerHeight = 296.dp
    val sheetTop = 412.dp
    val skylineSheetTop = 404.dp
    val compactHeroHeight = 200.dp
}

/** Corner radii: sm 12 tiles · md 16 cards · lg 24 sheets · full buttons (plus the 20dp cards used on screens). */
object SsRadius {
    val xs = 4.dp
    val sm = 12.dp
    val md = 16.dp
    val card = 20.dp
    val lg = 24.dp
    val option = 18.dp
    val tile = 14.dp
    val shapeXs = RoundedCornerShape(xs)
    val shapeSm = RoundedCornerShape(sm)
    val shapeMd = RoundedCornerShape(md)
    val shapeCard = RoundedCornerShape(card)
    val shapeLg = RoundedCornerShape(lg)
    val shapeSheet = RoundedCornerShape(topStart = lg, topEnd = lg)
    val shapeFull = RoundedCornerShape(50)

    /** Share-card preview corners (14dp in the composer, 16dp on the boards). */
    val shapeTileCard = RoundedCornerShape(tile)
}

/** Elevation: 0 = outline only, 1 = floating card, 2 = sheet / snackbar. Values are the CSS box-shadows. */
object SsElevation {
    data class ShadowLayer(val offsetY: androidx.compose.ui.unit.Dp, val blur: androidx.compose.ui.unit.Dp, val color: Color)

    val level1 = listOf(
        ShadowLayer(1.dp, 2.dp, Color(0x0F3C2D19)),
        ShadowLayer(4.dp, 12.dp, Color(0x0D3C2D19)),
    )
    val level2 = listOf(ShadowLayer((-2).dp, 24.dp, Color(0x1A3C2D19)))
    val widget = listOf(ShadowLayer(8.dp, 24.dp, Color(0x2E3C2D19)))
}

/** Motion specs from "Motion principles". Reduce-motion swaps every move for a 150ms crossfade. */
object SsMotion {
    const val FLOOR_DROP_MS = 420
    const val FLOOR_STAGGER_MS = 140
    const val FLOOR_DROP_DISTANCE = 26f
    const val FLOOR_OVERSHOOT = 1.6f
    const val CROWN_LIFT_MS = 760
    const val CROWN_LIFT_DELAY_MS = 200
    const val CROWN_LIFT_DISTANCE = 44f
    const val CROWN_OVERSHOOT = 1.4f
    const val WINDOW_FLICKER_MS = 900
    const val WINDOW_FLICKER_SPREAD_MS = 1_400
    const val NUMBER_ROLL_MS = 250
    const val SEGMENT_FILL_MS = 160
    const val SNACKBAR_SLIDE_MS = 220
    const val SNACKBAR_VISIBLE_MS = 4_000L
    const val SHEET_MS = 300
    const val PRESS_MS = 90
    const val PRESS_SCALE = 0.97f
    const val PRESS_DARKEN = 0.08f
    const val CHIP_POP_MS = 180
    const val BADGE_FLIP_MS = 300
    const val RESKIN_MS = 400
    const val CROSSFADE_MS = 150
    const val LIFT_OFF_MS = 80
    const val RESTYLE_STAGGER_MS = 60
    const val STEP_COUNT_ROLL_MS = 1_200
    const val CEREMONY_WINDOWS_AT_MS = 900L
    const val CEREMONY_SHEET_AT_MS = 1_600L
    const val CEREMONY_TOTAL_MS = 2_400L
    const val LOCKED_PREVIEW_MS = 3_000L
    const val ZOOM_SCALE = 1.4f

    /** spring(0.6, 400) approximated by the CSS curve the prototype uses. */
    val Drop: Easing = CubicBezierEasing(0.3f, 1.3f, 0.5f, 1f)
    val Lift: Easing = CubicBezierEasing(0.25f, 1f, 0.4f, 1f)

    /** Material 3 emphasized-decelerate (sheets). */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
