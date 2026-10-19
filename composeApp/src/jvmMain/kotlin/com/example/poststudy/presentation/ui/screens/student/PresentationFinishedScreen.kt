package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.ui.components.*

/** End of a presentation-only session: there is no test, so nothing is scored or sent. */
@Composable
fun PresentationFinishedScreen(title: String, studentName: String, onFinish: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val parallax = rememberParallax()
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .parallaxSource(parallax)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    onFinish(); true
                } else false
            }
    ) {
        SignalBackground(parallax = parallax.value)
        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            GlassCard(Modifier.widthIn(max = 560.dp).fillMaxWidth(), padding = 40.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(96.dp).background(Signal.Success.copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Signal.SuccessOnDark, modifier = Modifier.size(52.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Taqdimot yakunlandi", style = MaterialTheme.typography.headlineLarge, color = Signal.OnDark, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listOf(studentName, title).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Signal.OnDarkMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    SignalButton("Tayyor", onClick = onFinish, style = ButtonStyle.Cyan, height = 48.dp, contentPadding = PaddingValues(horizontal = 40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Enter — tayyor", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkFaint)
                }
            }
        }
    }
}
