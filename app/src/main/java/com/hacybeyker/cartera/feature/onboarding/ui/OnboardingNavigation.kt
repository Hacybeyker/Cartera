package com.hacybeyker.cartera.feature.onboarding.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.core.domain.entry.EntryStep

fun EntryProviderScope<NavKey>.onboardingEntry(onFinished: (EntryStep) -> Unit) {
    entry<OnboardingNavKey> { OnboardingScreen(onFinished = onFinished) }
}
