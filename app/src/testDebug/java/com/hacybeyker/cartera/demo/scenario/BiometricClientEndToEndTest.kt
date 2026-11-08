package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.BearerInterceptor
import com.hacybeyker.cartera.core.data.network.BiometricApi
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import com.hacybeyker.cartera.core.data.session.InMemorySessionRepository
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.testing.FakeBiometricAvailability
import com.hacybeyker.cartera.core.testing.FakeDeviceTokenVault
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.TestAppConfig
import com.hacybeyker.cartera.core.testing.resolveEntryStep
import com.hacybeyker.cartera.demo.backend.CarteraBackend
import com.hacybeyker.cartera.demo.backend.MutableClock
import com.hacybeyker.cartera.demo.backend.fileFixtures
import com.hacybeyker.cartera.feature.biometric.data.repository.RemoteBiometricEnrollmentRepository
import com.hacybeyker.cartera.feature.biometric.domain.model.EnableBiometricResult
import com.hacybeyker.cartera.feature.biometric.domain.usecase.DisableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.biometric.domain.usecase.EnableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.login.data.repository.RemoteAuthRepository
import com.hacybeyker.cartera.feature.login.domain.model.BiometricDisabledReason
import com.hacybeyker.cartera.feature.login.domain.model.BiometricLoginResult
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.usecase.LogInUseCase
import com.hacybeyker.cartera.feature.login.domain.usecase.LogInWithBiometricsUseCase
import java.net.InetAddress
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Registro, ingreso con huella, revocación e `INVALID_DEVICE`, con el cliente de verdad y una huella simulada. */
class BiometricClientEndToEndTest {
    private val backend = CarteraBackend(fileFixtures, MutableClock())
    private val scenario = ScenarioStore(FakeScenarioStorage())
    private val tokens = InMemoryAccessTokenStore()
    private val session = InMemorySessionRepository(tokens)
    private val caller = ApiCaller(CarteraHttpClient.json, session)
    private val vault = FakeDeviceTokenVault()
    private val firstRun = FakeFirstRunRepository(
        acceptedTermsVersion = TestAppConfig.termsVersion,
        onboardingSeen = true
    )
    private lateinit var client: OkHttpClient
    private lateinit var api: CarteraApi
    private lateinit var biometricApi: BiometricApi

    private val auth by lazy { RemoteAuthRepository(api, biometricApi, caller) }
    private val enrollment by lazy { RemoteBiometricEnrollmentRepository(biometricApi, caller) }
    private val resolve = resolveEntryStep(firstRun = firstRun, vault = vault)
    private val logIn by lazy { LogInUseCase(auth, session, resolve) }
    private val logInWithBiometrics by lazy { LogInWithBiometricsUseCase(vault, auth, session, resolve) }
    private val enable by lazy { EnableBiometricLoginUseCase(FakeBiometricAvailability(), vault, enrollment, firstRun) }
    private val disable by lazy { DisableBiometricLoginUseCase(vault, enrollment) }

    @Before
    fun setUp() {
        backend.start(InetAddress.getLoopbackAddress(), port = 0)
        scenario.update(mapOf("latency" to "0"))
        client = CarteraHttpClient.okHttpClient(
            timeoutSeconds = 2,
            interceptors = listOf(BearerInterceptor(tokens), DemoScenarioInterceptor(scenario))
        )
        val retrofit = CarteraHttpClient.retrofit(backend.baseUrl.toString(), client)
        api = retrofit.create(CarteraApi::class.java)
        biometricApi = retrofit.create(BiometricApi::class.java)
    }

    @After
    fun tearDown() {
        client.dispatcher.executorService.shutdown()
        backend.close()
    }

    @Test
    fun `enroll, log in with the device, revoke and the old token is an invalid device`() = runTest {
        assertEquals(Outcome.Success(EntryStep.HOME), logIn(Credentials("ana.torres", "Cartera2026")))

        assertEquals(EnableBiometricResult.Enabled, enable())
        val deviceToken = requireNotNull(vault.token)
        assertTrue(deviceToken.startsWith("dvc_"))
        assertTrue(firstRun.biometricOfferResolved)

        // Cerrar sesión no borra el deviceToken: es lo que permite volver a entrar con huella.
        session.end()
        assertNull(tokens.accessToken())
        assertEquals(deviceToken, vault.token)

        assertEquals(BiometricLoginResult.LoggedIn(EntryStep.HOME), logInWithBiometrics())
        val biometricSession = requireNotNull(tokens.accessToken())
        assertEquals(Outcome.Success(Unit), disable())
        assertNull(vault.token)
        assertEquals(biometricSession, tokens.accessToken())

        assertEquals(Outcome.Failure(AppError.InvalidDevice), auth.logInWithDevice(deviceToken))
        vault.token = deviceToken
        assertEquals(BiometricLoginResult.Disabled(BiometricDisabledReason.DEVICE_REVOKED), logInWithBiometrics())
        assertNull(vault.token)
        // INVALID_DEVICE no es una sesión vencida: la sesión abierta sigue y no se abre el modal.
        assertEquals(false, session.isExpired.value)
        assertNotNull(tokens.accessToken())
    }

    @Test
    fun `the biometric session honors the session ttl of the scenario`() = runTest {
        logIn(Credentials("ana.torres", "Cartera2026"))
        enable()
        scenario.update(mapOf("session_ttl" to "60"))

        val outcome = auth.logInWithDevice(requireNotNull(vault.token)) as Outcome.Success

        assertEquals(60L, outcome.value.expiresInSeconds)
    }

    @Test
    fun `each enroll issues a new token and both work until revoked`() = runTest {
        logIn(Credentials("ana.torres", "Cartera2026"))
        enable()
        val first = requireNotNull(vault.token)
        enable()
        val second = requireNotNull(vault.token)
        assertNotEquals(first, second)

        assertTrue(auth.logInWithDevice(first) is Outcome.Success)
        assertTrue(auth.logInWithDevice(second) is Outcome.Success)
        disable()

        assertEquals(Outcome.Failure(AppError.InvalidDevice), auth.logInWithDevice(first))
        assertEquals(Outcome.Failure(AppError.InvalidDevice), auth.logInWithDevice(second))
    }

    @Test
    fun `enroll without a session expires it and keeps nothing`() = runTest {
        assertEquals(EnableBiometricResult.Failure(AppError.SessionExpired), enable())

        assertNull(vault.token)
        assertEquals(true, session.isExpired.value)
    }
}
