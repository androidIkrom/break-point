package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.session.AppServer
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.AssignmentKind
import com.example.poststudy.domain.model.ExamRecord
import com.example.poststudy.domain.model.GroupOverview
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import com.example.poststudy.presentation.ui.screens.groups.GroupAssignmentDialog
import com.example.poststudy.presentation.ui.screens.groups.GroupPasswordDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
fun TeacherHomeScreen(
    subjectId: Int,
    subjectName: String,
    onNavigateToLessons: () -> Unit,
    onNavigateToExam: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMonitoring: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit,
    onBack: () -> Unit,
    adminName: String? = null,
    onEditAdminName: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        ThreeStepVerificationDialog(
            title = "Admin hisobidan chiqish",
            warning = "Chiqqaningizdan keyin admin rejimi qulflanadi va qayta kirish uchun maxfiy kalit kerak bo'ladi. Tarmoq serveri to'xtatiladi. Fanlar, darslar va sozlamalar saqlanib qoladi.",
            confirmWord = "CHIQISH",
            finalButtonText = "Chiqish",
            color = Signal.Warning,
            onDismiss = { showLogoutDialog = false },
            onConfirmed = {
                showLogoutDialog = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        AppServer.stop()
                        AppContainer.localRepository.lockAdmin()
                    }
                    onLogout()
                }
            }
        )
    }

    if (showDeleteDialog) {
        ThreeStepVerificationDialog(
            title = "Hisobni butunlay o'chirish",
            warning = "Admin hisobi va BARCHA ma'lumotlar o'chiriladi: fanlar, darslar, imtihonlar, guruhlar, tinglovchilar va natijalar. Bu amalni qaytarib bo'lmaydi!",
            confirmWord = "O'CHIRISH",
            finalButtonText = "Butunlay o'chirish",
            color = Signal.Danger,
            onDismiss = { showDeleteDialog = false },
            onConfirmed = {
                showDeleteDialog = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        AppServer.stop()
                        AppContainer.localRepository.deleteAccount()
                    }
                    onAccountDeleted()
                }
            }
        )
    }

    var groups by remember { mutableStateOf<List<GroupOverview>?>(null) }
    var records by remember { mutableStateOf<List<ExamRecord>>(emptyList()) }
    var studentCount by remember { mutableStateOf(0) }
    var groupToEdit by remember { mutableStateOf<GroupOverview?>(null) }
    var passwordGroup by remember { mutableStateOf<GroupOverview?>(null) }

    fun reloadGroups() {
        scope.launch {
            val repo = AppContainer.localRepository
            val loaded = withContext(Dispatchers.IO) { repo.getGroupOverviews(subjectId) }
            groups = loaded
            studentCount = withContext(Dispatchers.IO) {
                loaded.sumOf { repo.getStudentsByGroup(it.id).first().size }
            }
        }
    }

    LaunchedEffect(subjectId) {
        reloadGroups()
        AppContainer.localRepository.getAllExamRecords(subjectId).collect { records = it.sortedByDescending { r -> r.timestamp } }
    }

    passwordGroup?.let { group ->
        GroupPasswordDialog(
            groupName = group.name,
            currentPassword = group.password.orEmpty(),
            onDismiss = { passwordGroup = null },
            onSave = { password ->
                passwordGroup = null
                scope.launch {
                    withContext(Dispatchers.IO) { AppContainer.localRepository.setGroupPassword(group.id, password) }
                    reloadGroups()
                }
            }
        )
    }

    groupToEdit?.let { group ->
        GroupAssignmentDialog(
            group = group,
            onDismiss = { groupToEdit = null },
            onSaved = {
                groupToEdit = null
                reloadGroups()
            }
        )
    }

    val serverRunning by AppContainer.networkRepository.serverRunning.collectAsState()
    val today = LocalDate.now()
    val todayRecords = records.filter { Date(it.timestamp).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today }
    val avg = records.filter { it.totalQuestions > 0 }.map { it.correctAnswers * 100 / it.totalQuestions }.average().takeIf { !it.isNaN() }?.toInt()
    val unlockedGroups = groups.orEmpty().count { !it.isLocked }

    Column(Modifier.fillMaxSize().background(Signal.Surface).verticalScroll(rememberScrollState())) {
        // Dark hero with the live signal backdrop
        Box(Modifier.fillMaxWidth()) {
            SignalBackground(Modifier.matchParentSize(), alpha = 0.8f)
            Column(Modifier.fillMaxWidth().padding(horizontal = Space.page, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignalButton("Fanlar", onClick = onBack, style = ButtonStyle.Glass, icon = Icons.AutoMirrored.Filled.ArrowBack, height = 36.dp, contentPadding = PaddingValues(start = 12.dp, end = 14.dp))
                    SignalButton(subjectName, onClick = onBack, style = ButtonStyle.Glass, trailingIcon = Icons.Default.KeyboardArrowDown, height = 36.dp, contentPadding = PaddingValues(start = 14.dp, end = 10.dp))
                    Text("${groups?.size ?: 0} guruh · $studentCount tinglovchi", style = MaterialTheme.typography.bodySmall, color = Signal.OnDarkMuted)
                    Spacer(Modifier.weight(1f))
                    if (serverRunning) GlassPill("Tarmoq yoqilgan", live = true)
                    Box {
                        SignalButton(adminName?.takeIf { it.isNotBlank() } ?: "admin", onClick = { accountMenu = true }, style = ButtonStyle.Glass, trailingIcon = Icons.Default.KeyboardArrowDown, height = 36.dp, contentPadding = PaddingValues(start = 12.dp, end = 10.dp))
                        DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }, shape = AppDesign.ComponentShape) {
                            DropdownMenuItem(text = { Text("Username'ni o'zgartirish") }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }, onClick = { accountMenu = false; onEditAdminName() })
                            DropdownMenuItem(text = { Text("Chiqish") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Signal.Warning) }, onClick = { accountMenu = false; showLogoutDialog = true })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Hisobni o'chirish", color = Signal.Danger) }, leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Signal.Danger) }, onClick = { accountMenu = false; showDeleteDialog = true })
                        }
                    }
                }

                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Eyebrow(uzbekDate(today), color = Signal.Amber)
                        Text("Xush kelibsiz, ${adminName?.takeIf { it.isNotBlank() } ?: "admin"}", style = MaterialTheme.typography.headlineLarge, color = Signal.OnDark)
                        Text(
                            buildString {
                                append("Bugun ${todayRecords.size} ta urinish")
                                avg?.let { append(", o'rtacha $it%") }
                                if (unlockedGroups > 0) append(". $unlockedGroups ta guruhga parol qo'yilmagan.") else append(".")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Signal.OnDarkMuted
                        )
                    }
                    Row(Modifier.widthIn(max = 480.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiTile("Bugungi urinishlar", "${todayRecords.size}", Modifier.weight(1f), onDark = true)
                        KpiTile("O'rtacha natija", avg?.let { "$it%" } ?: "—", Modifier.weight(1f), onDark = true, valueColor = Signal.Cyan)
                        KpiTile("Tinglovchilar", "$studentCount", Modifier.weight(1f), onDark = true)
                    }
                }
            }
        }

        // Network control sits right under the hero, on the light workspace
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            NetworkServerCard(
                modifier = Modifier.widthIn(max = Space.contentMaxWidth + Space.page * 2).fillMaxWidth().padding(start = Space.page, end = Space.page, top = Space.xl)
            )
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Row(
                Modifier.widthIn(max = Space.contentMaxWidth + Space.page * 2).fillMaxWidth().padding(start = Space.page, end = Space.page, top = 18.dp, bottom = Space.xl),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                GroupAssignmentsTable(
                    groups = groups,
                    onEdit = { groupToEdit = it },
                    onEditPassword = { passwordGroup = it },
                    onManageGroups = onNavigateToGroups,
                    modifier = Modifier.weight(2f)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SignalCard(padding = 16.dp) {
                        Text("Bo'limlar", style = MaterialTheme.typography.titleMedium, color = Signal.Ink)
                        Spacer(Modifier.height(6.dp))
                        QuickAction("Darslar", "Materiallarni boshqarish", Icons.AutoMirrored.Outlined.MenuBook, Signal.GradientPrimary, onNavigateToLessons)
                        QuickAction("Imtihon", "Test sinovlarini o'tkazish", Icons.Outlined.Assignment, Signal.GradientCyanAmber, onNavigateToExam)
                        QuickAction("Guruhlar", "Tinglovchilar va guruhlar", Icons.Outlined.Groups, Signal.GradientAmberPrimary, onNavigateToGroups)
                        QuickAction("Monitoring", "Natijalar statistikasi", Icons.Outlined.BarChart, Signal.GradientPrimary, onNavigateToMonitoring)
                        QuickAction("Tahlil", "Natijalarni ko'rish", Icons.Outlined.History, Signal.GradientCyanAmber, onNavigateToHistory)
                    }
                    RecentResultsCard(records, onNavigateToMonitoring)
                }
            }
        }
    }
}

