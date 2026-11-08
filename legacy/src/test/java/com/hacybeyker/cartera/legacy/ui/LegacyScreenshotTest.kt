package com.hacybeyker.cartera.legacy.ui

import androidx.fragment.app.Fragment
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.legacy.testing.RecordingAccountView
import com.hacybeyker.cartera.legacy.testing.RecordingDebtsView
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.fixtureBillers
import io.reactivex.rxjava3.core.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class LegacyScreenshotTest(private val dark: Boolean) {

    @Before
    fun disableAnimations() = setAnimationsEnabled(false)

    @After
    fun enableAnimations() = setAnimationsEnabled(true)

    private fun captureLightAndDark(name: String, presenters: () -> LegacyPresenterSource, screen: () -> Fragment) =
        captureScreen(name, dark, presenters, screen)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "oscuro={0}")
        fun modes(): List<Boolean> = listOf(false, true)
    }

    private fun billersScreen(prepare: TestPresenters.() -> Unit): () -> LegacyPresenterSource = {
        TestPresenters().apply(prepare)
    }

    private fun flowAtCustomerCode(): TestPresenters = TestPresenters().apply { flow.chooseBiller(Samples.luz) }

    private fun flowAtDebts(): TestPresenters = flowAtCustomerCode().apply { flow.chooseCustomerCode("100200") }

    private fun flowAtAccount(): TestPresenters = flowAtDebts().apply {
        flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        accounts.respond = { Single.just(listOf(Samples.sueldo, Samples.gastos, Samples.dolares)) }
    }

    @Test
    fun billersWithList() = captureLightAndDark(
        "legacy_empresas_lista",
        billersScreen { billers.respond = { Single.just(fixtureBillers()) } }
    ) { BillersFragment() }

    @Test
    fun billersLoading() = captureLightAndDark(
        "legacy_empresas_cargando",
        billersScreen { billers.respond = { Single.never() } }
    ) { BillersFragment() }

    @Test
    fun billersEmpty() = captureLightAndDark(
        "legacy_empresas_vacia",
        billersScreen {
            billers.respond = { Single.just(emptyList()) }
            onBillersPresenter = { it.onQueryChanged("zzz") }
        }
    ) { BillersFragment() }

    @Test
    fun customerCode() = captureLightAndDark("legacy_codigo_campo", { flowAtCustomerCode() }) { CustomerCodeFragment() }

    @Test
    fun customerCodeWithFormatError() = captureLightAndDark(
        "legacy_codigo_error",
        {
            flowAtCustomerCode().apply {
                onCustomerCodePresenter = { presenter ->
                    presenter.onCodeChanged("1002")
                    presenter.onContinue()
                }
            }
        }
    ) { CustomerCodeFragment() }

    @Test
    fun customerCodeRejectedByTheServer() = captureLightAndDark(
        "legacy_codigo_inexistente",
        {
            flowAtDebts().apply {
                flow.rejectCustomerCode()
            }
        }
    ) { CustomerCodeFragment() }

    @Test
    fun debtsWithSelection() = captureLightAndDark(
        "legacy_recibos_seleccion",
        {
            flowAtDebts().apply {
                debts.respond = { Single.just(Samples.luzDebts) }
                onDebtsPresenter = { presenter ->
                    val primer = RecordingDebtsView()
                    presenter.attach(primer)
                    presenter.onDebtToggled(Samples.octubre.id)
                    presenter.detach()
                }
            }
        }
    ) { DebtsFragment() }

    @Test
    fun debtsEmpty() = captureLightAndDark(
        "legacy_recibos_vacio",
        {
            flowAtDebts().apply {
                debts.respond = { Single.just(Samples.luzDebts.copy(debts = emptyList())) }
            }
        }
    ) { DebtsFragment() }

    @Test
    fun debtsLoading() = captureLightAndDark(
        "legacy_recibos_cargando",
        { flowAtDebts().apply { debts.respond = { Single.never() } } }
    ) { DebtsFragment() }

    @Test
    fun accountWithSelection() = captureLightAndDark(
        "legacy_cuenta_seleccion",
        {
            flowAtAccount().apply {
                onAccountPresenter = { presenter ->
                    presenter.attach(RecordingAccountView())
                    presenter.onAccountSelected(Samples.gastos.id)
                    presenter.detach()
                }
            }
        }
    ) { AccountFragment() }

    @Test
    fun accountWithInsufficientBalance() = captureLightAndDark(
        "legacy_cuenta_saldo_insuficiente",
        {
            flowAtAccount().apply {
                accounts.respond = { Single.just(listOf(Samples.poca, Samples.gastos)) }
                onAccountPresenter = { presenter ->
                    presenter.attach(RecordingAccountView())
                    presenter.onAccountSelected(Samples.poca.id)
                    presenter.detach()
                }
            }
        }
    ) { AccountFragment() }

    @Test
    fun accountLoading() = captureLightAndDark(
        "legacy_cuenta_cargando",
        { flowAtAccount().apply { accounts.respond = { Single.never() } } }
    ) { AccountFragment() }

    @Test
    fun pinPlaceholder() = captureLightAndDark("legacy_clave_pendiente", { TestPresenters() }) {
        LegacyPlaceholderFragment()
    }
}
