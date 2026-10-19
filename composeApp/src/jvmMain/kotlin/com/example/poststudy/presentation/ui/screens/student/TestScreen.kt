package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.domain.model.Question
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.delay
import kotlin.math.ceil

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
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val toasts = rememberToastState()

    // Counted from the clock, not from ticks, so the timer does not drift on a busy computer
    val startedAt = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        if (questions.isEmpty() || testTimerSeconds <= 0) return@LaunchedEffect
        while (true) {
            val left = testTimerSeconds - ((System.currentTimeMillis() - startedAt) / 1000).toInt()
            timeLeftSeconds = left.coerceAtLeast(0)
            if (left <= 0) break
            delay(250)
        }
        onFinished(selectedAnswers.toList(), testTimerSeconds)
    }

    // Milestone messages: halfway and all answered, each once
    val answeredCount = selectedAnswers.count { it != null }
    var halfShown by remember { mutableStateOf(false) }
    var allShown by remember { mutableStateOf(false) }
    LaunchedEffect(answeredCount) {
        val total = questions.size
        if (total == 0) return@LaunchedEffect
        val half = ceil(total / 2.0).toInt()
        if (!halfShown && answeredCount >= half && answeredCount < total && total >= 4) {
            halfShown = true
            toasts.show("Yarmidan o'tdingiz!", "$answeredCount / $total savol. Shu tempda davom eting.")
        }
        if (!allShown && answeredCount == total) {
            allShown = true
            toasts.show("Hammasiga javob berdingiz.", "Tekshirib, \"Yakunlash\" tugmasini bosing.", Icons.Default.Flag)
        }
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
            .background(Signal.Surface)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key) {
                        Key.DirectionRight -> {
                            if (currentQuestionIndex < questions.size - 1) currentQuestionIndex++
                            true
                        }
                        Key.DirectionLeft -> {
                            if (currentQuestionIndex > 0) currentQuestionIndex--
                            true
                        }
                        Key.Enter -> { handleNextSubmit(); true }
                        Key.Escape -> { showBackDialog = true; true }
                        Key.One -> { selectAnswer(0); true }
                        Key.Two -> { selectAnswer(1); true }
                        Key.Three -> { selectAnswer(2); true }
                        Key.Four -> { selectAnswer(3); true }
                        else -> false
                    }
                } else false
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header: progress ring, title, timer
            Surface(color = Signal.Card, border = BorderStroke(1.dp, Signal.Outline)) {
                Row(
                    Modifier.fillMaxWidth().height(68.dp).padding(horizontal = Space.xl),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BackButton(onClick = { showBackDialog = true }, modifier = Modifier.padding(start = 0.dp))
                    ProgressRing(progress = if (questions.isEmpty()) 0f else (currentQuestionIndex + 1f) / questions.size) {
                        Text("${currentQuestionIndex + 1}/${questions.size}", style = MaterialTheme.typography.labelSmall, color = Signal.Ink)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(sessionTitle, style = MaterialTheme.typography.titleMedium, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$studentName · $answeredCount / ${questions.size} javob", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
                    }
                    HelpIcon(
                        title = "Test topshirish",
                        helpText = "Savollarga javob bering va 'Keyingisi' tugmasini bosing. Klaviaturadan ham foydalanishingiz mumkin: \n\n- O'ng/Chap strelkalar: Navigatsiya\n- 1, 2, 3, 4: Javob tanlash\n- Enter: Keyingi savol\n- Esc: Chiqish"
                    )
                    TimerPill(timeLeftSeconds)
                }
            }

            if (questions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Test faylida savollar topilmadi.", style = MaterialTheme.typography.titleMedium, color = Signal.Ink)
                        Text("Admin test faylini tekshirishi kerak.", style = MaterialTheme.typography.bodyMedium, color = Signal.Muted)
                        SignalButton("Orqaga", onClick = onBack, style = ButtonStyle.Outlined)
                    }
                }
                return@Column
            }

            val reduceMotion = LocalReduceMotion.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.page, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.widthIn(max = 800.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    AnimatedContent(
                        targetState = currentQuestionIndex,
                        transitionSpec = {
                            val forward = targetState > initialState
                            screenTransition(back = !forward, reduceMotion = reduceMotion)
                        },
                        label = "question"
                    ) { index ->
                        val question = questions[index]
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Eyebrow("${index + 1} / ${questions.size}-savol")
                                Text(question.text, style = MaterialTheme.typography.headlineLarge, color = Signal.Ink)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                question.options.forEachIndexed { optionIndex, option ->
                                    OptionCard(
                                        index = optionIndex,
                                        text = option,
                                        isSelected = selectedAnswers[index] == optionIndex,
                                        onClick = { selectedAnswers[index] = optionIndex }
                                    )
                                }
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SignalButton(
                            "Oldingi",
                            onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                            enabled = currentQuestionIndex > 0,
                            style = ButtonStyle.Outlined,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            height = 46.dp
                        )
                        Text("1–4 tugmalar javobni tanlaydi", style = MaterialTheme.typography.bodySmall, color = Signal.Muted, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        SignalButton(
                            if (currentQuestionIndex < questions.size - 1) "Keyingisi" else "Yakunlash",
                            onClick = handleNextSubmit,
                            style = if (currentQuestionIndex < questions.size - 1) ButtonStyle.Primary else ButtonStyle.Gradient,
                            trailingIcon = if (currentQuestionIndex < questions.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            height = 46.dp
                        )
                    }
                }
            }
        }

        ToastHost(toasts, Modifier.fillMaxSize())
    }

    if (showSubmitDialog) {
        PostStudyDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = "Testni yakunlash?",
            text = "Javoblarni yuborish va testni yakunlashga ishonchingiz komilmi?",
            confirmText = "Ha, yuborish",
            onConfirm = {
                showSubmitDialog = false
                onFinished(selectedAnswers.toList(), ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceIn(0, testTimerSeconds))
            }
        )
    }

    if (showBackDialog) {
        PostStudyDialog(
            onDismissRequest = { showBackDialog = false },
            title = "Sessiyani tugatish?",
            text = "Sessiyani tugatishga ishonchingiz komilmi? Joriy test natijalari yo'qoladi.",
            confirmText = "Ha, tugatish",
            confirmColor = Signal.Danger,
            onConfirm = {
                showBackDialog = false
                onBack()
            }
        )
    }
}

/** One answer: number key badge, text, and a check that pops in when picked. */
@Composable
fun OptionCard(index: Int, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(if (isSelected) Signal.Primary else if (hovered) Signal.Primary.copy(alpha = 0.6f) else Signal.Outline, AppMotion.tweenOrSnap(AppMotion.fast), label = "optBorder")
    val bg by animateColorAsState(if (isSelected) Signal.Container else Signal.Card, AppMotion.tweenOrSnap(AppMotion.fast), label = "optBg")
    val keyBg by animateColorAsState(if (isSelected) Signal.Primary else Signal.SurfaceContainer, AppMotion.tweenOrSnap(AppMotion.fast), label = "keyBg")

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppDesign.ComponentShape,
        color = bg,
        border = BorderStroke(2.dp, border),
        interactionSource = interaction
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(30.dp).background(keyBg, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = if (isSelected) Color.White else Signal.OnContainer)
            }
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = Signal.Ink
            )
            PopCheck(visible = isSelected)
        }
    }
}
