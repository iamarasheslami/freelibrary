package com.freelibrary.app.ui.profile.avatar

import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteTest {
    @Test
    fun `shading multiplies every channel and cuts it to the colour range`() {
        assertEquals(0x404040, Palette.shade(0x808080, 0.5f))
        assertEquals(0xFFFFFF, Palette.shade(0xFFFFFF, 1.5f))
        assertEquals(0x000000, Palette.shade(0x000000, 0.8f))
    }

    @Test
    fun `mixing takes the given share of the second colour`() {
        assertEquals(0x7F7F7F, Palette.mix(0x000000, 0xFFFFFF, 0.5f))
        assertEquals(0x0D4C3C, Palette.mix(0x0D4C3C, 0xFFFFFF, 0f))
        assertEquals(0xFFFFFF, Palette.mix(0x0D4C3C, 0xFFFFFF, 1f))
    }
}
