package com.example.poststudy.data.local

import com.example.poststudy.domain.model.Lesson
import com.example.poststudy.domain.model.LessonMode
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.poststudy.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseHelperTest {

    @Test
    fun unknownModeFromOtherBuildsDoesNotCrash() {
        TestFixtures.initDatabase()
        DatabaseHelper.addLesson(Lesson(title = "ospf", presentationPath = "a.pptx", testPath = "a.docx", slideTimerSeconds = 1200, testTimerSeconds = 1800))
        DatabaseHelper.addLesson(Lesson(title = "quiz", presentationPath = "", testPath = "b.docx", slideTimerSeconds = 0, testTimerSeconds = 600, mode = LessonMode.TestOnly))

        // What another BreakPoint build writes into the shared database
        transaction {
            exec("UPDATE lessons_v3 SET mode = 'Seminar' WHERE title = 'ospf'")
            exec("UPDATE settings_v5 SET mode = 'Seminar'")
        }

        val lessons = DatabaseHelper.getAllLessons(1).associate { it.title to it.mode }
        assertEquals(LessonMode.ReAppropriation, lessons["ospf"])
        assertEquals(LessonMode.TestOnly, lessons["quiz"])
        assertEquals(LessonMode.ReAppropriation, DatabaseHelper.getSettings(1).mode)
    }
}
