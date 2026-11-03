package com.freelibrary.app.ui.profile.avatar

import com.freelibrary.app.domain.model.AvatarSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarArtTest {
    private val pathCharacters = Regex("[MmLlHhVvCcSsQqTtAaZz0-9 .,\\-]+")

    private fun allShapes(): List<ArtShape> =
        AvatarArt.hair.flatMap { it.back + it.front } +
            AvatarArt.outfits.flatMap { it.back + it.front } +
            AvatarArt.eyes.flatten() +
            AvatarArt.mouths.flatten() +
            AvatarArt.glasses.flatten() +
            AvatarArt.hats.flatMap { it.back + it.front } +
            AvatarArt.earrings.flatten() +
            AvatarArt.noses.flatten() +
            AvatarArt.brows + AvatarArt.blush

    @Test
    fun `every slot has exactly as many drawings or colours as it has choices`() {
        assertEquals(AvatarSlot.HAIR.count, AvatarArt.hair.size)
        assertEquals(AvatarSlot.EYES.count, AvatarArt.eyes.size)
        assertEquals(AvatarSlot.MOUTH.count, AvatarArt.mouths.size)
        assertEquals(AvatarSlot.GLASSES.count, AvatarArt.glasses.size)
        assertEquals(AvatarSlot.HAT.count, AvatarArt.hats.size)
        assertEquals(AvatarSlot.EARRINGS.count, AvatarArt.earrings.size)
        assertEquals(AvatarSlot.NOSE.count, AvatarArt.noses.size)
        assertEquals(AvatarSlot.FACE_SHAPE.count, AvatarArt.heads.size)
        assertEquals(AvatarSlot.EYE_COLOUR.count, AvatarArt.eyeColours.size)
        assertEquals(AvatarSlot.HAT_COLOUR.count, AvatarArt.hatColours.size)
        assertEquals(AvatarSlot.OUTFIT.count, AvatarArt.outfits.size)
        assertEquals(AvatarSlot.SKIN.count, AvatarArt.skinTones.size)
        assertEquals(AvatarSlot.HAIR_COLOUR.count, AvatarArt.hairColours.size)
        assertEquals(AvatarSlot.OUTFIT_COLOUR.count, AvatarArt.outfitColours.size)
        assertEquals(AvatarSlot.BACKGROUND.count, AvatarArt.backgrounds.size)
    }

    @Test
    fun `every path is made of path data and starts with a move`() {
        val body = listOf(AvatarArt.BODY, AvatarArt.NECK, AvatarArt.EAR_LEFT, AvatarArt.EAR_RIGHT) + AvatarArt.heads
        val paths = allShapes().map { it.path } + body
        paths.forEach { path ->
            assertTrue("not path data: $path", pathCharacters.matches(path))
            assertTrue("does not start with a move: $path", path.startsWith("M"))
        }
    }

    @Test
    fun `lines have a width and fills have none`() {
        allShapes().forEach { shape -> assertTrue(shape.strokeWidth >= 0f) }
        assertTrue(allShapes().any { it.strokeWidth > 0f })
        assertTrue(allShapes().any { it.strokeWidth == 0f })
    }

    @Test
    fun `every colour is plain RGB`() {
        (
            AvatarArt.skinTones + AvatarArt.hairColours + AvatarArt.eyeColours + AvatarArt.outfitColours +
                AvatarArt.hatColours + AvatarArt.backgrounds + AvatarArt.INK_RGB
        )
            .forEach { assertTrue(it in 0..0xFFFFFF) }
    }

    @Test
    fun `the figure is enlarged around a point inside the picture`() {
        assertTrue(AvatarArt.FIGURE_SCALE >= 1f)
        assertTrue(AvatarArt.FIGURE_CENTRE_X in 0f..AvatarArt.BOX)
        assertTrue(AvatarArt.FIGURE_CENTRE_Y in 0f..AvatarArt.BOX)
    }
}
