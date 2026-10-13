package com.freelibrary.app.ui.insets

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pads this view's top and bottom by the system bars (status bar, navigation bar, cutout),
 * on top of whatever padding it already has. The insets are not consumed, so other views
 * still receive them.
 */
fun View.applySystemBarInsetsAsPadding() {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars: Insets =
            windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
        view.setPadding(
            view.paddingLeft,
            initialTop + bars.top,
            view.paddingRight,
            initialBottom + bars.bottom,
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}
