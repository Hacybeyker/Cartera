package com.hacybeyker.cartera.legacy.ui

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/** Base de las pantallas del flujo: pone su título en la barra cada vez que queda a la vista. */
internal abstract class LegacyScreenFragment(@LayoutRes layout: Int, @StringRes private val title: Int) :
    Fragment(layout) {

    override fun onResume() {
        super.onResume()
        legacyHost.navigator.setScreenTitle(title)
    }
}
