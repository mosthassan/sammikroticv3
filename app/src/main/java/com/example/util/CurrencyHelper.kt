package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * مساعد إدارة العملات وأسعار الصرف المرنة باستخدام BigDecimal لتفادي أخطاء التقريب الحسابي
 * يدعم: الريال اليمني (YER)، الريال السعودي (SAR)، الدولار الأمريكي (USD)
 */
object CurrencyHelper {

    const val CURRENCY_YER = "YER"
    const val CURRENCY_SAR = "SAR"
    const val CURRENCY_USD = "USD"

    val SUPPORTED_CURRENCIES = listOf(CURRENCY_YER, CURRENCY_SAR, CURRENCY_USD)

    fun getCurrencyLabel(currency: String): String {
        return when (currency.uppercase()) {
            CURRENCY_SAR -> "ريال سعودي (SAR)"
            CURRENCY_USD -> "دولار أمريكي (USD)"
            else -> "ريال يمني (YER)"
        }
    }

    fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase()) {
            CURRENCY_SAR -> "ر.س"
            CURRENCY_USD -> "$"
            else -> "ر.ي"
        }
    }

    /**
     * تحويل أي مبلغ من عملة مصدرية إلى الريال اليمني (العملة الأساسية للنظام)
     */
    fun convertToYer(
        amount: BigDecimal,
        fromCurrency: String,
        sarToYer: BigDecimal,
        usdToYer: BigDecimal
    ): BigDecimal {
        if (amount <= BigDecimal.ZERO) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        val defaultSarRate = if (sarToYer > BigDecimal.ZERO) sarToYer else BigDecimal("140.0")
        val defaultUsdRate = if (usdToYer > BigDecimal.ZERO) usdToYer else BigDecimal("530.0")

        val converted = when (fromCurrency.uppercase()) {
            CURRENCY_SAR -> amount.multiply(defaultSarRate)
            CURRENCY_USD -> amount.multiply(defaultUsdRate)
            else -> amount
        }
        return converted.setScale(2, RoundingMode.HALF_UP)
    }

    fun convertToYer(
        amount: Double,
        fromCurrency: String,
        sarToYer: Double,
        usdToYer: Double
    ): Double {
        return convertToYer(
            BigDecimal.valueOf(amount),
            fromCurrency,
            BigDecimal.valueOf(sarToYer),
            BigDecimal.valueOf(usdToYer)
        ).toDouble()
    }

    /**
     * تحويل من الريال اليمني إلى عملة أخرى
     */
    fun convertFromYer(
        amountInYer: BigDecimal,
        targetCurrency: String,
        sarToYer: BigDecimal,
        usdToYer: BigDecimal
    ): BigDecimal {
        if (amountInYer <= BigDecimal.ZERO) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        val defaultSarRate = if (sarToYer > BigDecimal.ZERO) sarToYer else BigDecimal("140.0")
        val defaultUsdRate = if (usdToYer > BigDecimal.ZERO) usdToYer else BigDecimal("530.0")

        val converted = when (targetCurrency.uppercase()) {
            CURRENCY_SAR -> amountInYer.divide(defaultSarRate, 4, RoundingMode.HALF_UP)
            CURRENCY_USD -> amountInYer.divide(defaultUsdRate, 4, RoundingMode.HALF_UP)
            else -> amountInYer
        }
        return converted.setScale(2, RoundingMode.HALF_UP)
    }

    fun convertFromYer(
        amountInYer: Double,
        targetCurrency: String,
        sarToYer: Double,
        usdToYer: Double
    ): Double {
        return convertFromYer(
            BigDecimal.valueOf(amountInYer),
            targetCurrency,
            BigDecimal.valueOf(sarToYer),
            BigDecimal.valueOf(usdToYer)
        ).toDouble()
    }

    /**
     * تحويل بين أي عملتين
     */
    fun convertBetween(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        sarToYer: BigDecimal,
        usdToYer: BigDecimal
    ): BigDecimal {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount.setScale(2, RoundingMode.HALF_UP)
        val amountInYer = convertToYer(amount, fromCurrency, sarToYer, usdToYer)
        return convertFromYer(amountInYer, toCurrency, sarToYer, usdToYer)
    }

    fun convertBetween(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        sarToYer: Double,
        usdToYer: Double
    ): Double {
        return convertBetween(
            BigDecimal.valueOf(amount),
            fromCurrency,
            toCurrency,
            BigDecimal.valueOf(sarToYer),
            BigDecimal.valueOf(usdToYer)
        ).toDouble()
    }

    /**
     * تنسيق المبلغ مع رمز العملة عبر الدالة الموحدة toCleanCurrency
     */
    fun formatAmount(amount: BigDecimal, currency: String = CURRENCY_YER): String {
        return amount.toCleanCurrency(currency)
    }

    fun formatAmount(amount: Double, currency: String = CURRENCY_YER): String {
        return amount.toCleanCurrency(currency)
    }

    fun formatAmount(amount: Number, currency: String = CURRENCY_YER): String {
        return amount.toDouble().toCleanCurrency(currency)
    }
}
