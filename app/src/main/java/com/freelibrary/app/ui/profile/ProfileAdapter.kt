package com.freelibrary.app.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.dispose
import coil.load
import com.freelibrary.app.R
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.databinding.ItemProfileHeaderBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.ProfileSection
import com.freelibrary.app.ui.booklist.BookSliderViewHolder
import com.freelibrary.app.ui.cover.CoverTypefaces

/** One row of the Profile list: the reader's picture and name first, then one slider per shelf. */
sealed interface ProfileRow {
    data class Header(
        val name: String?,
        val avatarVersion: Long?,
        val bookmarks: Int,
        val highlights: Int,
    ) : ProfileRow

    data class Slider(
        val section: ProfileSection,
    ) : ProfileRow
}

internal fun profileRows(state: ProfileState): List<ProfileRow> =
    listOf(ProfileRow.Header(state.name, state.avatarVersion, state.counts.bookmarks, state.counts.highlights)) +
        state.sections.map { ProfileRow.Slider(it) }

/** The header, then the shelves. Every slider shares one pool, so cards are reused across rows. */
class ProfileAdapter(
    private val typefaces: CoverTypefaces,
    private val imageLoader: ImageLoader,
    private val avatarStore: AvatarStore,
    private val onBookClick: (BookCard) -> Unit,
    private val onViewMore: (ProfileSection) -> Unit,
) : ListAdapter<ProfileRow, RecyclerView.ViewHolder>(DIFF) {
    private val viewPool = RecyclerView.RecycledViewPool()

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ProfileRow.Header -> TYPE_HEADER
            is ProfileRow.Slider -> TYPE_SLIDER
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> HeaderViewHolder.create(parent, imageLoader, avatarStore)
            else -> BookSliderViewHolder.create(parent, typefaces, imageLoader, viewPool, onBookClick)
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val row = getItem(position)
        if (holder is HeaderViewHolder && row is ProfileRow.Header) {
            holder.bind(row)
        } else if (holder is BookSliderViewHolder && row is ProfileRow.Slider) {
            val section = row.section
            val context = holder.itemView.context
            holder.bind(
                heading = context.getString(section.titleRes),
                books = section.books,
                hasMore = section.hasMore,
                onViewMore = { onViewMore(section) },
                emptyMessage = context.getString(section.emptyRes),
            )
        }
    }

    private class HeaderViewHolder(
        private val binding: ItemProfileHeaderBinding,
        private val imageLoader: ImageLoader,
        private val avatarStore: AvatarStore,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ProfileRow.Header) {
            binding.profileName.text = row.name.orEmpty()
            binding.profileName.isVisible = row.name != null

            val resources = itemView.resources
            binding.profileCounts.text =
                resources.getString(
                    R.string.profile_counts,
                    resources.getQuantityString(R.plurals.profile_bookmarks, row.bookmarks, row.bookmarks),
                    resources.getQuantityString(R.plurals.profile_highlights, row.highlights, row.highlights),
                )
            showAvatar(row.avatarVersion)
        }

        /** The saved picture, or the gray reader placeholder. The version keeps an old picture from showing after a change. */
        private fun showAvatar(version: Long?) {
            val file = if (version != null) avatarStore.file() else null
            if (file == null) {
                binding.profileAvatar.dispose()
                binding.profileAvatar.setBackgroundResource(R.drawable.bg_avatar_placeholder)
                binding.profileAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            } else {
                binding.profileAvatar.background = null
                binding.profileAvatar.load(file, imageLoader) {
                    memoryCacheKey("avatar-$version")
                    crossfade(false)
                }
            }
        }

        companion object {
            fun create(
                parent: ViewGroup,
                imageLoader: ImageLoader,
                avatarStore: AvatarStore,
            ) = HeaderViewHolder(
                ItemProfileHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                imageLoader,
                avatarStore,
            )
        }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SLIDER = 1

        /** A slider is the same item while its section id is the same; there is only ever one header. */
        val DIFF =
            object : DiffUtil.ItemCallback<ProfileRow>() {
                override fun areItemsTheSame(
                    oldItem: ProfileRow,
                    newItem: ProfileRow,
                ): Boolean =
                    when {
                        oldItem is ProfileRow.Header && newItem is ProfileRow.Header -> true
                        oldItem is ProfileRow.Slider && newItem is ProfileRow.Slider -> oldItem.section.id == newItem.section.id
                        else -> false
                    }

                override fun areContentsTheSame(
                    oldItem: ProfileRow,
                    newItem: ProfileRow,
                ): Boolean = oldItem == newItem
            }
    }
}
