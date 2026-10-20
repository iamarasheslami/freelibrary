package com.freelibrary.app.ui.bookmarks

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freelibrary.app.R
import com.freelibrary.app.databinding.ItemBookmarkBinding
import com.freelibrary.app.domain.model.ReaderPosition
import java.text.DateFormat
import java.util.Date

/** The rows of the bookmark list: the note (or the place when there is no note), then the place and the date. */
class BookmarkAdapter(
    private val onOpen: (BookmarkItem) -> Unit,
    private val onDelete: (BookmarkItem) -> Unit,
) : ListAdapter<BookmarkItem, BookmarkAdapter.Holder>(Differ) {
    class Holder(
        val binding: ItemBookmarkBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder = Holder(ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        holder: Holder,
        position: Int,
    ) {
        val item = getItem(position)
        val context = holder.itemView.context
        val place = placeLabel(context, item)
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.bookmark.createdAt))
        val note = item.bookmark.note
        holder.binding.bookmarkTitle.text = note ?: place
        holder.binding.bookmarkDetails.text = if (note != null) context.getString(R.string.bookmark_details, place, date) else date
        holder.itemView.setOnClickListener { onOpen(item) }
        holder.binding.bookmarkDelete.setOnClickListener { onDelete(item) }
    }

    private fun placeLabel(
        context: Context,
        item: BookmarkItem,
    ): String =
        when (val position = item.bookmark.position) {
            is ReaderPosition.Epub -> context.getString(R.string.bookmark_place_epub, position.chapter + 1, position.permille / 10)
            is ReaderPosition.Pdf -> context.getString(R.string.bookmark_place_pdf, position.page + 1)
            is ReaderPosition.Text ->
                item.textPercent?.let { context.getString(R.string.bookmark_place_text_percent, it) }
                    ?: context.getString(R.string.bookmark_place_text)
        }

    private object Differ : DiffUtil.ItemCallback<BookmarkItem>() {
        override fun areItemsTheSame(
            oldItem: BookmarkItem,
            newItem: BookmarkItem,
        ): Boolean = oldItem.bookmark.id == newItem.bookmark.id

        override fun areContentsTheSame(
            oldItem: BookmarkItem,
            newItem: BookmarkItem,
        ): Boolean = oldItem == newItem
    }
}
