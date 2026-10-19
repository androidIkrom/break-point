package com.example.poststudy.data.session

import com.example.poststudy.data.local.DatabaseHelper
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.ExamRecord
import com.example.poststudy.domain.model.GradedResult
import com.example.poststudy.domain.model.Submission

/** Starts and stops the LAN server that serves group sessions to students and grades their answers. */
object AppServer {
    fun start(): Boolean = AppContainer.networkRepository.startServer(
        getSession = { groupId ->
            // Runs on a server thread. The copy sent out carries no correct answers.
            SessionBuilder.buildForGroup(groupId).map { prepared ->
                prepared.toSessionData(ServedSessions.issue(groupId, prepared))
            }
        },
        onSubmission = ::handleSubmission,
        // Students see this name in their list of active admins
        adminName = {
            val repo = AppContainer.localRepository
            repo.getAdminName()?.takeIf { it.isNotBlank() } ?: repo.getAdminLogin().orEmpty()
        }
    )

    fun stop() {
        AppContainer.networkRepository.stopServer()
        ServedSessions.clear()
    }

    /**
     * Grades a student's answers against the session served under their token and stores the
     * result. Everything that matters (score, time, name, timestamp) is decided here.
     */
    fun handleSubmission(submission: Submission): Result<GradedResult> {
        val served = ServedSessions.find(submission.token)
            ?: return Result.failure(Exception("Sessiya topilmadi yoki muddati tugagan. Mashg'ulotni qaytadan oching."))
        if (served.groupId != submission.groupId) return Result.failure(Exception("Guruh sessiyaga mos emas."))

        synchronized(served) {
            val earlier = served.result
            if (earlier != null) {
                // A retry after a lost reply gets the same answer; a second attempt needs a new session
                return if (served.submittedId == submission.submissionId) Result.success(earlier)
                else Result.failure(Exception("Bu sessiya allaqachon topshirilgan. Yangi urinish uchun mashg'ulotni qaytadan oching."))
            }
            if (submission.answers.size != served.questions.size) {
                return Result.failure(Exception("Javoblar soni savollar soniga mos emas."))
            }

            val outcome = Grading.grade(served.questions, submission.answers)
            val student = submission.studentId?.let { id -> DatabaseHelper.getStudentsByGroup(submission.groupId).firstOrNull { it.id == id } }
            val maxSeconds = if (served.testTimerSeconds > 0) served.testTimerSeconds else Int.MAX_VALUE
            val record = ExamRecord(
                studentName = student?.name ?: submission.studentName.trim().ifBlank { "Anonim" },
                lessonTitle = served.title,
                totalQuestions = outcome.result.totalQuestions,
                correctAnswers = outcome.result.correctAnswers,
                wrongAnswers = outcome.result.wrongIndices.size,
                wrongDetails = outcome.wrongDetails,
                timeSpentSeconds = submission.timeSpentSeconds.coerceIn(0, maxSeconds),
                timestamp = System.currentTimeMillis(),
                groupId = submission.groupId,
                studentId = student?.id,
                subjectId = served.subjectId,
                submissionId = submission.submissionId
            )
            AppContainer.localRepository.saveExamRecord(record)
            served.submittedId = submission.submissionId
            served.result = outcome.result
            return Result.success(outcome.result)
        }
    }
}
