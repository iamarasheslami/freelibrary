package com.freelibrary.app.ui.profile.avatar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import com.freelibrary.app.domain.model.AvatarDesign
import com.freelibrary.app.domain.model.AvatarSlot

/**
 * A round, square-sized view that shows an avatar, or a plain colour when it is only a colour choice. It draws with
 * [AvatarRenderer], so the creator's preview, its small previews and the saved picture all look the same.
 */
class AvatarView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private var design: AvatarDesign? = null
        private var focus: AvatarSlot? = null
        private var swatch: Int? = null

        init {
            outlineProvider =
                object : ViewOutlineProvider() {
                    override fun getOutline(
                        view: View,
                        outline: Outline,
                    ) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            clipToOutline = true
        }

        /** Shows [design], enlarged around the part of the face that [focus] names, if any. */
        fun showDesign(
            design: AvatarDesign,
            focus: AvatarSlot? = null,
        ) {
            if (this.design == design && this.focus == focus && swatch == null) return
            this.design = design
            this.focus = focus
            swatch = null
            invalidate()
        }

        /** Shows a plain colour ([rgb], no alpha), for a colour choice. */
        fun showSwatch(rgb: Int) {
            if (swatch == rgb && design == null) return
            swatch = rgb
            design = null
            invalidate()
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val side = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
            setMeasuredDimension(side, side)
        }

        override fun onDraw(canvas: Canvas) {
            val shown = design
            val colour = swatch
            when {
                shown != null -> AvatarRenderer.draw(canvas, width.toFloat(), shown, focus)
                colour != null -> canvas.drawColor(OPAQUE or colour)
            }
        }

        private companion object {
            const val OPAQUE = 0xFF000000.toInt()
        }
    }
