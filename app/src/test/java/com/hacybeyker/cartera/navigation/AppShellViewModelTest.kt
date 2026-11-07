package com.hacybeyker.cartera.navigation

import com.hacybeyker.cartera.core.domain.session.AcknowledgeSessionExpiryUseCase
import com.hacybeyker.cartera.core.domain.session.ObserveSessionExpiredUseCase
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppShellViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val session = FakeSessionRepository()

    @Test
    fun `session expiry shows the modal until it is acknowledged`() = runTest {
        // Se crea dentro del test: stateIn toma Dispatchers.Main al construirse y la regla ya debe haberlo cambiado.
        val viewModel = AppShellViewModel(
            observeSessionExpired = ObserveSessionExpiredUseCase(session),
            acknowledgeSessionExpiry = AcknowledgeSessionExpiryUseCase(session)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        assertFalse(viewModel.uiState.value.isSessionExpired)

        session.expire()
        assertTrue(viewModel.uiState.value.isSessionExpired)

        viewModel.onAction(AppShellAction.SessionExpiredAcknowledged)
        assertFalse(viewModel.uiState.value.isSessionExpired)
    }
}
