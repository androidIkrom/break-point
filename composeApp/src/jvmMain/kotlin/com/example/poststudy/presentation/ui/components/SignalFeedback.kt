package com.example.poststudy.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Countdown pill: neutral, amber under a minute, red and pulsing under ten seconds. */
@Composable
fun TimerPill(secondsLeft: Int, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val urgent = secondsLeft <= 10
    val warn = secondsLeft <= 60
    val bg by animateColorAsState(
        when { urgent -> Signal.DangerContainer; warn -> Signal.WarningContainer; onDark -> Color.White.copy(alpha = 0.08f); else -> Signal.Card },
        AppMotion.tweenOrSnap(300), label = "timerBg"
    )
    val fg by animateColorAsState(
        when { urgent -> Signal.OnDangerContainer; warn -> Signal.OnWarningContainer; onDark -> Signal.OnDark; else -> Signal.Ink },
        AppMotion.tweenOrSnap(300), label = "timerFg"
    )
    val border by animateColorAsState(
        when { urgent -> Signal.DangerBorder; warn -> Signal.WarningBorder; onDark -> Color.White.copy(alpha = 0.16f); else -> Signal.OutlineStrong },
        AppMotion.tweenOrSnap(300), label = "timerBorder"
    )
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(keyframes { durationMillis = 1000; 1f at 0; 1.06f at 300; 1f at 1000 }),
        label = "pulseScale"
    )
    val scale = if (urgent && !reduce) pulse else 1f
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    Surface(
        modifier = modifier.height(40.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = AppDesign.PillShape,
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Timer, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Text("%d:%02d".format(minutes, seconds), color = fg, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Small ring showing "answered / total" style progress; the arc springs to the new value. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 44.dp, stroke: Dp = 5.dp, track: Color = Signal.Container, color: Color = Signal.Primary, content: @Composable BoxScope.() -> Unit = {}) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), AppMotion.tweenOrSnap(320), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2f
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        content()
    }
}

/**
 * Result ring: the arc draws from zero to [percent] over one second with a decelerating curve
 * and a glow, while the number in the middle counts up in step.
 */
@Composable
fun ScoreRing(percent: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 176.dp, stroke: Dp = 12.dp, onDark: Boolean = true, caption: String? = null) {
    val reduce = LocalReduceMotion.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(percent, reduce) {
        anim.snapTo(0f)
        if (reduce) anim.snapTo(percent / 100f)
        else anim.animateTo(percent / 100f, tween(AppMotion.slow, delayMillis = 80, easing = AppMotion.decelerateEasing))
    }
    val shown = (anim.value * 100f).toInt()
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2f
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(if (onDark) Color.White.copy(alpha = 0.12f) else Signal.SurfaceContainer, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            // glow
            drawArc(color.copy(alpha = 0.35f), -90f, 360f * anim.value, false, Offset(inset, inset), arcSize, style = Stroke(sw * 1.9f, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * anim.value, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$shown%", style = MaterialTheme.typography.displayLarge, color = if (onDark) Signal.OnDark else Signal.Ink)
            if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = if (onDark) Signal.OnDarkMuted else Signal.Muted)
        }
    }
}

/** A check badge that pops in (0.5 → 1.18 → 1) when [visible] turns true; used on a picked answer. */
@Composable
fun PopCheck(visible: Boolean, modifier: Modifier = Modifier, color: Color = Signal.Primary, size: Dp = 26.dp) {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (!visible) { scale.snapTo(0f); return@LaunchedEffect }
        if (reduce) { scale.snapTo(1f); return@LaunchedEffect }
        scale.snapTo(0.5f)
        scale.animateTo(1f, keyframes { durationMillis = 280; 0.5f at 0; 1.18f at 170; 1f at 280 })
    }
    if (visible || scale.value > 0f) {
        Box(
            modifier
                .size(size)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = scale.value.coerceIn(0f, 1f) }
                .shadow(6.dp, CircleShape, ambientColor = color, spotColor = color)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** One toast at a time, shown at the bottom center for 2.6 s. */
class ToastState {
    var current by mutableStateOf<ToastData?>(null)
        private set
    private var serial = 0
    fun show(title: String, text: String = "", icon: ImageVector = Icons.Default.Bolt) {
        serial++
        current = ToastData(serial, title, text, icon)
    }
    fun dismiss(id: Int) { if (current?.id == id) current = null }
}

data class ToastData(val id: Int, val title: String, val text: String, val icon: ImageVector)

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val toast = state.current
    LaunchedEffect(toast?.id) {
        val id = toast?.id ?: return@LaunchedEffect
        kotlinx.coroutines.delay(2600)
        state.dismiss(id)
    }
    val reduce = LocalReduceMotion.current
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = toast != null,
            enter = if (reduce) fadeIn(tween(1)) else fadeIn(tween(AppMotion.emphasized)) + slideInVertically(tween(AppMotion.emphasized, easing = AppMotion.decelerateEasing)) { it / 2 },
            exit = if (reduce) fadeOut(tween(1)) else fadeOut(tween(AppMotion.fast)) + slideOutVertically(tween(AppMotion.fast)) { it / 2 }
        ) {
            val data = toast ?: return@AnimatedVisibility
            Surface(
                modifier = Modifier.padding(bottom = 28.dp).shadow(16.dp, AppDesign.CardShape, ambientColor = Signal.Dark, spotColor = Signal.Dark),
                shape = AppDesign.CardShape,
                color = Signal.Dark,
                border = BorderStroke(1.dp, Signal.Cyan.copy(alpha = 0.4f))
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(30.dp).background(Signal.Cyan, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(data.icon, contentDescription = null, tint = Signal.Dark, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(data.title, color = Signal.OnDark, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (data.text.isNotBlank()) Text(data.text, color = Signal.OnDarkMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * Burst of 48 confetti pieces from [origin] (fractions of the box) that fly out, spin and fade
 * over 1.5 s. Re-plays whenever [playKey] changes; draws nothing when motion is reduced.
 */
@Composable
fun ConfettiBurst(playKey: Any?, modifier: Modifier = Modifier, origin: Offset = Offset(0.15f, 0.5f)) {
    val reduce = LocalReduceMotion.current
    var progress by remember { mutableStateOf(1f) }
    val pieces = remember(playKey) {
        val colors = listOf(Signal.Cyan, Signal.Amber, Signal.Primary, Color.White)
        List(48) { i ->
            val angle = i / 48f * 2f * PI.toFloat() + (playKey.hashCode() % 7) * 0.3f
            val dist = 70f + ((i * 37) % 110)
            Piece(colors[i % 4], cos(angle) * dist * 3f, sin(angle) * dist * 1.4f + 60f, (i * 13 % 180) / 1500f, (i * 71 % 360).toFloat())
        }
    }
    LaunchedEffect(playKey, reduce) {
        if (playKey == null || reduce) { progress = 1f; return@LaunchedEffect }
        progress = 0f
        val start = withFrameNanos { it }
        while (progress < 1f) {
            withFrameNanos { now -> progress = ((now - start) / 1_500_000_000f).coerceAtMost(1f) }
        }
    }
    if (progress >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val ox = size.width * origin.x
        val oy = size.height * origin.y
        pieces.forEach { p ->
            val local = ((progress - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val eased = 1f - (1f - local) * (1f - local)
            val x = ox + p.dx * eased
            val y = oy + p.dy * eased + 120f * local * local
            val alpha = 1f - local
            rotate(p.rot + 540f * local, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - 4f, y - 6f), Size(8f, 13f))
            }
        }
    }
}

private class Piece(val color: Color, val dx: Float, val dy: Float, val delay: Float, val rot: Float)
