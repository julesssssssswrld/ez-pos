package com.iandevs.ezpos.util

import java.text.NumberFormat
import java.util.Locale

/** Shared Philippine Peso currency formatter. */
val pesoFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/** Shorthand for formatting a Double as ₱X,XXX.XX */
fun Double.formatPeso(): String = pesoFormat.format(this)

/** Format a quantity, stripping unnecessary trailing zeros (e.g. 5.0 → "5") */
fun Double.formatQty(): String = toBigDecimal().stripTrailingZeros().toPlainString()
