package com.freelibrary.app.ui.home

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.HomeSection
import com.freelibrary.app.ui.booklist.BookSliderViewHolder
import com.freelibrary.app.ui.cover.CoverTypefaces

/** The Home list: one slider per section. Every slider shares one pool, so cards are reused across rows. */
class HomeAdapter(
    private val typefaces: CoverTypefaces,
    private val imageLoader: ImageLoader,
    private val onBookClick: (BookCard) -> Unit,
    private val onViewMore: (HomeSection) -> Unit,
) : ListAdapter<HomeSection, BookSliderViewHolder>(DIFF) {
    private val viewPool = RecyclerView.RecycledViewPool()

    fun submitSections(sections: List<HomeSection>) = submitList(sections)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BookSliderViewHolder = BookSliderViewHolder.create(parent, typefaces, imageLoader, viewPool, onBookClick)

    override fun onBindViewHolder(
        holder: BookSliderViewHolder,
        position: Int,
    ) {
        val section = getItem(position)
        holder.bind(
            heading = holder.itemView.context.getString(section.titleRes),
            books = section.books,
            hasMore = section.hasMore,
            onViewMore = { onViewMore(section) },
        )
    }

    companion object {
        /** A slider is the same item while its section id is the same, even when its books change. */
        val DIFF =
            object : DiffUtil.ItemCallback<HomeSection>() {
                override fun areItemsTheSame(
                    oldItem: HomeSection,
                    newItem: HomeSection,
                ): Boolean = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: HomeSection,
                    newItem: HomeSection,
                ): Boolean = oldItem == newItem
            }
    }
}
