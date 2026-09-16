package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.hoverEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherIntroScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(Color(0xFF6366F1).copy(alpha = 0.05f), CircleShape)
        )

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if ((it.key == Key.Enter || it.key == Key.DirectionRight) && it.type == KeyEventType.KeyDown) {
                        onNext()
                        true
                    } else false
                },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Admin uchun qo'llanma",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46)
                        ) 
                    },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                TeacherInfoCard(
                    modifier = Modifier.hoverEffect(scale = 1.02f, yOffset = -5f),
                    title = "1. Taqdimot va testlarni tanlash",
                    content = "Dars yaratish jarayonida 'Taqdimot fayli' uchun (.pptx) va 'Test fayli' uchun (.docx) formatidagi hujjatlarni yuklang. Shuningdek, tinglovchilar har bir slaydni va testni ko'rishlari uchun vaqt limitlarini belgilang."
                )

                TeacherInfoCard(
                    modifier = Modifier.hoverEffect(scale = 1.02f, yOffset = -5f),
                    title = "2. Darsni faollashtirish",
                    content = "Darsni yaratganingizdan so'ng, 'Saqlangan darslar' ro'yxatidan kerakli darsni toping va 'Boshlash' tugmasini bosing. Bu darsni tinglovchilar kirishi uchun asosiy dars sifatida sozlaydi."
                )

                TeacherInfoCard(
                    modifier = Modifier.hoverEffect(scale = 1.02f, yOffset = -5f),
                    title = "3. Word test formati (muhim!)",
                    content = "Test fayli quyidagi formatda bo'lishi shart:\n\n1. Savol matni\na) Birinchi variant\nb) Ikkinchi variant*\nc) Uchinchi variant\nd) To'rtinchi variant\n\nTo'g'ri javobning oxiriga yulduzcha (*) belgisini qo'ying. Har bir savoldan keyin bitta bo'sh qator tashlang."
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Misol:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "O'zbekiston poytaxti qaysi shahar?\na) Samarqand\nb) Toshkent*\nc) Buxoro\nd) Xiva",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475569),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier.width(300.dp).height(64.dp).hoverEffect(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text("Tushunarli", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun TeacherInfoCard(modifier: Modifier = Modifier, title: String, content: String) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B)
            )
        }
    }
}
