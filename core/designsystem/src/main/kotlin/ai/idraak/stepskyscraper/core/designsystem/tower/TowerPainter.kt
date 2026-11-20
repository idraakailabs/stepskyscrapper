package ai.idraak.stepskyscraper.core.designsystem.tower

import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** How the scene is fitted into the canvas (tower.js `finish` + preserveAspectRatio). */
data class TowerFit(
    val pad: Float = 6f,
    val minHeight: Float = 0f,
    val minWidth: Float = 0f,
    /** "xMidYMax meet" (default) or "xMidYMid meet". */
    val alignBottom: Boolean = true,
)

/** A scene with cached [Path]s so redraws during animation do not allocate. */
internal class PreparedScene(val scene: Scene) {
    val paths: List<List<Path?>> = scene.groups.map { g ->
        g.prims.map { prim ->
            (prim as? Prim.Poly)?.let { poly ->
                Path().apply {
                    moveTo(poly.points[0], poly.points[1])
                    var i = 2
                    while (i < poly.points.size) {
                        lineTo(poly.points[i], poly.points[i + 1])
                        i += 2
                    }
                    close()
                }
            }
        }
    }
    val hasTwinkle: Boolean = scene.groups.any { g -> g.prims.any { it.twinklePhase >= 0f } }
}

internal data class ViewTransform(val scale: Float, val offsetX: Float, val offsetY: Float, val minX: Float, val minY: Float) {
    fun toCanvas(x: Float, y: Float) = Offset(offsetX + (x - minX) * scale, offsetY + (y - minY) * scale)
}

internal fun viewTransform(bounds: Bounds, fit: TowerFit, size: Size): ViewTransform {
    var minX = bounds.minX - fit.pad
    var maxX = bounds.maxX + fit.pad
    var minY = bounds.minY - fit.pad
    val maxY = bounds.maxY + fit.pad
    if (fit.minHeight > 0 && maxY - minY < fit.minHeight) minY = maxY - fit.minHeight
    if (fit.minWidth > 0 && maxX - minX < fit.minWidth) {
        val cx = (minX + maxX) / 2
        minX = cx - fit.minWidth / 2
        maxX = cx + fit.minWidth / 2
    }
    val vbw = maxX - minX
    val vbh = maxY - minY
    val scale = min(size.width / vbw, size.height / vbh)
    val offX = (size.width - vbw * scale) / 2
    val offY = if (fit.alignBottom) size.height - vbh * scale else (size.height - vbh * scale) / 2
    return ViewTransform(scale, offX, offY, minX, minY)
}

/**
 * Paints a prepared scene. [timeMs] is the time since the animation started (null = final state);
 * [twinkleMs] drives the endless premium "Slow twinkle" lights.
 */
internal fun DrawScope.drawScene(
    prepared: PreparedScene,
    fit: TowerFit,
    timeMs: Float?,
    twinkleMs: Float,
    mirror: Boolean = false,
) {
    val t = viewTransform(prepared.scene.bounds, fit, size)
    withTransform({
        if (mirror) scale(-1f, 1f, pivot = center)
        translate(t.offsetX - t.minX * t.scale, t.offsetY - t.minY * t.scale)
        scale(t.scale, t.scale, pivot = Offset.Zero)
    }) {
        prepared.scene.groups.forEachIndexed { gi, group ->
            val (dy, alpha) = groupState(group.anim, timeMs)
            if (alpha <= 0f) return@forEachIndexed
            translate(top = dy - group.liftPx) {
                group.prims.forEachIndexed { pi, prim ->
                    val primAlpha = alpha * group.opacity * primOpacity(prim, timeMs, twinkleMs)
                    drawPrim(prim, prepared.paths[gi][pi], primAlpha)
                }
            }
        }
    }
}

