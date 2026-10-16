package com.freelibrary.app.ui.cover

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import okio.FileSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
class CoilCoverCacheTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `an empty cache has no size`() {
        assertEquals(0L, CoilCoverCache(createCoverImageLoader(context)).sizeBytes())
    }

    @Test
    fun `the size counts the stored covers and clear empties the cache`() {
        val loader = createCoverImageLoader(context)
        val cache = CoilCoverCache(loader)
        val editor = loader.diskCache!!.openEditor("cover-1")!!
        FileSystem.SYSTEM.write(editor.metadata) { writeUtf8("meta") }
        FileSystem.SYSTEM.write(editor.data) { writeUtf8("0123456789") }
        editor.commit()

        assertTrue(cache.sizeBytes() >= 10L)

        cache.clear()

        assertEquals(0L, cache.sizeBytes())
    }
}
