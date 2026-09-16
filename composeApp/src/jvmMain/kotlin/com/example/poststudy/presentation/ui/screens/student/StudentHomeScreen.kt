package com.example.poststudy.presentation.ui.screens.student

import com.example.poststudy.presentation.ui.components.ScrollableCentered
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.hoverEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onNavigateToPreparation: () -> Unit,
    onNavigateToTest: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(600.dp)
                .offset(x = (-200).dp, y = (-200).dp)
                .background(Color(0xFF10B981).copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 150.dp)
                .background(Color(0xFF3B82F6).copy(alpha = 0.05f), CircleShape)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Tinglovchi Paneli",
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Black
                        )
                    },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            ScrollableCentered(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Xush kelibsiz, tinglovchi",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Bugun qaysi mavzuda bilimingizni boyitamiz?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 64.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 1000.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    StudentHomeCard(
                        modifier = Modifier.weight(1f),
                        title = "Tayyorgarlik",
                        subtitle = "Darsliklarni o'rganish",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        color = Color(0xFFF59E0B), // Amber 500
                        onClick = onNavigateToPreparation
                    )
                    StudentHomeCard(
                        modifier = Modifier.weight(1f),
                        title = "Bilim testi",
                        subtitle = "O'zingizni sinab ko'ring",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        color = Color(0xFF10B981), // Emerald 500
                        onClick = onNavigateToTest
                    )
                }
            }
        }
    }
}

@Composable
fun StudentHomeCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 200.dp)
            .hoverEffect(),
        shape = AppDesign.ComponentShape,
        color = Color.White,
        border = BorderStroke(3.dp, color),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
