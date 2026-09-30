package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "retailers")
data class RetailerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // e.g. "بقالة البركة والخير"
    val ownerName: String,               // e.g. "أبو فهد الحبيشي"
    val phone: String,                   // e.g. "771234567"
    val location: String,                // e.g. "حي الجامعة - شارع 14"
    val commissionPercent: Double = 10.0,// نسبة عمولة البقالة
    val balanceOwed: Double = 0.0,       // المبلغ المستحق على البقالة للشبكة
    val totalPaid: Double = 0.0,         // إجمالي المبالغ المسددة
    val activeCardsCount: Int = 0,       // الكروت الموجودة حالياً لديه
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
