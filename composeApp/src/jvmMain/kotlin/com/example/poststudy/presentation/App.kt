package com.example.poststudy.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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
import com.example.poststudy.data.session.Grading
import com.example.poststudy.data.session.MaterialCache
import com.example.poststudy.data.session.Outbox
import com.example.poststudy.data.session.PreparedSession
import com.example.poststudy.data.session.SessionBuilder
import com.example.poststudy.data.session.toPreparedSession
import com.example.poststudy.domain.model.*
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.theme.LocalReduceMotion
import com.example.poststudy.presentation.theme.PostStudyTheme
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.ui.components.screenTransition
import com.example.poststudy.presentation.ui.components.AdminNameDialog
import com.example.poststudy.presentation.ui.components.AdminRail
import com.example.poststudy.presentation.ui.components.AdminSection
import com.example.poststudy.presentation.ui.components.LocalAdminNav
import com.example.poststudy.presentation.ui.components.LocalUnsavedGuard
import com.example.poststudy.presentation.ui.components.UnsavedGuard
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
    // Direction of the last navigation; back transitions slide the other way
    var navBack by remember { mutableStateOf(false) }
    fun go(screen: Screen) { navBack = false; currentScreen = screen }
    fun goBack(screen: Screen) { navBack = true; currentScreen = screen }

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

    // Subject ids are not stable: the first subject is not always 1 (e.g. after a reset)
    suspend fun useFirstSubjectIfMissing() {
        val subjects = withContext(Dispatchers.IO) { AppContainer.localRepository.getAllSubjects().first() }
        if (subjects.none { it.id == currentSubjectId }) {
            subjects.firstOrNull()?.let {
                currentSubjectId = it.id
                currentSubjectName = it.name
            }
        }
    }
    LaunchedEffect(Unit) { useFirstSubjectIfMissing() }

    // Rail navigation asks first while a form holds unsaved input
    val unsavedGuard = remember { UnsavedGuard() }
    var blockedNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingSubmission by remember { mutableStateOf<Submission?>(null) }
    var submitStatus by remember { mutableStateOf<SubmitStatus?>(null) }
    // Score of the last test: computed here in local mode, by the admin computer in network mode
    var graded by remember { mutableStateOf<GradedResult?>(null) }

    fun submitPendingSubmission() {
        val submission = pendingSubmission ?: return
        val address = serverAddress
        val password = groupPassword
        if (address == null) {
            submitStatus = SubmitStatus.Failed("Admin manzili noma'lum.")
            return
        }
        submitStatus = SubmitStatus.Sending
        appScope.launch {
            var result = Result.failure<GradedResult>(IllegalStateException())
            for (attempt in 1..3) {
                result = withContext(Dispatchers.IO) {
                    AppContainer.networkRepository.submitAnswers(address, submission, password)
                }
                if (result.isSuccess) break
                if (attempt < 3) delay(2000)
            }
            // Ignore the outcome if a newer test has started meanwhile
            if (pendingSubmission !== submission) return@launch
            submitStatus = result.fold(
                onSuccess = {
                    graded = it
                    pendingSubmission = null
                    withContext(Dispatchers.IO) { Outbox.remove(submission.submissionId) }
                    SubmitStatus.Sent
                },
                onFailure = {
                    // Kept on disk: sent again on the next start or the next retry
                    withContext(Dispatchers.IO) { Outbox.add(Outbox.Entry(address.host, address.port, password, submission)) }
                    SubmitStatus.Failed(
                        (it.message ?: "Javoblarni yuborib bo'lmadi.") +
                            " Javoblar shu kompyuterda saqlandi va admin tarmog'i topilganda avtomatik yuboriladi."
                    )
                }
            )
        }
    }

    // Results that never reached the admin are retried quietly in the background
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { runCatching { Outbox.flush() } }
    }

    fun resetStudent() {
        studentName = ""
        currentStudentId = null
        currentGroup = null
        groupPassword = ""
        session = null
        // Slides are big; do not keep the last deck alive between sessions
        slides = emptyList()
        questions = emptyList()
        userAnswers = emptyList()
    }

    PostStudyTheme {
        val reduceMotion = LocalReduceMotion.current
        val adminNav: (AdminSection) -> Unit = { section ->
            val navigate: () -> Unit = {
                when (section) {
                    AdminSection.Home -> go(Screen.TeacherHome)
                    AdminSection.Lessons -> go(Screen.LessonSelection)
                    AdminSection.Exams -> go(Screen.ExamSelection)
                    AdminSection.Groups -> go(Screen.Groups)
                    AdminSection.Monitoring -> go(Screen.Monitoring)
                    AdminSection.History -> go(Screen.History)
                    AdminSection.Help -> go(Screen.Readme)
                }
            }
            if (unsavedGuard.isDirty()) blockedNavigation = navigate else navigate()
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
          Row(Modifier.fillMaxSize()) {
            // The rail stays put while admin screens slide underneath it
            AnimatedVisibility(visible = currentScreen.hasAdminRail(), enter = expandHorizontally(), exit = shrinkHorizontally()) {
                AdminRail(
                    current = currentScreen.adminSection(),
                    onNavigate = adminNav,
                    onExit = {
                        val leave: () -> Unit = { goBack(Screen.RoleSelection) }
                        if (unsavedGuard.isDirty()) blockedNavigation = leave else leave()
                    }
                )
            }
            CompositionLocalProvider(LocalAdminNav provides adminNav, LocalUnsavedGuard provides unsavedGuard) {
            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                transitionSpec = { screenTransition(navBack, reduceMotion) },
                label = "screens"
            ) { screen ->
                when (screen) {
                    is Screen.Info -> InfoScreen(
                        onContinue = { go(Screen.Splash) }
                    )
                    is Screen.Splash -> SplashScreen(
                        onContinue = { go(Screen.RoleSelection) }
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
                                        go(if (unlocked) Screen.Login else Screen.AdminLocked)
                                    }
                                } else {
                                    go(Screen.StudentHome)
                                }
                            },
                            onJoinNetwork = {
                                isNetworkMode = true
                                resetStudent()
                                go(Screen.NetworkConnect)
                            },
                            onAbout = { goBack(Screen.Info) }
                        )
                        HelpIcon(
                            title = "Rolni tanlash",
                            helpText = "BreakPointga xush kelibsiz! Darslarni boshqarish uchun 'Admin' yoki test topshirish uchun 'Tinglovchi' rolini tanlang.",
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                            onDark = true
                        )
                    }
                    is Screen.NetworkConnect -> NetworkConnectScreen(
                        onConnected = { address ->
                            serverAddress = address
                            go(Screen.GroupSelection)
                        },
                        onBack = { goBack(Screen.RoleSelection) }
                    )
                    is Screen.GroupSelection -> {
                        val address = serverAddress
                        if (isNetworkMode && address == null) return@AnimatedContent
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
                                go(Screen.StudentSelection(group))
                            },
                            onBack = {
                                goBack(if (isNetworkMode) Screen.NetworkConnect else Screen.StudentHome)
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
                                go(Screen.StudentIntro)
                            },
                            onBack = { goBack(Screen.GroupSelection) }
                        )
                    }
                    is Screen.StudentHome -> Box {
                        StudentHomeScreen(
                            onNavigateToPreparation = {
                                go(Screen.PreparationLessonSelection)
                            },
                            onNavigateToTest = {
                                resetStudent()
                                go(Screen.GroupSelection)
                            },
                            onBack = { goBack(Screen.RoleSelection) }
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
                            go(Screen.PreparationSlideShow(lesson))
                        },
                        onAddNewLesson = {}, // Not needed for students
                        onEditLesson = {}, // Not needed for students
                        onViewHistory = {}, // Not needed for students
                        onBack = { goBack(Screen.StudentHome) }
                    )
                    is Screen.PreparationSlideShow -> {
                        val lesson = screen.lesson
                        var isPrepLoading by remember { mutableStateOf(true) }
                        LaunchedEffect(lesson) {
                            slides = withContext(Dispatchers.IO) {
                                MaterialCache.slides(lesson.presentationPath)
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
                                onFinished = { go(Screen.StudentHome) },
                                onBack = { goBack(Screen.StudentHome) }
                            )
                        }
                    }
                    is Screen.AdminLocked -> AdminLockedScreen(
                        onUnlocked = { go(Screen.Login) },
                        onBack = { goBack(Screen.RoleSelection) }
                    )
                    is Screen.Login -> LoginScreen(
                        // The LAN server is started from the admin home screen, not on login
                        onLoginSuccess = { go(Screen.TeacherIntro) },
                        onBack = { goBack(Screen.RoleSelection) }
                    )
                    is Screen.TeacherIntro -> TeacherIntroScreen(
                        onNext = { go(Screen.SubjectSelection) },
                        onBack = { goBack(Screen.Login) }
                    )
                    is Screen.SubjectSelection -> SubjectSelectionScreen(
                        currentSubjectId = currentSubjectId,
                        onSubjectSelected = { subject: Subject ->
                            currentSubjectId = subject.id
                            currentSubjectName = subject.name
                            go(Screen.TeacherHome)
                        },
                        onBack = { goBack(Screen.TeacherIntro) }
                    )
                    is Screen.TeacherHome -> Box {
                        TeacherHomeScreen(
                            subjectId = currentSubjectId,
                            subjectName = currentSubjectName,
                            onNavigateToLessons = { go(Screen.LessonSelection) },
                            onNavigateToExam = { go(Screen.ExamSelection) },
                            onNavigateToHistory = { go(Screen.History) },
                            onNavigateToMonitoring = { go(Screen.Monitoring) },
                            onNavigateToGroups = { go(Screen.Groups) },
                            onLogout = { goBack(Screen.RoleSelection) },
                            adminName = adminName,
                            onEditAdminName = { showNameEditor = true },
                            onAccountDeleted = {
                                appScope.launch {
                                    currentSubjectId = -1
                                    useFirstSubjectIfMissing()
                                    go(Screen.RoleSelection)
                                }
                            },
                            onBack = { goBack(Screen.SubjectSelection) }
                        )
                        HelpIcon(
                            title = "Admin asosiysi",
                            helpText = "Pastdagi jadvalda har bir guruhga qaysi dars yoki imtihon biriktirilganini ko'rasiz. Qatorni bosib uni o'zgartirishingiz mumkin.",
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        )
                    }
                    is Screen.Groups -> GroupManagementScreen(
                        subjectId = currentSubjectId,
                        onGroupSelected = { group -> go(Screen.GroupDetails(group)) },
                        onBack = { goBack(Screen.TeacherHome) }
                    )
                    is Screen.GroupDetails -> StudentListScreen(
                        group = screen.group,
                        onBack = { goBack(Screen.Groups) }
                    )
                    is Screen.ExamSelection -> ExamSelectionScreen(
                        subjectId = currentSubjectId,
                        onExamSelected = { exam ->
                            assignTarget = AssignTarget(AssignmentKind.Exam, exam.id, exam.title, null)
                        },
                        onAddNewExam = { go(Screen.ExamSettings) },
                        onEditExam = { exam -> go(Screen.EditExam(exam)) },
                        onBack = { goBack(Screen.TeacherHome) }
                    )
                    is Screen.ExamSettings -> ExamSettingsScreen(
                        subjectId = currentSubjectId,
                        onSaveComplete = { go(Screen.ExamSelection) },
                        onBack = { goBack(Screen.ExamSelection) }
                    )
                    is Screen.EditExam -> ExamSettingsScreen(
                        subjectId = currentSubjectId,
                        examToEdit = screen.exam,
                        onSaveComplete = { go(Screen.ExamSelection) },
                        onBack = { goBack(Screen.ExamSelection) }
                    )
                    is Screen.Readme -> ReadmeScreen(
                        onNext = { go(Screen.TeacherHome) },
                        onBack = { goBack(Screen.TeacherHome) }
                    )
                    is Screen.LessonSelection -> LessonSelectionScreen(
                        subjectId = currentSubjectId,
                        onLessonSelected = { lesson ->
                            assignTarget = AssignTarget(AssignmentKind.Lesson, lesson.id, lesson.title, lesson.mode)
                        },
                        onAddNewLesson = { go(Screen.Settings) },
                        onEditLesson = { lesson -> go(Screen.EditLesson(lesson)) },
                        onViewHistory = { go(Screen.History) },
                        onBack = { goBack(Screen.TeacherHome) }
                    )
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
                                        go(Screen.GroupSelection)
                                    },
                                    title = "Xatolik",
                                    text = message,
                                    confirmText = "Tushunarli",
                                    onConfirm = {
                                        errorMessage = null
                                        go(Screen.GroupSelection)
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
                                        go(if (current.mode.hasSlides && slides.isNotEmpty()) {
                                            Screen.SlideShow
                                        } else if (current.mode.hasTest) {
                                            Screen.Test
                                        } else {
                                            Screen.PresentationFinished
                                        })
                                    },
                                    onRefresh = {
                                        errorMessage = null
                                        isLoading = true
                                    },
                                    onBack = {
                                        goBack(Screen.GroupSelection)
                                    }
                                )
                            }
                        }
                    }
                    is Screen.Settings -> SettingsScreen(
                        subjectId = currentSubjectId,
                        onSaveComplete = { go(Screen.LessonSelection) },
                        onBack = { goBack(Screen.LessonSelection) }
                    )
                    is Screen.EditLesson -> SettingsScreen(
                        subjectId = currentSubjectId,
                        lessonToEdit = screen.lesson,
                        onSaveComplete = { go(Screen.LessonSelection) },
                        onBack = { goBack(Screen.LessonSelection) }
                    )
                    is Screen.SlideShow -> {
                        val hasTest = session?.mode?.hasTest == true
                        SlideShowScreen(
                            slides = slides,
                            slideTimerSeconds = session?.slideTimerSeconds ?: 0,
                            hasTestAfter = hasTest,
                            onFinished = {
                                go(if (hasTest) Screen.Test else Screen.PresentationFinished)
                            },
                            onBack = {
                                goBack(Screen.RoleSelection)
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

                                if (isNetworkMode) {
                                    // Only the answers leave this computer; the admin grades and stores them
                                    graded = null
                                    pendingSubmission = Submission(
                                        token = current?.token.orEmpty(),
                                        groupId = currentGroup?.id ?: -1,
                                        studentId = currentStudentId,
                                        studentName = studentName.ifBlank { "Anonim" },
                                        answers = answers,
                                        timeSpentSeconds = spent,
                                        submissionId = java.util.UUID.randomUUID().toString()
                                    )
                                    submitPendingSubmission()
                                } else {
                                    val outcome = Grading.grade(questions, answers)
                                    AppContainer.localRepository.saveExamRecord(
                                        ExamRecord(
                                            studentName = studentName.ifBlank { "Anonim" },
                                            lessonTitle = current?.title.orEmpty(),
                                            totalQuestions = outcome.result.totalQuestions,
                                            correctAnswers = outcome.result.correctAnswers,
                                            wrongAnswers = outcome.result.wrongIndices.size,
                                            wrongDetails = outcome.wrongDetails,
                                            timeSpentSeconds = spent,
                                            timestamp = System.currentTimeMillis(),
                                            groupId = currentGroup?.id,
                                            studentId = currentStudentId,
                                            subjectId = currentGroup?.subjectId ?: current?.subjectId
                                        )
                                    )
                                    graded = outcome.result
                                    pendingSubmission = null
                                    submitStatus = null
                                }

                                go(Screen.Result)
                            },
                            onBack = {
                                goBack(Screen.RoleSelection)
                            }
                        )
                    }
                    is Screen.Result -> {
                        ResultScreen(
                            questions = questions,
                            userAnswers = userAnswers,
                            graded = graded,
                            studentName = studentName,
                            timeSpentSeconds = timeSpent,
                            submitStatus = if (isNetworkMode) submitStatus else null,
                            onRetrySubmit = { submitPendingSubmission() },
                            onFinish = {
                                resetStudent()
                                go(Screen.RoleSelection)
                            }
                        )
                    }
                    is Screen.PresentationFinished -> PresentationFinishedScreen(
                        title = session?.title.orEmpty(),
                        studentName = studentName,
                        onFinish = {
                            resetStudent()
                            goBack(Screen.RoleSelection)
                        }
                    )
                    is Screen.History -> {
                        HistoryScreen(subjectId = currentSubjectId, onBack = { goBack(Screen.TeacherHome) })
                    }
                    is Screen.Monitoring -> {
                        MonitoringScreen(subjectId = currentSubjectId, onBack = { goBack(Screen.TeacherHome) })
                    }
                }
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

            blockedNavigation?.let { proceed ->
                PostStudyDialog(
                    onDismissRequest = { blockedNavigation = null },
                    title = "Saqlanmagan o'zgarishlar",
                    text = "Formadagi ma'lumotlar hali saqlanmagan. Chiqib ketsangiz, ular yo'qoladi.",
                    confirmText = "Baribir chiqish",
                    dismissText = "Qolish",
                    confirmColor = Signal.Danger,
                    onConfirm = {
                        blockedNavigation = null
                        unsavedGuard.isDirty = { false }
                        proceed()
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
            CircularProgressIndicator(color = Signal.Primary, trackColor = Signal.Container)
            Spacer(Modifier.height(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = Signal.Muted)
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
                    shape = AppDesign.SmallShape,
                )
                Text(
                    text = "${name.length} / 22",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (name.length == 22) Signal.Danger else Signal.Muted,
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

/** Admin screens that sit next to the navigation rail (login, intro and subject pick do not). */
private fun Screen.hasAdminRail(): Boolean = when (this) {
    Screen.TeacherHome, Screen.Groups, Screen.ExamSelection, Screen.ExamSettings, Screen.Readme,
    Screen.LessonSelection, Screen.Settings, Screen.History, Screen.Monitoring -> true
    is Screen.GroupDetails, is Screen.EditExam, is Screen.EditLesson -> true
    else -> false
}

private fun Screen.adminSection(): AdminSection? = when (this) {
    Screen.TeacherHome -> AdminSection.Home
    Screen.LessonSelection, Screen.Settings, is Screen.EditLesson -> AdminSection.Lessons
    Screen.ExamSelection, Screen.ExamSettings, is Screen.EditExam -> AdminSection.Exams
    Screen.Groups, is Screen.GroupDetails -> AdminSection.Groups
    Screen.Monitoring -> AdminSection.Monitoring
    Screen.History -> AdminSection.History
    Screen.Readme -> AdminSection.Help
    else -> null
}
