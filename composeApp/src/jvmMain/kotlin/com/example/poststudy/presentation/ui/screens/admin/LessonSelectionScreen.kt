package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.Lesson
import com.example.poststudy.domain.model.LessonMode
import com.example.poststudy.domain.model.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import com.example.poststudy.presentation.ui.screens.groups.EmptyBox
import com.example.poststudy.presentation.ui.screens.groups.LoadingBox
import java.io.File

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
    var filter by remember { mutableStateOf<LessonMode?>(null) }

    // Students are not tied to the subject the admin last opened: they pick their own
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var studentSubjectId by remember { mutableStateOf(subjectId) }
    LaunchedEffect(isTeacher) {
        if (isTeacher) return@LaunchedEffect
        val all = withContext(Dispatchers.IO) { AppContainer.localRepository.getAllSubjects().first() }
        subjects = all
        if (all.none { it.id == studentSubjectId }) all.firstOrNull()?.let { studentSubjectId = it.id }
    }
    val activeSubjectId = if (isTeacher) subjectId else studentSubjectId

    LaunchedEffect(activeSubjectId) {
        isLoading = true
        AppContainer.localRepository.getAllLessons(activeSubjectId).collect { all ->
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
            confirmColor = Signal.Danger,
            onConfirm = {
                lessonToDelete?.let {
                    AppContainer.localRepository.deleteLesson(it.id)
                    lessons.remove(it)
                }
                lessonToDelete = null
            }
        )
    }

    val shown = lessons.filter { filter == null || it.mode == filter }

    val grid: @Composable () -> Unit = {
        when {
            isLoading -> LoadingBox()
            lessons.isEmpty() -> EmptyBox(
                Icons.AutoMirrored.Outlined.MenuBook,
                "Darslar hali qo'shilmagan",
                if (isTeacher) "Birinchi darsni yarating: taqdimot va test faylini tanlang, taymerlarni belgilang." else "Admin dars qo'shganda shu yerda ko'rinadi.",
                action = if (isTeacher) ({ SignalButton("Birinchi darsni yaratish", onClick = onAddNewLesson, icon = Icons.Default.Add, style = ButtonStyle.Gradient) }) else null
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.l),
                horizontalArrangement = Arrangement.spacedBy(Space.l),
                verticalArrangement = Arrangement.spacedBy(Space.l)
            ) {
                itemsIndexed(shown, key = { _, l -> l.id }) { index, lesson ->
                    LessonCard(
                        lesson = lesson,
                        index = index,
                        isTeacher = isTeacher,
                        onSelect = { onLessonSelected(lesson) },
                        onEdit = { onEditLesson(lesson) },
                        onDelete = { lessonToDelete = lesson }
                    )
                }
                if (isTeacher) {
                    itemsIndexed(listOf(Unit), key = { _, _ -> "add" }) { _, _ ->
                        AddCard("Yangi dars", "PowerPoint va Word test faylini tanlang, taymerlarni belgilang", onAddNewLesson)
                    }
                }
            }
        }
    }

    val filters: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = Space.page, vertical = Space.m), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SignalChip("Hammasi · ${lessons.size}", filter == null, { filter = null })
            LessonMode.entries.forEach { mode ->
                val count = lessons.count { it.mode == mode }
                if (count > 0 || isTeacher) SignalChip("${mode.label} · $count", filter == mode, { filter = if (filter == mode) null else mode })
            }
            Spacer(Modifier.weight(1f))
            Text(if (isTeacher) "Darsni bosib guruhlarga biriktiring" else "Darsni bosib taqdimotni oching", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
        }
    }

    if (isTeacher) {
        AdminPage(
            title = "Darslar · ${lessons.size}",
            eyebrow = "Materiallar",
            actions = {
                HelpIcon(
                    title = "Darslarni tanlash",
                    helpText = "Darsni bosib uni guruhlarga biriktiring va rejimini tanlang. Yangi dars qo'shish uchun + tugmasidan foydalaning."
                )
                SignalIconButton(Icons.Outlined.History, "Tarix", onViewHistory, size = 42.dp, iconSize = 20.dp)
                SignalButton("Dars qo'shish", onClick = onAddNewLesson, icon = Icons.Default.Add, style = ButtonStyle.Gradient, height = 42.dp)
            }
        ) {
            Column(Modifier.fillMaxSize()) {
                filters()
                Box(Modifier.weight(1f)) { grid() }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                SignalBackground(alpha = 0.85f)
                Column(Modifier.fillMaxSize().padding(horizontal = Space.page, vertical = 16.dp)) {
                    BackButton(onClick = onBack, onDark = true, modifier = Modifier.padding(start = 0.dp))
                    Spacer(Modifier.weight(1f))
                    Eyebrow("Tayyorgarlik", color = Signal.Amber)
                    Text("Dars tanlash", style = MaterialTheme.typography.displaySmall, color = Signal.OnDark)
                    Text("Taqdimotni taymersiz, o'z sur'atingizda ko'rib chiqing.", style = MaterialTheme.typography.bodyMedium, color = Signal.OnDarkMuted)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = Space.contentMaxWidth + Space.page * 2).fillMaxSize()) {
                    if (subjects.size > 1) {
                        Row(
                            Modifier.padding(start = Space.page, end = Space.page, top = Space.m),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fan:", style = MaterialTheme.typography.labelLarge, color = Signal.Ink)
                            subjects.forEach { subject ->
                                SignalChip(subject.name, subject.id == studentSubjectId, { studentSubjectId = subject.id })
                            }
                        }
                    }
                    filters()
                    Box(Modifier.weight(1f)) { grid() }
                }
            }
        }
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    index: Int,
    isTeacher: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val presentationExists = !lesson.mode.hasSlides || File(lesson.presentationPath).exists()
    val testExists = !lesson.mode.hasTest || File(lesson.testPath).exists()
    val hasError = !presentationExists || !testExists

    SignalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        padding = 0.dp,
        borderColor = if (hasError) Signal.DangerBorder else Signal.Outline
    ) {
        LessonCover(index, Modifier.fillMaxWidth().height(104.dp), danger = hasError) {
            Box(Modifier.fillMaxSize().padding(12.dp)) {
                if (hasError) {
                    StatusChip("Fayl topilmadi", Color.White.copy(alpha = 0.18f), Color.White, icon = Icons.Outlined.WarningAmber, iconTint = Color.White)
                } else {
                    StatusChip(lesson.mode.label, Color.White.copy(alpha = 0.14f), Color.White)
                }
                Text(
                    "%02d".format(index + 1),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
        Column(Modifier.padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(lesson.title, style = MaterialTheme.typography.titleLarge, color = Signal.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (hasError) {
                Text(
                    if (!presentationExists) "Taqdimot fayli yo'q: ${File(lesson.presentationPath).name}" else "Test fayli yo'q: ${File(lesson.testPath).name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Signal.Danger,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (lesson.mode.hasSlides) Meta(Icons.Outlined.Slideshow, "${lesson.slideTimerSeconds / 60} daq dars")
                if (lesson.mode.hasTest) Meta(Icons.Outlined.Quiz, "${lesson.testTimerSeconds / 60} daq test")
            }
            if (isTeacher) {
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
}

@Composable
fun Meta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = Signal.Muted, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
    }
}

/** Dashed "add new" tile at the end of a grid. */
@Composable
fun AddCard(title: String, text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
        shape = AppDesign.CardShape,
        color = Signal.Card,
        border = androidx.compose.foundation.BorderStroke(2.dp, Signal.OutlineStrong)
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(shape = AppDesign.CardShape, color = Signal.Container) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = null, tint = Signal.Primary) }
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Signal.Primary)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Signal.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.widthIn(max = 240.dp))
        }
    }
}
