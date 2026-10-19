package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.util.TestParser
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.AppMotion
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    subjectId: Int,
    lessonToEdit: Lesson? = null,
    onSaveComplete: () -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(lessonToEdit?.title ?: "") }
    var presentationPath by remember { mutableStateOf(lessonToEdit?.presentationPath ?: "") }
    var testPath by remember { mutableStateOf(lessonToEdit?.testPath ?: "") }
    var testWarnings by remember { mutableStateOf<List<String>>(emptyList()) }

    var slideTimerMin by remember { mutableStateOf(((lessonToEdit?.slideTimerSeconds ?: 300) / 60).toString()) }
    var testTimerMin by remember { mutableStateOf(((lessonToEdit?.testTimerSeconds ?: 1800) / 60).toString()) }

    var selectedMode by remember { mutableStateOf(lessonToEdit?.mode ?: LessonMode.ReAppropriation) }
    // True once the admin changed anything; leaving then needs a confirmation
    var touched by remember { mutableStateOf(false) }

    // A new lesson starts from the values of the last one, loaded off the UI thread
    LaunchedEffect(subjectId) {
        if (lessonToEdit != null) return@LaunchedEffect
        val last = withContext(Dispatchers.IO) { AppContainer.localRepository.getSettings(subjectId).first() }
        if (title.isBlank() && presentationPath.isEmpty() && testPath.isEmpty()) {
            presentationPath = last.presentationPath
            testPath = last.testPath
            slideTimerMin = (last.slideTimerSeconds / 60).toString()
            testTimerMin = (last.testTimerSeconds / 60).toString()
            selectedMode = last.mode
        }
    }
    var showSaveDialog by remember { mutableStateOf(false) }

    val presentationLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("ppt", "pptx")),
        mode = PickerMode.Single
    ) { file: PlatformFile? ->
        file?.path?.let { presentationPath = it; touched = true }
    }

    val testLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("doc", "docx")),
        mode = PickerMode.Single
    ) { file: PlatformFile? ->
        file?.path?.let {
            testPath = it
            touched = true
            val result = TestParser.parseTest(it)
            testWarnings = result.warnings
        }
    }

    val slideTimerVal = slideTimerMin.toIntOrNull() ?: 0
    val testTimerVal = testTimerMin.toIntOrNull() ?: 0

    val isReady = title.isNotBlank() &&
        (!selectedMode.hasSlides || (presentationPath.isNotEmpty() && slideTimerVal >= 5)) &&
        (!selectedMode.hasTest || (testPath.isNotEmpty() && testTimerVal >= 5))

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    val guard = LocalUnsavedGuard.current
    val dirty = rememberUpdatedState(touched && !isSaving)
    DisposableEffect(guard) {
        guard.isDirty = { dirty.value }
        onDispose { guard.isDirty = { false } }
    }
    var showLeaveDialog by remember { mutableStateOf(false) }
    val leave: () -> Unit = { if (dirty.value) showLeaveDialog = true else onBack() }

    val saveSettings: () -> Unit = save@{
        if (isSaving) return@save
        isSaving = true
        val lesson = Lesson(
            id = lessonToEdit?.id ?: 0,
            title = title.trim(),
            // Drop materials the chosen type does not use, so the lesson list does not warn about them
            presentationPath = if (selectedMode.hasSlides) presentationPath else "",
            testPath = if (selectedMode.hasTest) testPath else "",
            slideTimerSeconds = if (selectedMode.hasSlides) slideTimerVal * 60 else 0,
            testTimerSeconds = if (selectedMode.hasTest) testTimerVal * 60 else 0,
            mode = selectedMode,
            subjectId = subjectId
        )

        val lastPaths = presentationPath to testPath
        scope.launch {
            // Saving copies the files into the app folder, which can take a moment for big decks
            withContext(Dispatchers.IO) {
                if (lessonToEdit == null) {
                    AppContainer.localRepository.addLesson(lesson)
                } else {
                    AppContainer.localRepository.updateLesson(lesson)
                }
                // Remembered only to prefill the next new lesson
                AppContainer.localRepository.saveSettings(lastPaths.first, lastPaths.second, slideTimerVal, testTimerVal, selectedMode, title, 0, subjectId)
            }
            isSaving = false
            touched = false
            onSaveComplete()
        }
    }

    AdminPage(
        title = if (lessonToEdit == null) "Yangi dars" else "Darsni tahrirlash",
        eyebrow = "Darslar",
        onBack = leave,
        actions = {
            HelpIcon(
                title = "Dars sozlamalari",
                helpText = "Bu yerda dars nomi, taqdimot va test fayllarini tanlang. Taymerlar kamida 5 daqiqa bo'lishi kerak."
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                        if (isReady) showSaveDialog = true
                        true
                    } else false
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.page, vertical = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SignalCard(Modifier.widthIn(max = 880.dp).fillMaxWidth(), padding = 28.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    SettingsSection("Dars nomi", "Tinglovchilar ro'yxatda shu nomni ko'radi") {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; touched = true },
                            placeholder = { Text("masalan, Rim tarixi") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppDesign.SmallShape,
                            singleLine = true
                        )
                    }

                    SettingsSection("Dars turi", "Guruhga biriktirishda rejim tanlanadi") {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModeCard(Modifier.weight(1f).fillMaxHeight(), "Prezentatsiya + test", "Avval slaydlar, keyin test", selectedMode == LessonMode.ReAppropriation) { selectedMode = LessonMode.ReAppropriation; touched = true }
                            ModeCard(Modifier.weight(1f).fillMaxHeight(), "Faqat prezentatsiya", "Test fayli kerak emas", selectedMode == LessonMode.PresentationOnly) { selectedMode = LessonMode.PresentationOnly; touched = true }
                            ModeCard(Modifier.weight(1f).fillMaxHeight(), "Faqat test", "Prezentatsiya kerak emas", selectedMode == LessonMode.TestOnly) { selectedMode = LessonMode.TestOnly; touched = true }
                        }
                    }

                    if (selectedMode.hasSlides) {
                        SettingsSection("Prezentatsiya fayli", "PowerPoint (.ppt, .pptx)") {
                            FilePickerRow(path = presentationPath, label = "PowerPoint fayli", onSelect = { presentationLauncher.launch() })
                        }
                    }

                    if (selectedMode.hasTest) SettingsSection("Test fayli", "Word hujjati (.doc, .docx), to'g'ri javob yulduzcha bilan") {
                        FilePickerRow(path = testPath, label = "Word hujjati", onSelect = { testLauncher.launch() })
                        if (testWarnings.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            WarningsBox(testWarnings)
                        }
                    }

                    SettingsSection("Taymerlar", "Kamida 5 daqiqa") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (selectedMode.hasSlides) {
                                MinutesField("O'rganish (daqiqa)", slideTimerMin, { slideTimerMin = it; touched = true }, Modifier.weight(1f))
                            }
                            if (selectedMode.hasTest) {
                                MinutesField("Test (daqiqa)", testTimerMin, { testTimerMin = it; touched = true }, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Row(Modifier.widthIn(max = 880.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text("Enter — saqlash · Esc — orqaga", style = MaterialTheme.typography.bodySmall, color = Signal.Muted, modifier = Modifier.weight(1f))
                SignalButton("Bekor qilish", onClick = leave, style = ButtonStyle.Outlined, height = 46.dp)
                SignalButton(
                    if (isSaving) "Saqlanmoqda..." else "Darsni saqlash",
                    onClick = { saveSettings() },
                    enabled = isReady && !isSaving,
                    style = ButtonStyle.Gradient,
                    icon = Icons.Default.Check,
                    height = 46.dp
                )
            }
        }
    }

    if (showLeaveDialog) {
        PostStudyDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = "Saqlanmagan o'zgarishlar",
            text = "Dars hali saqlanmagan. Chiqib ketsangiz, kiritilgan ma'lumotlar yo'qoladi.",
            confirmText = "Baribir chiqish",
            dismissText = "Qolish",
            confirmColor = Signal.Danger,
            onConfirm = {
                showLeaveDialog = false
                touched = false
                onBack()
            }
        )
    }

    if (showSaveDialog) {
        PostStudyDialog(
            onDismissRequest = { showSaveDialog = false },
            title = "Sozlamalarni saqlash",
            text = "Ushbu o'zgarishlarni saqlab, darslar ro'yxatiga qaytishni xohlaysizmi?",
            confirmText = "Ha, saqlash",
            dismissText = "Bekor qilish",
            onConfirm = {
                showSaveDialog = false
                saveSettings()
            }
        )
    }
}

