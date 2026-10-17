package com.freelibrary.app.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale

/**
 * Reads the picked picture with Coil, which turns a camera photo upright and reads it at about the
 * size needed instead of at full size. The result is not cached: it is saved by [AvatarStore].
 */
class CoilAvatarDecoder(
    private val context: Context,
    private val imageLoader: ImageLoader,
) : AvatarDecoder {
    override suspend fun decode(
        source: Uri,
        minSidePx: Int,
    ): Bitmap? {
        val request =
            ImageRequest.Builder(context)
                .data(source)
                .size(minSidePx, minSidePx)
                .scale(Scale.FILL)
                .allowHardware(false)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        val result = imageLoader.execute(request)
        return ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
    }
}
