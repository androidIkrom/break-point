package com.example.poststudy.data.util

import com.example.poststudy.domain.model.ExamRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResultsCsvTest {

    @Test
    fun excelFriendlyFileWithOneRowPerResult() {
        val records = listOf(
            ExamRecord(
                studentName = "Aliyev \"Bobur\"", lessonTitle = "Radio; asoslari", totalQuestions = 6, correctAnswers = 5,
                wrongAnswers = 1, wrongDetails = "S: savol\nTo'g'ri: a\nTanlangan: b", timeSpentSeconds = 90,
                timestamp = 1_700_000_000_000, groupId = 7, studentId = 1, subjectId = 1
            )
        )
        val text = String(ResultsCsv.build(records, mapOf(7 to "Guruh 1")), Charsets.UTF_8)
        val lines = text.removePrefix("﻿").trim().split("\r\n")

        assertTrue(text.startsWith("﻿"))
        assertEquals(2, lines.size)
        assertEquals(ResultsCsv.header.size, lines[0].split(";").size)
        val row = lines[1]
        assertTrue(row.contains("\"Aliyev \"\"Bobur\"\"\""))
        assertTrue(row.contains("\"Radio; asoslari\""))
        assertTrue(row.contains("\"Guruh 1\""))
        assertTrue(row.contains("\"83\""))
        assertTrue(row.contains("S: savol | To'g'ri: a | Tanlangan: b"))
    }
}
