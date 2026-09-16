package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box

/** Top bar back button that stays readable on both light and dark screen backgrounds. */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF065F46)
) {
    Surface(
        onClick = onClick,
        modifier = modifier.padding(start = 8.dp).size(44.dp).hoverEffect(scale = 1.1f, yOffset = 0f),
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(2.dp, tint.copy(alpha = 0.35f)),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Orqaga",
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
