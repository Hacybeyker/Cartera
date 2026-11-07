package com.hacybeyker.cartera.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class CommonComponentsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loadingState() = compose.captureLightAndDark("comun_cargando") {
        LoadingState(screen = "muestra")
    }

    @Test
    fun errorState() = compose.captureLightAndDark("comun_error") {
        ErrorState(screen = "muestra", message = "No pudimos conectarnos.", onRetry = {})
    }

    @Test
    fun backTopBar() = compose.captureLightAndDark("comun_barra_volver") {
        BackTopBar(title = "Detalle", onBack = {})
    }

    @Test
    fun errorDialog() = compose.captureLightAndDark("comun_modal_error", withWindows = true) {
        ErrorDialog(title = "Algo salió mal", message = "No pudimos conectarnos.", onRetry = {}, onCancel = {})
    }

    @Test
    fun confirmationDialog() = compose.captureLightAndDark("comun_modal_confirmacion", withWindows = true) {
        ConfirmationDialog(
            title = "¿Cerrar sesión?",
            message = "Tendrás que volver a ingresar.",
            confirmLabel = "Cerrar sesión",
            onConfirm = {},
            onCancel = {}
        )
    }

    @Test
    fun sessionExpiredDialog() = compose.captureLightAndDark("comun_modal_sesion_expirada", withWindows = true) {
        SessionExpiredDialog(onConfirm = {})
    }
}
