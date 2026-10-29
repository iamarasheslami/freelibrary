package com.freelibrary.app.ui.booklist

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.ui.cover.CoverTypefaces

/**
 * The books of one slider, without the author line. A [large] slider shows bigger cards. The paged
 * "View all" lists reuse [BookCardViewHolder] and [DIFF].
 */
class BookCardAdapter(
    private val typefaces: CoverTypefaces,
    private val imageLoader: ImageLoader,
    private val large: Boolean,
    private val onBookClick: (BookCard) -> Unit,
) : ListAdapter<BookCard, BookCardViewHolder>(DIFF) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BookCardViewHolder =
        BookCardViewHolder.create(
            parent,
            typefaces,
            imageLoader,
            showAuthor = false,
            large = large,
            onBookClick = onBookClick,
        )

    /** Large and normal cards have different sizes, so a shared pool must not hand one to the other. */
    override fun getItemViewType(position: Int): Int = if (large) 1 else 0

    override fun onBindViewHolder(
        holder: BookCardViewHolder,
        position: Int,
    ) = holder.bind(getItem(position))

    companion object {
        /** The same book is the same item, and a card is redrawn only when something it shows changed. */
        val DIFF =
            object : DiffUtil.ItemCallback<BookCard>() {
                override fun areItemsTheSame(
                    oldItem: BookCard,
                    newItem: BookCard,
                ): Boolean = oldItem.bookId == newItem.bookId

                override fun areContentsTheSame(
                    oldItem: BookCard,
                    newItem: BookCard,
                ): Boolean = oldItem == newItem
            }
    }
}
