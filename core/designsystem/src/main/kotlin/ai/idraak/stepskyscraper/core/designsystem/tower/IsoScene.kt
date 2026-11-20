package ai.idraak.stepskyscraper.core.designsystem.tower

import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.AMBER
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.AMBER_D
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.BLACK
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.C
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.FH
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.LOB
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.W
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.WHITE
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Stroke settings for outline polygons (dashed construction outlines). */
internal data class StrokeSpec(val color: Int, val width: Float, val dash: FloatArray?, val opacity: Float = 1f)

/** Drawable primitives in SVG user units, mirroring the elements tower.js creates. */
internal sealed interface Prim {
    /** Animated lit window: flicker (tower.js ssFlick) or the premium slow twinkle. */
    val flickerDelayMs: Int get() = -1
    val twinklePhase: Float get() = -1f

    data class Poly(
        val points: FloatArray,
        val fill: Int?,
        val stroke: StrokeSpec? = null,
        override val flickerDelayMs: Int = -1,
        override val twinklePhase: Float = -1f,
    ) : Prim

    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val color: Int, val width: Float) : Prim

    data class Circle(val cx: Float, val cy: Float, val r: Float, val color: Int) : Prim
}

internal sealed interface GroupAnim {
    data class Drop(val delayMs: Int) : GroupAnim

    data class Lift(val delayMs: Int) : GroupAnim

    /** Re-style lift-off (1d): the group rises and fades out. */
    data class LiftOff(val delayMs: Int) : GroupAnim
}

internal data class Group(val prims: List<Prim>, val anim: GroupAnim? = null, val opacity: Float = 1f, val liftPx: Float = 0f)

internal data class Bounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

/** A built scene ready to paint: ordered groups (painter's algorithm), bounds and the active-floor anchor. */
internal data class Scene(
    val groups: List<Group>,
    val bounds: Bounds,
    val activeAnchor: Pair<Float, Float>,
    val animationEndMs: Int,
)

/**
 * Port of tower.js `Ctx` + drawing functions. Coordinates: x/y on the ground plane, z up; projected with
 * X = (x − y)·0.866, Y = (x + y)·0.5 − z. The bounding box tracks every projected point, exactly like `pt`.
 */
