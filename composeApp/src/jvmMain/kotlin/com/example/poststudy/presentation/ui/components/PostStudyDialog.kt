package com.example.poststudy.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal

/**
 * App dialog. Enters with a 0.94 → 1 scale and fade in 200 ms. Enter confirms, Escape dismisses.
 * [confirmColor] picks the confirm button style: the danger red gives a red button.
 */
@Composable
fun PostStudyDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String = "Tasdiqlash",
    dismissText: String = "Bekor qilish",
    confirmColor: Color = Signal.Primary,
    onConfirm: () -> Unit,
    showDismissButton: Boolean = true,
    content: @Composable (() -> Unit)? = null
) {
    val reduce = LocalReduceMotion.current
    val appear = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) appear.animateTo(1f, tween(200, easing = AppMotion.decelerateEasing))
    }
    // Plain confirm dialogs take keyboard focus so Enter/Esc work at once; dialogs with
    // their own fields leave focus to the field.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (content == null) runCatching { focusRequester.requestFocus() }
    }
    val danger = confirmColor == Signal.Danger || confirmColor == AppDesign.Rose
    val warning = confirmColor == Signal.Warning || confirmColor == AppDesign.Amber

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .graphicsLayer {
                    val s = 0.94f + 0.06f * appear.value
                    scaleX = s; scaleY = s; alpha = appear.value
                    translationY = (1f - appear.value) * 8.dp.toPx()
                }
                .shadow(24.dp, AppDesign.CardShape, ambientColor = Signal.Dark, spotColor = Signal.Dark)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.Enter -> { onConfirm(); true }
                            Key.Escape -> { onDismissRequest(); true }
                            else -> false
                        }
                    } else false
                },
            shape = AppDesign.CardShape,
            color = Signal.Card,
            border = BorderStroke(1.dp, Signal.Outline)
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Signal.Ink)
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Signal.Muted, textAlign = TextAlign.Start)

                if (content != null) {
                    Spacer(Modifier.height(20.dp))
                    content()
                }

                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    if (showDismissButton) {
                        SignalButton(dismissText, onClick = onDismissRequest, style = ButtonStyle.Outlined, height = 42.dp)
                    }
                    SignalButton(
                        confirmText,
                        onClick = onConfirm,
                        style = when { danger -> ButtonStyle.Danger; else -> ButtonStyle.Primary },
                        height = 42.dp
                    )
                }
            }
        }
    }
}
