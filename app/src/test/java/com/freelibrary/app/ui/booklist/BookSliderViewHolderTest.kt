package com.freelibrary.app.ui.booklist

import android.content.Context
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.BookSlider
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.cover.createCoverImageLoader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookSliderViewHolderTest {
    private lateinit var imageLoader: ImageLoader
    private lateinit var context: Context

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

    private fun holder() =
        BookSliderViewHolder.create(
            FrameLayout(context),
            CoverTypefaces(Typeface.DEFAULT_BOLD, Typeface.DEFAULT),
            imageLoader,
            RecyclerView.RecycledViewPool(),
        ) {}

    private fun slider(
        shown: Int,
        total: Int,
    ) = BookSlider(
        books = List(shown) { BookCard(it.toLong(), "$it", "Title $it", null, null) },
        totalBooks = total,
    )

    private fun BookSliderViewHolder.view(id: Int) = itemView.findViewById<View>(id)

    @Test
    fun `the heading shows the shelf name`() {
        val holder = holder()

        holder.bind("Harvard Classics", slider(shown = 10, total = 51)) {}

        assertEquals("Harvard Classics", holder.view(R.id.sliderHeading).let { (it as TextView).text.toString() })
    }

    @Test
    fun `view more is offered when the shelf holds more books than shown`() {
        val holder = holder()

        holder.bind("Poetry", slider(shown = 10, total = 40)) {}

        assertEquals(View.VISIBLE, holder.view(R.id.sliderViewMore).visibility)
    }

    @Test
    fun `view more is hidden when every book of the shelf is shown`() {
        val holder = holder()

        holder.bind("Poetry", slider(shown = 7, total = 7)) {}

        assertEquals(View.GONE, holder.view(R.id.sliderViewMore).visibility)
    }

    @Test
    fun `a screen reader hears which shelf view more belongs to`() {
        val holder = holder()

        holder.bind("Harvard Classics", slider(shown = 10, total = 51)) {}

        assertEquals("View more in Harvard Classics", holder.view(R.id.sliderViewMore).contentDescription.toString())
    }

    @Test
    fun `tapping view more reports it`() {
        val holder = holder()
        var taps = 0

        holder.bind("Poetry", slider(shown = 10, total = 40)) { taps++ }
        holder.view(R.id.sliderViewMore).performClick()

        assertEquals(1, taps)
    }

    @Test
    fun `the row holds the books of the slider`() {
        val holder = holder()

        holder.bind("Poetry", slider(shown = 10, total = 40)) {}

        assertEquals(10, (holder.view(R.id.sliderBooks) as RecyclerView).adapter?.itemCount)
    }
}
