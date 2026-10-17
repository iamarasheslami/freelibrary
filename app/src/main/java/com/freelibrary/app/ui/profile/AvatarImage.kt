package com.freelibrary.app.ui.profile

import android.widget.ImageView
import coil.ImageLoader
import coil.dispose
import coil.load
import com.freelibrary.app.R

/** Shows the gray reader placeholder, for a reader with no picture. */
internal fun ImageView.showAvatarPlaceholder() {
    dispose()
    setBackgroundResource(R.drawable.bg_avatar_placeholder)
    setImageResource(R.drawable.ic_avatar_placeholder)
}

/**
 * Shows a picture: the saved file or a freshly chosen one. [memoryKey] keeps an old picture from
 * being shown after the saved one changes; it can be null for a picture that is never saved again.
 */
internal fun ImageView.showAvatarPicture(
    data: Any,
    memoryKey: String?,
    imageLoader: ImageLoader,
) {
    background = null
    load(data, imageLoader) {
        memoryKey?.let { memoryCacheKey(it) }
        crossfade(false)
    }
}
