package com.hacybeyker.cartera.feature.onboarding.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

fun EntryProviderScope<NavKey>.onboardingEntry(onFinished: () -> Unit) {
    entry<OnboardingNavKey> { OnboardingScreen(onFinished = onFinished) }
}
