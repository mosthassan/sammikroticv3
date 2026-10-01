package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل حركة مخزنية رسمية (توريد رصيد بالعدد + أو خصم مبيعات فاتورة -)
 * لتوثيق حركة كل صنف بدقة محاسبية متناهية
 */
@Entity(tableName = "inventory_movements")
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val movementType: String, // "SUPPLY" (توريد كروت للمخزن +), "SALE" (بيع بفاتورة -), "RETURN" (استرجاع/إلغاء), "ADJUSTMENT" (تعديل جرد)
    val quantityChange: Int, // موجب في التوريد، سالب في البيع
    val resultingBalance: Int, // الرصيد المتبقي بعد الحركة
    val referenceNumber: String = "", // رقم الفاتورة أو مرجع التوريد
    val customerOrSupplier: String = "",
    val unitPrice: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    @Ignore
    constructor(
        id: Long = 0,
        packageName: String,
        movementType: String,
        quantityChange: Int,
        resultingBalance: Int,
        referenceNumber: String = "",
        customerOrSupplier: String = "",
        unitPrice: BigDecimal,
        notes: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        packageName = packageName,
        movementType = movementType,
        quantityChange = quantityChange,
        resultingBalance = resultingBalance,
        referenceNumber = referenceNumber,
        customerOrSupplier = customerOrSupplier,
        unitPrice = unitPrice.toDouble(),
        notes = notes,
        timestamp = timestamp
    )
}
