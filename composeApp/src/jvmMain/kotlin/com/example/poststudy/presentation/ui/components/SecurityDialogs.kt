package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.theme.AppDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Danger = Color(0xFFEF4444)

/**
 * True while Ctrl+Alt+1+2 are held together. Feed every key event of the screen to [onEvent].
 */
class SecretChordDetector {
    private val pressed = mutableSetOf<Key>()

    fun onEvent(event: KeyEvent): Boolean {
        when (event.type) {
            KeyEventType.KeyDown -> pressed += event.key
            KeyEventType.KeyUp -> pressed -= event.key
        }
        val one = Key.One in pressed || Key.NumPad1 in pressed
        val two = Key.Two in pressed || Key.NumPad2 in pressed
        return event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.isAltPressed && one && two
    }
}

/** Masked input with an eye toggle, used for the secret key and the admin password. */
@Composable
fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Key,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Yashirish" else "Ko'rsatish"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        enabled = enabled,
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        shape = AppDesign.ComponentShape
    )
}

/** Asks for the secret key; [onAccepted] gets the key and runs only when it is right. */
@Composable
fun SecretKeyDialog(
    title: String,
    text: String,
    confirmText: String = "Tasdiqlash",
    onDismiss: () -> Unit,
    onAccepted: (key: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = text,
        confirmText = if (isChecking) "Tekshirilmoqda..." else confirmText,
        onConfirm = {
            if (!isChecking && key.isNotBlank()) {
                isChecking = true
                val typed = key
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { AppContainer.localRepository.isSecretKey(typed) }
                    if (ok) {
                        onAccepted(typed)
                    } else {
                        // Slow down guessing
                        delay(800)
                        error = "Maxfiy kalit noto'g'ri"
                        key = ""
                    }
                    isChecking = false
                }
            }
        },
        content = {
            Column {
                SecretField(
                    value = key,
                    onValueChange = { key = it; error = null },
                    label = "Maxfiy kalit",
                    isError = error != null,
                    enabled = !isChecking,
                    modifier = Modifier.focusRequester(focusRequester)
                )
                error?.let { Text(it, color = Danger, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    )
}

/**
 * Three confirmations before a serious action: warning, admin password, then typing [confirmWord].
 */
@Composable
fun ThreeStepVerificationDialog(
    title: String,
    warning: String,
    confirmWord: String,
    finalButtonText: String,
    color: Color,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }
    var password by remember { mutableStateOf("") }
    var typedWord by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    val next: () -> Unit = {
        when (step) {
            1 -> { step = 2 }
            2 -> if (!isChecking && password.isNotEmpty()) {
                isChecking = true
                val typed = password
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        AppContainer.localRepository.validateUserPassword(typed).first()
                    }
                    isChecking = false
                    if (ok) {
                        error = null
                        step = 3
                    } else {
                        error = "Parol noto'g'ri"
                        password = ""
                    }
                }
            }
            3 -> if (typedWord.trim() == confirmWord) {
                onConfirmed()
            } else {
                error = "\"$confirmWord\" so'zini aynan shunday yozing"
            }
        }
    }

    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = when (step) {
            1 -> warning
            2 -> "Davom etish uchun admin parolini kiriting."
            else -> "Tasdiqlash uchun quyidagi maydonga \"$confirmWord\" deb yozing."
        },
        confirmText = when {
            step < 3 && isChecking -> "Tekshirilmoqda..."
            step < 3 -> "Davom etish"
            else -> finalButtonText
        },
        confirmColor = color,
        onConfirm = next,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StepIndicator(step, color)
                Spacer(Modifier.height(16.dp))
                when (step) {
                    1 -> Row(
                        Modifier
                            .fillMaxWidth()
                            .background(color.copy(alpha = 0.08f), AppDesign.ComponentShape)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = color)
                        Spacer(Modifier.width(8.dp))
                        Text("Bu amalni bajarish uchun 3 ta tasdiq kerak.", color = color, fontWeight = FontWeight.Bold)
                    }
                    2 -> SecretField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = "Admin paroli",
                        isError = error != null,
                        enabled = !isChecking,
                        icon = Icons.Default.Lock
                    )
                    else -> OutlinedTextField(
                        value = typedWord,
                        onValueChange = { typedWord = it; error = null },
                        label = { Text(confirmWord) },
                        singleLine = true,
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppDesign.ComponentShape
                    )
                }
                error?.let {
                    Text(it, color = Danger, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    )
}

@Composable
private fun StepIndicator(step: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..3).forEach { i ->
            val done = i <= step
            Surface(
                shape = CircleShape,
                color = if (done) color else Color.White,
                border = BorderStroke(2.dp, if (done) color else Color(0xFFCBD5E1)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$i", color = if (done) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Black)
                }
            }
            if (i < 3) {
                Box(Modifier.width(32.dp).height(2.dp).background(if (i < step) color else Color(0xFFCBD5E1)))
            }
        }
    }
    Text("$step / 3-qadam", color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
}
