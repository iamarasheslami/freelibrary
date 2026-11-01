package com.freelibrary.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFontTest {
    @Test
    fun `the list offers the ten approved fonts`() {
        assertEquals(10, AppFont.entries.size)
    }

    @Test
    fun `ids and labels are unique, so a stored id always means one font`() {
        assertEquals(AppFont.entries.size, AppFont.entries.map { it.id }.toSet().size)
        assertEquals(AppFont.entries.size, AppFont.entries.map { it.label }.toSet().size)
    }

    @Test
    fun `every font has an upright and an italic file with its own name`() {
        val names = mutableSetOf<String>()
        for (font in AppFont.entries) {
            val faces = font.forReader().faces
            assertTrue(font.id, faces.any { !it.italic } && faces.any { it.italic })
            for (face in faces) assertTrue("${face.file} is used twice", names.add(face.file))
        }
    }

    @Test
    fun `a stored id finds its font and anything else finds none`() {
        assertEquals(AppFont.LORA, AppFont.fromId("lora"))
        assertNull(AppFont.fromId("comic_sans"))
        assertNull(AppFont.fromId(null))
    }
}
