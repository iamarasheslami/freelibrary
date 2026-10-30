package com.freelibrary.app.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freelibrary.app.R

/** The row under the last slider of Explore while the next categories are loading: a spinner and a line of text. */
class ExploreLoadStateAdapter : LoadStateAdapter<ExploreLoadStateAdapter.ViewHolder>() {
    class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view)

    override fun displayLoadStateAsItem(loadState: LoadState): Boolean = loadState is LoadState.Loading

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState,
    ): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_explore_loading, parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        loadState: LoadState,
    ) = Unit
}
