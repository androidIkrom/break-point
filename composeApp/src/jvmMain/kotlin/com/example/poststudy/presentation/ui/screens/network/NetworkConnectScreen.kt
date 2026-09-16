package com.example.poststudy.presentation.ui.screens.network

import com.example.poststudy.presentation.ui.components.ScrollableCentered
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.presentation.ui.components.NetworkHelpDialog
import com.example.poststudy.di.AppContainer
import com.example.poststudy.data.network.ActiveAdmin
import com.example.poststudy.data.network.ServerAddress
import com.example.poststudy.presentation.theme.AppDesign
import java.util.prefs.Preferences

private val prefs: Preferences? = try {
    Preferences.userRoot().node("breakpoint")
} catch (e: Exception) {
    null
}
private const val LAST_ADDRESS_KEY = "last_server_address"
private const val SCAN_INTERVAL_MS = 3000L

private val Indigo = Color(0xFF6366F1)
private val TextDark = Color(0xFF1E293B)
private val TextMuted = Color(0xFF64748B)

/** Typed text that is meant as an IP/host rather than an admin's name. */
private fun looksLikeAddress(text: String): Boolean =
    text.any { it == '.' || it == ':' } || text.all { it.isDigit() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkConnectScreen(
    onConnected: (ServerAddress) -> Unit,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf(prefs?.get(LAST_ADDRESS_KEY, "") ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var admins by remember { mutableStateOf<List<ActiveAdmin>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    var scanRequest by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        NetworkHelpDialog(forAdmin = false, onDismiss = { showHelp = false })
    }

    // Keep the list of running admins fresh while this screen is open
    LaunchedEffect(scanRequest) {
        while (true) {
            isScanning = true
            admins = withContext(Dispatchers.IO) { AppContainer.networkRepository.discoverAdmins() }
            isScanning = false
            delay(SCAN_INTERVAL_MS)
        }
    }

    /** Pings [target] (an address string) and opens it; [remembered] is prefilled next time. */
    fun open(target: String, remembered: String) {
        if (isLoading) return
        isLoading = true
        errorMessage = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) { AppContainer.networkRepository.connect(target) }
            isLoading = false
            result
                .onSuccess { address ->
                    try {
                        prefs?.put(LAST_ADDRESS_KEY, remembered)
                    } catch (_: Exception) {
                    }
                    onConnected(address)
                }
                .onFailure { errorMessage = it.message ?: "Adminga ulanib bo'lmadi." }
        }
    }

    fun connectTyped() {
        val typed = input.trim().replace(Regex("\\s+"), " ")
        if (typed.isEmpty()) {
            errorMessage = "Admin username'ini kiriting yoki ro'yxatdan tanlang"
            return
        }
        val matches = admins.filter { it.name.equals(typed, ignoreCase = true) }
        when {
            matches.size == 1 -> matches.single().problem?.let { errorMessage = it }
                ?: open(matches.single().address.toString(), typed)
            matches.size > 1 -> errorMessage = "Bu username bilan bir nechta admin bor. Kerakligini ro'yxatdan tanlang."
            looksLikeAddress(typed) -> {
                if (AppContainer.networkRepository.parseAddress(typed) == null) {
                    errorMessage = "IP manzil noto'g'ri. Masalan: 192.168.1.5"
                } else {
                    open(typed, typed)
                }
            }
            else -> errorMessage = "\"$typed\" username'li faol admin topilmadi. Admin tarmoqni yoqqanini tekshiring."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Tarmoqqa ulanish",
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Black
                        )
                    },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            ScrollableCentered(Modifier.fillMaxSize().padding(paddingValues)) {
                Card(
                    modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth().padding(16.dp),
                    shape = AppDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(3.dp, Indigo.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lan,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Indigo
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Adminga ulanish",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextDark
                        )
                        Text(
                            text = "Ro'yxatdan adminni tanlang yoki uning username'ini yozing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        ActiveAdminsList(
                            admins = admins,
                            isScanning = isScanning,
                            enabled = !isLoading,
                            onRefresh = { scanRequest++ },
                            onSelect = { admin ->
                                input = admin.name
                                val problem = admin.problem
                                if (problem != null) errorMessage = problem else open(admin.address.toString(), admin.name)
                            }
                        )

                        TextButton(onClick = { showHelp = true }) {
                            Text("Admin topilmayaptimi?", color = Indigo, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(8.dp))

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFB91C1C),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it; errorMessage = "" },
                            label = { Text("Admin username") },
                            supportingText = { Text("Ro'yxat bo'sh bo'lsa, admin ekranidagi IP manzilni yozing") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().onPreviewKeyEvent {
                                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                                    connectTyped()
                                    true
                                } else false
                            },
                            shape = AppDesign.ComponentShape,
                            singleLine = true,
                            enabled = !isLoading,
                            isError = errorMessage.isNotEmpty(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Indigo)
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { connectTyped() },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = AppDesign.ComponentShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("ULANISH", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveAdminsList(
    admins: List<ActiveAdmin>,
    isScanning: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    onSelect: (ActiveAdmin) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppDesign.ComponentShape,
        color = Color(0xFFF8FAFC),
        border = BorderStroke(2.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(if (admins.isEmpty()) Color(0xFF94A3B8) else AppDesign.Emerald, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Faol adminlar (${admins.size})",
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Indigo)
                } else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = Indigo)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (admins.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (isScanning) "Tarmoqdan qidirilmoqda..."
                        else "Hozircha faol admin topilmadi.\nAdmin \"Tarmoqqa ulanish\" tugmasini bosgan bo'lishi kerak.",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(admins, key = { "${it.address.host}:${it.address.port}" }) { admin ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(AppDesign.ComponentShape)
                                .background(Color.White)
                                .clickable(enabled = enabled) { onSelect(admin) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp).background(Indigo.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(admin.name, fontWeight = FontWeight.Black, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    admin.problem?.let { "${admin.address} • versiya mos emas" } ?: admin.address.toString(),
                                    color = if (admin.problem != null) Color(0xFFDC2626) else TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text("Ulanish ›", color = Indigo, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
