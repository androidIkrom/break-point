package com.example.poststudy.presentation.ui.screens.admin

import com.example.poststudy.presentation.ui.components.ScrollableCentered
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.presentation.ui.components.SecretChordDetector
import com.example.poststudy.presentation.ui.components.SecretKeyDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown instead of the admin area until this computer is unlocked. The key dialog opens only
 * with Ctrl+Alt+1+2; the screen itself does not hint at it.
 */
@Composable
fun AdminLockedScreen(onUnlocked: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val chord = remember { SecretChordDetector() }
    var showKeyDialog by remember { mutableStateOf(false) }
    val danger = Color(0xFFDC2626)

    LaunchedEffect(showKeyDialog) {
        // Get keyboard focus back after the dialog closes
        if (!showKeyDialog) focusRequester.requestFocus()
    }

    if (showKeyDialog) {
        SecretKeyDialog(
            title = "Admin rejimini yoqish",
            text = "Maxfiy kalitni kiriting.",
            confirmText = "Yoqish",
            onDismiss = { showKeyDialog = false },
            onAccepted = { key ->
                scope.launch {
                    withContext(Dispatchers.IO) { AppContainer.localRepository.unlockAdmin(key) }
                    showKeyDialog = false
                    onUnlocked()
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (chord.onEvent(it)) {
                    showKeyDialog = true
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Card(
            modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
            shape = AppDesign.CardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            border = BorderStroke(3.dp, danger.copy(alpha = 0.35f))
        ) {
            Column(Modifier.padding(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(140.dp).background(danger.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GppBad, contentDescription = null, tint = danger, modifier = Modifier.size(84.dp))
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    "Siz admin emassiz",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = danger
                )
                Text(
                    "Bu kompyuterda admin rejimi yoqilmagan. Dasturdan faqat tinglovchi sifatida foydalanishingiz mumkin.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Spacer(Modifier.height(36.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.width(320.dp).height(60.dp),
                    shape = AppDesign.ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Emerald)
                ) {
                    Text("Tinglovchi sifatida davom etish", fontWeight = FontWeight.Black)
                }
            }
        }
        }
        // Drawn last so the scrolling content does not cover it
        BackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(top = 12.dp))
    }
}
