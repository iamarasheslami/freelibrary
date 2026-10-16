package com.freelibrary.app.ui.insets

import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat

/**
 * These insets without the bottom edge of the system bars. Used above a view that already pads
 * for the navigation bar (the bottom bar), so the screens above it do not pad for it a second time.
 */
fun WindowInsetsCompat.withoutBottomSystemBar(): WindowInsetsCompat {
    val bars = getInsets(WindowInsetsCompat.Type.systemBars())
    return WindowInsetsCompat
        .Builder(this)
        .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(bars.left, bars.top, bars.right, 0))
        .build()
}
