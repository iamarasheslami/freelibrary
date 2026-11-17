package com.freelibrary.app.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.freelibrary.app.R
import kotlin.math.abs

private const val PDF_SWIPE_DP = 48f
private const val PERCENT = 100

/** Far enough past any edge that the page is clamped to its far side. */
private const val FAR = 1e9f

/**
 * How a zoomed view moves on to another page: the new page keeps the [zoom] and starts at its top left corner when the reader
 * went [forward], or at its bottom right corner when the reader went back, the way the reading would continue.
 */
data class PdfViewCarry(
    val zoom: Float,
    val forward: Boolean,
)

/**
 * Shows one PDF page, fitted whole into the view. Pinch or double-tap zooms, one finger moves a zoomed page, and while the
 * whole page shows a swipe sideways or a tap near an edge asks for another page through [onTurnPage]. While zoomed, a swipe
 * that starts with the page already against a side edge and pushes further that way asks for the neighbouring page through
 * [onTurnPageZoomed], which keeps the zoom. When a zoom settles, [onZoomSettled] lets the owner draw the page sharper for
 * the new zoom.
 *
 * A screen reader hears the description given by [setPageDescription], is told when it changes, and can turn the page and zoom
 * in and out with its own actions, because a swipe or a pinch is not available to it.
 */
