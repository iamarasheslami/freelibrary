package com.freelibrary.app.ui.reader

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.webkit.WebView
import com.freelibrary.app.domain.model.ReaderPosition
import kotlin.math.abs

private const val PAGE_TURN_MS = 180L
private const val SWIPE_DP = 48f

/**
 * A web view that knows how far through its chapter it is, in thousandths, and can put itself back
 * at such a place. In page view the chapter is laid out as pages side by side, each as wide as the
 * view: a swipe sideways or a tap near an edge asks for another page through [onTurnPage], and the
 * view never scrolls by itself, not even up or down. In scroll view it is an ordinary vertical scroll.
 */
class ReaderWebView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.webViewStyle,
    ) : WebView(context, attrs, defStyleAttr) {
        /** True while the chapter is laid out as pages side by side. */
        var paged: Boolean = false

        /** True for a right to left book: a swipe to the right and a tap at the left edge turn forward. */
        var rightToLeft: Boolean = false

        /** Asked for the next (true) or the previous (false) page, by a swipe or a tap at an edge. */
        var onTurnPage: ((forward: Boolean) -> Unit)? = null

        /** Called for a tap that is not at an edge of a page: the reader shows or hides its bars. */
        var onTap: (() -> Unit)? = null

        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val swipeThreshold = SWIPE_DP * resources.displayMetrics.density
        private var downX = 0f
        private var downY = 0f
        private var swiping = false
        private var movedAway = false
        private var animator: ObjectAnimator? = null

        /** How many pages the chapter is, in page view; 1 while it is not laid out yet. */
        fun pageCount(): Int = PageMath.pageCount(computeHorizontalScrollRange(), width)

        /** The page showing, counted from 0, in page view. */
        fun currentPage(): Int = PageMath.pageAt(scrollX, width, pageCount())

        /** How far through the chapter the reader is: 0 at the start, 1000 when the end shows. */
        fun scrolledPermille(): Int =
            if (paged) {
                PageMath.permilleOf(currentPage(), pageCount())
            } else {
                scrolledPermilleVertically()
            }

        /** Moves to [permille] of the chapter. Returns false while the chapter is not laid out enough to scroll. */
        fun scrollToPermille(permille: Int): Boolean {
            if (paged) {
                if (computeHorizontalScrollRange() <= width) return false
                val count = pageCount()
                scrollTo(PageMath.scrollXOf(PageMath.pageOf(permille, count), width, count), 0)
                return true
            }
            val scrollable = scrollableHeight()
            if (scrollable <= 0) return false
            scrollTo(0, (scrollable.toLong() * permille.coerceIn(0, MAX) / MAX).toInt())
            return true
        }

        /** Turns one page in page view. Returns false at the first or last page of the chapter. */
        fun turnPage(forward: Boolean): Boolean {
            val count = pageCount()
            val target = currentPage() + if (forward) 1 else -1
            if (target !in 0 until count) return false
            val x = PageMath.scrollXOf(target, width, count)
            if (rightToLeft) slideIn(x, fromLeft = forward) else animateTo(x)
            return true
        }

        /** Settles on the page showing, after a swipe that turned nothing. */
        fun snapToPage() {
            animateTo(PageMath.scrollXOf(currentPage(), width, pageCount()))
        }

        /**
         * Turning a page in a right to left book: the columns lie left to right underneath, so sliding
         * the scroll would move the page against the finger. Instead the scroll jumps to the new page and
         * the view slides in from the side the finger came from ([fromLeft] for a swipe to the right).
         */
        private fun slideIn(
            x: Int,
            fromLeft: Boolean,
        ) {
            animator?.cancel()
            scrollTo(x, 0)
            val start = if (fromLeft) -width.toFloat() else width.toFloat()
            translationX = start
            animator =
                ObjectAnimator.ofFloat(this, "translationX", start, 0f).apply {
                    duration = PAGE_TURN_MS
                    interpolator = DecelerateInterpolator()
                    start()
                }
        }

        private fun animateTo(x: Int) {
            animator?.cancel()
            translationX = 0f
            animator =
                ObjectAnimator.ofInt(this, "scrollX", scrollX, x).apply {
                    duration = PAGE_TURN_MS
                    interpolator = DecelerateInterpolator()
                    start()
                }
        }

        private fun scrollableHeight(): Int = computeVerticalScrollRange() - computeVerticalScrollExtent()

        private fun scrolledPermilleVertically(): Int {
            val scrollable = scrollableHeight()
            if (scrollable <= 0) return 0
            return (scrollY.toLong() * MAX / scrollable).toInt().coerceIn(0, MAX)
        }

        /**
         * In page view the web view only sees touches that stay put, so a tap on a link still opens
         * it; as soon as a finger moves the web view is told the touch is over and the move is ours.
         */
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!paged) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = event.x - downX
                        val sideways = abs(dx) > abs(event.y - downY) * SIDEWAYS_RATIO
                        val forward = if (sideways) PageMath.swipeForward(dx, swipeThreshold, rightToLeft) else null
                        // In scroll view a swipe to the side asks for the next or the previous chapter.
                        if (forward != null) {
                            onTurnPage?.invoke(forward)
                        } else if (isTap(event)) {
                            onTap?.invoke()
                        }
                    }
                }
                return super.onTouchEvent(event)
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    swiping = false
                    movedAway = false
                    animator?.cancel()
                    translationX = 0f
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!movedAway && (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop)) {
                        movedAway = true
                        cancelTouchInWebView(event)
                    }
                    if (!swiping && isSideways(event)) swiping = true
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (swiping) {
                        swiping = false
                        val forward = PageMath.swipeForward(event.x - downX, swipeThreshold, rightToLeft)
                        if (forward == null) snapToPage() else onTurnPage?.invoke(forward)
                        return true
                    }
                    if (movedAway) return true
                    val edge = if (isTap(event)) PageMath.tapForward(event.x, width, rightToLeft) else null
                    if (edge != null) {
                        cancelTouchInWebView(event)
                        onTurnPage?.invoke(edge)
                        return true
                    }
                    if (isTap(event)) onTap?.invoke()
                }
                MotionEvent.ACTION_CANCEL -> swiping = false
            }
            return super.onTouchEvent(event)
        }

        private fun isSideways(event: MotionEvent): Boolean {
            val dx = abs(event.x - downX)
            return dx > touchSlop && dx > abs(event.y - downY)
        }

        private fun isTap(event: MotionEvent): Boolean =
            abs(event.x - downX) <= touchSlop &&
                abs(event.y - downY) <= touchSlop &&
                event.eventTime - event.downTime < ViewConfiguration.getLongPressTimeout()

        /** Tells the web view its touch ended without a click or a scroll, so it leaves the page alone. */
        private fun cancelTouchInWebView(event: MotionEvent) {
            val cancel = MotionEvent.obtain(event)
            cancel.action = MotionEvent.ACTION_CANCEL
            super.onTouchEvent(cancel)
            cancel.recycle()
        }

        private companion object {
            const val MAX = ReaderPosition.MAX_PERMILLE

            /** How much longer a swipe must be sideways than up or down to count as a swipe to the side. */
            const val SIDEWAYS_RATIO = 2
        }
    }
