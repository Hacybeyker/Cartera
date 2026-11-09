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
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import com.hacybeyker.cartera.core.data.session.InMemorySessionRepository
import com.hacybeyker.cartera.core.designsystem.component.CommonTestTags
import com.hacybeyker.cartera.core.designsystem.component.SessionExpiredDialog
import com.hacybeyker.cartera.core.domain.session.AcknowledgeSessionExpiryUseCase
import com.hacybeyker.cartera.core.domain.session.ObserveSessionExpiredUseCase
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.feature.legacy.data.ListenerLegacySessionExpiry
import com.hacybeyker.cartera.feature.legacy.domain.usecase.RegisterBillPaymentUseCase
import com.hacybeyker.cartera.navigation.AppShellAction
import com.hacybeyker.cartera.navigation.AppShellViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La mitad de Compose de un 401 dentro de `:legacy`: el módulo avisa la sesión vencida por el puente real y se cierra
 * sin pago (`Dismissed`). La entrada se cierra y el modal global, que vive fuera de la pantalla, sigue abierto hasta
 * que la persona lo acepta. `CarteraApp` no se puede componer aquí (sus pantallas piden Hilt), así que se arma con las
 * mismas piezas: el `AppShellViewModel` real sobre la sesión en memoria y el `SessionExpiredDialog`.
 */
@RunWith(AndroidJUnit4::class)
class LegacySessionExpiryTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val registry = RecordingRegistry(ApplicationProvider.getApplicationContext())
    private val tokens = InMemoryAccessTokenStore()
    private val session = InMemorySessionRepository(tokens)
    private val moduleExpiry = ListenerLegacySessionExpiry(session)
    private val changes = FakeAccountChanges()
    private val legacyViewModel = LegacyViewModel(RegisterBillPaymentUseCase(changes))
    private var backs = 0
    private var acknowledged = 0

    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = registry
    }

    @Composable
    private fun Host(shell: AppShellViewModel) {
        val state by shell.uiState.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
            LegacyScreen(onBack = { backs++ }, viewModel = legacyViewModel)
            if (state.isSessionExpired) {
                SessionExpiredDialog(
                    onConfirm = {
                        shell.onAction(AppShellAction.SessionExpiredAcknowledged)
                        acknowledged++
                    }
                )
            }
        }
    }

    private fun show() {
        val shell = AppShellViewModel(
            observeSessionExpired = ObserveSessionExpiredUseCase(session),
            acknowledgeSessionExpiry = AcknowledgeSessionExpiryUseCase(session)
        )
        session.start("jwt")
        compose.setContent { Host(shell) }
        compose.waitForIdle()
    }

    @Test
    fun `a 401 in the module closes the entry and leaves the expired session modal open`() {
        show()
        assertEquals(1, registry.intents.size)
        assertEquals(0, modals())

        moduleExpiry.onSessionExpired()
        registry.finish(Activity.RESULT_CANCELED)
        compose.waitForIdle()

        assertEquals(1, backs)
        assertEquals(0, changes.revision)
        assertNull(tokens.accessToken())
        compose.onNodeWithTag(CommonTestTags.SESSION_EXPIRED_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `accepting the modal acknowledges the expiry once and closes it`() {
        show()
        moduleExpiry.onSessionExpired()
        registry.finish(Activity.RESULT_CANCELED)
        compose.waitForIdle()

        compose.onNodeWithTag(CommonTestTags.SESSION_EXPIRED_CONFIRM).performClick()
        compose.waitForIdle()

        assertEquals(1, acknowledged)
        assertFalse(session.isExpired.value)
        assertEquals(0, modals())
    }

    @Test
    fun `the module dismissed without an expiry closes the entry and shows no modal`() {
        show()

        registry.finish(Activity.RESULT_CANCELED)
        compose.waitForIdle()

        assertEquals(1, backs)
        assertEquals("jwt", tokens.accessToken())
        assertEquals(0, modals())
    }

    private fun modals() = compose.onAllNodesWithTag(CommonTestTags.SESSION_EXPIRED_DIALOG).fetchSemanticsNodes().size

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

        fun finish(resultCode: Int, data: Intent = Intent()) = dispatchResult(lastRequestCode, resultCode, data)
    }
}
