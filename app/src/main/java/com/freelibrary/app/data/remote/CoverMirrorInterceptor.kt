package com.freelibrary.app.data.remote

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** The longest the first cover waits for the check of the official site. */
internal const val FIRST_CHECK_WAIT_MILLIS = 3_500L

private val COVER_PATH = Regex("^/cache/epub/\\d+/pg\\d+\\.cover\\.medium\\.jpg$")

/**
 * Lets a book cover come from a Project Gutenberg mirror when gutenberg.org does not answer.
 *
 * [health] says whether the official site can be reached. If it can, a cover request for [official]
 * is tried there first and, when that fails or is not a success, asked from [mirror], which keeps
 * the generated EPUBs and their covers under the same paths. If it cannot, the mirror is asked first
 * and the official site is the second choice. Any other request passes through untouched.
 */
class CoverMirrorInterceptor(
    private val official: HttpUrl,
    private val mirror: HttpUrl,
    private val health: OfficialHostHealth,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isOfficialCover(request.url)) return chain.proceed(request)

        val order = if (health.reachable(FIRST_CHECK_WAIT_MILLIS)) listOf(official, mirror) else listOf(mirror, official)
        var failure: IOException? = null
        var fallback: Response? = null
        for (base in order) {
            try {
                val response = chain.proceed(request.newBuilder().url(request.url.movedTo(base)).build())
                if (base == official) health.reportSuccess()
                if (response.isSuccessful) {
                    fallback?.close()
                    return response
                }
                fallback?.close()
                fallback = response
            } catch (e: IOException) {
                // A request the app itself cancelled, for example a cover scrolled out of sight, is not a failure of the site.
                if (chain.call().isCanceled()) {
                    fallback?.close()
                    throw e
                }
                if (base == official) health.reportFailure()
                failure = e
            }
        }
        return fallback ?: throw (failure ?: IOException("No cover address answered"))
    }

    private fun isOfficialCover(url: HttpUrl): Boolean =
        url.scheme == official.scheme && url.host == official.host && url.port == official.port && COVER_PATH.matches(url.encodedPath)

    private fun HttpUrl.movedTo(base: HttpUrl): HttpUrl = newBuilder().scheme(base.scheme).host(base.host).port(base.port).build()
}
