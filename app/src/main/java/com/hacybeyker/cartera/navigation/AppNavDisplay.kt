package com.hacybeyker.cartera.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.feature.exchange.ui.FxConfirmNavKey
import com.hacybeyker.cartera.feature.exchange.ui.exchangeEntries
import com.hacybeyker.cartera.feature.exchange.ui.toArgs
import com.hacybeyker.cartera.feature.home.ui.HomeActions
import com.hacybeyker.cartera.feature.home.ui.HomeNavKey
import com.hacybeyker.cartera.feature.home.ui.HomeTab
import com.hacybeyker.cartera.feature.home.ui.HomeTabRequest
import com.hacybeyker.cartera.feature.home.ui.homeEntry
import com.hacybeyker.cartera.feature.legacy.ui.LegacyNavKey
import com.hacybeyker.cartera.feature.legacy.ui.legacyEntry
import com.hacybeyker.cartera.feature.login.ui.LoginNavKey
import com.hacybeyker.cartera.feature.login.ui.loginEntry
import com.hacybeyker.cartera.feature.market.ui.PortfolioNavKey
import com.hacybeyker.cartera.feature.market.ui.StockDetailNavKey
import com.hacybeyker.cartera.feature.market.ui.marketEntries
import com.hacybeyker.cartera.feature.movements.ui.AccountMovementsNavKey
import com.hacybeyker.cartera.feature.movements.ui.MovementBreakdownNavKey
import com.hacybeyker.cartera.feature.movements.ui.MovementDetailNavKey
import com.hacybeyker.cartera.feature.movements.ui.movementsEntries
import com.hacybeyker.cartera.feature.onboarding.ui.OnboardingNavKey
import com.hacybeyker.cartera.feature.onboarding.ui.onboardingEntry
import com.hacybeyker.cartera.feature.order.ui.OrderFormNavKey
import com.hacybeyker.cartera.feature.order.ui.orderEntries
import com.hacybeyker.cartera.feature.splash.ui.splashEntry
import com.hacybeyker.cartera.feature.terms.ui.TermsNavKey
import com.hacybeyker.cartera.feature.terms.ui.termsEntry
import com.hacybeyker.cartera.feature.transfer.ui.TransferFormNavKey
import com.hacybeyker.cartera.feature.transfer.ui.transferEntries

@Composable
fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    extraEntries: Set<NavEntryInstaller> = emptySet(),
    hiddenScreens: Set<HiddenScreen> = emptySet()
) {
    val back: () -> Unit = { backStack.removeLastOrNull() }
    val homeTabRequest = remember { HomeTabRequest() }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = back,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            splashEntry(onReady = { backStack.resetTo(LoginNavKey) })
            loginEntry(onLoggedIn = { step -> backStack.resetTo(step.navKey()) })
            termsEntry(
                onAccepted = { step -> backStack.resetTo(step.navKey()) },
                onRejected = { backStack.resetTo(LoginNavKey) }
            )
            onboardingEntry(onFinished = { backStack.resetTo(HomeNavKey) })
            homeEntry(actions = backStack.homeActions(hiddenScreens), tabRequest = homeTabRequest)
            movementsEntries(
                onBack = back,
                onMovementClick = { backStack.add(MovementDetailNavKey(it)) },
                onBreakdownClick = { backStack.add(MovementBreakdownNavKey(it)) }
            )
            marketEntries(
                onBack = back,
                onStockClick = { backStack.add(StockDetailNavKey(it)) },
                onTrade = { symbol, side -> backStack.add(OrderFormNavKey(symbol, side)) }
            )
            legacyEntry(onBack = back)
            // Volver al inicio reutiliza la entrada de Home (con su estado) en vez de crear otra: Inicio refresca
            // solo las cuentas, una vez, porque la transferencia las cambió.
            transferEntries(
                onBack = back,
                onNavigate = { backStack.add(it) },
                onBackToHome = { backStack.popTo(HomeNavKey) }
            )
            // Como la transferencia, el resultado vuelve a la entrada de Home que ya existe, ahora en Mercado: Mercado
            // vuelve a pedir solo el portafolio porque la orden subió su revisión. Cancelar vuelve a la acción.
            orderEntries(
                onBack = back,
                onNavigate = { backStack.add(it) },
                onCancelled = { symbol -> backStack.popToOrHome(StockDetailNavKey(symbol)) },
                onBackToMarket = {
                    homeTabRequest.request(HomeTab.MARKET)
                    backStack.popTo(HomeNavKey)
                }
            )
            // Igual que la orden: el resultado y Cancelar vuelven a la entrada de Home que ya existe, en el tab Cambio,
            // que vuelve a pedir solo las cuentas porque el cambio subió su revisión.
            exchangeEntries(
                onBack = back,
                onNavigate = { backStack.add(it) },
                onBackToExchange = {
                    homeTabRequest.request(HomeTab.EXCHANGE)
                    backStack.popTo(HomeNavKey)
                }
            )
            extraEntries.forEach { installer -> with(installer) { install(onBack = back) } }
        }
    )
}

private fun NavBackStack<NavKey>.homeActions(hiddenScreens: Set<HiddenScreen>) = HomeActions(
    onAccountClick = { add(AccountMovementsNavKey(it)) },
    onTransferClick = { add(TransferFormNavKey) },
    onMovementClick = { add(MovementDetailNavKey(it)) },
    onStockClick = { add(StockDetailNavKey(it)) },
    onPortfolioClick = { add(PortfolioNavKey) },
    onExchangeContinue = { add(FxConfirmNavKey(it.toArgs())) },
    onLegacyClick = { add(LegacyNavKey) },
    onLoggedOut = { resetTo(LoginNavKey) },
    // Sin pantalla oculta (release), los 7 toques no hacen nada.
    onHiddenScreenRequested = { hiddenScreens.firstOrNull()?.let { add(it.navKey()) } }
)

private fun EntryStep.navKey(): NavKey = when (this) {
    EntryStep.TERMS -> TermsNavKey
    EntryStep.ONBOARDING -> OnboardingNavKey
    EntryStep.HOME -> HomeNavKey
}
