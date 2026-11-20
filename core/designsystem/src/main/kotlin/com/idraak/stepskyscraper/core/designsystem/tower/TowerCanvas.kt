package com.idraak.stepskyscraper.core.designsystem.tower

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme

/**
 * Compose port of tower.js `SSTower`. Draws the isometric tower with its exact geometry, sky tint, lit
 * windows, construction state, crane and crown, and plays the drop / lift / flicker animations.
 * With "Remove animations" on, the final state fades in over 150ms instead.
 */
@Composable
fun TowerCanvas(
    spec: TowerSpec,
    contentDescription: String,
    modifier: Modifier = Modifier,
    animation: TowerAnimation = TowerAnimation.None,
    fit: TowerFit = TowerFit(),
    mirror: Boolean = false,
) {
    val prepared = remember(spec, animation) { PreparedScene(buildTowerScene(spec, animation)) }
    SceneCanvas(prepared, animation, fit, mirror, contentDescription, modifier)
}

/** Compose port of tower.js `SSSkyline`. */
@Composable
fun SkylineCanvas(
    spec: SkylineSpec,
    contentDescription: String,
    modifier: Modifier = Modifier,
    animation: TowerAnimation = TowerAnimation.None,
    fit: TowerFit = TowerFit(),
) {
    val prepared = remember(spec, animation) { PreparedScene(buildSkylineScene(spec, animation)) }
    SceneCanvas(prepared, animation, fit, false, contentDescription, modifier)
}

@Composable
private fun SceneCanvas(
    prepared: PreparedScene,
    animation: TowerAnimation,
    fit: TowerFit,
    mirror: Boolean,
    contentDescription: String,
    modifier: Modifier,
) {
    val reduceMotion = SsTheme.reduceMotion
    val animate = !animation.isEmpty && !reduceMotion && prepared.scene.animationEndMs > 0
    val clock = remember(prepared) { Animatable(if (animate) 0f else Float.MAX_VALUE) }
    val fade = remember(prepared) { Animatable(if (reduceMotion && !animation.isEmpty) 0f else 1f) }
    LaunchedEffect(prepared, animate) {
        if (animate) {
            val end = prepared.scene.animationEndMs.toFloat()
            clock.snapTo(0f)
            clock.animateTo(end, tween(prepared.scene.animationEndMs, easing = LinearEasing))
        }
    }
    LaunchedEffect(prepared, reduceMotion) {
        if (fade.value < 1f) fade.animateTo(1f, tween(SsMotion.CROSSFADE_MS))
    }
    val twinkle = if (prepared.hasTwinkle && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "twinkle")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = TWINKLE_LOOP_MS,
            animationSpec = infiniteRepeatable(tween(TWINKLE_LOOP_MS.toInt(), easing = LinearEasing), RepeatMode.Restart),
            label = "twinkle-ms",
        )
        value
    } else {
        0f
    }
    Canvas(
        modifier
            .graphicsLayer { alpha = fade.value }
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Image
            },
    ) {
        val time = if (clock.value == Float.MAX_VALUE) null else clock.value
        drawScene(prepared, fit, time, twinkle, mirror)
    }
}

/** Position (in the composable's coordinate space) of the active floor, used for the 1.4× zoom. */
fun towerActiveFloorAnchor(spec: TowerSpec, fit: TowerFit, size: Size): Offset {
    val scene = buildTowerScene(spec, TowerAnimation.None)
    val t = viewTransform(scene.bounds, fit, size)
    return t.toCanvas(scene.activeAnchor.first, scene.activeAnchor.second)
}

/** Renders a tower to a bitmap (Glance widgets and share cards); always the final, static state. */
fun renderTowerBitmap(spec: TowerSpec, widthPx: Int, heightPx: Int, density: Float, fit: TowerFit = TowerFit()): ImageBitmap =
    renderScene(PreparedScene(buildTowerScene(spec, TowerAnimation.None)), widthPx, heightPx, density, fit)

fun renderSkylineBitmap(spec: SkylineSpec, widthPx: Int, heightPx: Int, density: Float, fit: TowerFit = TowerFit()): ImageBitmap =
    renderScene(PreparedScene(buildSkylineScene(spec, TowerAnimation.None)), widthPx, heightPx, density, fit)

private fun renderScene(prepared: PreparedScene, widthPx: Int, heightPx: Int, density: Float, fit: TowerFit): ImageBitmap {
    val bitmap = ImageBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1))
    val canvas = ComposeCanvas(bitmap)
    CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, canvas, Size(widthPx.toFloat(), heightPx.toFloat())) {
        drawScene(prepared, fit, null, 0f)
    }
    return bitmap
}

private const val TWINKLE_LOOP_MS = 24_000f
