package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formato de montos y períodos en español de Perú ("S/ 2,122.50", "Octubre 2026"), sin depender del locale. */
object BillFormat {
    private val MONTHS = listOf(
        "enero",
        "febrero",
        "marzo",
        "abril",
        "mayo",
        "junio",
        "julio",
        "agosto",
        "septiembre",
        "octubre",
        "noviembre",
        "diciembre"
    )

    fun money(value: BigDecimal, currency: String): String {
        val symbol = if (currency == "PEN") "S/" else currency
        return "$symbol ${DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(value)}"
    }

    /** "2026-10" es "octubre 2026". */
    fun period(period: String): String {
        val month = period.substringAfter('-').toIntOrNull()?.let { MONTHS.getOrNull(it - 1) }
        return if (month == null) period else "$month ${period.substringBefore('-')}"
    }
}

/**
 * La constancia de un pago como HTML simple y semántico, sin ids ni scripts: se prueba por texto. Cada dato va en su
 * propio elemento para que se pueda leer y buscar por separado.
 */
object BillVoucher {
    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(LIMA)

    fun render(payment: BillPayment): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("""<html lang="es">""")
        appendLine("<head>")
        appendLine("""<meta charset="utf-8">""")
        appendLine("""<meta name="viewport" content="width=device-width, initial-scale=1">""")
        appendLine("""<meta http-equiv="Content-Security-Policy" content="$POLICY">""")
        appendLine("<title>Constancia de pago</title>")
        appendLine("<style>$STYLE</style>")
        appendLine("</head>")
        appendLine("<body>")
        appendLine("<main>")
        appendLine("<h1>Constancia de pago</h1>")
        appendLine("<dl>")
        field("Número de operación", payment.operationNumber)
        field("Fecha y hora", DATE_TIME.format(payment.createdAt))
        field("Empresa", payment.billerName)
        field("Cliente", payment.customerName)
        field("Código de cliente", payment.customerCode)
        field("Cuenta de cargo", payment.accountLabel)
        appendLine("</dl>")
        debts(payment)
        appendLine("<dl>")
        field("Nuevo saldo disponible", BillFormat.money(payment.newBalance, payment.currency))
        appendLine("</dl>")
        appendLine("</main>")
        appendLine("</body>")
        appendLine("</html>")
    }

    private fun StringBuilder.debts(payment: BillPayment) {
        appendLine("<table>")
        appendLine("<caption>Recibos pagados</caption>")
        appendLine("""<thead><tr><th scope="col">Período</th><th scope="col">Importe</th></tr></thead>""")
        appendLine("<tbody>")
        payment.paidDebts.forEach { debt ->
            val period = escape(BillFormat.period(debt.period).replaceFirstChar { it.uppercase() })
            appendLine("<tr><td>$period</td><td>${escape(BillFormat.money(debt.amount, payment.currency))}</td></tr>")
        }
        appendLine("</tbody>")
        appendLine("<tfoot>")
        val total = escape(BillFormat.money(payment.total, payment.currency))
        appendLine("""<tr><th scope="row">Total pagado</th><td>$total</td></tr>""")
        appendLine("</tfoot>")
        appendLine("</table>")
    }

    private fun StringBuilder.field(label: String, value: String) {
        appendLine("<dt>${escape(label)}</dt><dd>${escape(value)}</dd>")
    }

    fun escape(text: String): String = buildString(text.length) {
        text.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    private const val POLICY = "default-src 'none'; style-src 'unsafe-inline'"

    private const val STYLE = ":root{color-scheme:light dark}" +
        "body{font-family:sans-serif;margin:16px;line-height:1.4}" +
        "h1{font-size:1.25rem;margin:0 0 16px}" +
        "dl{margin:0 0 16px}" +
        "dt{font-size:.8rem;opacity:.7;margin-top:12px}" +
        "dd{margin:2px 0 0}" +
        "table{width:100%;border-collapse:collapse}" +
        "caption{text-align:left;font-weight:bold;padding-bottom:8px}" +
        "th,td{padding:8px 0;border-bottom:1px solid rgba(128,128,128,.4);text-align:left}" +
        "td:last-child,th:last-child{text-align:right}"
}
