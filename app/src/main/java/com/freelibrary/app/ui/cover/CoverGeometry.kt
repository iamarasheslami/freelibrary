package com.freelibrary.app.ui.cover

/**
 * Where everything sits on a generated cover of a given size, in pixels. The design is drawn on a
 * 240 by 360 reference cover and scaled by width, so every number a designer would tweak lives here.
 */
class CoverGeometry(
    val width: Float,
    val height: Float,
) {
    private val scale = width / REFERENCE_WIDTH

    /** False when the cover is too small to show more than its background. */
    val canDrawText: Boolean = width >= MIN_TEXT_WIDTH && height >= MIN_TEXT_HEIGHT

    val textLeft = TEXT_LEFT * scale
    val textWidth = (REFERENCE_WIDTH - TEXT_LEFT - TEXT_RIGHT) * scale

    val ruleY = 44f * scale
    val ruleLength = 40f * scale
    val ruleThickness = 3f * scale

    val titleTop = 58f * scale
    val titleMaxLines = 4

    /** Title text sizes to try, largest first. The first one that fits within the lines is used. */
    val titleSizes: List<Float> = TITLE_SIZES.map { it * scale }
    val subtitleGap = 8f * scale
    val subtitleSize = 15f * scale
    val subtitleMaxLines = 2

    val authorSize = 15f * scale
    val authorBottom = height - 28f * scale

    /** Whether the subtitle and the author are big enough to read. Smaller covers show only the title. */
    val showsDetails: Boolean = canDrawText && subtitleSize >= MIN_DETAIL_SIZE

    /** The decorations stay above this line, so they never run behind the author. */
    val horizonY = height * 0.80f

    val spineWidth = width * 0.07f
    val spineLineX = width * 0.10f
    val spineLineThickness = 2f * scale

    val motifStroke = 3f * scale
    val arcRadii: List<Float> = ARC_RADII.map { it * scale }

    val dotColumns = 6
    val dotRows = 5
    val dotSpacing = 22f * scale
    val dotRadius = 3f * scale
    val dotsRight = width - 30f * scale
    val dotsBottom = horizonY - 20f * scale

    val lineSpacing = 28f * scale
    val lineRise = 170f * scale

    private companion object {
        const val REFERENCE_WIDTH = 240f
        const val TEXT_LEFT = 28f
        const val TEXT_RIGHT = 20f
        const val MIN_TEXT_WIDTH = 48f
        const val MIN_TEXT_HEIGHT = 72f
        const val MIN_DETAIL_SIZE = 9f
        val TITLE_SIZES = listOf(30f, 28f, 26f, 24f, 22f, 20f, 18f, 16f)
        val ARC_RADII = listOf(90f, 140f, 190f, 240f)
    }
}
