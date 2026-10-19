package com.example.poststudy.presentation.ui.screens.network

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.network.ActiveAdmin
import com.example.poststudy.data.network.ServerAddress
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences

private val prefs: Preferences? = try {
    Preferences.userRoot().node("breakpoint")
} catch (e: Exception) {
    null
}
private const val LAST_ADDRESS_KEY = "last_server_address"
private const val SCAN_INTERVAL_MS = 3000L

/** Typed text that is meant as an IP/host rather than an admin's name. */
private fun looksLikeAddress(text: String): Boolean =
    text.any { it == '.' || it == ':' } || text.all { it.isDigit() }

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
    // Key events reach the screen only while something in it has focus
    val screenFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { screenFocus.requestFocus() } }

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

    Column(
        Modifier
            .fillMaxSize()
            .background(Signal.Surface)
            .focusRequester(screenFocus)
            .focusable()
            // Esc leaves the screen even while the address field has focus
            .onPreviewKeyEvent {
                if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                    onBack()
                    true
                } else false
            }
    ) {
        // Dark band: radar finds the admins
        Box(Modifier.fillMaxWidth().height(400.dp)) {
            SignalBackground()
            Column(Modifier.fillMaxSize().padding(horizontal = Space.page, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BackButton(onClick = onBack, onDark = true, modifier = Modifier.padding(start = 0.dp))
                    Text("Tarmoqqa ulanish", style = MaterialTheme.typography.titleLarge, color = Signal.OnDark)
                    Spacer(Modifier.weight(1f))
                    GlassPill(if (isScanning) "Qidirilmoqda · UDP 8099" else "Ro'yxat yangilangan", live = isScanning)
                    SignalIconButton(Icons.Outlined.Refresh, "Yangilash", { scanRequest++ }, tint = Signal.OnDark, container = Signal.GlassFill, borderColor = Signal.GlassBorderStrong, size = 40.dp, iconSize = 18.dp, onDark = true)
                    SignalButton("Admin topilmayaptimi?", onClick = { showHelp = true }, style = ButtonStyle.Glass, height = 40.dp, contentPadding = PaddingValues(horizontal = 14.dp))
                }
                Column(Modifier.widthIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("Faol adminlar · ${admins.size}", color = Signal.Amber)
                    Text(
                        if (admins.isEmpty()) "Radar adminlarni qidirmoqda" else "Radar adminlarni topdi. Birini tanlang.",
                        style = MaterialTheme.typography.displaySmall,
                        color = Signal.OnDark
                    )
                    Text(
                        "Bir xil lokal tarmoqdagi barcha \"Tarmoq yoqilgan\" adminlar shu yerda ko'rinadi. Ro'yxat har 3 soniyada yangilanadi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Signal.OnDarkMuted
                    )
                }
                Spacer(Modifier.weight(1f))
                if (admins.isEmpty()) {
                    GlassCard(Modifier.widthIn(max = 560.dp), padding = 16.dp) {
                        Text(
                            if (isScanning) "Tarmoqdan qidirilmoqda..." else "Hozircha faol admin topilmadi. Admin \"Tarmoqqa ulanish\" tugmasini bosgan bo'lishi kerak.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Signal.OnDarkMuted
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(300.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 170.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(admins, key = { "${it.address.host}:${it.address.port}" }) { admin ->
                            val problem = admin.problem
                            GlassCard(
                                Modifier.fillMaxWidth(),
                                padding = 16.dp,
                                onClick = {
                                    input = admin.name
                                    if (problem != null) errorMessage = problem else open(admin.address.toString(), admin.name)
                                }
                            ) { hovered ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    InitialsAvatar(admin.name, Modifier.glassIcon(hovered), size = 44.dp, gradient = true)
                                    Column(Modifier.weight(1f)) {
                                        Text(admin.name, style = MaterialTheme.typography.titleMedium, color = Signal.OnDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            if (problem != null) "${admin.address} · versiya mos emas" else admin.address.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (problem != null) Signal.DangerOnDark else Signal.OnDarkMuted
                                        )
                                    }
                                    if (problem == null) LiveDot()
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Signal.Cyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Light part: manual connection
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Row(
                Modifier.widthIn(max = Space.contentMaxWidth + Space.page * 2).fillMaxWidth().padding(horizontal = Space.page, vertical = Space.xl),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SignalCard(Modifier.weight(1.3f), padding = 22.dp) {
                    Text("Ro'yxatda yo'qmi? Qo'lda ulaning", style = MaterialTheme.typography.headlineSmall, color = Signal.Ink)
                    Text("Admin ekranidagi \"Sizning IP\" manzilini yoki username'ini yozing.", style = MaterialTheme.typography.bodyMedium, color = Signal.Muted)
                    Spacer(Modifier.height(16.dp))
                    if (errorMessage.isNotEmpty()) {
                        Surface(shape = AppDesign.ComponentShape, color = Signal.DangerContainer, border = BorderStroke(1.dp, Signal.DangerBorder), modifier = Modifier.padding(bottom = 12.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Signal.Danger, modifier = Modifier.size(18.dp))
                                Text(errorMessage, color = Signal.OnDangerContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Admin username yoki IP manzil", style = MaterialTheme.typography.labelLarge, color = Signal.Ink)
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it; errorMessage = "" },
                                leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null, tint = Signal.Muted) },
                                placeholder = { Text("masalan, admin yoki 192.168.1.5") },
                                modifier = Modifier.fillMaxWidth().onPreviewKeyEvent {
                                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                                        connectTyped()
                                        true
                                    } else false
                                },
                                shape = AppDesign.SmallShape,
                                singleLine = true,
                                enabled = !isLoading,
                                isError = errorMessage.isNotEmpty()
                            )
                        }
                        SignalButton(
                            text = if (isLoading) "Ulanmoqda..." else "Ulanish",
                            onClick = { connectTyped() },
                            enabled = !isLoading,
                            style = ButtonStyle.Primary,
                            height = 48.dp,
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Enter — ulanish · Esc — orqaga", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
                }
                SignalCard(Modifier.weight(1f), padding = 22.dp, color = Signal.WarningContainer, borderColor = Signal.WarningBorder) {
                    Text("Admin topilmasa", style = MaterialTheme.typography.titleMedium, color = Signal.OnWarningContainer)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Ikkala kompyuter bitta tarmoqda bo'lsin (bir router yoki Wi-Fi).",
                        "Admin \"Tarmoqqa ulanish\" tugmasini bosgan bo'lsin.",
                        "IP bilan ham ulanmasa: admin kompyuterida Firewall ruxsati kerak."
                    ).forEachIndexed { i, tip ->
                        Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${i + 1}.", style = MaterialTheme.typography.labelLarge, color = Signal.OnWarningContainer)
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = Signal.OnWarningContainer)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SignalButton("Firewall ko'rsatmalari", onClick = { showHelp = true }, style = ButtonStyle.Outlined, height = 36.dp)
                }
            }
        }
    }
}
