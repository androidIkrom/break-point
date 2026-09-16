package com.example.poststudy.presentation.ui.screens.admin

import com.example.poststudy.presentation.ui.components.AdaptiveGrid
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.AssignmentKind
import com.example.poststudy.domain.model.Group
import com.example.poststudy.domain.model.GroupOverview
import com.example.poststudy.presentation.ui.screens.groups.GroupAssignmentDialog
import com.example.poststudy.presentation.ui.screens.groups.GroupPasswordDialog
import com.example.poststudy.presentation.ui.components.PasswordReveal
import com.example.poststudy.domain.model.LessonMode
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.NetworkServerCard
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import com.example.poststudy.presentation.ui.components.ThreeStepVerificationDialog
import com.example.poststudy.data.session.AppServer
import androidx.compose.material.icons.automirrored.filled.Logout
import com.example.poststudy.presentation.ui.components.hoverEffect

@OptIn(ExperimentalMaterial3Api::class)
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

    if (showLogoutDialog) {
        ThreeStepVerificationDialog(
            title = "Admin hisobidan chiqish",
            warning = "Chiqqaningizdan keyin admin rejimi qulflanadi va qayta kirish uchun maxfiy kalit kerak bo'ladi. Tarmoq serveri to'xtatiladi. Fanlar, darslar va sozlamalar saqlanib qoladi.",
            confirmWord = "CHIQISH",
            finalButtonText = "Chiqish",
            color = AppDesign.Amber,
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
            color = Color(0xFFDC2626),
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
    var groupToEdit by remember { mutableStateOf<GroupOverview?>(null) }
    var passwordGroup by remember { mutableStateOf<GroupOverview?>(null) }

    fun reloadGroups() {
        scope.launch {
            groups = withContext(Dispatchers.IO) { AppContainer.localRepository.getGroupOverviews(subjectId) }
        }
    }

    LaunchedEffect(subjectId) { reloadGroups() }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(600.dp)
                .offset(x = (-150).dp, y = (-150).dp)
                .background(Color(0xFF10B981).copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 150.dp)
                .background(Color(0xFF3B82F6).copy(alpha = 0.05f), CircleShape)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(subjectName, color = Color(0xFF065F46), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    actions = {
                        // Leave room for the help icon drawn over the top-right corner
                        Row(Modifier.padding(end = 72.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                shape = AppDesign.ComponentShape,
                                border = BorderStroke(2.dp, AppDesign.Amber)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AppDesign.Amber)
                                Spacer(Modifier.width(8.dp))
                                Text("Chiqish", color = AppDesign.Amber, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626))
                                Spacer(Modifier.width(4.dp))
                                Text("Hisobni o'chirish", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Xush kelibsiz, ${adminName?.takeIf { it.isNotBlank() } ?: "admin"}",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = onEditAdminName,
                            modifier = Modifier.background(AppDesign.Emerald.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Ismni o'zgartirish", tint = AppDesign.Emerald)
                        }
                    }

                    NetworkServerCard(modifier = Modifier.padding(top = 20.dp).widthIn(max = 900.dp).fillMaxWidth())

                    Text(
                        text = "Bugun o'quv jarayonini qanday boshqaramiz?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                AdaptiveGrid(
                    items = listOf(
                        HomeItem("Darslar", "Materiallarni boshqarish", Icons.AutoMirrored.Filled.MenuBook, AppDesign.Amber, onNavigateToLessons),
                        HomeItem("Imtihon", "Test sinovlarini o'tkazish", Icons.Default.Assignment, AppDesign.Indigo, onNavigateToExam),
                        HomeItem("Guruhlar", "Tinglovchilar va guruhlar", Icons.Default.Groups, AppDesign.Violet, onNavigateToGroups),
                        HomeItem("Monitoring", "Natijalar statistikasi", Icons.Default.Analytics, AppDesign.Sky, onNavigateToMonitoring),
                        HomeItem("Tahlil", "Natijalarni ko'rish", Icons.AutoMirrored.Filled.List, AppDesign.Emerald, onNavigateToHistory)
                    ),
                    minItemWidth = 200.dp,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 1200.dp).padding(top = 16.dp)
                ) { item, itemModifier ->
                    HomeCard(itemModifier, item.title, item.subtitle, item.icon, item.color, item.onClick)
                }

                Spacer(modifier = Modifier.height(32.dp))

                GroupAssignmentsTable(
                    groups = groups,
                    onEdit = { groupToEdit = it },
                    onEditPassword = { passwordGroup = it },
                    onManageGroups = onNavigateToGroups,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 1200.dp)
                )
            }
        }
    }
}

