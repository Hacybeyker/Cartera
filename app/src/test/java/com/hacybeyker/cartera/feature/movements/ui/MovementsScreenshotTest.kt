package com.hacybeyker.cartera.feature.movements.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.core.ui.LoadUiState
import com.hacybeyker.cartera.core.ui.movement.MovementListUiState
import com.hacybeyker.cartera.core.ui.paging.PagedList
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.core.ui.preview.previewMovements
import com.hacybeyker.cartera.feature.movements.TestBreakdown
import com.hacybeyker.cartera.feature.movements.domain.model.Breakdown
import com.hacybeyker.cartera.feature.movements.domain.model.MovementDetail
import com.hacybeyker.cartera.feature.movements.ui.account.AccountMovementsContent
import com.hacybeyker.cartera.feature.movements.ui.account.AccountMovementsUiState
import com.hacybeyker.cartera.feature.movements.ui.breakdown.MovementBreakdownContent
import com.hacybeyker.cartera.feature.movements.ui.detail.MovementDetailContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class MovementsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun accountMovements() = compose.captureLightAndDark("cuenta_movimientos") {
        AccountMovementsContent(
            uiState = AccountMovementsUiState(
                account = previewAccounts[1],
                list = MovementListUiState(isLoading = false, movements = PagedList(previewMovements, nextCursor = "x"))
            ),
            onAction = {},
            onBack = {},
            onMovementClick = {}
        )
    }

    @Test
    fun accountLoading() = compose.captureLightAndDark("cuenta_cargando") {
        AccountMovementsContent(uiState = AccountMovementsUiState(), onAction = {}, onBack = {}, onMovementClick = {})
    }

    @Test
    fun accountNotFound() = compose.captureLightAndDark("cuenta_no_encontrada") {
        AccountMovementsContent(
            uiState = AccountMovementsUiState(list = MovementListUiState(isLoading = false, error = AppError.NotFound)),
            onAction = {},
            onBack = {},
            onMovementClick = {}
        )
    }

    @Test
    fun detail() = compose.captureLightAndDark("movimiento_detalle") {
        MovementDetailContent(
            uiState = LoadUiState(isLoading = false, content = sampleDetail),
            onAction = {},
            onBack = {},
            onBreakdownClick = {}
        )
    }

    @Test
    fun detailError() = compose.captureLightAndDark("movimiento_error") {
        MovementDetailContent(
            uiState = LoadUiState<MovementDetail>(isLoading = false, error = AppError.Network),
            onAction = {},
            onBack = {},
            onBreakdownClick = {}
        )
    }

    @Test
    fun breakdown() = compose.captureLightAndDark("desglose_contenido") {
        MovementBreakdownContent(uiState = LoadUiState(isLoading = false, content = TestBreakdown), onAction = {
        }, onBack = {})
    }

    @Test
    fun breakdownWithExchangeRate() = compose.captureLightAndDark("desglose_tipo_cambio") {
        MovementBreakdownContent(
            uiState = LoadUiState(
                isLoading = false,
                content = TestBreakdown.copy(merchant = null, exchangeRate = "3.7520".toBigDecimal())
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun breakdownNotFound() = compose.captureLightAndDark("desglose_no_encontrado") {
        MovementBreakdownContent(
            uiState = LoadUiState<Breakdown>(isLoading = false, error = AppError.NotFound),
            onAction = {},
            onBack = {}
        )
    }

    private companion object {
        val sampleDetail = MovementDetail(
            movement = previewMovements.first(),
            operationNumber = "20000001",
            channel = "POS",
            status = "COMPLETED",
            counterpartyName = "Supermercado La Canasta",
            counterpartyReference = "VISA ****9034"
        )
    }
}
