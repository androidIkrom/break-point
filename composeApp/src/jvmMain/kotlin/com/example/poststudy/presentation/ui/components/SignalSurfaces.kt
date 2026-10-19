package com.example.poststudy.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Radius
import com.example.poststudy.presentation.theme.Signal

/** Plain workspace card: white, 1dp outline, 18dp corners. */
@Composable
fun SignalCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppDesign.CardShape,
    color: Color = Signal.Card,
    borderColor: Color = Signal.Outline,
    padding: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = color, border = BorderStroke(1.dp, borderColor)) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        val interaction = remember { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()
        val pressed by interaction.collectIsPressedAsState()
        val focused by interaction.collectIsFocusedAsState()
        val lift by animateDpAsState(if (hovered) (-6).dp else 0.dp, AppMotion.tweenOrSnap(AppMotion.normal), label = "lift")
        val scale by animateFloatAsState(if (pressed) 0.985f else 1f, AppMotion.tweenOrSnap(AppMotion.fast), label = "press")
        val border by animateColorAsState(if (hovered) Signal.Primary else borderColor, AppMotion.tweenOrSnap(AppMotion.normal), label = "border")
        Surface(
            onClick = onClick,
            modifier = modifier
                .graphicsLayer { translationY = lift.toPx(); scaleX = scale; scaleY = scale }
                .then(if (hovered) Modifier.shadow(16.dp, shape, ambientColor = Signal.Ink.copy(alpha = 0.5f), spotColor = Signal.Ink.copy(alpha = 0.5f)) else Modifier)
                .then(if (focused) Modifier.border(2.dp, Signal.Primary, shape) else Modifier),
            shape = shape,
            color = color,
            border = BorderStroke(1.dp, border),
            interactionSource = interaction
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

/**
 * Frosted card for dark hero surfaces. On hover it lifts, scales to 1.02 and its border
 * turns cyan with a glow; children tagged with [Modifier.glassIcon] tilt.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 24.dp,
    shape: Shape = AppDesign.CardShape,
    interactive: Boolean = onClick != null,
    content: @Composable ColumnScope.(hovered: Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val active = interactive && hovered
    val lift by animateDpAsState(if (active) (-6).dp else 0.dp, AppMotion.tweenOrSnap(AppMotion.normal), label = "lift")
    val scale by animateFloatAsState(if (pressed) 0.985f else if (active) 1.02f else 1f, AppMotion.tweenOrSnap(AppMotion.normal), label = "scale")
    val fill by animateColorAsState(if (active) Signal.GlassFillHover else Signal.GlassFill, AppMotion.tweenOrSnap(AppMotion.normal), label = "fill")
    val border by animateColorAsState(if (active) Signal.Cyan else Signal.GlassBorder, AppMotion.tweenOrSnap(AppMotion.normal), label = "border")
    val glow by animateFloatAsState(if (active) 1f else 0f, AppMotion.tweenOrSnap(AppMotion.normal), label = "glow")

    val base = modifier
        .graphicsLayer { translationY = lift.toPx(); scaleX = scale; scaleY = scale }
        .outerGlow(Signal.Cyan, glow, Radius.l)
        .then(if (focused) Modifier.border(2.dp, Signal.Cyan, shape) else Modifier)

    CompositionLocalProvider(LocalContentColor provides Signal.OnDark) {
        if (onClick != null) {
            Surface(onClick = onClick, modifier = base, shape = shape, color = fill, border = BorderStroke(1.dp, border), interactionSource = interaction) {
                Column(Modifier.padding(padding)) { content(active) }
            }
        } else {
            Surface(modifier = base, shape = shape, color = fill, border = BorderStroke(1.dp, border)) {
                Column(Modifier.padding(padding)) { content(false) }
            }
        }
    }
}

/**
 * Soft colored glow around a rounded rectangle, drawn as fading strokes outside the bounds.
 * Unlike [shadow] it stays clean behind translucent surfaces. [intensity] 0..1.
 */
fun Modifier.outerGlow(color: Color, intensity: Float, radius: Dp, spread: Dp = 18.dp): Modifier =
    if (intensity <= 0f) this else drawBehind {
        val steps = 6
        val r = radius.toPx()
        val max = spread.toPx()
        for (i in 1..steps) {
            val inset = max * i / steps
            val alpha = 0.28f * intensity * (1f - i.toFloat() / (steps + 1))
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = androidx.compose.ui.geometry.Offset(-inset, -inset + max * 0.3f),
                size = androidx.compose.ui.geometry.Size(size.width + inset * 2, size.height + inset * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r + inset),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = max / steps * 1.2f)
            )
        }
    }

/** Icon tile inside a [GlassCard]; rotates and grows while the card is hovered. */
@Composable
fun Modifier.glassIcon(hovered: Boolean): Modifier {
    val rot by animateFloatAsState(if (hovered) -6f else 0f, AppMotion.tweenOrSnap(AppMotion.emphasized, AppMotion.decelerateEasing), label = "rot")
    val sc by animateFloatAsState(if (hovered) 1.12f else 1f, AppMotion.tweenOrSnap(AppMotion.emphasized, AppMotion.decelerateEasing), label = "sc")
    return graphicsLayer { rotationZ = rot; scaleX = sc; scaleY = sc }
}

