package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.util.ResultsCsv
import com.example.poststudy.di.AppContainer
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.poststudy.domain.model.ExamRecord
import com.example.poststudy.domain.model.Group
import com.example.poststudy.domain.model.Student
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import com.example.poststudy.presentation.ui.screens.groups.EmptyBox
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(subjectId: Int, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(HistoryTab.GROUPS) }

    // Navigation states
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    var showClearAllDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<ExamRecord?>(null) }

    // Every result of this subject as a spreadsheet the teacher can keep or hand over
    val scope = rememberCoroutineScope()
    val saver = rememberFileSaverLauncher { }
    var exporting by remember { mutableStateOf(false) }
    val exportCsv: () -> Unit = {
        if (!exporting) {
            exporting = true
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    val repo = AppContainer.localRepository
                    val groups = repo.getAllGroups(subjectId).first().associate { it.id to it.name }
                    ResultsCsv.build(repo.getAllExamRecords(subjectId).first(), groups)
                }
                saver.launch(bytes, "breakpoint-natijalar-${java.time.LocalDate.now()}", "csv")
                exporting = false
            }
        }
    }

    // Keyboard back handling
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val handleBack = {
        when {
            selectedStudent != null -> selectedStudent = null
            selectedGroup != null -> selectedGroup = null
            selectedDate != null -> selectedDate = null
            else -> onBack()
        }
    }

    if (showClearAllDialog) {
        PostStudyDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = "Barcha tarixlarni tozalash",
            text = "Haqiqatan ham barcha imtihon yozuvlarini o'chirib tashlamoqchimisiz? Bu amalni qaytarib bo'lmaydi.",
            confirmText = "Hammasini tozalash",
            confirmColor = Signal.Danger,
            onConfirm = {
                AppContainer.localRepository.clearAllExamRecords(subjectId)
                showClearAllDialog = false
            }
        )
    }

    if (recordToDelete != null) {
        PostStudyDialog(
            onDismissRequest = { recordToDelete = null },
            title = "Yozuvni o'chirish",
            text = "Haqiqatan ham '${recordToDelete?.studentName}' tinglovchisining ushbu imtihon yozuvini o'chirib tashlamoqchimisiz?",
            confirmText = "O'chirish",
            confirmColor = Signal.Danger,
            onConfirm = {
                recordToDelete?.let { AppContainer.localRepository.deleteExamRecord(it.id) }
                recordToDelete = null
            }
        )
    }

    val crumbs = buildList {
        add(if (selectedTab == HistoryTab.GROUPS) "Guruhlar" else "So'ngilar")
        selectedGroup?.let { add(it.name) }
        selectedStudent?.let { add(it.name) }
        selectedDate?.let { add(it) }
    }

    AdminPage(
        title = "Imtihon tahlili",
        eyebrow = "Tarix",
        subtitle = crumbs.joinToString(" › "),
        onBack = if (crumbs.size > 1) handleBack else null,
        actions = {
            HelpIcon(
                title = "Imtihon tarixi",
                helpText = "Bu yerda barcha topshirilgan testlar natijalarini ko'rishingiz mumkin. Guruhlar bo'yicha yoki so'nggi natijalar bo'yicha saralashingiz mumkin."
            )
            SignalButton("CSV eksport", onClick = exportCsv, enabled = !exporting, icon = Icons.Outlined.FileDownload, style = ButtonStyle.Outlined, height = 42.dp)
            SignalIconButton(Icons.Outlined.DeleteSweep, "Hammasini tozalash", { showClearAllDialog = true }, tint = Signal.Danger, size = 42.dp, iconSize = 20.dp)
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.Escape, Key.Backspace -> { handleBack(); true }
                            else -> false
                        }
                    } else false
                }
        ) {
            Row(Modifier.padding(horizontal = Space.page, vertical = Space.m), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SignalChip("Guruhlar bo'yicha", selectedTab == HistoryTab.GROUPS, { selectedTab = HistoryTab.GROUPS; selectedDate = null }, icon = Icons.Outlined.Groups)
                SignalChip("So'nggi natijalar", selectedTab == HistoryTab.RECENT, { selectedTab = HistoryTab.RECENT; selectedGroup = null; selectedStudent = null }, icon = Icons.Outlined.History)
                Spacer(Modifier.weight(1f))
                Text("Esc — bir qadam orqaga", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    HistoryTab.GROUPS -> GroupsView(
                        subjectId = subjectId,
                        selectedGroup = selectedGroup,
                        selectedStudent = selectedStudent,
                        onGroupClick = { selectedGroup = it },
                        onStudentClick = { selectedStudent = it },
                        onDeleteRecord = { recordToDelete = it }
                    )
                    HistoryTab.RECENT -> RecentView(
                        subjectId = subjectId,
                        selectedDate = selectedDate,
                        onDateClick = { selectedDate = it },
                        onDeleteRecord = { recordToDelete = it }
                    )
                }
            }
        }
    }
}

enum class HistoryTab { GROUPS, RECENT }

