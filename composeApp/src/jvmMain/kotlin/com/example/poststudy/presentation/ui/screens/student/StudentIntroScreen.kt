package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.domain.model.LessonMode
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.hoverEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentIntroScreen(
    title: String,
    totalSlides: Int,
    totalQuestions: Int,
    slideTimerSeconds: Int,
    testTimerSeconds: Int,
    mode: LessonMode,
    isExam: Boolean,
    groupName: String,
    onStart: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if ((it.key == Key.Enter || it.key == Key.DirectionRight) && it.type == KeyEventType.KeyDown) {
                        onStart()
                        true
                    } else false
                },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            title.ifBlank { if (isExam) "Imtihon brifingi" else "Mashg'ulot brifingi" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF065F46)
                        )
                    },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = Color(0xFF10B981))
                        }
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

                Card(
                    modifier = Modifier.widthIn(max = 650.dp).padding(bottom = 32.dp).hoverEffect(),
                    shape = AppDesign.CardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    border = BorderStroke(2.dp, Color(0xFF4F46E5).copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFF4F46E5).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (groupName.isNotBlank()) {
                            Text(
                                text = "$groupName • ${if (isExam) "Imtihon" else mode.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        Text(
                            text = when {
                                isExam -> "Imtihonga tayyorlaning. Bu sizning bilimingizni bevosita tekshirishdir."
                                mode == LessonMode.ReAppropriation ->
                                    "Mashg'ulotga tayyorlaning. Avval taqdimotni o'rganasiz, so'ngra test topshirasiz."
                                mode == LessonMode.PresentationOnly ->
                                    "Taqdimotni diqqat bilan o'rganing. Bu mashg'ulotda test yo'q."
                                else -> "Test topshirishga tayyorlaning."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            if (mode.hasSlides) {
                                IntroRow(
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    label = "O'rganish bosqichi:",
                                    value = "$totalSlides slayd",
                                    subValue = "${slideTimerSeconds / 60} daqiqa",
                                    color = Color(0xFFFACC15)
                                )
                            }

                            if (mode.hasTest) IntroRow(
                                icon = Icons.Default.Quiz,
                                label = "Test bosqichi:",
                                value = "$totalQuestions savol",
                                subValue = "${testTimerSeconds / 60} daqiqa",
                                color = Color(0xFF4ADE80)
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .hoverEffect(),
                            shape = AppDesign.ComponentShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text("BOSHLASH", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun IntroRow(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    color: Color
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = AppDesign.ComponentShape,
        border = BorderStroke(2.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "• $subValue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
