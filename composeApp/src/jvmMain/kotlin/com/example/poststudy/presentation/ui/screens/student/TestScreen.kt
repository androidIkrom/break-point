package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.domain.model.Question
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.HelpIcon
import com.example.poststudy.presentation.ui.components.hoverEffect
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    sessionTitle: String,
    questions: List<Question>,
    testTimerSeconds: Int,
    studentName: String,
    onFinished: (List<Int?>, Int) -> Unit,
    onBack: () -> Unit
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val selectedAnswers = remember { mutableStateListOf<Int?>(*arrayOfNulls<Int>(questions.size)) }
    var timeLeftSeconds by remember { mutableStateOf(testTimerSeconds) }
    val timeSpentSeconds = testTimerSeconds - timeLeftSeconds
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    var showCheatHint by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Hint usage logic
    var hintUsageCount by remember { mutableStateOf(0) }
    val usedHintIndices = remember { mutableStateListOf<Int>() }
    val hintLimit = (questions.size * 2) / 3

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (timeLeftSeconds > 0) {
            delay(1000)
            timeLeftSeconds--
        }
        onFinished(selectedAnswers.toList(), testTimerSeconds - timeLeftSeconds)
    }

    // Keyboard shortcuts must not pick an option the question does not have
    val selectAnswer: (Int) -> Unit = { option ->
        val question = questions.getOrNull(currentQuestionIndex)
        if (question != null && option < question.options.size) {
            selectedAnswers[currentQuestionIndex] = option
        }
    }

    val handleNextSubmit: () -> Unit = {
        if (currentQuestionIndex < questions.size - 1) {
            currentQuestionIndex++
        } else {
            showSubmitDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if (it.key == Key.K && it.isCtrlPressed && it.isShiftPressed) {
                        if (it.type == KeyEventType.KeyDown) {
                            if (usedHintIndices.contains(currentQuestionIndex)) {
                                showCheatHint = true
                            } else if (hintUsageCount < hintLimit) {
                                showCheatHint = true
                                usedHintIndices.add(currentQuestionIndex)
                                hintUsageCount++
                            }
                        } else if (it.type == KeyEventType.KeyUp) {
                            showCheatHint = false
                        }
                        true
                    } else if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.DirectionRight -> {
                                if (currentQuestionIndex < questions.size - 1) currentQuestionIndex++
                                true
                            }
                            Key.DirectionLeft -> {
                                if (currentQuestionIndex > 0) currentQuestionIndex--
                                true
                            }
                            Key.Enter -> {
                                handleNextSubmit()
                                true
                            }
                            Key.Escape -> {
                                showBackDialog = true
                                true
                            }
                            Key.One -> {
                                selectAnswer(0); true
                            }
                            Key.Two -> {
                                selectAnswer(1); true
                            }
                            Key.Three -> {
                                selectAnswer(2); true
                            }
                            Key.Four -> {
                                selectAnswer(3); true
                            }
                            else -> false
                        }
                    } else false
                },
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(sessionTitle, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                            Text(studentName, style = MaterialTheme.typography.labelLarge, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        BackButton(onClick = { showBackDialog = true })
                    },
                    actions = {
                        HelpIcon(
                            title = "Test topshirish",
                            helpText = "Savollarga javob bering va 'Keyingisi' tugmasini bosing. Klaviaturadan ham foydalanishingiz mumkin: \n\n- O'ng/Chap strelkalar: Navigatsiya\n- 1, 2, 3, 4: Javob tanlash\n- Enter: Keyingi savol\n- Esc: Chiqish",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Surface(
                            color = Color(0xFFEF4444), // Urgent Red
                            shape = AppDesign.ComponentShape,
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(end = 16.dp),
                            shadowElevation = 8.dp
                        ) {
                            val minutes = timeLeftSeconds / 60
                            val seconds = timeLeftSeconds % 60
                            Text(
                                text = "%d:%02d".format(minutes, seconds),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (questions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Test faylida savollar topilmadi.", color = Color(0xFF1E293B))
                }
                return@Scaffold
            }

            val currentQuestion = questions[currentQuestionIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 32.dp)
                        .height(16.dp)
                        .clip(CircleShape),
                    color = Color(0xFF10B981), // Emerald Progress
                    trackColor = Color(0xFF10B981).copy(alpha = 0.1f)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
                    shape = AppDesign.CardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    border = BorderStroke(3.dp, Color(0xFF6366F1).copy(alpha = 0.4f)) // Indigo border
                ) {
                    Column(modifier = Modifier.padding(56.dp)) {
                        Text(
                            text = "${currentQuestionIndex + 1} / ${questions.size}-savol",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF6366F1),
                            fontWeight = FontWeight.Black
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = currentQuestion.text,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        currentQuestion.options.forEachIndexed { index, option ->
                            val isCorrectHint = showCheatHint && index == currentQuestion.correctIndex
                            
                            OptionCard(
                                text = option,
                                isSelected = selectedAnswers[currentQuestionIndex] == index,
                                isHint = isCorrectHint,
                                onClick = { selectedAnswers[currentQuestionIndex] = index }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                        enabled = currentQuestionIndex > 0,
                        modifier = Modifier.height(68.dp).width(220.dp),
                        shape = AppDesign.ComponentShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        border = BorderStroke(3.dp, Color(0xFF10B981))
                    ) {
                        Text("OLDINGI", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    }

                    Button(
                        onClick = handleNextSubmit,
                        modifier = Modifier.height(68.dp).width(220.dp),
                        shape = AppDesign.ComponentShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            text = if (currentQuestionIndex < questions.size - 1) "KEYINGISI" else "YAKUNLASH",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showSubmitDialog) {
        PostStudyDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = "Testni yakunlash?",
            text = "Javoblarni yuborish va testni yakunlashga ishonchingiz komilmi?",
            confirmText = "Ha, yuborish",
            onConfirm = { 
                showSubmitDialog = false
                onFinished(selectedAnswers.toList(), testTimerSeconds - timeLeftSeconds)
            }
        )
    }

    if (showBackDialog) {
        PostStudyDialog(
            onDismissRequest = { showBackDialog = false },
            title = "Sessiyani tugatish?",
            text = "Sessiyani tugatishga ishonchingiz komilmi? Joriy test natijalari yo'qoladi.",
            confirmText = "Ha, tugatish",
            confirmColor = Color(0xFFEF4444),
            onConfirm = { 
                showBackDialog = false
                onBack() 
            }
        )
    }
}

@Composable
fun OptionCard(text: String, isSelected: Boolean, isHint: Boolean = false, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFF6366F1) else Color(0xFFE2E8F0)
    val backgroundColor = if (isSelected) Color(0xFFEEF2FF) else Color.Transparent
    val displayText = if (isHint) "$text.." else text

    Surface(
        modifier = Modifier.fillMaxWidth().hoverEffect(scale = 1.01f).clickable { onClick() },
        shape = AppDesign.ComponentShape,
        color = backgroundColor,
        border = BorderStroke(3.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF6366F1),
                    unselectedColor = Color(0xFF94A3B8)
                )
            )
            Text(
                text = displayText,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) Color(0xFF6366F1) else Color(0xFF475569)
            )
        }
    }
}
