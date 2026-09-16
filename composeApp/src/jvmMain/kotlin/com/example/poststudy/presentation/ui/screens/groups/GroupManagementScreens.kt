package com.example.poststudy.presentation.ui.screens.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.AssignmentKind
import com.example.poststudy.domain.model.Group
import com.example.poststudy.domain.model.GroupOverview
import com.example.poststudy.domain.model.Student
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.presentation.ui.components.PasswordReveal
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import com.example.poststudy.presentation.ui.components.hoverEffect
import com.example.poststudy.presentation.ui.screens.admin.EmptyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DangerRed = Color(0xFFEF4444)
private val HeaderGreen = Color(0xFF065F46)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    subjectId: Int,
    onGroupSelected: (Group) -> Unit,
    onBack: () -> Unit
) {
    val repo = AppContainer.localRepository
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<GroupOverview>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var groupToRename by remember { mutableStateOf<GroupOverview?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupOverview?>(null) }

    fun reload() {
        scope.launch {
            groups = withContext(Dispatchers.IO) { repo.getGroupOverviews(subjectId) }
            isLoading = false
        }
    }

    LaunchedEffect(subjectId) { reload() }

    if (showAddDialog || groupToRename != null) {
        val editing = groupToRename
        GroupFormDialog(
            title = if (editing == null) "Yangi guruh" else "Guruhni tahrirlash",
            initialName = editing?.name ?: "",
            initialPassword = editing?.password.orEmpty(),
            onDismiss = { showAddDialog = false; groupToRename = null },
            onConfirm = { name, password ->
                showAddDialog = false
                groupToRename = null
                scope.launch {
                    withContext(Dispatchers.IO) {
                        if (editing == null) {
                            repo.addGroup(name, subjectId, password).first()
                        } else {
                            repo.updateGroup(editing.group.copy(name = name))
                            repo.setGroupPassword(editing.id, password)
                        }
                    }
                    reload()
                }
            }
        )
    }

    groupToDelete?.let { group ->
        PostStudyDialog(
            onDismissRequest = { groupToDelete = null },
            title = "Guruhni o'chirish",
            text = "'${group.name}' guruhi va undagi barcha tinglovchilar o'chiriladi. Bu amalni qaytarib bo'lmaydi.",
            confirmText = "O'chirish",
            confirmColor = DangerRed,
            onConfirm = {
                groupToDelete = null
                scope.launch {
                    withContext(Dispatchers.IO) { repo.deleteGroup(group.id) }
                    reload()
                }
            }
        )
    }

    ManagementScaffold(
        title = "Guruhlar",
        onBack = onBack,
        onAdd = { showAddDialog = true },
        addDescription = "Guruh qo'shish"
    ) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppDesign.Violet)
            }
            groups.isEmpty() -> EmptyState("Guruhlar hali qo'shilmagan. Yuqoridagi + tugmasini bosing.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 360.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                itemsIndexed(groups, key = { _, g -> g.id }) { index, group ->
                    ManagementCard(
                        title = group.name,
                        subtitle = (if (group.isAssigned) {
                            "${group.assignmentTitle} • ${assignmentLabel(group.kind!!, group.mode)}"
                        } else "Mashg'ulot biriktirilmagan") + if (group.isLocked) " • parolli" else " • PAROL YO'Q",
                        icon = Icons.Default.Groups,
                        color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size],
                        onClick = { onGroupSelected(group.group) },
                        onEdit = { groupToRename = group },
                        onDelete = { groupToDelete = group }
                    )
                }
            }
        }
    }
}

