package com.freelibrary.app.ui.reader

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.freelibrary.app.R
import com.freelibrary.app.data.reader.ReaderStyle
import com.freelibrary.app.databinding.PopupTextStyleBinding
import com.freelibrary.app.ui.theme.AppFont
import com.freelibrary.app.ui.theme.ReaderTheme
import com.freelibrary.app.ui.theme.TextColour
import com.freelibrary.app.ui.theme.colourAdapter
import com.freelibrary.app.ui.theme.fontAdapter
import com.freelibrary.app.ui.theme.swatch
import com.freelibrary.app.ui.theme.themeAdapter
import com.google.android.material.slider.Slider

private const val STEP_PERCENT = 10

/** The font drop-down of the text panel: the font now, whether a book's own font is an entry, and what to do with a pick. */
class FontChoice(
    val current: () -> AppFont?,
    val includeBookOwn: Boolean,
    val onPick: (AppFont?) -> Unit,
)

/** The page drop-down of the text panel: the page theme now and what to do with a pick. */
class ThemeChoice(
    val current: () -> ReaderTheme,
    val onPick: (ReaderTheme) -> Unit,
)

/** The colour drop-down of the text panel: the colour now and what to do with a pick. */
class ColourChoice(
    val current: () -> TextColour,
    val onPick: (TextColour) -> Unit,
)

/**
 * Shows the text panel under a reader's toolbar. At the top is the text size: a bar to drag, with a minus and a plus
 * button at its ends. The buttons do what the reader's larger and smaller text actions do ([onSmaller], [onLarger]). The bar
 * changes the size when the finger lifts, so the page is laid out once and not on every step of the drag; the size in
 * percent shows while dragging. [current] is the size now, in percent, and [onSet] is asked for a size picked on the bar.
 * Under the bar are drop-downs for the page ([theme]), the font ([font]) and the text colour ([colour]).
 */
fun showTextStyleControl(
    anchor: View,
    current: () -> Int,
    onLarger: () -> Unit,
    onSmaller: () -> Unit,
    onSet: (Int) -> Unit,
    theme: ThemeChoice,
    font: FontChoice,
    colour: ColourChoice,
) {
    val binding = PopupTextStyleBinding.inflate(LayoutInflater.from(anchor.context))
    val slider = binding.textSizeSlider
    slider.valueFrom = ReaderStyle.MIN_TEXT_PERCENT.toFloat()
    slider.valueTo = ReaderStyle.MAX_TEXT_PERCENT.toFloat()
    slider.stepSize = STEP_PERCENT.toFloat()
    slider.setLabelFormatter { anchor.context.getString(R.string.reader_text_size_percent, it.toInt()) }

    fun show(percent: Int) {
        slider.value = percent.toFloat().coerceIn(slider.valueFrom, slider.valueTo)
        binding.textSizeValue.text = anchor.context.getString(R.string.reader_text_size_percent, slider.value.toInt())
    }
    show(current())

    var dragging = false
    slider.addOnSliderTouchListener(
        object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                dragging = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                dragging = false
                onSet(slider.value.toInt())
            }
        },
    )
    slider.addOnChangeListener { changed, value, fromUser ->
        binding.textSizeValue.text = anchor.context.getString(R.string.reader_text_size_percent, value.toInt())
        // A change that is not a drag, such as one made with a screen reader's gestures, applies at once.
        if (fromUser && !dragging) onSet(changed.value.toInt())
    }
    binding.textSizeSmaller.setOnClickListener {
        onSmaller()
        show(current())
    }
    binding.textSizeLarger.setOnClickListener {
        onLarger()
        show(current())
    }

    val context = anchor.context
    val arrow = ContextCompat.getDrawable(context, R.drawable.ic_arrow_drop_down)

    fun page() = theme.current().page(anchor)

    fun showTheme() {
        val chosen = theme.current()
        binding.readerPageField.setText(chosen.label)
        binding.readerPageField.setCompoundDrawablesRelativeWithIntrinsicBounds(
            swatch(context, chosen.page(anchor).background),
            null,
            arrow,
            null,
        )
    }

    fun showFont() {
        binding.readerFontField.text = font.current()?.label ?: context.getString(R.string.font_book_own)
        binding.readerFontField.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null)
    }

    fun showColour() {
        val chosen = colour.current()
        binding.readerColourField.setText(chosen.label)
        binding.readerColourField.setCompoundDrawablesRelativeWithIntrinsicBounds(
            swatch(context, chosen.resolve(context, page())),
            null,
            arrow,
            null,
        )
    }
    showTheme()
    showFont()
    showColour()

    binding.readerPageField.setOnClickListener {
        val themes = ReaderTheme.entries
        val adapter = themeAdapter(context, themes, themes.indexOf(theme.current()))
        dropDown(binding.readerPageField, adapter) { position ->
            theme.onPick(themes[position])
            showTheme()
            showColour()
        }
    }
    binding.readerFontField.setOnClickListener {
        val fonts: List<AppFont?> = (if (font.includeBookOwn) listOf(null) else emptyList()) + AppFont.entries
        val adapter = fontAdapter(context, fonts, context.getString(R.string.font_book_own), fonts.indexOf(font.current()))
        dropDown(binding.readerFontField, adapter) { position ->
            font.onPick(fonts[position])
            showFont()
        }
    }
    binding.readerColourField.setOnClickListener {
        val colours = TextColour.entries
        val adapter = colourAdapter(context, colours, page(), colours.indexOf(colour.current()))
        dropDown(binding.readerColourField, adapter) { position ->
            colour.onPick(colours[position])
            showColour()
        }
    }

    PopupWindow(binding.root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        .apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = anchor.resources.displayMetrics.density * 8
            showAsDropDown(anchor, 0, 0, Gravity.END)
        }
}

/** Opens a list under [field] as wide as the field and calls [onPick] with the position tapped. */
private fun dropDown(
    field: TextView,
    adapter: ListAdapter,
    onPick: (Int) -> Unit,
) {
    ListPopupWindow(field.context).apply {
        anchorView = field
        width = field.width
        isModal = true
        setAdapter(adapter)
        setOnItemClickListener { _, _, position, _ ->
            dismiss()
            onPick(position)
        }
        show()
    }
}
