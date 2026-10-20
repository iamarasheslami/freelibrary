package com.freelibrary.app.ui.reader

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.ui.bookpage.ARG_BOOK_ID

/** Opens the bookmark list of the book this screen shows. Choosing a bookmark there replaces this reader. */
fun Fragment.openBookmarkList() {
    val arguments = arguments ?: return
    if (!arguments.containsKey(ARG_BOOK_ID)) return
    findNavController().navigate(
        R.id.action_global_bookmarksFragment,
        bundleOf(ARG_BOOK_ID to arguments.getLong(ARG_BOOK_ID), ARG_FROM_READER to true),
    )
}
