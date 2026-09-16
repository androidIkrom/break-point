package com.example.poststudy.data.system

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Windows Defender Firewall checks for the admin computer. When the first "allow access" prompt
 * is cancelled, Windows silently adds block rules and never asks again; [fix] removes those and
 * allows the app for the local subnet (after a UAC prompt).
 */
object WindowsFirewall {
    enum class Status { Allowed, Blocked, NoRule, Unknown }

    private const val RULE_NAME = "BreakPoint (lokal tarmoq)"
    /** Exit code PowerShell returns here when the user declines the UAC prompt. */
    private const val UAC_DECLINED = 1223

    val isWindows: Boolean = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    /** BreakPoint.exe when installed, java.exe when started from the IDE. */
    val executablePath: String? = runCatching { ProcessHandle.current().info().command().orElse(null) }.getOrNull()

    private fun psQuote(value: String) = "'" + value.replace("'", "''") + "'"

    /** Passing the script base64-encoded avoids Windows command-line quoting problems. */
    private fun powerShell(script: String): ProcessBuilder {
        // Progress records would otherwise be mixed into the output as CLIXML
        val full = "${'$'}ProgressPreference = 'SilentlyContinue'\n$script"
        val encoded = java.util.Base64.getEncoder().encodeToString(full.toByteArray(StandardCharsets.UTF_16LE))
        return ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded)
            .redirectErrorStream(true)
    }

    fun status(): Status {
        val exe = executablePath
        if (!isWindows || exe == null) return Status.Unknown
        val script = """
            ${'$'}rules = Get-NetFirewallApplicationFilter -Program ${psQuote(exe)} -ErrorAction SilentlyContinue |
                Get-NetFirewallRule -ErrorAction SilentlyContinue |
                Where-Object { ${'$'}_.Enabled -eq 'True' -and ${'$'}_.Direction -eq 'Inbound' }
            if (${'$'}rules | Where-Object { ${'$'}_.Action -eq 'Block' }) { 'BLOCKED' }
            elseif (${'$'}rules | Where-Object { ${'$'}_.Action -eq 'Allow' }) { 'ALLOWED' }
            else { 'NONE' }
        """.trimIndent()
        val output = runPowerShell(script, timeoutSeconds = 20) ?: return Status.Unknown
        val answers = output.lines().map { it.trim() }
        return when {
            "BLOCKED" in answers -> Status.Blocked
            "ALLOWED" in answers -> Status.Allowed
            "NONE" in answers -> Status.NoRule
            else -> Status.Unknown
        }
    }

    /** Removes block rules for this program and allows it on the local subnet. Shows a UAC prompt. */
    fun fix(): Result<Unit> {
        val exe = executablePath
        if (!isWindows || exe == null) return Result.failure(Exception("Bu amal faqat Windows'da ishlaydi."))
        val script = File.createTempFile("breakpoint-firewall", ".ps1")
        return try {
            // BOM so Windows PowerShell 5.1 reads non-ASCII paths (e.g. Cyrillic user names) correctly
            script.writeText(
                "\uFEFF" + """
                ${'$'}ErrorActionPreference = 'Stop'
                ${'$'}p = ${psQuote(exe)}
                Get-NetFirewallApplicationFilter -Program ${'$'}p -ErrorAction SilentlyContinue |
                    Get-NetFirewallRule -ErrorAction SilentlyContinue |
                    Where-Object { ${'$'}_.Action -eq 'Block' } |
                    Remove-NetFirewallRule
                Get-NetFirewallRule -DisplayName ${psQuote(RULE_NAME)} -ErrorAction SilentlyContinue | Remove-NetFirewallRule
                New-NetFirewallRule -DisplayName ${psQuote(RULE_NAME)} -Direction Inbound -Program ${'$'}p `
                    -Action Allow -Profile Any -RemoteAddress LocalSubnet | Out-Null
                exit 0
                """.trimIndent(),
                StandardCharsets.UTF_8
            )
            val launcher = """
                try {
                    ${'$'}pr = Start-Process powershell -Verb RunAs -Wait -PassThru -WindowStyle Hidden `
                        -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass','-File',${psQuote("\"${script.path}\"")}
                    exit ${'$'}pr.ExitCode
                } catch { exit $UAC_DECLINED }
            """.trimIndent()
            val process = powerShell(launcher).start()
            process.inputStream.readAllBytes()
            if (!process.waitFor(180, TimeUnit.SECONDS)) {
                process.destroy()
                return Result.failure(Exception("Vaqt tugadi. Qaytadan urinib ko'ring."))
            }
            when (process.exitValue()) {
                0 -> Result.success(Unit)
                UAC_DECLINED -> Result.failure(Exception("Administrator ruxsati berilmadi. Qo'lda sozlash yo'riqnomasidan foydalaning."))
                else -> Result.failure(Exception("Firewall sozlanmadi (kod ${process.exitValue()}). Qo'lda sozlab ko'ring."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Firewall sozlanmadi: ${e.message}"))
        } finally {
            script.delete()
        }
    }

    private fun runPowerShell(script: String, timeoutSeconds: Long): String? = try {
        val process = powerShell(script).start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) output else {
            process.destroy()
            null
        }
    } catch (e: Exception) {
        null
    }
}
