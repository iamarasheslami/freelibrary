package com.freelibrary.app.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_SECONDS = 20L
private const val READ_TIMEOUT_SECONDS = 30L
private const val CALL_TIMEOUT_SECONDS = 180L

/** The largest answer the catalog sync accepts. The whole manifest is about 4 MB; a book file is a few kilobytes. */
const val MAX_CATALOG_RESPONSE_BYTES = 64L * 1024 * 1024

/**
 * The client for the catalog sync. Every call has a time limit, so a server that stops answering cannot
 * hold the sync (and its background scope) forever. An answer larger than [maxResponseBytes] is refused,
 * counted after decompression, so neither a mistake nor a hostile server can fill the phone's memory.
 * HTTPS to HTTP redirects are not followed. [userAgent], when given, tells the server which app is asking.
 */
fun createCatalogHttpClient(
    userAgent: String? = null,
    maxResponseBytes: Long = MAX_CATALOG_RESPONSE_BYTES,
): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followSslRedirects(false)
        .apply {
            if (userAgent != null) {
                addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
                }
            }
        }
        .addInterceptor(ResponseSizeLimit(maxResponseBytes))
        .build()

/** Fails a call with an [IOException] as soon as its answer is larger than [maxBytes]. */
class ResponseSizeLimit(
    private val maxBytes: Long,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        if (body.contentLength() > maxBytes) {
            response.close()
            throw IOException("The answer is larger than $maxBytes bytes")
        }
        val counting =
            object : ForwardingSource(body.source()) {
                private var total = 0L

                override fun read(
                    sink: Buffer,
                    byteCount: Long,
                ): Long {
                    val read = super.read(sink, byteCount)
                    if (read > 0) {
                        total += read
                        if (total > maxBytes) throw IOException("The answer is larger than $maxBytes bytes")
                    }
                    return read
                }
            }
        return response.newBuilder().body(counting.buffer().asResponseBody(body.contentType(), body.contentLength())).build()
    }
}
