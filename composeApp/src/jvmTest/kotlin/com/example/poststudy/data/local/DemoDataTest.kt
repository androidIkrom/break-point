package com.example.poststudy.data.local

import com.example.poststudy.TestFixtures
import com.example.poststudy.data.session.SessionBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoDataTest {

    @Test
    fun freshDatabaseGetsSamplesOnceAndCanDropThemAgain() {
        TestFixtures.initDatabase()
        val subjectId = DatabaseHelper.getAllSubjects().first().id
        // Other tests may already have added rows; a seeded run must only happen on a clean database
        val wasFresh = DatabaseHelper.getAllLessons(subjectId).isEmpty() && DatabaseHelper.getAllExamRecords().isEmpty()

        DemoData.seedIfFresh()
        if (!wasFresh) {
            assertEquals("skipped", DatabaseHelper.getMeta("demo_seeded"))
            assertFalse(DemoData.isPresent())
            return
        }

        val lessons = DatabaseHelper.getAllLessons(subjectId)
        assertEquals(5, lessons.size)
        assertEquals(2, DatabaseHelper.getAllExams(subjectId).size)
        assertEquals(2, DatabaseHelper.getAllGroups(subjectId).size)
        assertEquals(11, DatabaseHelper.getAllExamRecords(subjectId).size)
        assertTrue(lessons.all { MaterialStore.isManaged(it.testPath) && java.io.File(it.testPath).isFile })
        assertTrue(DemoData.isPresent())

        // The seeded lesson really works end to end
        val group1 = DatabaseHelper.getAllGroups(subjectId).first().id
        val session = SessionBuilder.buildForGroup(group1).getOrThrow()
        assertEquals(8, session.questions.size)

        // Running again changes nothing
        DemoData.seedIfFresh()
        assertEquals(5, DatabaseHelper.getAllLessons(subjectId).size)

        DemoData.removeAll()
        assertFalse(DemoData.isPresent())
        assertTrue(DatabaseHelper.getAllLessons(subjectId).isEmpty())
        assertTrue(DatabaseHelper.getAllExams(subjectId).isEmpty())
        assertEquals(1, DatabaseHelper.getAllGroups(subjectId).size)
        assertTrue(DatabaseHelper.getAllExamRecords(subjectId).isEmpty())
        assertTrue(DatabaseHelper.getStudentsByGroup(group1).isEmpty())
    }
}