class PdfPageView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        /** Asked for the next (true) or the previous (false) page. */
        var onTurnPage: ((forward: Boolean) -> Unit)? = null

        /** Asked for the next (true) or the previous (false) page by a swipe at an edge of a zoomed page, which is at [zoom]. */
        var onTurnPageZoomed: ((forward: Boolean, zoom: Float) -> Unit)? = null

        /** Called for a tap that is not at an edge of a page: the reader shows or hides its bars. */
        var onTap: (() -> Unit)? = null

        /** Called with the zoom once a pinch or a double tap is over. */
        var onZoomSettled: ((zoom: Float) -> Unit)? = null

        private var bitmap: Bitmap? = null
        private var zoom = 1f
        private var offsetX = 0f
        private var offsetY = 0f
        private val drawMatrix = Matrix()
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val swipeThreshold = PDF_SWIPE_DP * resources.displayMetrics.density
        private var downX = 0f
        private var downY = 0f
        private var scaledDuringGesture = false
        private var startEdge: Boolean? = null

        init {
            isFocusable = true
            ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
            ViewCompat.setAccessibilityLiveRegion(this, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
            ViewCompat.setAccessibilityDelegate(this, PageDelegate())
        }

        private val scaleDetector =
            ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                        scaledDuringGesture = true
                        return true
                    }

                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        zoomTo(zoom * detector.scaleFactor, detector.focusX, detector.focusY)
                        return true
                    }

                    override fun onScaleEnd(detector: ScaleGestureDetector) {
                        onZoomSettled?.invoke(zoom)
                    }
                },
            )

        private val gestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent): Boolean = true

                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        val target = if (isZoomed()) 1f else PdfZoomMath.DOUBLE_TAP_ZOOM
                        zoomTo(target, e.x, e.y)
                        onZoomSettled?.invoke(zoom)
                        return true
                    }

                    override fun onScroll(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        distanceX: Float,
                        distanceY: Float,
                    ): Boolean {
                        if (!isZoomed() && !isTallerThanView()) return false
                        offsetX -= distanceX
                        offsetY -= distanceY
                        clampOffsets()
                        invalidate()
                        return true
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        val edge = if (isZoomed()) null else PageMath.tapForward(e.x, width)
                        if (edge != null) onTurnPage?.invoke(edge) else onTap?.invoke()
                        return true
                    }
                },
            )

        /** What a screen reader says about the page showing, for example its number; the view is a live region, so a change is read. */
        fun setPageDescription(text: CharSequence?) {
            contentDescription = text
        }

        /** Zooms in or out by a whole level around the middle of the view, as a screen reader asks, and lets the owner sharpen it. */
        private fun zoomFromScreenReader(zoomIn: Boolean) {
            zoomTo(PdfZoomMath.stepZoom(zoom, zoomIn), width / 2f, height / 2f)
            onZoomSettled?.invoke(zoom)
        }

        private inner class PageDelegate : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat,
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isScrollable = true
                if (isZoomed()) info.stateDescription = context.getString(R.string.pdf_zoom_state, (zoom * PERCENT).toInt())
                info.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
                info.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
                if (zoom < PdfZoomMath.MAX_ZOOM) {
                    info.addAction(AccessibilityActionCompat(R.id.action_pdf_zoom_in, context.getString(R.string.reader_action_zoom_in)))
                }
                if (isZoomed()) {
                    info.addAction(AccessibilityActionCompat(R.id.action_pdf_zoom_out, context.getString(R.string.reader_action_zoom_out)))
                }
            }

            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: Bundle?,
            ): Boolean =
                when (action) {
                    AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD -> true.also { onTurnPage?.invoke(true) }
                    AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD -> true.also { onTurnPage?.invoke(false) }
                    R.id.action_pdf_zoom_in -> true.also { zoomFromScreenReader(zoomIn = true) }
                    R.id.action_pdf_zoom_out -> true.also { zoomFromScreenReader(zoomIn = false) }
                    else -> super.performAccessibilityAction(host, action, args)
                }
        }

        /**
         * Shows [page]. With [keepView] the zoom and position stay as they are, which is how a page is
         * swapped for a sharper drawing of itself; otherwise the whole page shows, or, with a [carry], the page at the carried
         * zoom starting at the corner that the reading continues from.
         */
        fun showPage(
            page: Bitmap,
            keepView: Boolean,
            carry: PdfViewCarry? = null,
        ) {
            val previous = bitmap
            bitmap = page
            if (!keepView) {
                zoom = carry?.let { PdfZoomMath.clampZoom(it.zoom) } ?: 1f
                val start = if (carry == null || carry.forward) 0f else -FAR
                offsetX = start
                offsetY = start
            }
            clampOffsets()
            invalidate()
            if (previous !== page) previous?.recycle()
        }

        /**
         * Shows pages in dark theme colours: white becomes [paper] and black becomes [ink]. With either
         * one null the page shows as it is. The change happens while drawing, so it costs no memory and
         * takes effect at once.
         */
        fun setNightColors(
            paper: Int?,
            ink: Int?,
        ) {
            paint.colorFilter =
                if (paper == null || ink == null) {
                    null
                } else {
                    ColorMatrixColorFilter(ColorMatrix(PdfNightColors.matrix(paper, ink)))
                }
            invalidate()
        }

        private fun isZoomed(): Boolean = zoom > PdfZoomMath.ZOOMED_THRESHOLD

        private fun isTallerThanView(): Boolean {
            val page = bitmap ?: return false
            return page.height * fitScale(page) * zoom > height
        }

        /** The page is fitted to the width of the view; a page taller than the view is moved up and down. */
        private fun fitScale(page: Bitmap): Float = width.toFloat() / page.width

        private fun zoomTo(
            requested: Float,
            focusX: Float,
            focusY: Float,
        ) {
            val old = zoom
            val new = PdfZoomMath.clampZoom(requested)
            if (new == old) return
            offsetX = PdfZoomMath.translationAfterZoom(offsetX, focusX, old, new)
            offsetY = PdfZoomMath.translationAfterZoom(offsetY, focusY, old, new)
            zoom = new
            clampOffsets()
            invalidate()
        }

        private fun clampOffsets() {
            val page = bitmap ?: return
            val scale = fitScale(page) * zoom
            offsetX = PdfZoomMath.clampTranslation(offsetX, page.width * scale, width.toFloat())
            offsetY = PdfZoomMath.clampTranslation(offsetY, page.height * scale, height.toFloat())
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            clampOffsets()
        }

        override fun onDraw(canvas: Canvas) {
            val page = bitmap ?: return
            val scale = fitScale(page) * zoom
            drawMatrix.setScale(scale, scale)
            drawMatrix.postTranslate(offsetX, offsetY)
            canvas.drawBitmap(page, drawMatrix, paint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (bitmap == null) return super.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                downX = event.x
                downY = event.y
                scaledDuringGesture = false
                val page = bitmap
                startEdge =
                    if (page != null && isZoomed()) {
                        PdfZoomMath.edgeOf(offsetX, page.width * fitScale(page) * zoom, width.toFloat())
                    } else {
                        null
                    }
            }
            scaleDetector.onTouchEvent(event)
            if (!scaleDetector.isInProgress) gestureDetector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP && !scaledDuringGesture) {
                val dx = event.x - downX
                val dy = event.y - downY
                if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                    if (isZoomed()) {
                        PdfZoomMath.turnAtEdge(startEdge, dx, swipeThreshold)?.let { onTurnPageZoomed?.invoke(it, zoom) }
                    } else {
                        PageMath.swipeForward(dx, swipeThreshold)?.let { onTurnPage?.invoke(it) }
                    }
                }
            }
            return true
        }
    }
