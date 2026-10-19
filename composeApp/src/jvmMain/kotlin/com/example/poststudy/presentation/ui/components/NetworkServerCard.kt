package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.session.AppServer
import com.example.poststudy.data.system.WindowsFirewall
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Turns the LAN server on and off. It is off after every start of the app; with background mode
 * on, closing the window keeps it running in the system tray. [onDark] renders the glass
 * variant used inside the admin hero.
 */
@Composable
fun NetworkServerCard(modifier: Modifier = Modifier, onDark: Boolean = false) {
    val scope = rememberCoroutineScope()
    val running by AppContainer.networkRepository.serverRunning.collectAsState()
    var backgroundMode by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    var firewallBlocked by remember { mutableStateOf(false) }
    var firewallNotice by remember { mutableStateOf<String?>(null) }
    var preparing by remember { mutableStateOf(false) }

    if (showHelp) {
        NetworkHelpDialog(forAdmin = true, onDismiss = { showHelp = false })
    }

    // A cancelled Windows prompt leaves a silent block rule; warn right away
    LaunchedEffect(running) {
        firewallBlocked = running && withContext(Dispatchers.IO) {
            WindowsFirewall.status() == WindowsFirewall.Status.Blocked
        }
    }

    LaunchedEffect(Unit) {
        backgroundMode = withContext(Dispatchers.IO) { AppContainer.localRepository.isBackgroundModeEnabled() }
    }

    val toggle: () -> Unit = {
        if (!isBusy) {
            isBusy = true
            error = null
            firewallNotice = null
            scope.launch {
                val startFailed = withContext(Dispatchers.IO) {
                    if (running) {
                        AppServer.stop()
                        null
                    } else {
                        // Windows asks "allow access?" the first time the app listens; a cancelled
                        // prompt silently blocks it for good. Adding the rule first (one UAC
                        // prompt) avoids that, and works for standard users via admin credentials.
                        if (WindowsFirewall.isWindows && !WindowsFirewall.checkedThisRun) {
                            preparing = true
                            val status = WindowsFirewall.status()
                            if (status == WindowsFirewall.Status.NoRule || status == WindowsFirewall.Status.Blocked) {
                                WindowsFirewall.fix().onFailure { firewallNotice = it.message }
                            }
                            WindowsFirewall.checkedThisRun = true
                            preparing = false
                        }
                        if (!AppServer.start()) {
                            AppContainer.networkRepository.serverError() ?: "Serverni yoqib bo'lmadi"
                        } else null
                    }
                }
                error = startFailed
                isBusy = false
            }
        }
    }

    val titleColor = if (onDark) Signal.OnDark else Signal.Ink
    val mutedColor = if (onDark) Signal.OnDarkMuted else Signal.Muted
    val accent = if (running) (if (onDark) Signal.Cyan else Signal.Primary) else mutedColor

    val body: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (running) {
                GradientTile(Signal.GradientPrimary, size = 48.dp) {
                    Icon(Icons.Outlined.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            } else {
                Surface(shape = AppDesign.ComponentShape, color = if (onDark) Color.White.copy(alpha = 0.08f) else Signal.SurfaceContainer) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = mutedColor, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (running) LiveDot(accent)
                    Text(if (running) "Tarmoq yoqilgan" else "Tarmoq o'chiq", style = MaterialTheme.typography.titleLarge, color = titleColor)
                }
                Text(
                    if (running) "Tinglovchilar quyidagi manzil orqali ulanishi mumkin"
                    else "Tinglovchilar hozir bu kompyuterga ulana olmaydi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor
                )
            }
            SignalButton(
                text = when {
                    preparing -> "Firewall sozlanmoqda..."
                    running -> "Tarmoqni to'xtatish"
                    else -> "Tarmoqqa ulanish"
                },
                onClick = toggle,
                enabled = !isBusy,
                icon = if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
                style = when {
                    running && onDark -> ButtonStyle.Glass
                    running -> ButtonStyle.Outlined
                    onDark -> ButtonStyle.Cyan
                    else -> ButtonStyle.Primary
                },
                height = 44.dp
            )
        }

        if (running) {
            ServerInfoBadge(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), onDark = onDark)
        }
        error?.let {
            Text(it, color = if (onDark) Signal.DangerOnDark else Signal.Danger, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
        }
        firewallNotice?.let {
            Text(
                "Firewall qoidasi qo'shilmadi: $it Tinglovchilar ulana olmasa, pastdagi yo'riqnomadan foydalaning.",
                color = if (onDark) Signal.Amber else Signal.OnWarningContainer,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (firewallBlocked) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = AppDesign.ComponentShape,
                color = if (onDark) Signal.Danger.copy(alpha = 0.25f) else Signal.DangerContainer,
                border = BorderStroke(1.dp, if (onDark) Signal.Danger.copy(alpha = 0.5f) else Signal.DangerBorder)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Windows Firewall BreakPoint'ni bloklagan — tinglovchilar ulana olmaydi.",
                        color = if (onDark) Signal.OnDark else Signal.OnDangerContainer,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    SignalButton("Tuzatish", onClick = { showHelp = true }, style = ButtonStyle.Danger, height = 36.dp)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SignalButton(
                "Ulanish muammosi bormi? Firewall va tarmoq sozlamalari",
                onClick = { showHelp = true },
                style = if (onDark) ButtonStyle.Glass else ButtonStyle.Text,
                height = 34.dp,
                contentPadding = PaddingValues(horizontal = 12.dp)
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Fonda ishlash", style = MaterialTheme.typography.labelLarge, color = titleColor)
                Text(
                    if (backgroundMode) "Oyna yopilsa ham tarmoq ishlaydi (soat yonidagi belgi)" else "Dastur yopilishi bilan tarmoq to'xtaydi",
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = backgroundMode,
                onCheckedChange = { enabled ->
                    backgroundMode = enabled
                    scope.launch(Dispatchers.IO) { AppContainer.localRepository.setBackgroundModeEnabled(enabled) }
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = if (onDark) Signal.Cyan else Signal.Primary,
                    checkedThumbColor = if (onDark) Signal.Dark else Color.White
                )
            )
        }
    }

    if (onDark) {
        GlassCard(modifier = modifier, padding = 20.dp) { body() }
    } else {
        SignalCard(modifier = modifier, padding = 20.dp, borderColor = if (running) Signal.CyanSoft else Signal.Outline) { body() }
    }
}
