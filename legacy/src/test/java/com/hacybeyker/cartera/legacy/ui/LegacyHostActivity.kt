package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.databinding.LegacyActivityBinding

/** Lo que el test decide del anfitrión: qué pantalla muestra, con qué Presenters y qué pidió la pantalla. */
internal object HostEnvironment {
    lateinit var presenters: LegacyPresenterSource
    lateinit var screen: () -> Fragment

    val navigated = mutableListOf<LegacyScreen>()
    val stackReplacedWith = mutableListOf<LegacyScreen>()
    val restartedAt = mutableListOf<LegacyScreen>()
    var backs = 0
    var closes = 0

    fun reset(presenters: LegacyPresenterSource, screen: () -> Fragment) {
        this.presenters = presenters
        this.screen = screen
        navigated.clear()
        stackReplacedWith.clear()
        restartedAt.clear()
        backs = 0
        closes = 0
    }
}

/**
 * Anfitrión de los fragmentos en los tests: arma la misma pantalla que `LegacyActivity` sin Hilt, que en la JVM
 * exige una Application con su componente. Entrega los Presenters del test y anota la navegación que piden.
 */
internal class LegacyHostActivity :
    AppCompatActivity(),
    LegacyHost,
    LegacyNavigator {

    private lateinit var binding: LegacyActivityBinding

    override val presenters: LegacyPresenterSource get() = HostEnvironment.presenters
    override val navigator: LegacyNavigator get() = this

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Cartera_Legacy)
        super.onCreate(savedInstanceState)
        binding = LegacyActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager.commit { replace(R.id.legacy_actividad_contenedor, HostEnvironment.screen()) }
    }

    override fun navigateTo(screen: LegacyScreen) {
        HostEnvironment.navigated += screen
    }

    override fun replaceStackWith(screen: LegacyScreen) {
        HostEnvironment.stackReplacedWith += screen
    }

    override fun restartAt(screen: LegacyScreen) {
        HostEnvironment.restartedAt += screen
    }

    override fun goBack() {
        HostEnvironment.backs++
    }

    override fun close() {
        HostEnvironment.closes++
    }

    override fun setScreenTitle(@StringRes title: Int) {
        binding.legacyActividadBarra.setTitle(title)
    }
}
