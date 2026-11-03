package com.freelibrary.app.ui.profile.avatar

import com.freelibrary.app.domain.model.AvatarDesign
import com.freelibrary.app.domain.model.AvatarSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarRowsTest {
    private val design = AvatarDesign.DEFAULT.with(AvatarSlot.HAIR, 3).with(AvatarSlot.HAIR_COLOUR, 5)

    @Test
    fun `each group has a heading and then one row for every choice`() {
        val rows = avatarRows(AvatarTab.HAIR, design)

        assertEquals(2 + AvatarSlot.HAIR.count + AvatarSlot.HAIR_COLOUR.count, rows.size)
        assertTrue(rows[0] is AvatarRow.Heading)
        assertTrue(rows[1 + AvatarSlot.HAIR.count] is AvatarRow.Heading)
    }

    @Test
    fun `style choices show an avatar that already wears the choice`() {
        val styles = avatarRows(AvatarTab.HAIR, design).filterIsInstance<AvatarRow.Style>()

        assertEquals(AvatarSlot.HAIR.count, styles.size)
        styles.forEach { row ->
            assertEquals(row.choice, row.preview[AvatarSlot.HAIR])
            assertEquals(design[AvatarSlot.HAIR_COLOUR], row.preview[AvatarSlot.HAIR_COLOUR])
        }
    }

    @Test
    fun `colour choices carry their colour`() {
        val colours = avatarRows(AvatarTab.HAIR, design).filterIsInstance<AvatarRow.Colour>()

        assertEquals(AvatarArt.hairColours.toList(), colours.map { it.rgb })
    }

    @Test
    fun `only the current choice of each group is selected`() {
        val rows = avatarRows(AvatarTab.HAIR, design)

        assertEquals(listOf(3), rows.filterIsInstance<AvatarRow.Style>().filter { it.selected }.map { it.choice })
        assertEquals(listOf(5), rows.filterIsInstance<AvatarRow.Colour>().filter { it.selected }.map { it.choice })
    }

    @Test
    fun `every tab has rows and the tabs together cover every slot once`() {
        AvatarTab.entries.forEach { assertTrue(avatarRows(it, design).isNotEmpty()) }

        val slots = AvatarTab.entries.flatMap { tab -> tab.sections.map { it.slot } }
        assertEquals(AvatarSlot.entries.toSet(), slots.toSet())
        assertEquals(slots.size, slots.toSet().size)
    }
}
