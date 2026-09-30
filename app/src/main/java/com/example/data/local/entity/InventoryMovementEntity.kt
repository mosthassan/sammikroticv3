package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * يمثل حركة مخزنية رسمية (توريد رصيد بالعدد + أو خصم مبيعات فاتورة -)
 * لتوثيق حركة كل صنف بدقة محاسبية متناهية
 */
@Entity(tableName = "inventory_movements")
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val movementType: String, // "SUPPLY" (توريد كروت للمخزن +), "SALE" (بيع بفاتورة -), "ADJUSTMENT" (تعديل جرد)
    val quantityChange: Int, // موجب في التوريد، سالب في البيع
    val resultingBalance: Int, // الرصيد المتبقي بعد الحركة
    val referenceNumber: String = "", // رقم الفاتورة أو مرجع التوريد
    val customerOrSupplier: String = "",
    val unitPrice: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
