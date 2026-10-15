package com.freelibrary.app.ui.home

import android.content.Context
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.HomeSection
import com.freelibrary.app.domain.model.ViewMoreTarget
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.cover.createCoverImageLoader
import org.junit.After
import org.junit.Assert.assertEquals
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

    private fun adapter(onExplore: () -> Unit = {}) =
        HomeAdapter(CoverTypefaces(Typeface.DEFAULT_BOLD, Typeface.DEFAULT), imageLoader, {}, {}, onExplore)

    @Test
    fun `the explore button in the header calls back`() {
        var opened = 0
        val holder = adapter { opened++ }.onCreateViewHolder(FrameLayout(context), HomeAdapter.TYPE_HEADER)

        holder.itemView.findViewById<android.view.View>(R.id.homeExplore).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `the title row comes first and every section follows it in order`() {
        val rows = homeRows(listOf(section("a", "One"), section("b", "Two")))

        assertEquals(3, rows.size)
        assertEquals(HomeRow.Header, rows[0])
        assertEquals("a", (rows[1] as HomeRow.Slider).section.id)
        assertEquals("b", (rows[2] as HomeRow.Slider).section.id)
    }

    @Test
    fun `with no sections only the title row remains`() {
        assertEquals(listOf(HomeRow.Header), homeRows(emptyList()))
    }

    @Test
    fun `the same section is the same row even when its books change`() {
        val old = HomeRow.Slider(section("a", "One"))
        val new = HomeRow.Slider(section("a", "One", "Two"))

        assertTrue(HomeAdapter.DIFF.areItemsTheSame(old, new))
        assertFalse(HomeAdapter.DIFF.areContentsTheSame(old, new))
    }

    @Test
    fun `different sections are different rows`() {
        val a = HomeRow.Slider(section("a", "One"))
        val b = HomeRow.Slider(section("b", "One"))

        assertFalse(HomeAdapter.DIFF.areItemsTheSame(a, b))
    }

    @Test
    fun `the title row is never the same row as a slider`() {
        val slider = HomeRow.Slider(section("a", "One"))

        assertFalse(HomeAdapter.DIFF.areItemsTheSame(HomeRow.Header, slider))
        assertTrue(HomeAdapter.DIFF.areItemsTheSame(HomeRow.Header, HomeRow.Header))
    }

    @Test
    fun `the title row shows the app name as a heading`() {
        val holder = adapter().onCreateViewHolder(FrameLayout(context), HomeAdapter.TYPE_HEADER)
        val title = holder.itemView.findViewById<android.widget.TextView>(R.id.homeTitle)

        assertNotNull(title)
        assertEquals(context.getString(R.string.app_name), title.text.toString())
        assertTrue(ViewCompat.isAccessibilityHeading(title))
    }

    @Test
    fun `a slider row is built for the slider type`() {
        val holder = adapter().onCreateViewHolder(FrameLayout(context), HomeAdapter.TYPE_SLIDER)

        assertNotNull(holder.itemView.findViewById(R.id.sliderBooks))
    }
}
