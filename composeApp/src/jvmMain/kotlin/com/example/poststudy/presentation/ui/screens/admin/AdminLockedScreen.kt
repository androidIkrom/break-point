package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown when someone picks "Admin" on a computer where admin mode was never unlocked.
 * The secret chord (Ctrl+Alt+1+2) opens the key dialog; everything else sends them back.
 */
@Composable
fun AdminLockedScreen(onUnlocked: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val chord = remember { SecretChordDetector() }
    var showKeyDialog by remember { mutableStateOf(false) }
    val parallax = rememberParallax()

    LaunchedEffect(showKeyDialog) {
        // Get keyboard focus back after the dialog closes
        if (!showKeyDialog) runCatching { focusRequester.requestFocus() }
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
            .parallaxSource(parallax)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (chord.onEvent(it)) {
                    showKeyDialog = true
                    true
                } else false
            }
    ) {
        SignalBackground(parallax = parallax.value)
        Box(Modifier.align(Alignment.TopStart).padding(12.dp)) { BackButton(onClick = onBack, onDark = true) }

        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            GlassCard(Modifier.widthIn(max = 560.dp).fillMaxWidth(), padding = 40.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(88.dp).background(Signal.Danger.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.GppBad, contentDescription = null, tint = Signal.DangerOnDark, modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Siz admin emassiz", style = MaterialTheme.typography.headlineLarge, color = Signal.OnDark, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bu kompyuterda admin rejimi yoqilmagan. Dasturdan faqat tinglovchi sifatida foydalanishingiz mumkin.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Signal.OnDarkMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    SignalButton("Tinglovchi sifatida davom etish", onClick = onBack, style = ButtonStyle.Cyan, height = 48.dp, contentPadding = PaddingValues(horizontal = 28.dp))
                }
            }
        }
    }
}
