package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.domain.model.Question
import com.example.poststudy.presentation.theme.AppDesign

/** Delivery state of a network-mode result to the admin computer. */
sealed interface SubmitStatus {
    data object Sending : SubmitStatus
    data object Sent : SubmitStatus
    data class Failed(val message: String) : SubmitStatus
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    questions: List<Question>,
    userAnswers: List<Int?>,
    studentName: String,
    timeSpentSeconds: Int,
    submitStatus: SubmitStatus? = null,
    onRetrySubmit: () -> Unit = {},
    onFinish: () -> Unit
) {
    val correctCount = questions.zip(userAnswers).count { (question, answer) ->
        question.correctIndex == answer
    }
    val percentage =
        if (questions.isNotEmpty()) (correctCount.toDouble() / questions.size * 100).toInt() else 0
    val focusRequester = remember { FocusRequester() }

    val scoreColor = when {
        percentage >= 80 -> Color(0xFF10B981) // Emerald
        percentage >= 60 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AppDesign.BackgroundGradient)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier.size(600.dp).offset(x = (-200).dp, y = (-200).dp)
                .background(scoreColor.copy(alpha = 0.05f), CircleShape)
        )

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.focusRequester(focusRequester).onPreviewKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                        onFinish()
                        true
                    } else false
                },
            topBar = {
                val wrongCount =
                    questions.zip(userAnswers).count { it.first.correctIndex != it.second }
                CenterAlignedTopAppBar(
                    title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Natijalar tahlili",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            "Savollar: ${questions.size} | Javoblar: ${userAnswers.size} | Xatolar: $wrongCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                    navigationIcon = {
                        BackButton(onClick = onFinish)
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }) { paddingValues ->
            // Use a single LazyColumn for the entire screen to ensure scrolling works perfectly
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
            ) {
                // 1. Result Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 700.dp),
                        shape = AppDesign.CardShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
                        border = BorderStroke(
                            4.dp, scoreColor.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(48.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sizning yakuniy natijangiz",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = studentName,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Box(
                                modifier = Modifier.size(220.dp)
                                    .background(scoreColor.copy(alpha = 0.1f), CircleShape)
                                    .border(6.dp, scoreColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$percentage%",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        color = scoreColor
                                    )
                                    Text(
                                        text = "$correctCount / ${questions.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = scoreColor.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(48.dp))

                            val timeMinutes = timeSpentSeconds / 60
                            val timeSeconds = timeSpentSeconds % 60
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = CircleShape,
                                border = BorderStroke(
                                    2.dp, Color(0xFFE2E8F0)
                                )
                            ) {
                                Text(
                                    text = "Sarflangan vaqt: %d:%02d".format(
                                        timeMinutes, timeSeconds
                                    ),
                                    modifier = Modifier.padding(
                                        horizontal = 32.dp, vertical = 12.dp
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                if (submitStatus != null) {
                    item {
                        SubmitStatusBanner(submitStatus, onRetrySubmit)
                    }
                }

                // 2. Analysis Section
                val wrongQuestions = questions.mapIndexedNotNull { index, question ->
                    val userAnswer = userAnswers.getOrNull(index)
                    if (userAnswer != question.correctIndex) {
                        Triple(index, question, userAnswer)
                    } else null
                }

                if (wrongQuestions.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Barcha javoblar to'g'ri! Ajoyib natija!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Xato javoblar tahlili:",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp)
                                .padding(top = 20.dp)
                        )
                    }

                    items(wrongQuestions) { triple ->
                        val originalIndex = triple.first
                        val question = triple.second
                        val userAnswerIndex = triple.third

                        Card(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp)
                                .animateContentSize(),
                            shape = AppDesign.CardShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            border = BorderStroke(
                                3.dp, Color(0xFFEF4444).copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(32.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Surface(
                                        color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp),
                                        border = BorderStroke(
                                            3.dp, Color(0xFFEF4444)
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${originalIndex + 1}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Text(
                                        text = question.text,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1E293B)
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                val userAnswerText =
                                    userAnswerIndex?.let { question.options.getOrNull(it) }
                                        ?: "O'tkazib yuborildi"

                                ResultDetailBox(
                                    label = "Sizning javobingiz",
                                    text = userAnswerText,
                                    color = Color(0xFFEF4444),
                                    backgroundColor = Color(0xFFFFF1F2)
                                )
                            }
                        }
                    }
                }

                // 3. Finish Button
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onFinish,
                            modifier = Modifier.padding(vertical = 32.dp).width(400.dp)
                                .height(72.dp),
                            shape = AppDesign.ComponentShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981), contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                        ) {
                            Text(
                                "TAYYOR",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitStatusBanner(status: SubmitStatus, onRetry: () -> Unit) {
    val (color, text) = when (status) {
        SubmitStatus.Sending -> Color(0xFF6366F1) to "Natija adminga yuborilmoqda..."
        SubmitStatus.Sent -> Color(0xFF10B981) to "Natija adminga yuborildi."
        is SubmitStatus.Failed -> Color(0xFFEF4444) to "Natija adminga yuborilmadi: ${status.message}"
    }
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 700.dp),
        color = color.copy(alpha = 0.08f),
        shape = AppDesign.ComponentShape,
        border = BorderStroke(2.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == SubmitStatus.Sending) {
                CircularProgressIndicator(color = color, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text,
                modifier = Modifier.weight(1f),
                color = color,
                fontWeight = FontWeight.Bold
            )
            if (status is SubmitStatus.Failed) {
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onRetry,
                    shape = AppDesign.ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Text("Qayta yuborish", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResultDetailBox(label: String, text: String, color: Color, backgroundColor: Color) {
    Surface(
        color = backgroundColor,
        shape = AppDesign.ComponentShape,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}
