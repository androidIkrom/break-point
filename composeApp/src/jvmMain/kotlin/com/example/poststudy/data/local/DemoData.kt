package com.example.poststudy.data.local

import com.example.poststudy.domain.model.*
import com.google.gson.Gson
import java.io.File
import java.util.UUID

/**
 * Sample lessons, exams, groups, students and results for a fresh installation, so the app has
 * something to show before the first real lesson is created. Seeded once per database; the
 * admin edits or deletes the items like any other, and the help screen can remove them all.
 */
object DemoData {
    private const val SEEDED_KEY = "demo_seeded"
    private const val IDS_KEY = "demo_ids"
    private const val GROUP_2_PASSWORD = "1234"

    private class LessonSpec(val title: String, val file: String, val testMinutes: Int)
    private class ExamSpec(val title: String, val file: String, val minutes: Int, val perStudent: Int)

    /** Ids of the rows this object created, so they can be removed again. */
    data class Ids(
        val lessons: List<Int> = emptyList(),
        val exams: List<Int> = emptyList(),
        val groups: List<Int> = emptyList(),
        val students: List<Int> = emptyList()
    )

    private val lessons = listOf(
        LessonSpec("Radioaloqa asoslari", "radioaloqa_asoslari.docx", 12),
        LessonSpec("Tarmoq asoslari", "tarmoq_asoslari.docx", 10),
        LessonSpec("Morze alifbosi va signallar", "morze_va_signallar.docx", 8),
        LessonSpec("Antenna va fider qurilmalari", "antenna_va_fider.docx", 10),
        LessonSpec("Raqamli aloqa", "raqamli_aloqa.docx", 10)
    )
    private val exams = listOf(
        ExamSpec("Yakuniy nazorat", "yakuniy_nazorat.docx", 20, 8),
        ExamSpec("Oraliq nazorat", "tarmoq_asoslari.docx", 10, 5)
    )
    private val group1Students = listOf("Aliyev Bobur", "Karimova Dilnoza", "Rahimov Sardor", "To'xtayev Jasur")
    private val group2Students = listOf("Yusupova Madina", "Ergashev Doston")

    private val gson = Gson()

    /** Seeds a database that has no lessons, exams or results yet. Runs at most once per database. */
    fun seedIfFresh() {
        if (DatabaseHelper.getMeta(SEEDED_KEY) != null) return
        val subjectId = DatabaseHelper.getAllSubjects().firstOrNull()?.id ?: return
        val fresh = DatabaseHelper.getAllLessons(subjectId).isEmpty() &&
            DatabaseHelper.getAllExams(subjectId).isEmpty() &&
            DatabaseHelper.getAllExamRecords().isEmpty()
        if (!fresh) {
            // An upgraded installation with real data: never mix samples into it
            DatabaseHelper.setMeta(SEEDED_KEY, "skipped")
            return
        }
        runCatching { seed(subjectId) }
            .onSuccess { DatabaseHelper.setMeta(SEEDED_KEY, "1") }
            .onFailure { println("Demo data not seeded: ${it.message}") }
    }

