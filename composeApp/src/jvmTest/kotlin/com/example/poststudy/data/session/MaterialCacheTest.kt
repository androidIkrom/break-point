package com.example.poststudy.data.session

import com.example.poststudy.TestFixtures
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MaterialCacheTest {

    @Test
    fun deckIsRenderedOnceUntilTheFileChanges() {
        val dir = TestFixtures.tempDir()
        val path = TestFixtures.pptx(dir, 2)

        val first = MaterialCache.slideJpegs(path)
        val second = MaterialCache.slideJpegs(path)
        assertEquals(2, first.size)
        assertSame(first, second)
        assertEquals(2, MaterialCache.encodedSlides(path).size)
        assertEquals(2, MaterialCache.slides(path).size)

        // A re-saved deck must not be served from the old copy
        val file = File(path)
        file.writeBytes(File(TestFixtures.pptx(TestFixtures.tempDir(), 3)).readBytes())
        file.setLastModified(file.lastModified() + 5_000)
        val third = MaterialCache.slideJpegs(path)
        assertNotSame(first, third)
        assertEquals(3, third.size)
    }

    @Test
    fun missingDeckGivesNoSlides() {
        assertTrue(MaterialCache.slideJpegs("").isEmpty())
        assertTrue(MaterialCache.slideJpegs(File(TestFixtures.tempDir(), "yoq.pptx").path).isEmpty())
    }

    @Test
    fun parsedTestIsReused() {
        val dir = TestFixtures.tempDir()
        val path = TestFixtures.docx(dir, 3)
        val first = MaterialCache.questions(path)
        assertEquals(3, first.questions.size)
        assertSame(first, MaterialCache.questions(path))
    }
}
