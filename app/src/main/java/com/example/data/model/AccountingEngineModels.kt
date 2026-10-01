package com.example.data.model

import java.math.BigDecimal

/**
 * يمثل ملخص إقفال الدورة المالية وتوزيع الأرباح التشغيلية وفق المعايير المحاسبية
 */
data class PeriodClosingSummary(
    val startDateMillis: Long,
    val endDateMillis: Long,
    val totalGrossRevenue: BigDecimal,        // 4101 + 4201
    val cardSalesRevenue: BigDecimal = totalGrossRevenue,           // 4101
    val directSubscriptionsRevenue: BigDecimal = BigDecimal.ZERO,  // 4201
    val totalCogs: BigDecimal,                // 5101
    val totalOperatingExpenses: BigDecimal,   // 5201
    val totalMaintenanceExpenses: BigDecimal, // 5202
    val totalDepreciation: BigDecimal,        // 5203
    val totalExpenses: BigDecimal,            // COGS + Operating + Maintenance + Depreciation
    val netOperatingProfit: BigDecimal,       // Gross Revenue - Total Expenses
    val partnerShares: List<PartnerPeriodShare>
)

/**
 * حصة الشريك من صافي الأرباح التشغيلية المعتمدة للإقفال
 */
data class PartnerPeriodShare(
    val partnerId: Long,
    val partnerName: String,
    val sharePercentage: Double,
    val allocatedProfit: BigDecimal
)

/**
 * تفاصيل نتيجة قيد إهلاك الأصول الثابتة
 */
data class AssetDepreciationResult(
    val totalDepreciationAmount: BigDecimal,
    val assetsProcessedCount: Int,
    val periodMonths: Int,
    val entryHeaderId: Long,
    val items: List<AssetDepreciationItem>
)

data class AssetDepreciationItem(
    val assetId: Long,
    val assetName: String,
    val category: String,
    val purchaseCost: BigDecimal,
    val previousBookValue: BigDecimal,
    val depreciationAmount: BigDecimal,
    val newBookValue: BigDecimal
)
