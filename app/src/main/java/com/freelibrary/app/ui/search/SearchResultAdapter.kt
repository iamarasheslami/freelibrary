package com.freelibrary.app.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freelibrary.app.data.repository.SearchResult
import com.freelibrary.app.databinding.ItemSearchResultBinding

/** The authors of a result on one line, in credit order. Names are stored as "Last, First", so the separator is a semicolon. */
internal fun SearchResult.authorLine(): String = authors.joinToString("; ")

/** One search hit: the title and, when the catalog has them, the authors. */
class SearchResultViewHolder(
    private val binding: ItemSearchResultBinding,
    private val onClick: (SearchResult) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(result: SearchResult) {
        val authors = result.authorLine()
        binding.searchResultTitle.text = result.title
        binding.searchResultAuthors.text = authors
        binding.searchResultAuthors.isVisible = authors.isNotEmpty()
        binding.root.setOnClickListener { onClick(result) }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onClick: (SearchResult) -> Unit,
        ) = SearchResultViewHolder(
            ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onClick,
        )
    }
}

class SearchResultAdapter(
    private val onClick: (SearchResult) -> Unit,
) : ListAdapter<SearchResult, SearchResultViewHolder>(DIFF) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = SearchResultViewHolder.create(parent, onClick)

    override fun onBindViewHolder(
        holder: SearchResultViewHolder,
        position: Int,
    ) = holder.bind(getItem(position))

    companion object {
        val DIFF =
            object : DiffUtil.ItemCallback<SearchResult>() {
                override fun areItemsTheSame(
                    oldItem: SearchResult,
                    newItem: SearchResult,
                ): Boolean = oldItem.bookId == newItem.bookId

                override fun areContentsTheSame(
                    oldItem: SearchResult,
                    newItem: SearchResult,
                ): Boolean = oldItem == newItem
            }
    }
}
