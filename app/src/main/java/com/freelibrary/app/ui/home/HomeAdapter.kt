package com.freelibrary.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.freelibrary.app.databinding.ItemHomeHeaderBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.HomeSection
import com.freelibrary.app.ui.booklist.BookSliderViewHolder
import com.freelibrary.app.ui.cover.CoverTypefaces

/** One row of the Home list: the scrolling title first, then one slider per section. */
sealed interface HomeRow {
    data object Header : HomeRow

    data class Slider(
        val section: HomeSection,
    ) : HomeRow
}

internal fun homeRows(sections: List<HomeSection>): List<HomeRow> = listOf(HomeRow.Header) + sections.map { HomeRow.Slider(it) }

/** The title, then the sliders. Every slider shares one pool, so cards are reused across rows. */
class HomeAdapter(
    private val typefaces: CoverTypefaces,
    private val imageLoader: ImageLoader,
    private val onBookClick: (BookCard) -> Unit,
    private val onViewMore: (HomeSection) -> Unit,
) : ListAdapter<HomeRow, RecyclerView.ViewHolder>(DIFF) {
    private val viewPool = RecyclerView.RecycledViewPool()

    fun submitSections(sections: List<HomeSection>) = submitList(homeRows(sections))

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            HomeRow.Header -> TYPE_HEADER
            is HomeRow.Slider -> TYPE_SLIDER
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> HeaderViewHolder.create(parent)
            else -> BookSliderViewHolder.create(parent, typefaces, imageLoader, viewPool, onBookClick)
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val row = getItem(position)
        if (holder is BookSliderViewHolder && row is HomeRow.Slider) {
            val section = row.section
            holder.bind(
                heading = holder.itemView.context.getString(section.titleRes),
                books = section.books,
                hasMore = section.hasMore,
                onViewMore = { onViewMore(section) },
            )
        }
    }

    private class HeaderViewHolder(
        binding: ItemHomeHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            ViewCompat.setAccessibilityHeading(binding.homeLogo, true)
        }

        companion object {
            fun create(parent: ViewGroup) =
                HeaderViewHolder(ItemHomeHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SLIDER = 1

        /** A slider is the same item while its section id is the same; a title never changes. */
        val DIFF =
            object : DiffUtil.ItemCallback<HomeRow>() {
                override fun areItemsTheSame(
                    oldItem: HomeRow,
                    newItem: HomeRow,
                ): Boolean =
                    if (oldItem is HomeRow.Slider && newItem is HomeRow.Slider) {
                        oldItem.section.id == newItem.section.id
                    } else {
                        oldItem == newItem
                    }

                override fun areContentsTheSame(
                    oldItem: HomeRow,
                    newItem: HomeRow,
                ): Boolean = oldItem == newItem
            }
    }
}
