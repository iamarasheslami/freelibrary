package com.freelibrary.app.ui.reader

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import com.freelibrary.app.domain.model.ReaderPosition

/** A web view that reports and restores how far down the page it is, in thousandths. */
class ReaderWebView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.webViewStyle,
    ) : WebView(context, attrs, defStyleAttr) {
        /** How far the page can scroll, or 0 or less while it is not laid out or fits the screen. */
        private fun scrollableHeight(): Int = computeVerticalScrollRange() - computeVerticalScrollExtent()

        /** How far down the page is: 0 at the top, 1000 when its end shows. 0 for a page that does not scroll. */
        fun scrolledPermille(): Int {
            val scrollable = scrollableHeight()
            if (scrollable <= 0) return 0
            return (scrollY.toLong() * ReaderPosition.MAX_PERMILLE / scrollable).toInt().coerceIn(0, ReaderPosition.MAX_PERMILLE)
        }

        /** Scrolls to [permille] of the page. Returns false while the page is not yet long enough to scroll. */
        fun scrollToPermille(permille: Int): Boolean {
            val scrollable = scrollableHeight()
            if (scrollable <= 0) return false
            scrollTo(0, (scrollable.toLong() * permille.coerceIn(0, ReaderPosition.MAX_PERMILLE) / ReaderPosition.MAX_PERMILLE).toInt())
            return true
        }
    }
