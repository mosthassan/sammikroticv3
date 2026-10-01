package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل شريكاً في استثمار وتأسيس الشبكة اللاسلكية
 * تم تحويل كافة الحقول المالية إلى BigDecimal لضمان دقة صفرية في الأرباح ورأس المال
 */
@Entity(tableName = "partners")
data class PartnerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val capitalInvested: BigDecimal = BigDecimal.ZERO,      // المبلغ المدفوع بالريال اليمني (العملة الأساسية)
    val currency: String = "YER",                           // العملة الأصلية للمساهمة: YER, SAR, USD
    val originalCapital: BigDecimal = BigDecimal.ZERO,      // المبلغ بالعملة الأصلية (مثلاً 50,000 ريال سعودي أو 10,000 دولار)
    val sharePercentage: Double = 0.0,                      // النسبة المئوية المتفق عليها من الأرباح (مثلاً 50.0%)
    val joinDateMillis: Long = System.currentTimeMillis(),
    val totalWithdrawnProfit: BigDecimal = BigDecimal.ZERO, // إجمالي الأرباح والمسحوبات المستلمة حتى الآن
    val currentAccountBalance: BigDecimal = BigDecimal.ZERO,// رصيد الحساب الجاري الدائن للشريك
    val isActive: Boolean = true,
    val notes: String = ""
) {
    @Ignore
    constructor(
        id: Long = 0,
        name: String,
        phone: String = "",
        capitalInvested: Double,
        currency: String = "YER",
        originalCapital: Double = 0.0,
        sharePercentage: Double = 0.0,
        joinDateMillis: Long = System.currentTimeMillis(),
        totalWithdrawnProfit: Double = 0.0,
        currentAccountBalance: Double = 0.0,
        isActive: Boolean = true,
        notes: String = ""
    ) : this(
        id = id,
        name = name,
        phone = phone,
        capitalInvested = BigDecimal.valueOf(capitalInvested),
        currency = currency,
        originalCapital = BigDecimal.valueOf(originalCapital),
        sharePercentage = sharePercentage,
        joinDateMillis = joinDateMillis,
        totalWithdrawnProfit = BigDecimal.valueOf(totalWithdrawnProfit),
        currentAccountBalance = BigDecimal.valueOf(currentAccountBalance),
        isActive = isActive,
        notes = notes
    )

    val capitalInvestedDouble: Double get() = capitalInvested.toDouble()
    val originalCapitalDouble: Double get() = originalCapital.toDouble()
    val totalWithdrawnProfitDouble: Double get() = totalWithdrawnProfit.toDouble()
    val currentAccountBalanceDouble: Double get() = currentAccountBalance.toDouble()
}
