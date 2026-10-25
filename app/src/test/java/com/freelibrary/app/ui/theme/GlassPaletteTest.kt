package com.freelibrary.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The glass colors must exist in both themes under the same names, and be real ARGB values. */
class GlassPaletteTest {
    private val light = read("src/main/res/values/colors_glass.xml")
    private val dark = read("src/main/res/values-night/colors_glass.xml")

    @Test
    fun `light and dark define the same glass colors`() {
        assertEquals(light.keys, dark.keys)
        assertTrue(light.isNotEmpty())
    }

    @Test
    fun `every glass color is translucent`() {
        val opaque = (light + dark.mapKeys { "night " + it.key }).filter { (_, value) -> (value ushr 24) == 0xFFL }
        assertTrue("opaque glass colors: ${opaque.keys}", opaque.isEmpty())
    }

    private fun read(path: String): Map<String, Long> {
        val file = File(path)
        require(file.exists()) { "$path not found from ${File(".").absolutePath}" }
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("color")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val value = node.textContent.trim()
            require(Regex("#[0-9A-Fa-f]{8}").matches(value)) { "Glass colors need #AARRGGBB, got $value" }
            node.attributes.getNamedItem("name").nodeValue to value.substring(1).toLong(16)
        }
    }
}
