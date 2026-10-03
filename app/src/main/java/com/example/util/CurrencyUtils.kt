package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * دالة تنسيق العملات الموحدة للنظام المحاسبي
 * Clean unified currency and number formatting with thousand separators and 2 optional/fixed decimal places
 * Handles negative values cleanly with LRM direction markers to eliminate reversed or corrupted strings like -13.490.00-
 */
fun BigDecimal.toCleanCurrency(currency: String = "YER", forceDecimals: Boolean = false): String {
    val symbol = CurrencyHelper.getCurrencySymbol(currency)
    val isNeg = this.signum() < 0
    val absVal = this.abs().setScale(2, RoundingMode.HALF_UP)

    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    val hasFractions = absVal.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0
    val pattern = if (forceDecimals || hasFractions) "#,##0.00" else "#,##0"
    val df = DecimalFormat(pattern, symbols)
    val formattedNum = df.format(absVal)

    return if (isNeg) {
        "\u200E-$formattedNum $symbol"
    } else {
        "$formattedNum $symbol"
    }
}

fun Double.toCleanCurrency(currency: String = "YER", forceDecimals: Boolean = false): String {
    return BigDecimal.valueOf(this).toCleanCurrency(currency, forceDecimals)
}

fun Float.toCleanCurrency(currency: String = "YER", forceDecimals: Boolean = false): String {
    return BigDecimal.valueOf(this.toDouble()).toCleanCurrency(currency, forceDecimals)
}

fun Long.toCleanCurrency(currency: String = "YER", forceDecimals: Boolean = false): String {
    return BigDecimal.valueOf(this).toCleanCurrency(currency, forceDecimals)
}

fun Int.toCleanCurrency(currency: String = "YER", forceDecimals: Boolean = false): String {
    return BigDecimal.valueOf(this.toLong()).toCleanCurrency(currency, forceDecimals)
}

fun Number.toCleanCurrency(currency: String = "YER", forceDecimals: Boolean = false): String {
    return BigDecimal.valueOf(this.toDouble()).toCleanCurrency(currency, forceDecimals)
}
