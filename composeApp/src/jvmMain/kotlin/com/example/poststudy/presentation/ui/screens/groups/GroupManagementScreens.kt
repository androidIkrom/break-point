package com.example.poststudy.presentation.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PersonAdd
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
import com.example.poststudy.domain.model.AssignmentKind
import com.example.poststudy.domain.model.Group
import com.example.poststudy.domain.model.GroupOverview
import com.example.poststudy.domain.model.Student
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import com.example.poststudy.presentation.ui.screens.admin.AddCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            text = "'${group.name}' guruhi, undagi barcha tinglovchilar va ularning natijalari o'chiriladi. Bu amalni qaytarib bo'lmaydi.",
            confirmText = "O'chirish",
            confirmColor = Signal.Danger,
            onConfirm = {
                groupToDelete = null
                scope.launch {
                    withContext(Dispatchers.IO) { repo.deleteGroup(group.id) }
                    reload()
                }
            }
        )
    }

    AdminPage(
        title = "Guruhlar · ${groups.size}",
        eyebrow = "Tinglovchilar",
        subtitle = "Guruhni bosib tinglovchilar ro'yxatini oching",
        actions = { SignalButton("Guruh qo'shish", onClick = { showAddDialog = true }, icon = Icons.Default.Add, style = ButtonStyle.Gradient, height = 42.dp) }
    ) {
        when {
            isLoading -> LoadingBox()
            groups.isEmpty() -> EmptyBox(
                Icons.Outlined.Groups, "Guruhlar hali qo'shilmagan", "Guruh yarating, parol qo'ying va unga dars yoki imtihon biriktiring.",
                action = { SignalButton("Birinchi guruhni qo'shish", onClick = { showAddDialog = true }, icon = Icons.Default.Add, style = ButtonStyle.Gradient) }
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(Space.l),
                verticalArrangement = Arrangement.spacedBy(Space.l),
                contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.xl)
            ) {
                itemsIndexed(groups, key = { _, g -> g.id }) { index, group ->
                    GroupCard(group, index, onClick = { onGroupSelected(group.group) }, onEdit = { groupToRename = group }, onDelete = { groupToDelete = group })
                }
                itemsIndexed(listOf(Unit), key = { _, _ -> "add" }) { _, _ ->
                    AddCard("Yangi guruh", "Nom va kirish paroli bilan", onClick = { showAddDialog = true })
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: GroupOverview, index: Int, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val brush = listOf(Signal.GradientPrimary, Signal.GradientCyanAmber, Signal.GradientAmberPrimary)[index % 3]
    SignalCard(modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), onClick = onClick, padding = 18.dp, borderColor = if (group.isLocked) Signal.Outline else Signal.WarningBorder) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientTile(brush, size = 44.dp) {
                Text(group.name.filter { it.isDigit() }.take(2).ifBlank { group.name.take(1) }, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleLarge, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(group.subjectName, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
            }
            SignalIconButton(Icons.Default.Edit, "Tahrirlash", onEdit, size = 32.dp, iconSize = 15.dp)
            SignalIconButton(Icons.Default.Delete, "O'chirish", onDelete, tint = Signal.Danger, size = 32.dp, iconSize = 15.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (group.isAssigned) group.assignmentTitle.orEmpty() else if (group.assignmentMissing) "Biriktirilgan mashg'ulot o'chirilgan" else "Mashg'ulot biriktirilmagan",
            style = MaterialTheme.typography.titleSmall,
            color = if (group.isAssigned) Signal.Ink else if (group.assignmentMissing) Signal.Danger else Signal.Muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (group.isAssigned) Text(assignmentLabel(group.kind!!, group.mode), style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (group.isLocked) StatusChip("Parolli", Signal.Container, Signal.OnContainer, icon = Icons.Outlined.Lock, iconTint = Signal.Primary)
            else StatusChip("Parol yo'q", Signal.WarningContainer, Signal.OnWarningContainer, icon = Icons.Outlined.LockOpen, iconTint = Signal.Warning)
            Spacer(Modifier.weight(1f))
            Text("Tinglovchilar →", style = MaterialTheme.typography.labelMedium, color = Signal.Primary)
        }
    }
}

private data class StudentStats(val attempts: Int, val averagePercent: Int)

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
            confirmColor = Signal.Danger,
            onConfirm = {
                studentToDelete = null
                scope.launch {
                    withContext(Dispatchers.IO) { repo.deleteStudent(student.id) }
                    reload()
                }
            }
        )
    }

    AdminPage(
        title = group.name,
        eyebrow = "Guruh",
        subtitle = "${students.size} tinglovchi",
        onBack = onBack,
        actions = { SignalButton("Tinglovchi qo'shish", onClick = { showAddDialog = true }, icon = Icons.Outlined.PersonAdd, style = ButtonStyle.Primary, height = 42.dp) }
    ) {
        // One scrolling grid: the header cards scroll away with the students on small screens
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.m),
            contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.xl)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.l)) {
                    AssignmentHeroCard(overview = overview, onEdit = { showAssignmentDialog = true }, modifier = Modifier.weight(1.6f).fillMaxHeight())
                    GroupPasswordCard(overview = overview, onEdit = { showPasswordDialog = true }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Tinglovchilar · ${students.size}", style = MaterialTheme.typography.headlineSmall, color = Signal.Ink, modifier = Modifier.padding(top = 8.dp))
            }
            when {
                isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Signal.Primary, trackColor = Signal.Container)
                    }
                }
                students.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    SignalCard(padding = 20.dp) {
                        Text("Bu guruhda tinglovchilar yo'q", style = MaterialTheme.typography.titleMedium, color = Signal.Ink)
                        Text("Ular tarmoq orqali ulanganda avtomatik qo'shiladi yoki yuqoridagi tugma bilan qo'shing.", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
                    }
                }
                else -> itemsIndexed(students, key = { _, s -> s.first.id }) { _, (student, stats) ->
                    SignalCard(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InitialsAvatar(student.name, size = 40.dp)
                            Column(Modifier.weight(1f)) {
                                Text(student.name, style = MaterialTheme.typography.titleSmall, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (stats.attempts == 0) "Hali test topshirmagan" else "${stats.attempts} ta urinish · o'rtacha ${stats.averagePercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (stats.attempts == 0) Signal.Muted else when { stats.averagePercent >= 80 -> Signal.Success; stats.averagePercent >= 60 -> Signal.Warning; else -> Signal.Danger }
                                )
                            }
                            SignalIconButton(Icons.Default.Delete, "O'chirish", { studentToDelete = student }, tint = Signal.Danger, size = 32.dp, iconSize = 15.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupPasswordCard(overview: GroupOverview?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val locked = overview?.isLocked == true
    SignalCard(modifier = modifier, padding = 18.dp, borderColor = if (locked || overview == null) Signal.Outline else Signal.WarningBorder, color = if (locked || overview == null) Signal.Card else Signal.WarningContainer.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, contentDescription = null, tint = if (locked) Signal.Primary else Signal.Warning, modifier = Modifier.size(22.dp))
            Text("Guruh paroli", style = MaterialTheme.typography.titleMedium, color = Signal.Ink)
        }
        Spacer(Modifier.height(10.dp))
        PasswordReveal(overview?.password, style = MaterialTheme.typography.headlineSmall)
        if (!locked && overview != null) {
            Text("Istalgan tinglovchi bu guruhga kira oladi", style = MaterialTheme.typography.bodySmall, color = Signal.Warning)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        SignalButton(if (locked) "Parolni o'zgartirish" else "Parol qo'yish", onClick = onEdit, enabled = overview != null, icon = Icons.Outlined.Key, style = if (locked) ButtonStyle.Outlined else ButtonStyle.Primary, height = 40.dp)
    }
}

/** Large card at the top of a group: what its students get right now. */
@Composable
private fun AssignmentHeroCard(overview: GroupOverview?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val assigned = overview?.isAssigned == true
    SignalCard(modifier = modifier, padding = 0.dp) {
        LessonCover(if (overview?.kind == AssignmentKind.Exam) 1 else 0, Modifier.fillMaxWidth().height(80.dp), amber = overview?.kind == AssignmentKind.Exam, danger = overview?.assignmentMissing == true) {
            Box(Modifier.fillMaxSize().padding(12.dp)) {
                StatusChip("Hozir biriktirilgan mashg'ulot", Color.White.copy(alpha = 0.14f), Color.White)
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    if (assigned) kindIcon(overview!!.kind!!) else Icons.Outlined.EventBusy,
                    contentDescription = null,
                    tint = if (assigned) Signal.Primary else Signal.Warning,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = when {
                        overview == null -> "Yuklanmoqda..."
                        assigned -> overview.assignmentTitle.orEmpty()
                        overview.assignmentMissing -> "Biriktirilgan mashg'ulot o'chirilgan"
                        else -> "Hech narsa biriktirilmagan"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (assigned) Signal.Ink else Signal.Warning,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            if (assigned) {
                StatusChip(assignmentLabel(overview!!.kind!!, overview.mode), Signal.Container, Signal.OnContainer)
            } else if (overview != null) {
                Text("Bu guruh tinglovchilari hozircha mashg'ulot boshlay olmaydi.", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
            }
            Spacer(Modifier.height(4.dp))
            SignalButton(if (assigned) "O'zgartirish" else "Biriktirish", onClick = onEdit, enabled = overview != null, icon = Icons.Default.Edit, style = if (assigned) ButtonStyle.Tonal else ButtonStyle.Gradient, height = 40.dp)
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
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }
    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = "$groupName paroli",
        text = "Tinglovchilar bu guruhga kirishda shu parolni kiritadi. Maydonni bo'sh qoldirsangiz, parol olib tashlanadi.",
        confirmText = "Saqlash",
        onConfirm = { onSave(value.trim()) },
        content = { PasswordField(value, onValueChange = { value = it }, modifier = Modifier.focusRequester(fieldFocus)) }
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
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }
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
                    modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
                    shape = AppDesign.SmallShape
                )
                PasswordField(password, onValueChange = { password = it; error = null })
                error?.let { Text(it, color = Signal.Danger, style = MaterialTheme.typography.labelLarge) }
            }
        }
    )
}

/** Plain text on purpose: the teacher needs to see what they type. */
@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 32 && '\n' !in it) onValueChange(it) },
        label = { Text("Guruh paroli") },
        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = AppDesign.SmallShape
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
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }
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
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
                shape = AppDesign.SmallShape
            )
        }
    )
}
