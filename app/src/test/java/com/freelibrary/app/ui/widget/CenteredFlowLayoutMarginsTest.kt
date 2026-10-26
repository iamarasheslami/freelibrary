package com.freelibrary.app.ui.widget

import android.app.Application
import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CenteredFlowLayoutMarginsTest {
    private class SquareView(
        context: Context,
    ) : View(context) {
        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) = setMeasuredDimension(SIZE, SIZE)
    }

    @Test
    fun `margins keep the children apart and are centred with them`() {
        val layout = CenteredFlowLayout(ApplicationProvider.getApplicationContext<Application>())
        val views = List(2) { SquareView(layout.context).also(layout::addView) }
        views.forEach { (it.layoutParams as ViewGroup.MarginLayoutParams).setMargins(10, 5, 10, 5) }

        layout.measure(MeasureSpec.makeMeasureSpec(350, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        layout.layout(0, 0, 350, layout.measuredHeight)

        assertEquals(110, layout.measuredHeight)
        assertEquals(65, views[0].left)
        assertEquals(185, views[1].left)
        assertEquals(20, views[1].left - views[0].right)
    }

    @Test
    fun `margins can push a child onto the next row`() {
        val layout = CenteredFlowLayout(ApplicationProvider.getApplicationContext<Application>())
        val views = List(3) { SquareView(layout.context).also(layout::addView) }
        views.forEach { (it.layoutParams as ViewGroup.MarginLayoutParams).setMargins(10, 5, 10, 5) }

        layout.measure(MeasureSpec.makeMeasureSpec(350, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))

        assertEquals(220, layout.measuredHeight)
    }

    private companion object {
        const val SIZE = 100
    }
}
