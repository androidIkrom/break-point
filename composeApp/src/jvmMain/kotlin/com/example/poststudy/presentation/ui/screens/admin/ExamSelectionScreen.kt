package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.Exam
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import com.example.poststudy.presentation.ui.screens.groups.EmptyBox
import com.example.poststudy.presentation.ui.screens.groups.LoadingBox
import java.io.File

@Composable
fun ExamSelectionScreen(
    subjectId: Int,
    onExamSelected: (Exam) -> Unit,
    onAddNewExam: () -> Unit,
    onEditExam: (Exam) -> Unit,
    onBack: () -> Unit
) {
    val exams = remember { mutableStateListOf<Exam>() }
    var isLoading by remember { mutableStateOf(true) }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }

    LaunchedEffect(subjectId) {
        isLoading = true
        AppContainer.localRepository.getAllExams(subjectId).collect {
            exams.clear()
            exams.addAll(it)
            isLoading = false
        }
    }

    if (examToDelete != null) {
        PostStudyDialog(
            onDismissRequest = { examToDelete = null },
            title = "Imtihonni o'chirish",
            text = "'${examToDelete?.title}' imtihonini o'chirishni xohlaysizmi? Bu amalni qaytarib bo'lmaydi.",
            confirmText = "O'chirish",
            confirmColor = Signal.Danger,
            onConfirm = {
                examToDelete?.let {
                    AppContainer.localRepository.deleteExam(it.id)
                    exams.remove(it)
                }
                examToDelete = null
            }
        )
    }

    AdminPage(
        title = "Imtihonlar · ${exams.size}",
        eyebrow = "Nazorat",
        subtitle = "Imtihon faqat test sifatida o'tkaziladi. Imtihonni bosib guruhlarga biriktiring.",
        actions = {
            HelpIcon(
                title = "Imtihonlar",
                helpText = "Imtihonni bosib uni guruhlarga biriktiring. Imtihon faqat test sifatida o'tkaziladi."
            )
            SignalButton("Imtihon qo'shish", onClick = onAddNewExam, icon = Icons.Default.Add, style = ButtonStyle.Gradient, height = 42.dp)
        }
    ) {
        when {
            isLoading -> LoadingBox()
            exams.isEmpty() -> EmptyBox(
                Icons.Outlined.Assignment,
                "Imtihonlar hali qo'shilmagan",
                "Word test faylini tanlang, savollar sonini va vaqtni belgilang.",
                action = { SignalButton("Birinchi imtihonni yaratish", onClick = onAddNewExam, icon = Icons.Default.Add, style = ButtonStyle.Gradient) }
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.xl),
                horizontalArrangement = Arrangement.spacedBy(Space.l),
                verticalArrangement = Arrangement.spacedBy(Space.l)
            ) {
                itemsIndexed(exams, key = { _, e -> e.id }) { index, exam ->
                    ExamCard(exam, index, onSelect = { onExamSelected(exam) }, onEdit = { onEditExam(exam) }, onDelete = { examToDelete = exam })
                }
                itemsIndexed(listOf(Unit), key = { _, _ -> "add" }) { _, _ ->
                    AddCard("Yangi imtihon", "Word test faylini tanlang, savollar soni va vaqtni belgilang", onAddNewExam)
                }
            }
        }
    }
}

@Composable
fun ExamCard(exam: Exam, index: Int, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val fileExists = File(exam.testPath).exists()
    SignalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        padding = 0.dp,
        borderColor = if (fileExists) Signal.Outline else Signal.DangerBorder
    ) {
        LessonCover(index + 1, Modifier.fillMaxWidth().height(104.dp), danger = !fileExists, amber = fileExists) {
            Box(Modifier.fillMaxSize().padding(12.dp)) {
                if (fileExists) StatusChip("Imtihon", Color.White.copy(alpha = 0.14f), Color.White)
                else StatusChip("Fayl topilmadi", Color.White.copy(alpha = 0.18f), Color.White, icon = Icons.Outlined.WarningAmber, iconTint = Color.White)
                Text("%02d".format(index + 1), style = MaterialTheme.typography.displaySmall, color = Color.White.copy(alpha = 0.18f), modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
        Column(Modifier.padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(exam.title, style = MaterialTheme.typography.titleLarge, color = Signal.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!fileExists) Text("Test fayli yo'q: ${File(exam.testPath).name}", style = MaterialTheme.typography.labelLarge, color = Signal.Danger, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Meta(Icons.Outlined.Quiz, "${exam.questionsPerStudent} ta savol")
                Meta(Icons.Outlined.Timer, "${exam.testTimerSeconds / 60} daqiqa")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Biriktirish →", style = MaterialTheme.typography.labelMedium, color = Signal.Primary)
                Spacer(Modifier.weight(1f))
                SignalIconButton(Icons.Default.Edit, "Tahrirlash", onEdit, size = 32.dp, iconSize = 15.dp)
                Spacer(Modifier.width(6.dp))
                SignalIconButton(Icons.Default.Delete, "O'chirish", onDelete, tint = Signal.Danger, size = 32.dp, iconSize = 15.dp)
            }
        }
    }
}
