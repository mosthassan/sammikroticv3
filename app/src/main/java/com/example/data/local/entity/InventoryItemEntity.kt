package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,                            // e.g., "فئة 200 ريال"
    val quantityAvailable: Int,                         // الرصيد المخزني الفعلي للكروت
    val wholesalePrice: BigDecimal = BigDecimal.ZERO,   // سعر بيع الجملة (للبقالة)
    val retailPrice: BigDecimal = BigDecimal.ZERO,      // سعر البيع للجمهور
    val costPrice: BigDecimal = BigDecimal.ZERO,        // سعر التكلفة المباشرة لإنتاج/شراء الكرت (COGS)
    val createdAt: Long = System.currentTimeMillis()
) {
    @Ignore
    constructor(
        id: Long = 0,
        packageName: String,
        quantityAvailable: Int,
        wholesalePrice: Double,
        retailPrice: Double,
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        packageName = packageName,
        quantityAvailable = quantityAvailable,
        wholesalePrice = BigDecimal.valueOf(wholesalePrice),
        retailPrice = BigDecimal.valueOf(retailPrice),
        costPrice = BigDecimal.ZERO,
        createdAt = createdAt
    )

    val wholesalePriceDouble: Double get() = wholesalePrice.toDouble()
    val retailPriceDouble: Double get() = retailPrice.toDouble()
    val costPriceDouble: Double get() = costPrice.toDouble()
}
