package com.hacybeyker.cartera.demo.backend

import java.util.Base64

data class Page(val items: List<MovementRecord>, val nextCursor: String?)

// El cursor apunta al último id entregado, no a una posición: un movimiento nuevo al inicio no desplaza las páginas.
object CursorPager {
    const val DEFAULT_LIMIT = 20
    const val MAX_LIMIT = 1000

    fun page(items: List<MovementRecord>, cursor: String?, limit: Int): Page {
        val start = if (cursor == null) 0 else indexAfter(items, cursor)
        val pageItems = items.drop(start).take(limit)
        val hasMore = start + pageItems.size < items.size
        val next = if (hasMore && pageItems.isNotEmpty()) encode(pageItems.last().id) else null
        return Page(pageItems, next)
    }

    fun encode(id: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(id.toByteArray())

    private fun indexAfter(items: List<MovementRecord>, cursor: String): Int {
        val id = decode(cursor)
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) throw invalidCursor()
        return index + 1
    }

    private fun decode(cursor: String): String = try {
        String(Base64.getUrlDecoder().decode(cursor))
    } catch (_: IllegalArgumentException) {
        throw invalidCursor()
    }

    private fun invalidCursor() = ProblemException(
        code = ProblemCode.VALIDATION_ERROR,
        detail = "El cursor no corresponde a esta lista.",
        errors = listOf(FieldError("cursor", "INVALID", "Cursor inválido o vencido."))
    )
}