private class HomeItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 170.dp)
            .hoverEffect()
            .clickable { onClick() },
        shape = AppDesign.ComponentShape,
        color = Color.White,
        border = BorderStroke(4.dp, color.copy(alpha = 0.8f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
    val headerColor = Color(0xFF065F46)
    val muted = Color(0xFF64748B)
    Surface(
        modifier = modifier,
        shape = AppDesign.CardShape,
        color = Color.White,
        border = BorderStroke(3.dp, AppDesign.Emerald.copy(alpha = 0.3f)),
        shadowElevation = 12.dp
    ) {
        Column(Modifier.padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Guruhlar va mashg'ulotlar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = headerColor
                    )
                    Text(
                        "Har bir guruh tinglovchilari kirganda o'z mashg'ulotini oladi",
                        color = muted
                    )
                }
                OutlinedButton(onClick = onManageGroups, shape = AppDesign.ComponentShape) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = AppDesign.Violet)
                    Spacer(Modifier.width(8.dp))
                    Text("Guruhlarni boshqarish", color = AppDesign.Violet, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))

            // Header row
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeader("Guruh", Modifier.weight(1.2f))
                TableHeader("Mashg'ulot", Modifier.weight(2f))
                TableHeader("Turi", Modifier.weight(0.9f))
                TableHeader("Rejim", Modifier.weight(1.3f))
                TableHeader("Parol", Modifier.weight(1.2f))
                Spacer(Modifier.width(96.dp))
            }

            when {
                groups == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppDesign.Emerald)
                }
                groups.isEmpty() -> Text(
                    "Bu fanda guruhlar yo'q. 'Guruhlarni boshqarish' orqali guruh qo'shing.",
                    color = muted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)
                )
                else -> groups.forEachIndexed { index, g ->
                    if (index > 0) HorizontalDivider(color = Color(0xFFF1F5F9))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onEdit(g) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            g.name,
                            modifier = Modifier.weight(1.2f),
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                g.isAssigned -> g.assignmentTitle.orEmpty()
                                g.assignmentMissing -> "O'chirilgan mashg'ulot"
                                else -> "Biriktirilmagan"
                            },
                            modifier = Modifier.weight(2f),
                            fontWeight = if (g.isAssigned) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                g.isAssigned -> Color(0xFF1E293B)
                                g.assignmentMissing -> Color(0xFFEF4444)
                                else -> Color(0xFF94A3B8)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(Modifier.weight(0.9f)) {
                            if (g.isAssigned) {
                                val kindColor = if (g.kind == AssignmentKind.Exam) AppDesign.Indigo else AppDesign.Amber
                                TableChip(g.kind!!.label, kindColor)
                            }
                        }
                        Box(Modifier.weight(1.3f)) {
                            if (g.isAssigned) {
                                TableChip(if (g.kind == AssignmentKind.Exam) "Test" else g.mode?.label.orEmpty(), AppDesign.Emerald)
                            }
                        }
                        PasswordReveal(g.password, modifier = Modifier.weight(1.2f))
                        IconButton(
                            onClick = { onEditPassword(g) },
                            modifier = Modifier.size(40.dp).background(AppDesign.Violet.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = "Parolni o'zgartirish", tint = AppDesign.Violet)
                        }
                        Spacer(Modifier.width(16.dp))
                        IconButton(
                            onClick = { onEdit(g) },
                            modifier = Modifier.size(40.dp).background(AppDesign.Emerald.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "O'zgartirish", tint = AppDesign.Emerald)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, fontWeight = FontWeight.Black, color = Color(0xFF475569), style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun TableChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = CircleShape) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}
