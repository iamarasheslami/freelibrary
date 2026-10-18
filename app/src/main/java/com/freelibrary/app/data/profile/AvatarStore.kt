package com.freelibrary.app.data.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/** Reads a picture the reader chose into a bitmap at least [minSidePx] on its short side, or null if it cannot be read. */
fun interface AvatarDecoder {
    suspend fun decode(
        source: Uri,
        minSidePx: Int,
    ): Bitmap?
}

private const val AVATAR_SIZE_PX = 512
private const val JPEG_QUALITY = 90
private const val FILE_NAME = "avatar.jpg"
private const val COPY_BUFFER_BYTES = 8 * 1024
private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

/**
 * Keeps the reader's profile picture: one square JPEG of at most 512 px in the app's private
 * folder, so no storage permission is needed and no other app can read it. The picked picture is
 * cut to its centre square, so any photo fits the round frame. A new picture replaces the old one
 * only once it has been written completely; a picture that cannot be read changes nothing.
 */
class AvatarStore(
    private val directory: File,
    private val decoder: AvatarDecoder,
) {
    private val avatarFile = File(directory, FILE_NAME)

    /** The saved picture, or null when there is none. */
    fun file(): File? = avatarFile.takeIf { it.isFile }

    /** Saves [source] as the picture. False when it could not be read as an image; writing problems throw [IOException]. */
    suspend fun save(source: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val decoded = decoder.decode(source, AVATAR_SIZE_PX) ?: return@withContext false
            if (decoded.width <= 0 || decoded.height <= 0) return@withContext false
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("The profile folder could not be created")
            val square = centreSquare(decoded)
            val partial = File(directory, "$FILE_NAME.part")
            try {
                partial.outputStream().use { output ->
                    if (!square.compress(
                            Bitmap.CompressFormat.JPEG,
                            JPEG_QUALITY,
                            output,
                        )
                    ) {
                        throw IOException("The picture could not be saved")
                    }
                }
                avatarFile.delete()
                if (!partial.renameTo(avatarFile)) throw IOException("The picture could not be saved")
            } finally {
                partial.delete()
                square.recycle()
            }
            true
        }

    /**
     * Saves a picture that is already a finished avatar (read from a backup). It is only kept if it
     * is a real image no larger than [maxBytes]; otherwise nothing changes and the result is false.
     */
    suspend fun saveFinished(
        input: InputStream,
        maxBytes: Long,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("The profile folder could not be created")
            val partial = File(directory, "$FILE_NAME.part")
            try {
                var total = 0L
                var tooBig = false
                partial.outputStream().use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBytes) {
                            tooBig = true
                            break
                        }
                        output.write(buffer, 0, read)
                    }
                }
                if (tooBig || !isImage(partial)) return@withContext false
                avatarFile.delete()
                if (!partial.renameTo(avatarFile)) throw IOException("The picture could not be saved")
                true
            } finally {
                partial.delete()
            }
        }

    /** A JPEG (the only kind the app ever writes) that the system can read the size of. */
    private fun isImage(file: File): Boolean {
        val signature = ByteArray(JPEG_SIGNATURE.size)
        val read = file.inputStream().use { it.read(signature) }
        if (read != signature.size || !signature.contentEquals(JPEG_SIGNATURE)) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    /** Removes the picture, if there is one. */
    suspend fun remove() {
        withContext(Dispatchers.IO) { avatarFile.delete() }
    }

    /** A new bitmap holding the middle square of [source], at most [AVATAR_SIZE_PX] wide, on a white ground. */
    private fun centreSquare(source: Bitmap): Bitmap {
        val side = minOf(source.width, source.height)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        val target = minOf(side, AVATAR_SIZE_PX)
        val result = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, Rect(left, top, left + side, top + side), Rect(0, 0, target, target), Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }
}
