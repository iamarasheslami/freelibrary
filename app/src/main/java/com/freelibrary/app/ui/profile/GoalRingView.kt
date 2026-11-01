package com.freelibrary.app.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.freelibrary.app.R

/**
 * A ring that fills clockwise from the top as the yearly goal is reached. The colours and the
 * thickness come from the layout; the number inside the ring is a separate view, so this one only draws.
 */
class GoalRingView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private val trackPaint = ringPaint()
        private val progressPaint = ringPaint()
        private val bounds = RectF()
        private var progress = 0f

        init {
            val attributes = context.obtainStyledAttributes(attrs, R.styleable.GoalRingView, defStyleAttr, 0)
            try {
                trackPaint.color = attributes.getColor(R.styleable.GoalRingView_trackColor, Color.LTGRAY)
                progressPaint.color = attributes.getColor(R.styleable.GoalRingView_ringColor, Color.DKGRAY)
                val defaultWidth = DEFAULT_STROKE_DP * resources.displayMetrics.density
                val width = attributes.getDimension(R.styleable.GoalRingView_ringStroke, defaultWidth)
                trackPaint.strokeWidth = width
                progressPaint.strokeWidth = width
            } finally {
                attributes.recycle()
            }
        }

        /** Sets how much of the ring is filled, from 0 to 1; values outside that are cut to it. */
        fun setProgress(fraction: Float) {
            val kept = fraction.coerceIn(0f, 1f)
            if (kept == progress) return
            progress = kept
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val inset = trackPaint.strokeWidth / 2
            bounds.set(inset, inset, width - inset, height - inset)
            canvas.drawArc(bounds, 0f, FULL_CIRCLE, false, trackPaint)
            if (progress > 0f) {
                canvas.drawArc(bounds, START_ANGLE, FULL_CIRCLE * progress, false, progressPaint)
            }
        }

        private fun ringPaint() =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }

        private companion object {
            const val FULL_CIRCLE = 360f
            const val START_ANGLE = -90f
            const val DEFAULT_STROKE_DP = 8
        }
    }
