package com.example.poststudy.data.local

import com.example.poststudy.TestFixtures
import com.example.poststudy.domain.model.ExamRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResultsStorageTest {

    private fun record(student: String, groupId: Int?, studentId: Int?, submissionId: String? = null) = ExamRecord(
        studentName = student,
        lessonTitle = "Dars",
        totalQuestions = 10,
        correctAnswers = 7,
        wrongAnswers = 3,
        wrongDetails = "",
        timeSpentSeconds = 120,
        timestamp = System.currentTimeMillis(),
        groupId = groupId,
        studentId = studentId,
        subjectId = 1,
        submissionId = submissionId
    )

    @Test
    fun retriedNetworkSubmitIsStoredOnce() {
        TestFixtures.initDatabase()
        val before = DatabaseHelper.getAllExamRecords(1).size
        val id = "sub-${System.nanoTime()}"
        DatabaseHelper.saveExamRecord(record("Aliyev", null, null, id))
        DatabaseHelper.saveExamRecord(record("Aliyev", null, null, id))
        assertEquals(before + 1, DatabaseHelper.getAllExamRecords(1).size)
        assertEquals(id, DatabaseHelper.getAllExamRecords(1).first { it.studentName == "Aliyev" && it.submissionId == id }.submissionId)
    }

    @Test
    fun localResultsWithoutSubmissionIdAreAllKept() {
        TestFixtures.initDatabase()
        val before = DatabaseHelper.getAllExamRecords(1).size
        DatabaseHelper.saveExamRecord(record("Karimov", null, null))
        DatabaseHelper.saveExamRecord(record("Karimov", null, null))
        assertEquals(before + 2, DatabaseHelper.getAllExamRecords(1).size)
    }

    @Test
    fun deletingGroupRemovesItsStudentsAndResults() {
        TestFixtures.initDatabase()
        val groupId = DatabaseHelper.addGroup("Guruh ${System.nanoTime()}", 1)
        val studentId = DatabaseHelper.addStudent("Rahimov", groupId)
        DatabaseHelper.saveExamRecord(record("Rahimov", groupId, studentId))
        DatabaseHelper.saveExamRecord(record("Rahimov", groupId, null))

        DatabaseHelper.deleteGroup(groupId)

        assertTrue(DatabaseHelper.getStudentsByGroup(groupId).isEmpty())
        assertTrue(DatabaseHelper.getAllExamRecords(1).none { it.groupId == groupId || it.studentId == studentId })
    }

    @Test
    fun schemaVersionIsRecorded() {
        TestFixtures.initDatabase()
        assertEquals(SchemaMigrations.CURRENT, SchemaMigrations.currentVersion())
    }
}
