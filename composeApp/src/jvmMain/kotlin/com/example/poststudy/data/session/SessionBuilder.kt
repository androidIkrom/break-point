package com.example.poststudy.data.session

import com.example.poststudy.data.network.SessionData
import com.example.poststudy.data.util.PptConverter
import com.example.poststudy.data.util.TestParser
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

/** Everything a student needs to run the session assigned to their group. */
data class PreparedSession(
    val title: String,
    val kind: AssignmentKind,
    val mode: LessonMode,
    val questions: List<Question>,
    val slides: List<BufferedImage>,
    val slideTimerSeconds: Int,
    val testTimerSeconds: Int,
    val subjectId: Int
)

/** Builds sessions from group assignments. Blocking: call from Dispatchers.IO. */
object SessionBuilder {

    fun buildForGroup(groupId: Int): Result<PreparedSession> {
        val repo = AppContainer.localRepository
        val assignment = repo.getGroupAssignment(groupId)
            ?: return failure("Bu guruhga hali dars yoki imtihon biriktirilmagan. Adminga murojaat qiling.")

        return when (assignment.kind) {
            AssignmentKind.Lesson -> {
                val lesson = repo.getLesson(assignment.itemId)
                    ?: return failure("Guruhga biriktirilgan dars o'chirib yuborilgan. Adminga murojaat qiling.")
                // The lesson may have lost a material since it was assigned
                val mode = assignment.mode.takeIf { it in allowedModes(lesson) } ?: lesson.mode
                build(
                    title = lesson.title,
                    kind = AssignmentKind.Lesson,
                    mode = mode,
                    presentationPath = lesson.presentationPath,
                    testPath = lesson.testPath,
                    questionsPerStudent = 0,
                    slideTimerSeconds = lesson.slideTimerSeconds,
                    testTimerSeconds = lesson.testTimerSeconds,
                    subjectId = lesson.subjectId
                )
            }
            AssignmentKind.Exam -> {
                val exam = repo.getExam(assignment.itemId)
                    ?: return failure("Guruhga biriktirilgan imtihon o'chirib yuborilgan. Adminga murojaat qiling.")
                build(
                    title = exam.title,
                    kind = AssignmentKind.Exam,
                    mode = LessonMode.TestOnly,
                    presentationPath = "",
                    testPath = exam.testPath,
                    questionsPerStudent = exam.questionsPerStudent,
                    slideTimerSeconds = 0,
                    testTimerSeconds = exam.testTimerSeconds,
                    subjectId = exam.subjectId
                )
            }
        }
    }

    private fun build(
        title: String,
        kind: AssignmentKind,
        mode: LessonMode,
        presentationPath: String,
        testPath: String,
        questionsPerStudent: Int,
        slideTimerSeconds: Int,
        testTimerSeconds: Int,
        subjectId: Int
    ): Result<PreparedSession> {
        val slides = if (mode.hasSlides) {
            PptConverter.convertSlidesToImages(presentationPath).ifEmpty {
                return failure("'$title' prezentatsiyasini ochib bo'lmadi: ${File(presentationPath).name.ifBlank { "fayl tanlanmagan" }}")
            }
        } else emptyList()

        val questions = if (mode.hasTest) {
            val parsed = TestParser.parseTest(testPath)
            if (parsed.error != null) return failure("'$title' test fayli bilan xatolik: ${parsed.error}")
            if (parsed.questions.isEmpty()) return failure("'$title' test faylida savollar topilmadi.")
            prepareQuestions(parsed.questions, questionsPerStudent)
        } else emptyList()

        return Result.success(
            PreparedSession(title, kind, mode, questions, slides, slideTimerSeconds, testTimerSeconds, subjectId)
        )
    }

    /** Picks [perStudent] random questions (all if 0) and shuffles their options. */
    fun prepareQuestions(source: List<Question>, perStudent: Int): List<Question> {
        val count = if (perStudent > 0) minOf(perStudent, source.size) else source.size
        return source.shuffled().take(count).map { q ->
            val indexedOptions = q.options.withIndex().shuffled()
            q.copy(
                options = indexedOptions.map { it.value },
                correctIndex = indexedOptions.indexOfFirst { it.index == q.correctIndex }
            )
        }
    }

    private fun failure(message: String): Result<PreparedSession> = Result.failure(Exception(message))
}

fun PreparedSession.toSessionData(): SessionData = SessionData(
    title = title,
    questions = questions,
    slideTimerSeconds = slideTimerSeconds,
    testTimerSeconds = testTimerSeconds,
    mode = mode,
    subjectId = subjectId,
    kind = kind,
    encodedSlides = slides.map { img ->
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "jpg", out)
        Base64.getEncoder().encodeToString(out.toByteArray())
    }
)

fun SessionData.toPreparedSession(): PreparedSession = PreparedSession(
    title = title,
    kind = kind ?: AssignmentKind.Lesson,
    mode = mode,
    questions = questions,
    slides = encodedSlides.mapNotNull { base64 ->
        try {
            ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(base64)))
        } catch (e: Exception) {
            null
        }
    },
    slideTimerSeconds = slideTimerSeconds,
    testTimerSeconds = testTimerSeconds,
    subjectId = subjectId
)
