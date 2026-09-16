package com.example.poststudy.data.repository

import com.example.poststudy.data.local.AdminAccess
import com.example.poststudy.data.local.DatabaseHelper
import com.example.poststudy.domain.model.*
import com.example.poststudy.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val BACKGROUND_MODE_KEY = "background_mode"

class LocalRepositoryImpl : LocalRepository {
    override fun init() = DatabaseHelper.init()

    override fun isUserRegistered(): Flow<Boolean> = flow {
        emit(DatabaseHelper.isUserRegistered())
    }

    override fun registerUser(username: String, password: String, fullName: String) {
        DatabaseHelper.registerUser(username, password, fullName)
    }

    override fun getAdminName(): String? = DatabaseHelper.getAdminName()

    override fun getAdminLogin(): String? = DatabaseHelper.getAdminLogin()

    override fun setAdminName(fullName: String) {
        DatabaseHelper.setAdminName(fullName)
    }

    override fun validateUser(username: String, password: String): Flow<Boolean> = flow {
        emit(DatabaseHelper.validateUser(username, password))
    }

    override fun validateUserPassword(password: String): Flow<Boolean> = flow {
        emit(DatabaseHelper.validateUserPassword(password))
    }

    override fun clearAllUsers() {
        DatabaseHelper.clearAllUsers()
    }

    override fun isAdminUnlocked(): Boolean = AdminAccess.isUnlocked()

    override fun unlockAdmin(secretKey: String): Boolean = AdminAccess.unlock(secretKey)

    override fun isSecretKey(secretKey: String): Boolean = AdminAccess.isSecretKey(secretKey)

    override fun lockAdmin() {
        AdminAccess.lock()
    }

    override fun deleteAccount() {
        DatabaseHelper.wipeAllData()
        // The last typed admin address lives in the Windows registry under HKCU\Software\JavaSoft\Prefs\breakpoint
        runCatching {
            val root = java.util.prefs.Preferences.userRoot()
            if (root.nodeExists("breakpoint")) root.node("breakpoint").removeNode()
        }
    }

    override fun isBackgroundModeEnabled(): Boolean = DatabaseHelper.getMeta(BACKGROUND_MODE_KEY) == "1"

    override fun setBackgroundModeEnabled(enabled: Boolean) {
        DatabaseHelper.setMeta(BACKGROUND_MODE_KEY, if (enabled) "1" else "0")
    }

    // Subjects
    override fun getAllSubjects(): Flow<List<Subject>> = flow {
        emit(DatabaseHelper.getAllSubjects())
    }

    override fun addSubject(name: String): Flow<Int> = flow {
        emit(DatabaseHelper.addSubject(name))
    }

    override fun updateSubject(subject: Subject) {
        DatabaseHelper.updateSubject(subject)
    }

    override fun deleteSubject(id: Int) {
        DatabaseHelper.deleteSubject(id)
    }

    override fun saveSettings(
        presentationPath: String,
        testPath: String,
        slideTimerMin: Int,
        testTimerMin: Int,
        mode: LessonMode,
        sessionTitle: String?,
        qCount: Int,
        subjectId: Int
    ) {
        DatabaseHelper.saveSettings(
            presentationPath, testPath, slideTimerMin, testTimerMin, mode, sessionTitle, qCount, subjectId
        )
    }

    override fun getSettings(subjectId: Int): Flow<Settings> = flow {
        emit(DatabaseHelper.getSettings(subjectId))
    }

    override fun getAllLessons(subjectId: Int): Flow<List<Lesson>> = flow {
        emit(DatabaseHelper.getAllLessons(subjectId))
    }

    override fun addLesson(lesson: Lesson) {
        DatabaseHelper.addLesson(lesson)
    }

    override fun updateLesson(lesson: Lesson) {
        DatabaseHelper.updateLesson(lesson)
    }

    override fun deleteLesson(lessonId: Int) {
        DatabaseHelper.deleteLesson(lessonId)
    }

    override fun getAllExams(subjectId: Int): Flow<List<Exam>> = flow {
        emit(DatabaseHelper.getAllExams(subjectId))
    }

    override fun addExam(exam: Exam) {
        DatabaseHelper.addExam(exam)
    }

    override fun updateExam(exam: Exam) {
        DatabaseHelper.updateExam(exam)
    }

    override fun deleteExam(examId: Int) {
        DatabaseHelper.deleteExam(examId)
    }

    override fun saveExamRecord(record: ExamRecord) {
        DatabaseHelper.saveExamRecord(record)
    }

    override fun getAllExamRecords(subjectId: Int?): Flow<List<ExamRecord>> = flow {
        emit(DatabaseHelper.getAllExamRecords(subjectId))
    }

    override fun deleteExamRecord(id: Int) {
        DatabaseHelper.deleteExamRecord(id)
    }

    override fun clearAllExamRecords(subjectId: Int?) {
        DatabaseHelper.clearAllExamRecords(subjectId)
    }

    override fun getLesson(lessonId: Int): Lesson? = DatabaseHelper.getLesson(lessonId)

    override fun getExam(examId: Int): Exam? = DatabaseHelper.getExam(examId)

    override fun getGroupAssignment(groupId: Int): GroupAssignment? = DatabaseHelper.getGroupAssignment(groupId)

    override fun setGroupAssignment(assignment: GroupAssignment) {
        DatabaseHelper.setGroupAssignment(assignment)
    }

    override fun clearGroupAssignment(groupId: Int) {
        DatabaseHelper.clearGroupAssignment(groupId)
    }

    override fun getGroupOverviews(subjectId: Int?): List<GroupOverview> = DatabaseHelper.getGroupOverviews(subjectId)

    override fun getAllGroups(subjectId: Int): Flow<List<Group>> = flow {
        emit(DatabaseHelper.getAllGroups(subjectId))
    }

    override fun addGroup(name: String, subjectId: Int, password: String): Flow<Int> = flow {
        emit(DatabaseHelper.addGroup(name, subjectId, password))
    }

    override fun getGroupPassword(groupId: Int): String? = DatabaseHelper.getGroupPassword(groupId)

    override fun setGroupPassword(groupId: Int, password: String) {
        DatabaseHelper.setGroupPassword(groupId, password)
    }

    override fun checkGroupPassword(groupId: Int, password: String): Boolean =
        DatabaseHelper.checkGroupPassword(groupId, password)

    override fun updateGroup(group: Group) {
        DatabaseHelper.updateGroup(group)
    }

    override fun deleteGroup(id: Int) {
        DatabaseHelper.deleteGroup(id)
    }

    override fun getStudentsByGroup(groupId: Int): Flow<List<Student>> = flow {
        emit(DatabaseHelper.getStudentsByGroup(groupId))
    }

    override fun addStudent(name: String, groupId: Int): Flow<Int> = flow {
        emit(DatabaseHelper.addStudent(name, groupId))
    }

    override fun deleteStudent(id: Int) {
        DatabaseHelper.deleteStudent(id)
    }

    override fun getStudentRecords(studentId: Int): Flow<List<ExamRecord>> = flow {
        emit(DatabaseHelper.getStudentRecords(studentId))
    }

    override fun getGroupRecords(groupId: Int, subjectId: Int?): Flow<List<ExamRecord>> = flow {
        emit(DatabaseHelper.getGroupRecords(groupId, subjectId))
    }

    override fun getAllGroupsWithStats(subjectId: Int): Flow<List<Pair<Group, Int>>> = flow {
        emit(DatabaseHelper.getAllGroupsWithStats(subjectId))
    }
}
