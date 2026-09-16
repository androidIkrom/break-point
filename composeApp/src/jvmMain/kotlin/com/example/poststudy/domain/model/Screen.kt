package com.example.poststudy.domain.model

sealed class Screen {
    data object Info : Screen()
    data object Splash : Screen()
    data object RoleSelection : Screen()
    data object Login : Screen()
    data object AdminLocked : Screen()
    data object Readme : Screen()
    data object TeacherIntro : Screen()
    data object StudentHome : Screen()
    data object PreparationLessonSelection : Screen()
    data class PreparationSlideShow(val lesson: Lesson) : Screen()
    data object StudentIntro : Screen()
    data object TeacherHome : Screen()
    data object Settings : Screen()
    data object ExamSelection : Screen()
    data object ExamSettings : Screen()
    data class EditExam(val exam: Exam) : Screen()
    data class EditLesson(val lesson: Lesson) : Screen()
    data object LessonSelection : Screen()
    data object SlideShow : Screen()
    data object Test : Screen()
    data object Result : Screen()
    data object History : Screen()
    data object Monitoring : Screen()
    data object Groups : Screen()
    data class GroupDetails(val group: Group) : Screen()
    data object NetworkConnect : Screen()
    data object GroupSelection : Screen()
    data class StudentSelection(val group: GroupOverview) : Screen()
    data object PresentationFinished : Screen()
    data object SubjectSelection : Screen()
}

data class Subject(
    val id: Int = 0,
    val name: String
)

data class Group(
    val id: Int,
    val name: String,
    val subjectId: Int
)

data class Student(
    val id: Int,
    val name: String,
    val groupId: Int
)

data class ExamRecord(
    val id: Int = 0,
    val studentName: String,
    val lessonTitle: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val wrongDetails: String,
    val timeSpentSeconds: Int,
    val timestamp: Long,
    val groupId: Int? = null,
    val studentId: Int? = null,
    val subjectId: Int? = null
)

enum class UserRole {
    Teacher,
    Student
}
