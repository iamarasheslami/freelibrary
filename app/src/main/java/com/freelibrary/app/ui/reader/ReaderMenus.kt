package com.freelibrary.app.ui.reader

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.freelibrary.app.R
import com.freelibrary.app.data.reader.ReaderStyle
import com.freelibrary.app.databinding.PopupTextSizeBinding
import com.google.android.material.slider.Slider

private const val STEP_PERCENT = 10

/**
 * Shows the text size control under a reader's toolbar: a bar to drag, with a minus and a plus
 * button at its ends. The buttons do what the reader's larger and smaller text actions do
 * ([onSmaller], [onLarger]). The bar changes the size when the finger lifts, so the page is laid out
 * once and not on every step of the drag; the size in percent shows while dragging. [current] is the
 * size now, in percent, and [onSet] is asked for a size picked on the bar.
 */
fun showTextSizeControl(
    anchor: View,
    current: () -> Int,
    onLarger: () -> Unit,
    onSmaller: () -> Unit,
    onSet: (Int) -> Unit,
) {
    val binding = PopupTextSizeBinding.inflate(LayoutInflater.from(anchor.context))
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

    PopupWindow(binding.root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        .apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = anchor.resources.displayMetrics.density * 8
            showAsDropDown(anchor, 0, 0, Gravity.END)
        }
}
