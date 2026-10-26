package com.freelibrary.app.ui.reader

import com.freelibrary.app.domain.model.ReaderPosition

/** The arithmetic of a chapter laid out as pages side by side, each exactly as wide as the screen. */
internal object PageMath {
    /** The share of the screen at each side where a tap turns the page. */
    const val EDGE_FRACTION = 0.25f

    private const val MAX = ReaderPosition.MAX_PERMILLE

    /** How many pages [contentWidth] holds when each is [pageWidth] wide. At least 1, also before anything is laid out. */
    fun pageCount(
        contentWidth: Int,
        pageWidth: Int,
    ): Int {
        if (pageWidth <= 0 || contentWidth <= 0) return 1
        return ((contentWidth + pageWidth / 2) / pageWidth).coerceAtLeast(1)
    }

    /** The page showing when the view is scrolled [scrollX] pixels sideways. */
    fun pageAt(
        scrollX: Int,
        pageWidth: Int,
        pageCount: Int,
    ): Int {
        if (pageWidth <= 0) return 0
        return ((scrollX + pageWidth / 2) / pageWidth).coerceIn(0, pageCount - 1)
    }

    /** The sideways scroll that shows [page] exactly. */
    fun scrollXOf(
        page: Int,
        pageWidth: Int,
        pageCount: Int,
    ): Int = page.coerceIn(0, pageCount - 1) * pageWidth

    /** How far through the chapter [page] is, in thousandths; 0 for a chapter that is one page or not laid out yet. */
    fun permilleOf(
        page: Int,
        pageCount: Int,
    ): Int {
        if (pageCount <= 1) return 0
        return (page.toLong() * MAX / (pageCount - 1)).toInt().coerceIn(0, MAX)
    }

    /** The page that [permille] thousandths through the chapter falls on. */
    fun pageOf(
        permille: Int,
        pageCount: Int,
    ): Int {
        if (pageCount <= 1) return 0
        return ((permille.coerceIn(0, MAX).toLong() * (pageCount - 1) + MAX / 2) / MAX).toInt()
    }

    /**
     * Whether a swipe that moved [dx] pixels sideways turns forward (true) or back (false); null if it
     * was too short. In a right to left book ([rightToLeft]) a swipe to the right turns forward.
     */
    fun swipeForward(
        dx: Float,
        threshold: Float,
        rightToLeft: Boolean = false,
    ): Boolean? {
        val towardsRight =
            when {
                dx <= -threshold -> false
                dx >= threshold -> true
                else -> return null
            }
        return towardsRight == rightToLeft
    }

    /**
     * Whether a tap at [x] on a view [width] wide turns forward (true) or back (false); null in the
     * middle. In a right to left book ([rightToLeft]) the left edge turns forward.
     */
    fun tapForward(
        x: Float,
        width: Int,
        rightToLeft: Boolean = false,
    ): Boolean? {
        val atRightEdge =
            when {
                x < width * EDGE_FRACTION -> false
                x > width * (1 - EDGE_FRACTION) -> true
                else -> return null
            }
        return atRightEdge != rightToLeft
    }
}

/** The page height in the units the web view lays a page out in: the view's pixels over the screen's density, less one so it never overflows. */
internal fun cssPageHeight(
    heightPixels: Int,
    density: Float,
): Int = if (heightPixels <= 0 || density <= 0f) 0 else (heightPixels / density).toInt() - 1
