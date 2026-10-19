package com.example.poststudy.data.system

import com.example.poststudy.AppInfo
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.time.LocalDateTime

/**
 * Copies everything the app prints (and every uncaught exception) into
 * `~/.breakpoint/logs/breakpoint.log`, so a problem on a cadet's computer can be read later.
 * The file rolls over to `breakpoint.1.log` at [MAX_BYTES].
 */
object AppLog {
    private const val MAX_BYTES = 2L * 1024 * 1024

    val folder: File get() = File(System.getProperty("user.home"), ".breakpoint/logs")
    val file: File get() = File(folder, "breakpoint.log")

    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        runCatching {
            folder.mkdirs()
            if (file.isFile && file.length() > MAX_BYTES) {
                val previous = File(folder, "breakpoint.1.log")
                previous.delete()
                file.renameTo(previous)
            }
            val sink = PrintStream(FileOutputStream(file, true), true, "UTF-8")
            System.setOut(Tee(System.out, sink))
            System.setErr(Tee(System.err, sink))
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                System.err.println("Uncaught exception in ${thread.name}:")
                error.printStackTrace()
            }
            println(
                "=== BreakPoint ${AppInfo.version} started ${LocalDateTime.now()} on " +
                    "${System.getProperty("os.name")} ${System.getProperty("os.version")}, Java ${System.getProperty("java.version")}"
            )
        }.onFailure { System.err.println("Log file unavailable: ${it.message}") }
    }

    /** Writes to the console and the log file. */
    private class Tee(private val console: PrintStream, private val sink: PrintStream) : PrintStream(console, true) {
        override fun write(buf: ByteArray, off: Int, len: Int) {
            console.write(buf, off, len)
            sink.write(buf, off, len)
        }

        override fun write(b: Int) {
            console.write(b)
            sink.write(b)
        }

        override fun flush() {
            console.flush()
            sink.flush()
        }
    }
}
