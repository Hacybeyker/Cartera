package com.hacybeyker.cartera.legacy.ui.pinpad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.android.material.color.MaterialColors
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.pin.domain.PinBounds

private const val KEY_OVERLAY = 0.08f
private const val PRESSED_OVERLAY = 0.24f
private const val DISABLED_ALPHA = 128

/** Dibuja las teclas con los colores del tema, así el teclado sigue el modo claro y el oscuro. */
internal class PinPadPainter(context: Context) {
    private val resources = context.resources
    private val margin = resources.getDimension(R.dimen.legacy_pin_key_margin)
    private val radius = resources.getDimension(R.dimen.legacy_pin_key_radius)
    private val digitSize = resources.getDimension(R.dimen.legacy_pin_key_text_size)
    private val labelSize = resources.getDimension(R.dimen.legacy_pin_delete_text_size)
    private val surface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.WHITE)
    private val onSurface = MaterialColors.getColor(
        context,
        com.google.android.material.R.attr.colorOnSurface,
        Color.BLACK
    )
    private val primary = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, Color.BLUE)
    private val keyColor = MaterialColors.layer(surface, onSurface, KEY_OVERLAY)
    private val pressedColor = MaterialColors.layer(surface, primary, PRESSED_OVERLAY)
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = onSurface
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    fun draw(canvas: Canvas, bounds: PinBounds, label: String, isDigit: Boolean, pressed: Boolean, enabled: Boolean) {
        val alpha = if (enabled) 255 else DISABLED_ALPHA
        rect.set(
            bounds.left + margin,
            bounds.top + margin,
            bounds.right - margin,
            bounds.bottom - margin
        )
        keyPaint.color = if (pressed) pressedColor else keyColor
        keyPaint.alpha = alpha
        canvas.drawRoundRect(rect, radius, radius, keyPaint)
        textPaint.textSize = if (isDigit) digitSize else labelSize
        textPaint.alpha = alpha
        val baseline = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }
}
