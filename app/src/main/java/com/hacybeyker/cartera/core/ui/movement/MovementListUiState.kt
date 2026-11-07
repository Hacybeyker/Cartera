package com.hacybeyker.cartera.core.ui.movement

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.ui.paging.PagedList

/** Estado de una lista de movimientos: Cargando → Contenido | Vacío | Error, y la paginación del contenido. */
data class MovementListUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val movements: PagedList<Movement> = PagedList(),
    /** Sube con cada primera página cargada: es la clave de la marca de contenido visible. */
    val contentVersion: Int = 0
) {
    val isEmpty: Boolean get() = !isLoading && error == null && movements.items.isEmpty() && !movements.hasMore

    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }
}
