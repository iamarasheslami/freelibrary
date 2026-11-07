package com.freelibrary.app.ui.reader

import kotlin.math.ceil

/** The arithmetic of zooming and moving a PDF page: pure functions, so they are easy to test. */
internal object PdfZoomMath {
    const val MAX_ZOOM = 3f
    const val DOUBLE_TAP_ZOOM = 2f

    /** Anything this close to 1 counts as not zoomed. */
    const val ZOOMED_THRESHOLD = 1.01f

    fun clampZoom(zoom: Float): Float = zoom.coerceIn(1f, MAX_ZOOM)

    /**
     * Where the page's edge may sit on the view: a page smaller than the view is centred, and one
     * larger than it can be moved only as far as its edges reach the edges of the view.
     */
    fun clampTranslation(
        translation: Float,
        contentSize: Float,
        viewSize: Float,
    ): Float =
        if (contentSize <= viewSize) {
            (viewSize - contentSize) / 2f
        } else {
            translation.coerceIn(viewSize - contentSize, 0f)
        }

    /** The translation that keeps the point at [focus] on the view under the finger while zooming from [oldZoom] to [newZoom]. */
    fun translationAfterZoom(
        translation: Float,
        focus: Float,
        oldZoom: Float,
        newZoom: Float,
    ): Float = focus - (focus - translation) * (newZoom / oldZoom)

    /** How close to an edge of the view the page must sit, in pixels, to count as being at that edge. */
    private const val EDGE_TOLERANCE = 1f

    /**
     * Which side of the view a zoomed page touches along one axis: false for its start (left) edge, true for its end (right)
     * edge, null when it sits between them or fits inside the view. A swipe that starts on an edge can turn the page.
     */
    fun edgeOf(
        translation: Float,
        contentSize: Float,
        viewSize: Float,
    ): Boolean? =
        when {
            contentSize <= viewSize -> null
            translation >= -EDGE_TOLERANCE -> false
            translation <= viewSize - contentSize + EDGE_TOLERANCE -> true
            else -> null
        }

    /**
     * Whether a swipe of [dx] pixels (positive to the right) that started with the page at [startEdge] turns the page: true
     * for the next page, false for the previous one, null for no turn. Only a swipe that pushes against the edge it started
     * on counts, and only when it is at least [threshold] long; a swipe that merely reaches the edge does not turn.
     */
    fun turnAtEdge(
        startEdge: Boolean?,
        dx: Float,
        threshold: Float,
    ): Boolean? =
        when {
            startEdge == false && dx >= threshold -> false
            startEdge == true && dx <= -threshold -> true
            else -> null
        }

    /** How sharp a drawing of the page must be to look good at [zoom]: 1 for the whole page, then 2 and 3. */
    fun renderLevel(zoom: Float): Int = ceil(zoom - 0.01f).toInt().coerceIn(1, MAX_ZOOM.toInt())
}
