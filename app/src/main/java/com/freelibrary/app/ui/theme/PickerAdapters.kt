package com.freelibrary.app.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.freelibrary.app.R
import com.google.android.material.color.MaterialColors

/** The typeface of [font]'s family, or null if it cannot be loaded. */
fun typefaceOf(
    context: Context,
    font: AppFont,
): Typeface? = runCatching { ResourcesCompat.getFont(context, font.body) }.getOrNull()

private abstract class PickerAdapter<T>(
    private val items: List<T>,
    private val selected: Int,
) : BaseAdapter() {
    override fun getCount(): Int = items.size

    override fun getItem(position: Int): T = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val row =
            (convertView as? TextView)
                ?: LayoutInflater.from(parent.context).inflate(R.layout.item_picker_row, parent, false) as TextView
        bind(row, items[position])
        if (position == selected) row.setTextColor(MaterialColors.getColor(row, androidx.appcompat.R.attr.colorPrimary))
        return row
    }

    /** Fills [row] for [item]; the selected row is tinted afterwards. */
    abstract fun bind(
        row: TextView,
        item: T,
    )
}

/** A list of fonts, each name written in its own face. A null font is the book's own, shown with [bookOwnLabel]. */
fun fontAdapter(
    context: Context,
    fonts: List<AppFont?>,
    bookOwnLabel: String,
    selected: Int,
): BaseAdapter =
    object : PickerAdapter<AppFont?>(fonts, selected) {
        override fun bind(
            row: TextView,
            item: AppFont?,
        ) {
            row.text = item?.label ?: bookOwnLabel
            row.typeface = item?.let { typefaceOf(context, it) } ?: Typeface.DEFAULT
            row.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface))
        }
    }

/** A list of text colours, each with a round swatch in its colour. */
fun colourAdapter(
    context: Context,
    colours: List<TextColour>,
    page: ReaderPage,
    selected: Int,
): BaseAdapter =
    object : PickerAdapter<TextColour>(colours, selected) {
        override fun bind(
            row: TextView,
            item: TextColour,
        ) {
            row.setText(item.label)
            row.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface))
            row.setCompoundDrawablesRelativeWithIntrinsicBounds(swatch(context, item.resolve(context, page)), null, null, null)
        }
    }

/** A list of reader pages, each with a round swatch in its paper colour. */
fun themeAdapter(
    context: Context,
    themes: List<ReaderTheme>,
    selected: Int,
): BaseAdapter =
    object : PickerAdapter<ReaderTheme>(themes, selected) {
        override fun bind(
            row: TextView,
            item: ReaderTheme,
        ) {
            row.setText(item.label)
            row.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface))
            row.setCompoundDrawablesRelativeWithIntrinsicBounds(swatch(context, item.page(row).background), null, null, null)
        }
    }

/** A round swatch of [colour] with a thin outline, so a dark colour is still visible on a dark page. */
fun swatch(
    context: Context,
    colour: Int,
): GradientDrawable {
    val density = context.resources.displayMetrics.density
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(colour)
        val outline = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, colour)
        setStroke(density.toInt().coerceAtLeast(1), outline)
        setSize((SWATCH_DP * density).toInt(), (SWATCH_DP * density).toInt())
    }
}

private const val SWATCH_DP = 24
