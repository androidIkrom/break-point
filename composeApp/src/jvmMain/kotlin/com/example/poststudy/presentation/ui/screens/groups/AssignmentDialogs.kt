package com.example.poststudy.presentation.ui.screens.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.ui.components.ButtonStyle
import com.example.poststudy.presentation.ui.components.SignalButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun kindIcon(kind: AssignmentKind): ImageVector = when (kind) {
    AssignmentKind.Lesson -> Icons.AutoMirrored.Outlined.MenuBook
    AssignmentKind.Exam -> Icons.Outlined.Assignment
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
        title = "${group.name} guruhiga biriktirish",
        subtitle = "Guruh tinglovchilari kirganda aynan shu mashg'ulot ochiladi",
        onDismiss = onDismiss,
        onConfirm = save,
        confirmEnabled = selectedId != null && !isSaving,
        extraAction = if (group.kind != null) {
            {
                SignalButton(
                    "Biriktirishni olib tashlash",
                    onClick = {
                        isSaving = true
                        scope.launch {
                            withContext(Dispatchers.IO) { repo.clearGroupAssignment(group.id) }
                            onSaved()
                        }
                    },
                    enabled = !isSaving,
                    style = ButtonStyle.Text,
                    height = 40.dp
                )
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
                CircularProgressIndicator(color = Signal.Primary, trackColor = Signal.Container)
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
            Text("Imtihon faqat test sifatida o'tkaziladi.", color = Signal.Muted, style = MaterialTheme.typography.bodyMedium)
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
        title = "$title — guruhlarga biriktirish",
        subtitle = "Belgilangan guruhlarning avvalgi biriktirmasi shu ${kind.label.lowercase()} bilan almashtiriladi",
        onDismiss = onDismiss,
        onConfirm = save,
        confirmEnabled = groups != null && !isSaving
    ) {
        val loaded = groups
        when {
            loaded == null -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Signal.Primary, trackColor = Signal.Container)
            }
            loaded.isEmpty() -> Text(
                "Bu fanda guruhlar yo'q. Avval 'Guruhlar' bo'limida guruh yarating.",
                color = Signal.Muted,
                style = MaterialTheme.typography.bodyMedium,
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
                        leading = { Checkbox(checked = isChecked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Signal.Primary)) },
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
            Text("Imtihon faqat test sifatida o'tkaziladi.", color = Signal.Muted, style = MaterialTheme.typography.bodyMedium)
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
                .shadow(24.dp, AppDesign.CardShape, ambientColor = Signal.Dark, spotColor = Signal.Dark)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        onDismiss(); true
                    } else false
                },
            shape = AppDesign.CardShape,
            color = Signal.Card,
            border = BorderStroke(1.dp, Signal.Outline)
        ) {
            Column(Modifier.padding(28.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Signal.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Signal.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

                content()

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    extraAction?.invoke()
                    Spacer(Modifier.weight(1f))
                    SignalButton("Bekor qilish", onClick = onDismiss, style = ButtonStyle.Outlined, height = 42.dp)
                    Spacer(Modifier.width(8.dp))
                    SignalButton("Saqlash", onClick = onConfirm, enabled = confirmEnabled, style = ButtonStyle.Primary, height = 42.dp)
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
            .height(48.dp)
            .clip(AppDesign.ComponentShape)
            .background(Signal.SurfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AssignmentKind.entries.forEach { kind ->
            val isSelected = kind == selected
            val bg by animateColorAsState(if (isSelected) Signal.Card else Color.Transparent, AppMotion.tweenOrSnap(AppMotion.fast), label = "kindBg")
            Surface(
                onClick = { onSelect(kind) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = AppDesign.SmallShape,
                color = bg,
                shadowElevation = if (isSelected) 1.dp else 0.dp
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    val color = if (isSelected) Signal.Primary else Signal.Muted
                    Icon(kindIcon(kind), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(kind.label, color = if (isSelected) Signal.Ink else Signal.Muted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private data class ListItem(val id: Int, val title: String, val subtitle: String)

@Composable
private fun ItemList(items: List<ListItem>, emptyText: String, selectedId: Int?, onSelect: (Int) -> Unit) {
    if (items.isEmpty()) {
        Text(emptyText, color = Signal.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 24.dp))
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
                leading = { RadioButton(selected = item.id == selectedId, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Signal.Primary)) },
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
    val bg by animateColorAsState(if (selected) Signal.Container else Signal.Surface, AppMotion.tweenOrSnap(AppMotion.fast), label = "rowBg")
    val border by animateColorAsState(if (selected) Signal.Primary else Signal.Outline, AppMotion.tweenOrSnap(AppMotion.fast), label = "rowBorder")
    Surface(
        modifier = Modifier.fillMaxWidth().clip(AppDesign.ComponentShape).clickable(onClick = onClick),
        shape = AppDesign.ComponentShape,
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Signal.Ink, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Signal.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The three lesson modes; options outside [allowed] are shown disabled. */
@Composable
fun ModeSelector(selected: LessonMode, allowed: List<LessonMode>, onSelect: (LessonMode) -> Unit) {
    Text("Rejim", style = MaterialTheme.typography.labelLarge, color = Signal.Ink, modifier = Modifier.padding(bottom = 8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LessonMode.entries.forEach { mode ->
            val enabled = mode in allowed
            val isSelected = enabled && mode == selected
            Surface(
                onClick = { onSelect(mode) },
                enabled = enabled,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = AppDesign.ComponentShape,
                color = when {
                    isSelected -> Signal.Primary
                    enabled -> Signal.Card
                    else -> Signal.SurfaceContainer
                },
                border = BorderStroke(1.dp, if (isSelected) Signal.Primary else Signal.Outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        mode.label,
                        color = when {
                            isSelected -> Color.White
                            enabled -> Signal.Ink
                            else -> Signal.Faint
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
    if (allowed.size == 1) {
        Text(
            "Bu dars faqat \"${allowed.single().label}\" materialidan iborat, shuning uchun rejimni o'zgartirib bo'lmaydi.",
            color = Signal.Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    } else if (allowed.isEmpty()) {
        Text("Avval darsni tanlang.", color = Signal.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
    }
}
