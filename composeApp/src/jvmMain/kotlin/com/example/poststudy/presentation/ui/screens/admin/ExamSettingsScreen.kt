package com.example.poststudy.presentation.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.poststudy.data.util.TestParser
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.Exam
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.theme.Space
import com.example.poststudy.presentation.ui.components.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExamSettingsScreen(
    subjectId: Int,
    examToEdit: Exam? = null,
    onSaveComplete: () -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(examToEdit?.title ?: "") }
    var testPath by remember { mutableStateOf(examToEdit?.testPath ?: "") }
    var totalQuestionsInFile by remember { mutableStateOf(0) }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var testCountPerStudent by remember { mutableStateOf(examToEdit?.questionsPerStudent?.toString() ?: "10") }
    var testTimerMin by remember { mutableStateOf((examToEdit?.testTimerSeconds?.div(60))?.toString() ?: "30") }
    var errorMessage by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }

    // Initial parse if editing
    LaunchedEffect(testPath) {
        if (testPath.isNotBlank()) {
            val result = TestParser.parseTest(testPath)
            totalQuestionsInFile = result.questions.size
            warnings = result.warnings
            errorMessage = result.error ?: ""
        }
    }

    val testLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("doc", "docx")),
        mode = PickerMode.Single
    ) { file ->
        file?.let {
            testPath = it.path ?: ""
            touched = true
            val result = TestParser.parseTest(testPath)
            totalQuestionsInFile = result.questions.size
            warnings = result.warnings
            if (result.error != null) {
                errorMessage = result.error
            } else if (totalQuestionsInFile == 0) {
                errorMessage = "Faylda testlar topilmadi yoki format noto'g'ri"
            } else {
                errorMessage = ""
                if (testCountPerStudent.toIntOrNull() ?: 0 > totalQuestionsInFile) {
                    testCountPerStudent = totalQuestionsInFile.toString()
                }
            }
        }
    }

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

    val saveExam: () -> Unit = {
        val tCount = testCountPerStudent.toIntOrNull() ?: 0
        val tMin = testTimerMin.toIntOrNull() ?: 0

        if (title.isBlank()) {
            errorMessage = "Imtihon nomini kiriting"
        } else if (testPath.isBlank()) {
            errorMessage = "Test faylini tanlang"
        } else if (tCount <= 0 || tCount > totalQuestionsInFile) {
            errorMessage = "Savollar soni noto'g'ri (1 - $totalQuestionsInFile)"
        } else if (tMin <= 0) {
            errorMessage = "Vaqtni to'g'ri kiriting"
        } else if (!isSaving) {
            isSaving = true
            val exam = Exam(
                id = examToEdit?.id ?: 0,
                title = title,
                testPath = testPath,
                testTimerSeconds = tMin * 60,
                questionsPerStudent = tCount,
                subjectId = subjectId
            )

            scope.launch {
                // Saving copies the Word file into the app folder
                withContext(Dispatchers.IO) {
                    if (examToEdit == null) {
                        AppContainer.localRepository.addExam(exam)
                    } else {
                        AppContainer.localRepository.updateExam(exam)
                    }
                }
                isSaving = false
                touched = false
                onSaveComplete()
            }
        }
    }

    if (showLeaveDialog) {
        PostStudyDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = "Saqlanmagan o'zgarishlar",
            text = "Imtihon hali saqlanmagan. Chiqib ketsangiz, kiritilgan ma'lumotlar yo'qoladi.",
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

    AdminPage(
        title = if (examToEdit == null) "Yangi imtihon" else "Imtihonni tahrirlash",
        eyebrow = "Imtihonlar",
        onBack = leave
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.page, vertical = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SignalCard(Modifier.widthIn(max = 880.dp).fillMaxWidth(), padding = 28.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    if (errorMessage.isNotEmpty()) {
                        Surface(color = Signal.DangerContainer, shape = AppDesign.ComponentShape, border = BorderStroke(1.dp, Signal.DangerBorder), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Signal.Danger, modifier = Modifier.size(18.dp))
                                Text(errorMessage, color = Signal.OnDangerContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    SettingsSection("Imtihon nomi") {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; touched = true },
                            placeholder = { Text("masalan, Yakuniy nazorat") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppDesign.SmallShape,
                            singleLine = true
                        )
                    }

                    SettingsSection("Test materiali", "Word hujjati (.doc, .docx), har bir tinglovchiga tasodifiy savollar beriladi") {
                        FilePickerRow(path = testPath, label = "Word faylini tanlang", onSelect = { testLauncher.launch() })
                        if (totalQuestionsInFile > 0) {
                            Spacer(Modifier.height(8.dp))
                            StatusChip("Faylda $totalQuestionsInFile ta yaroqli savol", Signal.SuccessContainer, Signal.OnSuccessContainer, icon = Icons.Default.Check, iconTint = Signal.Success)
                        }
                        if (warnings.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            WarningsBox(warnings)
                        }
                    }

                    SettingsSection("Imtihon qoidalari") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = testCountPerStudent,
                                onValueChange = { testCountPerStudent = it; touched = true },
                                label = { Text("Har bir tinglovchi uchun savollar soni") },
                                supportingText = if (totalQuestionsInFile > 0) ({ Text("1 dan $totalQuestionsInFile gacha") }) else null,
                                modifier = Modifier.weight(1f),
                                shape = AppDesign.SmallShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = testTimerMin,
                                onValueChange = { testTimerMin = it; touched = true },
                                label = { Text("Vaqt (daqiqalarda)") },
                                modifier = Modifier.weight(1f),
                                shape = AppDesign.SmallShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Row(Modifier.widthIn(max = 880.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                SignalButton("Bekor qilish", onClick = leave, style = ButtonStyle.Outlined, height = 46.dp)
                SignalButton(if (isSaving) "Saqlanmoqda..." else "Imtihonni saqlash", onClick = saveExam, enabled = !isSaving, style = ButtonStyle.Gradient, icon = Icons.Default.Check, height = 46.dp)
            }
        }
    }
}
