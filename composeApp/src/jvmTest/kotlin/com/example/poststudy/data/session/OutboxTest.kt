package com.example.poststudy.data.session

import com.example.poststudy.TestFixtures
import com.example.poststudy.domain.model.Submission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutboxTest {

    private fun entry(id: String) = Outbox.Entry(
        host = "192.168.1.5",
        port = 8080,
        groupPassword = "1234",
        submission = Submission(
            token = "t-$id", groupId = 1, studentId = 1, studentName = "Aliyev",
            answers = listOf(0, null, 2), timeSpentSeconds = 60, submissionId = id
        )
    )

    @Test
    fun entriesSurviveAndSameSubmissionIsKeptOnce() {
        // user.home points at a temp folder, so the real outbox stays untouched
        TestFixtures.initDatabase()
        Outbox.all().forEach { Outbox.remove(it.submission.submissionId) }

        Outbox.add(entry("a"))
        Outbox.add(entry("b"))
        Outbox.add(entry("a"))

        val stored = Outbox.all()
        assertEquals(listOf("b", "a"), stored.map { it.submission.submissionId })
        assertEquals("192.168.1.5", stored.first().address.host)
        // A skipped answer survives the file round trip as null
        assertEquals(listOf(0, null, 2), stored.first().submission.answers)

        Outbox.remove("a")
        assertEquals(listOf("b"), Outbox.all().map { it.submission.submissionId })
        Outbox.remove("b")
        assertTrue(Outbox.all().isEmpty())
    }

    @Test
    fun unreachableAdminKeepsTheEntry() {
        TestFixtures.initDatabase()
        Outbox.all().forEach { Outbox.remove(it.submission.submissionId) }
        Outbox.add(entry("c").copy(host = "127.0.0.1", port = 1))

        assertEquals(0, Outbox.flush())
        assertEquals(1, Outbox.all().size)
        Outbox.remove("c")
    }
}
