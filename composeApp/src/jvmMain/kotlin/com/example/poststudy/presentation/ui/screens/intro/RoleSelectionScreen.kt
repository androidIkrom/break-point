package com.example.poststudy.presentation.ui.screens.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.UserRole
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit,
    onJoinNetwork: () -> Unit,
    onAbout: () -> Unit = {}
) {
    val parallax = rememberParallax()
    val serverRunning by AppContainer.networkRepository.serverRunning.collectAsState()
    val reduceMotion = LocalReduceMotion.current
    var visible by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) { delay(60); visible = true }

    Box(Modifier.fillMaxSize().parallaxSource(parallax)) {
        SignalBackground(parallax = parallax.value)

        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 32.dp)) {
            Column(Modifier.widthIn(max = 1240.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Reveal(visible, 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        InstituteLogo(size = 60.dp, glow = true)
                        Column {
                            Text("BreakPoint", style = MaterialTheme.typography.headlineMedium, color = Signal.OnDark)
                            Text("Axborot-kommunikatsiya texnologiyalari va harbiy aloqa instituti", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkMuted)
                        }
                        Spacer(Modifier.weight(1f))
                        if (serverRunning) GlassPill("Tarmoq yoqilgan", live = true)
                        SignalButton("Loyiha haqida", onClick = onAbout, style = ButtonStyle.Glass, height = 34.dp, contentPadding = PaddingValues(horizontal = 14.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                Reveal(visible, 60) {
                    Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow("Bilimlarni mustahkamlash va baholash tizimi", color = Signal.Amber)
                        Text("Bugun nimadan boshlaymiz?", style = MaterialTheme.typography.displayLarge, color = Signal.OnDark)
                        Text(
                            "Rolingizni tanlang. Admin darslar va guruhlarni boshqaradi, tinglovchi o'rganadi va test topshiradi, tarmoq orqali admin kompyuteriga ulanish mumkin.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Signal.OnDarkMuted
                        )
                    }
                }

                AdaptiveGrid(
                    items = listOf(
                        RoleItem("Admin", "Darslar, imtihonlar, guruhlar. Tarmoq serverini yoqish va natijalarni kuzatish.", "Boshqaruv paneli", Icons.Default.Person, Signal.GradientPrimary, Signal.Cyan, 120) { onRoleSelected(UserRole.Teacher) },
                        RoleItem("Tinglovchi", "Shu kompyuterdagi darslarni o'rganish va guruh testini topshirish.", "Lokal darslar", Icons.Default.School, Signal.GradientCyanAmber, Signal.Amber, 180) { onRoleSelected(UserRole.Student) },
                        RoleItem("Tarmoq", "Lokal tarmoqdagi admin kompyuteriga ulanib, guruh mashg'ulotini olish.", "Adminga ulanish", Icons.Default.Lan, Signal.GradientAmberPrimary, Signal.Cyan, 240, onJoinNetwork)
                    ),
                    minItemWidth = 260.dp,
                    spacing = 18.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { item, itemModifier ->
                    Reveal(visible, item.delay, itemModifier) {
                        RoleCard(item, Modifier.fillMaxWidth())
                    }
                }

                Reveal(visible, 300) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        listOf("Enter — davom etish", "Esc — orqaga", "Sichqonchani yurgizing: fon siljiydi").forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkFaint)
                        }
                    }
                }
            }
        }
    }
}

private class RoleItem(
    val title: String,
    val description: String,
    val action: String,
    val icon: ImageVector,
    val brush: Brush,
    val accent: Color,
    val delay: Int,
    val onClick: () -> Unit
)

@Composable
private fun RoleCard(item: RoleItem, modifier: Modifier) {
    GlassCard(modifier = modifier.heightIn(min = 230.dp), onClick = item.onClick) { hovered ->
        GradientTile(item.brush, Modifier.glassIcon(hovered), size = 52.dp, glow = item.accent) {
            Icon(item.icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = if (item.accent == Signal.Amber) Signal.Dark else Color.White)
        }
        Spacer(Modifier.height(16.dp))
        Text(item.title, style = MaterialTheme.typography.headlineSmall, color = Signal.OnDark)
        Spacer(Modifier.height(4.dp))
        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Signal.OnDarkMuted)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.action, style = MaterialTheme.typography.labelLarge, color = item.accent)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = item.accent, modifier = Modifier.size(16.dp))
        }
    }
}

/** Fade + rise entry used by hero screens; staggered through [delayMillis]. */
@Composable
fun Reveal(visible: Boolean, delayMillis: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduce = LocalReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduce) fadeIn(tween(1)) else fadeIn(tween(AppMotion.emphasized, delayMillis, AppMotion.decelerateEasing)) +
            slideInVertically(tween(AppMotion.emphasized, delayMillis, AppMotion.decelerateEasing)) { 14.dp.value.toInt() * 2 }
    ) { content() }
}
