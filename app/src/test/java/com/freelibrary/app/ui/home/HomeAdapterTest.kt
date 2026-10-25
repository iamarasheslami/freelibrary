package com.freelibrary.app.ui.home

import android.content.Context
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.HomeSection
import com.freelibrary.app.domain.model.ViewMoreTarget
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.cover.createCoverImageLoader
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeAdapterTest {
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
        vararg titles: String,
    ) = HomeSection(
        id = id,
        titleRes = R.string.home_recently_added,
        books = titles.mapIndexed { i, t -> BookCard(i.toLong(), "$i", t, null, null) },
        hasMore = false,
        viewMore = ViewMoreTarget.RecentlyAdded,
    )

    private fun adapter() = HomeAdapter(CoverTypefaces(Typeface.DEFAULT_BOLD, Typeface.DEFAULT), imageLoader, {}, {})

    @Test
    fun `the same section is the same row even when its books change`() {
        val old = section("a", "One")
        val new = section("a", "One", "Two")

        assertTrue(HomeAdapter.DIFF.areItemsTheSame(old, new))
        assertFalse(HomeAdapter.DIFF.areContentsTheSame(old, new))
    }

    @Test
    fun `different sections are different rows`() {
        assertFalse(HomeAdapter.DIFF.areItemsTheSame(section("a", "One"), section("b", "One")))
    }

    @Test
    fun `the first row is a slider, with no title row above it`() {
        val adapter = adapter()
        adapter.submitSections(listOf(section("a", "One"), section("b", "Two")))
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)

        assertNotNull(holder.itemView.findViewById(R.id.sliderBooks))
    }
}
