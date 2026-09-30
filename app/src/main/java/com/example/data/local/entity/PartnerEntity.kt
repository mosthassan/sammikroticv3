package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * يمثل شريكاً في استثمار وتأسيس الشبكة اللاسلكية
 */
@Entity(tableName = "partners")
data class PartnerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val capitalInvested: Double = 0.0,      // المبلغ المدفوع بالريال اليمني (العملة الأساسية)
    val currency: String = "YER",           // العملة الأصلية للمساهمة: YER, SAR, USD
    val originalCapital: Double = 0.0,      // المبلغ بالعملة الأصلية (مثلاً 50,000 ريال سعودي أو 10,000 دولار)
    val sharePercentage: Double = 0.0,     // النسبة المئوية المتفق عليها من الأرباح (مثلاً 50.0%)
    val joinDateMillis: Long = System.currentTimeMillis(),
    val totalWithdrawnProfit: Double = 0.0, // إجمالي الأرباح المستلمة حتى الآن
    val isActive: Boolean = true,
    val notes: String = ""
)
