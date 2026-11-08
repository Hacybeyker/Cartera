package com.hacybeyker.cartera.legacy.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.hacybeyker.cartera.legacy.R

/**
 * Apila otra pantalla sobre la actual y vuelve atrás con el `FragmentManager` real: la vista de la pantalla de abajo se
 * destruye al apilar y se crea de nuevo con su estado guardado al volver. [whileAway] corre mientras está tapada.
 */
internal fun LegacyHostActivity.pushAndPop(whileAway: () -> Unit = {}) {
    supportFragmentManager.commit {
        setReorderingAllowed(true)
        replace(R.id.legacy_actividad_contenedor, Fragment())
        addToBackStack(null)
    }
    supportFragmentManager.executePendingTransactions()
    idle()
    whileAway()
    supportFragmentManager.popBackStackImmediate()
    idle()
    layoutNow(window.decorView)
}
