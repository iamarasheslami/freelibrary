package com.freelibrary.app.ui.filter

import android.content.Context
import android.view.MenuItem
import androidx.core.view.MenuItemCompat

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
