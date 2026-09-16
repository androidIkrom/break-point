package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.poststudy.presentation.theme.AppDesign

@Composable
fun PostStudyDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String = "Tasdiqlash",
    dismissText: String = "Bekor qilish",
    confirmColor: Color = Color(0xFF10B981), // Default Emerald
    onConfirm: () -> Unit,
    showDismissButton: Boolean = true,
    content: @Composable (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.Enter -> {
                                onConfirm()
                                true
                            }
                            Key.Escape -> {
                                onDismissRequest()
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            shape = AppDesign.CardShape,
            color = Color.White,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981).copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF065F46)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (content != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    content()
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showDismissButton) OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = AppDesign.ComponentShape,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(dismissText, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = AppDesign.ComponentShape,
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(confirmText, color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
