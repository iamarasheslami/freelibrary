package com.freelibrary.app.ui.theme

/**
 * True black exists only inside dark mode. [darkIsShowing] is what the screen actually uses, so it
 * covers every way of getting there: the reader picked Dark, or the theme follows a dark phone.
 */
internal fun shouldUseTrueBlack(
    darkIsShowing: Boolean,
    trueBlack: Boolean,
): Boolean = darkIsShowing && trueBlack
