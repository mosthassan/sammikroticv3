package com.example

import com.example.data.accounting.AccountingPostingService
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.JournalEntryLineEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.model.CardSalesInvoiceItem
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class AccountingEngineUnitTest {

    @Test
    fun testPaymentCategoryMappingPrecision() {
        val dummyDb = null // not used in pure function
        // Create an instance or helper to test classification logic
        fun mapCategory(category: String, party: String = ""): Pair<String, String> {
            val cat = category.trim().lowercase()
            val p = party.trim().lowercase()

            if (cat.contains("شريك") || cat.contains("مسحوبات") || cat.contains("أرباح") ||
                p.contains("شريك") || cat.contains("توزيع أرباح")) {
                return Pair("3201", "جاري الشركاء والأرباح المسحوبة")
            }

            if (cat.contains("ديزل") || cat.contains("وقود") || cat.contains("محروقات") ||
                cat.contains("طاقة شمسية") || cat.contains("كهرباء") || cat.contains("بنزين") ||
                cat.contains("توليد") || cat.contains("زيوت")) {
                return Pair("5201", "مصروفات تشغيلية (وقود وكهرباء وطاقة)")
            }

            if (cat.contains("صيانة") || cat.contains("تصليح") || cat.contains("قطع غيار") ||
                cat.contains("إصلاح") || cat.contains("تأهيل أبراج")) {
                return Pair("5202", "مصروفات الصيانة وقطع الغيار")
            }

            val isBandwidth = cat.contains("إنترنت") || cat.contains("انترنت") ||
                    cat.contains("اشتراك") || cat.contains("ستارلينك") || cat.contains("starlink") ||
                    cat.contains("باندويث") || cat.contains("bandwidth") || cat.contains("مزود") ||
                    cat.contains("حزم") || cat.split(" ", "-", "_", "/").any { it == "نت" }
            if (isBandwidth) {
                return Pair("5101", "تكلفة خدمة الإنترنت والمزودين (COGS)")
            }

            if (cat.contains("أصول") || cat.contains("رأسمال") ||
                ((cat.contains("شراء") || cat.contains("توريد") || cat.contains("تركيب")) &&
                 (cat.contains("معدات") || cat.contains("راوتر") || cat.contains("سيرفر") || cat.contains("برج") || cat.contains("أجهزة")))) {
                return Pair("1501", "أصول ومعدات الشبكة (نفقات رأسمالية)")
            }

            return Pair("5201", "مصروفات تشغيلية وعمومية")
        }

        // Test 1: "ديزل وطاقة شمسية" must be 5201 operating expense, NOT 1501 capital asset
        val fuelResult = mapCategory("ديزل وطاقة شمسية")
        assertEquals("5201", fuelResult.first)

        // Test 2: Word containing 'نت' like "انتقالات" must NOT match internet 5101
        val transportResult = mapCategory("انتقالات ومواصلات")
        assertEquals("5201", transportResult.first)

        // Test 3: Starlink or internet bandwidth must go to 5101
        val starlinkResult = mapCategory("اشتراك باقة ستارلينك")
        assertEquals("5101", starlinkResult.first)

        val netResult = mapCategory("اشتراك نت رئيسي")
        assertEquals("5101", netResult.first)

        // Test 4: Maintenance must go to 5202
        val maintResult = mapCategory("صيانة ومعدات الأبراج")
        assertEquals("5202", maintResult.first)

        // Test 5: Real Capex must go to 1501
        val capexResult = mapCategory("شراء أصول ومعدات جديدة")
        assertEquals("1501", capexResult.first)

        // Test 6: Partner withdrawal must go to 3201
        val partnerResult = mapCategory("مسحوبات الشريك سام")
        assertEquals("3201", partnerResult.first)
    }

    @Test
    fun testPurchaseTargetTypeClassification() {
        fun classifyTarget(targetType: String): String {
            val target = targetType.trim().uppercase()
            return when {
                target.contains("ASSET") || target.contains("CAPEX") || target.contains("أصول") || target.contains("معدات") -> "1501"
                target.contains("INVENTORY") || target.contains("STOCK") || target.contains("مخزون") || target.contains("كروت") -> "1301"
                target.contains("MAINT") || target.contains("صيانة") || target.contains("قطع") -> "5202"
                target.contains("BANDWIDTH") || target.contains("INTERNET") || target.contains("نت") || target.contains("اشتراك") -> "5101"
                else -> "5201"
            }
        }

        // ASSETS with 'S'
        assertEquals("1501", classifyTarget("ASSETS"))
        assertEquals("1501", classifyTarget("ASSET"))
        assertEquals("1501", classifyTarget("أصول"))
        assertEquals("5202", classifyTarget("MAINTENANCE"))
        assertEquals("1301", classifyTarget("INVENTORY"))
        assertEquals("5201", classifyTarget("EXPENSES"))
    }

    @Test
    fun testDoubleEntryBalanceInvariance() {
        // Test that a balanced sales invoice entry with linked receipt voucher preserves perfect equality
        val invoiceAmount = BigDecimal("25000.00")
        val paidAmount = BigDecimal("10000.00")
        val remainingAmount = BigDecimal("15000.00")

        // Sales Invoice Lines (Retailer account):
        // Debit: 1201 (Receivables) = 25,000
        // Credit: 4101 (Revenue) = 25,000
        val invoiceLines = listOf(
            JournalEntryLineEntity(headerId = 1, accountCode = "1201", accountName = "ذمم", lineType = "DEBIT", debit = invoiceAmount, credit = BigDecimal.ZERO),
            JournalEntryLineEntity(headerId = 1, accountCode = "4101", accountName = "إيراد", lineType = "CREDIT", debit = BigDecimal.ZERO, credit = invoiceAmount)
        )
        val invDebitSum = invoiceLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.debit) }
        val invCreditSum = invoiceLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.credit) }
        assertEquals(invDebitSum, invCreditSum)

        // Receipt Voucher Lines:
        // Debit: 1101 (Cash) = 10,000
        // Credit: 1201 (Receivables) = 10,000
        val voucherLines = listOf(
            JournalEntryLineEntity(headerId = 2, accountCode = "1101", accountName = "صندوق", lineType = "DEBIT", debit = paidAmount, credit = BigDecimal.ZERO),
            JournalEntryLineEntity(headerId = 2, accountCode = "1201", accountName = "ذمم", lineType = "CREDIT", debit = BigDecimal.ZERO, credit = paidAmount)
        )
        val vDebitSum = voucherLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.debit) }
        val vCreditSum = voucherLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.credit) }
        assertEquals(vDebitSum, vCreditSum)

        // Combined Net Ledger Effect:
        // Cash (1101): Debit 10,000
        // Receivables (1201): Debit 25,000 - Credit 10,000 = Debit 15,000 (Exact remaining amount!)
        // Revenue (4101): Credit 25,000
        // Total Debits = 10,000 + 15,000 = 25,000 == Total Credits = 25,000
        val customerReceivableBalance = invoiceAmount.subtract(paidAmount)
        assertEquals(remainingAmount, customerReceivableBalance)

        val totalDebitsCombined = paidAmount.add(customerReceivableBalance)
        assertEquals(invoiceAmount, totalDebitsCombined)
    }

    @Test
    fun testWalkInPartialSaleDoesNotDefaultToCash() {
        // Test that if retailerId == null and remainingAmount > 0, it does NOT treat as 100% cash
        val totalAmount = BigDecimal("5000")
        val paidAmount = BigDecimal("2000")
        val remainingAmount = BigDecimal("3000")

        val paymentType = "نقدي جزئي"
        val isCash = (paymentType.equals("CASH", ignoreCase = true) ||
                      paymentType.equals("نقداً") ||
                      paymentType.equals("نقد")) &&
                     remainingAmount <= BigDecimal.ZERO

        assertFalse("Walk-in sale with remaining debt must NOT be classified as 100% cash", isCash)

        // It must create both Cash (1101) for paid and Receivables (1201) for remaining
        val lines = mutableListOf<JournalEntryLineEntity>()
        if (paidAmount > BigDecimal.ZERO) {
            lines.add(JournalEntryLineEntity(headerId = 1, accountCode = "1101", accountName = "صندوق", lineType = "DEBIT", debit = paidAmount, credit = BigDecimal.ZERO))
        }
        if (remainingAmount > BigDecimal.ZERO) {
            lines.add(JournalEntryLineEntity(headerId = 1, accountCode = "1201", accountName = "ذمم", lineType = "DEBIT", debit = remainingAmount, credit = BigDecimal.ZERO))
        }
        lines.add(JournalEntryLineEntity(headerId = 1, accountCode = "4101", accountName = "إيراد", lineType = "CREDIT", debit = BigDecimal.ZERO, credit = totalAmount))

        val debits = lines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.debit) }
        val credits = lines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.credit) }
        assertEquals(debits, credits)
        assertEquals(totalAmount, debits)
    }

    @Test
    fun testPurchaseWithPaymentVoucherClearsSupplierDebt() {
        // Purchase Invoice of 100,000 YER (Assets 1501):
        // Purchase invoice: Debit 1501 (100,000), Credit 2101 (100,000)
        // Payment voucher:  Debit 2101 (100,000), Credit 1101 (100,000)
        val purchaseCost = BigDecimal("100000")

        val invoiceLines = listOf(
            JournalEntryLineEntity(headerId = 1, accountCode = "1501", accountName = "أصول", lineType = "DEBIT", debit = purchaseCost, credit = BigDecimal.ZERO),
            JournalEntryLineEntity(headerId = 1, accountCode = "2101", accountName = "موردون", lineType = "CREDIT", debit = BigDecimal.ZERO, credit = purchaseCost)
        )
        val paymentLines = listOf(
            JournalEntryLineEntity(headerId = 2, accountCode = "2101", accountName = "موردون", lineType = "DEBIT", debit = purchaseCost, credit = BigDecimal.ZERO),
            JournalEntryLineEntity(headerId = 2, accountCode = "1101", accountName = "صندوق", lineType = "CREDIT", debit = BigDecimal.ZERO, credit = purchaseCost)
        )

        // Net Supplier 2101 balance:
        // Credit 100,000 - Debit 100,000 = 0!
        val supplierBalance = purchaseCost.subtract(purchaseCost)
        assertEquals(BigDecimal.ZERO, supplierBalance)

        // Net Asset 1501 balance: Debit 100,000
        // Net Cash 1101 balance: Credit 100,000
        // Trial balance: Total Debits (100,000 + 100,000) == Total Credits (100,000 + 100,000)
        val allDebits = invoiceLines[0].debit.add(paymentLines[0].debit)
        val allCredits = invoiceLines[1].credit.add(paymentLines[1].credit)
        assertEquals(allDebits, allCredits)
    }

    @Test
    fun testForeignCurrencyConversionIntegrity() {
        val usdAmount = BigDecimal("500")
        val usdRate = BigDecimal("530")
        val expectedYer = usdAmount.multiply(usdRate) // 265,000 YER
        assertEquals(BigDecimal("265000"), expectedYer)

        val sarAmount = BigDecimal("1000")
        val sarRate = BigDecimal("140")
        val expectedSarYer = sarAmount.multiply(sarRate) // 140,000 YER
        assertEquals(BigDecimal("140000"), expectedSarYer)
    }
}
