package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Animated "signal" backdrop for dark screens: three drifting waves whose amplitude breathes,
 * a small network of nodes with data flowing along the links, and a radar with a sweeping beam,
 * expanding rings and blips that light up when the beam passes. Drawn on a Canvas, no image
 * files. When the OS asks for reduced motion a single still frame is drawn.
 */
@Composable
fun SignalBackground(
    modifier: Modifier = Modifier,
    parallax: Offset = Offset.Zero,
    strong: Boolean = true,
    showRadar: Boolean = true,
    showNetwork: Boolean = true,
    alpha: Float = 1f
) {
    val reduceMotion = LocalReduceMotion.current
    var time by remember { mutableStateOf(1.7f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> time = (now - start) / 1_000_000_000f + 1.7f }
        }
    }

    Canvas(modifier.fillMaxSize().background(Signal.GradientHero)) {
        val speed = if (strong) 2f else 1f
        translate(parallax.x, parallax.y) {
            drawWaves(time, speed, alpha)
            if (showNetwork) drawNetwork(time, speed, alpha)
            if (showRadar) drawRadar(time, if (strong) 8f else 12f, alpha)
        }
    }
}

private fun DrawScope.drawWaves(t: Float, speed: Float, alpha: Float) {
    val w = size.width
    val h = size.height
    val waves = listOf(
        WaveSpec(baseY = 0.58f, amp = 0.09f, k = 2.1f, drift = 0.55f, breathe = 1.05f, color = Signal.Cyan, width = 2.2.dp.toPx(), a = 0.7f),
        WaveSpec(baseY = 0.66f, amp = 0.08f, k = 1.7f, drift = -0.38f, breathe = 0.8f, color = Signal.Cyan, width = 1.6.dp.toPx(), a = 0.45f),
        WaveSpec(baseY = 0.74f, amp = 0.07f, k = 2.6f, drift = 0.28f, breathe = 0.6f, color = Signal.Amber, width = 1.6.dp.toPx(), a = 0.35f)
    )
    waves.forEach { spec ->
        val breathing = 1f + 0.35f * sin(t * spec.breathe * speed)
        val path = Path()
        var x = -8f
        var first = true
        while (x <= w + 8f) {
            val phase = x / w * spec.k * 2f * PI.toFloat() + t * spec.drift * speed
            val y = h * spec.baseY + h * spec.amp * breathing * sin(phase)
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            x += 10f
        }
        drawPath(path, spec.color.copy(alpha = spec.a * alpha), style = Stroke(width = spec.width, cap = StrokeCap.Round))
    }
}

private class WaveSpec(
    val baseY: Float, val amp: Float, val k: Float, val drift: Float, val breathe: Float,
    val color: Color, val width: Float, val a: Float
)

private val nodes = listOf(
    Offset(0.086f, 0.17f), Offset(0.236f, 0.29f), Offset(0.371f, 0.135f),
    Offset(0.5f, 0.29f), Offset(0.293f, 0.5f), Offset(0.443f, 0.44f)
)
private val links = listOf(0 to 1, 1 to 2, 2 to 3, 1 to 4, 2 to 5)
private val routes = listOf(listOf(0, 1, 2, 3), listOf(1, 4), listOf(3, 2, 5))

private fun DrawScope.drawNetwork(t: Float, speed: Float, alpha: Float) {
    val w = size.width
    val h = size.height
    fun p(i: Int) = Offset(nodes[i].x * w, nodes[i].y * h)
    val cyan = Signal.Cyan
    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 12f), phase = -t * 26f * speed)
    links.forEach { (a, b) ->
        drawLine(cyan.copy(alpha = 0.4f * alpha), p(a), p(b), strokeWidth = 1.5.dp.toPx(), pathEffect = dash)
    }
    nodes.forEachIndexed { i, _ ->
        val pulse = 0.5f + 0.5f * (0.5f + 0.5f * sin(t * (if (speed > 1f) 4.5f else 2.1f) + i * 0.7f))
        val r = if (i == 1 || i == 3) 5.dp.toPx() else 4.dp.toPx()
        drawCircle(cyan.copy(alpha = pulse * alpha), r, p(i))
    }
    // Packets travelling along the routes
    routes.forEachIndexed { ri, route ->
        val period = (2.4f + ri * 0.4f) / speed * 1.2f
        val frac = ((t + ri * 0.9f) / period) % 1f
        val segments = route.size - 1
        val segF = frac * segments
        val seg = min(segF.toInt(), segments - 1)
        val local = segF - seg
        val from = p(route[seg]); val to = p(route[seg + 1])
        val pos = Offset(from.x + (to.x - from.x) * local, from.y + (to.y - from.y) * local)
        drawCircle(cyan.copy(alpha = 0.95f * alpha), 3.dp.toPx(), pos)
    }
    // Ping rings on two hub nodes
    listOf(1 to 0f, 3 to 1.2f).forEach { (i, delay) ->
        val period = 2.4f / speed
        val frac = ((t + delay) / period) % 1f
        val radius = 26.dp.toPx() * (0.3f + 0.7f * frac)
        drawCircle(cyan.copy(alpha = 0.8f * (1f - frac) * 0.35f * alpha), radius, p(i), style = Stroke(1.2.dp.toPx()))
    }
}

