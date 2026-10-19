package com.example.poststudy.presentation.theme

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference

/**
 * Reads the Windows accessibility setting "Animation effects" (SPI_GETCLIENTAREAANIMATION).
 * When it is off, the app plays no animations: screens and backgrounds switch instantly.
 * Other operating systems report no preference.
 */
object MotionPrefs {
    private const val SPI_GETCLIENTAREAANIMATION = 0x1042

    private interface User32Lib : Library {
        @Suppress("FunctionName")
        fun SystemParametersInfoW(uiAction: Int, uiParam: Int, pvParam: IntByReference, fWinIni: Int): Boolean
    }

    val reduceMotion: Boolean by lazy {
        val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("win")
        if (!isWindows) return@lazy false
        runCatching {
            val lib = Native.load("user32", User32Lib::class.java)
            val enabled = IntByReference(1)
            val ok = lib.SystemParametersInfoW(SPI_GETCLIENTAREAANIMATION, 0, enabled, 0)
            ok && enabled.value == 0
        }.getOrDefault(false)
    }
}
