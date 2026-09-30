package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Stores currency exchange rates to the system base currency (YER).
 */
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey
    val currencyCode: String,            // "USD", "SAR", "YER"
    val rateToBase: BigDecimal,           // 1 USD = 530 YER, 1 SAR = 140 YER, 1 YER = 1 YER
    val updatedAt: Long = System.currentTimeMillis()
)
