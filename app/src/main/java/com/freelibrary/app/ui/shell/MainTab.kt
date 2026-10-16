package com.freelibrary.app.ui.shell

import androidx.annotation.IdRes
import com.freelibrary.app.R

/** The three main pages the bottom bar switches between, in the order the bar shows them. */
enum class MainTab(
    @IdRes val destinationId: Int,
) {
    HOME(R.id.homeFragment),
    EXPLORE(R.id.exploreFragment),
    PROFILE(R.id.profileFragment),
}

/** The tab whose page is showing, or null on any other screen, where the bar is hidden. */
fun mainTabFor(
    @IdRes destinationId: Int?,
): MainTab? = MainTab.entries.firstOrNull { it.destinationId == destinationId }
