package com.hacybeyker.cartera

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.hacybeyker.cartera.core.data.biometric.device.ActivityBiometricPrompter
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.ui.marks.VisibleContentMarker
import com.hacybeyker.cartera.navigation.CarteraApp
import com.hacybeyker.cartera.navigation.HiddenScreen
import com.hacybeyker.cartera.navigation.NavEntryInstaller
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// FragmentActivity porque BiometricPrompt la exige; sigue siendo una ComponentActivity para Compose y edge-to-edge.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var navEntryInstallers: Set<@JvmSuppressWildcards NavEntryInstaller>

    @Inject
    lateinit var hiddenScreens: Set<@JvmSuppressWildcards HiddenScreen>

    @Inject
    lateinit var biometricPrompter: ActivityBiometricPrompter

    @Inject
    lateinit var visibleContentMarkers: Set<@JvmSuppressWildcards VisibleContentMarker>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        biometricPrompter.attach(this)
        val marker = VisibleContentMarker { mark -> visibleContentMarkers.forEach { it.onContentVisible(mark) } }
        setContent {
            CarteraTheme {
                CarteraApp(
                    extraEntries = navEntryInstallers,
                    hiddenScreens = hiddenScreens,
                    visibleContentMarker = marker
                )
            }
        }
    }
}