internal class IsoBuilder(
    private val time: TowerTime,
    private val animation: TowerAnimation,
    var seed: Int,
    private val mono: Int?,
) {
    private var minX = 1e9f
    private var maxX = -1e9f
    private var minY = 1e9f
    private var maxY = -1e9f
    val groups = mutableListOf<Group>()
    var endMs = 0
        private set
    var anchor = 0f to 0f

    fun pt(x: Float, y: Float, z: Float): Pair<Float, Float> {
        val px = (x - y) * C
        val py = (x + y) * 0.5f - z
        if (px < minX) minX = px
        if (px > maxX) maxX = px
        if (py < minY) minY = py
        if (py > maxY) maxY = py
        return px to py
    }

    fun tint(c: Int): Int {
        mono?.let { m ->
            val l = ColorMath.luminance(c)
            return if (l > 0.5f) ColorMath.mix(m, WHITE, (l - 0.5f) * 0.5f) else ColorMath.mix(m, BLACK, (0.5f - l) * 0.5f)
        }
        val sk = TowerConst.skyTint(time) ?: return c
        return ColorMath.mix(c, sk.first, sk.second)
    }

    private fun pts(vararg p: Triple<Float, Float, Float>): FloatArray {
        val out = FloatArray(p.size * 2)
        p.forEachIndexed { i, (x, y, z) ->
            val (a, b) = pt(x, y, z)
            // SVG `p()` rounds to one decimal; keep the same precision for identical output.
            out[i * 2] = (a * 10).roundToInt() / 10f
            out[i * 2 + 1] = (b * 10).roundToInt() / 10f
        }
        return out
    }

    private fun t3(x: Float, y: Float, z: Float) = Triple(x, y, z)

    fun poly(list: MutableList<Prim>, points: FloatArray, fill: Int?, stroke: StrokeSpec? = null) {
        list += Prim.Poly(points, fill, stroke)
    }

    /** `box`: left (front-left) face, right face, optional top, all tinted. */
    fun box(list: MutableList<Prim>, x: Float, y: Float, z: Float, w: Float, d: Float, h: Float, l: Int, r: Int, t: Int?) {
        poly(list, pts(t3(x, y + d, z), t3(x + w, y + d, z), t3(x + w, y + d, z + h), t3(x, y + d, z + h)), tint(l))
        poly(list, pts(t3(x + w, y + d, z), t3(x + w, y, z), t3(x + w, y, z + h), t3(x + w, y + d, z + h)), tint(r))
        if (t != null) poly(list, pts(t3(x, y, z + h), t3(x + w, y, z + h), t3(x + w, y + d, z + h), t3(x, y + d, z + h)), tint(t))
    }

    fun line(list: MutableList<Prim>, a: Pair<Float, Float>, b: Pair<Float, Float>, color: Int, width: Float) {
        list += Prim.Line(a.first, a.second, b.first, b.second, color, width)
    }

    fun circ(list: MutableList<Prim>, c: Pair<Float, Float>, r: Float, fill: Int) {
        list += Prim.Circle(c.first, c.second, r, fill)
    }

    fun group(prims: List<Prim>, anim: GroupAnim? = null, opacity: Float = 1f) {
        groups += Group(prims, anim, opacity)
        when (anim) {
            is GroupAnim.Drop -> endMs = max(endMs, anim.delayMs + 420)
            is GroupAnim.Lift -> endMs = max(endMs, anim.delayMs + 760)
            is GroupAnim.LiftOff -> endMs = max(endMs, anim.delayMs + 80)
            null -> Unit
        }
    }

    /** Top-level primitives outside any `<g>` in tower.js. */
    fun loose(prims: List<Prim>) {
        if (prims.isNotEmpty()) groups += Group(prims)
    }

    // ---- facades -------------------------------------------------------------------------------------

    /** tower.js `pat`: window/detail rectangles for one face: [x0, x1, z0, z1, colour, isWindow]. */
    private fun pattern(kit: Kit, facade: Facade, fw: Float, fh: Float): List<Rect> {
        val r = mutableListOf<Rect>()
        when (facade) {
            Facade.Deco -> {
                val n = max(3, (fw / 8f).roundJs())
                val p = fw / n
                for (i in 0 until n) {
                    val c = (i + 0.5f) * p
                    val h = p * 0.2f
                    r += Rect(c - h, c + h, 1.6f, fh - 1.4f, kit.win, true)
                }
            }
            Facade.Glass -> {
                r += Rect(0.6f, fw - 0.6f, 1f, fh - 0.5f, kit.win, true)
                var c = 5.6f
                while (c < fw - 2) {
                    r += Rect(c - 0.25f, c + 0.25f, 1f, fh - 0.5f, kit.mull ?: kit.top, false)
                    c += 5f
                }
            }
            Facade.Brick -> {
                val n = max(2, (fw / 11f).roundJs())
                val p = fw / n
                for (i in 0 until n) {
                    val c = (i + 0.5f) * p
                    val h = p * 0.22f
                    r += Rect(c - h, c + h, 2.2f, fh - 1.8f, kit.win, true)
                    r += Rect(c - h - 0.6f, c + h + 0.6f, 1.4f, 2.2f, kit.sill ?: kit.top, false)
                }
            }
            Facade.Brut -> {
                r += Rect(0f, fw, 0f, 1.6f, kit.slab ?: kit.top, false)
                val n = max(2, (fw / 15f).roundJs())
                val p = fw / n
                for (i in 0 until n) {
                    val c = (i + 0.5f) * p
                    val h = p * 0.36f
                    r += Rect(c - h, c + h, 2.6f, fh - 1.6f, kit.win, true)
                }
            }
            Facade.Eco -> {
                r += Rect(0.8f, fw - 0.8f, 2.4f, fh - 0.7f, kit.win, true)
                r += Rect(0f, fw, 0f, 1.5f, kit.plant ?: TowerConst.PLANT, false)
                r += Rect(0f, fw, 1.2f, 1.8f, TowerConst.PLANT2, false)
            }
        }
        return r
    }

    private fun lobbyPattern(kit: Kit, fw: Float, side: Int): List<Rect> {
        val band = kit.trim ?: kit.top
        return if (side == 0) {
            listOf(
                Rect(fw / 2 - 4, fw / 2 + 4, 0f, 8.5f, kit.win, true),
                Rect(3f, fw / 2 - 6.5f, 3f, 10f, kit.win, true),
                Rect(fw / 2 + 6.5f, fw - 3f, 3f, 10f, kit.win, true),
                Rect(0f, fw, 11.4f, 13f, band, false),
            )
        } else {
            listOf(
                Rect(3f, 11f, 3f, 10f, kit.win, true),
                Rect(14f, 22f, 3f, 10f, kit.win, true),
                Rect(25f, 33f, 3f, 10f, kit.win, true),
                Rect(0f, fw, 11.4f, 13f, band, false),
            )
        }
    }

    @Suppress("LongParameterList")
    private fun faces(
        list: MutableList<Prim>,
        kit: Kit,
        facade: Facade,
        lights: LightsStyle,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
        d: Float,
        fh: Float,
        fi: Int,
        lobby: Boolean,
    ) {
        for (side in 0..1) {
            val fw = if (side == 1) d else w
            val rects = if (lobby) lobbyPattern(kit, fw, side) else pattern(kit, facade, fw, fh)
            rects.forEachIndexed { ri, r ->
                var color: Int
                var flicker = -1
                var twinkle = -1f
                if (r.window) {
                    val lr = TowerConst.litRatio(time)
                    val hv = ColorMath.hash((fi + seed * 31).toDouble(), side.toDouble(), ri.toDouble())
                    if (lr > 0f && hv < lr) {
                        color = litColor(lights, ri, fi, side)
                        if (animation.lights) {
                            flicker = (ColorMath.hash(ri.toDouble(), fi.toDouble(), (side + 7).toDouble()) * 1400).roundToInt()
                            endMs = max(endMs, flicker + 900)
                        }
                        if (lights == LightsStyle.Twinkle) twinkle = ColorMath.hash(fi.toDouble(), ri.toDouble(), side + 3.0).toFloat()
                    } else {
                        color = tint(if (side == 1) ColorMath.mix(r.color, BLACK, 0.12f) else r.color)
                    }
                } else {
                    color = tint(if (side == 1) ColorMath.mix(r.color, BLACK, 0.1f) else r.color)
                }
                val points = if (side == 0) {
                    pts(
                        t3(x + r.x0, y + d, z + r.z0),
                        t3(x + r.x1, y + d, z + r.z0),
                        t3(x + r.x1, y + d, z + r.z1),
                        t3(
                            x + r.x0,
                            y + d,
                            z + r.z1,
                        ),
                    )
                } else {
                    pts(
                        t3(x + w, y + d - r.x0, z + r.z0),
                        t3(x + w, y + d - r.x1, z + r.z0),
                        t3(x + w, y + d - r.x1, z + r.z1),
                        t3(
                            x + w,
                            y + d - r.x0,
                            z + r.z1,
                        ),
                    )
                }
                list += Prim.Poly(points, color, null, flicker, twinkle)
            }
        }
    }

    private fun litColor(lights: LightsStyle, ri: Int, fi: Int, side: Int): Int = when (lights) {
        LightsStyle.Warm, LightsStyle.Twinkle -> TowerConst.LITC
        LightsStyle.Office -> 0xEAF2F8
        LightsStyle.Moonlit -> 0xC9DBF5
        LightsStyle.Festival -> FESTIVAL[(ColorMath.hash(ri + 3.0, fi.toDouble(), side.toDouble()) * FESTIVAL.size).toInt().coerceIn(0, 3)]
    }

    // ---- decoration ----------------------------------------------------------------------------------

    fun tree(list: MutableList<Prim>, x: Float, y: Float, z: Float, r: Float) {
        line(list, pt(x, y, z), pt(x, y, z + r * 1.3f), tint(TowerConst.TRUNK), 1.1f)
        circ(list, pt(x, y, z + r * 1.7f), r, tint(TowerConst.PLANT))
        circ(list, pt(x + r * 0.3f, y - r * 0.3f, z + r * 2.1f), r * 0.55f, tint(TowerConst.LEAF_LIGHT))
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun crown(list: MutableList<Prim>, kit: Kit, style: CrownStyle, x: Float, y: Float, z: Float, w: Float): Float {
        val cx = x + w / 2
        val cy = y + w / 2
        val trim = kit.trim ?: 0xB8873A
        when (style) {
            CrownStyle.Ziggurat, CrownStyle.Gilded -> {
                val t = if (style == CrownStyle.Gilded) 0xD4A537 else trim
                box(list, x + w * 0.12f, y + w * 0.12f, z, w * 0.76f, w * 0.76f, 5f, kit.left, kit.right, kit.top)
                box(list, x + w * 0.27f, y + w * 0.27f, z + 5, w * 0.46f, w * 0.46f, 6f, kit.left, kit.right, kit.top)
                box(
                    list, x + w * 0.39f, y + w * 0.39f, z + 11, w * 0.22f, w * 0.22f, 7f,
                    ColorMath.mix(t, WHITE, 0.25f), t, ColorMath.mix(t, WHITE, 0.45f),
                )
                line(list, pt(cx, cy, z + 18), pt(cx, cy, z + 34), tint(t), 1.3f)
                circ(list, pt(cx, cy, z + 34.5f), if (style == CrownStyle.Gilded) 2.2f else 1.6f, AMBER)
                return 36f
            }
            CrownStyle.Mast, CrownStyle.Beacon -> {
                box(
                    list, x + w * 0.06f, y + w * 0.06f, z, w * 0.88f, w * 0.88f, 3f, kit.right,
                    ColorMath.mix(
                        kit.right,
                        BLACK,
                        0.12f,
                    ),
                    kit.top,
                )
                box(list, x + w * 0.22f, y + w * 0.22f, z + 3, w * 0.56f, w * 0.56f, 6f, kit.left, kit.right, kit.top)
                line(list, pt(cx, cy, z + 9), pt(cx, cy, z + 28), tint(0x5E6F78), 1f)
                if (style == CrownStyle.Beacon) {
                    line(list, pt(cx - 3, cy, z + 20), pt(cx + 3, cy, z + 20), tint(0x5E6F78), 0.8f)
                    circ(list, pt(cx, cy, z + 28.5f), 2.6f, AMBER)
                    circ(list, pt(cx, cy, z + 28.5f), 1.2f, TowerConst.LITC)
                } else {
                    circ(list, pt(cx, cy, z + 28.5f), 1.4f, AMBER)
                }
                return 30f
            }
            CrownStyle.Tank -> {
                box(
                    list, x - 1.5f, y - 1.5f, z, w + 3, w + 3, 2.4f,
                    ColorMath.mix(kit.right, BLACK, 0.05f), ColorMath.mix(kit.right, BLACK, 0.2f), kit.top,
                )
                val tx = x + w * 0.5f
                val ty = y + w * 0.12f
                val q = 9f
                val zl = z + 2.4f
                listOf(0f to 0f, q to 0f, q to q, 0f to q).forEach { (ox, oy) ->
                    line(list, pt(tx + ox, ty + oy, zl), pt(tx + ox, ty + oy, zl + 5), tint(0x5A4636), 0.9f)
                }
                box(list, tx, ty, zl + 5, q, q, 6f, 0x8C6A52, 0x71553F, 0x9C7A60)
                val ap = pt(tx + q / 2, ty + q / 2, zl + 15)
                val apex = floatArrayOf((ap.first * 10).roundToInt() / 10f, (ap.second * 10).roundToInt() / 10f)
                poly(list, pts(t3(tx, ty + q, zl + 11), t3(tx + q, ty + q, zl + 11)) + apex, tint(0x6B5242))
                poly(list, pts(t3(tx + q, ty + q, zl + 11), t3(tx + q, ty, zl + 11)) + apex, tint(0x57423A))
                return 18f
            }
            CrownStyle.Cantilever -> {
                box(list, x - 3, y - 3, z, w + 6, w + 6, 7f, kit.left, kit.right, kit.top)
                poly(
                    list,
                    pts(
                        t3(x - 1, y + w + 3, z + 2.2f),
                        t3(x + w + 1, y + w + 3, z + 2.2f),
                        t3(x + w + 1, y + w + 3, z + 4.8f),
                        t3(
                            x - 1,
                            y + w + 3,
                            z + 4.8f,
                        ),
                    ),
                    tint(kit.win),
                )
                poly(
                    list,
                    pts(
                        t3(x + w + 3, y + w + 1, z + 2.2f),
                        t3(x + w + 3, y - 1, z + 2.2f),
                        t3(x + w + 3, y - 1, z + 4.8f),
                        t3(
                            x + w + 3,
                            y + w + 1,
                            z + 4.8f,
                        ),
                    ),
                    tint(ColorMath.mix(kit.win, BLACK, 0.15f)),
                )
                box(list, x + w * 0.3f, y + w * 0.3f, z + 7, w * 0.4f, w * 0.4f, 4f, kit.lobbyL, kit.lobbyR, kit.top)
                return 11f
            }
            CrownStyle.Garden -> {
                box(list, x, y, z, w, w, 1.6f, 0x8FA77A, 0x748C62, TowerConst.PLANT)
                tree(list, x + w * 0.28f, y + w * 0.25f, z + 1.6f, 4f)
                tree(list, x + w * 0.75f, y + w * 0.3f, z + 1.6f, 3.4f)
                tree(list, x + w * 0.35f, y + w * 0.72f, z + 1.6f, 3.6f)
                tree(list, x + w * 0.74f, y + w * 0.74f, z + 1.6f, 2.8f)
                return 12f
            }
            CrownStyle.Terrace -> {
                box(list, x, y, z, w, w, 1.6f, 0xC9B79A, 0xA8977C, 0xDCCDB3)
                // Two parasols and a hammock slung between posts.
                listOf(w * 0.3f to w * 0.3f, w * 0.7f to w * 0.62f).forEach { (px, py) ->
                    line(list, pt(x + px, y + py, z + 1.6f), pt(x + px, y + py, z + 9f), tint(0x6B5242), 0.7f)
                    box(list, x + px - 3, y + py - 3, z + 9f, 6f, 6f, 0.8f, 0xE8A23A, 0xC9862A, 0xF0B455)
                }
                line(list, pt(x + w * 0.2f, y + w * 0.85f, z + 5f), pt(x + w * 0.2f, y + w * 0.85f, z + 1.6f), tint(0x6B5242), 0.7f)
                line(list, pt(x + w * 0.8f, y + w * 0.85f, z + 5f), pt(x + w * 0.8f, y + w * 0.85f, z + 1.6f), tint(0x6B5242), 0.7f)
                line(list, pt(x + w * 0.2f, y + w * 0.85f, z + 5f), pt(x + w * 0.5f, y + w * 0.85f, z + 3f), tint(0xD9C7A8), 1.1f)
                line(list, pt(x + w * 0.5f, y + w * 0.85f, z + 3f), pt(x + w * 0.8f, y + w * 0.85f, z + 5f), tint(0xD9C7A8), 1.1f)
                tree(list, x + w * 0.78f, y + w * 0.2f, z + 1.6f, 2.8f)
                return 12f
            }
            CrownStyle.Native -> error("Native crown must be resolved to a facade crown first")
        }
    }

    // ---- tower ---------------------------------------------------------------------------------------

    @Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
    fun drawTower(
        facade: Facade,
        palette: TowerPalette?,
        floorsIn: Int,
        targetIn: Int,
        progress: Float,
        crownMode: CrownMode,
        crownStyle: CrownStyle,
        crane: Boolean,
        lights: LightsStyle,
        ox: Float = 0f,
        oy: Float = 0f,
    ): Float {
        val kit = Kit.of(facade).withPalette(palette)
        val target = max(if (targetIn > 0) targetIn else max(floorsIn, 1), 1)
        val floors = max(0, min(floorsIn, target))
        val dropFrom = animation.dropFrom
        val stagger = animation.staggerMs

        fun sc(i: Int): Float {
            var s = 1f
            kit.setbacks.forEach { (at, scale) -> if (i >= (at * target).roundJs()) s = scale }
            return s
        }

        val lobby = mutableListOf<Prim>()
        box(lobby, ox, oy, 0f, W, W, LOB, kit.lobbyL, kit.lobbyR, kit.top)
        faces(lobby, kit, facade, lights, ox, oy, 0f, W, W, LOB, -1, true)
        group(lobby, if (dropFrom != null && dropFrom < 0) GroupAnim.Drop(0) else null)

        for (i in 0 until floors) {
            val w = W * sc(i)
            val of = (W - w) / 2
            val z = LOB + i * FH
            val fl = mutableListOf<Prim>()
            box(fl, ox + of, oy + of, z, w, w, FH, kit.left, kit.right, kit.top)
            faces(fl, kit, facade, lights, ox + of, oy + of, z, w, w, FH, i, false)
            val anim = if (dropFrom != null && i >= max(dropFrom, 0)) {
                val order = if (dropFrom < 0) i + 1 else i - dropFrom
                GroupAnim.Drop(order * stagger)
            } else {
                null
            }
            group(fl, anim)
        }

        val topZ = LOB + floors * FH
        val lw = W * sc(max(floors - 1, 0))
        val lo = (W - lw) / 2
        var hookZ = topZ + FH + 6
        anchor = pt(ox + W / 2, oy + W / 2, topZ)

        if (floors < target) {
            val loose = mutableListOf<Prim>()
            val gc = if (time == TowerTime.Night || time == TowerTime.Dusk) TowerConst.GHOST_NIGHT else TowerConst.GHOST_DAY
            var start = floors
            if (progress > 0f) {
                val w2 = W * sc(floors)
                val o2 = (W - w2) / 2
                val x = ox + o2
                val y = oy + o2
                val ph = FH * progress.coerceIn(0f, 1f)
                box(mutableListOf(), x, y, topZ, w2, w2, FH, kit.left, kit.right, null)
                val translucent = mutableListOf<Prim>()
                box(translucent, x, y, topZ, w2, w2, FH, kit.left, kit.right, null)
                group(translucent, opacity = 0.28f)
                val solid = mutableListOf<Prim>()
                box(solid, x, y, topZ, w2, w2, ph, kit.left, kit.right, kit.top)
                group(solid)
                val dash = StrokeSpec(AMBER, 0.8f, floatArrayOf(1.6f, 1.6f))
                poly(
                    loose,
                    pts(t3(x, y + w2, topZ), t3(x + w2, y + w2, topZ), t3(x + w2, y + w2, topZ + FH), t3(x, y + w2, topZ + FH)),
                    null,
                    dash,
                )
                poly(
                    loose,
                    pts(t3(x + w2, y + w2, topZ), t3(x + w2, y, topZ), t3(x + w2, y, topZ + FH), t3(x + w2, y + w2, topZ + FH)),
                    null,
                    dash,
                )
                start = floors + 1
                hookZ = topZ + FH + 8
                anchor = pt(ox + W / 2, oy + W / 2, topZ + FH / 2)
            }
            val ghost = StrokeSpec(gc, 0.6f, floatArrayOf(2f, 2.4f), 0.55f)
            var a = start
            while (a < target) {
                val s = sc(a)
                var b2 = a
                while (b2 < target && sc(b2) == s) b2++
                val gw = W * s
                val go = (W - gw) / 2
                val gx = ox + go
                val gy = oy + go
                val za = LOB + a * FH
                val zb = LOB + b2 * FH
                poly(loose, pts(t3(gx, gy + gw, za), t3(gx + gw, gy + gw, za), t3(gx + gw, gy + gw, zb), t3(gx, gy + gw, zb)), null, ghost)
                poly(loose, pts(t3(gx + gw, gy + gw, za), t3(gx + gw, gy, za), t3(gx + gw, gy, zb), t3(gx + gw, gy + gw, zb)), null, ghost)
                if (b2 == target) {
                    poly(loose, pts(t3(gx, gy, zb), t3(gx + gw, gy, zb), t3(gx + gw, gy + gw, zb), t3(gx, gy + gw, zb)), null, ghost)
                }
                a = b2
            }
            loose(loose)
        }

        var ch = 0f
        if (floors >= target && crownMode != CrownMode.None) {
            val cl = mutableListOf<Prim>()
            ch = crown(cl, kit, resolveCrown(crownStyle, facade), ox + lo, oy + lo, topZ, lw)
            group(cl, if (crownMode == CrownMode.Lift || animation.liftCrown) GroupAnim.Lift(200) else null)
            hookZ = topZ + ch + 2
        }

        if (crane) {
            val list = mutableListOf<Prim>()
            val mx = ox + W + 10
            val my = oy + W / 2
            val jz = max(topZ + FH + ch + 20, 60f)
            val col = tint(AMBER)
            val dk = tint(AMBER_D)
            line(list, pt(mx, my, 0f), pt(mx, my, jz + 5), col, 1.8f)
            line(list, pt(mx + 2, my, 0f), pt(mx + 2, my, jz), col, 1.2f)
            var zz = 0f
            while (zz < jz) {
                line(list, pt(mx, my, zz), pt(mx + 2, my, zz + 6), dk, 0.5f)
                zz += 6f
            }
            val hx = ox + W / 2
            line(list, pt(mx + 14, my, jz), pt(ox - 8, my, jz), col, 1.5f)
            line(list, pt(mx, my, jz + 6), pt(ox - 8, my, jz), dk, 0.6f)
            line(list, pt(mx, my, jz + 6), pt(mx + 14, my, jz), dk, 0.6f)
            box(list, mx + 10, my - 2, jz - 5, 4f, 4f, 5f, 0x8A8278, 0x6E675E, 0x9E978C)
            box(list, mx - 3, my - 2, jz - 4, 4f, 4f, 4f, col, dk, col)
            val lifting = crownMode == CrownMode.Lift || animation.liftCrown
            line(list, pt(hx, my, jz), pt(hx, my, hookZ + if (lifting) 0f else 4f), dk, 0.6f)
            if (floors < target) box(list, hx - 7, my - 3, hookZ - 3, 14f, 6f, 3f, kit.left, kit.right, kit.top)
            loose(list)
        }
        return topZ + ch
    }

    // ---- ground --------------------------------------------------------------------------------------

    fun groundPlate(style: GroundStyle) {
        if (style == GroundStyle.None) return
        val colors = if (style == GroundStyle.Park || style == GroundStyle.Tram) GroundColors.Park else GroundColors.Plaza
        val list = mutableListOf<Prim>()
        box(list, -9f, -9f, -3f, W + 18, W + 18, 3f, colors.left, colors.right, colors.top)
        if (style == GroundStyle.Canal) {
            poly(list, pts(t3(-9f, W + 2.5f, 0f), t3(W + 9, W + 2.5f, 0f), t3(W + 9, W + 8.2f, 0f), t3(-9f, W + 8.2f, 0f)), tint(0x8FB3C9))
            line(list, pt(-4f, W + 5f, 0f), pt(W + 2, W + 5f, 0f), tint(0xC6DAE6), 0.5f)
        }
        if (style == GroundStyle.Tram) {
            line(list, pt(-8f, W + 5.2f, 0f), pt(W + 8, W + 5.2f, 0f), tint(0x8A8278), 0.5f)
            line(list, pt(-8f, W + 7.4f, 0f), pt(W + 8, W + 7.4f, 0f), tint(0x8A8278), 0.5f)
        }
        loose(list)
    }

    fun groundDetails(style: GroundStyle) {
        if (style == GroundStyle.None) return
        val list = mutableListOf<Prim>()
        tree(list, -4f, W + 4, 0f, 3.2f)
        tree(list, W + 5, -3f, 0f, 2.8f)
        when (style) {
            GroundStyle.Park -> tree(list, W + 5, W + 5, 0f, 3f)
            GroundStyle.Plaque -> box(list, 6f, W + 3.5f, 0f, 7f, 2f, 3.2f, 0x4A443D, 0x3A3530, 0xB8873A)
            GroundStyle.Tram -> {
                box(list, W + 2, W - 6, 0f, 5f, 2.5f, 5f, 0x9DB6D0, 0x7E97B0, 0x4E6A86)
                tree(list, W + 5, W + 5, 0f, 3f)
            }
            GroundStyle.Market -> listOf(0xE8A23A, 0x9DB6D0, 0xC57B5D).forEachIndexed { i, top ->
                val sx = -6f + i * 13f
                box(list, sx, W + 3.5f, 0f, 7f, 3.5f, 2.4f, 0xE9DCC3, 0xCBB793, null)
                box(list, sx - 0.5f, W + 3f, 2.4f, 8f, 4.5f, 0.8f, top, ColorMath.mix(top, BLACK, 0.15f), ColorMath.mix(top, WHITE, 0.2f))
            }
            else -> Unit
        }
        loose(list)
    }

    fun build(): Scene = Scene(groups.toList(), Bounds(minX, minY, maxX, maxY), anchor, endMs)

    private data class Rect(val x0: Float, val x1: Float, val z0: Float, val z1: Float, val color: Int, val window: Boolean)

    companion object {
        private val FESTIVAL = intArrayOf(0xF6B7C8, 0xA8D8C8, 0xFFD98A, 0xB8C4F2)

        fun resolveCrown(style: CrownStyle, facade: Facade): CrownStyle = if (style != CrownStyle.Native) {
            style
        } else {
            when (facade) {
                Facade.Deco -> CrownStyle.Ziggurat
                Facade.Glass -> CrownStyle.Mast
                Facade.Brick -> CrownStyle.Tank
                Facade.Brut -> CrownStyle.Cantilever
                Facade.Eco -> CrownStyle.Garden
            }
        }
    }
}

/** JavaScript Math.round semantics (half up). */
internal fun Float.roundJs(): Int = kotlin.math.floor(this + 0.5f).toInt()
