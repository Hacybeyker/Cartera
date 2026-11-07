package com.hacybeyker.cartera.feature.movements.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.ui.LoadUiState
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.format.formattedSigned
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.movement.CategoryIllustration
import com.hacybeyker.cartera.core.ui.movement.labelRes
import com.hacybeyker.cartera.core.ui.preview.previewMovements
import com.hacybeyker.cartera.feature.movements.domain.model.MovementDetail
import com.hacybeyker.cartera.feature.movements.ui.LabeledRow
import com.hacybeyker.cartera.feature.movements.ui.MovementDetailTestTags
import com.hacybeyker.cartera.feature.movements.ui.channelLabel
import com.hacybeyker.cartera.feature.movements.ui.statusLabel

@Composable
fun MovementDetailContent(
    uiState: LoadUiState<MovementDetail>,
    onAction: (MovementDetailAction) -> Unit,
    onBack: () -> Unit,
    onBreakdownClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().testTag(MovementDetailTestTags.SCREEN)) {
        BackTopBar(title = stringResource(R.string.movement_detail_title), onBack = onBack)
        val error = uiState.error
        val detail = uiState.content
        when {
            uiState.isLoading -> LoadingState(screen = MovementDetailTestTags.STATES)
            error != null -> ErrorState(
                screen = MovementDetailTestTags.STATES,
                message = stringResource(error.messageRes()),
                onRetry = { onAction(MovementDetailAction.RetryClicked) }.takeIf { error.isRetryable }
            )
            detail != null -> DetailBody(detail = detail, onBreakdownClick = onBreakdownClick)
        }
    }
}

@Composable
private fun DetailBody(detail: MovementDetail, onBreakdownClick: () -> Unit) {
    val movement = detail.movement
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryIllustration(category = movement.category, size = MaterialTheme.sizes.listIcon)
        Text(
            text = movement.amount.formattedSigned(),
            modifier = Modifier.testTag(MovementDetailTestTags.AMOUNT),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = movement.description,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screen),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        HorizontalDivider()
        LabeledRow(label = stringResource(R.string.movement_date), value = movement.date.formatted())
        LabeledRow(
            label = stringResource(R.string.movement_category),
            value = stringResource(movement.category.labelRes())
        )
        LabeledRow(label = stringResource(R.string.movement_operation_number), value = detail.operationNumber)
        LabeledRow(label = stringResource(R.string.movement_channel), value = channelLabel(detail.channel))
        LabeledRow(label = stringResource(R.string.movement_status), value = statusLabel(detail.status))
        LabeledRow(label = stringResource(R.string.movement_counterparty), value = detail.counterpartyName)
        LabeledRow(label = stringResource(R.string.movement_reference), value = detail.counterpartyReference)
        Button(
            onClick = onBreakdownClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.screen)
                .testTag(MovementDetailTestTags.BREAKDOWN)
        ) {
            Text(text = stringResource(R.string.movement_view_breakdown))
        }
    }
}

@PreviewLightDark
@Composable
private fun MovementDetailContentPreview() {
    CarteraTheme {
        MovementDetailContent(
            uiState = LoadUiState(
                isLoading = false,
                content = MovementDetail(
                    movement = previewMovements.first(),
                    operationNumber = "20000001",
                    channel = "POS",
                    status = "COMPLETED",
                    counterpartyName = "Supermercado La Canasta",
                    counterpartyReference = "VISA ****9034"
                )
            ),
            onAction = {},
            onBack = {},
            onBreakdownClick = {}
        )
    }
}
