package com.hacybeyker.cartera.demo.scenario.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag

@StringRes
internal fun FeatureFlag.labelRes(): Int = when (this) {
    FeatureFlag.MOVEMENTS_FULL_PAGE -> R.string.scenario_movements_full_page
    FeatureFlag.ACCOUNTS_EAGER_REFRESH -> R.string.scenario_accounts_eager_refresh
    FeatureFlag.BREAKDOWN_RICH_TRANSITION -> R.string.scenario_breakdown_rich_transition
    FeatureFlag.QUOTES_FULL_REFRESH -> R.string.scenario_quotes_full_refresh
    FeatureFlag.CHART_LIVE_PATH -> R.string.scenario_chart_live_path
}

@Composable
internal fun ScenarioMessage.text(): String = when (this) {
    ScenarioMessage.Saved -> stringResource(R.string.scenario_saved)
    is ScenarioMessage.Rejected -> stringResource(R.string.scenario_rejected, values.joinToString())
    ScenarioMessage.DefaultsRestored -> stringResource(R.string.scenario_defaults_restored)
    ScenarioMessage.DataReset -> stringResource(R.string.scenario_data_reset)
    ScenarioMessage.DataResetFailed -> stringResource(R.string.scenario_data_reset_failed)
}
