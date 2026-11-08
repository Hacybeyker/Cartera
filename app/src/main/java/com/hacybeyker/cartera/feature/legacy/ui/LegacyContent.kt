package com.hacybeyker.cartera.feature.legacy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.LoadingState

const val LEGACY_SCREEN = "legacy"

/** Lo que se ve mientras `LegacyActivity` se abre; al volver de ella, la pantalla se cierra. */
@Composable
fun LegacyContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        BackTopBar(title = stringResource(R.string.more_legacy), onBack = onBack)
        LoadingState(screen = LEGACY_SCREEN)
    }
}

@PreviewLightDark
@Composable
private fun LegacyContentPreview() {
    CarteraTheme {
        LegacyContent(onBack = {})
    }
}
