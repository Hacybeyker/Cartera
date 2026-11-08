package com.hacybeyker.cartera.legacy.ui.pinpad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.R as AppCompatR
import com.google.android.material.color.MaterialColors
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.pin.domain.PIN_LENGTH

/** Los cuatro puntos de avance de la clave: llenos los que ya se tecleó, vacíos el resto. Nunca muestra un dígito. */
internal class PinDotsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    var filled: Int = 0
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private val diameter = resources.getDimension(R.dimen.legacy_pin_dot_size)
    private val gap = resources.getDimension(R.dimen.legacy_pin_dot_gap)
    private val stroke = resources.getDimension(R.dimen.legacy_pin_dot_stroke)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(context, AppCompatR.attr.colorPrimary, Color.BLUE)
        strokeWidth = stroke
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = PIN_LENGTH * diameter + (PIN_LENGTH - 1) * gap + stroke
        setMeasuredDimension(
            resolveSize(width.toInt(), widthMeasureSpec),
            resolveSize((diameter + stroke).toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val radius = (diameter - stroke) / 2
        val cy = height / 2f
        val start = (width - (PIN_LENGTH * diameter + (PIN_LENGTH - 1) * gap)) / 2
        for (index in 0 until PIN_LENGTH) {
            paint.style = if (index < filled) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
            canvas.drawCircle(start + diameter / 2 + index * (diameter + gap), cy, radius, paint)
        }
    }
}
