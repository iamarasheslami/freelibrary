package com.freelibrary.app.ui.cover

import android.content.Context
import android.graphics.Typeface
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import coil.ImageLoader
import coil.load
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
 * has no picture, the request fails or the phone is offline. Otherwise the small cover replaces it
 * when it arrives. Calling this again on a recycled view cancels the earlier request.
 */
fun ImageView.loadCover(
    book: BookCard,
    typefaces: CoverTypefaces,
    imageLoader: ImageLoader,
) {
    val generated = generatedCoverFor(book, typefaces)
    load(smallCoverUrl(book.coverUrl), imageLoader) {
        placeholder(generated)
        error(generated)
        fallback(generated)
    }
}
