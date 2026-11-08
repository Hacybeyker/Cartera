package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.databinding.LegacyActivityBinding

/**
 * Anfitrión de los fragmentos en los tests: arma la misma pantalla que `LegacyActivity` sin Hilt, que en la JVM
 * exige una Application con su componente.
 */
class LegacyHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Cartera_Legacy)
        super.onCreate(savedInstanceState)
        setContentView(LegacyActivityBinding.inflate(layoutInflater).root)
        supportFragmentManager.commit { replace(R.id.legacy_actividad_contenedor, LegacyPlaceholderFragment()) }
    }
}
