package com.freelibrary.app.ui.shell

import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * The start-up screen: a page-coloured cover with the logo in the middle that sits above the app
 * until Home has content. [reveal] fades the logo and the cover away while the page rises into place.
 */
class SplashOverlay(
    private val overlay: View,
    private val logo: View,
    private val content: View,
) {
    /** Removes the cover at once, for a screen that is recreated after Home was already ready. */
    fun hideNow() {
        overlay.visibility = View.GONE
    }

    /** Plays the hand-over from the logo to the page, then removes the cover. */
    fun reveal() {
        if (overlay.visibility != View.VISIBLE) return
        val rise = content.resources.displayMetrics.density * RISE_DP
        content.alpha = 0f
        content.translationY = rise
        content
            .animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(PAGE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        logo
            .animate()
            .alpha(0f)
            .scaleX(LOGO_END_SCALE)
            .scaleY(LOGO_END_SCALE)
            .setDuration(LOGO_MS)
            .start()
        overlay
            .animate()
            .alpha(0f)
            .setStartDelay(COVER_DELAY_MS)
            .setDuration(COVER_MS)
            .withEndAction { overlay.visibility = View.GONE }
            .start()
    }

    private companion object {
        const val RISE_DP = 24f
        const val PAGE_MS = 400L
        const val LOGO_MS = 300L
        const val LOGO_END_SCALE = 1.15f
        const val COVER_DELAY_MS = 80L
        const val COVER_MS = 340L
    }
}
