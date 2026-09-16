package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.poststudy.data.system.WindowsFirewall
import com.example.poststudy.presentation.theme.AppDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TextDark = Color(0xFF1E293B)
private val TextMuted = Color(0xFF475569)

/** Firewall status plus step-by-step Windows 10/11 instructions for connection problems. */
@Composable
fun NetworkHelpDialog(forAdmin: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(760.dp)
                .heightIn(max = 760.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        onDismiss(); true
                    } else false
                },
            shape = AppDesign.CardShape,
            color = Color.White,
            border = BorderStroke(2.dp, AppDesign.Indigo.copy(alpha = 0.25f)),
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AppDesign.Indigo, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (forAdmin) "Tarmoq va Windows Firewall sozlamalari" else "Admin topilmayaptimi?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF065F46)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (forAdmin) AdminHelp() else StudentHelp()
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).height(52.dp),
                    shape = AppDesign.ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Emerald)
                ) {
                    Text("Tushunarli", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun AdminHelp() {
    FirewallStatusCard()

    HelpSection(
        "Windows 11 — qo'lda ruxsat berish",
        listOf(
            "Start menyusidan \"Windows Security\" (Windows xavfsizligi) dasturini oching.",
            "\"Firewall & network protection\" bo'limiga kiring va pastdagi \"Allow an app through firewall\" havolasini bosing.",
            "\"Change settings\" tugmasini bosing (administrator huquqi kerak).",
            "Ro'yxatdan \"BreakPoint\" ni toping va \"Private\" hamda \"Public\" katakchalarini belgilang. " +
                "Ro'yxatda bo'lmasa: \"Allow another app...\" → \"Browse...\" → pastdagi dastur faylini tanlang → \"Add\".",
            "\"OK\" ni bosing, so'ng BreakPoint'da tarmoqni to'xtatib, qayta yoqing."
        )
    )
    HelpSection(
        "Windows 10 — qo'lda ruxsat berish",
        listOf(
            "\"Control Panel\" (Boshqaruv paneli) → \"System and Security\" → \"Windows Defender Firewall\" ni oching.",
            "Chap tomondagi \"Allow an app or feature through Windows Defender Firewall\" havolasini bosing.",
            "\"Change settings\" tugmasini bosing (administrator huquqi kerak).",
            "\"BreakPoint\" qatorida \"Private\" va \"Public\" katakchalarini belgilang. Ro'yxatda bo'lmasa, " +
                "\"Allow another app...\" orqali pastdagi dastur faylini qo'shing.",
            "\"OK\" ni bosing va BreakPoint'da tarmoqni qayta yoqing."
        )
    )
    HelpSection(
        "Ruxsat oynasida \"Cancel\" bosilgan yoki oyna yopilgan bo'lsa",
        listOf(
            "Windows bu holatda BreakPoint uchun bloklash qoidasini yaratadi va oynani boshqa ko'rsatmaydi.",
            "Eng osoni — yuqoridagi \"Avtomatik tuzatish\" tugmasi. Qo'lda qilish uchun: \"Windows Defender Firewall\" → " +
                "\"Advanced settings\" → \"Inbound Rules\" ni oching.",
            "Ro'yxatdan \"BreakPoint\" (dasturdan ishga tushirilganda \"OpenJDK Platform binary\") nomli, qizil belgili " +
                "qoidalarni tanlab, o'ng tomondagi \"Delete\" ni bosing.",
            "BreakPoint'ni yopib qayta oching va tarmoqni yoqing. Chiqqan oynada \"Private\" va \"Public\" ni belgilab, " +
                "\"Allow access\" ni bosing."
        )
    )
    HelpSection(
        "Tarmoq turi \"Public\" bo'lsa",
        listOf(
            "Internetsiz lokal tarmoq ko'pincha \"Unidentified network\" (Public) deb aniqlanadi va ulanishlar cheklanadi.",
            "Windows 11: Settings → Network & internet → Ethernet (yoki Wi-Fi) → \"Network profile type\" → \"Private network\".",
            "Windows 10: Settings → Network & Internet → Status → \"Properties\" → \"Private\".",
            "\"Avtomatik tuzatish\" ruxsatni barcha tarmoq turlarida, lekin faqat lokal tarmoq (LocalSubnet) uchun beradi."
        )
    )
    HelpSection(
        "Boshqa sabablar",
        listOf(
            "Antivirus (Kaspersky, ESET, Avast va h.k.) o'z firewall'iga ega bo'lsa, unda ham BreakPoint'ga ruxsat bering.",
            "Administrator huquqingiz bo'lmasa, kompyuter administratoriga (IT bo'limiga) shu oynani ko'rsating.",
            "Dastur ishlatadigan portlar: TCP 8080–8089 (sessiyalar) va UDP 8099 (adminlar ro'yxati)."
        )
    )
    ExecutablePathRow()
}

@Composable
private fun StudentHelp() {
    HelpSection(
        "Tekshirib ko'ring",
        listOf(
            "Admin kompyuterida BreakPoint ochiq va \"Tarmoqqa ulanish\" tugmasi bosilgan (\"Tarmoq yoqilgan\") bo'lishi kerak.",
            "Ikkala kompyuter bitta lokal tarmoqda bo'lishi kerak: bir xil router, switch yoki Wi-Fi.",
            "Ro'yxat bo'sh bo'lsa, admin ekranidagi \"Sizning IP\" manzilini (masalan, 192.168.1.7) \"Admin username\" maydoniga yozib, \"Ulanish\" ni bosing.",
            "IP bilan ham ulanmasa, admin kompyuterida Windows Firewall ruxsat bermagan. Admin o'z ekranidagi " +
                "\"Ulanish muammosi?\" bo'limidagi ko'rsatmalarni bajarsin.",
            "Tarmoq turi \"Public\" bo'lsa, uni \"Private\" ga o'zgartiring: Windows 11 — Settings → Network & internet → " +
                "Ethernet/Wi-Fi → Network profile type; Windows 10 — Settings → Network & Internet → Status → Properties."
        )
    )
}

@Composable
private fun FirewallStatusCard() {
    if (!WindowsFirewall.isWindows) return
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<WindowsFirewall.Status?>(null) }
    var isFixing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        status = null
        scope.launch { status = withContext(Dispatchers.IO) { WindowsFirewall.status() } }
    }
    LaunchedEffect(Unit) { refresh() }

    val (color, text) = when (status) {
        null -> Color(0xFF64748B) to "Firewall holati tekshirilmoqda..."
        WindowsFirewall.Status.Allowed -> AppDesign.Emerald to "Windows Firewall BreakPoint'ga ruxsat bergan"
        WindowsFirewall.Status.Blocked -> Color(0xFFDC2626) to "Windows Firewall BreakPoint'ni BLOKLAGAN — tinglovchilar ulana olmaydi"
        WindowsFirewall.Status.NoRule -> Color(0xFFF59E0B) to "Firewall qoidasi hali yo'q — tarmoqni yoqqaningizda Windows ruxsat so'raydi"
        WindowsFirewall.Status.Unknown -> Color(0xFF64748B) to "Firewall holatini aniqlab bo'lmadi"
    }

    Surface(
        shape = AppDesign.ComponentShape,
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(2.dp, color.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status == null) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = color)
                } else {
                    Icon(
                        when (status) {
                            WindowsFirewall.Status.Allowed -> Icons.Default.CheckCircle
                            WindowsFirewall.Status.Blocked -> Icons.Default.Error
                            else -> Icons.Default.Shield
                        },
                        contentDescription = null,
                        tint = color
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(text, color = color, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                TextButton(onClick = ::refresh, enabled = status != null && !isFixing) { Text("Qayta tekshirish") }
            }
            if (status != WindowsFirewall.Status.Allowed) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            isFixing = true
                            message = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { WindowsFirewall.fix() }
                                isFixing = false
                                message = result.fold(
                                    onSuccess = { "Ruxsat berildi. Tarmoqni to'xtatib, qayta yoqing." },
                                    onFailure = { it.message }
                                )
                                refresh()
                            }
                        },
                        enabled = !isFixing && status != null,
                        shape = AppDesign.ComponentShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Indigo)
                    ) {
                        if (isFixing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Shield, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Avtomatik tuzatish", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Windows administrator ruxsatini so'raydi (\"Ha\" ni bosing)",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            message?.let { Text(it, color = TextDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun HelpSection(title: String, steps: List<String>) {
    Column {
        Text(title, fontWeight = FontWeight.Black, color = TextDark, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        steps.forEachIndexed { i, step ->
            Row(Modifier.padding(vertical = 2.dp)) {
                Text("${i + 1}.", fontWeight = FontWeight.Bold, color = AppDesign.Indigo, modifier = Modifier.width(24.dp))
                Text(step, color = TextMuted)
            }
        }
    }
}

@Composable
private fun ExecutablePathRow() {
    val path = WindowsFirewall.executablePath ?: return
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Column {
        Text("Dastur fayli (\"Browse...\" da shuni tanlang):", fontWeight = FontWeight.Bold, color = TextDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer(Modifier.weight(1f)) {
                Text(path, fontFamily = FontFamily.Monospace, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(path))
                copied = true
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (copied) "Nusxalandi" else "Nusxalash")
            }
        }
    }
}
