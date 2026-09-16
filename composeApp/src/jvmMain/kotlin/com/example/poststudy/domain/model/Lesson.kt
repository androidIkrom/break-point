package com.example.poststudy.domain.model

/**
 * For a [Lesson] this is which materials it has; for a group assignment it is what students do.
 */
enum class LessonMode(val label: String) {
    /** Presentation first, then the test. */
    ReAppropriation("Prezentatsiya + test"),
    TestOnly("Faqat test"),
    PresentationOnly("Faqat prezentatsiya");

    val hasSlides: Boolean get() = this != TestOnly
    val hasTest: Boolean get() = this != PresentationOnly

    companion object {
        /** Unknown names (e.g. "Seminar" from other builds) mean slides + test. */
        fun fromDbValue(value: String): LessonMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ReAppropriation
    }
}

data class Lesson(
    val id: Int = 0,
    val title: String,
    val presentationPath: String,
    val testPath: String,
    val slideTimerSeconds: Int,
    val testTimerSeconds: Int,
    val mode: LessonMode = LessonMode.ReAppropriation,
    val subjectId: Int = 1
)

data class Exam(
    val id: Int = 0,
    val title: String,
    val testPath: String,
    val testTimerSeconds: Int,
    val questionsPerStudent: Int,
    val subjectId: Int = 1
)
