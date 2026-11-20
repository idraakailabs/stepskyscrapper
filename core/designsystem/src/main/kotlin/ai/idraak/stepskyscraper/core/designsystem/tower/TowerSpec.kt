package ai.idraak.stepskyscraper.core.designsystem.tower

import androidx.compose.runtime.Immutable

/** Facade kit keys, identical to tower.js `ST` keys. */
enum class Facade(val key: String) { Deco("deco"), Glass("glass"), Brick("brick"), Brut("brut"), Eco("eco") }

/** Crown geometry. [Native] uses the facade's own crown (tower.js behaviour). */
enum class CrownStyle { Native, Ziggurat, Mast, Tank, Cantilever, Garden, Terrace, Gilded, Beacon }

/** tower.js `crown` prop: "auto" draws the crown when complete, "lift" animates it in, "none" hides it. */
enum class CrownMode { Auto, Lift, None }

/** Lit-window colour sets (Customise › Window lighting). [Warm] is tower.js `LITC`. */
enum class LightsStyle { Warm, Office, Moonlit, Festival, Twinkle }

/** Ground plates. Plaza and Park are tower.js `GR`; the rest are cosmetic extensions. */
enum class GroundStyle { Plaza, Park, Plaque, Canal, Tram, Market, None }

/** tower.js `time`: tints every face toward the sky colour and switches on lit windows. */
enum class TowerTime { Dawn, Day, Dusk, Night }

/** Optional palette override (light face, shaded face, trim) as 0xRRGGBB. */
@Immutable
data class TowerPalette(val light: Int, val mid: Int, val trim: Int)

/** Everything needed to draw one tower (props of `SSTower`). */
@Immutable
data class TowerSpec(
    val facade: Facade = Facade.Deco,
    val floors: Int = 10,
    val target: Int = floors,
    /** Fraction of the next floor already built (0..1). */
    val progress: Float = 0f,
    val crane: Boolean = false,
    val crownMode: CrownMode = CrownMode.Auto,
    val crownStyle: CrownStyle = CrownStyle.Native,
    val palette: TowerPalette? = null,
    val lights: LightsStyle = LightsStyle.Warm,
    val ground: GroundStyle = GroundStyle.Plaza,
    val time: TowerTime = TowerTime.Day,
    val seed: Int = 1,
    /** Single-colour silhouette tint (small widget), 0xRRGGBB. */
    val mono: Int? = null,
)

@Immutable
data class SkylineTower(
    val facade: Facade,
    val floors: Int,
    val target: Int = floors,
    val crownStyle: CrownStyle = CrownStyle.Native,
    val palette: TowerPalette? = null,
    val lights: LightsStyle = LightsStyle.Warm,
)

/** Props of `SSSkyline`. */
@Immutable
data class SkylineSpec(
    val towers: List<SkylineTower>,
    val selected: Int = -1,
    val time: TowerTime = TowerTime.Day,
    val ground: GroundStyle = GroundStyle.Park,
    /** Lift the selected tower by 4dp (stats sheet). */
    val liftSelected: Boolean = false,
)

/** Which animations to run (tower.js `animate`), plus the re-styling variants used on 1d and Build. */
@Immutable
data class TowerAnimation(
    /** Floors with index ≥ this drop in; -1 also drops the lobby (tower.js "drop"). null = no drop. */
    val dropFrom: Int? = null,
    val staggerMs: Int = 140,
    val liftCrown: Boolean = false,
    val lights: Boolean = false,
    /** Change to replay. */
    val key: Any = Unit,
) {
    val isEmpty: Boolean get() = dropFrom == null && !liftCrown && !lights

    companion object {
        val None = TowerAnimation()
        val Drop = TowerAnimation(dropFrom = -1)
        val Lights = TowerAnimation(lights = true)
        val Ceremony = TowerAnimation(liftCrown = true, lights = true)
    }
}
