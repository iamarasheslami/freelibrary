package com.freelibrary.app.ui.booklist

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** How many cards of [cardWidthPx], with at least [minGapPx] between them, fit in [availableWidthPx]. At least one. */
internal fun gridColumnCount(
    availableWidthPx: Int,
    cardWidthPx: Int,
    minGapPx: Int,
): Int {
    val cell = cardWidthPx + minGapPx
    return if (cell <= 0) 1 else maxOf(1, availableWidthPx / cell)
}

/** The sideways space on each side of a card that centres it in its column (never negative). */
internal fun cellSideInset(
    availableWidthPx: Int,
    columns: Int,
    cardWidthPx: Int,
): Int = if (columns <= 0) 0 else maxOf(0, (availableWidthPx / columns - cardWidthPx) / 2)

/** A grid that picks its number of columns from its own width, so rotation and tablets just work. */
class AutoSpanGridLayoutManager(
    context: Context,
    private val cardWidthPx: Int,
    private val minGapPx: Int,
) : GridLayoutManager(context, 1) {
    override fun onLayoutChildren(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
    ) {
        val available = width - paddingLeft - paddingRight
        if (available > 0) spanCount = gridColumnCount(available, cardWidthPx, minGapPx)
        super.onLayoutChildren(recycler, state)
    }
}

/** Centres each card in its column and leaves [rowGapPx] below it. */
class CenteredCardDecoration(
    private val cardWidthPx: Int,
    private val rowGapPx: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val columns = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 1
        val available = parent.width - parent.paddingLeft - parent.paddingRight
        val inset = cellSideInset(available, columns, cardWidthPx)
        outRect.set(inset, 0, inset, rowGapPx)
    }
}
