package com.hacybeyker.cartera.legacy.integration

/** Cómo terminó la visita al módulo; cada caso nuevo es un resultado que quien lo lanza debe atender. */
sealed interface LegacyResult {
    /** El usuario salió sin que el módulo cambiara nada de lo que ve quien lo aloja. */
    data object Dismissed : LegacyResult

    /** El módulo pagó, o pudo haber pagado, recibos con una cuenta: quien lo aloja debe volver a pedir los saldos. */
    data object Paid : LegacyResult
}
