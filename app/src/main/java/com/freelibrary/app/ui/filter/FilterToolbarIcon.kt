package com.freelibrary.app.ui.filter

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat

/** Shows the filter icon as the toolbar's left icon: with a dot while a filter is on. */
fun Toolbar.showFilterOnNavigation(active: Boolean) {
    setNavigationIcon(FilterLabels.iconRes(active))
    navigationContentDescription = context.getString(FilterLabels.descriptionRes(active))
}

/** Shows the filter icon as a menu item: with a dot while a filter is on. */
fun MenuItem.showFilterState(
    context: Context,
    active: Boolean,
) {
    setIcon(FilterLabels.iconRes(active))
    val description = context.getString(FilterLabels.descriptionRes(active))
    title = description
    MenuItemCompat.setContentDescription(this, description)
}
