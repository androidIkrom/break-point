package com.example.poststudy.data.local

import com.example.poststudy.TestFixtures
import com.example.poststudy.data.session.SessionBuilder
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.test.*

class MaterialStoreTest {
    private val repo get() = AppContainer.localRepository

    private fun newSubject(): Int = runBlocking {
        TestFixtures.initDatabase()
        repo.addSubject("Fan ${System.nanoTime()}").first()
    }

    @Test
    fun lessonKeepsWorkingAfterOriginalFilesAreDeleted() = runBlocking {
        val subjectId = newSubject()
        val groupId = repo.addGroup("G", subjectId).first()
        val dir = TestFixtures.tempDir()
        val pptx = TestFixtures.pptx(dir, 2)
        val docx = TestFixtures.docx(dir, 3)
        repo.addLesson(Lesson(title = "Dars", presentationPath = pptx, testPath = docx, slideTimerSeconds = 300, testTimerSeconds = 300, subjectId = subjectId))
        val lesson = repo.getAllLessons(subjectId).first().single()

        assertTrue(MaterialStore.isManaged(lesson.presentationPath))
        assertTrue(MaterialStore.isManaged(lesson.testPath))
        assertEquals("slides.pptx", File(lesson.presentationPath).name)

        // The teacher deletes the originals
        assertTrue(File(pptx).delete())
        assertTrue(File(docx).delete())

        repo.setGroupAssignment(GroupAssignment(groupId, AssignmentKind.Lesson, lesson.id, LessonMode.ReAppropriation))
        val session = SessionBuilder.buildForGroup(groupId).getOrThrow()
        assertEquals(2, session.slides.size)
        assertEquals(3, session.questions.size)
    }

    @Test
    fun replacedAndDeletedMaterialsAreCleanedUp() = runBlocking {
        val subjectId = newSubject()
        val dir = TestFixtures.tempDir()
        repo.addExam(Exam(title = "Imtihon", testPath = TestFixtures.docx(dir, 3), testTimerSeconds = 600, questionsPerStudent = 2, subjectId = subjectId))
        val exam = repo.getAllExams(subjectId).first().single()
        val firstCopy = exam.testPath
        assertTrue(File(firstCopy).exists())

        // Saving without changing the file keeps the same copy
        repo.updateExam(exam.copy(title = "Yangi nom"))
        assertEquals(firstCopy, repo.getExam(exam.id)!!.testPath)
        assertTrue(File(firstCopy).exists())

        // A new file replaces the copy and the old one goes away
        val otherDir = TestFixtures.tempDir()
        repo.updateExam(exam.copy(testPath = TestFixtures.docx(otherDir, 4)))
        val secondCopy = repo.getExam(exam.id)!!.testPath
        assertNotEquals(firstCopy, secondCopy)
        assertFalse(File(firstCopy).exists())

        repo.deleteExam(exam.id)
        assertFalse(File(secondCopy).exists())
    }

    @Test
    fun oldLessonsAreMigratedOnStartup() = runBlocking {
        val subjectId = newSubject()
        val dir = TestFixtures.tempDir()
        val docx = TestFixtures.docx(dir, 2)
        // A row written by an older version that stored the teacher's path
        transaction {
            LessonsTable.insert {
                it[title] = "Eski dars"
                it[presentationPath] = ""
                it[testPath] = docx
                it[slideTimerMinutes] = 0
                it[testTimerMinutes] = 5
                it[mode] = LessonMode.TestOnly
                it[LessonsTable.subjectId] = org.jetbrains.exposed.dao.id.EntityID(subjectId, SubjectsTable)
            }
        }
        DatabaseHelper.init()
        val migrated = repo.getAllLessons(subjectId).first().single()
        assertTrue(MaterialStore.isManaged(migrated.testPath))
        assertTrue(File(docx).exists(), "the original is left alone")
    }

    @Test
    fun missingOrBlankPathsAreKept() {
        TestFixtures.initDatabase()
        assertEquals("", MaterialStore.import(""))
        assertEquals("C:/yoq/fayl.docx", MaterialStore.import("C:/yoq/fayl.docx"))
    }
}
