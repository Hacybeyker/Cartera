package com.hacybeyker.cartera.core.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.windowRootTestTag
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import kotlinx.coroutines.launch

/**
 * Selector de cuenta en un bottom sheet: alias, número enmascarado y saldo disponible de cada una. Quien lo usa decide
 * qué cuentas se ofrecen (por ejemplo, solo las de una moneda) y lo saca de la composición en [onDismiss]; elegir una
 * cierra el sheet con su animación y después avisa con [onSelect].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsSheet(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (accountId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // El sheet se dibuja en otra ventana, como los diálogos.
        modifier = Modifier.windowRootTestTag(AccountsSheetTestTags.SHEET),
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.accounts_sheet_title),
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screen),
                style = MaterialTheme.typography.titleMedium
            )
            if (accounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.accounts_sheet_empty),
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screen),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.selectableGroup()) {
                accounts.forEach { account ->
                    AccountOption(
                        account = account,
                        selected = account.id == selectedAccountId,
                        onSelect = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onSelect(account.id) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountOption(account: Account, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.sm)
            .testTag(AccountsSheetTestTags.item(account.id)),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = account.alias, style = MaterialTheme.typography.bodyLarge)
            if (account.numberMasked.isNotBlank()) {
                Text(
                    text = account.numberMasked,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(text = account.availableBalance.formatted(), style = MaterialTheme.typography.bodyLarge)
    }
}

@PreviewLightDark
@Composable
private fun AccountsSheetPreview() {
    CarteraTheme {
        AccountsSheet(accounts = previewAccounts, selectedAccountId = "ACC-003", onSelect = {}, onDismiss = {})
    }
}
