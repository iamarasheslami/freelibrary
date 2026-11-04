package com.freelibrary.app.ui.theme

import android.content.Context
import androidx.core.content.edit

/**
 * The reader's choices of font and text colour. Like [AppScale] they live in the small "display" preferences file, not in
 * the settings store, so a backup made on one phone does not restyle another.
 */
object FontChoices {
    private const val PREFERENCES = "display"
    private const val KEY_TEXT_FONT = "text_font"
    private const val KEY_EPUB_FONT = "epub_font"
    private const val KEY_TEXT_COLOUR = "text_colour"
    private const val KEY_READER_THEME = "reader_theme"

    /** Stored for an EPUB when the book's own font is kept; a font id means that font replaces the book's. */
    private const val BOOK_OWN = "book"

    /**
     * Forgets every choice: the reader fonts go back to Literata (a book keeps its own font), the colour to the page's and
     * the page to the app's.
     */
    fun reset(context: Context) {
        preferences(context).edit {
            remove(KEY_TEXT_FONT)
            remove(KEY_EPUB_FONT)
            remove(KEY_TEXT_COLOUR)
            remove(KEY_READER_THEME)
        }
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /** The font of plain text books. */
    fun textFont(context: Context): AppFont = AppFont.fromId(preferences(context).getString(KEY_TEXT_FONT, null)) ?: AppFont.LITERATA

    fun setTextFont(
        context: Context,
        font: AppFont,
    ) = preferences(context).edit { putString(KEY_TEXT_FONT, font.id) }

    /** The font that replaces an EPUB book's own, or null to keep the book's font (the default). */
    fun epubFont(context: Context): AppFont? = AppFont.fromId(preferences(context).getString(KEY_EPUB_FONT, null))

    fun setEpubFont(
        context: Context,
        font: AppFont?,
    ) = preferences(context).edit { putString(KEY_EPUB_FONT, font?.id ?: BOOK_OWN) }

    /** The colour of reading text. */
    fun textColour(context: Context): TextColour = TextColour.fromId(preferences(context).getString(KEY_TEXT_COLOUR, null))

    fun setTextColour(
        context: Context,
        colour: TextColour,
    ) = preferences(context).edit { putString(KEY_TEXT_COLOUR, colour.id) }

    /** The page the readers draw: the app's own until the reader picks Sepia or Dim night. */
    fun readerTheme(context: Context): ReaderTheme = ReaderTheme.fromId(preferences(context).getString(KEY_READER_THEME, null))

    fun setReaderTheme(
        context: Context,
        theme: ReaderTheme,
    ) = preferences(context).edit { putString(KEY_READER_THEME, theme.id) }
}
