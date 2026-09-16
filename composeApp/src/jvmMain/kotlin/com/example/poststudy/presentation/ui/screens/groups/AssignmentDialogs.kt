package com.example.poststudy.presentation.ui.screens.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import com.example.poststudy.presentation.theme.AppDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TextDark = Color(0xFF1E293B)
private val TextMuted = Color(0xFF64748B)
private val BorderLight = Color(0xFFE2E8F0)
private val Danger = Color(0xFFEF4444)

fun kindIcon(kind: AssignmentKind): ImageVector = when (kind) {
    AssignmentKind.Lesson -> Icons.AutoMirrored.Filled.MenuBook
    AssignmentKind.Exam -> Icons.Default.Assignment
}

/** Short description of what a group's students will do, e.g. "Dars • Faqat test". */
fun assignmentLabel(kind: AssignmentKind, mode: LessonMode?): String =
    if (kind == AssignmentKind.Exam || mode == null) kind.label else "${kind.label} • ${mode.label}"

/** Picks the lesson or exam (and its mode) for one group. */
@Composable
fun GroupAssignmentDialog(
    group: GroupOverview,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val repo = AppContainer.localRepository
    val scope = rememberCoroutineScope()
    var lessons by remember { mutableStateOf<List<Lesson>?>(null) }
    var exams by remember { mutableStateOf<List<Exam>>(emptyList()) }

    var kind by remember { mutableStateOf(group.kind ?: AssignmentKind.Lesson) }
    var selectedId by remember { mutableStateOf(if (group.assignmentMissing) null else group.itemId) }
    var mode by remember { mutableStateOf(group.mode ?: LessonMode.ReAppropriation) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(group.subjectId) {
        withContext(Dispatchers.IO) {
            val l = repo.getAllLessons(group.subjectId).first()
            val e = repo.getAllExams(group.subjectId).first()
            l to e
        }.let { (l, e) ->
            lessons = l
            exams = e
        }
    }

    val selectedLesson = lessons?.firstOrNull { it.id == selectedId }.takeIf { kind == AssignmentKind.Lesson }
    val allowed = selectedLesson?.let { allowedModes(it) } ?: LessonMode.entries.toList()
    // Keep the mode valid for the chosen lesson
    LaunchedEffect(selectedLesson) {
        if (selectedLesson != null && mode !in allowed) mode = allowed.first()
    }

    val save: () -> Unit = {
        val id = selectedId
        if (id != null && !isSaving) {
            isSaving = true
            scope.launch {
                withContext(Dispatchers.IO) {
                    repo.setGroupAssignment(GroupAssignment(group.id, kind, id, mode))
                }
                onSaved()
            }
        }
    }

    AssignmentDialogFrame(
        title = "'${group.name}' guruhiga biriktirish",
        subtitle = "Guruh tinglovchilari kirganda aynan shu mashg'ulot ochiladi",
        onDismiss = onDismiss,
        onConfirm = save,
        confirmEnabled = selectedId != null && !isSaving,
        extraAction = if (group.kind != null) {
            {
                TextButton(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            withContext(Dispatchers.IO) { repo.clearGroupAssignment(group.id) }
                            onSaved()
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text("Biriktirishni olib tashlash", color = Danger, fontWeight = FontWeight.Bold)
                }
            }
        } else null
    ) {
        KindToggle(kind) {
            if (it != kind) {
                kind = it
                selectedId = null
            }
        }
        Spacer(Modifier.height(16.dp))

        val loadedLessons = lessons
        when {
            loadedLessons == null -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppDesign.Indigo)
            }
            kind == AssignmentKind.Lesson -> ItemList(
                items = loadedLessons.map { ListItem(it.id, it.title, it.mode.label) },
                emptyText = "Bu fanda darslar yo'q. Avval 'Darslar' bo'limida dars yarating.",
                selectedId = selectedId,
                onSelect = { selectedId = it }
            )
            else -> ItemList(
                items = exams.map { ListItem(it.id, it.title, "${it.questionsPerStudent} ta savol • ${it.testTimerSeconds / 60} daqiqa") },
                emptyText = "Bu fanda imtihonlar yo'q. Avval 'Imtihon' bo'limida imtihon yarating.",
                selectedId = selectedId,
                onSelect = { selectedId = it }
            )
        }

        Spacer(Modifier.height(16.dp))
        if (kind == AssignmentKind.Lesson) {
            ModeSelector(selected = mode, allowed = if (selectedLesson == null) emptyList() else allowed, onSelect = { mode = it })
        } else {
            Text("Imtihon faqat test sifatida o'tkaziladi.", color = TextMuted, fontWeight = FontWeight.Bold)
        }
    }
}

