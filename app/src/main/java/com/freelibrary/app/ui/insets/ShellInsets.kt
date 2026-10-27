package com.freelibrary.app.ui.insets

/**
 * How the activity's own views make room for the system, worked out from raw inset sizes in pixels.
 *
 * From Android 15 on (API 35) an app that targets API 35 or higher is drawn edge to edge by the system and
 * its window is no longer resized for the keyboard, so the activity itself must keep its content clear of the
 * side bars, the cutout and the keyboard. Below that the system already does so and nothing extra is applied.
 *
 * @property left padding for the app content on the left edge
 * @property right padding for the app content on the right edge
 * @property contentBottom padding under the app content, used to lift it above the keyboard
 * @property barBottom extra padding under the bottom bar for the navigation bar
 * @property consumeBottomSystemBar whether the screens above must not pad for the navigation bar themselves
 */
data class ShellInsets(
    val left: Int,
    val right: Int,
    val contentBottom: Int,
    val barBottom: Int,
    val consumeBottomSystemBar: Boolean,
)

fun shellInsets(
    edgeToEdge: Boolean,
    sideLeft: Int,
    sideRight: Int,
    navigationBottom: Int,
    imeBottom: Int,
    bottomBarShown: Boolean,
): ShellInsets {
    if (!edgeToEdge) {
        return ShellInsets(0, 0, 0, navigationBottom, bottomBarShown)
    }
    // The keyboard inset already includes the navigation bar's height, so while it shows the content is lifted
    // by all of it and neither the bottom bar nor the screens above pad for the navigation bar a second time.
    val keyboardShown = imeBottom > 0
    return ShellInsets(
        left = sideLeft,
        right = sideRight,
        contentBottom = if (keyboardShown) imeBottom else 0,
        barBottom = if (keyboardShown) 0 else navigationBottom,
        consumeBottomSystemBar = bottomBarShown || keyboardShown,
    )
}
