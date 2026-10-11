package com.freelibrary.app.ui.cover

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation

/**
 * A book cover drawn from the book's own data, for books without a cover image and while one
 * loads. It needs no network and no bitmap, and a book always gets the same cover. [CoverStyle]
 * chooses the color and motif, [CoverTitle] splits the title, and [CoverGeometry] holds every
 * position. Text layouts are built when the size changes, so [draw] allocates nothing.
 */
class GeneratedCoverDrawable(
    private val coverStyle: CoverStyle,
    title: String,
    author: String?,
    titleTypeface: Typeface,
    detailTypeface: Typeface,
) : Drawable() {
    private val coverTitle = CoverTitle.parse(title)
    private val authorName = author?.trim()?.takeIf { it.isNotEmpty() }

    private val backgroundPaint = Paint().apply { color = coverStyle.backgroundColor }
    private val rulePaint = Paint().apply { color = CoverStyle.RULE_COLOR }
    private val motifPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val layerPaint = Paint()
    private val titlePaint = textPaint(titleTypeface)
    private val detailPaint = textPaint(detailTypeface)

    private var geometry = CoverGeometry(0f, 0f)
    private var titleLayout: StaticLayout? = null
    private var subtitleLayout: StaticLayout? = null
    private var authorLayout: StaticLayout? = null

    override fun onBoundsChange(bounds: Rect) {
        geometry = CoverGeometry(bounds.width().toFloat(), bounds.height().toFloat())
        titleLayout = null
        subtitleLayout = null
        authorLayout = null
        if (!geometry.canDrawText) return
        titleLayout = fitTitle()
        if (geometry.showsDetails) {
            subtitleLayout =
                coverTitle.subtitle?.let {
                    layout(it, detailPaint, geometry.subtitleSize, geometry.subtitleMaxLines)
                }
            authorLayout = authorName?.let { layout(it, detailPaint, geometry.authorSize, 1) }
        }
    }

    override fun draw(canvas: Canvas) {
        val area = bounds
        if (area.isEmpty) return
        val needsLayer = layerPaint.alpha < FULLY_OPAQUE || layerPaint.colorFilter != null
        val saved =
            if (needsLayer) {
                canvas.saveLayer(
                    area.left.toFloat(),
                    area.top.toFloat(),
                    area.right.toFloat(),
                    area.bottom.toFloat(),
                    layerPaint,
                )
            } else {
                canvas.save()
            }
        canvas.translate(area.left.toFloat(), area.top.toFloat())
        canvas.drawRect(0f, 0f, geometry.width, geometry.height, backgroundPaint)
        if (geometry.canDrawText) {
            drawMotif(canvas)
            drawText(canvas)
        }
        canvas.restoreToCount(saved)
    }

    override fun setAlpha(alpha: Int) {
        layerPaint.alpha = alpha
        invalidateSelf()
    }

    override fun getAlpha(): Int = layerPaint.alpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        layerPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = if (layerPaint.alpha < FULLY_OPAQUE) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE

    private fun drawMotif(canvas: Canvas) {
        if (coverStyle.motif == CoverMotif.SPINE) {
            drawSpine(canvas)
            return
        }

        // Everything else stays above the author line.
        canvas.withClip(0f, 0f, geometry.width, geometry.horizonY) {
            when (coverStyle.motif) {
                CoverMotif.ARCS -> drawArcs(this)
                CoverMotif.DOTS -> drawDots(this)
                else -> drawLines(this)
            }
        }
    }

    private fun drawSpine(canvas: Canvas) {
        faint(SPINE_FILL_ALPHA, Paint.Style.FILL)
        canvas.drawRect(0f, 0f, geometry.spineWidth, geometry.height, motifPaint)
        faint(SPINE_LINE_ALPHA, Paint.Style.STROKE)
        motifPaint.strokeWidth = geometry.spineLineThickness
        canvas.drawLine(geometry.spineLineX, 0f, geometry.spineLineX, geometry.height, motifPaint)
    }

    private fun drawArcs(canvas: Canvas) {
        faint(ARC_ALPHA, Paint.Style.STROKE)
        motifPaint.strokeWidth = geometry.motifStroke
        for (radius in geometry.arcRadii) {
            canvas.drawCircle(geometry.width, geometry.height, radius, motifPaint)
        }
    }

    private fun drawDots(canvas: Canvas) {
        faint(DOT_ALPHA, Paint.Style.FILL)
        for (column in 0 until geometry.dotColumns) {
            for (row in 0 until geometry.dotRows) {
                val x = geometry.dotsRight - column * geometry.dotSpacing
                val y = geometry.dotsBottom - row * geometry.dotSpacing
                canvas.drawCircle(x, y, geometry.dotRadius, motifPaint)
            }
        }
    }

    private fun drawLines(canvas: Canvas) {
        faint(LINE_ALPHA, Paint.Style.STROKE)
        motifPaint.strokeWidth = geometry.motifStroke
        var x = -geometry.lineRise
        while (x < geometry.width) {
            canvas.drawLine(x, geometry.height, x + geometry.lineRise, geometry.height - geometry.lineRise, motifPaint)
            x += geometry.lineSpacing
        }
    }

    private fun drawText(canvas: Canvas) {
        val left = geometry.textLeft
        val halfRule = geometry.ruleThickness / 2
        canvas.drawRect(left, geometry.ruleY - halfRule, left + geometry.ruleLength, geometry.ruleY + halfRule, rulePaint)
        var y = geometry.titleTop
        titleLayout?.let {
            drawLayout(canvas, it, left, y)
            y += it.height + geometry.subtitleGap
        }
        subtitleLayout?.let { drawLayout(canvas, it, left, y) }
        authorLayout?.let { drawLayout(canvas, it, left, geometry.authorBottom - it.height) }
    }

    private fun drawLayout(
        canvas: Canvas,
        layout: StaticLayout,
        x: Float,
        y: Float,
    ) {
        canvas.withTranslation(x, y) { layout.draw(this) }
    }

    /** The largest title size whose text fits within the allowed lines, else the smallest, cut with an ellipsis. */
    private fun fitTitle(): StaticLayout? {
        val text = coverTitle.main
        if (text.isEmpty()) return null
        for (size in geometry.titleSizes) {
            val candidate = layout(text, titlePaint, size, Int.MAX_VALUE)
            if (candidate.lineCount <= geometry.titleMaxLines) return candidate
        }
        return layout(text, titlePaint, geometry.titleSizes.last(), geometry.titleMaxLines)
    }

    /** Each layout gets its own paint copy, because a layout reads the paint again every time it draws. */
    private fun layout(
        text: String,
        paint: TextPaint,
        size: Float,
        maxLines: Int,
    ): StaticLayout {
        val sized = TextPaint(paint).apply { textSize = size }
        val width = geometry.textWidth.toInt().coerceAtLeast(1)
        return StaticLayout.Builder
            .obtain(text, 0, text.length, sized, width)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .setLineSpacing(0f, LINE_SPACING)
            .build()
    }

    private fun faint(
        alpha: Int,
        paintStyle: Paint.Style,
    ) {
        motifPaint.style = paintStyle
        motifPaint.color = (CoverStyle.TEXT_COLOR and 0x00FFFFFF) or (alpha shl 24)
    }

    private companion object {
        const val FULLY_OPAQUE = 255
        const val LINE_SPACING = 1.12f
        const val SPINE_FILL_ALPHA = 34
        const val SPINE_LINE_ALPHA = 48
        const val ARC_ALPHA = 40
        const val DOT_ALPHA = 60
        const val LINE_ALPHA = 34

        fun textPaint(typeface: Typeface): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                color = CoverStyle.TEXT_COLOR
            }
    }
}
