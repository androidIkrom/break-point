package com.example.poststudy.domain

import com.example.poststudy.domain.model.*
import kotlin.test.*

class StatisticsTest {
    private fun record(name: String, studentId: Int?, groupId: Int?, correct: Int, total: Int, time: Long) = ExamRecord(
        studentName = name, lessonTitle = "T", totalQuestions = total, correctAnswers = correct,
        wrongAnswers = total - correct, wrongDetails = "", timeSpentSeconds = 0, timestamp = time,
        groupId = groupId, studentId = studentId
    )

    @Test
    fun studentsAreGroupedAndNamed() {
        val rows = Statistics.byStudent(
            listOf(
                record("Ali", 1, 10, 5, 10, 1),
                record("Ali Valiyev", 1, 10, 10, 10, 2), // renamed later: latest name wins
                record("Vali", 2, 10, 9, 10, 3),
                record("anonim", null, null, 3, 10, 4),
                record("Bo'sh", 3, 10, 0, 0, 5) // no questions: ignored
            )
        )
        assertEquals(listOf("Vali", "Ali Valiyev", "anonim"), rows.map { it.name })
        val ali = rows.first { it.name == "Ali Valiyev" }
        assertEquals(2, ali.attempts)
        assertEquals(75, ali.average)
        assertEquals(100, ali.best)
        assertEquals(50, ali.worst)
        assertEquals(100, ali.last)
    }

    @Test
    fun groupsIncludeEmptyOnes() {
        val groups = listOf(Group(10, "A", 1), Group(11, "B", 1))
        val rows = Statistics.byGroup(listOf(record("Ali", 1, 10, 8, 10, 1), record("Vali", 2, 10, 6, 10, 2)), groups)
        assertEquals(listOf("A", "B"), rows.map { it.name })
        assertEquals(2, rows[0].students)
        assertEquals(70, rows[0].average)
        assertNull(rows[1].average)
        assertEquals(PerformanceLevel.NoData, PerformanceLevel.of(rows[1].average))
        assertEquals(PerformanceLevel.Good, PerformanceLevel.of(70))
        assertEquals(PerformanceLevel.Excellent, PerformanceLevel.of(85))
        assertEquals(PerformanceLevel.Low, PerformanceLevel.of(59))
    }
}
