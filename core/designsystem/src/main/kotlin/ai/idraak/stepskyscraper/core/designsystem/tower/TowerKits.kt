package ai.idraak.stepskyscraper.core.designsystem.tower

import kotlin.math.floor
import kotlin.math.sin

/** Exact constants and colours from tower.js. Colours are 0xRRGGBB. */
internal object TowerConst {
    const val C = 0.866f
    const val W = 36f
    const val LOB = 13f
    const val FH = 8f

    const val AMBER = 0xE8A23A
    const val AMBER_D = 0x7A5212
    const val PLANT = 0x7EA26A
    const val PLANT2 = 0x5E8A50
    const val TRUNK = 0x7A5C3E
    const val LEAF_LIGHT = 0x94B57F
    const val LITC = 0xFFD98A
    const val WHITE = 0xFFFFFF
    const val BLACK = 0x000000
    const val GHOST_DAY = 0x6A635A
    const val GHOST_NIGHT = 0xC9C2E0
    const val STREET = 0xE6DFD3

    /** SK: sky tint colour + strength per time. */
    fun skyTint(time: TowerTime): Pair<Int, Float>? = when (time) {
        TowerTime.Dawn -> 0xF2B9A0 to 0.12f
        TowerTime.Dusk -> 0x4E3F7A to 0.3f
        TowerTime.Night -> 0x121A33 to 0.58f
        TowerTime.Day -> null
    }

    /** LIT: share of windows lit. */
    fun litRatio(time: TowerTime): Float = when (time) {
        TowerTime.Night -> 0.62f
        TowerTime.Dusk -> 0.38f
        else -> 0f
    }
}

/** One facade kit (tower.js `ST[style]`). */
internal data class Kit(
    val left: Int,
    val right: Int,
    val top: Int,
    val win: Int,
    val lobbyL: Int,
    val lobbyR: Int,
    val setbacks: List<Pair<Float, Float>>,
    val trim: Int? = null,
    val mull: Int? = null,
    val sill: Int? = null,
    val slab: Int? = null,
    val plant: Int? = null,
) {
    /** Applies a palette override while keeping the kit's window colour and details readable. */
    fun withPalette(p: TowerPalette?): Kit {
        if (p == null) return this
        return copy(
            left = p.light,
            right = p.mid,
            top = ColorMath.mix(p.light, TowerConst.WHITE, 0.45f),
            lobbyL = ColorMath.mix(p.light, p.mid, 0.4f),
            lobbyR = ColorMath.mix(p.mid, TowerConst.BLACK, 0.08f),
            trim = trim?.let { p.trim },
            mull = mull?.let { ColorMath.mix(p.light, TowerConst.WHITE, 0.3f) },
            sill = sill?.let { ColorMath.mix(p.light, TowerConst.WHITE, 0.6f) },
            slab = slab?.let { ColorMath.mix(p.light, TowerConst.WHITE, 0.2f) },
        )
    }

    companion object {
        fun of(facade: Facade): Kit = when (facade) {
            Facade.Deco -> Kit(
                0xE9DCC3,
                0xCBB793,
                0xF4ECDD,
                0x5B503F,
                0xDCC9A6,
                0xBCA67F,
                listOf(0.55f to 0.8f, 0.82f to 0.58f),
                trim = 0xB8873A,
            )
            Facade.Glass -> Kit(
                0xA8BFCB,
                0x7D98A8,
                0xD3DFE5,
                0x4D6A7D,
                0x90A8B5,
                0x6E8899,
                listOf(0.72f to 0.86f),
                mull = 0xC8D7DF,
            )
            Facade.Brick -> Kit(
                0xC57B5D,
                0xA26048,
                0xDDB09A,
                0x3E3430,
                0xB46B50,
                0x925540,
                listOf(0.78f to 0.84f),
                sill = 0xF1E7D9,
            )
            Facade.Brut -> Kit(
                0xC6C1B8,
                0xA5A097,
                0xD8D4CC,
                0x47433E,
                0xB4AFA6,
                0x948F86,
                emptyList(),
                slab = 0xDAD6CE,
            )
            Facade.Eco -> Kit(
                0xEEEEE4,
                0xCED1C4,
                0xF5F5ED,
                0x5C7268,
                0xDCDED1,
                0xBFC2B4,
                listOf(0.62f to 0.84f),
                plant = 0x7EA26A,
            )
        }
    }
}

/** tower.js GR. */
internal data class GroundColors(val top: Int, val left: Int, val right: Int) {
    companion object {
        val Plaza = GroundColors(0xE2DACD, 0xCEC4B4, 0xB7AC9A)
        val Park = GroundColors(0xC9D5B1, 0xBDB3A1, 0xA39987)
    }
}

internal object ColorMath {
    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF

    /** tower.js `mix`: per channel Math.round(v + (B - v) * t). */
    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int) = Math.round(x + (y - x) * t.toDouble()).toInt().coerceIn(0, 255)
        return (ch(r(a), r(b)) shl 16) or (ch(g(a), g(b)) shl 8) or ch(b(a), b(b))
    }

    fun luminance(c: Int): Float = (r(c) * 0.3f + g(c) * 0.59f + b(c) * 0.11f) / 255f

    /** tower.js `hash`. */
    fun hash(a: Double, b: Double, c: Double): Double {
        val x = sin(a * 127.1 + b * 311.7 + c * 74.7) * 43758.5453
        return x - floor(x)
    }
}
