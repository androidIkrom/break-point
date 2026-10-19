package com.example.poststudy.presentation.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.example.poststudy.presentation.theme.AppMotion

/**
 * Screen-to-screen transition: the new screen fades in while sliding 1/40 of the width from
 * the right (forward) or from the left (back); the old one fades out the other way. Instant
 * when motion is reduced.
 */
fun screenTransition(back: Boolean, reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) {
        return ContentTransform(
            targetContentEnter = fadeIn(snap<Float>()),
            initialContentExit = fadeOut(snap<Float>()),
            targetContentZIndex = 1f,
            sizeTransform = SizeTransform(clip = false)
        )
    }
    val dir = if (back) -1 else 1
    val enter = fadeIn(tween(AppMotion.screen, easing = AppMotion.decelerateEasing)) +
        slideInHorizontally(tween(AppMotion.screen, easing = AppMotion.decelerateEasing)) { width -> dir * width / 40 }
    val exit = fadeOut(tween(AppMotion.fast, easing = AppMotion.accelerateEasing)) +
        slideOutHorizontally(tween(AppMotion.screen, easing = AppMotion.accelerateEasing)) { width -> -dir * width / 40 }
    return ContentTransform(
        targetContentEnter = enter,
        initialContentExit = exit,
        targetContentZIndex = 1f,
        sizeTransform = SizeTransform(clip = false)
    )
}
