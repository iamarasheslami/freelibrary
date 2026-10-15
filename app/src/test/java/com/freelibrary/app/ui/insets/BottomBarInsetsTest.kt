package com.freelibrary.app.ui.insets

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BottomBarInsetsTest {
    private fun insets(
        top: Int,
        bottom: Int,
        left: Int = 0,
        right: Int = 0,
    ): WindowInsetsCompat =
        WindowInsetsCompat
            .Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(left, top, right, bottom))
            .build()

    @Test
    fun `the bottom of the system bars is removed`() {
        val result = insets(top = 24, bottom = 48).withoutBottomSystemBar()

        assertEquals(Insets.of(0, 24, 0, 0), result.getInsets(WindowInsetsCompat.Type.systemBars()))
    }

    @Test
    fun `the top and the sides are kept`() {
        val result = insets(top = 10, bottom = 20, left = 3, right = 4).withoutBottomSystemBar()

        assertEquals(Insets.of(3, 10, 4, 0), result.getInsets(WindowInsetsCompat.Type.systemBars()))
    }

    @Test
    fun `a screen above the bar pads for the top only`() {
        val screen = View(ApplicationProvider.getApplicationContext())
        screen.applySystemBarInsetsAsPadding()

        ViewCompat.dispatchApplyWindowInsets(screen, insets(top = 24, bottom = 48).withoutBottomSystemBar())

        assertEquals(24, screen.paddingTop)
        assertEquals(0, screen.paddingBottom)
    }
}
