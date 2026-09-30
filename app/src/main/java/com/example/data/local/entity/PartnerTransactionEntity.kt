package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل حركة مالية خاصة بالشركاء (توزيع أرباح، زيادة رأس مال، مسحوبات خاصة)
 */
@Entity(tableName = "partner_transactions")
data class PartnerTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partnerId: Long,
    val partnerName: String,
    val transactionType: String,              // DIVIDEND_PAYOUT (استلام أرباح), CAPITAL_ADDITION (إيداع رأس مال), DRAWING (مسحوبات خاصة)
    val amount: BigDecimal,                   // المبلغ بالريال اليمني
    val currency: String = "YER",             // العملة: YER, SAR, USD
    val originalAmount: BigDecimal = BigDecimal.ZERO, // المبلغ بالعملة الأصلية
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val referenceVoucherId: Long? = null
)
