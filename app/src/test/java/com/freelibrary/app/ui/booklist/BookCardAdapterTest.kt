package com.freelibrary.app.ui.booklist

import android.content.Context
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.cover.createCoverImageLoader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookCardAdapterTest {
    private lateinit var imageLoader: ImageLoader
    private lateinit var context: Context
    private val clicked = mutableListOf<BookCard>()

    @Before
    fun setUp() {
        val app: Context = ApplicationProvider.getApplicationContext()
        context = ContextThemeWrapper(app, R.style.Theme_FreeLibrary)
        imageLoader = createCoverImageLoader(app)
    }

    @After
    fun tearDown() {
        imageLoader.shutdown()
    }

    private fun book(
        bookId: Long = 1,
        title: String = "Pride and Prejudice",
        author: String? = "Austen, Jane",
    ) = BookCard(bookId = bookId, externalId = "1342", title = title, authorName = author, coverUrl = null)

    private fun holder() =
        BookCardViewHolder.create(
            FrameLayout(context),
            CoverTypefaces(Typeface.DEFAULT_BOLD, Typeface.DEFAULT),
            imageLoader,
        ) { clicked += it }

    private fun BookCardViewHolder.text(id: Int) = itemView.findViewById<TextView>(id)

    @Test
    fun `the card shows the main title only`() {
        val holder = holder()

        holder.bind(book(title = "Moby Dick; Or, The Whale"))

        assertEquals("Moby Dick", holder.text(R.id.bookTitle).text.toString())
    }

    @Test
    fun `the card shows the author`() {
        val holder = holder()

        holder.bind(book(author = "Austen, Jane"))

        assertEquals("Austen, Jane", holder.text(R.id.bookAuthor).text.toString())
        assertEquals(View.VISIBLE, holder.text(R.id.bookAuthor).visibility)
    }

    @Test
    fun `a book without an author hides the line but keeps its space`() {
        val holder = holder()

        holder.bind(book(author = null))

        assertEquals(View.INVISIBLE, holder.text(R.id.bookAuthor).visibility)
    }

    @Test
    fun `tapping the card reports the book it shows now, not an earlier one`() {
        val holder = holder()
        val first = book(bookId = 1, title = "First")
        val second = book(bookId = 2, title = "Second")

        holder.bind(first)
        holder.bind(second)
        holder.itemView.performClick()

        assertEquals(listOf(second), clicked)
    }

    @Test
    fun `the same book is the same item even when its details changed`() {
        val old = book(bookId = 7, title = "Old")
        val new = book(bookId = 7, title = "New")

        assertTrue(BookCardAdapter.DIFF.areItemsTheSame(old, new))
        assertFalse(BookCardAdapter.DIFF.areContentsTheSame(old, new))
    }

    @Test
    fun `identical books are the same item with the same contents`() {
        assertTrue(BookCardAdapter.DIFF.areItemsTheSame(book(), book()))
        assertTrue(BookCardAdapter.DIFF.areContentsTheSame(book(), book()))
    }

    @Test
    fun `different books are different items`() {
        assertFalse(BookCardAdapter.DIFF.areItemsTheSame(book(bookId = 1), book(bookId = 2)))
    }
}
