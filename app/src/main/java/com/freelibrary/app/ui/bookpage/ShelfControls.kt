package com.freelibrary.app.ui.bookpage

import com.freelibrary.app.data.local.entity.ShelfState

/** What a button on a book's page does to the reader's shelves. */
enum class ShelfAction {
    SAVE,
    MARK_FINISHED,
    REMOVE,
}

/** The buttons a book's page offers for its shelf state: a main one, if any, and a quieter one, if any. */
data class ShelfControls(
    val primary: ShelfAction?,
    val secondary: ShelfAction?,
)

/** [state] is null for a book on no shelf. */
fun shelfControlsFor(state: ShelfState?): ShelfControls =
    when (state) {
        null -> ShelfControls(primary = ShelfAction.SAVE, secondary = null)
        ShelfState.WANT_TO_READ -> ShelfControls(primary = null, secondary = ShelfAction.REMOVE)
        ShelfState.READING -> ShelfControls(primary = ShelfAction.MARK_FINISHED, secondary = ShelfAction.REMOVE)
        ShelfState.FINISHED -> ShelfControls(primary = null, secondary = ShelfAction.REMOVE)
    }