private fun DrawScope.drawPrim(prim: Prim, path: Path?, alpha: Float) {
    if (alpha <= 0f) return
    when (prim) {
        is Prim.Poly -> {
            val p = path ?: return
            prim.fill?.let { drawPath(p, rgb(it), alpha = alpha.coerceIn(0f, 1f), style = Fill) }
            prim.stroke?.let { s ->
                drawPath(
                    p,
                    rgb(s.color),
                    alpha = (alpha * s.opacity).coerceIn(0f, 1f),
                    style = Stroke(width = s.width, pathEffect = s.dash?.let { PathEffect.dashPathEffect(it) }),
                )
            }
        }
        is Prim.Line -> drawLine(
            rgb(prim.color),
            Offset(prim.x1, prim.y1),
            Offset(prim.x2, prim.y2),
            strokeWidth = prim.width,
            cap = StrokeCap.Round,
            alpha = alpha.coerceIn(0f, 1f),
        )
        is Prim.Circle -> drawCircle(rgb(prim.color), prim.r, Offset(prim.cx, prim.cy), alpha = alpha.coerceIn(0f, 1f))
    }
}

private fun rgb(c: Int) = Color(0xFF000000.toInt() or c)

/** ssDrop / ssLift keyframes with `animation-fill-mode: both`. Returns (translateY, opacity). */
internal fun groupState(anim: GroupAnim?, timeMs: Float?): Pair<Float, Float> {
    if (anim == null || timeMs == null) return 0f to 1f
    return when (anim) {
        is GroupAnim.Drop -> keyframes(
            local = timeMs - anim.delayMs,
            duration = SsMotion.FLOOR_DROP_MS.toFloat(),
            split = 0.65f,
            from = -SsMotion.FLOOR_DROP_DISTANCE,
            mid = SsMotion.FLOOR_OVERSHOOT,
            easing = SsMotion.Drop,
            fadeIn = true,
        )
        is GroupAnim.Lift -> keyframes(
            local = timeMs - anim.delayMs,
            duration = SsMotion.CROWN_LIFT_MS.toFloat(),
            split = 0.78f,
            from = -SsMotion.CROWN_LIFT_DISTANCE,
            mid = SsMotion.CROWN_OVERSHOOT,
            easing = SsMotion.Lift,
            fadeIn = false,
        )
        is GroupAnim.LiftOff -> {
            val p = ((timeMs - anim.delayMs) / SsMotion.LIFT_OFF_MS).coerceIn(0f, 1f)
            -SsMotion.FLOOR_DROP_DISTANCE * p to 1f - p
        }
    }
}

@Suppress("LongParameterList")
private fun keyframes(
    local: Float,
    duration: Float,
    split: Float,
    from: Float,
    mid: Float,
    easing: Easing,
    fadeIn: Boolean,
): Pair<Float, Float> {
    if (local <= 0f) return from to if (fadeIn) 0f else 1f
    val p = local / duration
    if (p >= 1f) return 0f to 1f
    return if (p < split) {
        val k = easing.transform(p / split)
        (from + (mid - from) * k) to if (fadeIn) k.coerceIn(0f, 1f) else 1f
    } else {
        val k = easing.transform((p - split) / (1f - split))
        (mid * (1f - k)) to 1f
    }
}

/** ssFlick (900ms, steps(1,end)) and the slow twinkle. */
internal fun primOpacity(prim: Prim, timeMs: Float?, twinkleMs: Float): Float {
    var opacity = 1f
    val delay = prim.flickerDelayMs
    if (delay >= 0 && timeMs != null) {
        val p = (timeMs - delay) / SsMotion.WINDOW_FLICKER_MS
        opacity = when {
            p < 0.35f -> 0.1f
            p < 0.45f -> 1f
            p < 0.60f -> 0.35f
            else -> 1f
        }
    }
    if (prim.twinklePhase >= 0f) {
        val wave = sin((twinkleMs / TWINKLE_PERIOD_MS + prim.twinklePhase) * 2 * PI).toFloat()
        opacity *= 0.7f + 0.3f * wave
    }
    return opacity
}

private const val TWINKLE_PERIOD_MS = 2_400f
