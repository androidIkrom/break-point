package com.example.poststudy.data.session

import com.example.poststudy.domain.model.GradedResult
import com.example.poststudy.domain.model.Question
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GradingTest {
    private val questions = listOf(
        Question("Bir?", listOf("a", "b", "c"), 1),
        Question("Ikki?", listOf("a", "b", "c"), 0),
        Question("Uch?", listOf("a", "b", "c"), 2)
    )

    @Test
    fun skippedAndWrongAnswersCountAsWrong() {
        val outcome = Grading.grade(questions, listOf(1, null, 0))
        assertEquals(GradedResult(1, 3, listOf(1, 2)), outcome.result)
        val blocks = outcome.wrongDetails.split("\n\n")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0].contains("Tanlangan javob: O'tkazib yuborildi"))
        assertTrue(blocks[1].contains("To'g'ri javob: c"))
        assertTrue(blocks[1].contains("Tanlangan javob: a"))
    }

    @Test
    fun allCorrectHasNoDetails() {
        val outcome = Grading.grade(questions, listOf(1, 0, 2))
        assertEquals(GradedResult(3, 3, emptyList()), outcome.result)
        assertEquals("", outcome.wrongDetails)
    }

    @Test
    fun shortAnswerListTreatsMissingAsSkipped() {
        assertEquals(GradedResult(1, 3, listOf(1, 2)), Grading.grade(questions, listOf(1)).result)
    }
}
