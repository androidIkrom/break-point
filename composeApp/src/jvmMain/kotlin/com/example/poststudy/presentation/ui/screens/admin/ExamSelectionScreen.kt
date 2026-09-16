package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.Exam
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.hoverEffect
import com.example.poststudy.presentation.ui.components.PostStudyDialog

@OptIn(ExperimentalMaterial3Api::class)
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
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                examToDelete?.let {
                    AppContainer.localRepository.deleteExam(it.id)
                    exams.remove(it)
                }
                examToDelete = null
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Imtihonlar ro'yxati", color = Color(0xFF065F46), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    actions = {
                        IconButton(onClick = onAddNewExam, modifier = Modifier.padding(top = 16.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Qo'shish", tint = Color(0xFF6366F1))
                        }
                        Spacer(Modifier.width(46.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                }
            } else if (exams.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF6366F1).copy(alpha = 0.2f))
                        Spacer(Modifier.height(24.dp))
                        Text("Imtihonlar hali qo'shilmagan", color = Color(0xFF64748B), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onAddNewExam,
                            modifier = Modifier.padding(top = 32.dp).height(64.dp).width(280.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                            shape = AppDesign.ComponentShape,
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text("Bbirinchi imtihonni yaratish", fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 400.dp),
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    itemsIndexed(exams) { index, exam ->
                        val color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size]
                        ExamCard(
                            exam = exam,
                            themeColor = color,
                            onSelect = { onExamSelected(exam) },
                            onEdit = { onEditExam(exam) },
                            onDelete = { examToDelete = exam }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamCard(
    exam: Exam,
    themeColor: Color = AppDesign.Indigo,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val fileExists = File(exam.testPath).exists()

    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp).hoverEffect(scale = 1.03f, yOffset = -6f),
        shape = AppDesign.CardShape,
        color = if (fileExists) Color.White else Color(0xFFFFF1F2),
        border = BorderStroke(4.dp, if (fileExists) themeColor.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f)),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (fileExists) themeColor else Color(0xFF991B1B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!fileExists) {
                        Text(
                            text = "FAYL TOPILMADI",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "IMTIHON",
                            style = MaterialTheme.typography.labelLarge,
                            color = themeColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.background(themeColor.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", tint = themeColor)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = Color(0xFFEF4444))
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBadge(
                    icon = Icons.Default.Timer,
                    text = "${exam.testTimerSeconds / 60} daqiqa",
                    color = themeColor
                )
                InfoBadge(
                    icon = Icons.Default.Assignment,
                    text = "${exam.questionsPerStudent} ta savol",
                    color = themeColor
                )
            }
        }
    }
}
