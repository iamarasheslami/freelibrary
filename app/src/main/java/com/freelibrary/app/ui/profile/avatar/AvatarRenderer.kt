package com.freelibrary.app.ui.profile.avatar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import com.freelibrary.app.domain.model.AvatarDesign
import com.freelibrary.app.domain.model.AvatarSlot

/**
 * Draws an [AvatarDesign] from the shapes in [AvatarArt]: first the background, then the figure, back to front. The path data is
 * parsed once and kept, so drawing the many small previews of the creator stays cheap.
 */
internal object AvatarRenderer {
    private class Shape(
        val path: Path,
        val colour: ArtColour,
        val strokeWidth: Float,
    )

    private class Layers(
        val back: List<Shape>,
        val front: List<Shape>,
    )

    private fun compile(shapes: List<ArtShape>) =
        shapes.map { Shape(PathParser.createPathFromPathData(it.path), it.colour, it.strokeWidth) }

    private fun compile(layers: ArtLayers) = Layers(compile(layers.back), compile(layers.front))

    private val body = PathParser.createPathFromPathData(AvatarArt.BODY)
    private val neck = PathParser.createPathFromPathData(AvatarArt.NECK)
    private val earLeft = PathParser.createPathFromPathData(AvatarArt.EAR_LEFT)
    private val earRight = PathParser.createPathFromPathData(AvatarArt.EAR_RIGHT)
    private val heads = AvatarArt.heads.map { PathParser.createPathFromPathData(it) }
    private val noses = AvatarArt.noses.map(::compile)
    private val hair = AvatarArt.hair.map(::compile)
    private val outfits = AvatarArt.outfits.map(::compile)
    private val eyes = AvatarArt.eyes.map(::compile)
    private val mouths = AvatarArt.mouths.map(::compile)
    private val glasses = AvatarArt.glasses.map(::compile)
    private val hats = AvatarArt.hats.map(::compile)
    private val earrings = AvatarArt.earrings.map(::compile)
    private val brows = compile(AvatarArt.brows)
    private val blush = compile(AvatarArt.blush)

    /** How far a small preview of [focus] is enlarged, and the point of the face it is enlarged around. */
    private class Zoom(
        val scale: Float,
        val x: Float,
        val y: Float,
    )

    private fun zoomFor(focus: AvatarSlot?): Zoom? =
        when (focus) {
            AvatarSlot.NOSE -> Zoom(2.3f, 100f, 104f)
            AvatarSlot.EYES -> Zoom(1.9f, 100f, 94f)
            AvatarSlot.MOUTH -> Zoom(2.1f, 100f, 108f)
            AvatarSlot.GLASSES -> Zoom(1.7f, 100f, 94f)
            AvatarSlot.EARRINGS -> Zoom(1.5f, 100f, 102f)
            else -> null
        }

    /**
     * Draws [design] filling a square of [size] pixels whose top left corner is the canvas origin. A small detail such as the
     * nose or the earrings is hard to see in a tiny preview, so [focus] names the slot a preview is about and the face is
     * enlarged around it.
     */
    fun draw(
        canvas: Canvas,
        size: Float,
        design: AvatarDesign,
        focus: AvatarSlot? = null,
    ) {
        val palette = Palette(design)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.save()
        canvas.scale(size / AvatarArt.BOX, size / AvatarArt.BOX)
        paint.style = Paint.Style.FILL
        paint.color = opaque(palette.background)
        canvas.drawRect(0f, 0f, AvatarArt.BOX, AvatarArt.BOX, paint)

        canvas.translate(AvatarArt.FIGURE_CENTRE_X, AvatarArt.FIGURE_CENTRE_Y)
        canvas.scale(AvatarArt.FIGURE_SCALE, AvatarArt.FIGURE_SCALE)
        canvas.translate(-AvatarArt.FIGURE_CENTRE_X, -AvatarArt.FIGURE_CENTRE_Y)
        zoomFor(focus)?.let {
            canvas.translate(it.x, it.y)
            canvas.scale(it.scale, it.scale)
            canvas.translate(-it.x, -it.y)
        }

        val hairLayers = hair[design[AvatarSlot.HAIR]]
        val hat = hats[design[AvatarSlot.HAT]]
        val outfit = outfits[design[AvatarSlot.OUTFIT]]
        drawShapes(canvas, paint, hairLayers.back, palette)
        drawShapes(canvas, paint, hat.back, palette)
        drawShapes(canvas, paint, outfit.back, palette)
        fill(canvas, paint, neck, palette.skinNeck)
        fill(canvas, paint, body, palette.outfit)
        drawShapes(canvas, paint, outfit.front, palette)
        fill(canvas, paint, earLeft, palette.skin)
        fill(canvas, paint, earRight, palette.skin)
        fill(canvas, paint, heads[design[AvatarSlot.FACE_SHAPE]], palette.skin)
        drawShapes(canvas, paint, blush, palette)
        drawShapes(canvas, paint, noses[design[AvatarSlot.NOSE]], palette)
        drawShapes(canvas, paint, eyes[design[AvatarSlot.EYES]], palette)
        drawShapes(canvas, paint, brows, palette)
        drawShapes(canvas, paint, mouths[design[AvatarSlot.MOUTH]], palette)
        drawShapes(canvas, paint, glasses[design[AvatarSlot.GLASSES]], palette)
        drawShapes(canvas, paint, hairLayers.front, palette)
        drawShapes(canvas, paint, earrings[design[AvatarSlot.EARRINGS]], palette)
        drawShapes(canvas, paint, hat.front, palette)
        canvas.restore()
    }

