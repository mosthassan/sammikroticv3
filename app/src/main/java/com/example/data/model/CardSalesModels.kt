package com.example.data.model

import java.util.UUID

/**
 * يمثل صنفاً مفرداً ضمن فاتورة مبيعات الكروت متعددة الأصناف
 */
data class CardSalesInvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    var packageName: String = "",
    var quantity: Int = 1,
    var unitPrice: Double = 0.0,
    var retailPrice: Double = 0.0
) {
    /** الإجمالي المحسوب للسطر: العدد × سعر الكرت */
    val lineTotal: Double
        get() = quantity * unitPrice

    /** إجمالي قيمة البيع للجمهور المقدرة */
    val retailTotal: Double
        get() = quantity * retailPrice

    /** هامش ربح البقالة/العميل المتوقع في هذا الصنف */
    val estimatedProfit: Double
        get() = (retailTotal - lineTotal).coerceAtLeast(0.0)
}
