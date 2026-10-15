package com.freelibrary.app.ui.shell

import android.view.View
import androidx.core.view.isVisible

/**
 * The bar at the bottom of the screen. [tabs] are the tab views; selecting a tab view also
 * selects what is inside it, which is how the pill and the icon or text change colour.
 */
class BottomBar(
    private val bar: View,
    private val tabs: Map<MainTab, View>,
    onTabClick: (MainTab) -> Unit,
) {
    init {
        tabs.forEach { (tab, view) -> view.setOnClickListener { onTabClick(tab) } }
    }

    /** Shows the bar with [tab] selected, or hides the bar when [tab] is null. */
    fun show(tab: MainTab?) {
        bar.isVisible = tab != null
        tabs.forEach { (candidate, view) -> view.isSelected = candidate == tab }
    }
}
