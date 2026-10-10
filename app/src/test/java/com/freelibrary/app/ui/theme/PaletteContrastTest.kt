package com.freelibrary.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow

/**
 * Guards the readability of the palette. Every text color keeps at least the WCAG AA contrast
 * (4.5 to 1) against the colors it is drawn on, and the outline stays visible (3 to 1). The
 * values are read straight from the color resource files, so changing a color without checking
 * its contrast fails this test.
 */
class PaletteContrastTest {
    private val light = readColors("src/main/res/values/colors.xml")
    private val dark = readColors("src/main/res/values-night/colors.xml")

    private val containers =
        listOf(
            "md_surface_container_lowest",
            "md_surface_container_low",
            "md_surface_container",
            "md_surface_container_high",
            "md_surface_container_highest",
        )

    private val textPairs =
        listOf(
            "md_on_primary" to "md_primary",
            "md_on_primary_container" to "md_primary_container",
            "md_on_secondary" to "md_secondary",
            "md_on_secondary_container" to "md_secondary_container",
            "md_on_background" to "md_background",
            "md_on_surface" to "md_surface",
            "md_on_surface_variant" to "md_surface_variant",
            "md_on_error" to "md_error",
            "md_on_error_container" to "md_error_container",
            "md_on_surface_inverse" to "md_surface_inverse",
            "md_primary_inverse" to "md_surface_inverse",
            "md_primary" to "md_surface",
            "md_secondary" to "md_surface",
            "md_error" to "md_surface",
        ) + containers.flatMap { listOf("md_on_surface" to it, "md_on_surface_variant" to it) }

    private val boundaryPairs =
        listOf(
            "md_outline" to "md_surface",
            "md_outline" to "md_surface_container_highest",
        )

    @Test
    fun `light and dark define the same color names`() {
        assertEquals(light.keys, dark.keys)
    }

    @Test
    fun `light text colors keep the AA contrast`() = assertContrast("light", light, textPairs, TEXT_MINIMUM)

    @Test
    fun `dark text colors keep the AA contrast`() = assertContrast("dark", dark, textPairs, TEXT_MINIMUM)

    @Test
    fun `the light outline stays visible`() = assertContrast("light", light, boundaryPairs, BOUNDARY_MINIMUM)

    @Test
    fun `the dark outline stays visible`() = assertContrast("dark", dark, boundaryPairs, BOUNDARY_MINIMUM)

    @Test
    fun `black on white is the maximum contrast of 21 to 1`() {
        assertEquals(21.0, contrast(0x000000, 0xFFFFFF), 0.01)
    }

    private fun assertContrast(
        mode: String,
        colors: Map<String, Int>,
        pairs: List<Pair<String, String>>,
        minimum: Double,
    ) {
        val failures =
            pairs.mapNotNull { (foreground, background) ->
                val ratio = contrast(colors.getValue(foreground), colors.getValue(background))
                if (ratio < minimum) "$mode: $foreground on $background is ${"%.2f".format(ratio)}, needs $minimum" else null
            }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun contrast(
        first: Int,
        second: Int,
    ): Double {
        val (lighter, darker) = listOf(luminance(first), luminance(second)).sortedDescending()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val value = ((rgb shr shift) and 0xFF) / 255.0
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun readColors(path: String): Map<String, Int> {
        val file = File(path)
        require(file.exists()) { "$path not found from ${File(".").absolutePath}" }
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("color")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val value = node.textContent.trim()
            require(Regex("#[0-9A-Fa-f]{6}").matches(value)) { "Unsupported color value $value" }
            node.attributes.getNamedItem("name").nodeValue to value.substring(1).toInt(16)
        }
    }

    private companion object {
        const val TEXT_MINIMUM = 4.5
        const val BOUNDARY_MINIMUM = 3.0
    }
}
