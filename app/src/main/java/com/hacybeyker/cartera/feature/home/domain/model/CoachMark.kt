package com.hacybeyker.cartera.feature.home.domain.model

/** Los pasos de los coach marks de Home, en orden: Transferir, el tab Mercado y el botón del menú. */
enum class CoachMark {
    TRANSFER,
    MARKET_TAB,
    MENU;

    /** Número del paso, desde 1 (`coach_paso_<n>`). */
    val number: Int get() = ordinal + 1

    val isLast: Boolean get() = this == entries.last()

    /** El paso siguiente, o `null` si este es el último. */
    fun next(): CoachMark? = entries.getOrNull(ordinal + 1)

    companion object {
        val first: CoachMark = entries.first()
    }
}
