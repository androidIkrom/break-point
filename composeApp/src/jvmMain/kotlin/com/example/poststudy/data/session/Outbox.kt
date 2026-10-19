package com.example.poststudy.data.session

import com.example.poststudy.data.network.ServerAddress
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.Submission
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Answers a student finished but could not deliver to the admin computer. They are kept in
 * `~/.breakpoint/outbox.json` and sent again on the next start of the app, and whenever the
 * student retries from the result screen. Each entry carries its submission id, so answers
 * that did arrive but whose reply was lost are graded and stored once on the admin side.
 */
object Outbox {
    data class Entry(val host: String, val port: Int, val groupPassword: String, val submission: Submission) {
        val address: ServerAddress get() = ServerAddress(host, port)
    }

    private val gson = Gson()
    private val file: File get() = File(System.getProperty("user.home"), ".breakpoint/outbox.json")

    @Synchronized
    fun all(): List<Entry> {
        if (!file.isFile) return emptyList()
        return runCatching {
            gson.fromJson<List<Entry>>(file.readText(), object : TypeToken<List<Entry>>() {}.type).orEmpty()
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun add(entry: Entry) {
        val id = entry.submission.submissionId
        write(all().filter { it.submission.submissionId != id } + entry)
    }

    @Synchronized
    fun remove(submissionId: String) {
        write(all().filter { it.submission.submissionId != submissionId })
    }

    /** Tries to deliver every stored submission once. Blocking: call from Dispatchers.IO. Returns how many went through. */
    fun flush(): Int {
        var sent = 0
        for (entry in all()) {
            val result = AppContainer.networkRepository.submitAnswers(entry.address, entry.submission, entry.groupPassword)
            if (result.isSuccess) {
                remove(entry.submission.submissionId)
                sent++
            }
        }
        return sent
    }

    private fun write(entries: List<Entry>) {
        runCatching {
            if (entries.isEmpty()) file.delete() else {
                file.parentFile.mkdirs()
                file.writeText(gson.toJson(entries))
            }
        }.onFailure { println("Outbox write failed: ${it.message}") }
    }
}
