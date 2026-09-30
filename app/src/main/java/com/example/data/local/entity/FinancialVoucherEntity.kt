package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "financial_vouchers")
data class FinancialVoucherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val voucherNumber: String,           // e.g. "REC-2026-0101" or "PAY-2026-0042"
    val voucherType: String,             // "RECEIPT" (سند قبض) or "PAYMENT" (سند صرف)
    val amount: BigDecimal,              // المبلغ بالريال اليمني
    val currency: String = "YER",        // العملة: YER, SAR, USD
    val originalAmount: BigDecimal = BigDecimal.ZERO, // المبلغ بالعملة الأصلية إن وجدت
    val partyName: String,               // استلمنا من / صرفنا إلى (مثلا "بقالة النور", "شركة الألياف الضوئية", "صيانة الأبراج")
    val retailerId: Long? = null,        // إذا كان السند مرتبط بحساب بقالة معينة
    val invoiceId: Long? = null,         // معرف الفاتورة المرتبطة إن وجد
    val invoiceNumber: String = "",       // رقم الفاتورة المرتبطة إن وجد
    val allocatedAmount: BigDecimal = BigDecimal.ZERO, // المبلغ المخصص للفواتير
    val category: String,                // "مبيعات كروت", "اشتراك نت رئيسي", "ديزل وطاقة شمسية", "صيانة ومعدات", "رواتب مهندسين", "مصاريف أخرى"
    val paymentMethod: String = "نقداً", // "نقداً", "تحويل كاش / بنكي", "حوالة صرافة"
    val description: String,             // البيان والتفاصيل
    val issuerName: String = "المهندس سام", // المحرر / المسؤول
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isVoided: Boolean = false        // حالة إلغاء السند المالي
)
