package com.freelibrary.app.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

private const val PDF_SWIPE_DP = 48f

/**
 * Shows one PDF page, fitted whole into the view. Pinch or double-tap zooms, one finger moves a
 * zoomed page, and while the whole page shows a swipe sideways or a tap near an edge asks for
 * another page through [onTurnPage]. When a zoom settles, [onZoomSettled] lets the owner draw the
 * page sharper for the new zoom.
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
                        if (isZoomed()) return false
                        PageMath.tapForward(e.x, width)?.let { onTurnPage?.invoke(it) }
                        return true
                    }
                },
            )

        /**
         * Shows [page]. With [keepView] the zoom and position stay as they are, which is how a page is
         * swapped for a sharper drawing of itself; otherwise the whole page shows.
         */
        fun showPage(
            page: Bitmap,
            keepView: Boolean,
        ) {
            val previous = bitmap
            bitmap = page
            if (!keepView) {
                zoom = 1f
                offsetX = 0f
                offsetY = 0f
            }
            clampOffsets()
            invalidate()
            if (previous !== page) previous?.recycle()
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
            }
            scaleDetector.onTouchEvent(event)
            if (!scaleDetector.isInProgress) gestureDetector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP && !scaledDuringGesture && !isZoomed()) {
                val dx = event.x - downX
                val dy = event.y - downY
                if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                    PageMath.swipeForward(dx, swipeThreshold)?.let { onTurnPage?.invoke(it) }
                }
            }
            return true
        }
    }
