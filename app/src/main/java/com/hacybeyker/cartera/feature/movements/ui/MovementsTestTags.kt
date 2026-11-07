package com.hacybeyker.cartera.feature.movements.ui

import com.hacybeyker.cartera.core.ui.movement.MovementListTags

object AccountMovementsTestTags {
    const val SCREEN = "cuenta_pantalla"
    const val BALANCE = "cuenta_saldo"

    /** Prefijo de los estados y marca de la pantalla. */
    const val STATES = "cuenta"
    const val MARK = "cuenta_movimientos"
    val list = MovementListTags(list = "cuenta_movimientos_lista", prefix = "cuenta_movimientos")
}

object MovementDetailTestTags {
    const val SCREEN = "movimiento_detalle"
    const val AMOUNT = "movimiento_monto"
    const val BREAKDOWN = "movimiento_ver_desglose"
    const val STATES = "movimiento"
    const val MARK = "movimiento_detalle"
}

object MovementBreakdownTestTags {
    const val SCREEN = "movimiento_desglose"
    const val ILLUSTRATION = "desglose_ilustracion"
    const val STATES = "desglose"
    const val MARK = "movimiento_desglose"

    /** Las líneas se numeran desde 1, en el orden del contrato. */
    fun line(number: Int) = "desglose_linea_$number"
}
