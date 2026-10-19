package com.example.poststudy.data.session

import com.example.poststudy.TestFixtures
import com.example.poststudy.domain.model.ExamRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutboxTest {

    private fun entry(id: String) = Outbox.Entry(
        host = "192.168.1.5",
        port = 8080,
        groupPassword = "1234",
        record = ExamRecord(
            studentName = "Aliyev",
            lessonTitle = "Dars",
            totalQuestions = 5,
            correctAnswers = 4,
            wrongAnswers = 1,
            wrongDetails = "",
            timeSpentSeconds = 60,
            timestamp = 1L,
            groupId = 1,
            studentId = 1,
            subjectId = 1,
            submissionId = id
        )
    )

    @Test
    fun entriesSurviveAndSameSubmissionIsKeptOnce() {
        // user.home points at a temp folder, so the real outbox stays untouched
        TestFixtures.initDatabase()
        Outbox.all().forEach { Outbox.remove(it.record.submissionId) }

        Outbox.add(entry("a"))
        Outbox.add(entry("b"))
        Outbox.add(entry("a"))

        val stored = Outbox.all()
        assertEquals(listOf("b", "a"), stored.map { it.record.submissionId })
        assertEquals("192.168.1.5", stored.first().address.host)

        Outbox.remove("a")
        assertEquals(listOf("b"), Outbox.all().map { it.record.submissionId })
        Outbox.remove("b")
        assertTrue(Outbox.all().isEmpty())
    }

    @Test
    fun unreachableAdminKeepsTheEntry() {
        TestFixtures.initDatabase()
        Outbox.all().forEach { Outbox.remove(it.record.submissionId) }
        Outbox.add(entry("c").copy(host = "127.0.0.1", port = 1))

        assertEquals(0, Outbox.flush())
        assertEquals(1, Outbox.all().size)
        Outbox.remove("c")
    }
}
