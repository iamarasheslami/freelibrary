package com.freelibrary.app.ui.explore

import android.content.Context
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.BookSlider
import com.freelibrary.app.domain.model.ExploreRow
import com.freelibrary.app.domain.model.ExploreSection
import com.freelibrary.app.domain.model.ExploreSectionKind
import com.freelibrary.app.ui.booklist.BookSliderViewHolder
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
class ExploreAdapterTest {
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

    private fun section(
        id: String,
        title: String = "Shelf",
        titleRes: Int? = null,
    ) = ExploreSection(id, "name-$id", ExploreSectionKind.SHELF, title, titleRes, 12)

    private fun row(
        section: ExploreSection,
        hasMore: Boolean,
        vararg titles: String,
    ) = ExploreRow(
        section,
        BookSlider(
            titles.mapIndexed {
                    i,
                    t,
                ->
                BookCard(i.toLong(), "$i", t, null, null)
            },
            if (hasMore) titles.size + 1 else titles.size,
        ),
    )

    @Test
    fun `a curated collection is headed by its string resource`() {
        val heading = section("c", titleRes = R.string.home_recently_added).heading(context::getString)

        assertEquals(context.getString(R.string.home_recently_added), heading)
    }

    @Test
    fun `a catalog shelf is headed by its catalog name`() {
        assertEquals("Category: Fiction", section("g", title = "Category: Fiction").heading(context::getString))
    }

    @Test
    fun `rows are the same item while the section id is the same`() {
        val a = row(section("x"), false, "One")
        val b = row(section("x"), false, "One", "Two")

        assertTrue(ExploreAdapter.DIFF.areItemsTheSame(a, b))
        assertFalse(ExploreAdapter.DIFF.areContentsTheSame(a, b))
        assertFalse(ExploreAdapter.DIFF.areItemsTheSame(a, row(section("y"), false, "One")))
    }

    @Test
    fun `view more shows only when the shelf holds more books`() {
        val adapter = ExploreAdapter(CoverTypefaces(Typeface.DEFAULT_BOLD, Typeface.DEFAULT), imageLoader, {}, {})
        val holder: BookSliderViewHolder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        val more = holder.itemView.findViewById<android.view.View>(R.id.sliderViewMore)

        holder.bind("Shelf", row(section("a"), true, "One").slider, {})
        assertTrue(more.isVisible)

        holder.bind("Shelf", row(section("a"), false, "One").slider, {})
        assertFalse(more.isVisible)
        assertTrue(holder.itemView.findViewById<RecyclerView>(R.id.sliderBooks) != null)
    }
}
