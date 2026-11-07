package com.hacybeyker.cartera.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import com.hacybeyker.cartera.core.designsystem.component.SessionExpiredDialog
import com.hacybeyker.cartera.core.ui.marks.LocalVisibleContentMarker
import com.hacybeyker.cartera.core.ui.marks.VisibleContentMarker
import com.hacybeyker.cartera.feature.login.ui.LoginNavKey
import com.hacybeyker.cartera.feature.splash.ui.SplashNavKey

@Composable
fun CarteraApp(
    extraEntries: Set<NavEntryInstaller>,
    visibleContentMarker: VisibleContentMarker,
    modifier: Modifier = Modifier,
    viewModel: AppShellViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(SplashNavKey)
    CompositionLocalProvider(LocalVisibleContentMarker provides visibleContentMarker) {
        // Expone los testTags como resource-id para Maestro y UiAutomator en toda la jerarquía.
        Scaffold(modifier = modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) { innerPadding ->
            AppNavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(paddingValues = innerPadding),
                extraEntries = extraEntries
            )
        }
        if (uiState.isSessionExpired) {
            SessionExpiredDialog(
                onConfirm = {
                    viewModel.onAction(AppShellAction.SessionExpiredAcknowledged)
                    backStack.resetTo(LoginNavKey)
                }
            )
        }
    }
}
