package com.example.poststudy.presentation.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.GroupOverview
import com.example.poststudy.domain.model.Student
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.presentation.ui.screens.admin.EmptyState
import com.example.poststudy.presentation.ui.screens.admin.SelectionCard
import com.example.poststudy.presentation.ui.components.hoverEffect
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HeaderGreen = Color(0xFF065F46)

/** Loading / error / content state for lists fetched from the admin computer. */
private sealed interface RemoteState<out T> {
    data object Loading : RemoteState<Nothing>
    data class Failed(val message: String) : RemoteState<Nothing>
    data class Loaded<T>(val data: T) : RemoteState<T>
}

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

    SelectionScaffold(
        title = "Guruhingizni tanlang",
        subtitle = sourceLabel,
        onBack = onBack,
        onRefresh = ::load
    ) {
        when (val s = state) {
            RemoteState.Loading -> LoadingBox()
            is RemoteState.Failed -> ErrorBox(s.message, onRetry = ::load)
            is RemoteState.Loaded -> if (s.data.isEmpty()) {
                EmptyState("Guruhlar yo'q. Admin 'Guruhlar' bo'limida guruh qo'shishi kerak.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 900.dp),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(s.data, key = { _, g -> g.id }) { index, group ->
                        StudentGroupCard(
                            group = group,
                            color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size],
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
private fun StudentGroupCard(group: GroupOverview, color: Color, onClick: () -> Unit) {
    val enabled = group.isAssigned
    val accent = if (enabled) color else Color(0xFF94A3B8)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().then(if (enabled) Modifier.hoverEffect(scale = 1.02f, yOffset = -4f) else Modifier),
        shape = AppDesign.CardShape,
        color = if (enabled) Color.White else Color(0xFFF8FAFC),
        border = BorderStroke(3.dp, accent.copy(alpha = 0.5f)),
        shadowElevation = if (enabled) 6.dp else 0.dp
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = accent)
                    if (group.isLocked) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Lock, contentDescription = "Parolli", tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
                Text(group.subjectName, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
            }
            if (enabled) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        group.assignmentTitle.orEmpty(),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )
                    Text(assignmentLabel(group.kind!!, group.mode), color = color, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Hali mashg'ulot biriktirilmagan", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
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
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
        title = "'${group.name}' guruhi",
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
                    shape = AppDesign.ComponentShape
                )
                error?.let {
                    Text(
                        it,
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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

    SelectionScaffold(
        title = "Ismingizni tanlang",
        subtitle = "${group.subjectName} • ${group.name} • ${group.assignmentTitle.orEmpty()}",
        onBack = onBack,
        onRefresh = ::load
    ) {
        Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 900.dp).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Ism bo'yicha qidirish") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = AppDesign.ComponentShape
                )
                Button(
                    onClick = { showAddDialog = true },
                    enabled = !isCreating,
                    modifier = Modifier.height(56.dp),
                    shape = AppDesign.ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Indigo)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ro'yxatda yo'qman", fontWeight = FontWeight.Bold)
                    }
                }
            }

            createError?.let {
                Text(
                    it,
                    color = Color(0xFFB91C1C),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val s = state) {
                    RemoteState.Loading -> LoadingBox()
                    is RemoteState.Failed -> ErrorBox(s.message, onRetry = ::load)
                    is RemoteState.Loaded -> {
                        val filtered = s.data.filter { it.name.contains(query.trim(), ignoreCase = true) }
                        if (filtered.isEmpty()) {
                            EmptyState(
                                if (s.data.isEmpty()) "Guruhda hali tinglovchilar yo'q. 'Ro'yxatda yo'qman' tugmasini bosing."
                                else "Bunday ism topilmadi."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(filtered, key = { _, st -> st.id }) { index, student ->
                                    SelectionCard(
                                        title = student.name,
                                        subtitle = "Bu menman",
                                        icon = Icons.Default.Person,
                                        color = AppDesign.RainbowPalette[index % AppDesign.RainbowPalette.size]
                                    ) { onStudentSelected(student) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
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
                    title = {
                        Column {
                            Text(title, color = HeaderGreen, fontWeight = FontWeight.Black)
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = HeaderGreen.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    actions = {
                        IconButton(onClick = onRefresh, modifier = Modifier.padding(end = 16.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = AppDesign.Emerald)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                content()
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppDesign.Indigo)
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                color = Color(0xFF7F1D1D),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 600.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = AppDesign.ComponentShape,
                colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Indigo)
            ) {
                Text("Qayta urinish", fontWeight = FontWeight.Bold)
            }
        }
    }
}
