package com.freelibrary.app.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.databinding.ItemProfileHeaderBinding
import com.freelibrary.app.databinding.ItemProfileReadingBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.ProfileSection
import com.freelibrary.app.domain.model.ReadingSummary
import com.freelibrary.app.ui.booklist.BookSliderViewHolder
import com.freelibrary.app.ui.cover.CoverTypefaces
import java.text.NumberFormat

/** One row of the Profile list: the reader's picture and name first, then one slider per shelf. */
sealed interface ProfileRow {
    data class Header(
        val name: String?,
        val avatarVersion: Long?,
    ) : ProfileRow

    data class Reading(
        val summary: ReadingSummary,
    ) : ProfileRow

    data class Slider(
        val section: ProfileSection,
    ) : ProfileRow
}

/** The header, the reading card, then a slider for each shelf that has books; a shelf with no books is not shown at all. */
internal fun profileRows(state: ProfileState): List<ProfileRow> =
    listOf(
        ProfileRow.Header(state.name, state.avatarVersion),
        ProfileRow.Reading(state.reading),
    ) + state.sections.filter { it.books.isNotEmpty() }.map { ProfileRow.Slider(it) }

/** The header, then the shelves. Every slider shares one pool, so cards are reused across rows. */
class ProfileAdapter(
    private val typefaces: CoverTypefaces,
    private val imageLoader: ImageLoader,
    private val avatarStore: AvatarStore,
    private val onBookClick: (BookCard) -> Unit,
    private val onViewMore: (ProfileSection) -> Unit,
    private val onAvatarClick: () -> Unit,
    private val onGoalClick: () -> Unit,
) : ListAdapter<ProfileRow, RecyclerView.ViewHolder>(DIFF) {
    private val viewPool = RecyclerView.RecycledViewPool()

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ProfileRow.Header -> TYPE_HEADER
            is ProfileRow.Reading -> TYPE_READING
            is ProfileRow.Slider -> TYPE_SLIDER
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> HeaderViewHolder.create(parent, imageLoader, avatarStore, onAvatarClick)
            TYPE_READING -> ReadingViewHolder.create(parent, onGoalClick)
            else -> BookSliderViewHolder.create(parent, typefaces, imageLoader, viewPool, onBookClick = onBookClick)
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val row = getItem(position)
        if (holder is HeaderViewHolder && row is ProfileRow.Header) {
            holder.bind(row)
        } else if (holder is ReadingViewHolder && row is ProfileRow.Reading) {
            holder.bind(row.summary)
        } else if (holder is BookSliderViewHolder && row is ProfileRow.Slider) {
            val section = row.section
            val context = holder.itemView.context
            holder.bind(
                heading = context.getString(section.titleRes),
                books = section.books,
                hasMore = section.hasMore,
                onViewMore = { onViewMore(section) },
            )
        }
    }

    private class HeaderViewHolder(
        private val binding: ItemProfileHeaderBinding,
        private val imageLoader: ImageLoader,
        private val avatarStore: AvatarStore,
        onAvatarClick: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.profileAvatarCard.setOnClickListener { onAvatarClick() }
        }

        fun bind(row: ProfileRow.Header) {
            binding.profileName.text = row.name.orEmpty()
            binding.profileName.isVisible = row.name != null

            val file = if (row.avatarVersion != null) avatarStore.file() else null
            if (file == null) {
                binding.profileAvatar.showAvatarPlaceholder()
            } else {
                binding.profileAvatar.showAvatarPicture(file, "avatar-${row.avatarVersion}", imageLoader)
            }
        }

        companion object {
            fun create(
                parent: ViewGroup,
                imageLoader: ImageLoader,
                avatarStore: AvatarStore,
                onAvatarClick: () -> Unit,
            ) = HeaderViewHolder(
                ItemProfileHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                imageLoader,
                avatarStore,
                onAvatarClick,
            )
        }
    }

    /** The reader's year: the goal ring with the books finished this year, and the month's count. */
    private class ReadingViewHolder(
        private val binding: ItemProfileReadingBinding,
        onGoalClick: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.readingCard.setOnClickListener { onGoalClick() }
        }

        fun bind(summary: ReadingSummary) {
            val resources = itemView.resources
            val goal = summary.goal
            binding.readingRing.setProgress(summary.progress)
            binding.readingCount.text = NumberFormat.getInstance().format(summary.finishedThisYear)
            binding.readingTitle.text =
                resources.getString(if (goal == null) R.string.profile_reading_title else R.string.profile_goal_title, summary.year)
            binding.readingGoal.text =
                when {
                    goal == null -> resources.getString(R.string.profile_goal_set)
                    summary.goalReached -> resources.getQuantityString(R.plurals.profile_goal_reached, goal, goal)
                    else -> resources.getQuantityString(R.plurals.profile_goal_progress, goal, summary.finishedThisYear, goal)
                }
            binding.readingMonth.text =
                resources.getQuantityString(R.plurals.profile_finished_this_month, summary.finishedThisMonth, summary.finishedThisMonth)
            val current = summary.streak.current
            binding.streakCount.text = NumberFormat.getInstance().format(current)
            binding.streakRing.setProgress(if (summary.streak.readToday) 1f else 0f)
            val streak = resources.getQuantityString(R.plurals.profile_streak, current, current)
            val spoken = listOf(binding.readingTitle.text, binding.readingGoal.text, binding.readingMonth.text, streak)
            binding.readingCard.contentDescription =
                resources.getString(R.string.profile_reading_description, spoken.filter { it.isNotEmpty() }.joinToString(". "))
        }

        companion object {
            fun create(
                parent: ViewGroup,
                onGoalClick: () -> Unit,
            ) = ReadingViewHolder(
                ItemProfileReadingBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onGoalClick,
            )
        }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_READING = 2
        const val TYPE_SLIDER = 1

        /** A slider is the same item while its section id is the same; there is only ever one header and one reading card. */
        val DIFF =
            object : DiffUtil.ItemCallback<ProfileRow>() {
                override fun areItemsTheSame(
                    oldItem: ProfileRow,
                    newItem: ProfileRow,
                ): Boolean =
                    when {
                        oldItem is ProfileRow.Header && newItem is ProfileRow.Header -> true
                        oldItem is ProfileRow.Reading && newItem is ProfileRow.Reading -> true
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
