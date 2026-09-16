package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.session.AppServer
import com.example.poststudy.data.system.WindowsFirewall
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.theme.AppDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Turns the LAN server on and off. It is off after every start of the app; with background mode
 * on, closing the window keeps it running in the system tray.
 */
@Composable
fun NetworkServerCard(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val running by AppContainer.networkRepository.serverRunning.collectAsState()
    var backgroundMode by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    var firewallBlocked by remember { mutableStateOf(false) }

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

    val accent = if (running) AppDesign.Emerald else Color(0xFF64748B)
    Surface(
        modifier = modifier,
        shape = AppDesign.CardShape,
        color = Color.White,
        border = BorderStroke(3.dp, accent.copy(alpha = 0.4f)),
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).background(accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (running) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (running) "Tarmoq yoqilgan" else "Tarmoq o'chiq",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                    Text(
                        if (running) "Tinglovchilar quyidagi manzil orqali ulanishi mumkin"
                        else "Tinglovchilar hozir bu kompyuterga ulana olmaydi",
                        color = Color(0xFF64748B)
                    )
                }
                Button(
                    onClick = {
                        if (!isBusy) {
                            isBusy = true
                            error = null
                            scope.launch {
                                val startFailed = withContext(Dispatchers.IO) {
                                    if (running) {
                                        AppServer.stop()
                                        null
                                    } else if (!AppServer.start()) {
                                        AppContainer.networkRepository.serverError() ?: "Serverni yoqib bo'lmadi"
                                    } else null
                                }
                                error = startFailed
                                isBusy = false
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.height(56.dp),
                    shape = AppDesign.ComponentShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (running) Color(0xFFDC2626) else AppDesign.Emerald
                    )
                ) {
                    Icon(if (running) Icons.Default.StopCircle else Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) "Tarmoqni to'xtatish" else "Tarmoqqa ulanish", fontWeight = FontWeight.Black)
                }
            }

            if (running) {
                ServerInfoBadge(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            }
            error?.let {
                Text(it, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            }
            if (firewallBlocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = AppDesign.ComponentShape,
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(2.dp, Color(0xFFDC2626).copy(alpha = 0.4f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Windows Firewall BreakPoint'ni bloklagan — tinglovchilar ulana olmaydi.",
                            color = Color(0xFFB91C1C),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showHelp = true },
                            shape = AppDesign.ComponentShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Tuzatish", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            TextButton(onClick = { showHelp = true }, modifier = Modifier.padding(top = 4.dp)) {
                Text("Ulanish muammosi bormi? Firewall va tarmoq sozlamalari", color = AppDesign.Indigo, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Fonda ishlash", fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                    Text(
                        "Yoqilsa, dastur oynasi yopilganda ham tarmoq kompyuter yoniq ekan ishlashda davom etadi " +
                            "(soat yonidagi BreakPoint belgisidan boshqariladi). O'chiq bo'lsa, dastur yopilishi bilan tarmoq to'xtaydi.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = backgroundMode,
                    onCheckedChange = { enabled ->
                        backgroundMode = enabled
                        scope.launch(Dispatchers.IO) { AppContainer.localRepository.setBackgroundModeEnabled(enabled) }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = AppDesign.Emerald)
                )
            }
        }
    }
}
