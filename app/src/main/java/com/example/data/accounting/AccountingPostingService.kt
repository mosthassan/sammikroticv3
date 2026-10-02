package com.example.data.accounting

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.JournalEntryHeaderEntity
import com.example.data.local.entity.JournalEntryLineEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.model.CardSalesInvoiceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Calendar

/**
 * المحرك المركزي الموحد لترحيل العمليات المحاسبية (Single Source of Truth Posting Engine)
 * يضمن ترحيل كافة الفواتير والسندات والمشتريات (اليدوية، المستوردة، والمستعادة)
 * عبر مسار قيد مزدوج متوازن إجبارياً وبشكل Idempotent يمنع التكرار
 */
class AccountingPostingService(private val db: AppDatabase) {

    /**
     * ترحيل فاتورة مبيعات كروت الشبكة
     * - قيد المبيعات (نقد أو آجل أو مختلط)
     * - قيد تكلفة البضاعة المباعة (COGS) ومخزون الكروت
     */
    suspend fun postSalesInvoice(
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem> = emptyList(),
        costMap: Map<String, BigDecimal> = emptyMap(),
        performer: String = invoice.issuerName.ifBlank { "النظام المحاسبي" }
    ): Long? = withContext(Dispatchers.IO) {
        val existing = db.journalEntryDao().getHeaderByReference("SALES_INVOICE", invoice.invoiceNumber)
        if (existing != null) return@withContext existing.id

        if (invoice.totalAmount <= BigDecimal.ZERO) return@withContext null

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val entryNumber = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")

        val lines = mutableListOf<JournalEntryLineEntity>()
        val isCash = invoice.paymentType.equals("CASH", ignoreCase = true) || invoice.paymentType.contains("نقد")
        val isCredit = invoice.paymentType.equals("CREDIT", ignoreCase = true) || invoice.paymentType.contains("آجل")

        if (isCash) {
            // مدين: الصندوق الرئيسي 1101
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "DEBIT",
                    debit = invoice.totalAmount,
                    credit = BigDecimal.ZERO,
                    lineDescription = "مبيعات نقدية فاتورة #${invoice.invoiceNumber} - ${invoice.customerName}"
                )
            )
        } else if (isCredit || invoice.retailerId != null) {
            if (invoice.paidAmount > BigDecimal.ZERO && invoice.remainingAmount > BigDecimal.ZERO) {
                // سداد جزئي: جزء مدين في الصندوق وجزء مدين في ذمم العميل
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "1101",
                        accountName = "الصندوق الرئيسي (النقدية)",
                        lineType = "DEBIT",
                        debit = invoice.paidAmount,
                        credit = BigDecimal.ZERO,
                        lineDescription = "دفعة نقدية مقبوضة لفاتورة #${invoice.invoiceNumber}"
                    )
                )
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "1201",
                        accountName = "ذمم العملاء والوكلاء / ${invoice.customerName}",
                        lineType = "DEBIT",
                        debit = invoice.remainingAmount,
                        credit = BigDecimal.ZERO,
                        partyId = invoice.retailerId,
                        partyType = "RETAILER",
                        lineDescription = "مبيعات آجلة (متبقي) فاتورة #${invoice.invoiceNumber}"
                    )
                )
            } else {
                // آجل كامل: مدين ذمم العملاء والوكلاء 1201
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "1201",
                        accountName = "ذمم العملاء والوكلاء / ${invoice.customerName}",
                        lineType = "DEBIT",
                        debit = invoice.totalAmount,
                        credit = BigDecimal.ZERO,
                        partyId = invoice.retailerId,
                        partyType = "RETAILER",
                        lineDescription = "مبيعات كروت آجلة فاتورة #${invoice.invoiceNumber} - ${invoice.customerName}"
                    )
                )
            }
        } else {
            // افتراضي نقدي
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "DEBIT",
                    debit = invoice.totalAmount,
                    credit = BigDecimal.ZERO,
                    lineDescription = "مبيعات فاتورة #${invoice.invoiceNumber} - ${invoice.customerName}"
                )
            )
        }

        // دائن: إيرادات مبيعات كروت الشبكة 4101
        lines.add(
            JournalEntryLineEntity(
                headerId = 0L,
                accountCode = "4101",
                accountName = "إيرادات مبيعات كروت الشبكة",
                lineType = "CREDIT",
                debit = BigDecimal.ZERO,
                credit = invoice.totalAmount,
                partyId = invoice.retailerId,
                partyType = if (invoice.retailerId != null) "RETAILER" else null,
                lineDescription = "إيراد مبيعات ${invoice.totalCardsCount} كرت - فاتورة #${invoice.invoiceNumber}"
            )
        )

        val headerId = db.journalEntryDao().postBalancedEntry(
            JournalEntryHeaderEntity(
                entryNumber = entryNumber,
                dateMillis = invoice.invoiceDateMillis,
                referenceType = "SALES_INVOICE",
                referenceId = invoice.invoiceNumber,
                description = "فاتورة مبيعات كروت #${invoice.invoiceNumber} للعميل ${invoice.customerName}",
                createdBy = performer
            ),
            lines
        )

        // ترحيل قيد تكلفة البضاعة المباعة (COGS)
        postCogsForInvoice(invoice, items, costMap, performer)

        headerId
    }

    private suspend fun postCogsForInvoice(
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>,
        costMap: Map<String, BigDecimal>,
        performer: String
    ) {
        val existingCogs = db.journalEntryDao().getHeaderByReference("COGS", invoice.invoiceNumber)
        if (existingCogs != null) return

        var totalCogs = BigDecimal.ZERO
        for (item in items) {
            val unitCost = costMap[item.packageName] ?: BigDecimal.ZERO
            if (unitCost > BigDecimal.ZERO) {
                totalCogs = totalCogs.add(unitCost.multiply(BigDecimal(item.quantity)))
            }
        }

        if (totalCogs <= BigDecimal.ZERO) {
            // احتساب افتراضي محافظ إذا لم تكن التكلفة محددة (50% من سعر الفاتورة)
            totalCogs = invoice.totalAmount.multiply(BigDecimal("0.5"))
        }

        if (totalCogs > BigDecimal.ZERO) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val cogsEntryNum = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")
            val cogsLines = listOf(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "5101",
                    accountName = "تكلفة الكروت المباعة (COGS)",
                    lineType = "DEBIT",
                    debit = totalCogs,
                    credit = BigDecimal.ZERO,
                    lineDescription = "تكلفة كروت فاتورة #${invoice.invoiceNumber}"
                ),
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1301",
                    accountName = "مخزون كروت الشبكة المطبوعة",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = totalCogs,
                    lineDescription = "صرف كروت من المخزون لفاتورة #${invoice.invoiceNumber}"
                )
            )

            db.journalEntryDao().postBalancedEntry(
                JournalEntryHeaderEntity(
                    entryNumber = cogsEntryNum,
                    dateMillis = invoice.invoiceDateMillis,
                    referenceType = "COGS",
                    referenceId = invoice.invoiceNumber,
                    description = "إثبات تكلفة البضاعة المباعة لفاتورة #${invoice.invoiceNumber}",
                    createdBy = performer
                ),
                cogsLines
            )
        }
    }

    /**
     * ترحيل سند مالي (قبض أو صرف)
     * - سند القبض: يرحل دائماً سواء ارتبط بفاتورة أم لا، فينقص 1201 للعميل ويزيد 1101 في الصندوق فوراً
     * - سند الصرف: يوزع على الحسابات الحقيقية (5101، 5201، 5202، 1501، 3201) حسب التصنيف
     */
    suspend fun postFinancialVoucher(
        voucher: FinancialVoucherEntity,
        performer: String = voucher.issuerName.ifBlank { "النظام المحاسبي" }
    ): Long? = withContext(Dispatchers.IO) {
        val existing = db.journalEntryDao().getHeaderByReference("FINANCIAL_VOUCHER", voucher.voucherNumber)
        if (existing != null) return@withContext existing.id

        if (voucher.amount <= BigDecimal.ZERO) return@withContext null

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val entryNumber = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")

        val lines = if (voucher.voucherType == "RECEIPT") {
            // سند قبض:
            // مدين: 1101 الصندوق الرئيسي
            // دائن: 1201 ذمم العملاء (إذا كان مرتبطاً بعميل أو فاتورة) أو 4201/4101 إذا كان إيراداً عاماً
            val creditAccountCode: String
            val creditAccountName: String
            val cat = voucher.category.trim()

            if (voucher.retailerId != null || voucher.invoiceId != null || voucher.invoiceNumber.isNotBlank()) {
                creditAccountCode = "1201"
                creditAccountName = "ذمم العملاء والوكلاء / ${voucher.partyName}"
            } else if (cat.contains("اشتراك") || cat.contains("مباشر") || cat.contains("خدمة")) {
                creditAccountCode = "4201"
                creditAccountName = "إيرادات الاشتراكات المباشرة والخدمات"
            } else {
                creditAccountCode = "4101"
                creditAccountName = "إيرادات مبيعات كروت الشبكة"
            }

            listOf(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "DEBIT",
                    debit = voucher.amount,
                    credit = BigDecimal.ZERO,
                    currency = voucher.currency,
                    lineDescription = "قبض نقدية سند #${voucher.voucherNumber} - ${voucher.description}"
                ),
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = creditAccountCode,
                    accountName = creditAccountName,
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = voucher.amount,
                    currency = voucher.currency,
                    partyId = voucher.retailerId,
                    partyType = if (voucher.retailerId != null) "RETAILER" else null,
                    lineDescription = "سداد/تحصيل سند #${voucher.voucherNumber} - ${voucher.partyName}"
                )
            )
        } else {
            // سند صرف:
            // مدين: الحساب المخصص حسب فئة وتصنيف النفقة
            // دائن: 1101 الصندوق الرئيسي
            val (debitCode, debitName) = mapPaymentCategoryToAccount(voucher.category, voucher.partyName)

            listOf(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = debitCode,
                    accountName = debitName,
                    lineType = "DEBIT",
                    debit = voucher.amount,
                    credit = BigDecimal.ZERO,
                    currency = voucher.currency,
                    partyId = voucher.retailerId,
                    lineDescription = "صرف نفقة سند #${voucher.voucherNumber} - ${voucher.description}"
                ),
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = voucher.amount,
                    currency = voucher.currency,
                    lineDescription = "صرف نقدية سند #${voucher.voucherNumber}"
                )
            )
        }

        db.journalEntryDao().postBalancedEntry(
            JournalEntryHeaderEntity(
                entryNumber = entryNumber,
                dateMillis = voucher.dateMillis,
                referenceType = "FINANCIAL_VOUCHER",
                referenceId = voucher.voucherNumber,
                description = "سند ${if (voucher.voucherType == "RECEIPT") "قبض" else "صرف"} #${voucher.voucherNumber} - ${voucher.partyName} (${voucher.category})",
                createdBy = performer
            ),
            lines
        )
    }

    /**
     * ترحيل فاتورة مشتريات
     */
    suspend fun postPurchaseInvoice(
        invoice: PurchaseInvoiceEntity,
        performer: String = "النظام المحاسبي"
    ): Long? = withContext(Dispatchers.IO) {
        val existing = db.journalEntryDao().getHeaderByReference("PURCHASE_INVOICE", invoice.invoiceNumber)
        if (existing != null) return@withContext existing.id

        if (invoice.totalAmount <= BigDecimal.ZERO) return@withContext null

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val entryNumber = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")

        val (debitCode, debitName) = when (invoice.targetType.uppercase()) {
            "ASSET", "CAPEX" -> Pair("1501", "أصول ومعدات الشبكة (مشتريات رأسمالية)")
            "MAINTENANCE" -> Pair("5202", "مصروفات الصيانة وقطع الغيار")
            else -> Pair("5201", "مصروفات تشغيلية وعمومية")
        }

        val lines = mutableListOf<JournalEntryLineEntity>()
        lines.add(
            JournalEntryLineEntity(
                headerId = 0L,
                accountCode = debitCode,
                accountName = debitName,
                lineType = "DEBIT",
                debit = invoice.totalAmount,
                credit = BigDecimal.ZERO,
                lineDescription = "فاتورة مشتريات #${invoice.invoiceNumber} من ${invoice.supplierName}"
            )
        )

        val isPaidCash = invoice.paymentMethod.equals("CASH", ignoreCase = true) || invoice.paidAmount >= invoice.totalAmount
        if (isPaidCash) {
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = invoice.totalAmount,
                    lineDescription = "سداد نقدي مشتريات فاتورة #${invoice.invoiceNumber}"
                )
            )
        } else {
            // آجل للمورد: دائن 2101
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "2101",
                    accountName = "الموردون والدائنون / ${invoice.supplierName}",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = invoice.totalAmount,
                    partyType = "SUPPLIER",
                    lineDescription = "مشتريات آجلة ذمة مورد فاتورة #${invoice.invoiceNumber}"
                )
            )
        }

        db.journalEntryDao().postBalancedEntry(
            JournalEntryHeaderEntity(
                entryNumber = entryNumber,
                dateMillis = invoice.invoiceDateMillis,
                referenceType = "PURCHASE_INVOICE",
                referenceId = invoice.invoiceNumber,
                description = "فاتورة مشتريات #${invoice.invoiceNumber} - ${invoice.supplierName}",
                createdBy = performer
            ),
            lines
        )
    }

    /**
     * فحص وترحيل كافة الفواتير والسندات السابقة التي لم تُرحل إلى اليومية
     * يضمن تطابق البيانات القديمة والمستوردة بنسبة 100%
     */
    suspend fun repairAllUnpostedEntries(): Int = withContext(Dispatchers.IO) {
        var repairedCount = 0

        // 1. فحص فواتير المبيعات
        val invoices = db.cardSalesInvoiceDao().getSalesInvoicesList()
        for (inv in invoices) {
            val existing = db.journalEntryDao().getHeaderByReference("SALES_INVOICE", inv.invoiceNumber)
            if (existing == null) {
                postSalesInvoice(inv)
                repairedCount++
            }
        }

        // 2. فحص السندات المالية
        val vouchers = db.financialVoucherDao().getVouchersList()
        for (v in vouchers) {
            if (!v.isVoided) {
                val existing = db.journalEntryDao().getHeaderByReference("FINANCIAL_VOUCHER", v.voucherNumber)
                if (existing == null) {
                    postFinancialVoucher(v)
                    repairedCount++
                }
            }
        }

        // 3. فحص فواتير المشتريات
        val purchases = db.purchaseInvoiceDao().getPurchaseInvoicesList()
        for (p in purchases) {
            val existing = db.journalEntryDao().getHeaderByReference("PURCHASE_INVOICE", p.invoiceNumber)
            if (existing == null) {
                postPurchaseInvoice(p)
                repairedCount++
            }
        }

        repairedCount
    }

    private fun mapPaymentCategoryToAccount(category: String, partyName: String): Pair<String, String> {
        val cat = category.trim().lowercase()
        val party = partyName.trim().lowercase()

        return when {
            cat.contains("اشتراك") || cat.contains("نت") || cat.contains("ستارلينك") || cat.contains("حزم") || cat.contains("مزود") ->
                Pair("5101", "تكلفة خدمة الإنترنت والمزودين (COGS)")

            cat.contains("صيانة") || cat.contains("قطع") || cat.contains("تصليح") || cat.contains("دعم") ->
                Pair("5202", "مصروفات الصيانة وقطع الغيار")

            cat.contains("أصول") || cat.contains("معدات") || cat.contains("راوتر") || cat.contains("سيرفر") || cat.contains("برج") || cat.contains("طاقة") ->
                Pair("1501", "أصول ومعدات الشبكة (نفقات رأسمالية)")

            cat.contains("شريك") || cat.contains("مسحوبات") || cat.contains("أرباح") || party.contains("شريك") ->
                Pair("3201", "جاري الشركاء والأرباح المسحوبة")

            else ->
                Pair("5201", "مصروفات تشغيلية وعمومية / $category")
        }
    }
}
