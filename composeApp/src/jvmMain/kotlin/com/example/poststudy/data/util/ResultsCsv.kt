package com.example.poststudy.data.util

import com.example.poststudy.domain.model.ExamRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exam results as a spreadsheet file. Semicolon-separated with a UTF-8 byte order mark, which is
 * what Excel on an Uzbek or Russian Windows opens correctly by double-click.
 */
object ResultsCsv {
    private const val SEPARATOR = ";"
    val header = listOf(
        "Sana", "Vaqt", "Tinglovchi", "Guruh", "Mashg'ulot", "Savollar", "To'g'ri", "Noto'g'ri",
        "Foiz", "Sarflangan vaqt (soniya)", "Xatolar"
    )

    fun build(records: List<ExamRecord>, groupNames: Map<Int, String>): ByteArray {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        val lines = ArrayList<String>(records.size + 1)
        lines += header.joinToString(SEPARATOR, transform = ::quote)
        for (r in records.sortedByDescending { it.timestamp }) {
            val percent = if (r.totalQuestions > 0) r.correctAnswers * 100 / r.totalQuestions else 0
            val stamp = Date(r.timestamp)
            lines += listOf(
                date.format(stamp),
                time.format(stamp),
                r.studentName,
                r.groupId?.let { groupNames[it] } ?: "",
                r.lessonTitle,
                r.totalQuestions.toString(),
                r.correctAnswers.toString(),
                r.wrongAnswers.toString(),
                percent.toString(),
                r.timeSpentSeconds.toString(),
                r.wrongDetails.replace("\r", "").replace("\n", " | ")
            ).joinToString(SEPARATOR, transform = ::quote)
        }
        return ("﻿" + lines.joinToString("\r\n") + "\r\n").toByteArray(Charsets.UTF_8)
    }

    private fun quote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}
