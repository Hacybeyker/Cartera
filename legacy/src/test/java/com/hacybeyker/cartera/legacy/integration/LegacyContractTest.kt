package com.hacybeyker.cartera.legacy.integration

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyContractTest {
    private val contract = LegacyContract()

    @Test
    fun `builds an explicit intent for the legacy activity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = contract.createIntent(context, Unit)

        assertEquals("com.hacybeyker.cartera.legacy.ui.LegacyActivity", intent.component?.className)
        assertEquals(context.packageName, intent.component?.packageName)
    }

    @Test
    fun `leaving the module without a result is a dismissal`() {
        assertEquals(LegacyResult.Dismissed, contract.parseResult(Activity.RESULT_CANCELED, null))
    }

    @Test
    fun `an ok result without a known payload is also a dismissal`() {
        assertEquals(LegacyResult.Dismissed, contract.parseResult(Activity.RESULT_OK, Intent()))
    }

    @Test
    fun `the marks are the names the scope fixes`() {
        assertEquals(
            listOf("legacy_empresas", "legacy_recibos", "legacy_constancia"),
            listOf(LegacyMarks.BILLERS, LegacyMarks.DEBTS, LegacyMarks.VOUCHER)
        )
    }
}
