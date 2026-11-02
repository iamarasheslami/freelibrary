package com.freelibrary.app.ui.theme

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.freelibrary.app.R
import com.google.android.material.color.MaterialColors

/** The colours a reader page is drawn in. [dark] says which shade of the reading text colours suits the page. */
data class ReaderPage(
    val background: Int,
    val ink: Int,
    val link: Int,
    val highlight: Int,
    val dark: Boolean,
)

/**
 * The page behind the book's text. [APP] takes the app's own colours, so the page follows the app theme. [SEPIA] is a warm
 * light page and [DIM] a soft dark one; both keep their colours in either app theme.
 */
enum class ReaderTheme(
    val id: String,
    @StringRes val label: Int,
    @ColorRes private val background: Int?,
    @ColorRes private val ink: Int?,
    @ColorRes private val link: Int?,
    @ColorRes private val highlight: Int?,
    private val dark: Boolean,
) {
    APP("app", R.string.reader_theme_app, null, null, null, null, false),
    SEPIA(
        "sepia",
        R.string.reader_theme_sepia,
        R.color.reader_page_sepia,
        R.color.reader_page_sepia_ink,
        R.color.reader_page_sepia_link,
        R.color.reader_page_sepia_highlight,
        false,
    ),
    DIM(
        "dim",
        R.string.reader_theme_dim,
        R.color.reader_page_dim,
        R.color.reader_page_dim_ink,
        R.color.reader_page_dim_link,
        R.color.reader_page_dim_highlight,
        true,
    ),
    ;

    /** The page's colours. [view] gives the app theme that [APP] reads. */
    fun page(view: View): ReaderPage {
        val context = view.context
        if (background == null || ink == null || link == null || highlight == null) {
            val surface = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface)
            return ReaderPage(
                background = surface,
                ink = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface),
                link = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary),
                highlight = MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimaryContainer),
                dark = ColorUtils.calculateLuminance(surface) < DARK_LUMINANCE_LIMIT,
            )
        }
        return ReaderPage(
            background = ContextCompat.getColor(context, background),
            ink = ContextCompat.getColor(context, ink),
            link = ContextCompat.getColor(context, link),
            highlight = ContextCompat.getColor(context, highlight),
            dark = dark,
        )
    }

    companion object {
        private const val DARK_LUMINANCE_LIMIT = 0.5

        /** The theme stored under [id]; the app's own for anything unknown. */
        fun fromId(id: String?): ReaderTheme = entries.firstOrNull { it.id == id } ?: APP
    }
}
