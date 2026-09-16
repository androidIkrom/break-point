package com.example.poststudy.data.util

import com.example.poststudy.domain.model.ParseResult
import com.example.poststudy.domain.model.Question
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.io.FileInputStream

object TestParser {

    fun parseTest(filePath: String): ParseResult {
        val file = File(filePath)
        if (filePath.isBlank()) return ParseResult(error = "Test fayli tanlanmagan")
        if (!file.exists()) return ParseResult(error = "Fayl topilmadi: $filePath")

        val fullText = try {
            when {
                filePath.endsWith(".docx", ignoreCase = true) -> FileInputStream(file).use { readDocx(XWPFDocument(it)) }
                // Legacy .doc: WordExtractor keeps paragraph and line breaks
                filePath.endsWith(".doc", ignoreCase = true) -> FileInputStream(file).use { WordExtractor(it).use { ex -> ex.text } }
                else -> return ParseResult(error = "Qo'llab-quvvatlanmaydigan fayl formati. Faqat .doc va .docx ishlaydi.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ParseResult(error = "Faylni o'qishda xatolik yuz berdi: ${e.message}")
        }

        if (fullText.isBlank()) return ParseResult(error = "Fayl bo'sh")

        return parseContent(fullText)
    }

    /**
     * Text of a .docx with Word's automatic list labels ("1.", "a)") written out, because
     * POI leaves them out and teachers often number questions with Word lists. Tables are read
     * cell by cell.
     */
    private fun readDocx(doc: XWPFDocument): String {
        val out = StringBuilder()
        val counters = HashMap<String, IntArray>()

        fun appendParagraph(p: XWPFParagraph) {
            val label = listLabel(p, counters)
            val text = p.text
            if (label != null && text.isNotBlank()) out.append(label).append(' ')
            out.append(text).append('\n')
        }

        for (element in doc.bodyElements) {
            when (element) {
                is XWPFParagraph -> appendParagraph(element)
                is XWPFTable -> for (row in element.rows) {
                    for (cell in row.tableCells) {
                        for (p in cell.paragraphs) appendParagraph(p)
                    }
                }
            }
        }
        return out.toString()
    }

    private fun listLabel(p: XWPFParagraph, counters: HashMap<String, IntArray>): String? {
        val numId = p.numID ?: return null
        val level = (p.numIlvl?.toInt() ?: 0).coerceIn(0, 8)
        val levels = counters.getOrPut(numId.toString()) { IntArray(9) }
        levels[level]++
        for (deeper in level + 1 until levels.size) levels[deeper] = 0
        val n = levels[level]

        val formatted = when (p.numFmt) {
            "lowerLetter" -> letter(n, 'a')
            "upperLetter" -> letter(n, 'A')
            "russianLower" -> cyrillicLetter(n).lowercase()
            "russianUpper" -> cyrillicLetter(n)
            "bullet", "none" -> return null
            else -> n.toString()
        }
        // "%1." / "%1)" from the list definition; fall back to "1." for numbers and "a)" for letters
        val pattern = p.numLevelText
        return if (pattern != null && pattern.contains("%")) {
            pattern.replace(Regex("%\\d"), formatted)
        } else if (formatted.first().isDigit()) "$formatted." else "$formatted)"
    }

    private fun letter(n: Int, base: Char): String = (base + ((n - 1) % 26)).toString()

    private const val CYRILLIC_OPTION_LETTERS = "АБВГДЕЖЗ"

    private fun cyrillicLetter(n: Int): String = CYRILLIC_OPTION_LETTERS[(n - 1) % CYRILLIC_OPTION_LETTERS.length].toString()

    /** Index of an option letter: A-H in Latin or Cyrillic (А Б В Г Д Е Ж З), 1-9 as digits. */
    private fun optionIndex(label: String): Int? {
        val c = label.first()
        return when {
            c in 'a'..'h' -> c - 'a'
            c in 'A'..'H' -> c - 'A'
            c.uppercaseChar() in CYRILLIC_OPTION_LETTERS -> CYRILLIC_OPTION_LETTERS.indexOf(c.uppercaseChar())
            c in '1'..'9' -> c - '1'
            else -> null
        }
    }

    private fun parseContent(content: String): ParseResult {
        val questions = mutableListOf<Question>()
        val warnings = mutableListOf<String>()

        // Normalize lines: tabs and non-breaking spaces become spaces, runs of spaces collapse
        val lines = content.split("\n", "\r")
            .map { it.replace('\t', ' ').replace(' ', ' ').replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }

        var currentQuestionText = ""
        val currentOptions = mutableListOf<String>()
        var currentCorrectIndex = -1
        var isBuildingOption = false
        var lastQuestionNumber = 0
        var anyMarked = false
        val unmarked = mutableListOf<String>()

        fun saveQuestion() {
            if (currentQuestionText.isBlank() && currentOptions.isEmpty()) return

            val trimmedText = currentQuestionText.trim()
            if (trimmedText.isBlank()) {
                if (currentOptions.isNotEmpty()) {
                    warnings.add("${lastQuestionNumber}-savol matni bo'sh bo'lganligi sababli o'tkazib yuborildi.")
                }
            } else if (currentOptions.size < 2) {
                warnings.add("\"${trimmedText.take(30)}...\" savolida variantlar yetarli emas (kamida 2 ta bo'lishi kerak).")
            } else {
                if (currentCorrectIndex == -1) unmarked += trimmedText
                questions.add(Question(trimmedText, currentOptions.toList(), currentCorrectIndex.coerceAtLeast(0)))
            }
        }

        // Questions: 1., 1), 1-savol., S1:, 1 - savol
        val questionRegex = Regex("""^(?:savol\s*|s\s*|vopros\s*|q\s*)?(\d+)(?:\s*-?\s*(?:savol|vopros))?\s*[\.\)\:]\s*(.*)""", RegexOption.IGNORE_CASE)
        // Options: A), a., А) (Cyrillic), 1), 1-variant., V1)
        val optionRegex = Regex("""^(?:variant\s*|javob\s*|v\s*)?([a-hA-HА-За-з]|[1-9])(?:\s*-?\s*(?:variant|javob))?\s*[\)\.\:]\s*(.*)""", RegexOption.IGNORE_CASE)

        for (rawLine in lines) {
            // "*" at the start or end marks the correct option; "+" at the start too
            val isStarred = rawLine.startsWith("*") || rawLine.startsWith("+")
            val isEndStarred = rawLine.endsWith("*")

            var line = rawLine
            if (isStarred) line = line.drop(1).trim()
            if (isEndStarred) line = line.removeSuffix("*").trim()
            if (line.isEmpty()) continue

            var isQuestion = false
            var isOption = false
            var text = ""

            val optMatch = optionRegex.find(line)?.takeIf { optionIndex(it.groupValues[1]) != null }
            val qMatch = questionRegex.find(line)

            if (optMatch != null && qMatch != null) {
                val lowerLine = line.lowercase()
                val number = qMatch.groupValues[1].toInt()
                // "1)" can start a question or an option; decide from what came before
                val expectsOption = currentQuestionText.isNotEmpty() && (
                    currentOptions.size < 2 ||
                        (number != lastQuestionNumber + 1 && number == currentOptions.size + 1)
                    )
                when {
                    (lowerLine.contains("variant") || lowerLine.contains("javob")) && !lowerLine.contains("savol") -> {
                        isOption = true
                        text = optMatch.groupValues[2]
                    }
                    lowerLine.contains("savol") || lowerLine.contains("vopros") -> {
                        isQuestion = true
                        text = qMatch.groupValues[2]
                    }
                    expectsOption -> {
                        isOption = true
                        text = optMatch.groupValues[2]
                    }
                    else -> {
                        isQuestion = true
                        text = qMatch.groupValues[2]
                    }
                }
            } else if (optMatch != null) {
                isOption = true
                text = optMatch.groupValues[2]
            } else if (qMatch != null) {
                isQuestion = true
                text = qMatch.groupValues[2]
            }

            if (isQuestion) {
                saveQuestion()
                lastQuestionNumber = qMatch!!.groupValues[1].toInt()
                currentQuestionText = text.trim()
                currentOptions.clear()
                currentCorrectIndex = -1
                isBuildingOption = false
            } else if (isOption) {
                isBuildingOption = true
                if (isStarred || isEndStarred) {
                    currentCorrectIndex = currentOptions.size
                    anyMarked = true
                }
                currentOptions.add(text.trim())
            } else {
                if (isBuildingOption && currentOptions.isNotEmpty()) {
                    val lastIdx = currentOptions.size - 1
                    if (isStarred || isEndStarred) {
                        currentCorrectIndex = lastIdx
                        anyMarked = true
                    }
                    currentOptions[lastIdx] = (currentOptions[lastIdx] + " " + line).trim()
                } else {
                    currentQuestionText = if (currentQuestionText.isEmpty()) line else "$currentQuestionText $line"
                }
            }
        }

        saveQuestion()

        if (questions.isEmpty() && warnings.isEmpty()) {
            return ParseResult(error = "Faylda testlar topilmadi. Iltimos, formatni tekshiring (masalan: 1. Savol, A) Variant).")
        }

        if (unmarked.isNotEmpty()) {
            if (!anyMarked) {
                // Common test-bank convention: the first option is the right one (options get shuffled later)
                warnings.add(
                    0,
                    "Hujjatda to'g'ri javoblar (*) bilan belgilanmagan, shuning uchun har bir savolda A varianti to'g'ri deb olindi."
                )
            } else {
                unmarked.forEach {
                    warnings.add("\"${it.take(30)}...\" savolida to'g'ri javob belgilanmagan, A varianti to'g'ri deb olindi.")
                }
            }
        }

        return ParseResult(questions, warnings)
    }
}
