package com.freelibrary.app.ui.reader

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import com.freelibrary.app.R
import com.freelibrary.app.data.reader.BookSearch
import com.freelibrary.app.data.reader.SearchHit
import com.freelibrary.app.data.reader.SearchResults
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Shows what a search found as a list: a title with the number of results, and for every place its chapter
 * or part ([labelOf]) in green and a line of the text with the words marked. [onPick] is called with the hit
 * the reader taps.
 */
fun showSearchResults(
    context: Context,
    query: String,
    results: SearchResults,
    labelOf: (SearchHit) -> String,
    onPick: (SearchHit) -> Unit,
): AlertDialog {
    val count = results.hits.size
    val title =
        if (results.capped) {
            context.getString(R.string.reader_search_results_capped, count, query)
        } else {
            context.resources.getQuantityString(R.plurals.reader_search_results, count, count, query)
        }
    val titleView = LayoutInflater.from(context).inflate(R.layout.dialog_search_title, null) as TextView
    titleView.text = title
    return MaterialAlertDialogBuilder(context)
        .setCustomTitle(titleView)
        .setAdapter(SearchHitAdapter(context, results.hits, labelOf)) { _, which -> onPick(results.hits[which]) }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

/** One line of the results list. The words found are bold, on the container colour of the theme. */
private class SearchHitAdapter(
    context: Context,
    private val hits: List<SearchHit>,
    private val labelOf: (SearchHit) -> String,
) : ArrayAdapter<SearchHit>(context, 0, hits) {
    private val markColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, Color.TRANSPARENT)
    private val markTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.BLACK)

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_search_hit, parent, false)
        val hit = hits[position]
        view.findViewById<TextView>(R.id.searchHitPlace).text = labelOf(hit)
        view.findViewById<TextView>(R.id.searchHitText).text =
            SpannableStringBuilder(hit.before).apply {
                val start = length
                append(hit.match)
                setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(BackgroundColorSpan(markColor), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(markTextColor), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                append(hit.after)
            }
        return view
    }
}

/**
 * Wires the search box in a reader's toolbar: a search of at least [BookSearch.MIN_QUERY_CHARS] characters
 * is passed, cleaned of extra white space, to [onSearch].
 */
fun SearchView.onBookSearch(onSearch: (String) -> Unit) {
    queryHint = context.getString(R.string.reader_search_hint)
    maxWidth = Int.MAX_VALUE
    setOnQueryTextListener(
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val cleaned = BookSearch.clean(query)
                if (cleaned.length >= BookSearch.MIN_QUERY_CHARS) {
                    clearFocus()
                    onSearch(cleaned)
                }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean = false
        },
    )
}
