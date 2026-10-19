package com.example.poststudy.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.Signal

/** How a [SignalButton] looks. One Gradient button per screen at most: it marks the main action. */
enum class ButtonStyle { Primary, Tonal, Outlined, Text, Danger, Gradient, Glass, Cyan }

/**
 * App button: press scales to 0.97, hover brightens, keyboard focus draws a 2dp ring.
 * Works on light and dark surfaces through [style].
 */
@Composable
fun SignalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    shape: Shape = AppDesign.ComponentShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp)
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, AppMotion.tweenOrSnap(AppMotion.fast), label = "press")

    val containerColor = when (style) {
        ButtonStyle.Primary -> if (hovered) Signal.PrimaryHover else Signal.Primary
        ButtonStyle.Tonal -> if (hovered) Signal.Container.copy(alpha = 0.8f) else Signal.Container
        ButtonStyle.Outlined, ButtonStyle.Text -> if (hovered && style == ButtonStyle.Outlined) Signal.Surface else Color.Transparent
        ButtonStyle.Danger -> if (hovered) Color(0xFF9A1E14) else Signal.Danger
        ButtonStyle.Gradient -> Color.Transparent
        ButtonStyle.Glass -> if (hovered) Signal.GlassFillHover else Signal.GlassFill
        ButtonStyle.Cyan -> if (hovered) Signal.CyanSoft else Signal.Cyan
    }
    val contentColor = when (style) {
        ButtonStyle.Primary, ButtonStyle.Danger, ButtonStyle.Gradient -> Color.White
        ButtonStyle.Tonal -> Signal.OnContainer
        ButtonStyle.Outlined -> Signal.Ink
        ButtonStyle.Text -> Signal.Primary
        ButtonStyle.Glass -> Signal.OnDark
        ButtonStyle.Cyan -> Signal.Dark
    }
    val border = when (style) {
        ButtonStyle.Outlined -> BorderStroke(1.dp, Signal.OutlineStrong)
        ButtonStyle.Glass -> BorderStroke(1.dp, Signal.GlassBorderStrong)
        else -> null
    }
    val focusColor = if (style == ButtonStyle.Glass || style == ButtonStyle.Cyan) Signal.Cyan else Signal.Primary
    val shadowColor = when (style) {
        ButtonStyle.Primary -> Signal.Primary
        ButtonStyle.Gradient, ButtonStyle.Cyan -> Signal.Cyan
        else -> Color.Transparent
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focused) Modifier.border(2.dp, focusColor, shape) else Modifier)
            .then(if (shadowColor != Color.Transparent && enabled) Modifier.shadow(if (hovered) 10.dp else 6.dp, shape, ambientColor = shadowColor, spotColor = shadowColor) else Modifier)
            .then(if (style == ButtonStyle.Gradient && enabled) Modifier.background(Signal.GradientPrimary, shape) else Modifier),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Signal.SurfaceContainer,
            disabledContentColor = Signal.Faint
        ),
        border = border,
        elevation = null,
        interactionSource = interaction,
        contentPadding = contentPadding
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/** Square icon-only button with the same press, hover and focus behaviour. */
@Composable
fun SignalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Signal.Muted,
    container: Color = Signal.Card,
    borderColor: Color? = Signal.Outline,
    size: Dp = 36.dp,
    iconSize: Dp = 16.dp,
    onDark: Boolean = false,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, AppMotion.tweenOrSnap(AppMotion.fast), label = "press")
    val bg by animateColorAsState(
        if (hovered) (if (onDark) Signal.GlassFillHover else Signal.SurfaceContainer) else container,
        AppMotion.tweenOrSnap(AppMotion.fast), label = "bg"
    )
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focused) Modifier.border(2.dp, if (onDark) Signal.Cyan else Signal.Primary, shape) else Modifier),
        shape = shape,
        color = bg,
        border = borderColor?.let { BorderStroke(1.dp, it) },
        interactionSource = interaction
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = if (enabled) tint else Signal.Faint, modifier = Modifier.size(iconSize))
        }
    }
}

/** Rounded pill chip used for filters and segmented choices. */
@Composable
fun SignalChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        when { selected -> Signal.Ink; hovered -> Signal.SurfaceContainer; else -> Signal.Card },
        AppMotion.tweenOrSnap(AppMotion.fast), label = "chipBg"
    )
    val fg by animateColorAsState(if (selected) Color.White else Signal.Ink, AppMotion.tweenOrSnap(AppMotion.fast), label = "chipFg")
    Surface(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = AppDesign.PillShape,
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, Signal.OutlineStrong),
        interactionSource = interaction
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, color = fg, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Row of content on a brush background, e.g. gradient icon tiles. */
@Composable
fun GradientTile(
    brush: Brush,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = AppDesign.ComponentShape,
    glow: Color = Signal.Cyan,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier
            .size(size)
            .shadow(8.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(brush),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}
