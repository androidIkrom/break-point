package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.Subject
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SubjectSelectionScreen(
    currentSubjectId: Int,
    onSubjectSelected: (Subject) -> Unit,
    onBack: () -> Unit
) {
    val repo = AppContainer.localRepository
    val scope = rememberCoroutineScope()
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    fun refresh() {
        scope.launch {
            subjects = withContext(Dispatchers.IO) { repo.getAllSubjects().first() }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    if (showAddDialog || subjectToEdit != null) {
        // Captured now: the dialog state is cleared before the save finishes
        val editing = subjectToEdit
        SubjectNameDialog(
            title = if (editing == null) "Yangi fan qo'shish" else "Fanni tahrirlash",
            initialName = editing?.name.orEmpty(),
            onDismiss = { showAddDialog = false; subjectToEdit = null },
            onSave = { name ->
                showAddDialog = false
                subjectToEdit = null
                scope.launch {
                    withContext(Dispatchers.IO) {
                        if (editing == null) repo.addSubject(name).first()
                        else repo.updateSubject(editing.copy(name = name))
                    }
                    refresh()
                }
            }
        )
    }

    subjectToDelete?.let { subject ->
        ThreeStepVerificationDialog(
            title = "Fanni o'chirish",
            warning = "'${subject.name}' fanini o'chirsangiz, undagi barcha darslar, imtihonlar, guruhlar, tinglovchilar va natijalar ham o'chadi. Bu amalni qaytarib bo'lmaydi.",
            confirmWord = subject.name,
            finalButtonText = "O'chirish",
            color = Signal.Danger,
            onDismiss = { subjectToDelete = null },
            onConfirmed = {
                subjectToDelete = null
                scope.launch {
                    withContext(Dispatchers.IO) { repo.deleteSubject(subject.id) }
                    refresh()
                }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            SignalBackground(alpha = 0.85f)
            Column(Modifier.fillMaxSize().padding(horizontal = Space.page, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BackButton(onClick = onBack, onDark = true, modifier = Modifier.padding(start = 0.dp))
                    Text("Fanlar boshqaruvi", style = MaterialTheme.typography.titleLarge, color = Signal.OnDark)
                    Spacer(Modifier.weight(1f))
                    SignalButton("Fan qo'shish", onClick = { showAddDialog = true }, icon = Icons.Default.Add, style = ButtonStyle.Cyan, height = 40.dp)
                }
                Spacer(Modifier.weight(1f))
                Eyebrow("Admin", color = Signal.Amber)
                Text("Fanni tanlang", style = MaterialTheme.typography.displaySmall, color = Signal.OnDark)
                Text("Boshqarishni davom ettirish uchun fanni tanlang. Darslar, guruhlar va natijalar fanga bog'liq.", style = MaterialTheme.typography.bodyMedium, color = Signal.OnDarkMuted)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(280.dp),
                modifier = Modifier.widthIn(max = Space.contentMaxWidth + Space.page * 2).fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(Space.l),
                verticalArrangement = Arrangement.spacedBy(Space.l),
                contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.xl)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    NetworkServerCard(modifier = Modifier.fillMaxWidth())
                }
                if (isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Signal.Primary, trackColor = Signal.Container)
                        }
                    }
                } else {
                    itemsIndexed(subjects, key = { _, s -> s.id }) { index, subject ->
                        SubjectSelectionCard(
                            subject = subject,
                            index = index,
                            isPrimary = subject.id == currentSubjectId,
                            // The app always needs at least one subject
                            canDelete = subjects.size > 1,
                            onClick = { onSubjectSelected(subject) },
                            onEdit = { subjectToEdit = subject },
                            onDelete = { subjectToDelete = subject }
                        )
                    }
                    item { AddCard("Yangi fan", "Alohida darslar, guruhlar va natijalar bilan", onClick = { showAddDialog = true }) }
                }
            }
        }
    }
}

@Composable
private fun SubjectNameDialog(title: String, initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }
    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = "Fan nomini kiriting:",
        confirmText = "Saqlash",
        onConfirm = {
            val trimmed = name.trim().replace(Regex("\\s+"), " ")
            if (trimmed.isEmpty()) error = true else onSave(trimmed)
        },
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 60) name = it; error = false },
                label = { Text("Fan nomi") },
                singleLine = true,
                isError = error,
                supportingText = if (error) {
                    { Text("Fan nomini kiriting") }
                } else null,
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
                shape = AppDesign.SmallShape
            )
        }
    )
}

@Composable
private fun SubjectSelectionCard(
    subject: Subject,
    index: Int,
    isPrimary: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SignalCard(
        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
        onClick = onClick,
        padding = 0.dp,
        borderColor = if (isPrimary) Signal.Primary else Signal.Outline
    ) {
        LessonCover(index + 3, Modifier.fillMaxWidth().height(96.dp)) {
            Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.Top) {
                if (isPrimary) StatusChip("Hozirgi fan", Signal.Cyan, Signal.Dark, icon = Icons.Default.Check, iconTint = Signal.Dark)
                Spacer(Modifier.weight(1f))
                SignalIconButton(Icons.Default.Edit, "Tahrirlash", onEdit, tint = Color.White, container = Color.White.copy(alpha = 0.14f), borderColor = Color.White.copy(alpha = 0.2f), size = 32.dp, iconSize = 15.dp, onDark = true)
                if (canDelete) {
                    Spacer(Modifier.width(6.dp))
                    SignalIconButton(Icons.Default.Delete, "O'chirish", onDelete, tint = Color.White, container = Signal.Danger.copy(alpha = 0.5f), borderColor = null, size = 32.dp, iconSize = 15.dp, onDark = true)
                }
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.School, contentDescription = null, tint = Signal.Primary, modifier = Modifier.size(20.dp))
                Text(subject.name, style = MaterialTheme.typography.titleLarge, color = Signal.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(if (isPrimary) "Hozir tanlangan · ochish" else "Boshqarish uchun tanlang", style = MaterialTheme.typography.bodySmall, color = if (isPrimary) Signal.Primary else Signal.Muted)
        }
    }
}
