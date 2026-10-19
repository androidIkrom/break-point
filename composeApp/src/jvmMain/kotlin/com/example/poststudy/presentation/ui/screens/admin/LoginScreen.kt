package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.AdminUsername
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var isRegistered by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    // Keyboard shortcuts only reach the screen when something in it has focus
    val firstField = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        AppContainer.localRepository.isUserRegistered().collect {
            isRegistered = it
            loaded = true
        }
    }
    LaunchedEffect(loaded, isRegistered) {
        if (!loaded) return@LaunchedEffect
        // The field is attached a frame later, so retry until focus sticks
        repeat(20) {
            withFrameNanos { }
            if (runCatching { firstField.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        SecretKeyDialog(
            title = "Admin parolini tiklash",
            text = "Joriy admin hisobi o'chiriladi va yangisini yaratasiz. Fanlar va darslar saqlanib qoladi. Davom etish uchun maxfiy kalitni kiriting.",
            confirmText = "Tiklash",
            onDismiss = { showResetDialog = false },
            onAccepted = {
                scope.launch {
                    withContext(Dispatchers.IO) { AppContainer.localRepository.clearAllUsers() }
                    showResetDialog = false
                    isRegistered = false
                    username = ""
                    password = ""
                    confirmPassword = ""
                    errorMessage = "Parol tiklash rejimi: yangi hisob yarating"
                }
            }
        )
    }

    val handleAction: () -> Unit = {
        if (isChecking) {
            // Ignore repeated Enter presses while the check runs
        } else if (username.isBlank() || password.isBlank()) {
            errorMessage = "Login va parolni kiriting"
        } else if (!isRegistered && AdminUsername.validate(fullName) != null) {
            errorMessage = AdminUsername.validate(fullName)!!
        } else if (!isRegistered && password != confirmPassword) {
            errorMessage = "Parollar mos kelmadi"
        } else {
            isChecking = true
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    if (!isRegistered) {
                        AppContainer.localRepository.registerUser(username.trim(), password, fullName.trim())
                        true
                    } else {
                        AppContainer.localRepository.validateUser(username, password).first()
                    }
                }
                isChecking = false
                if (success) onLoginSuccess() else errorMessage = "Login yoki parol noto'g'ri"
            }
        }
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(Signal.Surface)
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    handleAction()
                    true
                } else if (it.isCtrlPressed && it.isAltPressed && it.key == Key.One && it.type == KeyEventType.KeyDown) {
                    // Resetting the admin account needs the secret key
                    showResetDialog = true
                    true
                } else false
            }
    ) {
        // Left: dark panel with the signal backdrop
        Box(Modifier.weight(1f).fillMaxHeight()) {
            SignalBackground()
            Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SignalButton("Rol tanlash", onClick = onBack, style = ButtonStyle.Glass, icon = Icons.AutoMirrored.Filled.ArrowBack, height = 36.dp, contentPadding = PaddingValues(horizontal = 12.dp))
                Spacer(Modifier.weight(1f))
                InstituteLogo(size = 72.dp, glow = true)
                Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("Admin rejimi", color = Signal.Amber)
                    Text("Mashg'ulotlarni boshqaring, natijalarni jonli kuzating", style = MaterialTheme.typography.displayMedium, color = Signal.OnDark)
                    Text("Bu kompyuter maxfiy kalit bilan ochilgan. Admin hisobi faqat shu kompyuterda saqlanadi.", style = MaterialTheme.typography.bodyLarge, color = Signal.OnDarkMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Fan va darslarni yarating", "Guruhlarga biriktiring", "Tarmoqni yoqing, natijalar keladi").forEachIndexed { i, tip ->
                        GlassCard(Modifier.weight(1f), padding = 14.dp) {
                            Text("${i + 1}", style = MaterialTheme.typography.headlineSmall, color = Signal.Cyan)
                            Spacer(Modifier.height(4.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkMuted)
                        }
                    }
                }
            }
        }

        // Right: the form
        Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = Space.page, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isRegistered) "Xush kelibsiz" else "Hisob yaratish", style = MaterialTheme.typography.displaySmall, color = Signal.Ink)
                    Text(
                        if (isRegistered) "Davom etish uchun tizimga kiring." else "Bu kompyuterda admin hisobi hali yo'q. Login va parolni o'zingiz tanlang.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Signal.Muted
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Surface(shape = AppDesign.ComponentShape, color = Signal.DangerContainer, border = BorderStroke(1.dp, Signal.DangerBorder)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Signal.Danger, modifier = Modifier.size(18.dp))
                            Text(errorMessage, color = Signal.OnDangerContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                FormField("Login") {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.trim(); errorMessage = "" },
                        modifier = Modifier.fillMaxWidth().focusRequester(firstField),
                        shape = AppDesign.SmallShape,
                        singleLine = true
                    )
                }

                if (!isRegistered) {
                    FormField("Username", hint = "Tinglovchilar adminlar ro'yxatida shu nomni ko'radi: bitta so'z, ${AdminUsername.MAX_LENGTH} ta belgigacha") {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = AdminUsername.filterTyping(it); errorMessage = "" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppDesign.SmallShape,
                            singleLine = true
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FormField("Parol", modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = "" },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppDesign.SmallShape,
                            singleLine = true
                        )
                    }
                    if (!isRegistered) {
                        FormField("Parolni tasdiqlang", modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = "" },
                                visualTransformation = PasswordVisualTransformation(),
                                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppDesign.SmallShape,
                                singleLine = true
                            )
                        }
                    }
                }

                SignalButton(
                    text = when { isChecking -> "Tekshirilmoqda..."; isRegistered -> "Kirish"; else -> "Ro'yxatdan o'tish" },
                    onClick = handleAction,
                    enabled = !isChecking,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Gradient,
                    height = 50.dp
                )
                Text("Enter — yuborish · Esc — orqaga", style = MaterialTheme.typography.bodySmall, color = Signal.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

/** Label above a field, with an optional hint under it. */
@Composable
fun FormField(label: String, modifier: Modifier = Modifier, hint: String? = null, field: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Signal.Ink)
        field()
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
    }
}
