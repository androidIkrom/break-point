package com.example.poststudy.data.session

import com.example.poststudy.domain.model.GradedResult
import com.example.poststudy.domain.model.Question
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Sessions the admin computer handed out to students, by token. The questions kept here still
 * carry the correct answers, so a submission can be graded without trusting the student's
 * computer. A token grades one attempt; a repeated request with the same submission id gets
 * the same result back, any other submission on that token is refused.
 */
object ServedSessions {
    class Served(
        val groupId: Int,
        val subjectId: Int,
        val title: String,
        val questions: List<Question>,
        val testTimerSeconds: Int,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        @Volatile var submittedId: String? = null
        @Volatile var result: GradedResult? = null
    }

    private const val TTL_MS = 12L * 60 * 60 * 1000
    private const val MAX_LIVE = 2000

    private val served = ConcurrentHashMap<String, Served>()

    fun issue(groupId: Int, session: PreparedSession): String {
        prune()
        val token = UUID.randomUUID().toString()
        served[token] = Served(groupId, session.subjectId, session.title, session.questions, session.testTimerSeconds)
        return token
    }

    fun find(token: String): Served? {
        val entry = served[token] ?: return null
        if (System.currentTimeMillis() - entry.createdAt > TTL_MS) {
            served.remove(token)
            return null
        }
        return entry
    }

    fun clear() = served.clear()

    private fun prune() {
        val now = System.currentTimeMillis()
        served.entries.removeIf { now - it.value.createdAt > TTL_MS }
        if (served.size >= MAX_LIVE) {
            served.entries.sortedBy { it.value.createdAt }.take(served.size - MAX_LIVE + 1).forEach { served.remove(it.key) }
        }
    }
}
