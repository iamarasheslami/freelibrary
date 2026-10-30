package com.freelibrary.app.ui.reader

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.content.getSystemService

private const val FADE_MS = 150L

/**
 * The reader's top and bottom bars. A reader starts with the bars showing; a tap on the page hides
 * them, so only the book is left, and the next tap shows them again. The bars keep their space when
 * hidden, so the page never changes size and the text never moves. With a screen reader on, the
 * bars stay: a tap on the page is not available there.
 */
class ReaderChrome(
    private val bars: List<View>,
    private val alwaysShown: Boolean,
) {
    /** True while the bars show. */
    var shown: Boolean = true
        private set

    init {
        setShown(true, animate = false)
    }

    fun toggle() = setShown(!shown)

    fun show() = setShown(true)

    fun setShown(
        show: Boolean,
        animate: Boolean = true,
    ) {
        shown = show || alwaysShown
        for (bar in bars) {
            bar.animate().cancel()
            if (shown) {
                bar.visibility = View.VISIBLE
                if (animate) bar.animate().alpha(1f).setDuration(FADE_MS).start() else bar.alpha = 1f
            } else if (animate) {
                bar.animate().alpha(0f).setDuration(FADE_MS).withEndAction { if (!shown) bar.visibility = View.INVISIBLE }.start()
            } else {
                bar.alpha = 0f
                bar.visibility = View.INVISIBLE
            }
        }
    }

    companion object {
        /** True when a screen reader that explores by touch is on. */
        fun screenReaderOn(context: Context): Boolean = context.getSystemService<AccessibilityManager>()?.isTouchExplorationEnabled == true
    }
}
