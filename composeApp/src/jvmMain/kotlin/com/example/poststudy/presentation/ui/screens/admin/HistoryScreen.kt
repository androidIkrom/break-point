package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.ExamRecord
import com.example.poststudy.domain.model.Group
import com.example.poststudy.domain.model.Student
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.hoverEffect
import com.example.poststudy.presentation.ui.components.HelpIcon
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(subjectId: Int, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(HistoryTab.GROUPS) }
    
    // Navigation states
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    var showClearAllDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<ExamRecord?>(null) }

    // Keyboard back handling
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
            confirmColor = Color(0xFFEF4444),
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
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                recordToDelete?.let {
                    AppContainer.localRepository.deleteExamRecord(it.id)
                }
                recordToDelete = null
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key) {
                        Key.Escape, Key.Backspace -> {
                            handleBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Imtihon tahlili", fontWeight = FontWeight.Black, color = Color(0xFF065F46))
                            if (selectedGroup != null || selectedDate != null) {
                                val subtitle = when {
                                    selectedStudent != null -> "${selectedGroup?.name} > ${selectedStudent?.name}"
                                    selectedGroup != null -> selectedGroup?.name ?: ""
                                    selectedDate != null -> selectedDate ?: ""
                                    else -> ""
                                }
                                if (subtitle.isNotEmpty()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF065F46).copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        BackButton(onClick = handleBack)
                    },
                    actions = {
                        HelpIcon(
                            title = "Imtihon tarixi",
                            helpText = "Bu yerda barcha topshirilgan testlar natijalarini ko'rishingiz mumkin. Guruhlar bo'yicha yoki so'nggi natijalar bo'yicha saralashingiz mumkin.",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hammasini tozalash", tint = Color(0xFFEF4444))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                // Tab Switcher
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .height(56.dp)
                        .clip(AppDesign.ComponentShape)
                        .background(Color.White.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HistoryTabItem(
                        modifier = Modifier.weight(1f),
                        text = "Guruhlar",
                        isSelected = selectedTab == HistoryTab.GROUPS,
                        onClick = { 
                            selectedTab = HistoryTab.GROUPS
                            selectedDate = null
                        }
                    )
                    HistoryTabItem(
                        modifier = Modifier.weight(1f),
                        text = "So'ngilar",
                        isSelected = selectedTab == HistoryTab.RECENT,
                        onClick = { 
                            selectedTab = HistoryTab.RECENT
                            selectedGroup = null
                            selectedStudent = null
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        HistoryTab.GROUPS -> {
                            GroupsView(
                                subjectId = subjectId,
                                selectedGroup = selectedGroup,
                                selectedStudent = selectedStudent,
                                onGroupClick = { selectedGroup = it },
                                onStudentClick = { selectedStudent = it },
                                onDeleteRecord = { recordToDelete = it }
                            )
                        }
                        HistoryTab.RECENT -> {
                            RecentView(
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
    }
}

enum class HistoryTab { GROUPS, RECENT }

@Composable
fun HistoryTabItem(modifier: Modifier, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(if (isSelected) Color(0xFF10B981) else Color.Transparent)
    val textColor by animateColorAsState(if (isSelected) Color.White else Color(0xFF64748B))
    
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight().hoverEffect(scale = 1.02f, yOffset = -4f),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontWeight = FontWeight.Black, color = textColor)
        }
    }
}

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
            StudentsList(groupStudents, onStudentClick)
        }
        else -> {
            var allGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
            LaunchedEffect(subjectId) {
                AppContainer.localRepository.getAllGroups(subjectId).collect { allGroups = it }
            }
            GroupsList(allGroups, onGroupClick)
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

    if (selectedDate != null) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val records = allRecords.filter { sdf.format(Date(it.timestamp)) == selectedDate }
        RecordsList(records, onDeleteRecord)
    } else {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = allRecords.map { sdf.format(Date(it.timestamp)) }.distinct()
        DatesList(dates, onDateClick)
    }
}

@Composable
fun GroupsList(groups: List<Group>, onGroupClick: (Group) -> Unit) {
    if (groups.isEmpty()) {
        EmptyState("Guruhlar mavjud emas")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                itemsIndexed(groups) { index, group ->
                    val color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size]
                    SelectionCard(group.name, "Tinglovchilarni ko'rish", Icons.Default.Groups, color) { onGroupClick(group) }
                }
        }
    }
}

@Composable
fun StudentsList(students: List<Student>, onStudentClick: (Student) -> Unit) {
    if (students.isEmpty()) {
        EmptyState("Ushbu guruhda tinglovchilar mavjud emas")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                itemsIndexed(students) { index, student ->
                    val color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size]
                    SelectionCard(student.name, "Natijalarni ko'rish", Icons.Default.Person, color) { onStudentClick(student) }
                }
        }
    }
}

@Composable
fun DatesList(dates: List<String>, onDateClick: (String) -> Unit) {
    if (dates.isEmpty()) {
        EmptyState("Natijalar mavjud emas")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                itemsIndexed(dates) { index, date ->
                    val color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size]
                    SelectionCard(date, "Shu kundagi natijalar", Icons.Default.Event, color) { onDateClick(date) }
                }
        }
    }
}

@Composable
fun RecordsList(records: List<ExamRecord>, onDeleteRecord: (ExamRecord) -> Unit) {
    if (records.isEmpty()) {
        EmptyState("Natijalar topilmadi")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                items(records) { record ->
                    HistoryItem(record = record, onDelete = { onDeleteRecord(record) })
                }
        }
    }
}

@Composable
fun SelectionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().hoverEffect(scale = 1.02f),
        shape = AppDesign.CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(4.dp, color.copy(alpha = 0.5f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = color.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B))
    }
}


@Composable
fun HistoryItem(record: ExamRecord, onDelete: () -> Unit) {
    val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    val percentage = if (record.totalQuestions > 0) (record.correctAnswers.toFloat() / record.totalQuestions * 100).toInt() else 0
    val scoreColor = when {
        percentage >= 80 -> Color(0xFF4ADE80)
        percentage >= 60 -> Color(0xFFFACC15)
        else -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth().hoverEffect(scale = 1.02f),
        shape = AppDesign.CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, scoreColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.studentName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = record.lessonTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4F46E5),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = scoreColor.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = BorderStroke(2.dp, scoreColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "$percentage%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = scoreColor
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = Color(0xFFEF4444))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 1.dp, color = Color(0xFFF1F5F9))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("Ball", "${record.correctAnswers} / ${record.totalQuestions}")
                
                val m = record.timeSpentSeconds / 60
                val s = record.timeSpentSeconds % 60
                InfoColumn("Vaqt", "%d:%02d".format(m, s))

                InfoColumn("Sana", date)
            }

            if (record.wrongDetails.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppDesign.ComponentShape,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(if (expanded) "Tafsilotlarni yopish" else "Xatolarni ko'rish", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                }
                
                if (expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    record.wrongDetails.split("\n\n").forEach { errorBlock ->
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = AppDesign.ComponentShape,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val lines = errorBlock.split("\n")
                                lines.forEachIndexed { index, line ->
                                    val color = when {
                                        line.startsWith("To'g'ri") -> Color(0xFF059669)
                                        line.startsWith("Tanlangan") -> Color(0xFFE11D48)
                                        else -> Color(0xFF1E293B)
                                    }
                                    val weight = if (index == 0) FontWeight.ExtraBold else FontWeight.Bold
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color,
                                        fontWeight = weight
                                    )
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
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
    }
}