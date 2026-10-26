package com.freelibrary.app.ui.filter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freelibrary.app.R
import com.freelibrary.app.databinding.DialogFilterPickerBinding
import com.freelibrary.app.databinding.ItemFilterPickerOptionBinding
import com.freelibrary.app.domain.model.BookFilter
import com.freelibrary.app.domain.model.FilterOption
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/** One line of the picker: the choice, how to show it, and whether it is chosen now. */
data class PickerRow(
    val value: String,
    val label: String,
    val bookCount: Int,
    val selected: Boolean,
)

/**
 * The lines the picker shows for what the reader typed: every option whose name contains the text,
 * ignoring case and surrounding spaces, in the order given. Nothing typed shows everything.
 */
internal fun filterPickerRows(
    options: List<FilterOption>,
    query: String,
    label: (FilterOption) -> String,
    selected: Set<String>,
): List<PickerRow> {
    val needle = query.trim().lowercase(Locale.ROOT)
    return options
        .map { PickerRow(it.value, label(it), it.bookCount, it.value in selected) }
        .filter { needle.isEmpty() || it.label.lowercase(Locale.ROOT).contains(needle) }
}

/**
 * A drop-down of choices with a search box at the top, shown as a dialog. One long list (hundreds
 * of genres, dozens of languages) is searched instead of scrolled. A single-choice picker closes
 * on a tap; a multiple-choice one stays open until Done.
 */
internal class FilterPicker(
    private val context: Context,
    @StringRes private val title: Int,
    private val options: List<FilterOption>,
    private val label: (FilterOption) -> String,
    private val multiple: Boolean,
    private val onToggle: (String) -> Unit,
    private val onClear: () -> Unit,
) {
    /** Shows the picker. [draft] is the filter being edited and [selectedOf] reads this picker's choices out of it. */
    fun show(
        scope: CoroutineScope,
        draft: StateFlow<BookFilter>,
        selectedOf: (BookFilter) -> Set<String>,
    ) {
        val binding = DialogFilterPickerBinding.inflate(LayoutInflater.from(context))
        var query = ""
        lateinit var dialog: AlertDialog
        val adapter =
            PickerAdapter { value ->
                onToggle(value)
                if (!multiple) dialog.dismiss()
            }

        fun refresh() {
            val rows = filterPickerRows(options, query, label, selectedOf(draft.value))
            adapter.submitList(rows)
            binding.filterPickerNoMatches.isVisible = rows.isEmpty()
        }
        binding.filterPickerList.adapter = adapter
        binding.filterPickerSearch.doAfterTextChanged {
            query = it?.toString().orEmpty()
            refresh()
        }
        val job = scope.launch { draft.collect { refresh() } }

        dialog =
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(binding.root)
                .setPositiveButton(R.string.filter_picker_done, null)
                .setNeutralButton(R.string.filter_picker_clear) { _, _ -> onClear() }
                .create()
        dialog.setOnDismissListener { job.cancel() }
        dialog.show()
    }

    private class PickerAdapter(
        private val onClick: (String) -> Unit,
    ) : ListAdapter<PickerRow, PickerAdapter.Holder>(DIFF) {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ) = Holder(ItemFilterPickerOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(
            holder: Holder,
            position: Int,
        ) = holder.bind(getItem(position), onClick)

        class Holder(
            private val binding: ItemFilterPickerOptionBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                row: PickerRow,
                onClick: (String) -> Unit,
            ) {
                binding.filterPickerLabel.text = row.label
                binding.filterPickerCount.text = row.bookCount.toString()
                binding.filterPickerCheck.visibility = if (row.selected) View.VISIBLE else View.INVISIBLE
                binding.root.isSelected = row.selected
                binding.root.setOnClickListener { onClick(row.value) }
            }
        }

        companion object {
            val DIFF =
                object : DiffUtil.ItemCallback<PickerRow>() {
                    override fun areItemsTheSame(
                        oldItem: PickerRow,
                        newItem: PickerRow,
                    ) = oldItem.value == newItem.value

                    override fun areContentsTheSame(
                        oldItem: PickerRow,
                        newItem: PickerRow,
                    ) = oldItem == newItem
                }
        }
    }
}
