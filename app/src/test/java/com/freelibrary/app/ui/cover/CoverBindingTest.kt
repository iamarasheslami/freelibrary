package com.freelibrary.app.ui.cover

import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.domain.model.BookCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CoverBindingTest {
    private fun book(
        externalId: String = "1342",
        title: String = "Pride and Prejudice",
        author: String? = "Austen, Jane",
    ) = BookCard(
        bookId = 1,
        externalId = externalId,
        title = title,
        authorName = author,
        coverUrl = null,
    )

    @Test
    fun `a book's cover style comes from its Gutenberg id`() {
        assertEquals(CoverStyle.forKey("1342"), coverStyleFor(book(externalId = "1342")))
    }

    @Test
    fun `the style does not change with the title or author`() {
        val first = coverStyleFor(book(title = "Pride and Prejudice", author = "Austen, Jane"))
        val second = coverStyleFor(book(title = "Another title", author = null))

        assertEquals(first, second)
    }

    @Test
    fun `the typefaces always load, falling back to the system fonts`() {
        val typefaces = CoverTypefaces.from(ApplicationProvider.getApplicationContext())

        assertNotNull(typefaces.title)
        assertSame(Typeface.DEFAULT, typefaces.detail)
    }

    @Test
    fun `a generated cover can be made for a book with no author`() {
        val typefaces = CoverTypefaces(Typeface.DEFAULT_BOLD, Typeface.DEFAULT)

        assertNotNull(generatedCoverFor(book(author = null), typefaces))
    }
}
