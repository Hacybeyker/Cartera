package com.hacybeyker.cartera.feature.legacy.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hacybeyker.cartera.legacy.integration.LegacyContract
import com.hacybeyker.cartera.legacy.integration.LegacyResult

/**
 * Abre `LegacyActivity` una sola vez por entrada y, cuando devuelve su resultado, cierra la entrada para dejar
 * visible la pantalla anterior. Si hubo un pago, antes invalida las cuentas.
 */
@Composable
fun LegacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: LegacyViewModel = hiltViewModel()) {
    // Se guarda con el estado de la entrada: rotar o recrear el proceso mientras el módulo está al frente no
    // lo vuelve a abrir.
    var launched by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(LegacyContract()) { result ->
        when (result) {
            LegacyResult.Dismissed -> Unit
            LegacyResult.Paid -> viewModel.onPaid()
        }
        onBack()
    }
    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            launcher.launch(Unit)
        }
    }
    LegacyContent(onBack = onBack, modifier = modifier)
}
