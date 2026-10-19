package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.local.DatabaseHelper
import com.example.poststudy.data.local.DemoData
import com.example.poststudy.data.system.AppLog
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Help page inside the admin area. */
@Composable
fun ReadmeScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val scope = rememberCoroutineScope()
    var demoPresent by remember { mutableStateOf(false) }
    var showRemoveDemo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { demoPresent = withContext(Dispatchers.IO) { DemoData.isPresent() } }
    if (showRemoveDemo) {
        PostStudyDialog(
            onDismissRequest = { showRemoveDemo = false },
            title = "Namuna ma'lumotlarni o'chirish",
            text = "Dastur bilan kelgan namuna darslar, imtihonlar, 'Guruh 2', namuna tinglovchilar va ularning natijalari o'chiriladi. Siz qo'shgan ma'lumotlar qoladi.",
            confirmText = "O'chirish",
            confirmColor = Signal.Danger,
            onConfirm = {
                showRemoveDemo = false
                scope.launch {
                    withContext(Dispatchers.IO) { DemoData.removeAll() }
                    demoPresent = false
                }
            }
        )
    }

    AdminPage(
        title = "BreakPoint-dan foydalanish bo'yicha qo'llanma",
        eyebrow = "Yordam",
        onBack = onBack
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent {
                    if ((it.key == Key.Enter || it.key == Key.DirectionRight) && it.type == KeyEventType.KeyDown) {
                        onNext()
                        true
                    } else false
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.page, vertical = Space.xl),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GuideStep(1, "Bu qanday dastur?", "BreakPoint o'quv materiallarini o'zlashtirishga yordam beradi. Taqdimotingizni yuklang, taymer asosida o'rganing va so'ngra bilimingizni darhol sinab ko'ring.")
            GuideStep(2, "Sozlash bosqichi", "PowerPoint (.pptx) va Word (.docx) fayllarini tanlang. Slayd taymerini va test taymerini o'rnating. Tayyor bo'lgach, 'TAYYORMAN' tugmasini bosing.")
            GuideStep(3, "Formatlash qoidalari", "Testlar uchun ko'p variantli formatdan foydalaning. To'g'ri javobni yulduzcha (*) bilan belgilang.\nMasalan: a) javob*")
            SignalCard(padding = 20.dp) {
                Text("Klaviatura", style = MaterialTheme.typography.titleLarge, color = Signal.Ink)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Enter — tasdiqlash yoki davom etish, Esc — orqaga",
                    "Testda: 1 · 2 · 3 · 4 — javob, ← → — savollar",
                    "Slaydlarda: ← → — slaydlar, Enter — yakunlash"
                ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Signal.Muted) }
            }
            if (demoPresent) {
                SignalCard(padding = 20.dp) {
                    Text("Namuna ma'lumotlar", style = MaterialTheme.typography.titleLarge, color = Signal.Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Dastur birinchi ochilganda 5 ta namuna dars, 2 ta imtihon, 'Guruh 2' (parol: 1234), tinglovchilar va natijalar qo'shilgan. Ularni istalgancha tahrirlang yoki o'chiring; haqiqiy ish boshlanganda bir tugma bilan hammasini olib tashlang.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Signal.Muted
                    )
                    Spacer(Modifier.height(10.dp))
                    SignalButton("Namuna ma'lumotlarni o'chirish", onClick = { showRemoveDemo = true }, style = ButtonStyle.Danger, height = 38.dp)
                }
            }
            SignalCard(padding = 20.dp) {
                Text("Ma'lumotlar va loglar", style = MaterialTheme.typography.titleLarge, color = Signal.Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Baza, materiallar va kunlik zaxira nusxalar: ${DatabaseHelper.dataFolder.path}\nLog fayli: ${AppLog.file.path}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Signal.Muted
                )
                Spacer(Modifier.height(10.dp))
                SignalButton(
                    "Papkani ochish",
                    onClick = { runCatching { java.awt.Desktop.getDesktop().open(DatabaseHelper.dataFolder) } },
                    style = ButtonStyle.Tonal,
                    height = 38.dp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SignalButton("Bosh sahifaga", onClick = onNext, style = ButtonStyle.Primary, height = 44.dp)
            }
        }
    }
}
