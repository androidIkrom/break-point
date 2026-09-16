package com.example.poststudy.data.local

import java.io.File
import java.util.UUID

/**
 * Keeps private copies of the Word/PowerPoint files a lesson or exam uses, so deleting or
 * moving the teacher's original file does not break it. Each copy lives in its own folder to
 * keep the original file name visible.
 */
object MaterialStore {
    val root: File get() = File(System.getProperty("user.home"), ".breakpoint/materials")

    fun isManaged(path: String): Boolean =
        path.isNotBlank() && runCatching {
            File(path).canonicalFile.toPath().startsWith(root.canonicalFile.toPath())
        }.getOrDefault(false)

    /**
     * Returns the path of the private copy of [path]. Blank, already managed or missing files are
     * returned unchanged (a missing file is reported by the lesson list).
     */
    fun import(path: String): String {
        if (path.isBlank() || isManaged(path)) return path
        val source = File(path)
        if (!source.isFile) return path
        return try {
            val target = File(root, UUID.randomUUID().toString()).resolve(source.name)
            target.parentFile.mkdirs()
            source.copyTo(target)
            target.path
        } catch (e: Exception) {
            e.printStackTrace()
            path
        }
    }

    /** Deletes a private copy; the caller makes sure nothing references it any more. */
    fun delete(path: String) {
        if (!isManaged(path)) return
        runCatching {
            val file = File(path)
            val folder = file.parentFile
            file.delete()
            if (folder != null && folder.canonicalFile != root.canonicalFile && folder.listFiles().isNullOrEmpty()) folder.delete()
        }
    }

    fun deleteAll() {
        runCatching { root.deleteRecursively() }
    }
}
