package com.example.poststudy.data.session

import com.example.poststudy.data.util.PptConverter
import com.example.poststudy.data.util.TestParser
import com.example.poststudy.domain.model.ParseResult
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

/**
 * Rendered presentations and parsed tests, keyed by file path, size and modification time, so
 * thirty students joining at once render the deck once instead of thirty times. Slides are kept
 * as JPEG bytes (about 100 KB each) and decoded on demand; raw 1280x720 images would take
 * 3.6 MB apiece. Rendering is serialized: a second request for the same deck waits for the first.
 */
object MaterialCache {
    private const val MAX_DECKS = 3
    private const val MAX_TESTS = 12

    private data class Key(val path: String, val size: Long, val modified: Long)

    private fun keyOf(path: String): Key? {
        val file = File(path)
        if (path.isBlank() || !file.isFile) return null
        return Key(file.canonicalPath, file.length(), file.lastModified())
    }

    private fun <K, V> lru(max: Int) = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > max
    }

    private val decks = lru<Key, List<ByteArray>>(MAX_DECKS)
    private val tests = lru<Key, ParseResult>(MAX_TESTS)

    /** Slides of [path] as JPEG bytes; empty when the file is missing or cannot be rendered. */
    @Synchronized
    fun slideJpegs(path: String): List<ByteArray> {
        val key = keyOf(path) ?: return emptyList()
        decks[key]?.let { return it }
        val rendered = PptConverter.convertSlidesToImages(path).map { image ->
            ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
        }
        if (rendered.isNotEmpty()) decks[key] = rendered
        return rendered
    }

    /** Slides of [path] as images, for showing on this computer. */
    fun slides(path: String): List<BufferedImage> = slideJpegs(path).mapNotNull(::decode)

    /** Slides of [path] base64-encoded, for sending to students. */
    fun encodedSlides(path: String): List<String> = slideJpegs(path).map { Base64.getEncoder().encodeToString(it) }

    @Synchronized
    fun questions(path: String): ParseResult {
        val key = keyOf(path) ?: return TestParser.parseTest(path)
        tests[key]?.let { return it }
        val parsed = TestParser.parseTest(path)
        if (parsed.error == null) tests[key] = parsed
        return parsed
    }

    @Synchronized
    fun clear() {
        decks.clear()
        tests.clear()
    }

    fun decode(jpeg: ByteArray): BufferedImage? = try {
        ImageIO.read(ByteArrayInputStream(jpeg))
    } catch (e: Exception) {
        null
    }
}