/** Small translucent pill on dark surfaces, e.g. "Tarmoqda 3 ta faol admin". */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    color: Color = Signal.Cyan,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier.height(32.dp),
        shape = AppDesign.PillShape,
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (live) LiveDot(color)
            if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(text, color = color, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** 8dp dot with an expanding halo, for "live" states. */
@Composable
fun LiveDot(color: Color = Signal.Cyan, size: Dp = 8.dp) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "live")
    val halo by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(tween(1600)), label = "halo"
    )
    val h = if (reduce) 0f else halo
    Box(
        Modifier
            .size(size)
            .drawBehind {
                drawCircle(color.copy(alpha = 0.6f * (1f - h)), radius = this.size.minDimension / 2f + this.size.minDimension * h)
            }
            .background(color, CircleShape)
    )
}

/** Status pill: tinted container, icon plus label, never color alone. */
@Composable
fun StatusChip(
    text: String,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = contentColor
) {
    Surface(modifier = modifier.height(26.dp), shape = AppDesign.PillShape, color = container) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            Text(text, color = contentColor, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Small uppercase label above a heading, e.g. "BILIM TESTI". */
@Composable
fun Eyebrow(text: String, color: Color = Signal.Primary, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = color, style = MaterialTheme.typography.labelSmall, modifier = modifier)
}

/** Big number tile for dashboards. [onDark] switches to the glass look. */
@Composable
fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    valueColor: Color? = null,
    onDark: Boolean = false,
    container: Color? = null,
    contentColor: Color? = null
) {
    val fill = container ?: if (onDark) Color.White.copy(alpha = 0.08f) else Signal.Card
    val fg = contentColor ?: if (onDark) Signal.OnDark else Signal.Ink
    val muted = if (onDark) Signal.OnDarkMuted else Signal.Muted
    Surface(
        modifier = modifier,
        shape = AppDesign.CardShape,
        color = fill,
        border = BorderStroke(1.dp, if (onDark) Color.White.copy(alpha = 0.14f) else Signal.Outline)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = contentColor?.copy(alpha = 0.85f) ?: muted, style = MaterialTheme.typography.bodySmall)
            Text(value, color = valueColor ?: fg, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            if (note != null) Text(note, color = contentColor?.copy(alpha = 0.85f) ?: muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Four numbered steps for the student flow. */
@Composable
fun StepHeader(steps: List<String>, current: Int, onDark: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { i, label ->
            val done = i < current
            val active = i == current
            val bg by animateColorAsState(
                when {
                    done -> Signal.Primary
                    active -> if (onDark) Signal.Cyan else Signal.Container
                    else -> if (onDark) Color.White.copy(alpha = 0.08f) else Signal.Card
                }, AppMotion.tweenOrSnap(AppMotion.normal), label = "stepBg"
            )
            val fg = when {
                done -> Color.White
                active -> if (onDark) Signal.Dark else Signal.OnContainer
                else -> if (onDark) Signal.OnDarkFaint else Signal.Muted
            }
            val border = when {
                done -> Signal.Primary
                active -> if (onDark) Signal.Cyan else Signal.Primary
                else -> if (onDark) Color.White.copy(alpha = 0.2f) else Signal.OutlineStrong
            }
            Box(
                Modifier.size(28.dp).background(bg, CircleShape).border(1.dp, border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${i + 1}", color = fg, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                label,
                color = if (active) (if (onDark) Signal.OnDark else Signal.Ink) else (if (onDark) Signal.OnDarkFaint else Signal.Muted),
                style = MaterialTheme.typography.labelMedium
            )
            if (i < steps.lastIndex) {
                Spacer(Modifier.width(24.dp).height(2.dp).background(if (done) Signal.Primary else (if (onDark) Color.White.copy(alpha = 0.2f) else Signal.Outline)))
            }
        }
    }
}

/** Section title with optional subtitle and trailing content. */
@Composable
fun SectionTitle(title: String, subtitle: String? = null, onDark: Boolean = false, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = if (onDark) Signal.OnDark else Signal.Ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (onDark) Signal.OnDarkMuted else Signal.Muted)
        }
        if (trailing != null) trailing()
    }
}

/** Avatar circle with initials. */
@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp, gradient: Boolean = false, selected: Boolean = false) {
    val initials = name.trim().split(Regex("\\s+")).take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "?" }
    Box(
        modifier
            .size(size)
            .then(if (gradient) Modifier.shadow(8.dp, CircleShape, ambientColor = Signal.Cyan, spotColor = Signal.Cyan) else Modifier)
            .clip(CircleShape)
            .then(if (gradient) Modifier.background(Signal.GradientPrimary) else Modifier.background(if (selected) Signal.Primary else Signal.SurfaceContainer)),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = if (gradient || selected) Color.White else Signal.OnContainer, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
