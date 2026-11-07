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
import com.hacybeyker.cartera.feature.home.ui.HomeNavKey
import com.hacybeyker.cartera.feature.home.ui.homeEntry
import com.hacybeyker.cartera.feature.login.ui.LoginNavKey
import com.hacybeyker.cartera.feature.login.ui.loginEntry
import com.hacybeyker.cartera.feature.onboarding.ui.OnboardingNavKey
import com.hacybeyker.cartera.feature.onboarding.ui.onboardingEntry
import com.hacybeyker.cartera.feature.splash.ui.splashEntry
import com.hacybeyker.cartera.feature.terms.ui.TermsNavKey
import com.hacybeyker.cartera.feature.terms.ui.termsEntry

@Composable
fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    extraEntries: Set<NavEntryInstaller> = emptySet()
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
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
            homeEntry()
            extraEntries.forEach { installer -> with(installer) { install() } }
        }
    )
}

private fun EntryStep.navKey(): NavKey = when (this) {
    EntryStep.TERMS -> TermsNavKey
    EntryStep.ONBOARDING -> OnboardingNavKey
    EntryStep.HOME -> HomeNavKey
}
