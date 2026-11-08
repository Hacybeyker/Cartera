package com.hacybeyker.cartera.feature.transfer.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.feature.transfer.ui.TransferReceiptArgs
import com.hacybeyker.cartera.feature.transfer.ui.TransferTestTags

/** Sin [receipt], la transferencia no se pudo confirmar. */
@Composable
fun TransferResultContent(receipt: TransferReceiptArgs?, onBackToHome: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen)
            .testTag(TransferTestTags.RESULT),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (receipt != null) Success(receipt) else Failure()
        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TransferTestTags.BACK_TO_HOME)
        ) {
            Text(text = stringResource(R.string.transfer_back_to_home))
        }
    }
}

@Composable
private fun Success(receipt: TransferReceiptArgs) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(MaterialTheme.sizes.logo),
        tint = MaterialTheme.colorScheme.primary
    )
    Text(text = stringResource(R.string.transfer_success_title), style = MaterialTheme.typography.headlineSmall)
    Text(
        text = stringResource(R.string.transfer_success_operation, receipt.operationNumber),
        modifier = Modifier.testTag(TransferTestTags.RESULT_OPERATION),
        style = MaterialTheme.typography.bodyLarge
    )
    Text(
        text = stringResource(R.string.transfer_success_balance, receipt.balance.formatted()),
        modifier = Modifier.testTag(TransferTestTags.RESULT_BALANCE),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun Failure() {
    Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = null,
        modifier = Modifier.size(MaterialTheme.sizes.logo),
        tint = MaterialTheme.colorScheme.error
    )
    Text(text = stringResource(R.string.transfer_failure_title), style = MaterialTheme.typography.headlineSmall)
    Text(
        text = stringResource(R.string.transfer_failure_message),
        modifier = Modifier.testTag(TransferTestTags.RESULT_ERROR),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

@PreviewLightDark
@Composable
private fun TransferResultContentPreview() {
    CarteraTheme {
        TransferResultContent(receipt = TransferReceiptArgs("90000001", "2150.00", "PEN"), onBackToHome = {})
    }
}
