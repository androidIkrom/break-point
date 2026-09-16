package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.di.AppContainer
import com.example.poststudy.data.util.TestParser
import com.example.poststudy.domain.model.*
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.HelpIcon
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    subjectId: Int,
    lessonToEdit: Lesson? = null,
    onSaveComplete: () -> Unit,
    onBack: () -> Unit
) {
    val initialSettings = remember { runBlocking { AppContainer.localRepository.getSettings(subjectId).first() } }

    var title by remember { mutableStateOf(lessonToEdit?.title ?: "") }
    var presentationPath by remember { mutableStateOf(lessonToEdit?.presentationPath ?: initialSettings.presentationPath) }
    var testPath by remember { mutableStateOf(lessonToEdit?.testPath ?: initialSettings.testPath) }
    var testWarnings by remember { mutableStateOf<List<String>>(emptyList()) }

    var slideTimerMin by remember { mutableStateOf(( (lessonToEdit?.slideTimerSeconds ?: initialSettings.slideTimerSeconds) / 60 ).toString()) }
    var testTimerMin by remember { mutableStateOf(( (lessonToEdit?.testTimerSeconds ?: initialSettings.testTimerSeconds) / 60 ).toString()) }

    var selectedMode by remember { mutableStateOf(lessonToEdit?.mode ?: initialSettings.mode) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val presentationLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("ppt", "pptx")),
        mode = PickerMode.Single
    ) { file: PlatformFile? ->
        file?.path?.let { presentationPath = it }
    }

    val testLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("doc", "docx")),
        mode = PickerMode.Single
    ) { file: PlatformFile? ->
        file?.path?.let {
            testPath = it
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
            onSaveComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    if (isReady) showSaveDialog = true
                    true
                } else false
            },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (lessonToEdit == null) "Yangi dars" else "Darsni tahrirlash", fontWeight = FontWeight.Black, color = Color(0xFF1E293B)) },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    actions = {
                        HelpIcon(
                            title = "Dars sozlamalari",
                            helpText = "Bu yerda dars nomi, taqdimot va test fayllarini tanlang. Taymerlar kamida 5 daqiqa bo'lishi kerak.",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 1000.dp),
                    shape = AppDesign.CardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    border = BorderStroke(3.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(56.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                        SettingsSection(title = "Dars nomi") {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("masalan, Rim tarixi") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppDesign.ComponentShape,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    focusedLabelColor = Color(0xFF6366F1)
                                )
                            )
                        }

                        SettingsSection(title = "Dars turi") {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ModeCard(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    title = "Prezentatsiya + test",
                                    description = "Guruhga biriktirishda rejim tanlanadi",
                                    isSelected = selectedMode == LessonMode.ReAppropriation,
                                    onClick = { selectedMode = LessonMode.ReAppropriation }
                                )
                                ModeCard(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    title = "Faqat prezentatsiya",
                                    description = "Test fayli kerak emas",
                                    isSelected = selectedMode == LessonMode.PresentationOnly,
                                    onClick = { selectedMode = LessonMode.PresentationOnly }
                                )
                                ModeCard(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    title = "Faqat test",
                                    description = "Prezentatsiya kerak emas",
                                    isSelected = selectedMode == LessonMode.TestOnly,
                                    onClick = { selectedMode = LessonMode.TestOnly }
                                )
                            }
                        }

                        if (selectedMode.hasSlides) {
                            SettingsSection(title = "Prezentatsiya fayli") {
                                FilePickerRow(
                                    path = presentationPath,
                                    label = "PowerPoint fayli (.ppt, .pptx)",
                                    onSelect = { presentationLauncher.launch() }
                                )
                            }
                        }

                        if (selectedMode.hasTest) SettingsSection(title = "Test fayli") {
                            FilePickerRow(
                                path = testPath,
                                label = "Word hujjati (.doc, .docx)",
                                onSelect = { testLauncher.launch() }
                            )

                            if (testWarnings.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFFFF7ED),
                                    shape = AppDesign.ComponentShape,
                                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Ogohlantirishlar:", fontWeight = FontWeight.Bold, color = Color(0xFFC2410C), style = MaterialTheme.typography.labelMedium)
                                        testWarnings.forEach { warning ->
                                            Text("• $warning", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9A3412))
                                        }
                                    }
                                }
                            }
                        }

                        SettingsSection(title = "Taymerlar (kamida 5 daqiqa)") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                if (selectedMode.hasSlides) {
                                    OutlinedTextField(
                                        value = slideTimerMin,
                                        onValueChange = { if (it.all { c -> c.isDigit() }) slideTimerMin = it },
                                        label = { Text("O'rganish (daq)") },
                                        isError = slideTimerMin.isNotEmpty() && (slideTimerMin.toIntOrNull() ?: 0) < 5,
                                        modifier = Modifier.weight(1f),
                                        shape = AppDesign.ComponentShape,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF6366F1),
                                            focusedLabelColor = Color(0xFF6366F1)
                                        )
                                    )
                                }

                                if (selectedMode.hasTest) OutlinedTextField(
                                    value = testTimerMin,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) testTimerMin = it },
                                    label = { Text("Test (daq)") },
                                    isError = testTimerMin.isNotEmpty() && (testTimerMin.toIntOrNull() ?: 0) < 5,
                                    modifier = Modifier.weight(1f),
                                    shape = AppDesign.ComponentShape,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF6366F1),
                                        focusedLabelColor = Color(0xFF6366F1)
                                    )
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { saveSettings() },
                    enabled = isReady && !isSaving,
                    modifier = Modifier.width(400.dp).height(72.dp).padding(bottom = 32.dp),
                    shape = AppDesign.ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                ) {
                    Text("DARSNI SAQLASH", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (showSaveDialog) {
        PostStudyDialog(
            onDismissRequest = { showSaveDialog = false },
            title = "Sozlamalarni saqlash",
            text = "Ushbu o'zgarishlarni saqlab, darslar ro'yxatiga qaytishni xohlaysizmi?",
            confirmText = "Ha, Saqlash",
            dismissText = "Bekor qilish",
            confirmColor = Color(0xFF6366F1), // Indigo to match settings theme
            onConfirm = {
                showSaveDialog = false
                saveSettings()
            }
        )
    }
}

@Composable
fun ModeCard(
    modifier: Modifier,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 120.dp),
        shape = AppDesign.ComponentShape,
        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
        border = if (isSelected) BorderStroke(4.dp, Color(0xFF6366F1)) else BorderStroke(2.dp, Color(0xFFE2E8F0)),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = if (isSelected) Color(0xFF6366F1) else Color(0xFF64748B), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF6366F1), // Indigo header
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun FilePickerRow(path: String, label: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val file = remember(path) { if (path.isBlank()) null else java.io.File(path) }
        val missing = file != null && !file.exists()
        OutlinedTextField(
            value = file?.name.orEmpty(),
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text("Fayl tanlanmagan", color = Color(0xFF94A3B8)) },
            supportingText = when {
                file == null -> null
                missing -> { { Text("Fayl topilmadi: ${file.path}", color = Color(0xFFB91C1C)) } }
                else -> { { Text(file.parent.orEmpty(), color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
            },
            isError = missing,
            modifier = Modifier.weight(1f),
            readOnly = true,
            singleLine = true,
            shape = AppDesign.ComponentShape,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedLabelColor = Color(0xFF6366F1),
                unfocusedLabelColor = Color(0xFF64748B)
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onSelect,
            shape = AppDesign.ComponentShape,
            modifier = Modifier.height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White), // Emerald Action
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text("TANLASH", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        }
    }
}
