package com.hacybeyker.cartera.demo

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.hacybeyker.cartera.demo.scenario.ScenarioStore

// Los argumentos de launchApp de Maestro llegan como extras del Intent de arranque. Se aplican y se guardan
// como un broadcast de adb, así cada flow lleva su escenario aunque use clearState.
class LaunchArgumentsCallbacks(private val store: ScenarioStore) : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return
        val values = activity.intent?.extras.toScenarioValues()
        if (values.isEmpty()) return
        Log.i(DEMO_TAG, store.update(values).describe())
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
