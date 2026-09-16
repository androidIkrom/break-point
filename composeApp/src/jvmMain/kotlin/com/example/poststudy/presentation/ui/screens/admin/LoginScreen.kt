package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.SecretKeyDialog
import com.example.poststudy.presentation.ui.components.ScrollableCentered
import com.example.poststudy.domain.model.AdminUsername

@OptIn(ExperimentalMaterial3Api::class)
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
        // Scaffold places its content a frame later, so retry until the field is attached
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        // Subtle decorative circles
        Box(
            modifier = Modifier
                .size(600.dp)
                .offset(x = (-200).dp, y = (-200).dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                        handleAction()
                        true
                    } else if (it.isCtrlPressed && it.isAltPressed && it.key == Key.One && it.type == KeyEventType.KeyDown) {
                        // Resetting the admin account needs the secret key
                        showResetDialog = true
                        true
                    } else false
                },
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            ScrollableCentered(Modifier.fillMaxSize().padding(paddingValues)) {
                Card(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(16.dp),
                    shape = AppDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(3.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(48.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRegistered) "Xush kelibsiz" else "Hisob yaratish",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF065F46)
                        )

                        Text(
                            text = if (isRegistered) "Davom etish uchun tizimga kiring" else "Admin profilini sozlang",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                        )

                        if (errorMessage.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = AppDesign.ComponentShape,
                                border = BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = Color(0xFF991B1B),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.trim(); errorMessage = "" },
                            label = { Text("Login") },
                            modifier = Modifier.fillMaxWidth().focusRequester(firstField),
                            shape = AppDesign.ComponentShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                focusedLabelColor = Color(0xFF6366F1)
                            )
                        )

                        if (!isRegistered) {
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = AdminUsername.filterTyping(it); errorMessage = "" },
                                label = { Text("Username") },
                                supportingText = {
                                    Text("Tinglovchilar adminlar ro'yxatida shu nomni ko'radi: bitta so'z, ${AdminUsername.MAX_LENGTH} ta belgigacha")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppDesign.ComponentShape,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    focusedLabelColor = Color(0xFF6366F1)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = "" },
                            label = { Text("Parol") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppDesign.ComponentShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                focusedLabelColor = Color(0xFF6366F1)
                            )
                        )

                        if (!isRegistered) {
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = "" },
                                label = { Text("Parolni tasdiqlang") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppDesign.ComponentShape,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    focusedLabelColor = Color(0xFF6366F1)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = { handleAction() },
                            modifier = Modifier.fillMaxWidth().height(68.dp),
                            shape = AppDesign.ComponentShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                        ) {
                            Text(
                                text = if (isRegistered) "KIRISH" else "RO'YXATDAN O'TISH",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