    private fun seed(subjectId: Int) {
        val lessonIds = lessons.mapNotNull { spec ->
            val path = copyResource(spec.file) ?: return@mapNotNull null
            DatabaseHelper.addLesson(
                Lesson(
                    title = spec.title, presentationPath = "", testPath = path,
                    slideTimerSeconds = 0, testTimerSeconds = spec.testMinutes * 60,
                    mode = LessonMode.TestOnly, subjectId = subjectId
                )
            )
            DatabaseHelper.getAllLessons(subjectId).last { it.title == spec.title }.id
        }
        val examIds = exams.mapNotNull { spec ->
            val path = copyResource(spec.file) ?: return@mapNotNull null
            DatabaseHelper.addExam(
                Exam(title = spec.title, testPath = path, testTimerSeconds = spec.minutes * 60, questionsPerStudent = spec.perStudent, subjectId = subjectId)
            )
            DatabaseHelper.getAllExams(subjectId).last { it.title == spec.title }.id
        }

        // "Guruh 1" exists on every database; the second group shows the password flow
        val group1 = DatabaseHelper.getAllGroups(subjectId).firstOrNull()?.id
            ?: DatabaseHelper.addGroup("Guruh 1", subjectId)
        val group2 = DatabaseHelper.addGroup("Guruh 2", subjectId, GROUP_2_PASSWORD)
        lessonIds.firstOrNull()?.let { DatabaseHelper.setGroupAssignment(GroupAssignment(group1, AssignmentKind.Lesson, it, LessonMode.TestOnly)) }
        examIds.firstOrNull()?.let { DatabaseHelper.setGroupAssignment(GroupAssignment(group2, AssignmentKind.Exam, it, LessonMode.TestOnly)) }

        val students1 = group1Students.map { it to DatabaseHelper.addStudent(it, group1) }
        val students2 = group2Students.map { it to DatabaseHelper.addStudent(it, group2) }

        // A few results from the last days so monitoring and history are not empty
        val day = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        val lessonTitle = lessons.first().title
        val examTitle = exams.first().title
        val sample = listOf(
            Triple(0, 7 to 8, 2 * day + 3_600_000), Triple(1, 8 to 8, 2 * day + 2_000_000), Triple(2, 5 to 8, 2 * day + 900_000),
            Triple(3, 6 to 8, day + 5_400_000), Triple(0, 8 to 8, day + 3_600_000), Triple(2, 6 to 8, day + 1_200_000),
            Triple(1, 7 to 8, 7_200_000L), Triple(3, 4 to 8, 3_600_000L)
        )
        sample.forEach { (index, score, ago) ->
            val (name, id) = students1[index]
            DatabaseHelper.saveExamRecord(record(name, id, group1, subjectId, lessonTitle, score.first, score.second, now - ago))
        }
        listOf(Triple(0, 7 to 8, day + 1_800_000), Triple(1, 5 to 8, day + 600_000), Triple(0, 8 to 8, 5_400_000L)).forEach { (index, score, ago) ->
            val (name, id) = students2[index]
            DatabaseHelper.saveExamRecord(record(name, id, group2, subjectId, examTitle, score.first, score.second, now - ago))
        }

        DatabaseHelper.setMeta(
            IDS_KEY,
            gson.toJson(Ids(lessonIds, examIds, listOf(group2), (students1 + students2).map { it.second }))
        )
    }

    private fun record(name: String, studentId: Int, groupId: Int, subjectId: Int, title: String, correct: Int, total: Int, at: Long): ExamRecord {
        val wrong = total - correct
        val details = if (wrong == 0) "" else (1..wrong).joinToString("\n\n") {
            "S: Namuna savol $it\nTo'g'ri javob: To'g'ri variant\nTanlangan javob: Boshqa variant"
        }
        return ExamRecord(
            studentName = name, lessonTitle = title, totalQuestions = total, correctAnswers = correct,
            wrongAnswers = wrong, wrongDetails = details, timeSpentSeconds = 240 + correct * 35,
            timestamp = at, groupId = groupId, studentId = studentId, subjectId = subjectId
        )
    }

    /** Copies a bundled Word file into the material store like a teacher's own file. */
    private fun copyResource(name: String): String? {
        val stream = DemoData::class.java.classLoader.getResourceAsStream("demo/$name") ?: return null
        val target = File(MaterialStore.root, UUID.randomUUID().toString()).resolve(name)
        target.parentFile.mkdirs()
        stream.use { input -> target.outputStream().use { input.copyTo(it) } }
        return target.path
    }

    fun seededIds(): Ids? = DatabaseHelper.getMeta(IDS_KEY)?.let { runCatching { gson.fromJson(it, Ids::class.java) }.getOrNull() }

    /** True while any seeded lesson, exam or group still exists. */
    fun isPresent(): Boolean {
        val ids = seededIds() ?: return false
        val subjects = DatabaseHelper.getAllSubjects().map { it.id }
        val lessonsLeft = ids.lessons.any { DatabaseHelper.getLesson(it) != null }
        val examsLeft = ids.exams.any { DatabaseHelper.getExam(it) != null }
        val groupsLeft = subjects.any { s -> DatabaseHelper.getAllGroups(s).any { it.id in ids.groups } }
        return lessonsLeft || examsLeft || groupsLeft
    }

    /** Removes whatever is left of the samples; real data the admin added later stays. */
    fun removeAll() {
        val ids = seededIds() ?: return
        ids.lessons.forEach { if (DatabaseHelper.getLesson(it) != null) DatabaseHelper.deleteLesson(it) }
        ids.exams.forEach { if (DatabaseHelper.getExam(it) != null) DatabaseHelper.deleteExam(it) }
        // Students of the kept default group go with their results; the extra group goes whole
        ids.students.forEach { DatabaseHelper.deleteStudent(it) }
        ids.groups.forEach { DatabaseHelper.deleteGroup(it) }
        DatabaseHelper.setMeta(IDS_KEY, gson.toJson(Ids()))
    }
}
