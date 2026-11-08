package com.hacybeyker.cartera.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.feature.home.ui.HomeActions
import com.hacybeyker.cartera.feature.home.ui.HomeNavKey
import com.hacybeyker.cartera.feature.home.ui.homeEntry
import com.hacybeyker.cartera.feature.legacy.ui.LegacyNavKey
import com.hacybeyker.cartera.feature.legacy.ui.legacyEntry
import com.hacybeyker.cartera.feature.login.ui.LoginNavKey
import com.hacybeyker.cartera.feature.login.ui.loginEntry
import com.hacybeyker.cartera.feature.movements.ui.AccountMovementsNavKey
import com.hacybeyker.cartera.feature.movements.ui.MovementBreakdownNavKey
import com.hacybeyker.cartera.feature.movements.ui.MovementDetailNavKey
import com.hacybeyker.cartera.feature.movements.ui.movementsEntries
import com.hacybeyker.cartera.feature.onboarding.ui.OnboardingNavKey
import com.hacybeyker.cartera.feature.onboarding.ui.onboardingEntry
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
            homeEntry(
                actions = HomeActions(
                    onAccountClick = { backStack.add(AccountMovementsNavKey(it)) },
                    onTransferClick = { backStack.add(TransferFormNavKey) },
                    onMovementClick = { backStack.add(MovementDetailNavKey(it)) },
                    onLegacyClick = { backStack.add(LegacyNavKey) },
                    onLoggedOut = { backStack.resetTo(LoginNavKey) },
                    // Sin pantalla oculta (release), los 7 toques no hacen nada.
                    onHiddenScreenRequested = { hiddenScreens.firstOrNull()?.let { backStack.add(it.navKey()) } }
                )
            )
            movementsEntries(
                onBack = back,
                onMovementClick = { backStack.add(MovementDetailNavKey(it)) },
                onBreakdownClick = { backStack.add(MovementBreakdownNavKey(it)) }
            )
            legacyEntry(onBack = back)
            // Volver al inicio reutiliza la entrada de Home (con su estado) en vez de crear otra: Inicio refresca
            // solo las cuentas, una vez, porque la transferencia las cambió.
            transferEntries(
                onBack = back,
                onNavigate = { backStack.add(it) },
                onBackToHome = { backStack.popTo(HomeNavKey) }
            )
            extraEntries.forEach { installer -> with(installer) { install(onBack = back) } }
        }
    )
}

private fun EntryStep.navKey(): NavKey = when (this) {
    EntryStep.TERMS -> TermsNavKey
    EntryStep.ONBOARDING -> OnboardingNavKey
    EntryStep.HOME -> HomeNavKey
}
