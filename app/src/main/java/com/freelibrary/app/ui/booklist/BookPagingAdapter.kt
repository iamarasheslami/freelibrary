package com.freelibrary.app.ui.booklist

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import coil.ImageLoader
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.ui.cover.CoverTypefaces

/** The books of a full "View more" list, loaded a page at a time. It reuses the slider's card and diff rules. */
class BookPagingAdapter(
    private val typefaces: CoverTypefaces,
    private val imageLoader: ImageLoader,
    private val onBookClick: (BookCard) -> Unit,
) : PagingDataAdapter<BookCard, BookCardViewHolder>(BookCardAdapter.DIFF) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BookCardViewHolder = BookCardViewHolder.create(parent, typefaces, imageLoader, onBookClick)

    override fun onBindViewHolder(
        holder: BookCardViewHolder,
        position: Int,
    ) {
        getItem(position)?.let(holder::bind)
    }
}