    /** The avatar as a square picture [size] pixels wide. */
    fun toBitmap(
        design: AvatarDesign,
        size: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), size.toFloat(), design)
        return bitmap
    }

    private fun fill(
        canvas: Canvas,
        paint: Paint,
        path: Path,
        colour: Int,
    ) {
        paint.style = Paint.Style.FILL
        paint.color = opaque(colour)
        canvas.drawPath(path, paint)
    }

    private fun drawShapes(
        canvas: Canvas,
        paint: Paint,
        shapes: List<Shape>,
        palette: Palette,
    ) {
        for (shape in shapes) {
            paint.color = opaque(palette.colour(shape.colour))
            if (shape.strokeWidth > 0f) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = shape.strokeWidth
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
            } else {
                paint.style = Paint.Style.FILL
            }
            canvas.drawPath(shape.path, paint)
        }
    }

    private fun opaque(rgb: Int): Int = OPAQUE or rgb

    private const val OPAQUE = 0xFF000000.toInt()
}

/** The real colours of one design: the chosen ones and the shades worked out from them. */
internal class Palette(
    design: AvatarDesign,
) {
    val skin = AvatarArt.skinTones[design[AvatarSlot.SKIN]]
    val hair = AvatarArt.hairColours[design[AvatarSlot.HAIR_COLOUR]]
    val outfit = AvatarArt.outfitColours[design[AvatarSlot.OUTFIT_COLOUR]]
    val iris = AvatarArt.eyeColours[design[AvatarSlot.EYE_COLOUR]]
    val hat = AvatarArt.hatColours[design[AvatarSlot.HAT_COLOUR]]
    val background = AvatarArt.backgrounds[design[AvatarSlot.BACKGROUND]]
    val skinNeck = shade(skin, NECK_SHADE)

    fun colour(colour: ArtColour): Int =
        when (colour) {
            ArtColour.INK -> AvatarArt.INK_RGB
            ArtColour.WHITE -> WHITE
            ArtColour.SKIN -> skin
            ArtColour.SKIN_SHADE -> shade(skin, SKIN_SHADE)
            ArtColour.HAIR -> hair
            ArtColour.HAIR_DARK -> if (hair > DARK_HAIR_LIMIT) shade(hair, HAIR_SHADE) else VERY_DARK_BROW
            ArtColour.OUTFIT -> outfit
            ArtColour.OUTFIT_DARK -> shade(outfit, OUTFIT_SHADE)
            ArtColour.SCARF -> mix(outfit, WHITE, SCARF_LIGHTEN)
            ArtColour.BLUSH -> BLUSH
            ArtColour.TONGUE -> TONGUE
            ArtColour.IRIS -> iris
            ArtColour.HAT -> hat
            ArtColour.HAT_DARK -> shade(hat, OUTFIT_SHADE)
            ArtColour.METAL -> METAL
        }

    companion object {
        private const val WHITE = 0xFFFFFF
        private const val BLUSH = 0xE58C8C
        private const val TONGUE = 0xE27C86
        private const val METAL = 0xD9A441
        private const val VERY_DARK_BROW = 0x3A3438
        private const val DARK_HAIR_LIMIT = 0x303030
        private const val NECK_SHADE = 0.86f
        private const val SKIN_SHADE = 0.82f
        private const val HAIR_SHADE = 0.8f
        private const val OUTFIT_SHADE = 0.78f
        private const val SCARF_LIGHTEN = 0.4f
        private const val CHANNEL_MAX = 255

        /** [rgb] with every channel multiplied by [factor], kept within 0 to 255. */
        fun shade(
            rgb: Int,
            factor: Float,
        ): Int = combine { shift -> (((rgb shr shift) and CHANNEL_MAX) * factor).toInt().coerceIn(0, CHANNEL_MAX) }

        /** [first] with [share] (0 to 1) of [second] mixed in. */
        fun mix(
            first: Int,
            second: Int,
            share: Float,
        ): Int =
            combine { shift ->
                ((((first shr shift) and CHANNEL_MAX) * (1 - share)) + (((second shr shift) and CHANNEL_MAX) * share)).toInt()
            }

        /** Builds a colour from the red, green and blue channels [channel] makes, given each channel's bit shift. */
        private fun combine(channel: (Int) -> Int): Int =
            (channel(RED_SHIFT) shl RED_SHIFT) or (channel(GREEN_SHIFT) shl GREEN_SHIFT) or channel(0)

        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
    }
}
