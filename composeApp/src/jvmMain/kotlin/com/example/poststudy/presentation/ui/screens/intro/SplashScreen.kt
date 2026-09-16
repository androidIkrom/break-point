package com.example.poststudy.presentation.ui.screens.intro

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.hoverEffect
import com.example.poststudy.presentation.ui.components.AdaptiveGrid
import com.example.poststudy.presentation.ui.components.ScrollableCentered
import com.example.poststudy.presentation.ui.components.InstituteLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onContinue: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
            .focusRequester(focusRequester)
            .focusable() // Ensure it can catch Enter key
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    onContinue()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        ScrollableCentered(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
            Column(
                modifier = Modifier.widthIn(max = 1100.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + expandVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    InstituteLogo(size = 200.dp)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "BreakPoint",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF064E3B)
                    )
                    
                    Text(
                        text = "Bilimlarni mustahkamlash va baholash tizimi",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                AdaptiveGrid(
                    items = listOf(
                        Triple(Icons.Default.Person, "Admin - pedagoglar uchun", "Dars materiallarini yuklang, testlar yarating va tinglovchilar natijalarini real vaqtda kuzating."),
                        Triple(Icons.Default.Face, "Tinglovchilar uchun", "Taqdimotlarni ko'rib chiqing, bilimlaringizni sinab ko'ring va natijalaringizni darhol bilib oling."),
                        Triple(Icons.Default.CloudSync, "Tarmoq", "Lokal tarmoq orqali pedagoglar va tinglovchilar o'rtasida ma'lumotlarni oson almashing.")
                    ),
                    minItemWidth = 240.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { (icon, title, description), itemModifier ->
                    InstructionCard(
                        modifier = itemModifier.hoverEffect(),
                        icon = icon,
                        title = title,
                        description = description
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { it }
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.width(300.dp).height(64.dp).hoverEffect(),
                    shape = AppDesign.ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(
                        "Boshlash",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
    }
}

@Composable
fun InstructionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = modifier,
        shape = AppDesign.CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xFFF0FDF4),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF10B981))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
            )
        }
    }
}
