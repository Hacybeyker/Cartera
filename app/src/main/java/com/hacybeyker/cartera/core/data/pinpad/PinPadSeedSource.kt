package com.hacybeyker.cartera.core.data.pinpad

/**
 * Punto de extensión: de dónde sale la semilla del teclado en tiempo de ejecución. Release no tiene ninguna fuente y
 * rige el orden fijo; debug y demo aportan la del escenario. `null` significa "esta fuente no lo decide".
 */
fun interface PinPadSeedSource {
    fun seed(): Int?
}
