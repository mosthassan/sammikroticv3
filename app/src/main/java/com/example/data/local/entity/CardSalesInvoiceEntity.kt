package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل فاتورة مبيعات كروت متعددة الأصناف في النظام المحاسبي
 * مرتبطة بحسابات المخزن لخصم الكروت بالعدد مباشرة، وبحسابات العملاء والبقالات
 */
@Entity(tableName = "card_sales_invoices")
data class CardSalesInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String = "",
    val retailerId: Long? = null,
    val invoiceDateMillis: Long = System.currentTimeMillis(),
    val paymentType: String = "CASH", // "CASH" (نقد), "CREDIT" (آجل), "PARTIAL" (مقدم ومتبقي)
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val remainingAmount: BigDecimal = BigDecimal.ZERO,
    val totalCardsCount: Int = 0,
    val itemsCount: Int = 0,
    val itemsSummary: String = "",
    val itemsJson: String = "[]",
    val notes: String = "",
    val issuerName: String = "المهندس حسن",
    val status: String = "PAID", // "PAID", "CREDIT", "PARTIAL"
    val createdAt: Long = System.currentTimeMillis()
)
