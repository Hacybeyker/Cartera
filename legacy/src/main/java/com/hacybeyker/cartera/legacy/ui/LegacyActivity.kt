package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.databinding.LegacyActivityBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class LegacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = LegacyActivityBinding.inflate(layoutInflater)
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
        binding.legacyActividadBarra.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        if (savedInstanceState == null) {
            supportFragmentManager.commit { replace(R.id.legacy_actividad_contenedor, LegacyPlaceholderFragment()) }
        }
    }
}
