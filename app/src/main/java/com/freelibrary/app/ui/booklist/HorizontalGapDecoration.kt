package com.freelibrary.app.ui.booklist

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Puts a fixed gap between the items of a horizontal list, before every item except the first,
 * so the list needs no margin on its cards and no trailing space after the last one. The gap
 * flips with the layout direction, so it is on the correct side in right-to-left languages.
 */
class HorizontalGapDecoration(
    private val gapPx: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val rightToLeft = parent.layoutDirection == View.LAYOUT_DIRECTION_RTL
        applyGap(outRect, parent.getChildAdapterPosition(view), gapPx, rightToLeft)
    }
}

/** Sets the gap on the side facing the previous item, and none for the first item or an unknown position. */
internal fun applyGap(
    outRect: Rect,
    position: Int,
    gapPx: Int,
    rightToLeft: Boolean,
) {
    if (position <= 0) return
    if (rightToLeft) outRect.right = gapPx else outRect.left = gapPx
}
