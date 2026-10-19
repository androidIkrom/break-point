package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.ui.components.*
import com.example.poststudy.presentation.ui.screens.intro.Reveal
import kotlinx.coroutines.delay

@Composable
fun StudentHomeScreen(
    onNavigateToPreparation: () -> Unit,
    onNavigateToTest: () -> Unit,
    onBack: () -> Unit
) {
    val parallax = rememberParallax()
    val reduceMotion = LocalReduceMotion.current
    var visible by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) { delay(60); visible = true }

    Box(Modifier.fillMaxSize().parallaxSource(parallax)) {
        SignalBackground(parallax = parallax.value)
        Row(Modifier.align(Alignment.TopStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack, onDark = true)
            Spacer(Modifier.width(12.dp))
            Text("Tinglovchi paneli", style = MaterialTheme.typography.titleLarge, color = Signal.OnDark)
        }

        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 72.dp)) {
            Column(Modifier.widthIn(max = 1000.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Reveal(visible, 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow("Tinglovchi", color = Signal.Amber)
                        Text("Xush kelibsiz, tinglovchi", style = MaterialTheme.typography.displayLarge, color = Signal.OnDark)
                        Text("Bugun qaysi mavzuda bilimingizni boyitamiz?", style = MaterialTheme.typography.bodyLarge, color = Signal.OnDarkMuted)
                    }
                }
                AdaptiveGrid(
                    items = listOf(
                        HomeChoice("Tayyorgarlik", "Darsliklarni o'rganish. Shu kompyuterdagi taqdimotlarni taymersiz ko'rib chiqing.", "Darslarni ochish", Icons.AutoMirrored.Filled.MenuBook, Signal.GradientCyanAmber, Signal.Amber, 100, onNavigateToPreparation),
                        HomeChoice("Bilim testi", "O'zingizni sinab ko'ring. Guruhingizga biriktirilgan mashg'ulotni boshlang.", "Guruhni tanlash", Icons.AutoMirrored.Filled.Assignment, Signal.GradientPrimary, Signal.Cyan, 160, onNavigateToTest)
                    ),
                    minItemWidth = 320.dp,
                    spacing = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { item, itemModifier ->
                    Reveal(visible, item.delay, itemModifier) {
                        GlassCard(Modifier.fillMaxWidth().heightIn(min = 240.dp), onClick = item.onClick, padding = 28.dp) { hovered ->
                            GradientTile(item.brush, Modifier.glassIcon(hovered), size = 56.dp, glow = item.accent) {
                                Icon(item.icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = if (item.accent == Signal.Amber) Signal.Dark else Color.White)
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(item.title, style = MaterialTheme.typography.headlineMedium, color = Signal.OnDark)
                            Spacer(Modifier.height(6.dp))
                            Text(item.description, style = MaterialTheme.typography.bodyLarge, color = Signal.OnDarkMuted)
                            Spacer(Modifier.weight(1f))
                            Spacer(Modifier.height(18.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.action, style = MaterialTheme.typography.labelLarge, color = item.accent)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = item.accent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private class HomeChoice(
    val title: String,
    val description: String,
    val action: String,
    val icon: ImageVector,
    val brush: Brush,
    val accent: Color,
    val delay: Int,
    val onClick: () -> Unit
)
