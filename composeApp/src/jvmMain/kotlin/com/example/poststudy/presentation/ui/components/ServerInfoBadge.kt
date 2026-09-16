package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shows the admin's LAN address(es) that students must type, plus server problems. */
@Composable
fun ServerInfoBadge(modifier: Modifier = Modifier) {
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
    val color = when {
        serverError != null -> Color(0xFFEF4444)
        !running -> Color(0xFF94A3B8)
        else -> Color(0xFF6366F1)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            val list = addresses
            val text = when {
                list == null -> "IP aniqlanmoqda..."
                list.isEmpty() -> "Tarmoq topilmadi"
                else -> "Sizning IP: ${list.first()}$portSuffix"
            }
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        val others = addresses.orEmpty().drop(1)
        if (others.isNotEmpty()) {
            Text(
                text = "Boshqa manzillar: ${others.joinToString(", ") { it + portSuffix }}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (!running && serverError == null) {
            Text(
                text = "Tarmoq o'chiq — admin bosh sahifasidan yoqing",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (serverError != null) {
            Text(
                text = "Tarmoq serveri ishlamayapti: $serverError",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB91C1C),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

