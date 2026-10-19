package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.Signal

/** Top bar back button; [onDark] gives the glass look for hero screens. */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Signal.Ink,
    onDark: Boolean = false
) {
    SignalIconButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Orqaga",
        onClick = onClick,
        modifier = modifier.padding(start = 8.dp),
        tint = if (onDark) Signal.OnDark else tint,
        container = if (onDark) Signal.GlassFill else Signal.Card,
        borderColor = if (onDark) Signal.GlassBorderStrong else Signal.OutlineStrong,
        size = 40.dp,
        iconSize = 18.dp,
        onDark = onDark
    )
}
