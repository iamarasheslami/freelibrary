package com.freelibrary.app.ui.cover

import android.content.Context
import android.graphics.Typeface
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import coil.ImageLoader
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.freelibrary.app.R
import com.freelibrary.app.domain.model.BookCard

/**
 * The two typefaces a generated cover is drawn with: the bundled serif for the title and the system
 * font for the details. Load them once and share them, because every cover needs the same two.
 */
class CoverTypefaces(
    val title: Typeface,
    val detail: Typeface,
) {
    companion object {
        /** Falls back to the system bold font if the bundled serif cannot be loaded, so a cover always draws. */
        fun from(context: Context): CoverTypefaces {
            val serif = runCatching { ResourcesCompat.getFont(context, R.font.literata_semibold) }.getOrNull()
            return CoverTypefaces(serif ?: Typeface.DEFAULT_BOLD, Typeface.DEFAULT)
        }
    }
}

/** A book always gets the same cover style, chosen from its Gutenberg id. */
internal fun coverStyleFor(book: BookCard): CoverStyle = CoverStyle.forKey(book.externalId)

/** The cover drawn from the book's own data, for books without a picture and while one loads. */
fun generatedCoverFor(
    book: BookCard,
    typefaces: CoverTypefaces,
): GeneratedCoverDrawable =
    GeneratedCoverDrawable(
        coverStyle = coverStyleFor(book),
        title = book.title,
        author = book.authorName,
        titleTypeface = typefaces.title,
        detailTypeface = typefaces.detail,
    )

/**
 * Shows a book's cover in this view. The generated cover is on screen at once and stays if the book
 * has no picture, the request fails or the phone is offline. Otherwise the medium cover replaces it
 * when it arrives. Calling this again for the same book while its cover is showing or loading does nothing,
 * because a new request would cancel the one in flight and start the same download again; a request that
 * fails or is cancelled is forgotten, so the next call tries again. Calling it for another book (a recycled
 * view) cancels the earlier request and loads the new cover.
 */
fun ImageView.loadCover(
    book: BookCard,
    typefaces: CoverTypefaces,
    imageLoader: ImageLoader,
) {
    val url = mediumCoverUrl(book.coverUrl)
    val key = coverRequestKey(book.bookId, url)
    if (getTag(R.id.cover_request_key) == key) return
    setTag(R.id.cover_request_key, key)
    val generated = generatedCoverFor(book, typefaces)
    load(url, imageLoader) {
        placeholder(generated)
        error(generated)
        fallback(generated)
        listener(
            object : ImageRequest.Listener {
                override fun onCancel(request: ImageRequest) = forgetCoverRequest(key)

                override fun onError(
                    request: ImageRequest,
                    result: ErrorResult,
                ) = forgetCoverRequest(key)
            },
        )
    }
}

/** What identifies a cover request: the book and the picture asked for, so a different book never reuses another's cover. */
internal fun coverRequestKey(
    bookId: Long,
    url: String?,
): String = "$bookId|${url.orEmpty()}"

/** Lets the next [loadCover] for this book start a request again, unless the view has moved on to another book meanwhile. */
private fun ImageView.forgetCoverRequest(key: String) {
    if (getTag(R.id.cover_request_key) == key) setTag(R.id.cover_request_key, null)
}
