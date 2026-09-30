package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,       // e.g., "فئة 200 ريال"
    val quantityAvailable: Int,    // Current stock
    val wholesalePrice: Double,    // سعر بيع الجملة (للبقالة)
    val retailPrice: Double,       // سعر البيع للجمهور
    val createdAt: Long = System.currentTimeMillis()
)
