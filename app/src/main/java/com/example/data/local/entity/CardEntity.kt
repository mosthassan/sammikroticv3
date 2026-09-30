package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["batchId"]),
        Index(value = ["retailerId"])
    ]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long,
    val username: String,                // رمز المستخدم
    val password: String,                // كلمة المرور
    val categoryName: String,            // فئة الكرت
    val retailPrice: Double,             // سعر البيع للمستهلك
    val wholesalePrice: Double,          // سعر البيع للمحل
    val status: String = "AVAILABLE",    // "AVAILABLE", "DISTRIBUTED", "SOLD", "EXPIRED"
    val retailerId: Long? = null,        // معرف البقالة المستلمة
    val retailerName: String? = null,    // اسم البقالة
    val distributedAt: Long? = null,
    val soldAt: Long? = null,
    val invoiceId: Long? = null,         // معرف الفاتورة التي تم بيع الكرت فيها
    val createdAt: Long = System.currentTimeMillis()
)
