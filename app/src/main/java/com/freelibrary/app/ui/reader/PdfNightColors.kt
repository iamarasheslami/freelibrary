package com.freelibrary.app.ui.reader

/**
 * The colour maths for showing PDF pages in a dark theme. A PDF page is drawn black on white, so in
 * a dark theme every colour on it is remapped: white becomes the theme's paper colour, black becomes
 * its ink colour and everything between becomes the matching mix of the two. Pure functions, so they
 * are easy to test.
 */
internal object PdfNightColors {
    private const val CHANNEL_MAX = 255f
    private const val LUMINANCE_RED = 0.2126f
    private const val LUMINANCE_GREEN = 0.7152f
    private const val LUMINANCE_BLUE = 0.0722f
    private const val DARK_LUMINANCE_LIMIT = 0.5f

    /** True if [color] (an ARGB int) is closer to black than to white. */
    fun isDark(color: Int): Boolean {
        val luminance =
            (
                LUMINANCE_RED * channel(color, RED_SHIFT) +
                    LUMINANCE_GREEN * channel(color, GREEN_SHIFT) +
                    LUMINANCE_BLUE * channel(color, BLUE_SHIFT)
            ) / CHANNEL_MAX
        return luminance < DARK_LUMINANCE_LIMIT
    }

    /**
     * The 4 by 5 colour matrix (row by row, as android.graphics.ColorMatrix reads it) that turns white
     * into [paper] and black into [ink], channel by channel. The alpha channel is left alone.
     */
    fun matrix(
        paper: Int,
        ink: Int,
    ): FloatArray {
        val paperRed = channel(paper, RED_SHIFT)
        val paperGreen = channel(paper, GREEN_SHIFT)
        val paperBlue = channel(paper, BLUE_SHIFT)
        val inkRed = channel(ink, RED_SHIFT)
        val inkGreen = channel(ink, GREEN_SHIFT)
        val inkBlue = channel(ink, BLUE_SHIFT)
        return floatArrayOf(
            (paperRed - inkRed) / CHANNEL_MAX, 0f, 0f, 0f, inkRed,
            0f, (paperGreen - inkGreen) / CHANNEL_MAX, 0f, 0f, inkGreen,
            0f, 0f, (paperBlue - inkBlue) / CHANNEL_MAX, 0f, inkBlue,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BLUE_SHIFT = 0
    private const val BYTE_MASK = 0xFF

    private fun channel(
        color: Int,
        shift: Int,
    ): Float = ((color shr shift) and BYTE_MASK).toFloat()
}
