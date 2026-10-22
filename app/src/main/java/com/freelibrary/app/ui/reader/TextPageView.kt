package com.freelibrary.app.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import com.freelibrary.app.R
import kotlin.math.abs

private const val LINE_SPACING = 1.35f
private const val PARAGRAPH_GAP = 0.45f
private const val SWIPE_DP = 48f
private const val PAGE_PADDING_DP = 16f
private const val MAX_PERMILLE = 1000

/**
 * Shows one part of a plain text book without a web view: the text is laid out here and drawn
 * straight to the screen, so it needs little memory. In page view it shows one page of whole lines,
 * and a swipe sideways or a tap near an edge asks for another page through [onTurnPage]. In scroll
 * view it is an ordinary vertical scroll. [onMoved] tells when the place shown has changed.
 */
class TextPageView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        /** Asked for the next (true) or the previous (false) page, by a swipe or a tap at an edge. */
        var onTurnPage: ((forward: Boolean) -> Unit)? = null

        /** Called when the page or the scroll position changed. */
        var onMoved: (() -> Unit)? = null

        /** True while the text is cut into pages. */
        var paged: Boolean = true
            private set

        private val sidePadding = resources.getDimensionPixelSize(R.dimen.reader_page_margin)
        private val verticalPadding = (PAGE_PADDING_DP * resources.displayMetrics.density).toInt()
        private val swipeThreshold = SWIPE_DP * resources.displayMetrics.density
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.SERIF }
        private val scroller = OverScroller(context)
        private val scrollDetector = GestureDetector(context, ScrollListener())

        private var content: CharSequence = ""
        private var layout: StaticLayout? = null
        private var pageStarts = intArrayOf(0)
        private var page = 0
        private var wanted = 0
        private var quiet = false
        private var downX = 0f
        private var downY = 0f

        /** Shows [plain], the text of one part, starting [permille] thousandths of the way through it. */
        fun show(
            plain: String,
            paged: Boolean,
            textSizePx: Float,
            textColor: Int,
            backgroundColor: Int,
            permille: Int,
        ) {
            this.paged = paged
            content = withParagraphGaps(plain)
            paint.textSize = textSizePx
            paint.color = textColor
            setBackgroundColor(backgroundColor)
            isVerticalScrollBarEnabled = !paged
            rebuild(permille)
        }

        /** How many pages the part is, in page view. */
        fun pageCount(): Int = pageStarts.size

        /** The page showing, counted from 0, in page view. */
        fun currentPage(): Int = page

        /** How far through the part the reader is: 0 at the start, 1000 when the end shows. */
        fun permille(): Int {
            val l = layout ?: return 0
            if (paged) {
                if (page > 0 && page == pageStarts.lastIndex) return MAX_PERMILLE
                return TextPaging.permilleOf(l.getLineStart(pageStarts[page]), content.length)
            }
            val max = maxScroll()
            if (max > 0 && scrollY >= max) return MAX_PERMILLE
            return TextPaging.permilleOf(l.getLineStart(l.getLineForVertical(scrollY)), content.length)
        }

        /** Turns one page in page view. Returns false at the first or last page of the part. */
        fun turnPage(forward: Boolean): Boolean {
            val target = page + if (forward) 1 else -1
            if (target !in pageStarts.indices) return false
            page = target
            invalidate()
            onMoved?.invoke()
            return true
        }

        private fun withParagraphGaps(plain: String): CharSequence {
            val spanned = SpannableString(plain)
            var at = plain.indexOf("\n\n")
            while (at >= 0) {
                spanned.setSpan(RelativeSizeSpan(PARAGRAPH_GAP), at + 1, at + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                at = plain.indexOf("\n\n", at + 2)
            }
            return spanned
        }

        private fun rebuild(permille: Int) {
            wanted = permille
            quiet = true
            scroller.forceFinished(true)
            scrollTo(0, 0)
            page = 0
            pageStarts = intArrayOf(0)
            layout = null
            if (width > 0 && height > 0) {
                val builder =
                    StaticLayout.Builder.obtain(content, 0, content.length, paint, (width - 2 * sidePadding).coerceAtLeast(1))
                        .setLineSpacing(0f, LINE_SPACING)
                        .setIncludePad(false)
                // Justified text needs Android 8; older phones keep ragged edges.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                }
                val built = builder.build()
                layout = built
                if (paged) {
                    pageStarts =
                        TextPaging.pageStarts(
                            built.lineCount,
                            built::getLineTop,
                            built::getLineBottom,
                            (height - 2 * verticalPadding).coerceAtLeast(1),
                        )
                }
                goTo(wanted)
            }
            quiet = false
            invalidate()
        }

        private fun goTo(permille: Int) {
            val l = layout ?: return
            val line = l.getLineForOffset(TextPaging.offsetOf(permille, content.length))
            if (paged) {
                page = TextPaging.pageOfLine(pageStarts, line)
            } else {
                scrollTo(0, l.getLineTop(line).coerceIn(0, maxScroll()))
            }
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w > 0 && h > 0 && content.isNotEmpty()) {
                // Keep the reader's place when the size changes, for example after a turn of the phone.
                rebuild(if (layout != null) permille() else wanted)
            }
        }

        override fun onDraw(canvas: Canvas) {
            val l = layout ?: return
            canvas.save()
            if (paged) {
                // Only this page's own lines are drawn: the first line of the next page must not show in part.
                val first = pageStarts[page]
                val last = (if (page < pageStarts.lastIndex) pageStarts[page + 1] else l.lineCount) - 1
                val top = l.getLineTop(first)
                canvas.clipRect(0, verticalPadding, width, verticalPadding + l.getLineBottom(last) - top)
                canvas.translate(sidePadding.toFloat(), (verticalPadding - top).toFloat())
            } else {
                canvas.translate(sidePadding.toFloat(), verticalPadding.toFloat())
            }
            l.draw(canvas)
            canvas.restore()
        }

        override fun computeVerticalScrollRange(): Int = (layout?.height ?: 0) + 2 * verticalPadding

        override fun computeVerticalScrollOffset(): Int = scrollY

        override fun computeVerticalScrollExtent(): Int = height

        private fun maxScroll(): Int = (computeVerticalScrollRange() - height).coerceAtLeast(0)

        override fun onScrollChanged(
            l: Int,
            t: Int,
            oldl: Int,
            oldt: Int,
        ) {
            super.onScrollChanged(l, t, oldl, oldt)
            if (!quiet && !paged) onMoved?.invoke()
        }

        override fun computeScroll() {
            if (scroller.computeScrollOffset()) {
                scrollTo(0, scroller.currY)
                postInvalidateOnAnimation()
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean = if (paged) pagedTouch(event) else scrollDetector.onTouchEvent(event)

        private fun pagedTouch(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (abs(dx) > abs(dy)) {
                        PageMath.swipeForward(dx, swipeThreshold)?.let { onTurnPage?.invoke(it) }
                    } else if (abs(dx) <= touchSlop && abs(dy) <= touchSlop) {
                        val forward = PageMath.tapForward(event.x, width)
                        if (forward == null) performClick() else onTurnPage?.invoke(forward)
                    }
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        private inner class ScrollListener : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                scroller.forceFinished(true)
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                scrollTo(0, (scrollY + distanceY.toInt()).coerceIn(0, maxScroll()))
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                scroller.fling(0, scrollY, 0, -velocityY.toInt(), 0, 0, 0, maxScroll())
                postInvalidateOnAnimation()
                return true
            }
        }
    }
