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

    /** How sharp a drawing of the page must be to look good at [zoom]: 1 for the whole page, then 2 and 3. */
    fun renderLevel(zoom: Float): Int = ceil(zoom - 0.01f).toInt().coerceIn(1, MAX_ZOOM.toInt())
}
