package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.network.NetworkManager
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shows the admin's LAN address(es) that students must type, plus server problems. */
@Composable
fun ServerInfoBadge(modifier: Modifier = Modifier, onDark: Boolean = false) {
    var addresses by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(Unit) {
        addresses = withContext(Dispatchers.IO) { AppContainer.networkRepository.getLocalIpAddresses() }
    }
    // Recompose when the server starts or stops
    val running by AppContainer.networkRepository.serverRunning.collectAsState()
    val serverError = AppContainer.networkRepository.serverError()
    // Students only need the port when the default one was busy
    val portSuffix = AppContainer.networkRepository.serverPort()
        ?.takeIf { it != NetworkManager.DEFAULT_PORT }
        ?.let { ":$it" }
        .orEmpty()
    val muted = if (onDark) Signal.OnDarkMuted else Signal.Muted
    val color = when {
        serverError != null -> if (onDark) Signal.DangerOnDark else Signal.Danger
        !running -> muted
        else -> if (onDark) Signal.Cyan else Signal.Primary
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Surface(
            color = if (onDark) Color.White.copy(alpha = 0.08f) else Signal.Container,
            shape = AppDesign.PillShape,
            border = BorderStroke(1.dp, if (onDark) Color.White.copy(alpha = 0.16f) else Signal.CyanSoft)
        ) {
            val list = addresses
            val text = when {
                list == null -> "IP aniqlanmoqda..."
                list.isEmpty() -> "Tarmoq topilmadi"
                else -> "Sizning IP: ${list.first()}$portSuffix"
            }
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        val others = addresses.orEmpty().drop(1)
        if (others.isNotEmpty()) {
            Text(
                text = "Boshqa manzillar: ${others.joinToString(", ") { it + portSuffix }}",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (!running && serverError == null) {
            Text(
                text = "Tarmoq o'chiq — admin bosh sahifasidan yoqing",
                style = MaterialTheme.typography.labelLarge,
                color = muted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (serverError != null) {
            Text(
                text = "Tarmoq serveri ishlamayapti: $serverError",
                style = MaterialTheme.typography.labelLarge,
                color = color,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
