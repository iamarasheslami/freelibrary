package com.freelibrary.app.ui.theme

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.freelibrary.app.R

/**
 * The colour of the text in the readers. [DEFAULT] follows the theme. The ten others are real colours, each in a shade for the
 * light page and a lighter one for the dark page, so the text stays readable: every shade keeps a contrast of at least 7 to 1
 * on its page (PaletteContrastTest checks it).
 */
enum class TextColour(
    val id: String,
    @StringRes val label: Int,
    @ColorRes private val colour: Int?,
) {
    DEFAULT("default", R.string.text_colour_default, null),
    CONTRAST("contrast", R.string.text_colour_contrast, R.color.reading_text_contrast),
    GRAY("gray", R.string.text_colour_gray, R.color.reading_text_gray),
    BROWN("brown", R.string.text_colour_brown, R.color.reading_text_brown),
    RED("red", R.string.text_colour_red, R.color.reading_text_red),
    ORANGE("orange", R.string.text_colour_orange, R.color.reading_text_orange),
    GREEN("green", R.string.text_colour_green, R.color.reading_text_green),
    TEAL("teal", R.string.text_colour_teal, R.color.reading_text_teal),
    BLUE("blue", R.string.text_colour_blue, R.color.reading_text_blue),
    PURPLE("purple", R.string.text_colour_purple, R.color.reading_text_purple),
    PINK("pink", R.string.text_colour_pink, R.color.reading_text_pink),
    ;

    /** The colour to draw text in: this one's shade for the current theme, or [themeColour] for [DEFAULT]. */
    fun resolve(
        context: Context,
        themeColour: Int,
    ): Int = colour?.let { ContextCompat.getColor(context, it) } ?: themeColour

    companion object {
        fun fromId(id: String?): TextColour = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
