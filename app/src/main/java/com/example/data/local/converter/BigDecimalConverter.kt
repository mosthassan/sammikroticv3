package com.example.data.local.converter

import androidx.room.TypeConverter
import java.math.BigDecimal

/**
 * Room TypeConverter for BigDecimal.
 * Stores monetary amounts in SQLite as TEXT (String) to preserve exact precision without floating-point scale drift.
 */
class BigDecimalConverter {
    @TypeConverter
    fun fromString(value: String?): BigDecimal? {
        if (value.isNullOrBlank()) return BigDecimal.ZERO
        return try {
            BigDecimal(value)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    @TypeConverter
    fun toString(value: BigDecimal?): String? {
        return value?.toPlainString()
    }
}
