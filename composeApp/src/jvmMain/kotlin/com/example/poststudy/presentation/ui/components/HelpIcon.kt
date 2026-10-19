package com.example.poststudy.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.Signal

@Composable
fun HelpIcon(
    title: String,
    helpText: String,
    modifier: Modifier = Modifier,
    tint: Color = Signal.Muted,
    onDark: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }

    SignalIconButton(
        icon = Icons.AutoMirrored.Outlined.HelpOutline,
        contentDescription = "Yordam",
        onClick = { showDialog = true },
        modifier = modifier,
        tint = if (onDark) Signal.OnDark else tint,
        container = if (onDark) Signal.GlassFill else Signal.Card,
        borderColor = if (onDark) Signal.GlassBorderStrong else Signal.OutlineStrong,
        size = 40.dp,
        iconSize = 20.dp,
        onDark = onDark
    )

    if (showDialog) {
        PostStudyDialog(
            onDismissRequest = { showDialog = false },
            title = title,
            text = helpText,
            confirmText = "Tushunarli",
            showDismissButton = false,
            onConfirm = { showDialog = false }
        )
    }
}
