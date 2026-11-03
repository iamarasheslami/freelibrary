package com.freelibrary.app.ui.profile.avatar

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.freelibrary.app.domain.model.AvatarDesign
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** Turns an avatar the reader built into a picture file that the profile editor can take like a chosen photo. */
interface AvatarDrafts {
    /** Writes [design] as a picture and returns where it is; problems with writing throw [IOException]. */
    suspend fun write(design: AvatarDesign): Uri
}

/** Keeps the pictures in the app's cache folder, where the system may clear them; only the newest one is kept. */
class FileAvatarDrafts
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AvatarDrafts {
        override suspend fun write(design: AvatarDesign): Uri =
            withContext(Dispatchers.IO) {
                val directory = File(context.cacheDir, FOLDER)
                if (!directory.isDirectory && !directory.mkdirs()) throw IOException("The avatar folder could not be created")
                directory.listFiles()?.forEach { it.delete() }
                val file = File(directory, "avatar-${System.nanoTime()}.png")
                val bitmap = AvatarRenderer.toBitmap(design, SIZE_PX)
                try {
                    file.outputStream().use { output ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) {
                            throw IOException("The avatar could not be saved")
                        }
                    }
                } finally {
                    bitmap.recycle()
                }
                Uri.fromFile(file)
            }

        private companion object {
            const val FOLDER = "avatar-drafts"
            const val SIZE_PX = 512
            const val PNG_QUALITY = 100
        }
    }
