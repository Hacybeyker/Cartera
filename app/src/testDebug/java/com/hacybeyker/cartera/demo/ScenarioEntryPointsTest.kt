package com.hacybeyker.cartera.demo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.demo.scenario.ScenarioFeatureFlagSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class ScenarioEntryPointsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val source = ScenarioFeatureFlagSource(context.scenarioStore())

    @Test
    fun `an adb broadcast with boolean extras turns the legacy flags on and off`() {
        val receiver = ScenarioReceiver()
        val on = Intent("${context.packageName}.demo.SET_SCENARIO")
            .putExtra("billers_full_rebind", true)
            .putExtra("debts_main_decode", true)

        receiver.onReceive(context, on)

        assertEquals(true, source.valueOf(FeatureFlag.BILLERS_FULL_REBIND))
        assertEquals(true, source.valueOf(FeatureFlag.DEBTS_MAIN_DECODE))

        receiver.onReceive(
            context,
            Intent("${context.packageName}.demo.SET_SCENARIO").putExtra("billers_full_rebind", false)
        )

        assertEquals(false, source.valueOf(FeatureFlag.BILLERS_FULL_REBIND))
        assertEquals(true, source.valueOf(FeatureFlag.DEBTS_MAIN_DECODE))
    }

    @Test
    fun `launch arguments as text turn the legacy flags on, and an invalid text leaves the flag as it was`() {
        val intent = Intent().putExtra("billers_full_rebind", "true").putExtra("debts_main_decode", "si")
        val activity = Robolectric.buildActivity(Activity::class.java, intent).get()

        LaunchArgumentsCallbacks(context.scenarioStore()).onActivityCreated(activity, null)

        assertEquals(true, source.valueOf(FeatureFlag.BILLERS_FULL_REBIND))
        assertEquals(false, source.valueOf(FeatureFlag.DEBTS_MAIN_DECODE))
    }
}
