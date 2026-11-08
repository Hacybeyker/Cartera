package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.databinding.LegacyActivityBinding
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class LegacyActivity :
    AppCompatActivity(),
    LegacyHost,
    LegacyNavigator {

    @Inject
    lateinit var flow: PaymentFlow

    @Inject
    lateinit var presenterSource: LegacyPresenters

    private lateinit var binding: LegacyActivityBinding

    override val presenters: LegacyPresenterSource get() = presenterSource
    override val navigator: LegacyNavigator get() = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Con el proceso recreado el flujo vuelve vacío: sus pantallas ya no tienen datos que mostrar.
        if (savedInstanceState != null && !flow.isStarted) {
            close()
            return
        }
        enableEdgeToEdge()
        binding = LegacyActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        binding.legacyActividadBarra.setNavigationOnClickListener { goBack() }
        if (savedInstanceState == null) {
            flow.start()
            supportFragmentManager.commit { replace(R.id.legacy_actividad_contenedor, BillersFragment()) }
        }
    }

    override fun navigateTo(screen: LegacyScreen) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.legacy_actividad_contenedor, screen.createFragment())
            addToBackStack(null)
        }
    }

    override fun goBack() = onBackPressedDispatcher.onBackPressed()

    override fun close() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun setScreenTitle(@StringRes title: Int) {
        binding.legacyActividadBarra.setTitle(title)
    }
}
