package com.example.poststudy.data.local

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Copies of the SQLite file in `~/.breakpoint/backups`: one per day the app was started (the
 * last [KEEP] are kept) and one before every schema migration. Copies are taken while the
 * database is closed, so they are consistent.
 */
object DatabaseBackups {
    private const val KEEP = 7
    private const val PREFIX = "breakpoint-"

    fun folder(dbFile: File): File = File(dbFile.parentFile, "backups")

    /** Today's copy, if it does not exist yet. */
    fun daily(dbFile: File) {
        if (!dbFile.isFile || dbFile.length() == 0L) return
        val target = File(folder(dbFile), "$PREFIX${LocalDate.now()}.db")
        if (target.exists()) return
        runCatching {
            copy(dbFile, target)
            prune(folder(dbFile))
        }.onFailure { println("Backup failed: ${it.message}") }
    }

    /** Kept for good: restores the old file if a migration goes wrong. */
    fun beforeMigration(dbFile: File, fromVersion: Int) {
        if (!dbFile.isFile) return
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        runCatching { copy(dbFile, File(folder(dbFile), "${PREFIX}before-v$fromVersion-$stamp.db")) }
            .onFailure { println("Backup failed: ${it.message}") }
    }

    private fun copy(db: File, target: File) {
        target.parentFile.mkdirs()
        db.copyTo(target, overwrite = true)
        // A crash can leave WAL pages that are not in the main file yet
        for (suffix in listOf("-wal", "-shm")) {
            val extra = File(db.path + suffix)
            if (extra.isFile) extra.copyTo(File(target.path + suffix), overwrite = true)
        }
    }

    private fun prune(dir: File) {
        dir.listFiles { f -> f.name.startsWith(PREFIX) && f.name.endsWith(".db") && !f.name.contains("before") }
            ?.sortedByDescending { it.name }
            ?.drop(KEEP)
            ?.forEach { old ->
                old.delete()
                File(old.path + "-wal").delete()
                File(old.path + "-shm").delete()
            }
    }
}
