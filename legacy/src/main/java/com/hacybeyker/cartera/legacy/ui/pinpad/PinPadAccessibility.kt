package com.hacybeyker.cartera.legacy.ui.pinpad

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import com.hacybeyker.cartera.legacy.pin.domain.PinSlot

private const val NO_VIEW = -2
private const val HOST = AccessibilityNodeProviderCompat.HOST_VIEW_ID
private const val KEY_CLASS = "android.widget.Button"

/**
 * Las teclas del teclado de la clave como nodos virtuales de accesibilidad. Cada tecla expone únicamente su texto
 * y la acción de clic: ni identificador ni descripción, así que una herramienta solo puede elegirla por su texto. Los
 * clics llegan a la misma lógica que los toques ([PinPadView.press]).
 */
internal class PinPadAccessibility(private val pad: PinPadView) : AccessibilityDelegateCompat() {

    private val manager = pad.context.getSystemService(AccessibilityManager::class.java)
    private var focusedId = NO_VIEW
    private var hoveredId = NO_VIEW

    private val provider = object : AccessibilityNodeProviderCompat() {
        override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfoCompat? =
            if (virtualViewId == HOST) hostInfo() else slotOf(virtualViewId)?.let(::slotInfo)

        override fun performAction(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
            if (virtualViewId == HOST) {
                return pad.performAccessibilityAction(action, arguments)
            }
            return slotOf(virtualViewId)?.let { performSlotAction(it, action) } ?: false
        }

        override fun findFocus(focus: Int): AccessibilityNodeInfoCompat? =
            if (focus == AccessibilityNodeInfoCompat.FOCUS_ACCESSIBILITY) {
                slotOf(focusedId)?.let(::slotInfo)
            } else {
                null
            }
    }

    override fun getAccessibilityNodeProvider(host: View): AccessibilityNodeProviderCompat = provider

    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        pad.slots.forEach { info.addChild(host, it.id) }
    }

    /** Las teclas cambiaron de lugar o de dígito: los servicios de accesibilidad vuelven a leer el árbol. */
    fun invalidateAll() {
        focusedId = NO_VIEW
        send(HOST, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }

    fun dispatchHover(event: MotionEvent): Boolean {
        if (!manager.isEnabled || !manager.isTouchExplorationEnabled) return false
        val previous = hoveredId
        val over = if (event.action == MotionEvent.ACTION_HOVER_EXIT) NO_VIEW else pad.slotAt(event.x, event.y)?.id
        hoveredId = over ?: NO_VIEW
        if (hoveredId != previous) {
            send(hoveredId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
            send(previous, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT)
        }
        return over != null || previous != NO_VIEW
    }

    private fun slotOf(id: Int): PinSlot? = pad.slots.getOrNull(id)

    private fun hostInfo(): AccessibilityNodeInfoCompat {
        val info = AccessibilityNodeInfoCompat.obtain(pad)
        pad.onInitializeAccessibilityNodeInfo(info.unwrap())
        return info
    }

    private fun slotInfo(slot: PinSlot): AccessibilityNodeInfoCompat {
        val node = AccessibilityNodeInfoCompat.obtain()
        node.className = KEY_CLASS
        node.packageName = pad.context.packageName
        node.text = pad.labelOf(slot)
        node.isEnabled = pad.isEnabled
        node.isFocusable = true
        node.isClickable = true
        node.addAction(AccessibilityActionCompat.ACTION_CLICK)
        node.setParent(pad)
        node.setSource(pad, slot.id)
        node.isAccessibilityFocused = focusedId == slot.id
        node.addAction(
            if (focusedId == slot.id) {
                AccessibilityActionCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS
            } else {
                AccessibilityActionCompat.ACTION_ACCESSIBILITY_FOCUS
            }
        )
        placeIn(node, pad.boundsOf(slot))
        return node
    }

    private fun placeIn(node: AccessibilityNodeInfoCompat, bounds: Rect) {
        val visible = Rect()
        if (pad.getLocalVisibleRect(visible) && bounds.intersect(visible)) node.isVisibleToUser = true
        node.setBoundsInParent(bounds)
        val origin = IntArray(2)
        pad.getLocationOnScreen(origin)
        bounds.offset(origin[0], origin[1])
        node.setBoundsInScreen(bounds)
    }

    private fun performSlotAction(slot: PinSlot, action: Int): Boolean = when (action) {
        AccessibilityNodeInfoCompat.ACTION_CLICK -> pad.isEnabled.also { enabled ->
            if (enabled) {
                pad.press(slot)
                send(slot.id, AccessibilityEvent.TYPE_VIEW_CLICKED)
            }
        }
        AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS -> setFocused(slot.id, true)
        AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS -> setFocused(slot.id, false)
        else -> false
    }

    private fun setFocused(id: Int, focused: Boolean): Boolean {
        if ((focusedId == id) == focused) return false
        focusedId = if (focused) id else NO_VIEW
        send(
            id,
            if (focused) {
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
            } else {
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
            }
        )
        return true
    }

    // `obtain` es la única fábrica de eventos anterior a la API 30 (minSdk 26).
    @Suppress("DEPRECATION")
    private fun send(virtualViewId: Int, type: Int) {
        if (virtualViewId == NO_VIEW || !manager.isEnabled) return
        val event = AccessibilityEvent.obtain(type)
        event.className =
            if (virtualViewId == HOST) pad.javaClass.name else KEY_CLASS
        event.packageName = pad.context.packageName
        event.setSource(pad, virtualViewId)
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            event.contentChangeTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
        }
        pad.parent?.requestSendAccessibilityEvent(pad, event)
    }
}
