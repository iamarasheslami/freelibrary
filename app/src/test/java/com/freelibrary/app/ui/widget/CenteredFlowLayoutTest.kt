package com.freelibrary.app.ui.widget

import android.app.Application
import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CenteredFlowLayoutTest {
    /** A view that is always exactly [size] pixels square, whatever it is offered. */
    private class FixedView(
        context: Context,
        private val size: Int,
    ) : View(context) {
        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) = setMeasuredDimension(size, size)
    }

    private fun fixedView(
        layout: CenteredFlowLayout,
        size: Int,
    ): View = FixedView(layout.context, size).also { layout.addView(it) }

    private fun layoutOut(count: Int): List<View> {
        val layout = CenteredFlowLayout(ApplicationProvider.getApplicationContext<Application>())
        val views = List(count) { fixedView(layout, 100) }
        layout.measure(MeasureSpec.makeMeasureSpec(350, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        layout.layout(0, 0, 350, layout.measuredHeight)
        return views
    }

    @Test
    fun `children that fit stay on one centred row`() {
        val views = layoutOut(3)
        assertEquals(25, views[0].left)
        assertEquals(0, views[0].top)
        assertEquals(views[0].top, views[2].top)
    }

    @Test
    fun `a short second row is centred instead of starting at the edge`() {
        val views = layoutOut(4)
        assertEquals(100, views[3].top)
        assertEquals(125, views[3].left)
    }
}
