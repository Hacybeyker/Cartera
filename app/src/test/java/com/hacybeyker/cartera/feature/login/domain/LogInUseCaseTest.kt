package com.hacybeyker.cartera.feature.login.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.TestAppConfig
import com.hacybeyker.cartera.core.testing.resolveEntryStep
import com.hacybeyker.cartera.feature.login.FakeAuthRepository
import com.hacybeyker.cartera.feature.login.domain.model.AuthSession
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.usecase.LogInUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LogInUseCaseTest {
    private val auth = FakeAuthRepository()
    private val session = FakeSessionRepository()
    private val firstRun = FakeFirstRunRepository()
    private val logIn = LogInUseCase(auth, session, resolveEntryStep(firstRun = firstRun))
    private val credentials = Credentials("ana.torres", "Cartera2026")

    @Test
    fun `success starts the session and resolves the next step`() = runTest {
        assertEquals(Outcome.Success(EntryStep.TERMS), logIn(credentials))
        assertEquals("jwt-1", session.accessToken)
    }

    @Test
    fun `a returning user goes straight home`() = runTest {
        firstRun.acceptedTermsVersion = TestAppConfig.termsVersion
        firstRun.onboardingSeen = true

        assertEquals(Outcome.Success(EntryStep.HOME), logIn(credentials))
    }

    @Test
    fun `failure starts no session`() = runTest {
        auth.error = AppError.InvalidCredentials

        assertEquals(Outcome.Failure(AppError.InvalidCredentials), logIn(credentials))
        assertNull(session.accessToken)
    }

    @Test
    fun `secrets stay out of toString`() {
        assertFalse(credentials.toString().contains("Cartera2026"))
        assertFalse(AuthSession("secret-jwt", 900).toString().contains("secret-jwt"))
    }
}
