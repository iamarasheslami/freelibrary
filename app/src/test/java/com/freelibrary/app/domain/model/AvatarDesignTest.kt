package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.random.Random

class AvatarDesignTest {
    @Test
    fun `the default design is a valid one`() {
        AvatarSlot.entries.forEach { assertEquals(true, AvatarDesign.DEFAULT[it] in 0 until it.count) }
    }

    @Test
    fun `a design is written as text and read back the same`() {
        val design =
            AvatarDesign.DEFAULT
                .with(AvatarSlot.HAIR, 12)
                .with(AvatarSlot.HAT, 6)
                .with(AvatarSlot.EARRINGS, 5)
                .with(AvatarSlot.OUTFIT_COLOUR, 7)
                .with(AvatarSlot.BACKGROUND, 3)

        val decoded = AvatarDesign.decode(design.encode())

        assertEquals(design, decoded)
        assertEquals(design.hashCode(), decoded.hashCode())
    }

    @Test
    fun `the text starts with the format version`() {
        assertEquals(true, AvatarDesign.DEFAULT.encode().startsWith("2:"))
    }

    @Test
    fun `setting one slot leaves the others as they were`() {
        val changed = AvatarDesign.DEFAULT.with(AvatarSlot.EYES, 2)

        assertEquals(2, changed[AvatarSlot.EYES])
        AvatarSlot.entries.filter { it != AvatarSlot.EYES }.forEach { assertEquals(AvatarDesign.DEFAULT[it], changed[it]) }
        assertNotEquals(AvatarDesign.DEFAULT, changed)
    }

    @Test
    fun `a choice that does not exist is refused`() {
        assertThrows(IllegalArgumentException::class.java) { AvatarDesign.DEFAULT.with(AvatarSlot.GLASSES, AvatarSlot.GLASSES.count) }
        assertThrows(IllegalArgumentException::class.java) { AvatarDesign.DEFAULT.with(AvatarSlot.SKIN, -1) }
    }

    @Test
    fun `text that is not a design of this version is not read`() {
        val good = AvatarDesign.DEFAULT.encode()
        assertNull(AvatarDesign.decode(null))
        assertNull(AvatarDesign.decode(""))
        assertNull(AvatarDesign.decode("1:" + good.removePrefix("2:")))
        assertNull(AvatarDesign.decode("2:1.2.3"))
        assertNull(AvatarDesign.decode(good + ".0"))
        assertNull(AvatarDesign.decode("2:" + AvatarSlot.entries.joinToString(".") { "x" }))
        assertNull(AvatarDesign.decode("2:" + AvatarSlot.entries.joinToString(".") { "${it.count}" }))
        assertNull(AvatarDesign.decode("2:" + AvatarSlot.entries.joinToString(".") { "-1" }))
    }

    @Test
    fun `random designs are always valid and differ`() {
        val random = Random(42)
        val designs = (1..200).map { AvatarDesign.random(random) }

        designs.forEach { design -> AvatarSlot.entries.forEach { assertEquals(true, design[it] in 0 until it.count) } }
        assertEquals(true, designs.toSet().size > 100)
        designs.forEach { assertEquals(it, AvatarDesign.decode(it.encode())) }
    }
}