@Composable
fun GroupsView(
    subjectId: Int,
    selectedGroup: Group?,
    selectedStudent: Student?,
    onGroupClick: (Group) -> Unit,
    onStudentClick: (Student) -> Unit,
    onDeleteRecord: (ExamRecord) -> Unit
) {
    when {
        selectedStudent != null -> {
            var studentRecords by remember { mutableStateOf<List<ExamRecord>>(emptyList()) }
            LaunchedEffect(selectedStudent) {
                AppContainer.localRepository.getStudentRecords(selectedStudent.id).collect { studentRecords = it }
            }
            RecordsList(studentRecords, onDeleteRecord)
        }
        selectedGroup != null -> {
            var groupStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
            LaunchedEffect(selectedGroup) {
                AppContainer.localRepository.getStudentsByGroup(selectedGroup.id).collect { groupStudents = it }
            }
            if (groupStudents.isEmpty()) EmptyBox(Icons.Outlined.Person, "Ushbu guruhda tinglovchilar mavjud emas", "Tinglovchilar tarmoq orqali kirganda qo'shiladi.")
            else SelectionGrid(groupStudents.map { SelectionItem(it.name, "Natijalarni ko'rish", Icons.Outlined.Person) { onStudentClick(it) } })
        }
        else -> {
            var allGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
            LaunchedEffect(subjectId) {
                AppContainer.localRepository.getAllGroups(subjectId).collect { allGroups = it }
            }
            if (allGroups.isEmpty()) EmptyBox(Icons.Outlined.Groups, "Guruhlar mavjud emas", "'Guruhlar' bo'limida guruh qo'shing.")
            else SelectionGrid(allGroups.map { SelectionItem(it.name, "Tinglovchilarni ko'rish", Icons.Outlined.Groups) { onGroupClick(it) } })
        }
    }
}

@Composable
fun RecentView(
    subjectId: Int,
    selectedDate: String?,
    onDateClick: (String) -> Unit,
    onDeleteRecord: (ExamRecord) -> Unit
) {
    var allRecords by remember { mutableStateOf<List<ExamRecord>>(emptyList()) }
    LaunchedEffect(subjectId) {
        AppContainer.localRepository.getAllExamRecords(subjectId).collect { allRecords = it }
    }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    if (selectedDate != null) {
        val records = allRecords.filter { sdf.format(Date(it.timestamp)) == selectedDate }
        RecordsList(records, onDeleteRecord)
    } else {
        val dates = allRecords.map { sdf.format(Date(it.timestamp)) }.distinct()
        if (dates.isEmpty()) EmptyBox(Icons.Outlined.Event, "Natijalar mavjud emas", "Tinglovchilar test topshirganda sanalar shu yerda ko'rinadi.")
        else SelectionGrid(dates.map { date -> SelectionItem(date, "${allRecords.count { sdf.format(Date(it.timestamp)) == date }} ta natija", Icons.Outlined.Event) { onDateClick(date) } })
    }
}

private class SelectionItem(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun SelectionGrid(items: List<SelectionItem>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(280.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.m),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
        verticalArrangement = Arrangement.spacedBy(Space.m)
    ) {
        itemsIndexed(items) { index, item ->
            SignalCard(onClick = item.onClick, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientTile(listOf(Signal.GradientPrimary, Signal.GradientCyanAmber, Signal.GradientAmberPrimary)[index % 3], size = 40.dp) {
                        Icon(item.icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Signal.Faint)
                }
            }
        }
    }
}

@Composable
fun RecordsList(records: List<ExamRecord>, onDeleteRecord: (ExamRecord) -> Unit) {
    if (records.isEmpty()) {
        EmptyBox(Icons.Outlined.History, "Natijalar topilmadi", "Bu yerda hali yozuv yo'q.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.m)
        ) {
            items(records, key = { it.id }) { record ->
                HistoryItem(record = record, onDelete = { onDeleteRecord(record) })
            }
        }
    }
}

@Composable
fun HistoryItem(record: ExamRecord, onDelete: () -> Unit) {
    val date = remember(record.timestamp) { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(record.timestamp)) }
    val percentage = if (record.totalQuestions > 0) (record.correctAnswers.toFloat() / record.totalQuestions * 100).toInt() else 0
    val (chipBg, chipFg) = when {
        percentage >= 80 -> Signal.SuccessContainer to Signal.OnSuccessContainer
        percentage >= 60 -> Signal.WarningContainer to Signal.OnWarningContainer
        else -> Signal.DangerContainer to Signal.OnDangerContainer
    }
    var expanded by remember { mutableStateOf(false) }

    SignalCard(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InitialsAvatar(record.studentName, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(record.studentName, style = MaterialTheme.typography.titleMedium, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(record.lessonTitle, style = MaterialTheme.typography.bodySmall, color = Signal.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusChip("$percentage%", chipBg, chipFg)
            SignalIconButton(Icons.Default.Delete, "O'chirish", onDelete, tint = Signal.Danger, size = 34.dp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            InfoColumn("Ball", "${record.correctAnswers} / ${record.totalQuestions}")
            InfoColumn("Vaqt", "%d:%02d".format(record.timeSpentSeconds / 60, record.timeSpentSeconds % 60))
            InfoColumn("Sana", date)
        }
        if (record.wrongDetails.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SignalButton(if (expanded) "Tafsilotlarni yopish" else "Xatolarni ko'rish", onClick = { expanded = !expanded }, style = ButtonStyle.Tonal, height = 36.dp)
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    record.wrongDetails.split("\n\n").forEach { errorBlock ->
                        Surface(color = Signal.Surface, shape = AppDesign.ComponentShape, border = BorderStroke(1.dp, Signal.Outline), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                errorBlock.split("\n").forEachIndexed { index, line ->
                                    val color = when {
                                        line.startsWith("To'g'ri") -> Signal.Success
                                        line.startsWith("Tanlangan") -> Signal.Danger
                                        else -> Signal.Ink
                                    }
                                    Text(line, style = if (index == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, color = color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = Signal.Ink)
    }
}
