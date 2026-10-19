package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.domain.model.GradedResult
import com.example.poststudy.domain.model.Question
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*

/** Delivery state of a network-mode result to the admin computer. */
sealed interface SubmitStatus {
    data object Sending : SubmitStatus
    data object Sent : SubmitStatus
    data class Failed(val message: String) : SubmitStatus
}

@Composable
fun ResultScreen(
    questions: List<Question>,
    userAnswers: List<Int?>,
    /** Null while the admin computer has not graded the answers yet. */
    graded: GradedResult?,
    studentName: String,
    timeSpentSeconds: Int,
    submitStatus: SubmitStatus? = null,
    onRetrySubmit: () -> Unit = {},
    onFinish: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val parallax = rememberParallax()
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    if (graded == null) {
        PendingResult(studentName, timeSpentSeconds, submitStatus, onRetrySubmit, onFinish, focusRequester)
        return
    }

    val correctCount = graded.correctAnswers
    val percentage = if (graded.totalQuestions > 0) (correctCount.toDouble() / graded.totalQuestions * 100).toInt() else 0

    val scoreColor = when {
        percentage >= 80 -> Signal.Cyan
        percentage >= 60 -> Signal.Amber
        else -> Signal.DangerOnDark
    }
    val (badgeText, badgeBg, badgeFg) = when {
        percentage >= 80 -> Triple("A'lo daraja", Signal.Amber.copy(alpha = 0.2f), Signal.Amber)
        percentage >= 60 -> Triple("Yaxshi daraja", Signal.Cyan.copy(alpha = 0.18f), Signal.Cyan)
        else -> Triple("Qoniqarli", Color.White.copy(alpha = 0.1f), Signal.OnDarkMuted)
    }
    val title = when {
        percentage >= 80 -> "A'lo natija!"
        percentage >= 60 -> "Yaxshi natija"
        else -> "Yana mashq qiling"
    }

    val wrongQuestions = graded.wrongIndices.mapNotNull { index ->
        questions.getOrNull(index)?.let { Triple(index, it, userAnswers.getOrNull(index)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .parallaxSource(parallax)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    onFinish(); true
                } else false
            }
    ) {
        SignalBackground(parallax = parallax.value)

        Row(Modifier.fillMaxWidth().padding(horizontal = Space.page, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(onClick = onFinish, onDark = true, modifier = Modifier.padding(start = 0.dp))
            Column {
                Text("Natijalar tahlili", style = MaterialTheme.typography.titleMedium, color = Signal.OnDark)
                Text("Savollar: ${questions.size} · Javoblar: ${userAnswers.count { it != null }} · Xatolar: ${wrongQuestions.size}", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkMuted)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = Space.page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 80.dp, bottom = 48.dp)
        ) {
            item {
                Box(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    GlassCard(Modifier.fillMaxWidth(), padding = 32.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreRing(percent = percentage, color = scoreColor, caption = "$correctCount / ${graded.totalQuestions} to'g'ri")
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatusChip(badgeText, container = badgeBg, contentColor = badgeFg, icon = Icons.Default.EmojiEvents)
                                Text(title, style = MaterialTheme.typography.displaySmall, color = Signal.OnDark)
                                Text(studentName, style = MaterialTheme.typography.bodyLarge, color = Signal.OnDarkMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusChip("Vaqt %d:%02d".format(timeSpentSeconds / 60, timeSpentSeconds % 60), container = Color.White.copy(alpha = 0.1f), contentColor = Signal.OnDark)
                                    if (submitStatus != null) SubmitChip(submitStatus)
                                }
                            }
                        }
                    }
                    if (percentage >= 80) {
                        ConfettiBurst(playKey = percentage, modifier = Modifier.matchParentSize(), origin = Offset(0.14f, 0.5f))
                    }
                }
            }

            if (submitStatus is SubmitStatus.Failed) {
                item {
                    Box(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Surface(shape = AppDesign.CardShape, color = Signal.Danger.copy(alpha = 0.18f), border = BorderStroke(1.dp, Signal.Danger.copy(alpha = 0.5f))) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Natija adminga yuborilmadi: ${submitStatus.message}", color = Signal.OnDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                SignalButton("Qayta yuborish", onClick = onRetrySubmit, style = ButtonStyle.Cyan, icon = Icons.Default.Refresh, height = 40.dp)
                            }
                        }
                    }
                }
            }

            if (wrongQuestions.isEmpty()) {
                item {
                    Box(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Surface(shape = AppDesign.CardShape, color = Signal.Success.copy(alpha = 0.22f), border = BorderStroke(1.dp, Signal.SuccessOnDark.copy(alpha = 0.4f))) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Signal.SuccessOnDark)
                                Text("Barcha javoblar to'g'ri! Ajoyib natija!", color = Signal.OnDark, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Xato javoblar · ${wrongQuestions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Signal.OnDarkMuted,
                        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 8.dp)
                    )
                }
                itemsIndexed(wrongQuestions) { _, triple ->
                    val (originalIndex, question, userAnswerIndex) = triple
                    Box(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        GlassCard(Modifier.fillMaxWidth(), padding = 18.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(30.dp).background(Signal.Danger.copy(alpha = 0.35f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                                    Text("${originalIndex + 1}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFCA5A5))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(question.text, style = MaterialTheme.typography.titleMedium, color = Signal.OnDark)
                                    val userAnswerText = userAnswerIndex?.let { question.options.getOrNull(it) } ?: "O'tkazib yuborildi"
                                    Row {
                                        Text("Sizning javobingiz: ", style = MaterialTheme.typography.bodyMedium, color = Signal.OnDarkMuted)
                                        Text(userAnswerText, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFCA5A5), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    SignalButton("Tayyor", onClick = onFinish, style = ButtonStyle.Cyan, height = 48.dp, contentPadding = PaddingValues(horizontal = 32.dp))
                }
                Text("Enter — tayyor", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkFaint, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** Network mode before the admin computer answered: the answers are out, the score is not here yet. */
@Composable
private fun PendingResult(
    studentName: String,
    timeSpentSeconds: Int,
    submitStatus: SubmitStatus?,
    onRetrySubmit: () -> Unit,
    onFinish: () -> Unit,
    focusRequester: FocusRequester
) {
    val parallax = rememberParallax()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .parallaxSource(parallax)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    onFinish(); true
                } else false
            }
    ) {
        SignalBackground(parallax = parallax.value)
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.page, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(onClick = onFinish, onDark = true, modifier = Modifier.padding(start = 0.dp))
            Text("Natija", style = MaterialTheme.typography.titleMedium, color = Signal.OnDark)
        }
        Column(Modifier.fillMaxSize().padding(horizontal = Space.page, vertical = 80.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassCard(Modifier.widthIn(max = 840.dp).fillMaxWidth(), padding = 32.dp) {
                Text("Javoblar yuborildi", style = MaterialTheme.typography.displaySmall, color = Signal.OnDark)
                Text(studentName, style = MaterialTheme.typography.bodyLarge, color = Signal.OnDarkMuted)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Natijani admin kompyuteri hisoblaydi va saqlaydi. Javoblar bu kompyuterda baholanmaydi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Signal.OnDarkMuted
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("Vaqt %d:%02d".format(timeSpentSeconds / 60, timeSpentSeconds % 60), container = Color.White.copy(alpha = 0.1f), contentColor = Signal.OnDark)
                    if (submitStatus != null) SubmitChip(submitStatus)
                }
            }
            if (submitStatus is SubmitStatus.Failed) {
                Surface(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(), shape = AppDesign.CardShape, color = Signal.Danger.copy(alpha = 0.18f), border = BorderStroke(1.dp, Signal.Danger.copy(alpha = 0.5f))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Javoblar adminga yetib bormadi: ${submitStatus.message}", color = Signal.OnDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        SignalButton("Qayta yuborish", onClick = onRetrySubmit, style = ButtonStyle.Cyan, icon = Icons.Default.Refresh, height = 40.dp)
                    }
                }
            }
            Row(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                SignalButton("Tayyor", onClick = onFinish, style = ButtonStyle.Cyan, height = 48.dp, contentPadding = PaddingValues(horizontal = 32.dp))
            }
        }
    }
}

@Composable
private fun SubmitChip(status: SubmitStatus) {
    when (status) {
        SubmitStatus.Sending -> Surface(shape = AppDesign.PillShape, color = Color.White.copy(alpha = 0.1f)) {
            Row(Modifier.height(26.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CircularProgressIndicator(color = Signal.Cyan, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Text("Javoblar adminga yuborilmoqda...", color = Signal.OnDark, style = MaterialTheme.typography.labelMedium)
            }
        }
        SubmitStatus.Sent -> GlassPill("Natija adminda saqlandi", live = true)
        is SubmitStatus.Failed -> StatusChip("Yuborilmadi", container = Signal.Danger.copy(alpha = 0.3f), contentColor = Color(0xFFFCA5A5))
    }
}
