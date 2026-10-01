package com.example.data.local.model

import java.math.BigDecimal

/**
 * تقرير قائمة الدخل المعيارية (Income Statement / P&L)
 * مستخرج حصرياً من القيود المحاسبية المزدوجة الموزونة (Single Source of Truth)
 */
data class IncomeStatementReport(
    val startDateMillis: Long,
    val endDateMillis: Long,
    val cardSalesRevenue: BigDecimal,        // 4101 (Credit - Debit)
    val subscriptionRevenue: BigDecimal,     // 4201 (Credit - Debit)
    val totalOperationalRevenue: BigDecimal, // 4101 + 4201
    val cogs: BigDecimal,                    // 5101 (Debit - Credit)
    val grossProfit: BigDecimal,             // Revenue - COGS
    val operatingExpenses: BigDecimal,       // 5201 (Debit - Credit)
    val maintenanceExpenses: BigDecimal,     // 5202 (Debit - Credit)
    val totalOpexExpenses: BigDecimal,       // 5201 + 5202
    val depreciationExpense: BigDecimal,     // 5203 (Debit - Credit)
    val totalExpenses: BigDecimal,           // COGS + 5201 + 5202 + 5203
    val netOperatingProfit: BigDecimal,      // Gross Profit - (Opex + Maintenance + Depreciation)
    val grossMarginPercentage: Double,       // (Gross Profit / Revenue) * 100
    val netProfitMarginPercentage: Double    // (Net Profit / Revenue) * 100
)

data class IncomeStatementRow(
    val accountCode: String,
    val accountName: String,
    val totalDebit: BigDecimal,
    val totalCredit: BigDecimal,
    val normalBalance: String,
    val accountType: String
)
