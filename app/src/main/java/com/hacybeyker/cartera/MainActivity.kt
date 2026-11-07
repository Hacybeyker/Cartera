package com.hacybeyker.cartera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.ui.marks.VisibleContentMarker
import com.hacybeyker.cartera.navigation.CarteraApp
import com.hacybeyker.cartera.navigation.HiddenScreen
import com.hacybeyker.cartera.navigation.NavEntryInstaller
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navEntryInstallers: Set<@JvmSuppressWildcards NavEntryInstaller>

    @Inject
    lateinit var hiddenScreens: Set<@JvmSuppressWildcards HiddenScreen>

    @Inject
    lateinit var visibleContentMarkers: Set<@JvmSuppressWildcards VisibleContentMarker>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
