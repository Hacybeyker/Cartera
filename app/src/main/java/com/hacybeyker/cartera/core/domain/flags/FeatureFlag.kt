package com.hacybeyker.cartera.core.domain.flags

/**
 * Interruptores del cliente, apagados salvo que se enciendan. [key] es el nombre en el escenario, en adb y en los
 * argumentos de arranque; el nombre de la constante es el de BuildConfig.
 */
enum class FeatureFlag(val key: String) {
    /** Los movimientos de una cuenta llegan en una sola página completa. */
    MOVEMENTS_FULL_PAGE("movements_full_page"),

    /** Al volver a Inicio tras una transferencia, las cuentas se piden de nuevo sin pasar por la caché. */
    ACCOUNTS_EAGER_REFRESH("accounts_eager_refresh"),

    /** El desglose entra completo con una transición y su ilustración a tamaño completo. */
    BREAKDOWN_RICH_TRANSITION("breakdown_rich_transition"),

    /** Camino de actualización de los precios de Mercado. */
    QUOTES_FULL_REFRESH("quotes_full_refresh"),

    /** Camino de dibujo del gráfico del Detalle de acción. */
    CHART_LIVE_PATH("chart_live_path"),

    /** Camino del buscador de Empresas, en `:legacy`. */
    BILLERS_FULL_REBIND("billers_full_rebind"),

    /** Camino de carga de los Recibos pendientes, en `:legacy`. */
    DEBTS_MAIN_DECODE("debts_main_decode")
}
