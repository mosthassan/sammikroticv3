package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل ترويسة قيد اليومية المحاسبي المزدوج (Double-Entry Journal Header)
 * يضمن التوثيق الإلزامي والربط المرجعي والتدقيق المالي
 */
@Entity(
    tableName = "journal_entry_headers",
    indices = [
        Index(value = ["entryNumber"], unique = true),
        Index(value = ["dateMillis"]),
        Index(value = ["referenceType", "referenceId"])
    ]
)
data class JournalEntryHeaderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryNumber: String,             // رقم القيد التسلسلي، مثل: JE-2026-0001
    val dateMillis: Long = System.currentTimeMillis(),
    val referenceType: String = "MANUAL",// "SALES_INVOICE", "PURCHASE_INVOICE", "FINANCIAL_VOUCHER", "PARTNER_TRANSACTION", "CLOSING_ENTRY", "MANUAL", "VOID_REVERSAL"
    val referenceId: String = "",        // رقم الفاتورة أو السند المرتبط
    val description: String = "",        // البيان العام للقيد المحاسبي
    val status: String = "POSTED",       // "POSTED" (مرحل ومعتمد), "VOIDED" (ملغى بقيد عكسي)
    val totalDebit: BigDecimal = BigDecimal.ZERO,  // إجمالي المبالغ المدينة
    val totalCredit: BigDecimal = BigDecimal.ZERO, // إجمالي المبالغ الدائنة
    val createdBy: String = "النظام المحاسبي",
    val createdAt: Long = System.currentTimeMillis(),
    val voidReason: String? = null,      // سبب الإلغاء في حال تم إلغاء القيد
    val reversalOfEntryId: Long? = null  // معرف القيد الأصلي في حال كان هذا القيد قيداً عكسياً
)
