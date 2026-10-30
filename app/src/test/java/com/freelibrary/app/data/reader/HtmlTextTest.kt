package com.freelibrary.app.data.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HtmlTextTest {
    @Test
    fun `a word split by inline markup stays one word`() {
        assertEquals("whale", HtmlText.toText("<p>wh<b>al</b>e</p>").trim())
    }

    @Test
    fun `block tags separate words`() {
        assertEquals("one two", HtmlText.toText("<p>one</p><p>two</p>").replace("  ", " ").trim())
    }

    @Test
    fun `script and style are left out`() {
        val html =
            "<html><head><title>T</title><style>p { color: red }</style></head>" +
                "<body><p>Hello</p><script>var x = 1;</script></body></html>"

        val text = HtmlText.toText(html)

        assertEquals("Hello", text.trim())
    }

    @Test
    fun `character references become characters`() {
        assertEquals("Tom & Jerry — \"ok\" é é", HtmlText.toText("Tom &amp; Jerry &mdash; &quot;ok&quot; &#233; &#xE9;"))
    }

    @Test
    fun `an unknown or broken reference is kept as it is`() {
        val text = HtmlText.toText("AT&T and &nonsense; and 5 &lt 6")

        assertEquals("AT&T and &nonsense; and 5 &lt 6", text)
        assertFalse(text.isEmpty())
    }

    @Test
    fun `text after an unclosed tag is dropped without failing`() {
        assertEquals("before ", HtmlText.toText("before <p class=\"x"))
    }
}
