package com.hacybeyker.cartera.demo.scenario.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.demo.backend.FailureMode

@Composable
fun ScenarioContent(
    uiState: ScenarioUiState,
    onAction: (ScenarioAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().imePadding().testTag(ScenarioTestTags.SCREEN)) {
        BackTopBar(title = stringResource(R.string.scenario_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            ScenarioField(
                value = uiState.latency,
                label = R.string.scenario_latency,
                testTag = ScenarioTestTags.LATENCY,
                onValueChange = { onAction(ScenarioAction.LatencyChanged(it)) }
            )
            ScenarioField(
                value = uiState.failureRate,
                label = R.string.scenario_failure_rate,
                testTag = ScenarioTestTags.FAILURE_RATE,
                onValueChange = { onAction(ScenarioAction.FailureRateChanged(it)) }
            )
            FailureModeSelector(selected = uiState.failureMode, onAction = onAction)
            ScenarioField(
                value = uiState.sessionTtl,
                label = R.string.scenario_session_ttl,
                testTag = ScenarioTestTags.SESSION_TTL,
                onValueChange = { onAction(ScenarioAction.SessionTtlChanged(it)) },
                keyboardType = KeyboardType.Number
            )
            BackendDownSwitch(checked = uiState.backendDown, onAction = onAction)
            Text(text = stringResource(R.string.scenario_flags), style = MaterialTheme.typography.titleSmall)
            FeatureFlag.entries.forEach { flag ->
                LabeledSwitch(
                    label = stringResource(flag.labelRes()),
                    checked = flag in uiState.flags,
                    testTag = ScenarioTestTags.flag(flag),
                    onCheckedChange = { onAction(ScenarioAction.FlagChanged(flag, it)) }
                )
            }
            Text(
                text = stringResource(R.string.scenario_endpoints),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ScenarioButtons(uiState = uiState, onAction = onAction)
            uiState.message?.let { message ->
                Text(
                    text = message.text(),
                    modifier = Modifier.testTag(ScenarioTestTags.MESSAGE),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ScenarioField(
    value: String,
    @StringRes label: Int,
    testTag: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Ascii
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        label = { Text(text = stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = keyboardType)
    )
}

@Composable
private fun FailureModeSelector(selected: FailureMode, onAction: (ScenarioAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(text = stringResource(R.string.scenario_failure_mode), style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            modifier = Modifier.testTag(ScenarioTestTags.FAILURE_MODE),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            FailureMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selected,
                    onClick = { onAction(ScenarioAction.FailureModeSelected(mode)) },
                    label = { Text(text = mode.wireName) },
                    modifier = Modifier.testTag(ScenarioTestTags.failureMode(mode))
                )
            }
        }
    }
}

@Composable
private fun BackendDownSwitch(checked: Boolean, onAction: (ScenarioAction) -> Unit) {
    LabeledSwitch(
        label = stringResource(R.string.scenario_backend_down),
        checked = checked,
        testTag = ScenarioTestTags.BACKEND_DOWN,
        onCheckedChange = { onAction(ScenarioAction.BackendDownChanged(it)) }
    )
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, testTag: String, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(testTag))
    }
}

@StringRes
private fun FeatureFlag.labelRes(): Int = when (this) {
    FeatureFlag.MOVEMENTS_FULL_PAGE -> R.string.scenario_movements_full_page
    FeatureFlag.ACCOUNTS_EAGER_REFRESH -> R.string.scenario_accounts_eager_refresh
    FeatureFlag.BREAKDOWN_RICH_TRANSITION -> R.string.scenario_breakdown_rich_transition
}

@Composable
private fun ScenarioButtons(uiState: ScenarioUiState, onAction: (ScenarioAction) -> Unit) {
    Button(
        onClick = { onAction(ScenarioAction.SaveClicked) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ScenarioTestTags.SAVE)
    ) {
        Text(text = stringResource(R.string.scenario_save))
    }
    OutlinedButton(
        onClick = { onAction(ScenarioAction.DefaultsClicked) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ScenarioTestTags.DEFAULTS)
    ) {
        Text(text = stringResource(R.string.scenario_defaults))
    }
    OutlinedButton(
        onClick = { onAction(ScenarioAction.ResetDataClicked) },
        enabled = !uiState.isResettingData,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ScenarioTestTags.RESET_DATA)
    ) {
        Text(text = stringResource(R.string.scenario_reset_data))
    }
}

@Composable
private fun ScenarioMessage.text(): String = when (this) {
    ScenarioMessage.Saved -> stringResource(R.string.scenario_saved)
    is ScenarioMessage.Rejected -> stringResource(R.string.scenario_rejected, values.joinToString())
    ScenarioMessage.DefaultsRestored -> stringResource(R.string.scenario_defaults_restored)
    ScenarioMessage.DataReset -> stringResource(R.string.scenario_data_reset)
    ScenarioMessage.DataResetFailed -> stringResource(R.string.scenario_data_reset_failed)
}

@PreviewLightDark
@Composable
private fun ScenarioContentPreview() {
    CarteraTheme {
        ScenarioContent(
            uiState = ScenarioUiState(
                latency = "default=400,movements=2000",
                failureRate = "default=0",
                sessionTtl = "900",
                message = ScenarioMessage.Saved
            ),
            onAction = {},
            onBack = {}
        )
    }
}
