package com.example.poststudy.data.session

import com.example.poststudy.data.network.ServerAddress
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.ExamRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Results a student finished but could not deliver to the admin computer. They are kept in
 * `~/.breakpoint/outbox.json` and sent again on the next start of the app, and whenever the
 * student retries from the result screen. Each entry carries its submission id, so a result that
 * did arrive but whose reply was lost is stored once on the admin side.
 */
object Outbox {
    data class Entry(val host: String, val port: Int, val groupPassword: String, val record: ExamRecord) {
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
        val id = entry.record.submissionId
        val rest = all().filter { id == null || it.record.submissionId != id }
        write(rest + entry)
    }

    @Synchronized
    fun remove(submissionId: String?) {
        if (submissionId == null) return
        write(all().filter { it.record.submissionId != submissionId })
    }

    /** Tries to deliver every stored result once. Blocking: call from Dispatchers.IO. Returns how many went through. */
    fun flush(): Int {
        var sent = 0
        for (entry in all()) {
            val result = AppContainer.networkRepository.submitResult(entry.address, entry.record, entry.groupPassword)
            if (result.isSuccess) {
                remove(entry.record.submissionId)
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
