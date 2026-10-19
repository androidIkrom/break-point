package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.Signal
import kotlin.math.sin

/**
 * Generated cover art for lesson and exam cards: one of five signal-themed patterns picked by
 * [index], drawn on a dark gradient. [danger] switches to the red "file missing" look.
 */
@Composable
fun LessonCover(index: Int, modifier: Modifier = Modifier, danger: Boolean = false, amber: Boolean = false, content: @Composable () -> Unit = {}) {
    val brush = when {
        danger -> Brush.linearGradient(listOf(Color(0xFF3F1D1D), Signal.Danger))
        amber -> Brush.linearGradient(listOf(Color(0xFF3A2A05), Color(0xFF7C4A03)))
        else -> listOf(
            Brush.linearGradient(listOf(Signal.Dark2, Signal.Primary)),
            Brush.linearGradient(listOf(Signal.Dark, Color(0xFF155E75))),
            Brush.linearGradient(listOf(Signal.OnContainer, Signal.Primary)),
            Brush.linearGradient(listOf(Signal.Dark, Signal.Dark2)),
            Brush.linearGradient(listOf(Color(0xFF0E2F3A), Color(0xFF0F766E)))
        )[index % 5]
    }
    Box(modifier.background(brush)) {
        Canvas(Modifier.fillMaxSize()) {
            val line = if (danger) Color.White.copy(alpha = 0.5f) else Signal.Cyan.copy(alpha = 0.7f)
            val accent = if (danger) Color.White.copy(alpha = 0.6f) else Signal.Amber.copy(alpha = 0.85f)
            val w = size.width
            val h = size.height
            when (if (danger) 2 else index % 5) {
                0 -> { // waves + ring
                    for (k in 0 until 2) {
                        val p = Path()
                        var x = 0f
                        while (x <= w) {
                            val y = h * (0.62f + 0.12f * k) + h * 0.14f * sin(x / w * 6.28f * 1.5f + k)
                            if (x == 0f) p.moveTo(x, y) else p.lineTo(x, y)
                            x += 8f
                        }
                        drawPath(p, line.copy(alpha = if (k == 0) 0.7f else 0.4f), style = Stroke(if (k == 0) 2f else 1.2f))
                    }
                    drawCircle(accent, h * 0.18f, Offset(w * 0.82f, h * 0.32f), style = Stroke(1.2f))
                    drawCircle(accent, h * 0.07f, Offset(w * 0.82f, h * 0.32f), style = Stroke(1.2f))
                }
                1 -> { // spectrum
                    val pts = listOf(0.0f to 0.72f, 0.11f to 0.55f, 0.22f to 0.8f, 0.33f to 0.28f, 0.44f to 0.62f, 0.55f to 0.45f, 0.66f to 0.78f, 0.77f to 0.36f, 0.88f to 0.6f, 1f to 0.5f)
                    val p = Path()
                    pts.forEachIndexed { i, (fx, fy) -> if (i == 0) p.moveTo(fx * w, fy * h) else p.lineTo(fx * w, fy * h) }
                    drawPath(p, line, style = Stroke(2f))
                    drawCircle(accent, 4f, Offset(0.33f * w, 0.28f * h))
                    drawCircle(accent, 4f, Offset(0.77f * w, 0.36f * h))
                }
                2 -> { // pulse line
                    val p = Path()
                    p.moveTo(0f, h * 0.55f); p.lineTo(w * 0.12f, h * 0.55f); p.lineTo(w * 0.15f, h * 0.35f); p.lineTo(w * 0.18f, h * 0.75f)
                    p.lineTo(w * 0.21f, h * 0.55f); p.lineTo(w * 0.36f, h * 0.55f); p.lineTo(w * 0.39f, h * 0.25f); p.lineTo(w * 0.42f, h * 0.85f)
                    p.lineTo(w * 0.45f, h * 0.55f); p.lineTo(w, h * 0.55f)
                    drawPath(p, line, style = Stroke(2f))
                }
                3 -> { // network nodes
                    val nodes = listOf(Offset(0.12f * w, 0.72f * h), Offset(0.34f * w, 0.36f * h), Offset(0.56f * w, 0.64f * h), Offset(0.84f * w, 0.28f * h))
                    for (i in 0 until nodes.size - 1) drawLine(line, nodes[i], nodes[i + 1], 1.5f)
                    nodes.forEachIndexed { i, n -> drawCircle(if (i == 2) accent else line, 5f, n) }
                }
                else -> { // rings
                    val c = Offset(w * 0.5f, h * 0.5f)
                    listOf(0.16f, 0.32f, 0.48f).forEachIndexed { i, r ->
                        drawCircle(line.copy(alpha = 0.7f - i * 0.2f), h * r, c, style = Stroke(1.5f))
                    }
                    drawCircle(accent, 4f, c)
                    drawLine(line.copy(alpha = 0.5f), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
                }
            }
        }
        content()
    }
}
