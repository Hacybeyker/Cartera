package com.hacybeyker.cartera.feature.home.ui.overview

import com.hacybeyker.cartera.core.ui.movement.MovementListTags

object OverviewTestTags {
    const val SCREEN = "inicio_pantalla"
    const val GREETING = "inicio_saludo"
    const val TAB_ACCOUNTS = "inicio_tab_cuentas"
    const val TAB_MOVEMENTS = "inicio_tab_movimientos"

    const val ACCOUNTS = "cuentas"
    const val ACCOUNTS_LIST = "cuentas_lista"
    const val TRANSFER = "cuentas_transferir"
    const val MARK_ACCOUNTS = "inicio_cuentas"

    const val MOVEMENTS = "movimientos"
    const val MARK_MOVEMENTS = "inicio_movimientos"
    val movementList = MovementListTags(list = "movimientos_lista", prefix = MOVEMENTS)

    fun account(id: String) = "cuentas_item_$id"
}
