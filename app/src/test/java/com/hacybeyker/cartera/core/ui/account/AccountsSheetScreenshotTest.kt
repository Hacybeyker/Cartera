package com.hacybeyker.cartera.core.ui.account

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AccountsSheetScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun accounts() = compose.captureLightAndDark("sheet_cuentas", withWindows = true) {
        AccountsSheet(accounts = previewAccounts, selectedAccountId = "ACC-003", onSelect = {}, onDismiss = {})
    }

    @Test
    fun noAccounts() = compose.captureLightAndDark("sheet_cuentas_vacio", withWindows = true) {
        AccountsSheet(accounts = emptyList(), selectedAccountId = null, onSelect = {}, onDismiss = {})
    }
}
