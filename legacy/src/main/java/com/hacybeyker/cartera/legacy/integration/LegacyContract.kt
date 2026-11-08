package com.hacybeyker.cartera.legacy.integration

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.hacybeyker.cartera.legacy.ui.LegacyActivity

/** Único punto de entrada al módulo: lanza [LegacyActivity] con un `Intent` explícito y devuelve un [LegacyResult]. */
class LegacyContract : ActivityResultContract<Unit, LegacyResult>() {

    override fun createIntent(context: Context, input: Unit): Intent = Intent(context, LegacyActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): LegacyResult =
        if (resultCode == Activity.RESULT_OK && intent?.getBooleanExtra(EXTRA_PAID, false) == true) {
            LegacyResult.Paid
        } else {
            LegacyResult.Dismissed
        }

    companion object {
        internal const val EXTRA_PAID = "com.hacybeyker.cartera.legacy.extra.PAID"

        /** El resultado con el que `LegacyActivity` termina cuando hubo un pago; quien la aloja lo usa para simularlo. */
        fun paidResult(): Intent = Intent().putExtra(EXTRA_PAID, true)
    }
}
