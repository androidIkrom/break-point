package com.example.poststudy.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.poststudy.di.AppContainer
import com.example.poststudy.data.network.ServerAddress
import com.example.poststudy.data.session.PreparedSession
import com.example.poststudy.data.session.SessionBuilder
import com.example.poststudy.data.session.toPreparedSession
import com.example.poststudy.data.util.PptConverter
import com.example.poststudy.domain.model.*
import com.example.poststudy.presentation.theme.PostStudyTheme
import com.example.poststudy.presentation.ui.components.AdminNameDialog
import com.example.poststudy.presentation.ui.components.HelpIcon
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import com.example.poststudy.presentation.ui.screens.admin.AdminLockedScreen
import com.example.poststudy.presentation.ui.screens.admin.ExamSelectionScreen
import com.example.poststudy.presentation.ui.screens.admin.ExamSettingsScreen
import com.example.poststudy.presentation.ui.screens.admin.HistoryScreen
import com.example.poststudy.presentation.ui.screens.admin.LessonSelectionScreen
import com.example.poststudy.presentation.ui.screens.admin.LoginScreen
import com.example.poststudy.presentation.ui.screens.admin.MonitoringScreen
import com.example.poststudy.presentation.ui.screens.admin.ReadmeScreen
import com.example.poststudy.presentation.ui.screens.admin.SettingsScreen
import com.example.poststudy.presentation.ui.screens.admin.SubjectSelectionScreen
import com.example.poststudy.presentation.ui.screens.admin.TeacherHomeScreen
import com.example.poststudy.presentation.ui.screens.admin.TeacherIntroScreen
import com.example.poststudy.presentation.ui.screens.groups.*
import com.example.poststudy.presentation.ui.screens.intro.InfoScreen
import com.example.poststudy.presentation.ui.screens.intro.RoleSelectionScreen
import com.example.poststudy.presentation.ui.screens.intro.SplashScreen
import com.example.poststudy.presentation.ui.screens.network.NetworkConnectScreen
import com.example.poststudy.presentation.ui.screens.student.PresentationFinishedScreen
import com.example.poststudy.presentation.ui.screens.student.ResultScreen
import com.example.poststudy.presentation.ui.screens.student.SlideShowScreen
import com.example.poststudy.presentation.ui.screens.student.StudentHomeScreen
import com.example.poststudy.presentation.ui.screens.student.StudentIntroScreen
import com.example.poststudy.presentation.ui.screens.student.SubmitStatus
import com.example.poststudy.presentation.ui.screens.student.TestScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage

