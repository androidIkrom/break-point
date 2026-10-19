package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.delay
import java.awt.image.BufferedImage

/** Theater mode: dark room, the slide in the middle, controls on a glass bar below. */
@Composable
fun SlideShowScreen(
    slides: List<BufferedImage>,
    slideTimerSeconds: Int,
    // True when a test follows the slides; changes the wording of the finish dialog
    hasTestAfter: Boolean,
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    var currentSlideIndex by remember { mutableStateOf(0) }
    var timeLeftSeconds by remember { mutableStateOf(slideTimerSeconds) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val reduceMotion = LocalReduceMotion.current

    val startedAt = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        if (slideTimerSeconds > 0) {
            while (true) {
                val left = slideTimerSeconds - ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                timeLeftSeconds = left.coerceAtLeast(0)
                if (left <= 0) break
                delay(250)
            }
            onFinished()
        }
    }

    val bitmaps = remember(slides) { slides.map { it.toComposeImageBitmap() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Signal.Dark)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key) {
                        Key.DirectionRight -> {
                            if (currentSlideIndex < slides.size - 1) currentSlideIndex++ else showExitDialog = true
                            true
                        }
                        Key.DirectionLeft -> {
                            if (currentSlideIndex > 0) currentSlideIndex--
                            true
                        }
                        Key.Enter -> {
                            if (currentSlideIndex == slides.size - 1) showExitDialog = true
                            true
                        }
                        Key.Escape -> { showBackDialog = true; true }
                        else -> false
                    }
                } else false
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = Space.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BackButton(onClick = { showBackDialog = true }, onDark = true, modifier = Modifier.padding(start = 0.dp))
                Column(Modifier.weight(1f)) {
                    Text("O'rganish", style = MaterialTheme.typography.titleMedium, color = Signal.OnDark)
                    Text("${currentSlideIndex + 1} / ${slides.size}-slayd", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkMuted)
                }
                HelpIcon(
                    title = "Taqdimotni ko'rish",
                    helpText = "Slaydlarni ko'rib chiqing va o'rganing. Navigatsiya uchun tugmalardan yoki klaviaturadagi O'ng/Chap strelkalardan foydalanishingiz mumkin.",
                    onDark = true
                )
                if (slideTimerSeconds > 0) TimerPill(timeLeftSeconds, onDark = true)
            }

            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = Space.xl), contentAlignment = Alignment.Center) {
                if (bitmaps.isNotEmpty()) {
                    AnimatedContent(
                        targetState = currentSlideIndex,
                        transitionSpec = {
                            if (reduceMotion) fadeIn(snap<Float>()) togetherWith fadeOut(snap<Float>())
                            else fadeIn(tween(AppMotion.normal)) togetherWith fadeOut(tween(AppMotion.fast))
                        },
                        label = "slide"
                    ) { index ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = bitmaps[index],
                                contentDescription = "Slide ${index + 1}",
                                modifier = Modifier.fillMaxSize().clip(AppDesign.ComponentShape)
                            )
                        }
                    }
                }
            }

            // Glass control bar
            Box(Modifier.fillMaxWidth().padding(horizontal = Space.xl, vertical = 16.dp), contentAlignment = Alignment.Center) {
                Surface(
                    shape = AppDesign.CardShape,
                    color = Signal.GlassFill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Signal.GlassBorder),
                    modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth()
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SignalButton("Oldingi", onClick = { if (currentSlideIndex > 0) currentSlideIndex-- }, enabled = currentSlideIndex > 0, style = ButtonStyle.Glass, icon = Icons.AutoMirrored.Filled.ArrowBack, height = 42.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Slayd ${currentSlideIndex + 1} / ${slides.size}", style = MaterialTheme.typography.labelMedium, color = Signal.OnDarkMuted)
                                Text("← → — slaydlar · Enter — yakunlash", style = MaterialTheme.typography.labelMedium, color = Signal.OnDarkFaint)
                            }
                            val progress = if (slides.isEmpty()) 0f else (currentSlideIndex + 1f) / slides.size
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(AppDesign.PillShape).background(Color.White.copy(alpha = 0.12f))) {
                                val w by androidx.compose.animation.core.animateFloatAsState(progress, AppMotion.tweenOrSnap(AppMotion.emphasized), label = "slideProgress")
                                Box(Modifier.fillMaxWidth(w).fillMaxHeight().clip(AppDesign.PillShape).background(Signal.Cyan))
                            }
                        }
                        SignalButton(
                            if (currentSlideIndex < slides.size - 1) "Keyingisi" else "O'rganishni yakunlash",
                            onClick = { if (currentSlideIndex < slides.size - 1) currentSlideIndex++ else showExitDialog = true },
                            style = if (currentSlideIndex < slides.size - 1) ButtonStyle.Cyan else ButtonStyle.Gradient,
                            trailingIcon = if (currentSlideIndex < slides.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            height = 42.dp
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        PostStudyDialog(
            onDismissRequest = { showExitDialog = false },
            title = if (hasTestAfter) "O'rganish yakunlansinmi?" else "Taqdimotni tugatish?",
            text = if (hasTestAfter)
                "O'quv mashg'ulotini muddatidan oldin tugatib, testni boshlashga ishonchingiz komilmi?"
            else
                "Taqdimotni ko'rib bo'ldingizmi? Orqaga qaytishni xohlaysizmi?",
            confirmText = "Ha, yakunlash",
            onConfirm = {
                showExitDialog = false
                onFinished()
            }
        )
    }

    if (showBackDialog) {
        PostStudyDialog(
            onDismissRequest = { showBackDialog = false },
            title = "Sessiyadan chiqish?",
            text = "Chiqishga ishonchingiz komilmi? Sizning natijalaringiz saqlanmaydi va siz tanlov ekraniga qaytasiz.",
            confirmText = "Ha, chiqish",
            confirmColor = Signal.Danger,
            onConfirm = {
                showBackDialog = false
                onBack()
            }
        )
    }
}
