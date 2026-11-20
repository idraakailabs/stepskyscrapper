package ai.idraak.stepskyscraper.core.designsystem.tower

import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.AMBER
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerConst.W

/** `SSTower`: optional ground plate, the tower, then the front trees (drawn over the tower, as in tower.js). */
internal fun buildTowerScene(spec: TowerSpec, animation: TowerAnimation): Scene {
    val b = IsoBuilder(spec.time, animation, spec.seed, spec.mono)
    b.groundPlate(spec.ground)
    b.drawTower(
        facade = spec.facade,
        palette = spec.palette,
        floorsIn = spec.floors,
        targetIn = spec.target,
        progress = spec.progress,
        crownMode = spec.crownMode,
        crownStyle = spec.crownStyle,
        crane = spec.crane,
        lights = spec.lights,
    )
    b.groundDetails(spec.ground)
    return b.build()
}

/**
 * `SSSkyline`: towers stand diagonally on a hexagonal model base (S = W + 16), each unfinished one with
 * its crane and a 60% floor in progress; an amber pin marks the selected / newest tower.
 */
internal fun buildSkylineScene(spec: SkylineSpec, animation: TowerAnimation): Scene {
    val b = IsoBuilder(spec.time, animation, 3, null)
    val s = W + 16
    val m = 11f
    val gc = if (spec.ground == GroundStyle.Plaza) GroundColors.Plaza else GroundColors.Park
    val n = spec.towers.size.coerceAtLeast(1)
    val l = (n - 1) * s
    val th = 4f
    val base = mutableListOf<Prim>()
    fun p(x: Float, y: Float, z: Float): FloatArray {
        val (a, c) = b.pt(x, y, z)
        return floatArrayOf(a, c)
    }
    fun poly(vararg pts: FloatArray, fill: Int) = b.poly(base, pts.reduce { acc, f -> acc + f }, fill)
    poly(p(-m, W + m, 0f), p(W + m, W + m, 0f), p(W + m, W + m, -th), p(-m, W + m, -th), fill = b.tint(gc.left))
    poly(
        p(W + m, W + m, 0f),
        p(l + W + m, -l + W + m, 0f),
        p(l + W + m, -l + W + m, -th),
        p(W + m, W + m, -th),
        fill = b.tint(ColorMath.mix(gc.left, gc.right, 0.5f)),
    )
    poly(
        p(l + W + m, -l + W + m, 0f),
        p(l + W + m, -l - m, 0f),
        p(l + W + m, -l - m, -th),
        p(l + W + m, -l + W + m, -th),
        fill = b.tint(gc.right),
    )
    poly(
        p(-m, -m, 0f),
        p(l - m, -l - m, 0f),
        p(l + W + m, -l - m, 0f),
        p(l + W + m, -l + W + m, 0f),
        p(W + m, W + m, 0f),
        p(-m, W + m, 0f),
        fill = b.tint(gc.top),
    )
    poly(
        p(W + 4, W + 4, 0f),
        p(l + W + 4, -l + W + 4, 0f),
        p(l + W + 8, -l + W + 8, 0f),
        p(W + 8, W + 8, 0f),
        fill = b.tint(TowerConst.STREET),
    )
    b.loose(base)

    val tops = spec.towers.mapIndexed { i, t ->
        b.seed = i + 2
        val incomplete = t.floors < t.target
        val start = b.groups.size
        val top = b.drawTower(
            facade = t.facade,
            palette = t.palette,
            floorsIn = t.floors,
            targetIn = t.target,
            progress = if (incomplete) 0.6f else 0f,
            crownMode = CrownMode.Auto,
            crownStyle = t.crownStyle,
            crane = incomplete,
            lights = t.lights,
            ox = i * s,
            oy = -i * s,
        )
        if (spec.liftSelected && i == spec.selected) {
            for (g in start until b.groups.size) b.groups[g] = b.groups[g].copy(liftPx = LIFT_SELECTED)
        }
        top
    }
    val trees = mutableListOf<Prim>()
    spec.towers.indices.forEach { i ->
        if (i < n - 1) b.tree(trees, i * s + W + 8, -i * s + W - 8, 0f, 3.4f)
        b.tree(trees, i * s + W / 2 + 14, -i * s + W / 2 + 16, 0f, 2.6f)
    }
    b.loose(trees)
    val sel = spec.selected
    if (sel in spec.towers.indices) {
        val pin = mutableListOf<Prim>()
        val lift = if (spec.liftSelected) LIFT_SELECTED else 0f
        val extra = if (spec.towers[sel].facade == Facade.Deco) 40f else 32f
        val tp = b.pt(sel * s + W / 2, -sel * s + W / 2, tops[sel] + extra)
        val top = tp.first to tp.second - lift
        b.line(pin, top, top.first to top.second + 7, AMBER, 1.4f)
        b.circ(pin, top, 3.2f, AMBER)
        b.loose(pin)
    }
    return b.build()
}

private const val LIFT_SELECTED = 4f
