package com.freelibrary.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import kotlin.math.max

/**
 * Lays its children out left to right and wraps to a new row when the next child does not fit.
 * Every row is centered, so a short last row sits in the middle instead of at the start. It never
 * scrolls: the rows simply make the view taller.
 */
class CenteredFlowLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : ViewGroup(context, attrs, defStyleAttr) {
        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            val childSpecWidth = MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST)
            val childSpecHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            var rowWidth = 0
            var rowHeight = 0
            var height = 0
            var widest = 0
            for (index in 0 until childCount) {
                val child = getChildAt(index)
                if (child.isGone) continue
                child.measure(childSpecWidth, childSpecHeight)
                if (rowWidth > 0 && rowWidth + child.measuredWidth > available) {
                    height += rowHeight
                    widest = max(widest, rowWidth)
                    rowWidth = 0
                    rowHeight = 0
                }
                rowWidth += child.measuredWidth
                rowHeight = max(rowHeight, child.measuredHeight)
            }
            height += rowHeight
            widest = max(widest, rowWidth)
            setMeasuredDimension(
                resolveSize(widest + paddingLeft + paddingRight, widthMeasureSpec),
                resolveSize(height + paddingTop + paddingBottom, heightMeasureSpec),
            )
        }

        override fun onLayout(
            changed: Boolean,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {
            val available = right - left - paddingLeft - paddingRight
            var index = 0
            var y = paddingTop
            while (index < childCount) {
                val row = rowStartingAt(index, available)
                val rowWidth = row.sumOf { it.measuredWidth }
                var x = paddingLeft + (available - rowWidth) / 2
                val rowHeight = row.maxOf { it.measuredHeight }
                for (child in row) {
                    child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
                    x += child.measuredWidth
                }
                y += rowHeight
                index = indexAfter(row, index)
            }
        }

        /** The visible children that share one row, starting at child [start]. */
        private fun rowStartingAt(
            start: Int,
            available: Int,
        ): List<View> {
            val row = mutableListOf<View>()
            var width = 0
            for (index in start until childCount) {
                val child = getChildAt(index)
                if (child.isGone) {
                    continue
                }
                if (row.isNotEmpty() && width + child.measuredWidth > available) break
                row += child
                width += child.measuredWidth
            }
            return row
        }

        /** The index of the first child after [row], which began at [start]. */
        private fun indexAfter(
            row: List<View>,
            start: Int,
        ): Int {
            if (row.isEmpty()) return childCount
            return indexOfChild(row.last()).coerceAtLeast(start) + 1
        }
    }
