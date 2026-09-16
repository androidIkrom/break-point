package com.example.poststudy.data.session

import com.example.poststudy.TestFixtures
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SessionBuilderTest {
    private val repo get() = AppContainer.localRepository

    private fun newSubjectWithGroup(): Pair<Int, Int> = runBlocking {
        TestFixtures.initDatabase()
        val subjectId = repo.addSubject("Fan ${System.nanoTime()}").first()
        subjectId to repo.addGroup("Guruh", subjectId).first()
    }

    private fun addLesson(subjectId: Int, mode: LessonMode): Lesson = runBlocking {
        val dir = TestFixtures.tempDir()
        repo.addLesson(
            Lesson(
                title = "Dars $mode",
                presentationPath = if (mode.hasSlides) TestFixtures.pptx(dir, 3) else "",
                testPath = if (mode.hasTest) TestFixtures.docx(dir, 4) else "",
                slideTimerSeconds = if (mode.hasSlides) 600 else 0,
                testTimerSeconds = if (mode.hasTest) 900 else 0,
                mode = mode,
                subjectId = subjectId
            )
        )
        repo.getAllLessons(subjectId).first().last()
    }

    @Test
    fun unassignedGroupGivesMessage() {
        val (_, groupId) = newSubjectWithGroup()
        val error = SessionBuilder.buildForGroup(groupId).exceptionOrNull()
        assertNotNull(error)
        assertTrue(error.message!!.contains("biriktirilmagan"))
    }

    @Test
    fun lessonWithBothMaterialsFollowsChosenMode() {
        val (subjectId, groupId) = newSubjectWithGroup()
        val lesson = addLesson(subjectId, LessonMode.ReAppropriation)

        repo.setGroupAssignment(GroupAssignment(groupId, AssignmentKind.Lesson, lesson.id, LessonMode.ReAppropriation))
        SessionBuilder.buildForGroup(groupId).getOrThrow().let {
            assertEquals(3, it.slides.size)
            assertEquals(4, it.questions.size)
            assertEquals(600, it.slideTimerSeconds)
        }

        repo.setGroupAssignment(GroupAssignment(groupId, AssignmentKind.Lesson, lesson.id, LessonMode.TestOnly))
        SessionBuilder.buildForGroup(groupId).getOrThrow().let {
            assertEquals(LessonMode.TestOnly, it.mode)
            assertTrue(it.slides.isEmpty())
            assertEquals(4, it.questions.size)
        }

        repo.setGroupAssignment(GroupAssignment(groupId, AssignmentKind.Lesson, lesson.id, LessonMode.PresentationOnly))
        SessionBuilder.buildForGroup(groupId).getOrThrow().let {
            assertEquals(LessonMode.PresentationOnly, it.mode)
            assertEquals(3, it.slides.size)
            assertTrue(it.questions.isEmpty())
        }
    }

    @Test
    fun presentationOnlyLessonIgnoresImpossibleMode() {
        val (subjectId, groupId) = newSubjectWithGroup()
        val lesson = addLesson(subjectId, LessonMode.PresentationOnly)
        assertEquals(listOf(LessonMode.PresentationOnly), allowedModes(lesson))

        // A stale "test only" assignment must not produce an empty test
        repo.setGroupAssignment(GroupAssignment(groupId, AssignmentKind.Lesson, lesson.id, LessonMode.TestOnly))
        val session = SessionBuilder.buildForGroup(groupId).getOrThrow()
        assertEquals(LessonMode.PresentationOnly, session.mode)
        assertEquals(3, session.slides.size)
    }

    @Test
    fun examIsAlwaysTestOnlyWithQuestionLimit() {
        val (subjectId, groupId) = newSubjectWithGroup()
        val dir = TestFixtures.tempDir()
        repo.addExam(Exam(title = "Yakuniy", testPath = TestFixtures.docx(dir, 10), testTimerSeconds = 1200, questionsPerStudent = 5, subjectId = subjectId))
        val exam = runBlocking { repo.getAllExams(subjectId).first().single() }

        repo.setGroupAssignment(GroupAssignment(groupId, AssignmentKind.Exam, exam.id, LessonMode.ReAppropriation))
        assertEquals(LessonMode.TestOnly, repo.getGroupAssignment(groupId)!!.mode)

        val session = SessionBuilder.buildForGroup(groupId).getOrThrow()
        assertEquals(AssignmentKind.Exam, session.kind)
        assertEquals(5, session.questions.size)
        assertTrue(session.slides.isEmpty())
        // Options are shuffled but the correct one must still be "To'g'ri"
        session.questions.forEach { assertEquals("To'g'ri", it.options[it.correctIndex]) }
    }

    @Test
    fun overviewsAndCascades() {
        val (subjectId, groupId) = newSubjectWithGroup()
        val otherGroup = runBlocking { repo.addGroup("Bo'sh guruh", subjectId).first() }
        val lesson = addLesson(subjectId, LessonMode.TestOnly)
        repo.setGroupAssignment(GroupAssignment(groupId, AssignmentKind.Lesson, lesson.id, LessonMode.TestOnly))

        val overviews = repo.getGroupOverviews(subjectId).associateBy { it.id }
        assertEquals(lesson.title, overviews.getValue(groupId).assignmentTitle)
        assertTrue(overviews.getValue(groupId).isAssigned)
        assertFalse(overviews.getValue(otherGroup).isAssigned)
        assertTrue(repo.getGroupOverviews(null).any { it.id == groupId })

        // Deleting the lesson removes the assignment instead of leaving a broken one
        repo.deleteLesson(lesson.id)
        assertNull(repo.getGroupAssignment(groupId))

        repo.setGroupAssignment(GroupAssignment(otherGroup, AssignmentKind.Lesson, addLesson(subjectId, LessonMode.TestOnly).id, LessonMode.TestOnly))
        repo.deleteGroup(otherGroup)
        assertNull(repo.getGroupAssignment(otherGroup))
    }
}
