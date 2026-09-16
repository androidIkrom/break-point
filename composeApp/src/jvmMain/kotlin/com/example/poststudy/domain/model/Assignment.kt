package com.example.poststudy.domain.model

enum class AssignmentKind(val label: String) {
    Lesson("Dars"),
    Exam("Imtihon")
}

/** What a group's students get when they start a session. One per group. */
data class GroupAssignment(
    val groupId: Int,
    val kind: AssignmentKind,
    val itemId: Int,
    val mode: LessonMode
)

/**
 * A group together with its subject and current assignment, as shown in tables and
 * sent to students. [assignmentTitle] is null when nothing is assigned; [assignmentMissing]
 * is true when the assigned lesson/exam was deleted.
 */
data class GroupOverview(
    val id: Int,
    val name: String,
    val subjectId: Int,
    val subjectName: String,
    val kind: AssignmentKind? = null,
    val itemId: Int? = null,
    val mode: LessonMode? = null,
    val assignmentTitle: String? = null,
    val assignmentMissing: Boolean = false,
    /** Plain password for the teacher's screens; null in anything sent to students. */
    val password: String? = null,
    // Nullable so a missing JSON field does not break Gson
    val hasPassword: Boolean? = null
) {
    val isAssigned: Boolean get() = kind != null && !assignmentMissing

    val isLocked: Boolean get() = hasPassword ?: !password.isNullOrEmpty()

    /** Copy that is safe to send over the network. */
    fun forStudents(): GroupOverview = copy(password = null, hasPassword = isLocked)

    val group: Group get() = Group(id, name, subjectId)
}

/** Modes a teacher may pick when assigning [lesson]: only lessons with both materials offer a choice. */
fun allowedModes(lesson: Lesson): List<LessonMode> = when (lesson.mode) {
    LessonMode.ReAppropriation -> LessonMode.entries.toList()
    else -> listOf(lesson.mode)
}
