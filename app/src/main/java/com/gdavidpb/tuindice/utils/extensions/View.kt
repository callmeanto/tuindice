package com.gdavidpb.tuindice.utils.extensions

import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gdavidpb.tuindice.utils.TIME_DELAY_CLICK_ONCE
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*

fun View.onClickOnce(onClick: () -> Unit) {
    setOnClickListener(object : View.OnClickListener {
        override fun onClick(view: View) {
            view.setOnClickListener(null)

            also { listener ->
                CoroutineScope(Dispatchers.Main).launch {
                    onClick()

                    withContext(Dispatchers.IO) { delay(TIME_DELAY_CLICK_ONCE) }

                    view.setOnClickListener(listener)
                }
            }
        }
    })
}

fun SeekBar.onSeekBarChange(listener: (progress: Int, fromUser: Boolean) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            listener(progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {

        }
    })
}

fun RecyclerView.onScrollStateChanged(listener: (newState: Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            listener(newState)
        }
    })
}

fun TextInputLayout.selectAll() = editText?.selectAll()

fun TextView.drawables(
        left: Drawable? = null,
        top: Drawable? = null,
        right: Drawable? = null,
        bottom: Drawable? = null) = setCompoundDrawables(left, top, right, bottom)

fun TextView.drawables(
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0) = setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom)

fun EditText.onTextChanged(event: (CharSequence, Int, Int, Int) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            event(s ?: "", start, before, count)
        }

        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    })
}

fun TextInputLayout.text(value: String? = null) = value?.also { editText?.setText(it) }
        ?: "${editText?.text}"

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.visibleIf(value: Boolean, elseValue: Int = View.GONE) {
    visibility = if (value) View.VISIBLE else elseValue
}