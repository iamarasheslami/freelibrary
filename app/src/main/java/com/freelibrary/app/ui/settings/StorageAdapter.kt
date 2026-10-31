package com.freelibrary.app.ui.settings

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freelibrary.app.R
import com.freelibrary.app.data.download.StoredDownload
import com.freelibrary.app.databinding.ItemStorageDownloadBinding
import com.freelibrary.app.domain.model.BookFileFormat

/** The downloaded books on the Storage page: title, format and size, and a button that deletes the file. */
class StorageAdapter(
    private val onDelete: (StoredDownload) -> Unit,
) : ListAdapter<StoredDownload, StorageAdapter.ViewHolder>(DIFF) {
    class ViewHolder(
        val binding: ItemStorageDownloadBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = ViewHolder(ItemStorageDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val download = getItem(position)
        val context = holder.itemView.context
        holder.binding.storageTitle.text = download.title
        val format = context.getString(formatName(download.format))
        holder.binding.storageDetails.text =
            context.getString(R.string.storage_file_details, format, Formatter.formatShortFileSize(context, download.sizeBytes))
        holder.binding.storageDelete.contentDescription = context.getString(R.string.storage_delete_description, download.title)
        holder.binding.storageDelete.setOnClickListener { onDelete(download) }
    }

    companion object {
        fun formatName(format: BookFileFormat): Int =
            when (format) {
                BookFileFormat.EPUB -> R.string.format_epub
                BookFileFormat.PDF -> R.string.format_pdf
                BookFileFormat.TEXT -> R.string.format_txt
            }

        val DIFF =
            object : DiffUtil.ItemCallback<StoredDownload>() {
                override fun areItemsTheSame(
                    oldItem: StoredDownload,
                    newItem: StoredDownload,
                ): Boolean = oldItem.bookId == newItem.bookId && oldItem.format == newItem.format

                override fun areContentsTheSame(
                    oldItem: StoredDownload,
                    newItem: StoredDownload,
                ): Boolean = oldItem == newItem
            }
    }
}
