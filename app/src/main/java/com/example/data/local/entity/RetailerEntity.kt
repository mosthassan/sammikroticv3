package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل وكيلاً أو بقالة لتوزيع كروت الشبكة
 * تم تحويل الأرصدة والمديونيات إلى BigDecimal لضمان دقة الحسابات المالية
 */
@Entity(tableName = "retailers")
data class RetailerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // e.g. "بقالة البركة والخير"
    val ownerName: String = "",          // e.g. "أبو فهد الحبيشي"
    val phone: String = "",              // e.g. "771234567"
    val location: String = "",           // e.g. "حي الجامعة - شارع 14"
    val commissionPercent: Double = 10.0,// نسبة عمولة البقالة
    val balanceOwed: BigDecimal = BigDecimal.ZERO, // المبلغ المستحق على البقالة للشبكة
    val totalPaid: BigDecimal = BigDecimal.ZERO,   // إجمالي المبالغ المسددة
    val activeCardsCount: Int = 0,       // الكروت الموجودة حالياً لديه
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    @Ignore
    constructor(
        id: Long = 0,
        name: String,
        ownerName: String = "",
        phone: String = "",
        location: String = "",
        commissionPercent: Double = 10.0,
        balanceOwed: Double,
        totalPaid: Double = 0.0,
        activeCardsCount: Int = 0,
        notes: String = "",
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        name = name,
        ownerName = ownerName,
        phone = phone,
        location = location,
        commissionPercent = commissionPercent,
        balanceOwed = BigDecimal.valueOf(balanceOwed),
        totalPaid = BigDecimal.valueOf(totalPaid),
        activeCardsCount = activeCardsCount,
        notes = notes,
        createdAt = createdAt
    )

    val balanceOwedDouble: Double get() = balanceOwed.toDouble()
    val totalPaidDouble: Double get() = totalPaid.toDouble()
}
