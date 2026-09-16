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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.Lesson
import com.example.poststudy.domain.model.LessonMode
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.hoverEffect
import com.example.poststudy.presentation.ui.components.PostStudyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonSelectionScreen(
    subjectId: Int,
    isTeacher: Boolean = true,
    onLessonSelected: (Lesson) -> Unit,
    onAddNewLesson: () -> Unit,
    onEditLesson: (Lesson) -> Unit,
    onViewHistory: () -> Unit,
    onBack: () -> Unit
) {
    val lessons = remember { mutableStateListOf<Lesson>() }
    var isLoading by remember { mutableStateOf(true) }
    var lessonToDelete by remember { mutableStateOf<Lesson?>(null) }

    LaunchedEffect(subjectId) {
        isLoading = true
        AppContainer.localRepository.getAllLessons(subjectId).collect { all ->
            lessons.clear()
            lessons.addAll(if (isTeacher) all else all.filter { it.mode.hasSlides })
            isLoading = false
        }
    }

    if (lessonToDelete != null) {
        PostStudyDialog(
            onDismissRequest = { lessonToDelete = null },
            title = "Darsni o'chirish",
            text = "'${lessonToDelete?.title}' darsini o'chirishni xohlaysizmi? Bu amalni qaytarib bo'lmaydi.",
            confirmText = "O'chirish",
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                lessonToDelete?.let {
                    AppContainer.localRepository.deleteLesson(it.id)
                    lessons.remove(it)
                }
                lessonToDelete = null
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
                    title = { Text(if (isTeacher) "Darslar ro'yxati" else "Dars tanlash", color = Color(0xFF065F46), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    actions = {
                        Row(modifier = Modifier.padding(top = 16.dp, end = 46.dp)){
                            if (isTeacher) {
                                IconButton(onClick = onViewHistory) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Tarix", tint = Color(0xFF10B981))
                                }
                                IconButton(onClick = onAddNewLesson) {
                                    Icon(Icons.Default.Add, contentDescription = "Qo'shish", tint = Color(0xFF3B82F6))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else if (lessons.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF10B981).copy(alpha = 0.2f))
                        Spacer(Modifier.height(24.dp))
                        Text("Darslar hali qo'shilmagan", color = Color(0xFF64748B), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (isTeacher) {
                            Button(
                                onClick = onAddNewLesson,
                                modifier = Modifier.padding(top = 32.dp).height(64.dp).width(280.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                shape = AppDesign.ComponentShape,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Text("BIRINCHI DARSNI YARATISH", fontWeight = FontWeight.Black)
                            }
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
                    itemsIndexed(lessons) { index, lesson ->
                        val color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size]
                        LessonCard(
                            lesson = lesson,
                            isTeacher = isTeacher,
                            themeColor = color,
                            onSelect = { onLessonSelected(lesson) },
                            onEdit = { onEditLesson(lesson) },
                            onDelete = { lessonToDelete = lesson }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonCard(
    lesson: Lesson,
    isTeacher: Boolean,
    themeColor: Color = Color(0xFF10B981),
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val presentationExists = !lesson.mode.hasSlides || File(lesson.presentationPath).exists()
    val testExists = !lesson.mode.hasTest || File(lesson.testPath).exists()
    val hasError = !presentationExists || !testExists

    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp).hoverEffect(scale = 1.03f, yOffset = -6f),
        shape = AppDesign.CardShape,
        color = if (hasError) Color(0xFFFFF1F2) else Color.White,
        border = BorderStroke(4.dp, if (hasError) Color.Red.copy(alpha = 0.5f) else themeColor.copy(alpha = 0.5f)),
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
                        text = lesson.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (hasError) Color(0xFF991B1B) else themeColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasError) {
                        Text(
                            text = "FAYL TOPILMADI",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = lesson.mode.label.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = themeColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                if (isTeacher) {
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
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (lesson.mode.hasTest) InfoBadge(
                    icon = Icons.Default.Timer,
                    text = "${lesson.testTimerSeconds / 60} daq test",
                    color = themeColor
                )
                if (lesson.mode.hasSlides) {
                    InfoBadge(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        text = "${lesson.slideTimerSeconds / 60} daq dars",
                        color = themeColor
                    )
                }
            }
        }
    }
}

@Composable
fun InfoBadge(icon: ImageVector, text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