private val blipOffsets = listOf(Offset(110f, -50f), Offset(-70f, 60f), Offset(70f, 100f), Offset(-40f, -80f), Offset(40f, -110f))

private fun DrawScope.drawRadar(t: Float, sweepSeconds: Float, alpha: Float) {
    val w = size.width
    val h = size.height
    val radius = min(180.dp.toPx(), min(w * 0.22f, h * 0.42f))
    val center = Offset(w - w * 0.06f - radius, h * 0.08f + radius)
    val cyan = Signal.Cyan
    val thin = Stroke(1.dp.toPx())

    // Static rings and crosshair
    listOf(radius / 3f, radius * 2f / 3f, radius).forEach { drawCircle(cyan.copy(alpha = 0.5f * alpha), it, center, style = thin) }
    drawLine(cyan.copy(alpha = 0.5f * alpha), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1.dp.toPx())
    drawLine(cyan.copy(alpha = 0.5f * alpha), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1.dp.toPx())
    val d = radius * 0.7071f
    drawLine(cyan.copy(alpha = 0.25f * alpha), Offset(center.x - d, center.y - d), Offset(center.x + d, center.y + d), 1.dp.toPx())
    drawLine(cyan.copy(alpha = 0.25f * alpha), Offset(center.x + d, center.y - d), Offset(center.x - d, center.y + d), 1.dp.toPx())

    // Three ripples expanding from the center
    val ripplePeriod = sweepSeconds * 0.4f
    for (i in 0 until 3) {
        val frac = ((t + i * ripplePeriod / 3f) / ripplePeriod) % 1f
        val r = radius * (0.15f + 0.85f * frac)
        drawCircle(cyan.copy(alpha = 0.55f * (1f - frac) * alpha), r, center, style = Stroke(1.5.dp.toPx()))
    }

    // Sweeping beam: 12 wedges fading behind the leading edge
    val angle = (t / sweepSeconds * 360f) % 360f
    val arcTopLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    rotate(angle, center) {
        for (i in 0 until 12) {
            val a = 0.5f * (1f - i / 12f)
            drawArc(
                color = cyan.copy(alpha = a * alpha),
                startAngle = -90f - (i + 1) * 6f,
                sweepAngle = 6.2f,
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )
        }
        drawLine(cyan.copy(alpha = 0.95f * alpha), center, Offset(center.x, center.y - radius), 2.dp.toPx())
    }

    // Blips light up when the beam passes them, then fade
    val scale = radius / 180f
    blipOffsets.forEach { off ->
        val pos = Offset(center.x + off.x * scale, center.y + off.y * scale)
        var blipAngle = atan2(off.x, -off.y) * 180f / PI.toFloat()
        if (blipAngle < 0f) blipAngle += 360f
        var since = (angle - blipAngle) / 360f
        if (since < 0f) since += 1f
        val a = when {
            since < 0.02f -> since / 0.02f
            since < 0.25f -> 1f - (since - 0.02f) / 0.23f * 0.45f
            since < 0.55f -> 0.55f * (1f - (since - 0.25f) / 0.3f)
            else -> 0f
        }
        val r = (3f + 4f * (if (since < 0.02f) since / 0.02f else 1f - (since / 0.55f).coerceAtMost(1f))) .dp.toPx()
        if (a > 0f) drawCircle(Signal.Amber.copy(alpha = a * alpha), r, pos)
    }
    drawCircle(cyan.copy(alpha = alpha), 4.dp.toPx(), center)
}

/** Pointer-driven parallax offset for [SignalBackground]; attach [parallaxSource] to the hero container. */
@Composable
fun rememberParallax(): MutableState<Offset> = remember { mutableStateOf(Offset.Zero) }

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.parallaxSource(state: MutableState<Offset>, strength: Float = 28f): Modifier {
    var bounds = Size.Zero
    return this
        .onSizeChanged { bounds = Size(it.width.toFloat(), it.height.toFloat()) }
        .onPointerEvent(PointerEventType.Move) { event ->
            val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
            if (bounds.width <= 0f || bounds.height <= 0f) return@onPointerEvent
            val x = (pos.x / bounds.width - 0.5f) * -strength
            val y = (pos.y / bounds.height - 0.5f) * -strength * 0.7f
            state.value = Offset(x, y)
        }
}
