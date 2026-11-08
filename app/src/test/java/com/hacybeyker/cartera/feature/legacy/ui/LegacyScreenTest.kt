package com.hacybeyker.cartera.feature.legacy.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.designsystem.component.CommonTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val registry = RecordingRegistry(ApplicationProvider.getApplicationContext())
    private var backs = 0

    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = registry
    }

    @Composable
    private fun Host() {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
            LegacyScreen(onBack = { backs++ })
        }
    }

    @Test
    fun `opens the legacy activity with an explicit intent as soon as it is shown`() {
        compose.setContent { Host() }
        compose.waitForIdle()

        assertEquals(1, registry.intents.size)
        assertEquals("com.hacybeyker.cartera.legacy.ui.LegacyActivity", registry.intents.single().component?.className)
        assertEquals(0, backs)
    }

    @Test
    fun `closes the entry when the module returns`() {
        compose.setContent { Host() }
        compose.waitForIdle()

        registry.finish(Activity.RESULT_CANCELED)
        compose.waitForIdle()

        assertEquals(1, backs)
    }

    @Test
    fun `does not open the module again when the state is restored`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Host() }
        compose.waitForIdle()

        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        assertEquals(1, registry.intents.size)
    }

    @Test
    fun `the back bar closes the entry`() {
        compose.setContent { Host() }
        compose.waitForIdle()

        compose.onNodeWithTag(CommonTestTags.BACK_BAR).performClick()

        assertEquals(1, backs)
    }

    private class RecordingRegistry(private val context: Context) : ActivityResultRegistry() {
        val intents = mutableListOf<Intent>()
        private var lastRequestCode = 0

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            lastRequestCode = requestCode
            intents += contract.createIntent(context, input)
        }

        fun finish(resultCode: Int) = dispatchResult(lastRequestCode, resultCode, Intent())
    }
}
