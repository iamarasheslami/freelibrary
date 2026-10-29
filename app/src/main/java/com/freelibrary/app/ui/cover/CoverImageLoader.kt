package com.freelibrary.app.ui.cover

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.freelibrary.app.data.remote.CoverMirrorInterceptor
import com.freelibrary.app.data.remote.OfficialHostHealth
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The most memory the decoded covers may hold at once. Slider thumbnails are small, so this is a lot of them. */
internal const val COVER_MEMORY_CACHE_BYTES = 8 * 1024 * 1024

/** The most disk the downloaded covers may use. The least recently used ones are removed past this. */
internal const val COVER_DISK_CACHE_BYTES = 30L * 1024 * 1024

private const val COVER_DISK_CACHE_FOLDER = "covers"

/** Where the catalog's cover addresses point, and the mirror that carries the same files. */
private const val OFFICIAL_COVER_HOST = "https://www.gutenberg.org/"
private const val MIRROR_COVER_HOST = "https://gutenberg.pglaf.org/"

private const val COVER_CONNECT_TIMEOUT_SECONDS = 8L
private const val COVER_READ_TIMEOUT_SECONDS = 20L
private const val COVER_CALL_TIMEOUT_SECONDS = 40L
private const val CHECK_TIMEOUT_SECONDS = 3L
private const val CHECK_URL = "https://www.gutenberg.org/robots.txt"

/** Asks gutenberg.org for one small file and reports whether it answered at all, whatever the answer was. */
private fun officialSiteAnswers(): Boolean {
    val client =
        OkHttpClient.Builder()
            .connectTimeout(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    return try {
        client.newCall(Request.Builder().url(CHECK_URL).head().build()).execute().use { true }
    } catch (e: IOException) {
        false
    }
}

/** The connection covers use: short time limits, and the mirror when gutenberg.org cannot be reached. */
private fun coverHttpClient(health: OfficialHostHealth): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(COVER_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(COVER_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(COVER_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(CoverMirrorInterceptor(OFFICIAL_COVER_HOST.toHttpUrl(), MIRROR_COVER_HOST.toHttpUrl(), health))
        .build()

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
    // Found out now, so the first covers do not have to find out the hard way that the official site is blocked.
    val health = OfficialHostHealth(::officialSiteAnswers).also { it.start() }
    return ImageLoader.Builder(appContext)
        .okHttpClient(coverHttpClient(health))
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
