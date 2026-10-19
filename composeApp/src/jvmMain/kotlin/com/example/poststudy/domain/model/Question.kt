package com.example.poststudy.domain.model

/** A test question. [correctIndex] is -1 on a student's computer in network mode: only the admin computer knows it. */
data class Question(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

data class ParseResult(
    val questions: List<Question> = emptyList(),
    val warnings: List<String> = emptyList(),
    val error: String? = null
)

/**
 * What a student's computer sends back after a test: the answers, never a score. The admin
 * computer grades them against the session it served under [token].
 */
data class Submission(
    val token: String,
    val groupId: Int,
    val studentId: Int?,
    val studentName: String,
    /** One entry per question, in the order served; null for a skipped question. */
    val answers: List<Int?>,
    val timeSpentSeconds: Int,
    /** Random id per attempt so a retried request is stored once. */
    val submissionId: String
)

/** Outcome of grading, as shown to the student. */
data class GradedResult(
    val correctAnswers: Int,
    val totalQuestions: Int,
    /** Positions of wrongly answered or skipped questions. */
    val wrongIndices: List<Int>
)
