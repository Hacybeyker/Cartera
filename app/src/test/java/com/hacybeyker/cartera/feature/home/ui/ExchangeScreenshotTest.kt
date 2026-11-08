package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeAccountRole
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeContent
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeUiState
import com.hacybeyker.cartera.feature.home.ui.exchange.previewExchange
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ExchangeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** Recién abierto: la Cuenta Dólares preseleccionada y sin monto. */
    private val opened = previewExchange.copy(amount = "", destinationAccountId = null)

    @Test
    fun homeExchangeTab() = compose.captureLightAndDark("home_cambio") {
        HomeContent(selectedTab = HomeTab.EXCHANGE, onTabSelected = {}) {
            ExchangeContent(uiState = opened, onAction = {})
        }
    }

    @Test
    fun filled() = compose.captureLightAndDark("cambio_contenido") {
        ExchangeContent(uiState = previewExchange, onAction = {})
    }

    @Test
    fun solesToDollars() = compose.captureLightAndDark("cambio_soles_a_dolares") {
        ExchangeContent(uiState = previewExchange.reversed().copy(amount = "376.80"), onAction = {})
    }

    @Test
    fun errors() = compose.captureLightAndDark("cambio_errores") {
        ExchangeContent(
            uiState = opened.copy(
                amount = "2000",
                fieldErrors = mapOf(
                    ExchangeField.AMOUNT to ExchangeFieldError.EXCEEDS_BALANCE,
                    ExchangeField.DESTINATION to ExchangeFieldError.REQUIRED
                )
            ),
            onAction = {}
        )
    }

    @Test
    fun reconnecting() = compose.captureLightAndDark("cambio_reconectando") {
        ExchangeContent(uiState = previewExchange.copy(isReconnecting = true), onAction = {})
    }

    @Test
    fun loading() = compose.captureLightAndDark("cambio_cargando") {
        ExchangeContent(uiState = ExchangeUiState(), onAction = {})
    }

    @Test
    fun loadError() = compose.captureLightAndDark("cambio_error") {
        ExchangeContent(uiState = ExchangeUiState(isLoading = false, error = AppError.Network), onAction = {})
    }

    @Test
    fun accountsSheet() = compose.captureLightAndDark("cambio_sheet_cuentas", withWindows = true) {
        ExchangeContent(uiState = opened.copy(accountSheetFor = ExchangeAccountRole.DESTINATION), onAction = {})
    }

    @Test
    fun howWeCalculateSheet() = compose.captureLightAndDark("cambio_como_calculamos", withWindows = true) {
        ExchangeContent(uiState = previewExchange.copy(isHowWeCalculateVisible = true), onAction = {})
    }

    @Test
    fun quoteError() = compose.captureLightAndDark("cambio_error_cotizacion", withWindows = true) {
        ExchangeContent(uiState = previewExchange.copy(dialogError = AppError.Network), onAction = {})
    }
}
