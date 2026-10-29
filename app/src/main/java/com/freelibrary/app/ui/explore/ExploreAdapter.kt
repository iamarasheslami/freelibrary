package com.freelibrary.app.ui.explore

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.ExploreRow
import com.freelibrary.app.domain.model.ExploreSection
import com.freelibrary.app.ui.booklist.BookSliderViewHolder
import com.freelibrary.app.ui.cover.CoverTypefaces

/** The heading of a slider: a curated collection uses its string resource, any other shelf its catalog name. */
internal fun ExploreSection.heading(getString: (Int) -> String): String = titleRes?.let(getString) ?: title

/** The Explore list: one slider per section, loaded page by page. Every slider shares one view pool. */
class ExploreAdapter(
    private val typefaces: CoverTypefaces,
    private val imageLoader: ImageLoader,
    private val onBookClick: (BookCard) -> Unit,
    private val onViewMore: (ExploreSection) -> Unit,
) : PagingDataAdapter<ExploreRow, BookSliderViewHolder>(DIFF) {
    private val viewPool = RecyclerView.RecycledViewPool()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BookSliderViewHolder = BookSliderViewHolder.create(parent, typefaces, imageLoader, viewPool, onBookClick = onBookClick)

    override fun onBindViewHolder(
        holder: BookSliderViewHolder,
        position: Int,
    ) {
        val row = getItem(position) ?: return
        val context = holder.itemView.context
        holder.bind(
            heading = row.section.heading(context::getString),
            books = row.slider.books,
            hasMore = row.slider.hasMore,
            onViewMore = { onViewMore(row.section) },
        )
    }

    companion object {
        val DIFF =
            object : DiffUtil.ItemCallback<ExploreRow>() {
                override fun areItemsTheSame(
                    oldItem: ExploreRow,
                    newItem: ExploreRow,
                ): Boolean = oldItem.section.id == newItem.section.id

                override fun areContentsTheSame(
                    oldItem: ExploreRow,
                    newItem: ExploreRow,
                ): Boolean = oldItem == newItem
            }
    }
}
