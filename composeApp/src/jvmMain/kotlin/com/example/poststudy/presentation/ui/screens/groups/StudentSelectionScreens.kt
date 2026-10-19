package com.example.poststudy.presentation.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.GroupOverview
import com.example.poststudy.domain.model.Student
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Loading / error / content state for lists fetched from the admin computer. */
private sealed interface RemoteState<out T> {
    data object Loading : RemoteState<Nothing>
    data class Failed(val message: String) : RemoteState<Nothing>
    data class Loaded<T>(val data: T) : RemoteState<T>
}

val StudentSteps = listOf("Guruh", "Ism", "Brifing", "Mashg'ulot")

/**
 * Student picks their group. [loadGroups] talks either to the admin computer or to the local
 * database; groups without an assignment are shown but cannot be picked.
 */
@Composable
fun GroupSelectionScreen(
    sourceLabel: String,
    loadGroups: suspend () -> Result<List<GroupOverview>>,
    verifyPassword: suspend (group: GroupOverview, password: String) -> Result<Unit>,
    onGroupSelected: (group: GroupOverview, password: String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<RemoteState<List<GroupOverview>>>(RemoteState.Loading) }
    var lockedGroup by remember { mutableStateOf<GroupOverview?>(null) }

    lockedGroup?.let { group ->
        GroupPasswordPrompt(
            group = group,
            verify = { password -> verifyPassword(group, password) },
            onDismiss = { lockedGroup = null },
            onVerified = { password ->
                lockedGroup = null
                onGroupSelected(group, password)
            }
        )
    }

    fun load() {
        state = RemoteState.Loading
        scope.launch {
            val result = withContext(Dispatchers.IO) { loadGroups() }
            state = result.fold(
                onSuccess = { RemoteState.Loaded(it) },
                onFailure = { RemoteState.Failed(it.message ?: "Guruhlarni yuklab bo'lmadi.") }
            )
        }
    }

    LaunchedEffect(Unit) { load() }

    StudentFlowScaffold(
        eyebrow = "Bilim testi",
        title = "Guruhingizni tanlang",
        subtitle = "$sourceLabel. Har bir guruh o'z mashg'ulotini oladi.",
        step = 0,
        onBack = onBack,
        onRefresh = ::load
    ) {
        when (val s = state) {
            RemoteState.Loading -> LoadingBox()
            is RemoteState.Failed -> ErrorBox(s.message, onRetry = ::load)
            is RemoteState.Loaded -> if (s.data.isEmpty()) {
                EmptyBox(Icons.Default.Groups, "Guruhlar yo'q", "Admin 'Guruhlar' bo'limida guruh qo'shishi kerak.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(300.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.xl),
                    horizontalArrangement = Arrangement.spacedBy(Space.l),
                    verticalArrangement = Arrangement.spacedBy(Space.l)
                ) {
                    itemsIndexed(s.data, key = { _, g -> g.id }) { index, group ->
                        StudentGroupCard(
                            group = group,
                            index = index,
                            onClick = {
                                if (group.isLocked) lockedGroup = group else onGroupSelected(group, "")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentGroupCard(group: GroupOverview, index: Int, onClick: () -> Unit) {
    val enabled = group.isAssigned
    val brush = listOf(Signal.GradientPrimary, Signal.GradientCyanAmber, Signal.GradientAmberPrimary)[index % 3]
    SignalCard(
        modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp),
        onClick = if (enabled) onClick else null,
        color = if (enabled) Signal.Card else Signal.Surface,
        padding = 20.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (enabled) {
                GradientTile(brush, size = 44.dp) {
                    Text(group.name.filter { it.isDigit() }.take(2).ifBlank { group.name.take(1) }, style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            } else {
                Box(Modifier.size(44.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Signal.Faint)
                }
            }
            Column {
                Text(group.name, style = MaterialTheme.typography.titleLarge, color = if (enabled) Signal.Ink else Signal.Muted)
                Text(group.subjectName, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        if (enabled) {
            Text(group.assignmentTitle.orEmpty(), style = MaterialTheme.typography.titleSmall, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(assignmentLabel(group.kind!!, group.mode), style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
        } else {
            Text("Hali mashg'ulot biriktirilmagan", style = MaterialTheme.typography.bodyMedium, color = Signal.Faint)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(if (group.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = if (group.isLocked) "Parolli" else null, tint = Signal.Muted, modifier = Modifier.size(14.dp))
            Text(if (group.isLocked) "Parol bilan" else "Parolsiz", style = MaterialTheme.typography.labelMedium, color = Signal.Muted)
            if (enabled) {
                Spacer(Modifier.weight(1f))
                Text("Kirish →", style = MaterialTheme.typography.labelMedium, color = Signal.Primary)
            }
        }
    }
}

/** Asks a student for the group password and checks it before letting them in. */
@Composable
private fun GroupPasswordPrompt(
    group: GroupOverview,
    verify: suspend (String) -> Result<Unit>,
    onDismiss: () -> Unit,
    onVerified: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val submit: () -> Unit = {
        if (!isChecking && password.isNotEmpty()) {
            isChecking = true
            error = null
            val typed = password
            scope.launch {
                val result = withContext(Dispatchers.IO) { verify(typed) }
                isChecking = false
                result
                    .onSuccess { onVerified(typed) }
                    .onFailure {
                        error = it.message ?: "Parolni tekshirib bo'lmadi."
                        password = ""
                    }
            }
        }
    }

    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = "${group.name} guruhi",
        text = "Guruhga kirish uchun o'qituvchi bergan parolni kiriting.",
        confirmText = if (isChecking) "Tekshirilmoqda..." else "Kirish",
        onConfirm = submit,
        content = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Guruh paroli") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "Yashirish" else "Ko'rsatish"
                            )
                        }
                    },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !isChecking,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape = AppDesign.SmallShape
                )
                error?.let {
                    Text(it, color = Signal.Danger, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    )
}

@Composable
fun StudentSelectionScreen(
    group: GroupOverview,
    loadStudents: suspend () -> Result<List<Student>>,
    createStudent: suspend (name: String) -> Result<Int>,
    onStudentSelected: (Student) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<RemoteState<List<Student>>>(RemoteState.Loading) }
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    fun load() {
        state = RemoteState.Loading
        scope.launch {
            val result = withContext(Dispatchers.IO) { loadStudents() }
            state = result.fold(
                onSuccess = { RemoteState.Loaded(it.sortedBy { s -> s.name.lowercase() }) },
                onFailure = { RemoteState.Failed(it.message ?: "Tinglovchilarni yuklab bo'lmadi.") }
            )
        }
    }

    LaunchedEffect(group.id) { load() }

    if (showAddDialog) {
        NameDialog(
            title = "Ro'yxatda yo'qmisiz?",
            text = "To'liq ismingizni kiriting. Siz '${group.name}' guruhiga qo'shilasiz.",
            label = "To'liq ism",
            initialValue = query,
            confirmText = "Qo'shilish",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                showAddDialog = false
                isCreating = true
                createError = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { createStudent(name) }
                    isCreating = false
                    result
                        .onSuccess { id -> onStudentSelected(Student(id, name, group.id)) }
                        .onFailure { createError = it.message ?: "Ro'yxatga qo'shib bo'lmadi." }
                }
            }
        )
    }

    StudentFlowScaffold(
        eyebrow = group.name,
        title = "Kim siz?",
        subtitle = "${group.subjectName} · ${group.assignmentTitle.orEmpty()}. Ro'yxatdan ismingizni tanlang yoki yangi ism kiriting.",
        step = 1,
        onBack = onBack,
        onRefresh = ::load
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = Space.page)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Ism bo'yicha qidirish") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Signal.Muted) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).widthIn(max = 480.dp),
                    shape = AppDesign.SmallShape
                )
                SignalButton(
                    text = if (isCreating) "Qo'shilmoqda..." else "Ro'yxatda yo'qman",
                    onClick = { showAddDialog = true },
                    enabled = !isCreating,
                    icon = Icons.Default.PersonAdd,
                    style = ButtonStyle.Primary,
                    height = 48.dp
                )
            }

            createError?.let {
                Text(it, color = Signal.Danger, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val s = state) {
                    RemoteState.Loading -> LoadingBox()
                    is RemoteState.Failed -> ErrorBox(s.message, onRetry = ::load)
                    is RemoteState.Loaded -> {
                        val filtered = s.data.filter { it.name.contains(query.trim(), ignoreCase = true) }
                        if (filtered.isEmpty()) {
                            EmptyBox(
                                Icons.Default.PersonSearch,
                                if (s.data.isEmpty()) "Guruhda hali tinglovchilar yo'q" else "Bunday ism topilmadi",
                                if (s.data.isEmpty()) "'Ro'yxatda yo'qman' tugmasini bosing." else "Qidiruvni o'zgartiring yoki yangi ism qo'shing."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(260.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = Space.xl),
                                horizontalArrangement = Arrangement.spacedBy(Space.m),
                                verticalArrangement = Arrangement.spacedBy(Space.m)
                            ) {
                                itemsIndexed(filtered, key = { _, st -> st.id }) { _, student ->
                                    SignalCard(modifier = Modifier.fillMaxWidth(), onClick = { onStudentSelected(student) }, padding = 14.dp) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            InitialsAvatar(student.name, size = 40.dp)
                                            Column(Modifier.weight(1f)) {
                                                Text(student.name, style = MaterialTheme.typography.titleSmall, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("Bu menman", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Signal.Faint)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Light workspace with a dark signal band on top carrying the title and the four-step header.
 */
@Composable
fun StudentFlowScaffold(
    eyebrow: String,
    title: String,
    subtitle: String,
    step: Int,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            SignalBackground(alpha = 0.85f)
            Column(Modifier.fillMaxSize().padding(horizontal = Space.page, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack, onDark = true, modifier = Modifier.padding(start = 0.dp))
                    Spacer(Modifier.weight(1f))
                    StepHeader(StudentSteps, step, onDark = true)
                    if (onRefresh != null) {
                        Spacer(Modifier.width(16.dp))
                        SignalIconButton(Icons.Outlined.Refresh, "Yangilash", onRefresh, tint = Signal.OnDark, container = Signal.GlassFill, borderColor = Signal.GlassBorderStrong, size = 40.dp, iconSize = 18.dp, onDark = true)
                    }
                }
                Spacer(Modifier.weight(1f))
                Eyebrow(eyebrow, color = Signal.Amber)
                Text(title, style = MaterialTheme.typography.displaySmall, color = Signal.OnDark)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Signal.OnDarkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = Space.contentMaxWidth + Space.page * 2).fillMaxSize()) { content() }
        }
    }
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Signal.Primary, trackColor = Signal.Container)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = Signal.Danger, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, color = Signal.OnDangerContainer, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 600.dp))
            Spacer(Modifier.height(20.dp))
            SignalButton("Qayta urinish", onClick = onRetry, icon = Icons.Outlined.Refresh)
        }
    }
}

@Composable
fun EmptyBox(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(56.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                Surface(shape = AppDesign.ComponentShape, color = Signal.SurfaceContainer) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Signal.Muted, modifier = Modifier.size(26.dp)) }
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = Signal.Ink, textAlign = TextAlign.Center)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Signal.Muted, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
            if (action != null) { Spacer(Modifier.height(8.dp)); action() }
        }
    }
}
