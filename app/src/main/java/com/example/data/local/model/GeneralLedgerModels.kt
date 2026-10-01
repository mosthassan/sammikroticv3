package com.example.data.local.model

import java.math.BigDecimal

/**
 * سطر حركة مستخرج من استعلام قاعدة البيانات لدفتر الأستاذ
 */
data class GeneralLedgerLineRow(
    val lineId: Long,
    val headerId: Long,
    val entryNumber: String,
    val dateMillis: Long,
    val accountCode: String,
    val accountName: String,
    val lineDescription: String,
    val headerDescription: String,
    val debit: BigDecimal,
    val credit: BigDecimal,
    val referenceType: String,
    val referenceId: String
)

/**
 * سطر حركة كامل في دفتر الأستاذ مع الرصيد التراكمي المحتسب
 */
data class GeneralLedgerItem(
    val lineId: Long,
    val headerId: Long,
    val entryNumber: String,
    val dateMillis: Long,
    val accountCode: String,
    val accountName: String,
    val description: String,
    val debit: BigDecimal,
    val credit: BigDecimal,
    val runningBalance: BigDecimal,
    val referenceType: String,
    val referenceId: String
)

/**
 * كائن تقرير دفتر الأستاذ العام لحساب معين في فترة زمنية
 */
data class GeneralLedgerReport(
    val accountCode: String,
    val accountName: String,
    val accountType: String,
    val normalBalance: String, // "DEBIT" or "CREDIT"
    val startDateMillis: Long,
    val endDateMillis: Long,
    val openingBalance: BigDecimal,
    val totalPeriodDebit: BigDecimal,
    val totalPeriodCredit: BigDecimal,
    val closingBalance: BigDecimal,
    val lines: List<GeneralLedgerItem>
)

/**
 * مجموع المدين والدائن للرصيد الافتتاحي
 */
data class AccountDebitCreditSum(
    val totalDebit: BigDecimal,
    val totalCredit: BigDecimal
)
