package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card_batches")
data class CardBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchName: String,               // e.g. "دفعة رمضان فئة 200 ريال"
    val categoryName: String,            // e.g. "200 ريال - 1.5GB"
    val retailPrice: Double,             // سعر البيع للمستهلك (مثلاً 200 ريال)
    val wholesalePrice: Double,          // سعر الجملة للبقالة (مثلاً 180 ريال)
    val quotaMb: Long,                   // الحجم بالميجابايت (1500 MB)
    val validityHours: Int,              // الصلاحية بالساعات (24 ساعة)
    val speedLimit: String = "4M/2M",    // سرعة التنزيل والرفع
    val totalCount: Int,                 // إجمالي عدد الكروت
    val mikrotikProfile: String = "profile-200r",
    val prefix: String = "SAM",
    val createdAt: Long = System.currentTimeMillis()
)
