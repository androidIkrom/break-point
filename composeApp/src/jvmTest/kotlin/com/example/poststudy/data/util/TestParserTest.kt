package com.example.poststudy.data.util

import com.example.poststudy.TestFixtures
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat
import java.io.File
import java.math.BigInteger
import kotlin.test.*

class TestParserTest {

    /** One paragraph per string; "\n" inside a string becomes a soft line break (Shift+Enter). */
    private fun docx(vararg paragraphs: String): String {
        val file = File(TestFixtures.tempDir(), "t.docx")
        XWPFDocument().use { doc ->
            for (text in paragraphs) {
                val run = doc.createParagraph().createRun()
                text.split("\n").forEachIndexed { i, part ->
                    if (i > 0) run.addBreak()
                    run.setText(part, i)
                }
            }
            file.outputStream().use { doc.write(it) }
        }
        return file.path
    }

    @Test
    fun unmarkedDocumentUsesFirstOptionWithOneNote() {
        // Same shape as the institute's test bank: soft line breaks, no markers, gaps in numbering
        val path = docx(
            "1. Tarmoq texnologiyalari nima?\nA) To'g'ri javob \nB) Ikkinchi\nC) Uchinchi\nD) To'rtinchi\n2. OSI modelida nechta qavat bor?\nA) 7\nB) 6\nC) 5\nD) 8",
            "4.\u00A0\u00A0 Spanning-Tree qaysi qurilmada ishlaydi?\nA) Switch\nB) Router\nC) Hub\nD) Access Point"
        )
        val r = TestParser.parseTest(path)
        assertNull(r.error)
        assertEquals(3, r.questions.size)
        assertTrue(r.questions.all { it.correctIndex == 0 && it.options.size == 4 })
        assertEquals("Spanning-Tree qaysi qurilmada ishlaydi?", r.questions[2].text)
        assertEquals(1, r.warnings.size, r.warnings.toString())
    }

    @Test
    fun realInstituteFileIfPresent() {
        val sample = File("""C:\Users\user\Downloads\Telegram Desktop\Tarmoq test.docx""")
        if (!sample.exists()) return
        val r = TestParser.parseTest(sample.path)
        assertNull(r.error)
        assertEquals(149, r.questions.size)
        assertTrue(r.questions.all { it.options.size == 4 })
        assertEquals(1, r.warnings.size)
    }

    @Test
    fun markersSelectTheCorrectOption() {
        val path = docx(
            "1. Birinchi savol", "a) yo'q", "b) ha*", "c) yo'q",
            "2. Ikkinchi savol", "*A) ha", "B) yo'q",
            "3. Uchinchi savol", "A) yo'q", "+B) ha",
            "4. To'rtinchi savol", "A) belgisiz", "B) variant"
        )
        val r = TestParser.parseTest(path)
        assertEquals(listOf(1, 0, 1, 0), r.questions.map { it.correctIndex })
        assertEquals("ha", r.questions[0].options[1])
        // Only the unmarked question is reported when others are marked
        assertEquals(1, r.warnings.size)
        assertTrue(r.warnings.single().contains("To'rtinchi"))
    }

    @Test
    fun cyrillicOptionLetters() {
        val path = docx("1. Poytaxt qaysi?", "А) Toshkent*", "Б) Samarqand", "В) Buxoro", "Г) Xiva")
        val q = TestParser.parseTest(path).questions.single()
        assertEquals(listOf("Toshkent", "Samarqand", "Buxoro", "Xiva"), q.options)
        assertEquals(0, q.correctIndex)
    }

    @Test
    fun numberedOptionsAreNotQuestions() {
        val path = docx(
            "1. Birinchi savol", "1) bir", "2) ikki*", "3) uch",
            "2. Ikkinchi savol", "1) a*", "2) b"
        )
        val r = TestParser.parseTest(path)
        assertEquals(2, r.questions.size, r.questions.toString())
        assertEquals(listOf("bir", "ikki", "uch"), r.questions[0].options)
        assertEquals(1, r.questions[0].correctIndex)
        assertEquals(listOf("a", "b"), r.questions[1].options)
    }

    @Test
    fun multiLineQuestionAndOption() {
        val path = docx("1. Uzun savol", "davomi bor", "A) uzun variant", "davomi*", "B) qisqa")
        val q = TestParser.parseTest(path).questions.single()
        assertEquals("Uzun savol davomi bor", q.text)
        assertEquals("uzun variant davomi", q.options[0])
        assertEquals(0, q.correctIndex)
    }

    @Test
    fun wordAutomaticNumberingIsRead() {
        val file = File(TestFixtures.tempDir(), "auto.docx")
        XWPFDocument().use { doc ->
            val abstract = CTAbstractNum.Factory.newInstance().apply {
                abstractNumId = BigInteger.ZERO
                addNewLvl().apply {
                    ilvl = BigInteger.ZERO
                    addNewStart().`val` = BigInteger.ONE
                    addNewNumFmt().`val` = STNumberFormat.DECIMAL
                    addNewLvlText().`val` = "%1."
                }
                addNewLvl().apply {
                    ilvl = BigInteger.ONE
                    addNewStart().`val` = BigInteger.ONE
                    addNewNumFmt().`val` = STNumberFormat.UPPER_LETTER
                    addNewLvlText().`val` = "%2)"
                }
            }
            val numbering = doc.createNumbering()
            val numId = numbering.addNum(numbering.addAbstractNum(XWPFAbstractNum(abstract)))
            fun item(text: String, level: Int) = doc.createParagraph().apply {
                setNumID(numId)
                setNumILvl(BigInteger.valueOf(level.toLong()))
                createRun().setText(text)
            }
            item("Birinchi savol", 0)
            item("Noto'g'ri", 1)
            item("To'g'ri*", 1)
            item("Ikkinchi savol", 0)
            item("To'g'ri*", 1)
            item("Noto'g'ri", 1)
            file.outputStream().use { doc.write(it) }
        }
        val r = TestParser.parseTest(file.path)
        assertEquals(listOf("Birinchi savol", "Ikkinchi savol"), r.questions.map { it.text }, r.toString())
        assertEquals(listOf(1, 0), r.questions.map { it.correctIndex })
        assertTrue(r.warnings.isEmpty(), r.warnings.toString())
    }

    @Test
    fun questionsInsideTables() {
        val file = File(TestFixtures.tempDir(), "table.docx")
        XWPFDocument().use { doc ->
            val table = doc.createTable(2, 1)
            table.getRow(0).getCell(0).setText("1. Jadvaldagi savol")
            table.getRow(1).getCell(0).setText("A) javob*")
            doc.createParagraph().createRun().setText("B) boshqa")
            file.outputStream().use { doc.write(it) }
        }
        val q = TestParser.parseTest(file.path).questions.single()
        assertEquals("Jadvaldagi savol", q.text)
        assertEquals(listOf("javob", "boshqa"), q.options)
    }

    @Test
    fun badFilesGiveMessages() {
        assertNotNull(TestParser.parseTest("").error)
        assertNotNull(TestParser.parseTest("C:/yoq/fayl.docx").error)
        val txt = File(TestFixtures.tempDir(), "a.txt").apply { writeText("1. x") }
        assertNotNull(TestParser.parseTest(txt.path).error)
        val broken = File(TestFixtures.tempDir(), "broken.docx").apply { writeText("not a zip") }
        assertNotNull(TestParser.parseTest(broken.path).error)
    }
}
