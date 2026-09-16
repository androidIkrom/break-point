package com.example.poststudy

import com.example.poststudy.di.AppContainer
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.nio.file.Files

/** Shared setup: a throwaway database and real Office files. */
object TestFixtures {
    private var initialized = false

    /** Points the app at a temp database once per test JVM; the real ~/.breakpoint stays untouched. */
    fun initDatabase() {
        if (initialized) return
        System.setProperty("user.home", Files.createTempDirectory("breakpoint-test").toString())
        AppContainer.localRepository.init()
        initialized = true
    }

    fun tempDir(): File = Files.createTempDirectory("breakpoint-files").toFile()

    /** Word test with [count] questions, second option correct. */
    fun docx(dir: File, count: Int): String {
        val file = File(dir, "test.docx")
        XWPFDocument().use { doc ->
            repeat(count) { i ->
                doc.createParagraph().createRun().setText("${i + 1}. Savol $i?")
                doc.createParagraph().createRun().setText("a) Noto'g'ri")
                doc.createParagraph().createRun().setText("b) To'g'ri*")
            }
            file.outputStream().use { doc.write(it) }
        }
        return file.path
    }

    fun pptx(dir: File, slides: Int): String {
        val file = File(dir, "slides.pptx")
        XMLSlideShow().use { ppt ->
            repeat(slides) { ppt.createSlide() }
            file.outputStream().use { ppt.write(it) }
        }
        return file.path
    }
}
