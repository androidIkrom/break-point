package com.example.poststudy.data.session

import com.example.poststudy.domain.model.GradedResult
import com.example.poststudy.domain.model.Question

/** Scores a list of answers against the questions, the same way for local and network sessions. */
object Grading {
    class Outcome(val result: GradedResult, val wrongDetails: String)

    fun grade(questions: List<Question>, answers: List<Int?>): Outcome {
        val wrong = questions.indices.filter { i -> answers.getOrNull(i) != questions[i].correctIndex }
        val details = wrong.joinToString("\n\n") { i ->
            val q = questions[i]
            val correct = q.options.getOrNull(q.correctIndex) ?: "-"
            val chosen = answers.getOrNull(i)?.let { q.options.getOrNull(it) } ?: "O'tkazib yuborildi"
            "S: ${q.text}\nTo'g'ri javob: $correct\nTanlangan javob: $chosen"
        }
        return Outcome(GradedResult(questions.size - wrong.size, questions.size, wrong), details)
    }
}
