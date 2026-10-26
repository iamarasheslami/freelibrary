package com.freelibrary.app.ui.booklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.databinding.ItemBookSliderBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.BookSlider
import com.freelibrary.app.ui.cover.CoverTypefaces

/**
 * One slider on the Home screen: a heading, a "View more" button and a horizontal row of book
 * cards. [viewPool] is shared by every slider on the screen, so cards scrolled out of one row
 * are reused in the others instead of being created again.
 */
class BookSliderViewHolder(
    private val binding: ItemBookSliderBinding,
    typefaces: CoverTypefaces,
    imageLoader: ImageLoader,
    viewPool: RecyclerView.RecycledViewPool,
    onBookClick: (BookCard) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val bookAdapter = BookCardAdapter(typefaces, imageLoader, onBookClick)

    init {
        ViewCompat.setAccessibilityHeading(binding.sliderHeading, true)
        binding.sliderBooks.apply {
            adapter = bookAdapter
            setRecycledViewPool(viewPool)
            addItemDecoration(HorizontalGapDecoration(resources.getDimensionPixelSize(R.dimen.spacing_s)))
        }
    }

    /** "View more" is offered only when the shelf holds more books than the slider shows. */
    fun bind(
        heading: String,
        slider: BookSlider,
        onViewMore: () -> Unit,
    ) = bind(heading, slider.books, slider.hasMore, onViewMore)

    fun bind(
        heading: String,
        books: List<BookCard>,
        hasMore: Boolean,
        onViewMore: () -> Unit,
    ) {
        binding.sliderHeading.text = heading
        binding.sliderViewMore.isVisible = hasMore
        binding.sliderViewMore.contentDescription = itemView.context.getString(R.string.view_more_in, heading)
        binding.sliderViewMore.setOnClickListener { onViewMore() }
        bookAdapter.submitList(books)
    }

    companion object {
        fun create(
            parent: ViewGroup,
            typefaces: CoverTypefaces,
            imageLoader: ImageLoader,
            viewPool: RecyclerView.RecycledViewPool,
            onBookClick: (BookCard) -> Unit,
        ): BookSliderViewHolder =
            BookSliderViewHolder(
                ItemBookSliderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                typefaces,
                imageLoader,
                viewPool,
                onBookClick,
            )
    }
}
