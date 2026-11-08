package com.hacybeyker.cartera.legacy.integration

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.hacybeyker.cartera.legacy.ui.LegacyActivity

/** Único punto de entrada al módulo: lanza [LegacyActivity] con un `Intent` explícito y devuelve un [LegacyResult]. */
class LegacyContract : ActivityResultContract<Unit, LegacyResult>() {

    override fun createIntent(context: Context, input: Unit): Intent = Intent(context, LegacyActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): LegacyResult = LegacyResult.Dismissed
}
