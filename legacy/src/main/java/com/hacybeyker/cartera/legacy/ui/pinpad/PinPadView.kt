package com.hacybeyker.cartera.legacy.ui.pinpad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.pin.domain.PIN_PAD_ROWS
import com.hacybeyker.cartera.legacy.pin.domain.PinKey
import com.hacybeyker.cartera.legacy.pin.domain.PinPadLayout
import com.hacybeyker.cartera.legacy.pin.domain.PinPadOrder
import com.hacybeyker.cartera.legacy.pin.domain.PinSlot

/**
 * El teclado de la clave, dibujado con Canvas. No tiene `android:id` ni descripción por tecla (S2): cada tecla es un
 * nodo virtual de accesibilidad que solo expone su texto ([PinPadAccessibility]). Los toques y los clics de
 * accesibilidad usan la misma geometría ([PinPadLayout]) y la misma salida, [onKeyPressed].
 */
internal class PinPadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    var onKeyPressed: (PinKey) -> Unit = {}

    /** Los diez dígitos en el orden en que se muestran, por filas; el último va abajo, junto a Borrar. */
    var order: List<Int> = PinPadOrder.FIXED
        set(value) {
            if (field == value) return
            field = value
            pad = PinPadLayout(value)
            invalidate()
            accessibility.invalidateAll()
        }

    val slots: List<PinSlot> get() = pad.slots

    private var pad = PinPadLayout(PinPadOrder.FIXED)
    private val painter = PinPadPainter(context)
    private val accessibility = PinPadAccessibility(this)
    private val deleteLabel = context.getString(R.string.legacy_pin_delete)
    private var pressedSlot: PinSlot? = null
    private var tappedSlot: PinSlot? = null

    init {
        ViewCompat.setAccessibilityDelegate(this, accessibility)
    }

    fun slotAt(x: Float, y: Float): PinSlot? = pad.slotAt(x.toInt(), y.toInt(), width, height)

    fun labelOf(slot: PinSlot): String = when (val key = slot.key) {
        is PinKey.Digit -> key.value.toString()
        PinKey.Delete -> deleteLabel
    }

    /** Los límites de la tecla en la vista; es el rectángulo que ven los toques y los servicios de accesibilidad. */
    fun boundsOf(slot: PinSlot): Rect = with(pad.boundsOf(slot, width, height)) { Rect(left, top, right, bottom) }

    fun press(slot: PinSlot) = onKeyPressed(slot.key)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = resources.getDimensionPixelSize(R.dimen.legacy_pin_pad_max_width)
        val width = minOf(MeasureSpec.getSize(widthMeasureSpec), maxWidth)
        val height = PIN_PAD_ROWS * resources.getDimensionPixelSize(R.dimen.legacy_pin_key_height)
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        pad.slots.forEach { slot ->
            painter.draw(
                canvas = canvas,
                bounds = pad.boundsOf(slot, width, height),
                label = labelOf(slot),
                isDigit = slot.key is PinKey.Digit,
                pressed = slot == pressedSlot,
                enabled = isEnabled
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        val slot = slotAt(event.x, event.y)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> pressedSlot = slot
            MotionEvent.ACTION_MOVE -> if (slot != pressedSlot) pressedSlot = null
            MotionEvent.ACTION_UP -> release(slot)
            MotionEvent.ACTION_CANCEL -> pressedSlot = null
        }
        invalidate()
        return event.actionMasked != MotionEvent.ACTION_DOWN || slot != null
    }

    override fun performClick(): Boolean {
        val handled = super.performClick()
        tappedSlot?.let(::press)
        tappedSlot = null
        return handled
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        accessibility.dispatchHover(event) || super.dispatchHoverEvent(event)

    private fun release(slot: PinSlot?) {
        if (slot != null && slot == pressedSlot) {
            tappedSlot = slot
            performClick()
        }
        pressedSlot = null
    }
}
