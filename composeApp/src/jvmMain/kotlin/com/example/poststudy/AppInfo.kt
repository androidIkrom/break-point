package com.example.poststudy

object AppInfo {
    /** Set by the installed app's launcher from `packageVersion`; "dev" when run from Gradle/IDE. */
    val version: String = System.getProperty("jpackage.app-version") ?: "dev"

    /**
     * Version of the admin/student network protocol. Raise it whenever requests or replies change
     * incompatibly, so students with an older/newer app get a clear message instead of odd errors.
     */
    const val PROTOCOL_VERSION = 2
}
