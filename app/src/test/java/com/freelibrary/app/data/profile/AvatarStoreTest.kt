package com.freelibrary.app.data.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AvatarStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val source = Uri.parse("content://pictures/1")

    private fun store(decoder: AvatarDecoder) = AvatarStore(File(temporaryFolder.root, "profile"), decoder)

    private fun bitmap(
        width: Int,
        height: Int,
    ): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    private fun savedSize(file: File): Pair<Int, Int> {
        val bitmap = BitmapFactory.decodeFile(file.path)
        assertNotNull("The saved file is not an image", bitmap)
        return bitmap.width to bitmap.height
    }

    @Test
    fun `there is no picture until one is saved`() {
        assertNull(store { _, _ -> null }.file())
    }

    @Test
    fun `a large wide picture is cut to a 512 pixel square`() =
        runTest {
            val store = store { _, _ -> bitmap(1000, 600) }

            assertTrue(store.save(source))

            assertEquals(512 to 512, savedSize(store.file()!!))
        }

    @Test
    fun `a small picture is cut to a square but not enlarged`() =
        runTest {
            val store = store { _, _ -> bitmap(300, 200) }

            assertTrue(store.save(source))

            assertEquals(200 to 200, savedSize(store.file()!!))
        }

    @Test
    fun `a picture that cannot be read changes nothing`() =
        runTest {
            val good = store { _, _ -> bitmap(100, 100) }
            good.save(source)
            val before = good.file()!!.readBytes()

            val failing = store { _, _ -> null }

            assertFalse(failing.save(source))
            assertTrue(before.contentEquals(failing.file()!!.readBytes()))
        }

    @Test
    fun `a new picture replaces the old one and leaves no partial file behind`() =
        runTest {
            var side = 100
            val store = store { _, _ -> bitmap(side, side) }
            store.save(source)

            side = 150
            store.save(source)

            assertEquals(150 to 150, savedSize(store.file()!!))
            assertEquals(listOf("avatar.jpg"), File(temporaryFolder.root, "profile").list()!!.toList())
        }

    @Test
    fun `removing the picture deletes it`() =
        runTest {
            val store = store { _, _ -> bitmap(100, 100) }
            store.save(source)

            store.remove()

            assertNull(store.file())
        }

    @Test
    fun `removing when there is no picture is harmless`() =
        runTest {
            store { _, _ -> null }.remove()
        }
}
