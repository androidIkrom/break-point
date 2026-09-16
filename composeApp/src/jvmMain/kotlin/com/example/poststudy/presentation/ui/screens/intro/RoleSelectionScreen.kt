package com.example.poststudy.presentation.ui.screens.intro

import androidx.compose.material3.Text
import com.example.poststudy.presentation.ui.components.ScrollableCentered
import com.example.poststudy.presentation.ui.components.AdaptiveGrid
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.domain.model.UserRole
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.hoverEffect
import com.example.poststudy.presentation.ui.components.InstituteLogo

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit,
    onJoinNetwork: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(700.dp)
                .offset(x = (-250).dp, y = (-250).dp)
                .background(Color(0xFF10B981).copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 200.dp, y = 200.dp)
                .background(Color(0xFF3B82F6).copy(alpha = 0.05f), CircleShape)
        )

        ScrollableCentered(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth().padding(16.dp),
            shape = AppDesign.CardShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, Color(0xFF10B981).copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InstituteLogo(size = 140.dp, modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = "BreakPoint",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF059669) // Emerald 600
                )

                Text(
                    text = "O'rganishni boshlash uchun rolingizni tanlang",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                AdaptiveGrid(
                    items = listOf(
                        RoleItem("Admin", "Boshqaruv paneli", Icons.Default.Person, Color(0xFF3B82F6)) { onRoleSelected(UserRole.Teacher) },
                        RoleItem("Tinglovchi", "Lokal darslar", Icons.Default.School, Color(0xFF10B981)) { onRoleSelected(UserRole.Student) },
                        RoleItem("Tarmoq", "Adminga ulanish", Icons.Default.Lan, Color(0xFF6366F1), onJoinNetwork)
                    ),
                    minItemWidth = 190.dp,
                    spacing = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { item, itemModifier ->
                    RoleSelectionCard(item.title, item.subtitle, item.icon, item.color, itemModifier, item.onClick)
                }
            }
        }
        }
    }
}

private class RoleItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun RoleSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 230.dp).hoverEffect(),
        shape = AppDesign.ComponentShape,
        color = Color.White,
        border = BorderStroke(3.dp, color),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
