package com.hacybeyker.cartera.feature.help.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.ComingSoonState

const val HELP_SCREEN = "ayuda"

@Composable
fun HelpContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        BackTopBar(title = stringResource(R.string.menu_help), onBack = onBack)
        ComingSoonState(screen = HELP_SCREEN)
    }
}

@PreviewLightDark
@Composable
private fun HelpContentPreview() {
    CarteraTheme {
        HelpContent(onBack = {})
    }
}
