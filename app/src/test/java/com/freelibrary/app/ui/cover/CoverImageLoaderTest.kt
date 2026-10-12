package com.freelibrary.app.ui.cover

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
class CoverImageLoaderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the memory cache is capped at the agreed size`() {
        val cache = createCoverImageLoader(context).memoryCache

        assertNotNull(cache)
        assertEquals(COVER_MEMORY_CACHE_BYTES, cache!!.maxSize)
    }

    @Test
    fun `the disk cache is capped at about 30 MB`() {
        val cache = createCoverImageLoader(context).diskCache

        assertNotNull(cache)
        assertEquals(30L * 1024 * 1024, cache!!.maxSize)
    }

    @Test
    fun `covers are cached in their own folder inside the app cache`() {
        val cache = createCoverImageLoader(context).diskCache

        assertEquals(File(context.cacheDir, "covers"), cache!!.directory.toFile())
    }
}