/** A lesson or exam the teacher clicked in a list, to be assigned to groups. */
private data class AssignTarget(
    val kind: AssignmentKind,
    val itemId: Int,
    val title: String,
    val lessonMode: LessonMode?
)

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Info) }

    // Student session state
    var session by remember { mutableStateOf<PreparedSession?>(null) }
    var slides by remember { mutableStateOf<List<BufferedImage>>(emptyList()) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var userAnswers by remember { mutableStateOf<List<Int?>>(emptyList()) }
    var timeSpent by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var studentName by remember { mutableStateOf("") }
    var currentStudentId by remember { mutableStateOf<Int?>(null) }
    var currentGroup by remember { mutableStateOf<GroupOverview?>(null) }
    // Password the student typed for currentGroup; the admin checks it on every request
    var groupPassword by remember { mutableStateOf("") }

    // Teacher state
    var currentSubjectId by remember { mutableStateOf(1) }
    var currentSubjectName by remember { mutableStateOf("Asosiy fan") }
    var assignTarget by remember { mutableStateOf<AssignTarget?>(null) }
    // null until loaded; blank means the account has no name yet
    var adminName by remember { mutableStateOf<String?>(null) }
    var showNameEditor by remember { mutableStateOf(false) }
    val onAdminScreen = currentScreen.isAdminScreen()

    LaunchedEffect(onAdminScreen) {
        adminName = if (onAdminScreen) {
            withContext(Dispatchers.IO) { AppContainer.localRepository.getAdminName() }.orEmpty()
        } else null
    }

    var isNetworkMode by remember { mutableStateOf(false) }
    var serverAddress by remember { mutableStateOf<ServerAddress?>(null) }

    val appScope = rememberCoroutineScope()
    var pendingRecord by remember { mutableStateOf<ExamRecord?>(null) }
    var submitStatus by remember { mutableStateOf<SubmitStatus?>(null) }

    fun submitPendingRecord() {
        val record = pendingRecord ?: return
        val address = serverAddress
        val password = groupPassword
        if (address == null) {
            submitStatus = SubmitStatus.Failed("Admin manzili noma'lum.")
            return
        }
        submitStatus = SubmitStatus.Sending
        appScope.launch {
            var result = Result.failure<Unit>(IllegalStateException())
            for (attempt in 1..3) {
                result = withContext(Dispatchers.IO) {
                    AppContainer.networkRepository.submitResult(address, record, password)
                }
                if (result.isSuccess) break
                if (attempt < 3) delay(2000)
            }
            // Ignore the outcome if a newer test has started meanwhile
            if (pendingRecord !== record) return@launch
            submitStatus = result.fold(
                onSuccess = {
                    pendingRecord = null
                    SubmitStatus.Sent
                },
                onFailure = { SubmitStatus.Failed(it.message ?: "Natijani yuborib bo'lmadi.") }
            )
        }
    }

    fun resetStudent() {
        studentName = ""
        currentStudentId = null
        currentGroup = null
        groupPassword = ""
        session = null
    }

    PostStudyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Crossfade(targetState = currentScreen) { screen ->
                when (screen) {
                    is Screen.Info -> InfoScreen(
                        onContinue = { currentScreen = Screen.Splash }
                    )
                    is Screen.Splash -> SplashScreen(
                        onContinue = { currentScreen = Screen.RoleSelection }
                    )
                    is Screen.RoleSelection -> Box {
                        RoleSelectionScreen(
                            onRoleSelected = { role ->
                                isNetworkMode = false
                                serverAddress = null
                                resetStudent()
                                if (role == UserRole.Teacher) {
                                    // Admin works only on computers unlocked with the secret key
                                    appScope.launch {
                                        val unlocked = withContext(Dispatchers.IO) {
                                            AppContainer.localRepository.isAdminUnlocked()
                                        }
                                        currentScreen = if (unlocked) Screen.Login else Screen.AdminLocked
                                    }
                                } else {
                                    currentScreen = Screen.StudentHome
                                }
                            },
                            onJoinNetwork = {
                                isNetworkMode = true
                                resetStudent()
                                currentScreen = Screen.NetworkConnect
                            }
                        )
                        HelpIcon(
                            title = "Rolni tanlash",
                            helpText = "BreakPointga xush kelibsiz! Darslarni boshqarish uchun 'Admin' yoki test topshirish uchun 'Tinglovchi' rolini tanlang.",
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        )
                    }
                    is Screen.NetworkConnect -> Box {
                        NetworkConnectScreen(
                            onConnected = { address ->
                                serverAddress = address
                                currentScreen = Screen.GroupSelection
                            },
                            onBack = { currentScreen = Screen.RoleSelection }
                        )
                        HelpIcon(
                            title = "Tarmoqqa ulanish",
                            helpText = "Admin kompyuteridagi IP manzilni kiriting va 'Ulanish' tugmasini bosing.",
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        )
                    }
                    is Screen.GroupSelection -> {
                        val address = serverAddress
                        if (isNetworkMode && address == null) return@Crossfade
                        GroupSelectionScreen(
                            sourceLabel = if (address != null) "Admin: $address" else "Shu kompyuter",
                            loadGroups = {
                                if (address != null) {
                                    AppContainer.networkRepository.fetchGroups(address)
                                } else {
                                    runCatching { AppContainer.localRepository.getGroupOverviews() }
                                }
                            },
                            verifyPassword = { group, password ->
                                if (address != null) {
                                    AppContainer.networkRepository.verifyGroup(address, group.id, password)
                                } else if (AppContainer.localRepository.checkGroupPassword(group.id, password)) {
                                    Result.success(Unit)
                                } else {
                                    Result.failure(Exception("Guruh paroli noto'g'ri."))
                                }
                            },
                            onGroupSelected = { group, password ->
                                currentGroup = group
                                groupPassword = password
                                currentScreen = Screen.StudentSelection(group)
                            },
                            onBack = {
                                currentScreen = if (isNetworkMode) Screen.NetworkConnect else Screen.StudentHome
                            }
                        )
                    }
                    is Screen.StudentSelection -> {
                        val address = serverAddress
                        val group = screen.group
                        val password = groupPassword
                        StudentSelectionScreen(
                            group = group,
                            loadStudents = {
                                if (address != null) {
                                    AppContainer.networkRepository.fetchStudents(address, group.id, password)
                                } else {
                                    runCatching { AppContainer.localRepository.getStudentsByGroup(group.id).first() }
                                }
                            },
                            createStudent = { name ->
                                if (address != null) {
                                    AppContainer.networkRepository.createStudentRemote(address, name, group.id, password)
                                } else {
                                    runCatching {
                                        val repo = AppContainer.localRepository
                                        repo.getStudentsByGroup(group.id).first()
                                            .firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
                                            ?: repo.addStudent(name, group.id).first()
                                    }
                                }
                            },
                            onStudentSelected = { student ->
                                studentName = student.name
                                currentStudentId = student.id
                                currentGroup = group
                                errorMessage = null
                                isLoading = true
                                currentScreen = Screen.StudentIntro
                            },
                            onBack = { currentScreen = Screen.GroupSelection }
                        )
                    }
                    is Screen.StudentHome -> Box {
                        StudentHomeScreen(
                            onNavigateToPreparation = {
                                currentScreen = Screen.PreparationLessonSelection
                            },
                            onNavigateToTest = {
                                resetStudent()
                                currentScreen = Screen.GroupSelection
                            },
                            onBack = { currentScreen = Screen.RoleSelection }
                        )
                        HelpIcon(
                            title = "Tinglovchi asosiysi",
                            helpText = "Mavjud darslarni o'rganish uchun 'Tayyorgarlik' yoki guruhingizga biriktirilgan mashg'ulotni boshlash uchun 'Bilim testi'ni tanlang.",
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        )
                    }
                    is Screen.PreparationLessonSelection -> LessonSelectionScreen(
                        subjectId = currentSubjectId,
                        isTeacher = false,
                        onLessonSelected = { lesson ->
                            currentScreen = Screen.PreparationSlideShow(lesson)
                        },
                        onAddNewLesson = {}, // Not needed for students
                        onEditLesson = {}, // Not needed for students
                        onViewHistory = {}, // Not needed for students
                        onBack = { currentScreen = Screen.StudentHome }
                    )
                    is Screen.PreparationSlideShow -> {
                        val lesson = screen.lesson
                        var isPrepLoading by remember { mutableStateOf(true) }
                        LaunchedEffect(lesson) {
                            slides = withContext(Dispatchers.IO) {
                                PptConverter.convertSlidesToImages(lesson.presentationPath)
                            }
                            isPrepLoading = false
                        }

                        if (isPrepLoading) {
                            LoadingMessage("Taqdimot yuklanmoqda...")
                        } else {
                            SlideShowScreen(
                                slides = slides,
                                slideTimerSeconds = 0, // No timer
                                hasTestAfter = false,
                                onFinished = { currentScreen = Screen.StudentHome },
                                onBack = { currentScreen = Screen.StudentHome }
                            )
                        }
                    }
                    is Screen.AdminLocked -> AdminLockedScreen(
                        onUnlocked = { currentScreen = Screen.Login },
                        onBack = { currentScreen = Screen.RoleSelection }
                    )
                    is Screen.Login -> LoginScreen(
                        // The LAN server is started from the admin home screen, not on login
                        onLoginSuccess = { currentScreen = Screen.TeacherIntro },
                        onBack = { currentScreen = Screen.RoleSelection }
                    )
                    is Screen.TeacherIntro -> TeacherIntroScreen(
                        onNext = { currentScreen = Screen.SubjectSelection },
                        onBack = { currentScreen = Screen.Login }
                    )
                    is Screen.SubjectSelection -> SubjectSelectionScreen(
                        currentSubjectId = currentSubjectId,
                        onSubjectSelected = { subject: Subject ->
                            currentSubjectId = subject.id
                            currentSubjectName = subject.name
                            currentScreen = Screen.TeacherHome
                        },
                        onBack = { currentScreen = Screen.TeacherIntro }
                    )
                    is Screen.TeacherHome -> Box {
                        TeacherHomeScreen(
                            subjectId = currentSubjectId,
                            subjectName = currentSubjectName,
                            onNavigateToLessons = { currentScreen = Screen.LessonSelection },
                            onNavigateToExam = { currentScreen = Screen.ExamSelection },
                            onNavigateToHistory = { currentScreen = Screen.History },
                            onNavigateToMonitoring = { currentScreen = Screen.Monitoring },
                            onNavigateToGroups = { currentScreen = Screen.Groups },
                            onLogout = { currentScreen = Screen.RoleSelection },
                            adminName = adminName,
                            onEditAdminName = { showNameEditor = true },
                            onAccountDeleted = {
                                currentSubjectId = 1
                                currentSubjectName = "Asosiy fan"
                                currentScreen = Screen.RoleSelection
                            },
                            onBack = { currentScreen = Screen.SubjectSelection }
                        )
                        HelpIcon(
                            title = "Admin asosiysi",
                            helpText = "Pastdagi jadvalda har bir guruhga qaysi dars yoki imtihon biriktirilganini ko'rasiz. Qatorni bosib uni o'zgartirishingiz mumkin.",
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        )
                    }
                    is Screen.Groups -> GroupManagementScreen(
                        subjectId = currentSubjectId,
                        onGroupSelected = { group -> currentScreen = Screen.GroupDetails(group) },
                        onBack = { currentScreen = Screen.TeacherHome }
                    )
                    is Screen.GroupDetails -> StudentListScreen(
                        group = screen.group,
                        onBack = { currentScreen = Screen.Groups }
                    )
                    is Screen.ExamSelection -> Box {
                        ExamSelectionScreen(
                            subjectId = currentSubjectId,
                            onExamSelected = { exam ->
                                assignTarget = AssignTarget(AssignmentKind.Exam, exam.id, exam.title, null)
                            },
                            onAddNewExam = { currentScreen = Screen.ExamSettings },
                            onEditExam = { exam -> currentScreen = Screen.EditExam(exam) },
                            onBack = { currentScreen = Screen.TeacherHome }
                        )
                        HelpIcon(
                            title = "Imtihonlar",
                            helpText = "Imtihonni bosib uni guruhlarga biriktiring. Imtihon faqat test sifatida o'tkaziladi.",
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        )
                    }
                    is Screen.ExamSettings -> ExamSettingsScreen(
                        subjectId = currentSubjectId,
                        onSaveComplete = { currentScreen = Screen.ExamSelection },
                        onBack = { currentScreen = Screen.ExamSelection }
                    )
                    is Screen.EditExam -> ExamSettingsScreen(
                        subjectId = currentSubjectId,
                        examToEdit = screen.exam,
                        onSaveComplete = { currentScreen = Screen.ExamSelection },
                        onBack = { currentScreen = Screen.ExamSelection }
                    )
                    is Screen.Readme -> ReadmeScreen(
                        onNext = { currentScreen = Screen.TeacherHome },
                        onBack = { currentScreen = Screen.TeacherHome }
                    )
                    is Screen.LessonSelection -> Box {
                        LessonSelectionScreen(
                            subjectId = currentSubjectId,
                            onLessonSelected = { lesson ->
                                assignTarget = AssignTarget(AssignmentKind.Lesson, lesson.id, lesson.title, lesson.mode)
                            },
                            onAddNewLesson = {
                                currentScreen = Screen.Settings
                            },
                            onEditLesson = { lesson ->
                                currentScreen = Screen.EditLesson(lesson)
                            },
                            onViewHistory = {
                                currentScreen = Screen.History
                            },
                            onBack = {
                                currentScreen = Screen.TeacherHome
                            }
                        )
                        HelpIcon(
                            title = "Darslarni tanlash",
                            helpText = "Darsni bosib uni guruhlarga biriktiring va rejimini tanlang. Yangi dars qo'shish uchun + tugmasidan foydalaning.",
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        )
                    }
                    is Screen.StudentIntro -> {
                        if (isLoading) {
                            LaunchedEffect(Unit) {
                                val group = currentGroup
                                val address = serverAddress
                                val result = when {
                                    group == null -> Result.failure(Exception("Guruh tanlanmagan."))
                                    address != null -> withContext(Dispatchers.IO) {
                                        AppContainer.networkRepository.fetchSession(address, group.id, groupPassword)
                                            .map { it.toPreparedSession() }
                                    }
                                    isNetworkMode -> Result.failure(Exception("Admin manzili noma'lum. Qaytadan ulaning."))
                                    else -> withContext(Dispatchers.IO) { SessionBuilder.buildForGroup(group.id) }
                                }
                                result
                                    .onSuccess {
                                        session = it
                                        slides = it.slides
                                        questions = it.questions
                                        if (it.mode.hasSlides && it.slides.isEmpty()) {
                                            errorMessage = "Prezentatsiya slaydlarini yuklab bo'lmadi."
                                        }
                                    }
                                    .onFailure {
                                        session = null
                                        slides = emptyList()
                                        questions = emptyList()
                                        errorMessage = it.message ?: "Sessiya ma'lumotlarini yuklab bo'lmadi."
                                    }
                                isLoading = false
                            }
                            LoadingMessage("Sessiya tayyorlanmoqda...")
                        } else {
                            errorMessage?.let { message ->
                                PostStudyDialog(
                                    onDismissRequest = {
                                        errorMessage = null
                                        currentScreen = Screen.GroupSelection
                                    },
                                    title = "Xatolik",
                                    text = message,
                                    confirmText = "Tushunarli",
                                    onConfirm = {
                                        errorMessage = null
                                        currentScreen = Screen.GroupSelection
                                    }
                                )
                            }

                            val current = session
                            if (current != null) {
                                StudentIntroScreen(
                                    title = current.title,
                                    totalSlides = slides.size,
                                    totalQuestions = questions.size,
                                    slideTimerSeconds = current.slideTimerSeconds,
                                    testTimerSeconds = current.testTimerSeconds,
                                    mode = current.mode,
                                    isExam = current.kind == AssignmentKind.Exam,
                                    groupName = currentGroup?.name.orEmpty(),
                                    onStart = {
                                        currentScreen = if (current.mode.hasSlides && slides.isNotEmpty()) {
                                            Screen.SlideShow
                                        } else if (current.mode.hasTest) {
                                            Screen.Test
                                        } else {
                                            Screen.PresentationFinished
                                        }
                                    },
                                    onRefresh = {
                                        errorMessage = null
                                        isLoading = true
                                    },
                                    onBack = {
                                        currentScreen = Screen.GroupSelection
                                    }
                                )
                            }
                        }
                    }
                    is Screen.Settings -> SettingsScreen(
                        subjectId = currentSubjectId,
                        onSaveComplete = { currentScreen = Screen.LessonSelection },
                        onBack = { currentScreen = Screen.LessonSelection }
                    )
                    is Screen.EditLesson -> SettingsScreen(
                        subjectId = currentSubjectId,
                        lessonToEdit = screen.lesson,
                        onSaveComplete = { currentScreen = Screen.LessonSelection },
                        onBack = { currentScreen = Screen.LessonSelection }
                    )
                    is Screen.SlideShow -> {
                        val hasTest = session?.mode?.hasTest == true
                        SlideShowScreen(
                            slides = slides,
                            slideTimerSeconds = session?.slideTimerSeconds ?: 0,
                            hasTestAfter = hasTest,
                            onFinished = {
                                currentScreen = if (hasTest) Screen.Test else Screen.PresentationFinished
                            },
                            onBack = {
                                currentScreen = Screen.RoleSelection
                            }
                        )
                    }
                    is Screen.Test -> {
                        val current = session
                        TestScreen(
                            sessionTitle = current?.title.orEmpty(),
                            questions = questions,
                            testTimerSeconds = current?.testTimerSeconds ?: 0,
                            studentName = studentName,
                            onFinished = { answers, spent ->
                                userAnswers = answers
                                timeSpent = spent

                                val pairs = questions.zip(answers)
                                val correctCount = pairs.count { (q, a) -> q.correctIndex == a }
                                val wrongDetails = pairs
                                    .filter { (q, a) -> q.correctIndex != a }
                                    .joinToString("\n\n") { (q, a) ->
                                        val correct = q.options.getOrNull(q.correctIndex) ?: "-"
                                        val chosen = a?.let { q.options.getOrNull(it) } ?: "O'tkazib yuborildi"
                                        "S: ${q.text}\nTo'g'ri javob: $correct\nTanlangan javob: $chosen"
                                    }

                                val record = ExamRecord(
                                    studentName = studentName.ifBlank { "Anonim" },
                                    lessonTitle = current?.title.orEmpty(),
                                    totalQuestions = questions.size,
                                    correctAnswers = correctCount,
                                    wrongAnswers = questions.size - correctCount,
                                    wrongDetails = wrongDetails,
                                    timeSpentSeconds = spent,
                                    timestamp = System.currentTimeMillis(),
                                    groupId = currentGroup?.id,
                                    studentId = currentStudentId,
                                    subjectId = currentGroup?.subjectId ?: current?.subjectId
                                )

                                if (isNetworkMode) {
                                    // IDs belong to the admin database, so the admin stores the record
                                    pendingRecord = record
                                    submitPendingRecord()
                                } else {
                                    AppContainer.localRepository.saveExamRecord(record)
                                    pendingRecord = null
                                    submitStatus = null
                                }

                                currentScreen = Screen.Result
                            },
                            onBack = {
                                currentScreen = Screen.RoleSelection
                            }
                        )
                    }
                    is Screen.Result -> {
                        ResultScreen(
                            questions = questions,
                            userAnswers = userAnswers,
                            studentName = studentName,
                            timeSpentSeconds = timeSpent,
                            submitStatus = if (isNetworkMode) submitStatus else null,
                            onRetrySubmit = { submitPendingRecord() },
                            onFinish = {
                                currentScreen = Screen.RoleSelection
                            }
                        )
                    }
                    is Screen.PresentationFinished -> PresentationFinishedScreen(
                        title = session?.title.orEmpty(),
                        studentName = studentName,
                        onFinish = { currentScreen = Screen.RoleSelection }
                    )
                    is Screen.History -> {
                        HistoryScreen(subjectId = currentSubjectId, onBack = { currentScreen = Screen.TeacherHome })
                    }
                    is Screen.Monitoring -> {
                        MonitoringScreen(subjectId = currentSubjectId, onBack = { currentScreen = Screen.TeacherHome })
                    }
                }
            }

            // The admin must have a name on every admin screen, not only at registration
            // Older accounts may have a blank or multi-word name; both must be fixed first
            val nameMissing = onAdminScreen && adminName != null && !AdminUsername.isValid(adminName)
            if (nameMissing || (showNameEditor && onAdminScreen)) {
                AdminNameDialog(
                    initialName = if (nameMissing) AdminUsername.suggestFrom(adminName) else adminName.orEmpty(),
                    mandatory = nameMissing,
                    onDismiss = { showNameEditor = false },
                    onSave = { name ->
                        adminName = name
                        showNameEditor = false
                        appScope.launch(Dispatchers.IO) { AppContainer.localRepository.setAdminName(name) }
                    }
                )
            }

            assignTarget?.let { target ->
                AssignToGroupsDialog(
                    kind = target.kind,
                    itemId = target.itemId,
                    title = target.title,
                    subjectId = currentSubjectId,
                    lessonMode = target.lessonMode,
                    onDismiss = { assignTarget = null },
                    onSaved = { assignTarget = null }
                )
            }
        }
    }
}

