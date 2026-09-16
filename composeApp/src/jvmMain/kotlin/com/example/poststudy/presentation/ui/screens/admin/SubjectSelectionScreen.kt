package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.Subject
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.NetworkServerCard
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import com.example.poststudy.presentation.ui.components.ThreeStepVerificationDialog
import com.example.poststudy.presentation.ui.components.hoverEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
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
            color = Color(0xFFDC2626),
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Fanlar boshqaruvi", fontWeight = FontWeight.Black, color = Color(0xFF065F46)) },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    actions = {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.padding(end = 16.dp),
                            shape = AppDesign.ComponentShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Emerald)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Fan qo'shish", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            // One scrolling list: the header scrolls away with the cards on small screens
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 48.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Fanlarni tanlash",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669),
                            textAlign = TextAlign.Center
                        )
                        NetworkServerCard(modifier = Modifier.padding(top = 16.dp).widthIn(max = 900.dp).fillMaxWidth())
                        Text(
                            text = "Boshqarishni davom ettirish uchun fanni tanlang",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                }

                if (isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppDesign.Emerald)
                        }
                    }
                } else {
                    itemsIndexed(subjects, key = { _, s -> s.id }) { index, subject ->
                        SubjectSelectionCard(
                            title = subject.name,
                            subtitle = if (subject.id == currentSubjectId) "Hozirgi tanlangan" else "Boshqarish uchun tanlang",
                            icon = Icons.AutoMirrored.Filled.List,
                            color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size],
                            isPrimary = subject.id == currentSubjectId,
                            // The app always needs at least one subject
                            canDelete = subjects.size > 1,
                            onClick = { onSubjectSelected(subject) },
                            onEdit = { subjectToEdit = subject },
                            onDelete = { subjectToDelete = subject }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectNameDialog(title: String, initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf(false) }
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
                modifier = Modifier.fillMaxWidth(),
                shape = AppDesign.ComponentShape
            )
        }
    )
}

@Composable
fun SubjectSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    isPrimary: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .hoverEffect(scale = 1.03f, yOffset = -6f),
        shape = AppDesign.ComponentShape,
        color = Color.White,
        border = BorderStroke(if (isPrimary) 5.dp else 3.dp, color.copy(alpha = if (isPrimary) 1f else 0.6f)),
        shadowElevation = if (isPrimary) 16.dp else 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", tint = color)
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = Color(0xFFDC2626))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(if (isPrimary) 84.dp else 76.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(if (isPrimary) 40.dp else 34.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = if (isPrimary) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPrimary) color else Color(0xFF64748B),
                fontWeight = if (isPrimary) FontWeight.Black else FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
