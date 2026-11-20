package com.idraak.stepskyscraper.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Time of day used by the sky gradient and the tower tint. Mirrors tower.js `time`. */
enum class SkyTone { Dawn, Day, Dusk, Night }

/** Vertical sky gradient stops ("Sky follows the clock"). */
@Immutable
data class SkyGradient(val stops: List<Pair<Float, Color>>) {
    fun brush(): Brush = Brush.verticalGradient(*stops.toTypedArray())

    /** Linear crossfade between two skies (used for the 20-minute transitions). */
    fun blend(other: SkyGradient, t: Float): SkyGradient {
        val fraction = t.coerceIn(0f, 1f)
        val positions = (stops.map { it.first } + other.stops.map { it.first }).distinct().sorted()
        return SkyGradient(positions.map { p -> p to lerp(colorAt(p), other.colorAt(p), fraction) })
    }

    /** Same gradient but ending in [fade] at [fadeAt] (onboarding skies melt into the background). */
    fun fadingInto(fade: Color, fadeAt: Float): SkyGradient {
        val scale = fadeAt / stops.last().first
        return SkyGradient(stops.dropLast(1).map { (p, c) -> p * scale to c } + (fadeAt to fade))
    }

    fun colorAt(position: Float): Color {
        val p = position.coerceIn(0f, 1f)
        val upper = stops.indexOfFirst { it.first >= p }
        if (upper <= 0) return stops.first().second
        val (p0, c0) = stops[upper - 1]
        val (p1, c1) = stops[upper]
        return lerp(c0, c1, if (p1 == p0) 0f else (p - p0) / (p1 - p0))
    }
}

/** Ink and chrome used on top of the sky (header overline, header buttons). */
@Immutable
data class SkyChrome(
    val title: Color,
    val subtitle: Color,
    val buttonFill: Color,
    val buttonIcon: Color,
    val pillFill: Color,
    val pillText: Color,
    val pillSubtext: Color,
    val pillEmpty: Color,
    val lightStatusBar: Boolean,
)

object SkyTokens {
    val Dawn = SkyGradient(listOf(0f to Color(0xFFF0B8A0), 0.55f to Color(0xFFF8D9C2), 1f to Color(0xFFFCEBDD)))
    val Day = SkyGradient(listOf(0f to Color(0xFF9CC4E4), 0.55f to Color(0xFFC9DFEE), 1f to Color(0xFFEAF1F4)))
    val Dusk = SkyGradient(listOf(0f to Color(0xFF5D4C8C), 0.55f to Color(0xFFA27A9E), 1f to Color(0xFFE8AA8C)))
    val Night = SkyGradient(listOf(0f to Color(0xFF0C1530), 0.6f to Color(0xFF1A2647), 1f to Color(0xFF2C3858)))

    /** 1a Promise: dawn melting into the background at 78%. */
    val OnboardingDawn = SkyGradient(listOf(0f to Color(0xFFF0B8A0), 0.5f to Color(0xFFF8D9C2), 0.78f to Color(0xFFF4F1EC)))

    /** 1c Payoff: day melting into the background at 72%. */
    val OnboardingDay = SkyGradient(listOf(0f to Color(0xFF9CC4E4), 0.45f to Color(0xFFC9DFEE), 0.72f to Color(0xFFF4F1EC)))

    /** 1d preview card and the widget sky panel: two-stop day sky. */
    val CardDay = SkyGradient(listOf(0f to Color(0xFF9CC4E4), 1f to Color(0xFFEAF1F4)))

    /** Medium widget dark sky panel. */
    val CardNight = SkyGradient(listOf(0f to Color(0xFF0C1530), 1f to Color(0xFF2C3858)))

    /** Paywall hero. */
    val PaywallDusk = SkyGradient(listOf(0f to Color(0xFF5D4C8C), 0.6f to Color(0xFFA27A9E), 1f to Color(0xFFE8AA8C)))

    fun gradient(tone: SkyTone) = when (tone) {
        SkyTone.Dawn -> Dawn
        SkyTone.Day -> Day
        SkyTone.Dusk -> Dusk
        SkyTone.Night -> Night
    }

    fun chrome(tone: SkyTone): SkyChrome = when (tone) {
        SkyTone.Day -> SkyChrome(
            title = Color(0xFF24211D), subtitle = Color(0xFF3E4A55),
            buttonFill = Color(0xB3FBFAF7), buttonIcon = Color(0xFF24211D),
            pillFill = Color(0xBFFBFAF7), pillText = Color(0xFF24211D), pillSubtext = Color(0xFF4A443D),
            pillEmpty = Color(0xFFD9D2C8), lightStatusBar = false,
        )
        SkyTone.Dawn -> SkyChrome(
            title = Color(0xFF24211D), subtitle = Color(0xFF6B4A3A),
            buttonFill = Color(0xB3FBFAF7), buttonIcon = Color(0xFF24211D),
            pillFill = Color(0xBFFBFAF7), pillText = Color(0xFF24211D), pillSubtext = Color(0xFF4A443D),
            pillEmpty = Color(0xFFD9D2C8), lightStatusBar = false,
        )
        SkyTone.Dusk -> SkyChrome(
            title = Color(0xFFFBFAF7), subtitle = Color(0xFFEDE3F2),
            buttonFill = Color(0x5924211D), buttonIcon = Color(0xFFFBFAF7),
            pillFill = Color(0x1FFFFFFF), pillText = Color(0xFFFBFAF7), pillSubtext = Color(0xFFEDE3F2),
            pillEmpty = Color(0xFF4A4F66), lightStatusBar = true,
        )
        SkyTone.Night -> SkyChrome(
            title = Color(0xFFF1ECE5), subtitle = Color(0xFFC3CADB),
            buttonFill = Color(0x1AFFFFFF), buttonIcon = Color(0xFFF1ECE5),
            pillFill = Color(0x1FFFFFFF), pillText = Color(0xFFF1ECE5), pillSubtext = Color(0xFFC3CADB),
            pillEmpty = Color(0xFF4A4F66), lightStatusBar = true,
        )
    }

    /** Searchlight beams on the topping-out ceremony (light 28%, dark 22%). */
    val BeamLight = Color(0xFFFFECBE)
    val BeamDark = Color(0xFFFFE2A0)

    /** Home-screen mock wallpaper on 1f. */
    val WallpaperMock = listOf(Color(0xFFD8CFC2), Color(0xFFBDB2A3))
    val WallpaperMockLabel = Color(0xFF5A5248)
}