private data class StudentStats(val attempts: Int, val averagePercent: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    group: Group,
    onBack: () -> Unit
) {
    val repo = AppContainer.localRepository
    val scope = rememberCoroutineScope()
    var students by remember { mutableStateOf<List<Pair<Student, StudentStats>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var overview by remember { mutableStateOf<GroupOverview?>(null) }
    var showAssignmentDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    fun reloadAssignment() {
        scope.launch {
            overview = withContext(Dispatchers.IO) {
                repo.getGroupOverviews(group.subjectId).firstOrNull { it.id == group.id }
            }
        }
    }

    LaunchedEffect(group.id) { reloadAssignment() }

    val currentOverview = overview
    if (showPasswordDialog && currentOverview != null) {
        GroupPasswordDialog(
            groupName = currentOverview.name,
            currentPassword = currentOverview.password.orEmpty(),
            onDismiss = { showPasswordDialog = false },
            onSave = { password ->
                showPasswordDialog = false
                scope.launch {
                    withContext(Dispatchers.IO) { repo.setGroupPassword(currentOverview.id, password) }
                    reloadAssignment()
                }
            }
        )
    }
    if (showAssignmentDialog && currentOverview != null) {
        GroupAssignmentDialog(
            group = currentOverview,
            onDismiss = { showAssignmentDialog = false },
            onSaved = {
                showAssignmentDialog = false
                reloadAssignment()
            }
        )
    }

    fun reload() {
        scope.launch {
            students = withContext(Dispatchers.IO) {
                repo.getStudentsByGroup(group.id).first().map { student ->
                    val records = repo.getStudentRecords(student.id).first().filter { it.totalQuestions > 0 }
                    val avg = if (records.isEmpty()) 0
                    else records.map { it.correctAnswers * 100 / it.totalQuestions }.average().toInt()
                    student to StudentStats(records.size, avg)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(group.id) { reload() }

    if (showAddDialog) {
        NameDialog(
            title = "Yangi tinglovchi",
            text = "Tinglovchining to'liq ismini kiriting:",
            label = "To'liq ism",
            initialValue = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                showAddDialog = false
                scope.launch {
                    withContext(Dispatchers.IO) { repo.addStudent(name, group.id).first() }
                    reload()
                }
            }
        )
    }

    studentToDelete?.let { student ->
        PostStudyDialog(
            onDismissRequest = { studentToDelete = null },
            title = "Tinglovchini o'chirish",
            text = "'${student.name}' va uning barcha natijalari o'chiriladi. Bu amalni qaytarib bo'lmaydi.",
            confirmText = "O'chirish",
            confirmColor = DangerRed,
            onConfirm = {
                studentToDelete = null
                scope.launch {
                    withContext(Dispatchers.IO) { repo.deleteStudent(student.id) }
                    reload()
                }
            }
        )
    }

    ManagementScaffold(
        title = group.name,
        onBack = onBack,
        onAdd = { showAddDialog = true },
        addDescription = "Tinglovchi qo'shish"
    ) {
        // One scrolling grid: the header cards scroll away with the students on small screens
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 32.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AssignmentHeroCard(overview = overview, onEdit = { showAssignmentDialog = true })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GroupPasswordCard(overview = overview, onEdit = { showPasswordDialog = true })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Tinglovchilar (${students.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = HeaderGreen,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            when {
                isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppDesign.Violet)
                    }
                }
                students.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Bu guruhda tinglovchilar yo'q. Ular tarmoq orqali ulanganda ham qo'shiladi.",
                            color = Color(0xFF64748B)
                        )
                    }
                }
                else -> itemsIndexed(students, key = { _, s -> s.first.id }) { index, (student, stats) ->
                    ManagementCard(
                        title = student.name,
                        subtitle = if (stats.attempts == 0) "Hali test topshirmagan"
                        else "${stats.attempts} ta urinish • o'rtacha ${stats.averagePercent}%",
                        icon = Icons.Default.Person,
                        color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size],
                        onClick = null,
                        onEdit = null,
                        onDelete = { studentToDelete = student }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagementScaffold(
    title: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    addDescription: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(title, color = HeaderGreen, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    actions = {
                        Button(
                            onClick = onAdd,
                            modifier = Modifier.padding(end = 16.dp),
                            shape = AppDesign.ComponentShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Violet)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(addDescription, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                content()
            }
        }
    }
}

@Composable
private fun ManagementCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val cardContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.background(color.copy(alpha = 0.1f), CircleShape)) {
                    Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", tint = color)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.background(DangerRed.copy(alpha = 0.1f), CircleShape)) {
                Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = DangerRed)
            }
        }
    }

    val modifier = Modifier.fillMaxWidth().hoverEffect(scale = 1.02f, yOffset = -4f)
    val border = BorderStroke(3.dp, color.copy(alpha = 0.5f))
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = AppDesign.CardShape,
            color = Color.White,
            border = border,
            shadowElevation = 8.dp,
            content = cardContent
        )
    } else {
        Surface(
            modifier = modifier,
            shape = AppDesign.CardShape,
            color = Color.White,
            border = border,
            shadowElevation = 8.dp,
            content = cardContent
        )
    }
}

