package com.freelibrary.app.ui.search

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.R
import com.freelibrary.app.data.repository.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchResultAdapterTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_FreeLibrary)
    }

    private fun result(
        id: Long,
        title: String,
        vararg authors: String,
    ) = SearchResult(id, "$id", title, authors.toList())

    private fun holder(onClick: (SearchResult) -> Unit = {}) = SearchResultViewHolder.create(FrameLayout(context), onClick)

    @Test
    fun `authors are joined in credit order with semicolons`() {
        assertEquals("Austen, Jane; Brontë, Anne", result(1, "T", "Austen, Jane", "Brontë, Anne").authorLine())
    }

    @Test
    fun `a row shows the title and the authors`() {
        val holder = holder()

        holder.bind(result(1, "Hamlet", "Shakespeare, William"))

        assertEquals("Hamlet", holder.itemView.findViewById<TextView>(R.id.searchResultTitle).text.toString())
        assertEquals("Shakespeare, William", holder.itemView.findViewById<TextView>(R.id.searchResultAuthors).text.toString())
    }

    @Test
    fun `the authors line is hidden when the book has no author`() {
        val holder = holder()

        holder.bind(result(1, "Anonymous Tales"))

        assertFalse(holder.itemView.findViewById<View>(R.id.searchResultAuthors).isVisible)
    }

    @Test
    fun `a tap on a row passes its result to the callback`() {
        var tapped: SearchResult? = null
        val holder = holder { tapped = it }
        val hamlet = result(7, "Hamlet", "Shakespeare, William")

        holder.bind(hamlet)
        holder.itemView.performClick()

        assertEquals(hamlet, tapped)
    }

    @Test
    fun `rows are the same item while the book is the same`() {
        val a = result(1, "Hamlet")
        val b = result(1, "Hamlet", "Shakespeare, William")

        assertTrue(SearchResultAdapter.DIFF.areItemsTheSame(a, b))
        assertFalse(SearchResultAdapter.DIFF.areContentsTheSame(a, b))
        assertFalse(SearchResultAdapter.DIFF.areItemsTheSame(a, result(2, "Hamlet")))
    }
}
