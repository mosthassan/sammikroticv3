package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل فاتورة مشتريات أو أصول معتمدة في النظام
 */
@Entity(tableName = "purchase_invoices")
data class PurchaseInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val supplierName: String,
    val invoiceDateMillis: Long = System.currentTimeMillis(),
    val targetType: String = "ASSETS", // "ASSETS" (أصول ثابتة), "EXPENSES" (مصروفات تشغيلية)
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",      // العملة: USD, SAR, YER
    val originalAmount: BigDecimal = BigDecimal.ZERO,  // المبلغ بالعملة الأصلية
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val paymentMethod: String = "نقداً",
    val status: String = "APPROVED", // "APPROVED", "DRAFT", "VOIDED"
    val isVoided: Boolean = false,
    val voidReason: String = "",
    val notes: String = "",
    val itemsSummary: String = "",
    val itemsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)
