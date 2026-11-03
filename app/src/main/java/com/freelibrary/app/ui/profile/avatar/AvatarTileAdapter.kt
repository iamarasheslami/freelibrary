package com.freelibrary.app.ui.profile.avatar

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freelibrary.app.R
import com.freelibrary.app.databinding.ItemAvatarTileBinding
import com.freelibrary.app.domain.model.AvatarSlot

/** The creator's grid: a heading for each group of choices and a round tile for each choice. */
class AvatarTileAdapter(
    private val onPick: (AvatarSlot, Int) -> Unit,
) : ListAdapter<AvatarRow, RecyclerView.ViewHolder>(DIFF) {
    override fun getItemViewType(position: Int): Int = if (getItem(position) is AvatarRow.Heading) TYPE_HEADING else TYPE_TILE

    /** How many grid columns a row takes: a heading takes the whole line. */
    fun spanOf(
        position: Int,
        spanCount: Int,
    ): Int = if (getItem(position) is AvatarRow.Heading) spanCount else 1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADING) {
            HeadingHolder(inflater.inflate(R.layout.item_avatar_heading, parent, false) as TextView)
        } else {
            TileHolder(ItemAvatarTileBinding.inflate(inflater, parent, false), onPick)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (val row = getItem(position)) {
            is AvatarRow.Heading -> (holder as HeadingHolder).heading.setText(row.title)
            is AvatarRow.Style -> (holder as TileHolder).bindStyle(row)
            is AvatarRow.Colour -> (holder as TileHolder).bindColour(row)
        }
    }

    private class HeadingHolder(
        val heading: TextView,
    ) : RecyclerView.ViewHolder(heading) {
        init {
            ViewCompat.setAccessibilityHeading(heading, true)
        }
    }

    private class TileHolder(
        private val binding: ItemAvatarTileBinding,
        private val onPick: (AvatarSlot, Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindStyle(row: AvatarRow.Style) {
            binding.avatarTile.showDesign(row.preview, row.slot)
            show(row.title, row.choice, row.selected) { onPick(row.slot, row.choice) }
        }

        fun bindColour(row: AvatarRow.Colour) {
            binding.avatarTile.showSwatch(row.rgb)
            show(row.title, row.choice, row.selected) { onPick(row.slot, row.choice) }
        }

        private fun show(
            title: Int,
            choice: Int,
            selected: Boolean,
            onClick: () -> Unit,
        ) {
            val resources = itemView.resources
            binding.root.isSelected = selected
            binding.root.contentDescription = resources.getString(R.string.avatar_tile_description, resources.getString(title), choice + 1)
            ViewCompat.setStateDescription(binding.root, if (selected) resources.getString(R.string.avatar_tile_selected) else null)
            binding.root.setOnClickListener { onClick() }
        }
    }

    companion object {
        private const val TYPE_HEADING = 0
        private const val TYPE_TILE = 1

        val DIFF =
            object : DiffUtil.ItemCallback<AvatarRow>() {
                override fun areItemsTheSame(
                    oldItem: AvatarRow,
                    newItem: AvatarRow,
                ): Boolean =
                    when {
                        oldItem is AvatarRow.Heading && newItem is AvatarRow.Heading -> oldItem.title == newItem.title
                        oldItem is AvatarRow.Style && newItem is AvatarRow.Style ->
                            oldItem.slot == newItem.slot && oldItem.choice == newItem.choice
                        oldItem is AvatarRow.Colour && newItem is AvatarRow.Colour ->
                            oldItem.slot == newItem.slot && oldItem.choice == newItem.choice
                        else -> false
                    }

                override fun areContentsTheSame(
                    oldItem: AvatarRow,
                    newItem: AvatarRow,
                ): Boolean = oldItem == newItem
            }
    }
}
