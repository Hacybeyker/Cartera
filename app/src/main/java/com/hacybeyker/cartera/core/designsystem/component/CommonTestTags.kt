package com.hacybeyker.cartera.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

object CommonTestTags {
    const val BACK_BAR = "barra_volver"
    const val BLOCKING_PROGRESS = "progreso_bloqueante"
    const val SESSION_EXPIRED_DIALOG = "modal_sesion_expirada"
    const val SESSION_EXPIRED_CONFIRM = "modal_sesion_expirada_aceptar"
    const val ERROR_DIALOG = "modal_error"
    const val ERROR_DIALOG_RETRY = "modal_error_reintentar"
    const val ERROR_DIALOG_CANCEL = "modal_error_cancelar"
    const val CONFIRMATION_DIALOG = "modal_confirmacion"
    const val CONFIRMATION_DIALOG_ACCEPT = "modal_confirmacion_aceptar"
    const val CONFIRMATION_DIALOG_CANCEL = "modal_confirmacion_cancelar"

    fun loading(screen: String) = "${screen}_cargando"

    fun error(screen: String) = "${screen}_error"

    fun retry(screen: String) = "${screen}_reintentar"
}

// Un diálogo se dibuja en otra ventana y no hereda testTagsAsResourceId de la raíz: sin esto, Maestro y
// UiAutomator no verían sus testTags como resource-id.
fun Modifier.windowRootTestTag(tag: String): Modifier = semantics { testTagsAsResourceId = true }.testTag(tag)
