package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل سطراً تفصيلياً في قيد اليومية المحاسبي المزدوج (Journal Entry Line Item)
 * يرتبط مباشرة بترويسة القيد بحذف متتالي (CASCADE)
 */
@Entity(
    tableName = "journal_entry_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryHeaderEntity::class,
            parentColumns = ["id"],
            childColumns = ["headerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["headerId"]),
        Index(value = ["accountCode"]),
        Index(value = ["partyType", "partyId"])
    ]
)
data class JournalEntryLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val headerId: Long,                  // المعرف المرتبط بترويسة القيد
    val accountCode: String,             // كود الحساب من شجرة الحسابات (مثل 1101, 1201, 4101)
    val accountName: String,             // اسم الحساب وقت إنشاء القيد
    val lineType: String,                // "DEBIT" (مدين) أو "CREDIT" (دائن)
    val debit: BigDecimal = BigDecimal.ZERO,   // المبلغ المدين بالعملة الأساسية (YER)
    val credit: BigDecimal = BigDecimal.ZERO,  // المبلغ الدائن بالعملة الأساسية (YER)
    val currency: String = "YER",        // عملة الحركة: YER, SAR, USD
    val exchangeRate: BigDecimal = BigDecimal.ONE, // سعر الصرف المعتمد وقت المعاملة
    val originalAmount: BigDecimal = BigDecimal.ZERO, // المبلغ بالعملة الأجنبية قبل التحويل
    val lineDescription: String = "",    // بيان السطر التوضيحي
    val partyId: Long? = null,           // معرف الشريك، البقالة، أو المورد (إن وجد)
    val partyType: String? = null        // "PARTNER", "RETAILER", "SUPPLIER"
)
