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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.poststudy.data.util.TestParser
import com.example.poststudy.domain.model.Exam
import com.example.poststudy.presentation.theme.AppDesign
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType

@OptIn(ExperimentalMaterial3Api::class)
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
                onSaveComplete()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Imtihon Sozlamalari", color = Color(0xFF065F46), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth(),
                    shape = AppDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(3.dp, Color(0xFF6366F1).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(48.dp)) {
                        if (errorMessage.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = AppDesign.ComponentShape,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                                border = BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                            ) {
                                Text(errorMessage, color = Color(0xFFB91C1C), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("Imtihon nomi", style = MaterialTheme.typography.titleLarge, color = Color(0xFF6366F1), fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("masalan, Yakuniy nazorat") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppDesign.ComponentShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1))
                        )

                        Spacer(Modifier.height(32.dp))

                        Text("TEST MATERIALI", style = MaterialTheme.typography.titleLarge, color = Color(0xFF6366F1), fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(16.dp))

                        FilePickerRow(
                            path = testPath,
                            label = "Word faylini tanlang (.doc, .docx)",
                            onSelect = { testLauncher.launch() }
                        )

                        if (totalQuestionsInFile > 0) {
                            Text(
                                "Faylda jami $totalQuestionsInFile ta yaroqli savol aniqlandi",
                                color = Color(0xFF10B981),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (warnings.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFFFFF7ED),
                                shape = AppDesign.ComponentShape,
                                border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Ogohlantirishlar:", fontWeight = FontWeight.Black, color = Color(0xFFC2410C))
                                    warnings.forEach { warning ->
                                        Text("• $warning", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9A3412))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        Text("Imtihon qoidalari", style = MaterialTheme.typography.titleLarge, color = Color(0xFF6366F1), fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            OutlinedTextField(
                                value = testCountPerStudent,
                                onValueChange = { testCountPerStudent = it },
                                label = { Text("Har bir tinglovchi uchun savollar soni") },
                                modifier = Modifier.weight(1f),
                                shape = AppDesign.ComponentShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1))
                            )

                            OutlinedTextField(
                                value = testTimerMin,
                                onValueChange = { testTimerMin = it },
                                label = { Text("Vaqt (daqiqalarda)") },
                                modifier = Modifier.weight(1f),
                                shape = AppDesign.ComponentShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1))
                            )
                        }

                        Spacer(modifier = Modifier.height(64.dp))

                        Button(
                            onClick = saveExam,
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                            shape = AppDesign.ComponentShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                        ) {
                            Text("Imtihonni saqlash", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
