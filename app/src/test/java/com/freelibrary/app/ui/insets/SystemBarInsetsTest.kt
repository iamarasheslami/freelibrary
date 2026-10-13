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
class SystemBarInsetsTest {
    private fun newView(): View = View(ApplicationProvider.getApplicationContext())

    private fun insets(
        top: Int,
        bottom: Int,
    ): WindowInsetsCompat =
        WindowInsetsCompat
            .Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, top, 0, bottom))
            .build()

    @Test
    fun `the system bars are added to the top and bottom padding`() {
        val view = newView()
        view.applySystemBarInsetsAsPadding()

        ViewCompat.dispatchApplyWindowInsets(view, insets(top = 24, bottom = 48))

        assertEquals(24, view.paddingTop)
        assertEquals(48, view.paddingBottom)
    }

    @Test
    fun `the padding the view already had is kept`() {
        val view = newView()
        view.setPadding(5, 10, 7, 20)
        view.applySystemBarInsetsAsPadding()

        ViewCompat.dispatchApplyWindowInsets(view, insets(top = 24, bottom = 48))

        assertEquals(5, view.paddingLeft)
        assertEquals(34, view.paddingTop)
        assertEquals(7, view.paddingRight)
        assertEquals(68, view.paddingBottom)
    }

    @Test
    fun `new insets replace the old ones instead of piling up`() {
        val view = newView()
        view.setPadding(0, 10, 0, 20)
        view.applySystemBarInsetsAsPadding()

        ViewCompat.dispatchApplyWindowInsets(view, insets(top = 24, bottom = 48))
        ViewCompat.dispatchApplyWindowInsets(view, insets(top = 0, bottom = 0))

        assertEquals(10, view.paddingTop)
        assertEquals(20, view.paddingBottom)
    }
}
