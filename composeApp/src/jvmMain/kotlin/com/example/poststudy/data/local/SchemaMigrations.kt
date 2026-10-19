package com.example.poststudy.data.local

import java.io.File

/**
 * Numbered schema versions. `createMissingTablesAndColumns` adds new tables and columns by
 * itself; a step here is for everything else (moving data, backfilling, dropping). The version
 * reached is stored in `app_meta_v1`, and the database file is backed up before any step runs.
 */
object SchemaMigrations {
    const val CURRENT = 2
    private const val KEY = "schema_version"

    private val steps: Map<Int, () -> Unit> = mapOf(
        // 1: the tables as shipped in 1.1.x; nothing to move
        1 to {},
        // 2: exam_records_v5.submission_id for duplicate-safe network submits; old rows stay null
        2 to {}
    )

    fun currentVersion(): Int = DatabaseHelper.getMeta(KEY)?.toIntOrNull() ?: 0

    /** Runs the steps above [from] up to [CURRENT]. [hadData] is false for a brand-new file. */
    fun apply(dbFile: File, hadData: Boolean) {
        val from = currentVersion()
        if (from >= CURRENT) return
        if (hadData) DatabaseBackups.beforeMigration(dbFile, from)
        for (version in (from + 1)..CURRENT) {
            steps[version]?.invoke()
            DatabaseHelper.setMeta(KEY, version.toString())
            println("Database schema at version $version")
        }
    }
}
