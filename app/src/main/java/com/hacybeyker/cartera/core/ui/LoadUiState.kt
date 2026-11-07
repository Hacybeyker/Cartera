package com.hacybeyker.cartera.core.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome

/** Una pantalla que carga una sola cosa: Cargando → Contenido | Error. */
data class LoadUiState<T>(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val content: T? = null,
    /** Sube con cada carga correcta: es la clave de la marca de contenido visible. */
    val contentVersion: Int = 0
) {
    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null && content != null }

    fun loading(): LoadUiState<T> = copy(isLoading = true, error = null)

    fun loaded(outcome: Outcome<T>): LoadUiState<T> = when (outcome) {
        is Outcome.Success -> copy(isLoading = false, content = outcome.value, contentVersion = contentVersion + 1)
        is Outcome.Failure -> copy(isLoading = false, error = outcome.error)
    }
}
