package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.domain.model.LessonMode
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import com.example.poststudy.presentation.ui.screens.groups.StudentSteps

@Composable
fun StudentIntroScreen(
    title: String,
    totalSlides: Int,
    totalQuestions: Int,
    slideTimerSeconds: Int,
    testTimerSeconds: Int,
    mode: LessonMode,
    isExam: Boolean,
    groupName: String,
    onStart: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val parallax = rememberParallax()

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .parallaxSource(parallax)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if ((it.key == Key.Enter || it.key == Key.DirectionRight) && it.type == KeyEventType.KeyDown) {
                    onStart()
                    true
                } else false
            }
    ) {
        SignalBackground(parallax = parallax.value)

        Row(Modifier.fillMaxWidth().padding(horizontal = Space.page, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack, onDark = true, modifier = Modifier.padding(start = 0.dp))
            Spacer(Modifier.weight(1f))
            StepHeader(StudentSteps, 2, onDark = true)
            Spacer(Modifier.width(16.dp))
            SignalIconButton(Icons.Outlined.Refresh, "Yangilash", onRefresh, tint = Signal.OnDark, container = Signal.GlassFill, borderColor = Signal.GlassBorderStrong, size = 40.dp, iconSize = 18.dp, onDark = true)
        }

        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = Space.page, vertical = 72.dp)) {
            GlassCard(Modifier.widthIn(max = 640.dp).fillMaxWidth(), padding = 32.dp) {
                val kindLabel = if (isExam) "Imtihon" else "Dars · ${mode.label}"
                StatusChip(
                    text = listOf(groupName, kindLabel).filter { it.isNotBlank() }.joinToString(" · "),
                    container = Signal.Amber.copy(alpha = 0.18f),
                    contentColor = Signal.Amber
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    title.ifBlank { if (isExam) "Imtihon brifingi" else "Mashg'ulot brifingi" },
                    style = MaterialTheme.typography.displaySmall,
                    color = Signal.OnDark
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when {
                        isExam -> "Imtihonga tayyorlaning. Bu sizning bilimingizni bevosita tekshirishdir."
                        mode == LessonMode.ReAppropriation -> "Mashg'ulotga tayyorlaning. Avval taqdimotni o'rganasiz, so'ngra test topshirasiz."
                        mode == LessonMode.PresentationOnly -> "Taqdimotni diqqat bilan o'rganing. Bu mashg'ulotda test yo'q."
                        else -> "Test topshirishga tayyorlaning. Test boshlangach taymer ishlaydi, vaqt tugaganda javoblar avtomatik yuboriladi."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Signal.OnDarkMuted
                )
                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (mode.hasSlides) {
                        KpiTile("Slaydlar", "$totalSlides ta", Modifier.weight(1f), note = "${slideTimerSeconds / 60} daqiqa", onDark = true)
                    }
                    if (mode.hasTest) {
                        KpiTile("Savollar", "$totalQuestions ta", Modifier.weight(1f), note = "${testTimerSeconds / 60} daqiqa", onDark = true, valueColor = Signal.Cyan)
                    }
                }

                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Klaviatura", style = MaterialTheme.typography.labelLarge, color = Signal.OnDark, fontWeight = FontWeight.Bold)
                        Text(
                            if (mode.hasTest) "1 · 2 · 3 · 4 — javob · ← → — savollar yoki slaydlar · Enter — keyingisi · Esc — chiqish"
                            else "← → — slaydlar · Enter — yakunlash · Esc — chiqish",
                            style = MaterialTheme.typography.bodySmall,
                            color = Signal.OnDarkMuted
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SignalButton(
                    text = if (isExam) "Imtihonni boshlash" else if (mode.hasSlides) "Boshlash" else "Testni boshlash",
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Gradient,
                    height = 52.dp
                )
            }
        }
    }
}
