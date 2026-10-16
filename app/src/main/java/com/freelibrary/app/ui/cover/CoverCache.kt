package com.freelibrary.app.ui.cover

import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi

/** The downloaded covers kept on this phone: how much space they use, and a way to empty them. */
interface CoverCache {
    /** The space the stored covers use on disk, in bytes. */
    fun sizeBytes(): Long

    /** Removes every stored cover, in memory and on disk. They are downloaded again when needed. */
    fun clear()
}

/** The cover cache of the app's one Coil image loader. */
@OptIn(ExperimentalCoilApi::class)
class CoilCoverCache(
    private val imageLoader: ImageLoader,
) : CoverCache {
    override fun sizeBytes(): Long = imageLoader.diskCache?.size ?: 0L

    override fun clear() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}
