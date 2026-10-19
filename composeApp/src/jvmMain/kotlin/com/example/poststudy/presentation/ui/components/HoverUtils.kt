package com.example.poststudy.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.zIndex
import com.example.poststudy.presentation.theme.AppMotion

/**
 * Lifts the element a little on hover. Defaults are gentler than before (4dp, 1.02) so cards
 * feel responsive without jumping; heavy lifts belong to [GlassCard] and [SignalCard].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.hoverEffect(
    scale: Float = 1.02f,
    yOffset: Float = -4f,
    durationMillis: Int = AppMotion.normal
): Modifier {
    var isHovered by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) scale else 1.0f,
        animationSpec = AppMotion.tweenOrSnap(durationMillis),
        label = "hoverScale"
    )

    val animatedOffset by animateFloatAsState(
        targetValue = if (isHovered) yOffset else 0f,
        animationSpec = AppMotion.tweenOrSnap(durationMillis),
        label = "hoverLift"
    )

    return this
        .onPointerEvent(PointerEventType.Enter) { isHovered = true }
        .onPointerEvent(PointerEventType.Exit) { isHovered = false }
        .zIndex(if (isHovered) 10f else 0f)
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            translationY = animatedOffset
            clip = false
        }
}