@Composable
private fun LoadingMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF6366F1))
            Spacer(Modifier.height(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun NameInputDialog(onNameEntered: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    PostStudyDialog(
        onDismissRequest = onDismiss,
        title = "Tinglovchi ismi",
        text = "Iltimos, sessiyani boshlash uchun to'liq ismingizni kiriting. Bu natijalaringizni saqlash uchun ishlatiladi.",
        confirmText = "Sessiyani boshlash",
        onConfirm = { if (name.isNotBlank()) onNameEntered(name) },
        content = {
            Column(horizontalAlignment = Alignment.End) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 22) name = it },
                    label = { Text("To'liq ism") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
                )
                Text(
                    text = "${name.length} / 22",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (name.length == 22) Color.Red else Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )
            }
        }
    )
}

/** Screens that belong to the logged-in admin. */
private fun Screen.isAdminScreen(): Boolean = when (this) {
    Screen.TeacherIntro, Screen.SubjectSelection, Screen.TeacherHome, Screen.Groups,
    Screen.ExamSelection, Screen.ExamSettings, Screen.Readme, Screen.LessonSelection,
    Screen.Settings, Screen.History, Screen.Monitoring -> true
    is Screen.GroupDetails, is Screen.EditExam, is Screen.EditLesson -> true
    else -> false
}
