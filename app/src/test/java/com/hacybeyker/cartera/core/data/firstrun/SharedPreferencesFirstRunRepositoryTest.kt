package com.hacybeyker.cartera.core.data.firstrun

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesFirstRunRepositoryTest {
    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences(SharedPreferencesFirstRunRepository.FILE_NAME, Context.MODE_PRIVATE)

    @Test
    fun `a fresh install has nothing accepted nor seen`() = runTest {
        val repository = SharedPreferencesFirstRunRepository(preferences, StandardTestDispatcher(testScheduler))

        assertNull(repository.acceptedTermsVersion())
        assertFalse(repository.isOnboardingSeen())
        assertFalse(repository.isBiometricOfferResolved())
    }

    @Test
    fun `accepted terms and seen onboarding survive a new instance`() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        SharedPreferencesFirstRunRepository(preferences, io).apply {
            acceptTerms("2026-11-01")
            markOnboardingSeen()
            markBiometricOfferResolved()
        }

        val reopened = SharedPreferencesFirstRunRepository(preferences, io)

        assertEquals("2026-11-01", reopened.acceptedTermsVersion())
        assertTrue(reopened.isOnboardingSeen())
        assertTrue(reopened.isBiometricOfferResolved())
    }
}
