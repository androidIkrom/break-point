package com.example.poststudy.domain.repository

import com.example.poststudy.domain.model.*
import kotlinx.coroutines.flow.Flow

interface LocalRepository {
    fun init()
    fun isUserRegistered(): Flow<Boolean>
    fun registerUser(username: String, password: String, fullName: String = "")
    fun getAdminName(): String?
    fun getAdminLogin(): String?
    fun setAdminName(fullName: String)
    fun validateUser(username: String, password: String): Flow<Boolean>
    fun validateUserPassword(password: String): Flow<Boolean>
    fun clearAllUsers()

    // Admin lock (blocking)
    fun isAdminUnlocked(): Boolean
    fun unlockAdmin(secretKey: String): Boolean
    fun isSecretKey(secretKey: String): Boolean
    fun lockAdmin()
    /** Removes the account and all data; the app starts over as a fresh install. */
    fun deleteAccount()

    fun isBackgroundModeEnabled(): Boolean
    fun setBackgroundModeEnabled(enabled: Boolean)

    // Subjects
    fun getAllSubjects(): Flow<List<Subject>>
    fun addSubject(name: String): Flow<Int>
    fun updateSubject(subject: Subject)
    fun deleteSubject(id: Int)

    fun saveSettings(
        presentationPath: String,
        testPath: String,
        slideTimerMin: Int,
        testTimerMin: Int,
        mode: LessonMode,
        sessionTitle: String? = null,
        qCount: Int = 0,
        subjectId: Int = 1
    )
    fun getSettings(subjectId: Int = 1): Flow<Settings>

    fun getAllLessons(subjectId: Int): Flow<List<Lesson>>
    fun addLesson(lesson: Lesson)
    fun updateLesson(lesson: Lesson)
    fun deleteLesson(lessonId: Int)

    fun getAllExams(subjectId: Int): Flow<List<Exam>>
    fun addExam(exam: Exam)
    fun updateExam(exam: Exam)
    fun deleteExam(examId: Int)

    fun saveExamRecord(record: ExamRecord)
    fun getAllExamRecords(subjectId: Int? = null): Flow<List<ExamRecord>>
    fun deleteExamRecord(id: Int)
    fun clearAllExamRecords(subjectId: Int? = null)

    fun getLesson(lessonId: Int): Lesson?
    fun getExam(examId: Int): Exam?

    // Group assignments (blocking: call from Dispatchers.IO)
    fun getGroupAssignment(groupId: Int): GroupAssignment?
    fun setGroupAssignment(assignment: GroupAssignment)
    fun clearGroupAssignment(groupId: Int)
    fun getGroupOverviews(subjectId: Int? = null): List<GroupOverview>

    fun getAllGroups(subjectId: Int): Flow<List<Group>>
    fun addGroup(name: String, subjectId: Int, password: String = ""): Flow<Int>
    fun getGroupPassword(groupId: Int): String?
    fun setGroupPassword(groupId: Int, password: String)
    fun checkGroupPassword(groupId: Int, password: String): Boolean
    fun updateGroup(group: Group)
    fun deleteGroup(id: Int)

    fun getStudentsByGroup(groupId: Int): Flow<List<Student>>
    fun addStudent(name: String, groupId: Int): Flow<Int>
    fun deleteStudent(id: Int)

    fun getStudentRecords(studentId: Int): Flow<List<ExamRecord>>
    fun getGroupRecords(groupId: Int, subjectId: Int? = null): Flow<List<ExamRecord>>
    fun getAllGroupsWithStats(subjectId: Int): Flow<List<Pair<Group, Int>>>
}