@Composable
fun ModeCard(modifier: Modifier, title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (isSelected) Signal.Container else Signal.Card, AppMotion.tweenOrSnap(AppMotion.fast), label = "modeBg")
    val border by animateColorAsState(if (isSelected) Signal.Primary else Signal.Outline, AppMotion.tweenOrSnap(AppMotion.fast), label = "modeBorder")
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 96.dp),
        shape = AppDesign.ComponentShape,
        color = bg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, border)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PopCheck(visible = isSelected, size = 20.dp)
                Text(title, style = MaterialTheme.typography.titleSmall, color = Signal.Ink)
            }
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
        }
    }
}

@Composable
fun SettingsSection(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Signal.Ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Signal.Muted)
        }
        content()
    }
}

@Composable
fun MinutesField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val invalid = value.isNotEmpty() && (value.toIntOrNull() ?: 0) < 5
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
        label = { Text(label) },
        isError = invalid,
        supportingText = if (invalid) ({ Text("Kamida 5 daqiqa") }) else null,
        modifier = modifier,
        shape = AppDesign.SmallShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
fun WarningsBox(warnings: List<String>, title: String = "Ogohlantirishlar") {
    Surface(color = Signal.WarningContainer, shape = AppDesign.ComponentShape, border = BorderStroke(1.dp, Signal.WarningBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Signal.Warning, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = Signal.OnWarningContainer)
            }
            warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = Signal.OnWarningContainer) }
        }
    }
}

@Composable
fun FilePickerRow(path: String, label: String, onSelect: () -> Unit) {
    val file = remember(path) { if (path.isBlank()) null else java.io.File(path) }
    val missing = file != null && !file.exists()
    Surface(
        shape = AppDesign.ComponentShape,
        color = if (missing) Signal.DangerContainer else Signal.Surface,
        border = BorderStroke(1.dp, if (missing) Signal.DangerBorder else Signal.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = AppDesign.SmallShape, color = if (missing) Signal.Danger.copy(alpha = 0.15f) else Signal.Container) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = if (missing) Signal.Danger else Signal.Primary, modifier = Modifier.size(20.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    file?.name ?: "Fayl tanlanmagan",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (file == null) Signal.Muted else Signal.Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        file == null -> label
                        missing -> "Fayl topilmadi: ${file.path}"
                        else -> file.parent.orEmpty()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (missing) Signal.Danger else Signal.Muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            SignalButton(if (file == null) "Tanlash" else "Almashtirish", onClick = onSelect, style = if (file == null) ButtonStyle.Primary else ButtonStyle.Tonal, height = 40.dp)
        }
    }
}
