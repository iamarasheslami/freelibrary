package com.freelibrary.app.ui.cover

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

/** The most memory the decoded covers may hold at once. Slider thumbnails are small, so this is a lot of them. */
internal const val COVER_MEMORY_CACHE_BYTES = 8 * 1024 * 1024

/** The most disk the downloaded covers may use. The least recently used ones are removed past this. */
internal const val COVER_DISK_CACHE_BYTES = 30L * 1024 * 1024

private const val COVER_DISK_CACHE_FOLDER = "covers"

/**
 * The one image loader the app uses for book covers.
 *
 * Both caches have fixed ceilings, so browsing hundreds of books cannot grow memory or storage
 * without limit on a low-end phone. Covers are kept on disk whatever Gutenberg's cache headers
 * say, so a cover seen once also shows without a connection. Pixels are stored as RGB565, which
 * halves the memory of each decoded cover and is not noticeable at thumbnail size. There is no
 * fade-in, because the generated cover is already on screen while the picture loads.
 */
fun createCoverImageLoader(context: Context): ImageLoader {
    val appContext = context.applicationContext
    return ImageLoader.Builder(appContext)
        .memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizeBytes(COVER_MEMORY_CACHE_BYTES)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(appContext.cacheDir, COVER_DISK_CACHE_FOLDER))
                .maxSizeBytes(COVER_DISK_CACHE_BYTES)
                .build()
        }
        .allowRgb565(true)
        .respectCacheHeaders(false)
        .crossfade(false)
        .build()
}
