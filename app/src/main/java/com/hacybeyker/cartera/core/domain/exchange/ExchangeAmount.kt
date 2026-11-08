package com.hacybeyker.cartera.core.domain.exchange

import java.math.BigDecimal

private val AMOUNT_FORMAT = Regex("""^\d{1,9}(\.\d{1,2})?$""")

/** El monto escrito, si tiene el formato del contrato: hasta 9 enteros y 2 decimales, sin signo ni separadores. */
fun parseExchangeAmount(text: String): BigDecimal? = text.trim().takeIf { AMOUNT_FORMAT.matches(it) }?.toBigDecimal()
