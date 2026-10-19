package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*

@Composable
fun TeacherIntroScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if ((it.key == Key.Enter || it.key == Key.DirectionRight) && it.type == KeyEventType.KeyDown) {
                    onNext()
                    true
                } else false
            }
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            SignalBackground(alpha = 0.85f)
            Row(Modifier.fillMaxSize().padding(horizontal = Space.page), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BackButton(onClick = onBack, onDark = true, modifier = Modifier.padding(start = 0.dp))
                Column {
                    Eyebrow("Admin", color = Signal.Amber)
                    Text("Admin uchun qo'llanma", style = MaterialTheme.typography.headlineMedium, color = Signal.OnDark)
                    Text("Uch qadam: material, biriktirish, test formati.", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkMuted)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = Space.contentMaxWidth + Space.page * 2).fillMaxWidth().padding(horizontal = Space.page, vertical = Space.xl),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GuideStep(
                    1, "Taqdimot va testlarni tanlash",
                    "Dars yaratish jarayonida 'Taqdimot fayli' uchun (.pptx) va 'Test fayli' uchun (.docx) formatidagi hujjatlarni yuklang. Shuningdek, tinglovchilar har bir slaydni va testni ko'rishlari uchun vaqt limitlarini belgilang."
                )
                GuideStep(
                    2, "Darsni faollashtirish",
                    "Darsni yaratganingizdan so'ng, 'Saqlangan darslar' ro'yxatidan kerakli darsni toping va 'Boshlash' tugmasini bosing. Bu darsni tinglovchilar kirishi uchun asosiy dars sifatida sozlaydi."
                )
                GuideStep(
                    3, "Word test formati (muhim!)",
                    "Test fayli quyidagi formatda bo'lishi shart:\n\n1. Savol matni\na) Birinchi variant\nb) Ikkinchi variant*\nc) Uchinchi variant\nd) To'rtinchi variant\n\nTo'g'ri javobning oxiriga yulduzcha (*) belgisini qo'ying. Har bir savoldan keyin bitta bo'sh qator tashlang."
                )
                Surface(shape = AppDesign.CardShape, color = Signal.Dark, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Misol:", style = MaterialTheme.typography.titleMedium, color = Signal.Cyan)
                        Text(
                            "O'zbekiston poytaxti qaysi shahar?\na) Samarqand\nb) Toshkent*\nc) Buxoro\nd) Xiva",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Signal.OnDarkMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SignalButton("Tushunarli", onClick = onNext, style = ButtonStyle.Gradient, height = 48.dp, contentPadding = PaddingValues(horizontal = 32.dp))
                    Text("Enter — davom etish", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
                }
            }
        }
    }
}

/** Numbered guide card shared by the admin intro and the help page. */
@Composable
fun GuideStep(number: Int, title: String, content: String) {
    SignalCard(padding = 20.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GradientTile(Signal.GradientPrimary, size = 40.dp) {
                Text("$number", style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Signal.Ink)
                Text(content, style = MaterialTheme.typography.bodyMedium, color = Signal.Muted)
            }
        }
    }
}
