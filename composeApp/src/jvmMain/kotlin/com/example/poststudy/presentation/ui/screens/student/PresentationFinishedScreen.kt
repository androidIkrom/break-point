package com.example.poststudy.presentation.ui.screens.student

import com.example.poststudy.presentation.ui.components.ScrollableCentered
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.AppDesign

/** End of a presentation-only session: there is no test, so nothing is scored or sent. */
@Composable
fun PresentationFinishedScreen(title: String, studentName: String, onFinish: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    onFinish(); true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Card(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            shape = AppDesign.CardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            border = BorderStroke(3.dp, AppDesign.Emerald.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(110.dp).background(AppDesign.Emerald.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = AppDesign.Emerald, modifier = Modifier.size(60.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Taqdimot yakunlandi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF065F46)
                )
                Text(
                    text = listOf(studentName, title).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = onFinish,
                    modifier = Modifier.width(320.dp).height(64.dp),
                    shape = AppDesign.ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Emerald)
                ) {
                    Text("TAYYOR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
            }
        }
        }
    }
}
