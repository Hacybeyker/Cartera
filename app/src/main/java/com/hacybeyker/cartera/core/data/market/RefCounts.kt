package com.hacybeyker.cartera.core.data.market

/** Cuántos suscriptores tiene cada símbolo o par. */
internal class RefCounts {
    private val counts = mutableMapOf<String, Int>()

    val keys: Set<String> get() = counts.keys

    val isEmpty: Boolean get() = counts.isEmpty()

    operator fun contains(key: String): Boolean = key in counts

    /** Suma un suscriptor a cada clave; devuelve las que no tenían ninguno. */
    fun add(keys: Set<String>): List<String> = keys.filter { key ->
        val count = counts.getOrElse(key) { 0 } + 1
        counts[key] = count
        count == 1
    }

    /** Resta un suscriptor a cada clave; devuelve las que se quedaron sin ninguno. */
    fun remove(keys: Set<String>): List<String> = keys.filter { key ->
        val count = counts[key] ?: return@filter false
        if (count == 1) counts.remove(key) else counts[key] = count - 1
        count == 1
    }
}
