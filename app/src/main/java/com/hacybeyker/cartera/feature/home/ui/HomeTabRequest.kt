package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Un tab que Home muestra la próxima vez que se ve (por ejemplo, Mercado al volver de una orden). Es estado observable
 * y no un valor de la entrada: Home lo lee al recomponerse aunque su entrada ya exista en la pila.
 */
@Stable
class HomeTabRequest {
    var tab: HomeTab? by mutableStateOf(null)
        private set

    fun request(tab: HomeTab) {
        this.tab = tab
    }

    fun consume() {
        tab = null
    }
}
