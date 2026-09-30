package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "customer_ledger",
    indices = [
        Index(value = ["customerId"])
    ]
)
data class CustomerLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val transactionDate: Long,
    val transactionType: String, // "INVOICE", "PAYMENT", "RETURN", "DISCOUNT"
    val referenceId: String,     // e.g., "INV-2026-0022", "REC-2026-56"
    val debit: BigDecimal = BigDecimal.ZERO,  // Increases customer debt / Sales
    val credit: BigDecimal = BigDecimal.ZERO, // Decreases customer debt / Payments
    val description: String? = null
)
