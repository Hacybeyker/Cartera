package com.hacybeyker.cartera.feature.home.ui.coach

object CoachMarksTestTags {
    const val LAYER = "coach_capa"

    /** También en el último paso, donde dice "Entendido". */
    const val NEXT = "coach_siguiente"
    const val SKIP = "coach_omitir"

    fun step(number: Int) = "coach_paso_$number"
}
