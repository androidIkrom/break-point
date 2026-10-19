package com.example.poststudy.presentation.ui.screens.intro

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onContinue: () -> Unit) {
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
            Column(Modifier.widthIn(max = 1100.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Reveal(visible, 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InstituteLogo(size = 140.dp, glow = true)
                        Spacer(Modifier.height(8.dp))
                        Text("BreakPoint", style = MaterialTheme.typography.displayLarge, color = Signal.OnDark)
                        Text("Bilimlarni mustahkamlash va baholash tizimi", style = MaterialTheme.typography.bodyLarge, color = Signal.OnDarkMuted, textAlign = TextAlign.Center)
                    }
                }

                AdaptiveGrid(
                    items = listOf(
                        SplashItem(Icons.Default.Person, "Admin - pedagoglar uchun", "Dars materiallarini yuklang, testlar yarating va tinglovchilar natijalarini real vaqtda kuzating.", Signal.GradientPrimary, 100),
                        SplashItem(Icons.Default.Face, "Tinglovchilar uchun", "Taqdimotlarni ko'rib chiqing, bilimlaringizni sinab ko'ring va natijalaringizni darhol bilib oling.", Signal.GradientCyanAmber, 160),
                        SplashItem(Icons.Default.CloudSync, "Tarmoq", "Lokal tarmoq orqali pedagoglar va tinglovchilar o'rtasida ma'lumotlarni oson almashing.", Signal.GradientAmberPrimary, 220)
                    ),
                    minItemWidth = 260.dp,
                    spacing = 18.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { item, itemModifier ->
                    Reveal(visible, item.delay, itemModifier) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            GradientTile(item.brush, size = 48.dp) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SignalButton("Boshlash", onClick = onContinue, style = ButtonStyle.Cyan, height = 52.dp, contentPadding = PaddingValues(horizontal = 40.dp))
                        Text("Enter — boshlash", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkFaint)
                    }
                }
            }
        }
    }
}

private class SplashItem(val icon: ImageVector, val title: String, val description: String, val brush: Brush, val delay: Int)
