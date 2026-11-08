package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark
import com.hacybeyker.cartera.feature.home.ui.overview.AccountsTabActions
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewContent
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Home con Inicio a la vista: el botón del menú, el menú abierto y cada paso de los coach marks. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class HomeShellScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun start() = compose.captureLightAndDark("home_inicio") {
        HomeContent(selectedTab = HomeTab.START, onTabSelected = {}) { Overview() }
    }

    @Test
    fun menuOpen() = compose.captureLightAndDark("home_menu_abierto") {
        HomeContent(
            selectedTab = HomeTab.START,
            onTabSelected = {},
            drawerState = rememberDrawerState(DrawerValue.Open)
        ) { Overview() }
    }

    @Test
    fun menuLogOutConfirmation() = compose.captureLightAndDark("home_menu_cerrar_sesion", withWindows = true) {
        HomeContent(
            selectedTab = HomeTab.START,
            onTabSelected = {},
            uiState = HomeUiState(isLogoutConfirmationVisible = true)
        ) { Overview() }
    }

    @Test
    fun coachTransfer() = coach(CoachMark.TRANSFER)

    @Test
    fun coachMarketTab() = coach(CoachMark.MARKET_TAB)

    @Test
    fun coachMenu() = coach(CoachMark.MENU)

    @Test
    fun coachWhileStartLoads() = compose.captureLightAndDark("home_coach_paso_1_cargando") {
        HomeContent(
            selectedTab = HomeTab.START,
            onTabSelected = {},
            uiState = HomeUiState(coachMark = CoachMark.TRANSFER)
        ) {
            OverviewContent(
                uiState = OverviewUiState(),
                onAction = {},
                accountsActions = noActions,
                onMenuClick = {}
            ) {}
        }
    }

    private fun coach(mark: CoachMark) = compose.captureLightAndDark("home_coach_paso_${mark.number}") {
        HomeContent(selectedTab = HomeTab.START, onTabSelected = {}, uiState = HomeUiState(coachMark = mark)) {
            Overview()
        }
    }

    @Composable
    private fun Overview() {
        OverviewContent(
            uiState = OverviewUiState(isLoading = false, firstName = "Ana", accounts = previewAccounts),
            onAction = {},
            accountsActions = noActions,
            onMenuClick = {}
        ) {}
    }

    private val noActions = AccountsTabActions(onAccountClick = {}, onTransferClick = {})
}
