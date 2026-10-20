package com.freelibrary.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import com.freelibrary.app.domain.model.ReaderPosition

/** Navigation argument name of the place a reader should open at (it must match nav_graph.xml). */
const val ARG_START_POSITION = "startPosition"

/** Navigation argument name that is true when the bookmark list was opened from a reader (it must match nav_graph.xml). */
const val ARG_FROM_READER = "fromReader"

/** The place the reader was asked to open at, or null when it was not asked to, or the text is not a place. */
fun SavedStateHandle.startPosition(): ReaderPosition? = get<String>(ARG_START_POSITION)?.let { ReaderPosition.decode(it) }
