package com.freelibrary.app.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.domain.model.AppSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AppScaleTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the density is scaled by the size`() {
        assertEquals(320, scaledDensityDpi(320, AppSize.LARGE))
        assertEquals(294, scaledDensityDpi(320, AppSize.MEDIUM))
        assertEquals(272, scaledDensityDpi(320, AppSize.SMALL))
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun `the first start picks small on a narrow screen and remembers it`() {
        assertEquals(AppSize.SMALL, AppScale.current(context))

        AppScale.save(context, AppSize.LARGE)

        assertEquals(AppSize.LARGE, AppScale.current(context))
    }

    @Test
    @Config(qualifiers = "w480dp-h800dp")
    fun `the first start picks medium on a roomy screen`() {
        assertEquals(AppSize.MEDIUM, AppScale.current(context))
    }

    @Test
    fun `a wrapped context is drawn smaller for a smaller size`() {
        val original = context.resources.configuration.densityDpi

        AppScale.save(context, AppSize.SMALL)
        assertEquals(scaledDensityDpi(original, AppSize.SMALL), AppScale.wrap(context).resources.configuration.densityDpi)

        AppScale.save(context, AppSize.LARGE)
        assertEquals(original, AppScale.wrap(context).resources.configuration.densityDpi)
        assertTrue(scaledDensityDpi(original, AppSize.SMALL) < original)
    }
}
