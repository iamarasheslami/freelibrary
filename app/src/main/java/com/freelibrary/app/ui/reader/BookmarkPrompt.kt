package com.freelibrary.app.ui.reader

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.freelibrary.app.R
import com.freelibrary.app.data.repository.BookmarkRepository
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.bookpage.ARG_BOOK_ID
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Asks the reader for a note (it may be left empty) and bookmarks [position] in the book this
 * screen shows. The book is the one in the screen's arguments.
 */
fun Fragment.promptForBookmark(
    repository: BookmarkRepository,
    position: ReaderPosition,
) {
    val arguments = arguments ?: return
    if (!arguments.containsKey(ARG_BOOK_ID)) return
    val bookId = arguments.getLong(ARG_BOOK_ID)
    val content = layoutInflater.inflate(R.layout.dialog_add_bookmark, null)
    val input = content.findViewById<TextInputEditText>(R.id.bookmarkNoteInput)
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.bookmark_add_title)
        .setView(content)
        .setPositiveButton(R.string.bookmark_add_button) { _, _ ->
            val note = input.text?.toString()
            lifecycleScope.launch {
                val saved = repository.add(bookId, position, note)
                view?.let {
                    Snackbar.make(
                        it,
                        if (saved) R.string.bookmark_added else R.string.bookmark_not_saved,
                        Snackbar.LENGTH_SHORT,
                    ).show()
                }
            }
        }.setNegativeButton(android.R.string.cancel, null)
        .show()
}
