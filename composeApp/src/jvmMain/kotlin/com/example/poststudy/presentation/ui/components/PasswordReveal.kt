package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Teacher-side view of a group password: hidden by default, eye button reveals it. */
@Composable
fun PasswordReveal(
    password: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    if (password.isNullOrEmpty()) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Parol yo'q", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, style = style)
        }
        return
    }
    var visible by remember(password) { mutableStateOf(false) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (visible) password else "•".repeat(password.length.coerceIn(4, 12)),
            style = style,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        IconButton(onClick = { visible = !visible }, modifier = Modifier.size(32.dp)) {
            Icon(
                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (visible) "Yashirish" else "Ko'rsatish",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
