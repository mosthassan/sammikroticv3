package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل قيود اليومية المحاسبية المزدوجة (Double-Entry Journal Entries)
 * لتوثيق كافة الحركات المالية (مدين ودائن)
 */
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryNumber: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val referenceType: String = "",
    val referenceId: String = "",
    val debitAccount: String = "",
    val creditAccount: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "YER",
    val exchangeRate: BigDecimal = BigDecimal.ONE,
    val description: String = "",
    val createdBy: String = ""
)
