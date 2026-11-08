package com.hacybeyker.cartera.legacy.ui

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.legacy.pin.domain.PinKey
import com.hacybeyker.cartera.legacy.pin.domain.PinPadOrder
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.ui.pinpad.PinPadView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1080dp-h1920dp")
class PinPadViewTest {
    private val presenters = TestPresenters().apply {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("100200")
        flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        flow.chooseAccount(Samples.gastos)
    }
    private val pressed = mutableListOf<PinKey>()

    private fun launchPad(seed: Int = 0): PinPadView {
        presenters.padSeed.seed = seed
        val activity = launchScreen(presenters) { PinFragment() }.get()
        return activity.window.decorView.findViewWithTag<PinPadView>("legacy_pin_pad").also {
            it.onKeyPressed = pressed::add
        }
    }

    private fun provider(pad: PinPadView): AccessibilityNodeProvider = checkNotNull(pad.accessibilityNodeProvider)

    private fun nodes(pad: PinPadView): List<AccessibilityNodeInfo> =
        (0..10).map { checkNotNull(provider(pad).createAccessibilityNodeInfo(it)) }

    private fun tap(pad: PinPadView, x: Float, y: Float) {
        val down = SystemClock.uptimeMillis()
        pad.dispatchTouchEvent(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0))
        pad.dispatchTouchEvent(MotionEvent.obtain(down, down + 40, MotionEvent.ACTION_UP, x, y, 0))
    }

    private fun centerOf(pad: PinPadView, id: Int): Pair<Float, Float> {
        val bounds = pad.boundsOf(pad.slots[id])
        return bounds.exactCenterX() to bounds.exactCenterY()
    }

    @Test
    fun `the pad has no id, no description and no text of its own`() {
        val pad = launchPad()

        assertEquals(View.NO_ID, pad.id)
        assertNull(pad.contentDescription)
    }

    @Test
    fun `the host node lists eleven virtual keys as children`() {
        val pad = launchPad()

        val host = checkNotNull(provider(pad).createAccessibilityNodeInfo(View.NO_ID))

        assertEquals(11, host.childCount)
        assertNull(host.text)
        assertNull(host.contentDescription)
        assertNull(host.viewIdResourceName)
    }

    @Test
    fun `each key exposes only its text and the click action`() {
        val pad = launchPad()

        nodes(pad).forEach { node ->
            assertNull(node.viewIdResourceName)
            assertNull(node.contentDescription)
            assertNull(node.hintText)
            assertNull(node.stateDescription)
            assertNull(node.tooltipText)
            assertTrue(node.isClickable)
            assertTrue(node.isEnabled)
            assertTrue(node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
            assertTrue(node.text?.isNotEmpty() == true)
        }
    }

    @Test
    fun `with the fixed order the texts are 1 to 9, 0 and delete`() {
        val pad = launchPad()

        assertEquals(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "Borrar"),
            nodes(pad).map { it.text.toString() }
        )
    }

    @Test
    fun `with seed 7 the texts follow the permutation of the seed`() {
        val pad = launchPad(seed = 7)

        assertEquals(
            PinPadOrder.forSeed(7).map(Int::toString) + "Borrar",
            nodes(pad).map { it.text.toString() }
        )
        assertEquals(
            listOf("1", "2", "0", "4", "8", "5", "9", "6", "3", "7", "Borrar"),
            nodes(pad).map {
                it.text.toString()
            }
        )
    }

    @Test
    fun `the bounds of each node are the bounds of the key on the screen, so a tap on their center lands on it`() {
        val pad = launchPad(seed = 7)
        val origin = IntArray(2).also(pad::getLocationOnScreen)

        nodes(pad).forEachIndexed { id, node ->
            val onScreen = Rect().also(node::getBoundsInScreen)
            val inParent = Rect().also(node::getBoundsInParent)
            val expected = pad.boundsOf(pad.slots[id])
            assertEquals(expected, inParent)
            assertEquals(Rect(expected).apply { offset(origin[0], origin[1]) }, onScreen)
            assertTrue(node.isVisibleToUser)
            val slot = pad.slotAt(onScreen.exactCenterX() - origin[0], onScreen.exactCenterY() - origin[1])
            assertEquals(id, slot?.id)
        }
    }

    @Test
    fun `a virtual click sends the key of that node`() {
        val pad = launchPad(seed = 7)

        assertTrue(provider(pad).performAction(2, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertTrue(provider(pad).performAction(10, AccessibilityNodeInfo.ACTION_CLICK, null))

        assertEquals(listOf(PinKey.Digit(0), PinKey.Delete), pressed)
    }

    @Test
    fun `a touch on the center of a key sends the same key as the virtual click`() {
        val pad = launchPad(seed = 7)

        (0..10).forEach { id ->
            val (x, y) = centerOf(pad, id)
            tap(pad, x, y)
        }

        assertEquals(pad.slots.map { it.key }, pressed)
    }

    @Test
    fun `a touch in the gap or one that leaves the key sends nothing`() {
        val pad = launchPad()
        val gap = pad.width / 6f to pad.height * 7 / 8f
        tap(pad, gap.first, gap.second)
        val (x, y) = centerOf(pad, 0)
        val down = SystemClock.uptimeMillis()
        pad.dispatchTouchEvent(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0))
        val (farX, farY) = centerOf(pad, 8)
        pad.dispatchTouchEvent(MotionEvent.obtain(down, down + 20, MotionEvent.ACTION_MOVE, farX, farY, 0))
        pad.dispatchTouchEvent(MotionEvent.obtain(down, down + 40, MotionEvent.ACTION_UP, farX, farY, 0))

        assertEquals(emptyList<PinKey>(), pressed)
    }

    @Test
    fun `a disabled pad ignores touches and virtual clicks`() {
        val pad = launchPad()
        pad.isEnabled = false

        val (x, y) = centerOf(pad, 4)
        tap(pad, x, y)

        assertFalse(provider(pad).performAction(4, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(emptyList<PinKey>(), pressed)
        assertFalse(checkNotNull(provider(pad).createAccessibilityNodeInfo(4)).isEnabled)
    }

    @Test
    fun `accessibility focus moves between the keys`() {
        val pad = launchPad()

        assertTrue(provider(pad).performAction(3, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null))
        assertTrue(checkNotNull(provider(pad).createAccessibilityNodeInfo(3)).isAccessibilityFocused)
        assertEquals("4", provider(pad).findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.text?.toString())
        assertTrue(provider(pad).performAction(3, AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null))

        assertFalse(checkNotNull(provider(pad).createAccessibilityNodeInfo(3)).isAccessibilityFocused)
        assertNull(provider(pad).findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY))
    }

    @Test
    fun `changing the order changes the text of the nodes and the keys they send`() {
        val pad = launchPad()

        pad.order = PinPadOrder.forSeed(8)
        provider(pad).performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null)

        assertEquals("6", checkNotNull(provider(pad).createAccessibilityNodeInfo(0)).text.toString())
        assertEquals(listOf(PinKey.Digit(6)), pressed)
    }

    @Test
    fun `an unknown virtual id has no node and no action`() {
        val pad = launchPad()

        assertNull(provider(pad).createAccessibilityNodeInfo(11))
        assertFalse(provider(pad).performAction(11, AccessibilityNodeInfo.ACTION_CLICK, null))
    }
}
