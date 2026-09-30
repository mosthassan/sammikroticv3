package com.example.data.model

import java.util.UUID

/**
 * صنف مفرد ضمن فاتورة المشتريات أو الأصول
 */
data class InvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var quantity: Double = 1.0,
    var unitPrice: Double = 0.0,
    var subtotal: Double = quantity * unitPrice,
    var category: String = "GENERAL", // SERVERS, TOWERS, SOLAR_POWER, CABLES, FUEL, MAINTENANCE, GENERAL
    var currency: String = ""
)

/**
 * بيانات الفاتورة المحللة والمستخرجة بواسطة الذكاء الاصطناعي (Gemini Vision)
 */
data class ParsedInvoiceData(
    val supplierName: String = "",
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val invoiceType: String = "ASSETS", // "ASSETS" (أصول ثابتة) or "EXPENSES" (مصروفات ومشتريات تشغيلية)
    val totalAmount: Double = 0.0,
    val currency: String = "YER",
    val notes: String = "",
    val items: List<InvoiceItem> = emptyList(),
    val rawAiAnalysis: String = ""
)