private fun uzbekDate(date: LocalDate): String {
    val days = listOf("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba", "Yakshanba")
    val months = listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentyabr", "oktyabr", "noyabr", "dekabr")
    return "${days[date.dayOfWeek.value - 1]}, ${date.dayOfMonth} ${months[date.monthValue - 1]}"
}

@Composable
private fun QuickAction(title: String, subtitle: String, icon: ImageVector, brush: Brush, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shift by androidx.compose.animation.core.animateDpAsState(if (hovered) 4.dp else 0.dp, label = "shift")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AppDesign.ComponentShape)
            .background(if (hovered) Signal.Container else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .offset(x = shift),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GradientTile(brush, size = 36.dp) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Signal.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
        }
    }
}

@Composable
private fun RecentResultsCard(records: List<ExamRecord>, onOpenMonitoring: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    SignalCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("So'nggi natijalar", style = MaterialTheme.typography.titleMedium, color = Signal.Ink, modifier = Modifier.weight(1f))
            SignalButton("Monitoring", onClick = onOpenMonitoring, style = ButtonStyle.Text, height = 32.dp, contentPadding = PaddingValues(horizontal = 8.dp))
        }
        val recent = records.take(12).reversed().filter { it.totalQuestions > 0 }.map { it.correctAnswers * 100f / it.totalQuestions }
        if (recent.size >= 2) {
            Spacer(Modifier.height(8.dp))
            Sparkline(recent, Modifier.fillMaxWidth().height(44.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (records.isEmpty()) {
            Text("Hali natijalar yo'q. Tinglovchilar test topshirganda shu yerda ko'rinadi.", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
        } else {
            records.take(4).forEach { r ->
                val pct = if (r.totalQuestions > 0) r.correctAnswers * 100 / r.totalQuestions else 0
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InitialsAvatar(r.studentName, size = 28.dp)
                    Column(Modifier.weight(1f)) {
                        Text(r.studentName, style = MaterialTheme.typography.titleSmall, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${r.lessonTitle} · ${timeFormat.format(Date(r.timestamp))}", style = MaterialTheme.typography.bodySmall, color = Signal.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("$pct%", style = MaterialTheme.typography.labelLarge, color = when { pct >= 80 -> Signal.Success; pct >= 60 -> Signal.Warning; else -> Signal.Danger })
                }
            }
        }
    }
}

/** Which lesson or exam each group of the subject has, editable per row. */
@Composable
private fun GroupAssignmentsTable(
    groups: List<GroupOverview>?,
    onEdit: (GroupOverview) -> Unit,
    onEditPassword: (GroupOverview) -> Unit,
    onManageGroups: () -> Unit,
    modifier: Modifier = Modifier
) {
    SignalCard(modifier = modifier, padding = 0.dp) {
        Row(Modifier.padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Guruhlar va mashg'ulotlar", style = MaterialTheme.typography.headlineSmall, color = Signal.Ink)
                Text("Har bir guruh tinglovchilari kirganda o'z mashg'ulotini oladi. Qatorni bosib o'zgartiring.", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
            }
            SignalButton("Guruhlarni boshqarish", onClick = onManageGroups, style = ButtonStyle.Text, height = 36.dp, contentPadding = PaddingValues(horizontal = 10.dp))
        }

        Row(
            Modifier.fillMaxWidth().background(Signal.SurfaceContainer).padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableHeader("Guruh", Modifier.weight(1.1f))
            TableHeader("Mashg'ulot", Modifier.weight(2f))
            TableHeader("Turi", Modifier.weight(1f))
            TableHeader("Parol", Modifier.weight(1.3f))
            Spacer(Modifier.width(88.dp))
        }

        when {
            groups == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Signal.Primary, trackColor = Signal.Container)
            }
            groups.isEmpty() -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bu fanda guruhlar yo'q", style = MaterialTheme.typography.titleMedium, color = Signal.Ink)
                Text("'Guruhlarni boshqarish' orqali guruh qo'shing.", style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
                Spacer(Modifier.height(12.dp))
                SignalButton("Guruh qo'shish", onClick = onManageGroups, height = 40.dp)
            }
            else -> groups.forEachIndexed { index, g ->
                if (index > 0) HorizontalDivider(color = Signal.SurfaceContainer)
                val rowInteraction = remember { MutableInteractionSource() }
                val rowHovered by rowInteraction.collectIsHoveredAsState()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (!g.isLocked) Signal.WarningContainer.copy(alpha = 0.5f) else if (rowHovered) Signal.Surface else Color.Transparent)
                        .clickable(interactionSource = rowInteraction, indication = null) { onEdit(g) }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(g.name, modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.titleSmall, color = Signal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Column(Modifier.weight(2f)) {
                        Text(
                            text = when {
                                g.isAssigned -> g.assignmentTitle.orEmpty()
                                g.assignmentMissing -> "O'chirilgan mashg'ulot"
                                else -> "Biriktirilmagan"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = when { g.isAssigned -> Signal.Ink; g.assignmentMissing -> Signal.Danger; else -> Signal.Muted },
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (g.isAssigned) {
                            Text(if (g.kind == AssignmentKind.Exam) "Test" else g.mode?.label.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        if (g.isAssigned) {
                            if (g.kind == AssignmentKind.Exam) StatusChip("Imtihon", Signal.WarningContainer, Signal.OnWarningContainer)
                            else StatusChip("Dars", Signal.Container, Signal.OnContainer)
                        }
                    }
                    Box(Modifier.weight(1.3f)) {
                        if (g.isLocked) PasswordReveal(g.password)
                        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = Signal.Warning, modifier = Modifier.size(14.dp))
                            Text("Parol yo'q", style = MaterialTheme.typography.labelLarge, color = Signal.Warning)
                        }
                    }
                    Row(Modifier.width(88.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                        SignalIconButton(Icons.Outlined.Key, "Parolni o'zgartirish", { onEditPassword(g) }, tint = if (g.isLocked) Signal.Muted else Signal.Warning, size = 34.dp)
                        SignalIconButton(Icons.Default.Edit, "O'zgartirish", { onEdit(g) }, tint = Signal.Primary, container = Signal.Container, borderColor = null, size = 34.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelSmall, color = Signal.Muted)
}

/** Thin line chart with a soft fill, used for recent results. */
@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: Color = Signal.Primary) {
    androidx.compose.foundation.Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = 0f
        val max = 100f
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v -> androidx.compose.ui.geometry.Offset(i * stepX, size.height - (v - min) / (max - min) * size.height) }
        val line = androidx.compose.ui.graphics.Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val fill = androidx.compose.ui.graphics.Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Signal.Cyan.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
