package com.freelibrary.app.ui.booklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.freelibrary.app.databinding.ItemBookCardBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.ui.cover.CoverTitle
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.cover.loadCover

/**
 * One book card in a slider or a list. The title shows the main title only, because Gutenberg
 * titles often end in a long "Or, ..." subtitle that would not fit under a small cover. The
 * author line is hidden but keeps its space when a book has no author, so every card in a row
 * stays the same height.
 */
class BookCardViewHolder(
    private val binding: ItemBookCardBinding,
    private val typefaces: CoverTypefaces,
    private val imageLoader: ImageLoader,
    private val onBookClick: (BookCard) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private var book: BookCard? = null

    init {
        binding.root.setOnClickListener { book?.let(onBookClick) }
    }

    fun bind(book: BookCard) {
        this.book = book
        binding.bookTitle.text = CoverTitle.parse(book.title).main
        binding.bookAuthor.text = book.authorName.orEmpty()
        binding.bookAuthor.isInvisible = book.authorName.isNullOrBlank()
        binding.bookCover.loadCover(book, typefaces, imageLoader)
    }

    companion object {
        fun create(
            parent: ViewGroup,
            typefaces: CoverTypefaces,
            imageLoader: ImageLoader,
            onBookClick: (BookCard) -> Unit,
        ): BookCardViewHolder =
            BookCardViewHolder(
                ItemBookCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                typefaces,
                imageLoader,
                onBookClick,
            )
    }
}
