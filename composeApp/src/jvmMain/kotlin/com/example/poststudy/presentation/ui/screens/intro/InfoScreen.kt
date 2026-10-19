package com.example.poststudy.presentation.ui.screens.intro

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun InfoScreen(onContinue: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    var visible by remember { mutableStateOf(reduceMotion) }
    val focusRequester = remember { FocusRequester() }
    val parallax = rememberParallax()

    LaunchedEffect(Unit) {
        delay(60)
        visible = true
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .parallaxSource(parallax)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    onContinue()
                    true
                } else false
            }
    ) {
        SignalBackground(parallax = parallax.value)

        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 32.dp)) {
            Column(Modifier.widthIn(max = 1100.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Reveal(visible, 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        InstituteLogo(size = 96.dp, glow = true)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Eyebrow("Loyiha haqida", color = Signal.Amber)
                            Text("BreakPoint", style = MaterialTheme.typography.displayLarge, color = Signal.OnDark)
                            Text("Ta'lim sifatini oshirish uchun innovatsion yechim", style = MaterialTheme.typography.bodyLarge, color = Signal.OnDarkMuted)
                        }
                    }
                }

                AdaptiveGrid(
                    items = listOf(
                        InfoItem(Icons.Default.Adjust, "Loyihaning maqsadi", "O'quv jarayonini raqamlashtirish, bilimlarni nazorat qilishni avtomatlashtirish va pedagoglar bilan tinglovchilar o'rtasidagi aloqani soddalashtirish.", 100),
                        InfoItem(Icons.Default.TaskAlt, "Asosiy vazifalari", "Dars materiallarini elektron shaklda taqdim etish, interaktiv testlar orqali baholash va natijalarni real vaqt rejimida tahlil qilish.", 160),
                        InfoItem(Icons.Default.Computer, "Qo'llanish sohalari", "Oliy harbiy ta'lim muassasalari o'qitish tizimlarida foydalanish uchun mo'ljallangan.", 220)
                    ),
                    minItemWidth = 280.dp,
                    spacing = 18.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { item, itemModifier ->
                    Reveal(visible, item.delay, itemModifier) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            GradientTile(Signal.GradientPrimary, size = 48.dp) {
                                Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(item.title, style = MaterialTheme.typography.titleLarge, color = Signal.OnDark)
                            Spacer(Modifier.height(6.dp))
                            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Signal.OnDarkMuted)
                        }
                    }
                }

                Reveal(visible, 300) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SignalButton("Davom etish", onClick = onContinue, style = ButtonStyle.Cyan, height = 52.dp, contentPadding = PaddingValues(horizontal = 32.dp))
                        Text("Enter — davom etish", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkFaint)
                    }
                }
            }
        }
    }
}

private class InfoItem(val icon: ImageVector, val title: String, val description: String, val delay: Int)