@Composable
private fun GroupPasswordCard(overview: GroupOverview?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val locked = overview?.isLocked == true
    val accent = if (locked) AppDesign.Violet else DangerRed
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppDesign.ComponentShape,
        color = Color.White,
        border = BorderStroke(2.dp, accent.copy(alpha = 0.4f)),
        shadowElevation = 4.dp
    ) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text("Guruh paroli:", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(Modifier.width(12.dp))
            PasswordReveal(overview?.password, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (!locked && overview != null) {
                Text(
                    "Istalgan tinglovchi bu guruhga kira oladi",
                    color = DangerRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            OutlinedButton(
                onClick = onEdit,
                enabled = overview != null,
                shape = AppDesign.ComponentShape,
                border = BorderStroke(2.dp, accent)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text(if (locked) "Parolni o'zgartirish" else "Parol qo'yish", color = accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Large card at the top of a group: what its students get right now. */
@Composable
private fun AssignmentHeroCard(overview: GroupOverview?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val assigned = overview?.isAssigned == true
    val accent = when {
        overview == null -> Color(0xFF94A3B8)
        !assigned -> Color(0xFFF59E0B)
        overview.kind == AssignmentKind.Exam -> AppDesign.Indigo
        else -> AppDesign.Emerald
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppDesign.CardShape,
        color = Color.White,
        border = BorderStroke(4.dp, accent.copy(alpha = 0.6f)),
        shadowElevation = 10.dp
    ) {
        Row(Modifier.padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(88.dp).background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (assigned) kindIcon(overview!!.kind!!) else Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.width(28.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Hozir biriktirilgan mashg'ulot",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        overview == null -> "Yuklanmoqda..."
                        assigned -> overview.assignmentTitle.orEmpty()
                        overview.assignmentMissing -> "Biriktirilgan mashg'ulot o'chirilgan"
                        else -> "Hech narsa biriktirilmagan"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = if (assigned) Color(0xFF1E293B) else accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (assigned) {
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            assignmentLabel(overview!!.kind!!, overview.mode),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = accent,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else if (overview != null) {
                    Text(
                        "Bu guruh tinglovchilari hozircha mashg'ulot boshlay olmaydi.",
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Button(
                onClick = onEdit,
                enabled = overview != null,
                modifier = Modifier.height(56.dp),
                shape = AppDesign.ComponentShape,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (assigned) "O'zgartirish" else "Biriktirish", fontWeight = FontWeight.Black)
            }
        }
    }
}

/** Teacher sets or changes a group password; an empty value removes it. */
@Composable
fun GroupPasswordDialog(
    groupName: String,
    currentPassword: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember { mutableStateOf(currentPassword) }
    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = "'$groupName' paroli",
        text = "Tinglovchilar bu guruhga kirishda shu parolni kiritadi. Maydonni bo'sh qoldirsangiz, parol olib tashlanadi.",
        confirmText = "Saqlash",
        onConfirm = { onSave(value.trim()) },
        content = { PasswordField(value, onValueChange = { value = it }) }
    )
}

@Composable
private fun GroupFormDialog(
    title: String,
    initialName: String,
    initialPassword: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, password: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var password by remember { mutableStateOf(initialPassword) }
    var error by remember { mutableStateOf<String?>(null) }
    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = "Guruh nomi va tinglovchilar kirishi uchun parolni kiriting.",
        confirmText = "Saqlash",
        onConfirm = {
            val trimmedName = name.trim().replace(Regex("\\s+"), " ")
            val trimmedPassword = password.trim()
            error = when {
                trimmedName.isEmpty() -> "Guruh nomini kiriting"
                trimmedPassword.isEmpty() -> "Parolni kiriting"
                else -> null
            }
            if (error == null) onConfirm(trimmedName, trimmedPassword)
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it; error = null },
                    label = { Text("Guruh nomi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppDesign.ComponentShape
                )
                PasswordField(password, onValueChange = { password = it; error = null })
                error?.let { Text(it, color = DangerRed, fontWeight = FontWeight.Bold) }
            }
        }
    )
}

/** Plain text on purpose: the teacher needs to see what they type. */
@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 32 && '\n' !in it) onValueChange(it) },
        label = { Text("Guruh paroli") },
        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = AppDesign.ComponentShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppDesign.Violet,
            focusedLabelColor = AppDesign.Violet
        )
    )
}

/** Dialog with a single text field; confirm is ignored while the field is blank. */
@Composable
internal fun NameDialog(
    title: String,
    text: String,
    label: String,
    initialValue: String,
    confirmText: String = "Saqlash",
    maxLength: Int = 60,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = text,
        confirmText = confirmText,
        onConfirm = {
            val trimmed = value.trim().replace(Regex("\\s+"), " ")
            if (trimmed.isNotEmpty()) onConfirm(trimmed)
        },
        content = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= maxLength) value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = AppDesign.ComponentShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppDesign.Indigo,
                    focusedLabelColor = AppDesign.Indigo
                )
            )
        }
    )
}
