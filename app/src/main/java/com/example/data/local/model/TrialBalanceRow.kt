package com.example.data.local.model

import java.math.BigDecimal

data class TrialBalanceRow(
    val accountCode: String,
    val accountName: String,
    val totalDebit: BigDecimal,
    val totalCredit: BigDecimal,
    val netBalance: BigDecimal,
    val normalBalance: String = "DEBIT",
    val accountType: String = "ASSET"
)
