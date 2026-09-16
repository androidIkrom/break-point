package com.example.poststudy.domain.model

/** One row of the monitoring table: a student or a group with its results summarized. */
data class PerformanceRow(
    val key: String,
    val name: String,
    val attempts: Int,
    val students: Int,
    val average: Int?,
    val best: Int?,
    val worst: Int?,
    val last: Int?,
    val lastTimestamp: Long?
)

enum class PerformanceLevel(val label: String) {
    Excellent("A'lo"),
    Good("Yaxshi"),
    Low("Past"),
    NoData("Natija yo'q");

    companion object {
        fun of(average: Int?): PerformanceLevel = when {
            average == null -> NoData
            average >= 85 -> Excellent
            average >= 60 -> Good
            else -> Low
        }
    }
}

object Statistics {
    fun percent(record: ExamRecord): Int =
        if (record.totalQuestions > 0) record.correctAnswers * 100 / record.totalQuestions else 0

    private fun studentKey(record: ExamRecord): String =
        record.studentId?.let { "id:$it" } ?: "name:${record.studentName.trim().lowercase()}"

    private fun summarize(key: String, name: String, records: List<ExamRecord>): PerformanceRow {
        val valid = records.filter { it.totalQuestions > 0 }
        val scores = valid.map(::percent)
        val latest = valid.maxByOrNull { it.timestamp }
        return PerformanceRow(
            key = key,
            name = name,
            attempts = valid.size,
            students = valid.map(::studentKey).distinct().size,
            average = if (scores.isEmpty()) null else scores.average().let { Math.round(it).toInt() },
            best = scores.maxOrNull(),
            worst = scores.minOrNull(),
            last = latest?.let(::percent),
            lastTimestamp = latest?.timestamp
        )
    }

    /** One row per student, named after their latest record; best average first. */
    fun byStudent(records: List<ExamRecord>): List<PerformanceRow> =
        records.filter { it.totalQuestions > 0 }
            .groupBy(::studentKey)
            .map { (key, list) -> summarize(key, list.maxBy { it.timestamp }.studentName.ifBlank { "Anonim" }, list) }
            .sortedWith(compareByDescending<PerformanceRow> { it.average ?: -1 }.thenBy { it.name.lowercase() })

    /** One row per group (groups without results included); best average first. */
    fun byGroup(records: List<ExamRecord>, groups: List<Group>): List<PerformanceRow> {
        val byGroup = records.groupBy { it.groupId }
        return groups.map { group -> summarize("group:${group.id}", group.name, byGroup[group.id].orEmpty()) }
            .sortedWith(compareByDescending<PerformanceRow> { it.average ?: -1 }.thenBy { it.name.lowercase() })
    }

    /** Headline numbers over [records]. */
    fun overall(records: List<ExamRecord>): PerformanceRow = summarize("all", "Hammasi", records)
}