/** Assigns one lesson or exam to any number of groups of its subject. */
@Composable
fun AssignToGroupsDialog(
    kind: AssignmentKind,
    itemId: Int,
    title: String,
    subjectId: Int,
    lessonMode: LessonMode?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val repo = AppContainer.localRepository
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<GroupOverview>?>(null) }
    val checked = remember { mutableStateListOf<Int>() }
    val allowed = when (lessonMode) {
        null -> listOf(LessonMode.TestOnly)
        LessonMode.ReAppropriation -> LessonMode.entries.toList()
        else -> listOf(lessonMode)
    }
    var mode by remember { mutableStateOf(allowed.first()) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(subjectId) {
        val loaded = withContext(Dispatchers.IO) { repo.getGroupOverviews(subjectId) }
        groups = loaded
        val current = loaded.filter { it.kind == kind && it.itemId == itemId }
        checked.addAll(current.map { it.id })
        current.firstOrNull()?.mode?.takeIf { it in allowed && kind == AssignmentKind.Lesson }?.let { mode = it }
    }

    val save: () -> Unit = {
        val loaded = groups
        if (loaded != null && !isSaving) {
            isSaving = true
            scope.launch {
                withContext(Dispatchers.IO) {
                    loaded.forEach { g ->
                        val hadThis = g.kind == kind && g.itemId == itemId
                        when {
                            g.id in checked -> repo.setGroupAssignment(GroupAssignment(g.id, kind, itemId, mode))
                            hadThis -> repo.clearGroupAssignment(g.id)
                        }
                    }
                }
                onSaved()
            }
        }
    }

    AssignmentDialogFrame(
        title = "'$title' — guruhlarga biriktirish",
        subtitle = "Belgilangan guruhlarning avvalgi biriktirmasi shu ${kind.label.lowercase()} bilan almashtiriladi",
        onDismiss = onDismiss,
        onConfirm = save,
        confirmEnabled = groups != null && !isSaving
    ) {
        val loaded = groups
        when {
            loaded == null -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppDesign.Indigo)
            }
            loaded.isEmpty() -> Text(
                "Bu fanda guruhlar yo'q. Avval 'Guruhlar' bo'limida guruh yarating.",
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(loaded, key = { it.id }) { g ->
                    val isChecked = g.id in checked
                    val current = when {
                        g.kind == kind && g.itemId == itemId -> "Hozir: shu ${kind.label.lowercase()}"
                        g.isAssigned -> "Hozir: ${g.assignmentTitle} (${assignmentLabel(g.kind!!, g.mode)})"
                        else -> "Hozir: biriktirilmagan"
                    }
                    SelectableRow(
                        selected = isChecked,
                        onClick = { if (isChecked) checked.remove(g.id) else checked.add(g.id) },
                        leading = { Checkbox(checked = isChecked, onCheckedChange = null) },
                        title = g.name,
                        subtitle = current
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (kind == AssignmentKind.Lesson) {
            ModeSelector(selected = mode, allowed = allowed, onSelect = { mode = it })
        } else {
            Text("Imtihon faqat test sifatida o'tkaziladi.", color = TextMuted, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AssignmentDialogFrame(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    extraAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(640.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        onDismiss(); true
                    } else false
                },
            shape = AppDesign.CardShape,
            color = Color.White,
            border = BorderStroke(2.dp, AppDesign.Emerald.copy(alpha = 0.25f))
        ) {
            Column(Modifier.padding(32.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF065F46),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(subtitle, color = TextMuted, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

                content()

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    extraAction?.invoke()
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = AppDesign.ComponentShape,
                        border = BorderStroke(2.dp, BorderLight),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Bekor qilish", color = TextMuted, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        shape = AppDesign.ComponentShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Emerald),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Saqlash", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun KindToggle(selected: AssignmentKind, onSelect: (AssignmentKind) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(AppDesign.ComponentShape)
            .background(Color(0xFFF1F5F9))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AssignmentKind.entries.forEach { kind ->
            val isSelected = kind == selected
            Surface(
                onClick = { onSelect(kind) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Color.White else Color.Transparent,
                shadowElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    val color = if (isSelected) AppDesign.Emerald else TextMuted
                    Icon(kindIcon(kind), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(kind.label, color = color, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private data class ListItem(val id: Int, val title: String, val subtitle: String)

@Composable
private fun ItemList(items: List<ListItem>, emptyText: String, selectedId: Int?, onSelect: (Int) -> Unit) {
    if (items.isEmpty()) {
        Text(emptyText, color = TextMuted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            SelectableRow(
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) },
                leading = { RadioButton(selected = item.id == selectedId, onClick = null) },
                title = item.title,
                subtitle = item.subtitle
            )
        }
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(AppDesign.ComponentShape).clickable(onClick = onClick),
        shape = AppDesign.ComponentShape,
        color = if (selected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
        border = BorderStroke(2.dp, if (selected) AppDesign.Emerald else BorderLight)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextDark, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The three lesson modes; options outside [allowed] are shown disabled. */
@Composable
fun ModeSelector(selected: LessonMode, allowed: List<LessonMode>, onSelect: (LessonMode) -> Unit) {
    Text("Rejim", fontWeight = FontWeight.Black, color = TextDark, modifier = Modifier.padding(bottom = 8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LessonMode.entries.forEach { mode ->
            val enabled = mode in allowed
            val isSelected = enabled && mode == selected
            Surface(
                onClick = { onSelect(mode) },
                enabled = enabled,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = AppDesign.ComponentShape,
                color = when {
                    isSelected -> AppDesign.Indigo
                    enabled -> Color.White
                    else -> Color(0xFFF1F5F9)
                },
                border = BorderStroke(2.dp, if (isSelected) AppDesign.Indigo else BorderLight)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        mode.label,
                        color = when {
                            isSelected -> Color.White
                            enabled -> TextDark
                            else -> Color(0xFFCBD5E1)
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
    if (allowed.size == 1) {
        Text(
            "Bu dars faqat \"${allowed.single().label}\" materialidan iborat, shuning uchun rejimni o'zgartirib bo'lmaydi.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    } else if (allowed.isEmpty()) {
        Text("Avval darsni tanlang.", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
    }
}
