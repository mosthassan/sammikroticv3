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
import java.math.RoundingMode
import java.util.Calendar

/**
 * المحرك المركزي الموحد لترحيل العمليات المحاسبية (Single Source of Truth Posting Engine)
 * - يضمن ترحيل كافة الفواتير والسندات والمشتريات (اليدوية، المستوردة، والمستعادة)
 * - يعتمد القيد المزدوج المتوازن إجبارياً وبشكل Idempotent يمنع التكرار
 * - يوحّد العملات إلى العملة الأساسية (الريال اليمني) في اليومية العامة
 * - يستند حصرياً إلى التكاليف الفعلية دون أي نسب وهمية
 */
class AccountingPostingService(private val db: AppDatabase) {

    /**
     * تحويل المبالغ إلى العملة الأساسية (الريال اليمني) لتسجيلها في دفتر اليومية الموحد
     */
    suspend fun convertToBaseCurrency(amount: BigDecimal, currency: String): BigDecimal = withContext(Dispatchers.IO) {
        if (amount <= BigDecimal.ZERO) return@withContext BigDecimal.ZERO
        val curr = currency.trim().uppercase()
        if (curr.isBlank() || curr == "YER") return@withContext amount

        val rateEntity = try {
            db.currencyRateDao().getRateForCurrency(curr)
        } catch (_: Exception) {
            null
        }

        val rate = if (rateEntity != null && rateEntity.rateToBase > BigDecimal.ZERO) {
            rateEntity.rateToBase
        } else {
            when (curr) {
                "SAR" -> BigDecimal("140.0")
                "USD" -> BigDecimal("530.0")
                else -> BigDecimal.ONE
            }
        }
        return@withContext amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
    }

    private fun getDocumentYear(dateMillis: Long): Int {
        val cal = Calendar.getInstance()
        if (dateMillis > 0) {
            cal.timeInMillis = dateMillis
        }
        return cal.get(Calendar.YEAR)
    }

    /**
     * ترحيل فاتورة مبيعات كروت الشبكة
     * - للعملاء/البقالات (retailerId != null):
     *     تسجيل كامل قيمة الفاتورة على حساب العميل: مدين 1201 (ذمم العملاء)، دائن 4101 (إيرادات المبيعات).
     *     (أي دفعة مسددة تسجّل عبر سند القبض REC المربوط: مدين 1101 الصندوق، دائن 1201 ذمم العميل).
     *     هذا يضمن توازن كشف الحساب وعدم تكرار مبالغ الصندوق مطلقاً.
     * - للمبيعات النقدية المباشرة (retailerId == null):
     *     مدين 1101 (الصندوق الرئيسي) لكامل المبلغ، دائن 4101 (إيرادات المبيعات).
     */
    suspend fun postSalesInvoice(
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem> = emptyList(),
        costMap: Map<String, BigDecimal> = emptyMap(),
        performer: String = invoice.issuerName.ifBlank { "النظام المحاسبي" }
    ): Long? = withContext(Dispatchers.IO) {
        if (invoice.totalAmount <= BigDecimal.ZERO) return@withContext null

        // منع التكرار: البحث برقم الفاتورة أو بمعرف السجل القديم
        val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
            "SALES_INVOICE",
            invoice.invoiceNumber,
            invoice.id.toString()
        )
        if (existing != null) return@withContext existing.id

        val docYear = getDocumentYear(invoice.invoiceDateMillis)
        val entryNumber = db.numberSequenceDao().getNextNumber("JE", docYear, "JE")

        val lines = mutableListOf<JournalEntryLineEntity>()
        val effectiveCustomer = invoice.customerName.ifBlank { "عميل نقدي" }

        if (invoice.retailerId != null) {
            // بيع لبقالة أو نقطة توزيع: مدين كامل الفاتورة على حساب العميل 1201
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1201",
                    accountName = "ذمم العملاء والوكلاء / $effectiveCustomer",
                    lineType = "DEBIT",
                    debit = invoice.totalAmount,
                    credit = BigDecimal.ZERO,
                    partyId = invoice.retailerId,
                    partyType = "RETAILER",
                    lineDescription = "فاتورة مبيعات كروت #${invoice.invoiceNumber} ($effectiveCustomer)"
                )
            )
        } else {
            // بيع مباشر (بدون بقالة مسجلة)
            val isCash = (invoice.paymentType.equals("CASH", ignoreCase = true) ||
                          invoice.paymentType.equals("نقداً") ||
                          invoice.paymentType.equals("نقد")) &&
                         invoice.remainingAmount <= BigDecimal.ZERO

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
                        lineDescription = "مبيعات نقدية فاتورة #${invoice.invoiceNumber} - $effectiveCustomer"
                    )
                )
            } else {
                // بيع نقدي بآجل جزئي أو متبقي
                if (invoice.paidAmount > BigDecimal.ZERO) {
                    lines.add(
                        JournalEntryLineEntity(
                            headerId = 0L,
                            accountCode = "1101",
                            accountName = "الصندوق الرئيسي (النقدية)",
                            lineType = "DEBIT",
                            debit = invoice.paidAmount,
                            credit = BigDecimal.ZERO,
                            lineDescription = "دفعة نقدية مسددة لفاتورة #${invoice.invoiceNumber}"
                        )
                    )
                }
                if (invoice.remainingAmount > BigDecimal.ZERO) {
                    lines.add(
                        JournalEntryLineEntity(
                            headerId = 0L,
                            accountCode = "1201",
                            accountName = "ذمم العملاء والوكلاء / $effectiveCustomer",
                            lineType = "DEBIT",
                            debit = invoice.remainingAmount,
                            credit = BigDecimal.ZERO,
                            lineDescription = "متبقي آجل فاتورة #${invoice.invoiceNumber} ($effectiveCustomer)"
                        )
                    )
                }
            }
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
                description = "فاتورة مبيعات كروت #${invoice.invoiceNumber} للعميل $effectiveCustomer",
                createdBy = performer
            ),
            lines
        )

        // ترحيل قيد تكلفة البضاعة المباعة (COGS) حصرياً عند توفر تكلفة فعلية محددة
        postCogsForInvoice(invoice, items, costMap, performer)

        headerId
    }

    /**
     * ترحيل تكلفة البضاعة المباعة (COGS) ومخزون الكروت
     * تنبيه: لا يتم استخدام أي نسبة تقديرية وهمية. الترحيل يتم فقط إذا كانت التكلفة الفعلية > 0
     */
    private suspend fun postCogsForInvoice(
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>,
        costMap: Map<String, BigDecimal>,
        performer: String
    ) {
        if (costMap.isEmpty() && items.isEmpty()) return

        val existingCogs = db.journalEntryDao().getHeaderByReferenceWithAlt("COGS", invoice.invoiceNumber, invoice.id.toString())
        if (existingCogs != null) return

        var totalCogs = BigDecimal.ZERO
        for (item in items) {
            val unitCost = costMap[item.packageName] ?: BigDecimal.ZERO
            if (unitCost > BigDecimal.ZERO) {
                totalCogs = totalCogs.add(unitCost.multiply(BigDecimal(item.quantity)))
            }
        }

        // إذا لم توجد تكلفة كروت فعلية محددة، لا نرحل قيد تكلفة وهمي
        if (totalCogs <= BigDecimal.ZERO) return

        val docYear = getDocumentYear(invoice.invoiceDateMillis)
        val cogsEntryNum = db.numberSequenceDao().getNextNumber("JE", docYear, "JE")
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

    /**
     * ترحيل سند مالي (قبض أو صرف)
     * - سند القبض:
     *     مدين: 1101 الصندوق الرئيسي (بالمبلغ محولاً للعملة الأساسية).
     *     دائن: 1201 ذمم العملاء (إذا كان مرتبطاً بعميل أو فاتورة)، أو 4101/4201 إذا كان إيراداً عاماً.
     * - سند الصرف:
     *     مدين: الحساب الحقيقي حسب التصنيف الدقيق (5101، 5201، 5202، 1501، 3201).
     *     دائن: 1101 الصندوق الرئيسي.
     */
    suspend fun postFinancialVoucher(
        voucher: FinancialVoucherEntity,
        performer: String = voucher.issuerName.ifBlank { "النظام المحاسبي" }
    ): Long? = withContext(Dispatchers.IO) {
        if (voucher.amount <= BigDecimal.ZERO || voucher.isVoided) return@withContext null

        val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
            "FINANCIAL_VOUCHER",
            voucher.voucherNumber,
            voucher.id.toString()
        )
        if (existing != null) return@withContext existing.id

        // تحويل المبلغ إلى العملة الأساسية (الريال اليمني) لضمان توازن دفتر الأستاذ العام
        val amountYer = convertToBaseCurrency(voucher.amount, voucher.currency)
        if (amountYer <= BigDecimal.ZERO) return@withContext null

        val docYear = getDocumentYear(voucher.dateMillis)
        val entryNumber = db.numberSequenceDao().getNextNumber("JE", docYear, "JE")

        val effectiveParty = voucher.partyName.ifBlank { "جهة عامة" }

        val lines = if (voucher.voucherType == "RECEIPT") {
            // تحديد الحساب الدائن
            val (creditAccountCode, creditAccountName, partyId, partyType) = if (voucher.retailerId != null) {
                Quadruple("1201", "ذمم العملاء والوكلاء / $effectiveParty", voucher.retailerId, "RETAILER")
            } else if (voucher.invoiceId != null || voucher.invoiceNumber.isNotBlank()) {
                val linkedInv = if (voucher.invoiceId != null) {
                    db.cardSalesInvoiceDao().getInvoiceById(voucher.invoiceId)
                } else {
                    db.cardSalesInvoiceDao().getInvoiceByNumber(voucher.invoiceNumber)
                }
                if (linkedInv?.retailerId != null) {
                    Quadruple("1201", "ذمم العملاء والوكلاء / ${linkedInv.customerName}", linkedInv.retailerId, "RETAILER")
                } else {
                    // مبيعات نقدية مباشرة (عميل نقدي بدون بقالة مسجلة):
                    // قيد الفاتورة الأصلي سجل الحركة في الصندوق والإيراد مباشرة،
                    // لتفادي التكرار المالي المزدوج لا ننشئ قيداً إضافياً لسند القبض المالي المطبوع للعميل
                    val invHeader = db.journalEntryDao().getHeaderByReferenceWithAlt(
                        "SALES_INVOICE",
                        linkedInv?.invoiceNumber ?: voucher.invoiceNumber,
                        linkedInv?.id?.toString() ?: ""
                    )
                    return@withContext invHeader?.id
                }
            } else {
                val cat = voucher.category.trim()
                if (cat.contains("اشتراك") || cat.contains("مباشر") || cat.contains("خدمة")) {
                    Quadruple("4201", "إيرادات الاشتراكات المباشرة والخدمات", null, null)
                } else {
                    Quadruple("4101", "إيرادات مبيعات كروت الشبكة", null, null)
                }
            }

            listOf(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "DEBIT",
                    debit = amountYer,
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
                    credit = amountYer,
                    currency = voucher.currency,
                    partyId = partyId,
                    partyType = partyType,
                    lineDescription = "سداد/تحصيل سند #${voucher.voucherNumber} - $effectiveParty"
                )
            )
        } else {
            // سند صرف:
            val linkedPurchase = if (voucher.invoiceId != null) {
                db.purchaseInvoiceDao().getInvoiceById(voucher.invoiceId)
            } else if (voucher.invoiceNumber.isNotBlank()) {
                db.purchaseInvoiceDao().getInvoiceByNumber(voucher.invoiceNumber)
            } else null

            val (debitCode, debitName) = if (linkedPurchase != null) {
                Pair("2101", "الموردون وذمم المشتريات / $effectiveParty")
            } else {
                mapPaymentCategoryToAccount(voucher.category, effectiveParty)
            }

            listOf(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = debitCode,
                    accountName = debitName,
                    lineType = "DEBIT",
                    debit = amountYer,
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
                    credit = amountYer,
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
                description = "سند ${if (voucher.voucherType == "RECEIPT") "قبض" else "صرف"} #${voucher.voucherNumber} - $effectiveParty (${voucher.category})",
                createdBy = performer
            ),
            lines
        )
    }

    /**
     * ترحيل فاتورة مشتريات
     * - تحويل العملة الأصلية (USD, SAR) إلى العملة الأساسية (YER) لضبط الأستاذ
     * - التوجيه الدقيق للحساب المدين بناء على targetType (أصول، صيانة، مخزون، تشغيل)
     * - إثبات السداد النقدي في الصندوق أو مديونية المورد في الحسابات الدائنة
     */
    suspend fun postPurchaseInvoice(
        invoice: PurchaseInvoiceEntity,
        performer: String = "النظام المحاسبي"
    ): Long? = withContext(Dispatchers.IO) {
        if (invoice.totalAmount <= BigDecimal.ZERO || invoice.isVoided) return@withContext null

        val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
            "PURCHASE_INVOICE",
            invoice.invoiceNumber,
            invoice.id.toString()
        )
        if (existing != null) return@withContext existing.id

        // تحويل المبالغ إلى الريال اليمني
        val rawOriginal = if (invoice.originalAmount > BigDecimal.ZERO) invoice.originalAmount else invoice.totalAmount
        val totalYer = convertToBaseCurrency(rawOriginal, invoice.currency)
        val rawPaid = if (invoice.paidAmount > BigDecimal.ZERO) invoice.paidAmount else BigDecimal.ZERO
        val paidYer = convertToBaseCurrency(rawPaid, invoice.currency)
        if (totalYer <= BigDecimal.ZERO) return@withContext null

        val docYear = getDocumentYear(invoice.invoiceDateMillis)
        val entryNumber = db.numberSequenceDao().getNextNumber("JE", docYear, "JE")

        val target = invoice.targetType.trim().uppercase()
        val (debitCode, debitName) = when {
            target.contains("ASSET") || target.contains("CAPEX") || target.contains("أصول") || target.contains("معدات") ->
                Pair("1501", "أصول ومعدات الشبكة (مشتريات رأسمالية)")
            target.contains("INVENTORY") || target.contains("STOCK") || target.contains("مخزون") || target.contains("كروت") ->
                Pair("1301", "مخزون كروت ومواد الشبكة")
            target.contains("MAINT") || target.contains("صيانة") || target.contains("قطع") ->
                Pair("5202", "مصروفات الصيانة وقطع الغيار")
            target.contains("BANDWIDTH") || target.contains("INTERNET") || target.contains("نت") || target.contains("اشتراك") ->
                Pair("5101", "تكلفة خدمة الإنترنت والمزودين (COGS)")
            else ->
                Pair("5201", "مصروفات تشغيلية وعمومية")
        }

        val lines = mutableListOf<JournalEntryLineEntity>()
        lines.add(
            JournalEntryLineEntity(
                headerId = 0L,
                accountCode = debitCode,
                accountName = debitName,
                lineType = "DEBIT",
                debit = totalYer,
                credit = BigDecimal.ZERO,
                currency = invoice.currency,
                originalAmount = rawOriginal,
                lineDescription = "فاتورة مشتريات #${invoice.invoiceNumber} من ${invoice.supplierName}"
            )
        )

        val attachedVouchers = if (invoice.id > 0L) {
            db.financialVoucherDao().getVouchersByInvoiceId(invoice.id)
        } else {
            db.financialVoucherDao().getVouchersByInvoiceNumber(invoice.invoiceNumber)
        }
        val hasLinkedPaymentVoucher = attachedVouchers.any { it.voucherType == "PAYMENT" && !it.isVoided }

        val isPaidCash = !hasLinkedPaymentVoucher && (
            invoice.paymentMethod.equals("CASH", ignoreCase = true) ||
            invoice.paymentMethod.contains("نقد") ||
            invoice.paymentMethod.contains("صندوق") ||
            paidYer >= totalYer
        )

        if (isPaidCash) {
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = totalYer,
                    currency = invoice.currency,
                    originalAmount = rawOriginal,
                    lineDescription = "سداد نقدي مشتريات فاتورة #${invoice.invoiceNumber}"
                )
            )
        } else if (hasLinkedPaymentVoucher || paidYer <= BigDecimal.ZERO) {
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "2101",
                    accountName = "الموردون والدائنون / ${invoice.supplierName}",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = totalYer,
                    currency = invoice.currency,
                    partyType = "SUPPLIER",
                    lineDescription = "استحقاق للمورد فاتورة مشتريات #${invoice.invoiceNumber}"
                )
            )
        } else {
            // سداد جزئي للمورد
            val remainingYer = totalYer.subtract(paidYer)
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = paidYer,
                    currency = invoice.currency,
                    lineDescription = "دفعة نقدية مسددة لمشتريات فاتورة #${invoice.invoiceNumber}"
                )
            )
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "2101",
                    accountName = "الموردون والدائنون / ${invoice.supplierName}",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = remainingYer,
                    currency = invoice.currency,
                    partyType = "SUPPLIER",
                    lineDescription = "المتبقي الآجل لمشتريات فاتورة #${invoice.invoiceNumber}"
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
     * تحديث قيد فاتورة مبيعات معدلة لضمان مطابقة دفتر الأستاذ للتعديلات
     */
    suspend fun updateSalesInvoicePosting(
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem> = emptyList(),
        costMap: Map<String, BigDecimal> = emptyMap(),
        performer: String = invoice.issuerName.ifBlank { "النظام المحاسبي" }
    ): Long? = withContext(Dispatchers.IO) {
        val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
            "SALES_INVOICE",
            invoice.invoiceNumber,
            invoice.id.toString()
        )
        if (existing == null) {
            return@withContext postSalesInvoice(invoice, items, costMap, performer)
        }

        // إذا تم إلغاء الفاتورة، نقوم بإجراء قيد عكسي تسوية
        if (invoice.isVoided) {
            db.journalEntryDao().voidEntryWithReversal(existing.id, invoice.voidReason.ifBlank { "إلغاء فاتورة المبيعات" }, performer)
            return@withContext existing.id
        }

        // تحديث أسطر القيد الحالي لتطابق الفاتورة المعدلة
        db.journalEntryDao().deleteLinesByHeaderId(existing.id)

        val lines = mutableListOf<JournalEntryLineEntity>()
        val effectiveCustomer = invoice.customerName.ifBlank { "عميل نقدي" }

        if (invoice.retailerId != null) {
            lines.add(
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = "1201",
                    accountName = "ذمم العملاء والوكلاء / $effectiveCustomer",
                    lineType = "DEBIT",
                    debit = invoice.totalAmount,
                    credit = BigDecimal.ZERO,
                    partyId = invoice.retailerId,
                    partyType = "RETAILER",
                    lineDescription = "فاتورة مبيعات كروت معدلة #${invoice.invoiceNumber} ($effectiveCustomer)"
                )
            )
        } else {
            val isCash = (invoice.paymentType.equals("CASH", ignoreCase = true) ||
                          invoice.paymentType.equals("نقداً") ||
                          invoice.paymentType.equals("نقد")) &&
                         invoice.remainingAmount <= BigDecimal.ZERO
            if (isCash) {
                lines.add(
                    JournalEntryLineEntity(
                        headerId = existing.id,
                        accountCode = "1101",
                        accountName = "الصندوق الرئيسي (النقدية)",
                        lineType = "DEBIT",
                        debit = invoice.totalAmount,
                        credit = BigDecimal.ZERO,
                        lineDescription = "مبيعات نقدية فاتورة معدلة #${invoice.invoiceNumber} - $effectiveCustomer"
                    )
                )
            } else {
                if (invoice.paidAmount > BigDecimal.ZERO) {
                    lines.add(
                        JournalEntryLineEntity(
                            headerId = existing.id,
                            accountCode = "1101",
                            accountName = "الصندوق الرئيسي (النقدية)",
                            lineType = "DEBIT",
                            debit = invoice.paidAmount,
                            credit = BigDecimal.ZERO,
                            lineDescription = "دفعة نقدية مسددة لفاتورة معدلة #${invoice.invoiceNumber}"
                        )
                    )
                }
                if (invoice.remainingAmount > BigDecimal.ZERO) {
                    lines.add(
                        JournalEntryLineEntity(
                            headerId = existing.id,
                            accountCode = "1201",
                            accountName = "ذمم العملاء والوكلاء / $effectiveCustomer",
                            lineType = "DEBIT",
                            debit = invoice.remainingAmount,
                            credit = BigDecimal.ZERO,
                            lineDescription = "متبقي آجل فاتورة معدلة #${invoice.invoiceNumber}"
                        )
                    )
                }
            }
        }

        lines.add(
            JournalEntryLineEntity(
                headerId = existing.id,
                accountCode = "4101",
                accountName = "إيرادات مبيعات كروت الشبكة",
                lineType = "CREDIT",
                debit = BigDecimal.ZERO,
                credit = invoice.totalAmount,
                partyId = invoice.retailerId,
                partyType = if (invoice.retailerId != null) "RETAILER" else null,
                lineDescription = "إيراد مبيعات فاتورة معدلة #${invoice.invoiceNumber}"
            )
        )

        db.journalEntryDao().insertLines(lines)
        db.journalEntryDao().updateHeader(
            existing.copy(
                totalDebit = invoice.totalAmount,
                totalCredit = invoice.totalAmount,
                dateMillis = invoice.invoiceDateMillis,
                description = "فاتورة مبيعات كروت معدلة #${invoice.invoiceNumber} للعميل $effectiveCustomer"
            )
        )

        existing.id
    }

    /**
     * إلغاء السند المالي بقيد عكسي تسوية يحفظ مسار التدقيق الرقابي
     */
    suspend fun reverseFinancialVoucher(
        voucher: FinancialVoucherEntity,
        reason: String = "إلغاء السند المالي",
        performer: String = voucher.issuerName.ifBlank { "النظام المحاسبي" }
    ): Boolean = withContext(Dispatchers.IO) {
        val header = db.journalEntryDao().getHeaderByReferenceWithAlt(
            "FINANCIAL_VOUCHER",
            voucher.voucherNumber,
            voucher.id.toString()
        )
        if (header != null) {
            val revId = db.journalEntryDao().voidEntryWithReversal(header.id, reason, performer)
            return@withContext revId != null
        }
        false
    }

    /**
     * تحديث قيد سند مالي معدل لضمان مطابقة دفتر الأستاذ للتعديلات
     */
    suspend fun updateFinancialVoucherPosting(
        voucher: FinancialVoucherEntity,
        performer: String = voucher.issuerName.ifBlank { "النظام المحاسبي" }
    ): Long? = withContext(Dispatchers.IO) {
        if (voucher.amount <= BigDecimal.ZERO) return@withContext null

        val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
            "FINANCIAL_VOUCHER",
            voucher.voucherNumber,
            voucher.id.toString()
        )
        if (existing == null) {
            return@withContext postFinancialVoucher(voucher, performer)
        }

        if (voucher.isVoided) {
            db.journalEntryDao().voidEntryWithReversal(existing.id, voucher.notes.ifBlank { "إلغاء السند المالي" }, performer)
            return@withContext existing.id
        }

        val amountYer = convertToBaseCurrency(voucher.amount, voucher.currency)
        if (amountYer <= BigDecimal.ZERO) return@withContext null

        db.journalEntryDao().deleteLinesByHeaderId(existing.id)
        val effectiveParty = voucher.partyName.ifBlank { "جهة عامة" }

        val lines = if (voucher.voucherType == "RECEIPT") {
            val (creditAccountCode, creditAccountName, partyId, partyType) = if (voucher.retailerId != null) {
                Quadruple("1201", "ذمم العملاء والوكلاء / $effectiveParty", voucher.retailerId, "RETAILER")
            } else if (voucher.invoiceId != null || voucher.invoiceNumber.isNotBlank()) {
                val linkedInv = if (voucher.invoiceId != null) {
                    db.cardSalesInvoiceDao().getInvoiceById(voucher.invoiceId)
                } else {
                    db.cardSalesInvoiceDao().getInvoiceByNumber(voucher.invoiceNumber)
                }
                if (linkedInv?.retailerId != null) {
                    Quadruple("1201", "ذمم العملاء والوكلاء / ${linkedInv.customerName}", linkedInv.retailerId, "RETAILER")
                } else {
                    Quadruple("4101", "إيرادات مبيعات كروت الشبكة", null, null)
                }
            } else {
                Quadruple("4101", "إيرادات مبيعات كروت الشبكة", null, null)
            }

            listOf(
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "DEBIT",
                    debit = amountYer,
                    credit = BigDecimal.ZERO,
                    currency = voucher.currency,
                    lineDescription = "قبض نقدية سند معدل #${voucher.voucherNumber} - ${voucher.description}"
                ),
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = creditAccountCode,
                    accountName = creditAccountName,
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = amountYer,
                    currency = voucher.currency,
                    partyId = partyId,
                    partyType = partyType,
                    lineDescription = "سداد/تحصيل سند معدل #${voucher.voucherNumber} - $effectiveParty"
                )
            )
        } else {
            val linkedPurchase = if (voucher.invoiceId != null) {
                db.purchaseInvoiceDao().getInvoiceById(voucher.invoiceId)
            } else if (voucher.invoiceNumber.isNotBlank()) {
                db.purchaseInvoiceDao().getInvoiceByNumber(voucher.invoiceNumber)
            } else null

            val (debitCode, debitName) = if (linkedPurchase != null) {
                Pair("2101", "الموردون وذمم المشتريات / $effectiveParty")
            } else {
                mapPaymentCategoryToAccount(voucher.category, effectiveParty)
            }

            listOf(
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = debitCode,
                    accountName = debitName,
                    lineType = "DEBIT",
                    debit = amountYer,
                    credit = BigDecimal.ZERO,
                    currency = voucher.currency,
                    partyId = voucher.retailerId,
                    lineDescription = "صرف نفقة سند معدل #${voucher.voucherNumber} - ${voucher.description}"
                ),
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = amountYer,
                    currency = voucher.currency,
                    lineDescription = "صرف نقدية سند معدل #${voucher.voucherNumber}"
                )
            )
        }

        db.journalEntryDao().insertLines(lines)
        db.journalEntryDao().updateHeader(
            existing.copy(
                totalDebit = amountYer,
                totalCredit = amountYer,
                dateMillis = voucher.dateMillis,
                description = "سند ${if (voucher.voucherType == "RECEIPT") "قبض" else "صرف"} معدل #${voucher.voucherNumber} - $effectiveParty"
            )
        )

        existing.id
    }

    /**
     * تحديث قيد فاتورة مشتريات معدلة
     */
    suspend fun updatePurchaseInvoicePosting(
        invoice: PurchaseInvoiceEntity,
        performer: String = "النظام المحاسبي"
    ): Long? = withContext(Dispatchers.IO) {
        if (invoice.totalAmount <= BigDecimal.ZERO) return@withContext null

        val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
            "PURCHASE_INVOICE",
            invoice.invoiceNumber,
            invoice.id.toString()
        )
        if (existing == null) {
            return@withContext postPurchaseInvoice(invoice, performer)
        }

        if (invoice.isVoided) {
            db.journalEntryDao().voidEntryWithReversal(existing.id, invoice.voidReason.ifBlank { "إلغاء فاتورة المشتريات" }, performer)
            return@withContext existing.id
        }

        val rawOriginal = if (invoice.originalAmount > BigDecimal.ZERO) invoice.originalAmount else invoice.totalAmount
        val totalYer = convertToBaseCurrency(rawOriginal, invoice.currency)
        val rawPaid = if (invoice.paidAmount > BigDecimal.ZERO) invoice.paidAmount else BigDecimal.ZERO
        val paidYer = convertToBaseCurrency(rawPaid, invoice.currency)
        if (totalYer <= BigDecimal.ZERO) return@withContext null

        db.journalEntryDao().deleteLinesByHeaderId(existing.id)

        val target = invoice.targetType.trim().uppercase()
        val (debitCode, debitName) = when {
            target.contains("ASSET") || target.contains("CAPEX") || target.contains("أصول") || target.contains("معدات") ->
                Pair("1501", "أصول ومعدات الشبكة (مشتريات رأسمالية)")
            target.contains("INVENTORY") || target.contains("STOCK") || target.contains("مخزون") || target.contains("كروت") ->
                Pair("1301", "مخزون كروت ومواد الشبكة")
            target.contains("MAINT") || target.contains("صيانة") || target.contains("قطع") ->
                Pair("5202", "مصروفات الصيانة وقطع الغيار")
            target.contains("BANDWIDTH") || target.contains("INTERNET") || target.contains("نت") || target.contains("اشتراك") ->
                Pair("5101", "تكلفة خدمة الإنترنت والمزودين (COGS)")
            else ->
                Pair("5201", "مصروفات تشغيلية وعمومية")
        }

        val lines = mutableListOf<JournalEntryLineEntity>()
        lines.add(
            JournalEntryLineEntity(
                headerId = existing.id,
                accountCode = debitCode,
                accountName = debitName,
                lineType = "DEBIT",
                debit = totalYer,
                credit = BigDecimal.ZERO,
                currency = invoice.currency,
                originalAmount = rawOriginal,
                lineDescription = "فاتورة مشتريات معدلة #${invoice.invoiceNumber} من ${invoice.supplierName}"
            )
        )

        val attachedVouchers = if (invoice.id > 0L) {
            db.financialVoucherDao().getVouchersByInvoiceId(invoice.id)
        } else {
            db.financialVoucherDao().getVouchersByInvoiceNumber(invoice.invoiceNumber)
        }
        val hasLinkedPaymentVoucher = attachedVouchers.any { it.voucherType == "PAYMENT" && !it.isVoided }

        val isPaidCash = !hasLinkedPaymentVoucher && (
            invoice.paymentMethod.equals("CASH", ignoreCase = true) ||
            invoice.paymentMethod.contains("نقد") ||
            invoice.paymentMethod.contains("صندوق") ||
            paidYer >= totalYer
        )

        if (isPaidCash) {
            lines.add(
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = totalYer,
                    currency = invoice.currency,
                    originalAmount = rawOriginal,
                    lineDescription = "سداد نقدي مشتريات فاتورة معدلة #${invoice.invoiceNumber}"
                )
            )
        } else if (hasLinkedPaymentVoucher || paidYer <= BigDecimal.ZERO) {
            lines.add(
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = "2101",
                    accountName = "الموردون والدائنون / ${invoice.supplierName}",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = totalYer,
                    currency = invoice.currency,
                    partyType = "SUPPLIER",
                    lineDescription = "استحقاق للمورد فاتورة مشتريات معدلة #${invoice.invoiceNumber}"
                )
            )
        } else {
            val remainingYer = totalYer.subtract(paidYer)
            lines.add(
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = "1101",
                    accountName = "الصندوق الرئيسي (النقدية)",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = paidYer,
                    currency = invoice.currency,
                    lineDescription = "دفعة مسددة مشتريات فاتورة معدلة #${invoice.invoiceNumber}"
                )
            )
            lines.add(
                JournalEntryLineEntity(
                    headerId = existing.id,
                    accountCode = "2101",
                    accountName = "الموردون والدائنون / ${invoice.supplierName}",
                    lineType = "CREDIT",
                    debit = BigDecimal.ZERO,
                    credit = remainingYer,
                    currency = invoice.currency,
                    partyType = "SUPPLIER",
                    lineDescription = "المتبقي الآجل لمشتريات فاتورة معدلة #${invoice.invoiceNumber}"
                )
            )
        }

        db.journalEntryDao().insertLines(lines)
        db.journalEntryDao().updateHeader(
            existing.copy(
                totalDebit = totalYer,
                totalCredit = totalYer,
                dateMillis = invoice.invoiceDateMillis,
                description = "فاتورة مشتريات معدلة #${invoice.invoiceNumber} من ${invoice.supplierName}"
            )
        )

        existing.id
    }

    /**
     * فحص وترحيل كافة الفواتير والسندات السابقة التي لم تُرحل إلى اليومية
     * يضمن تطابق البيانات القديمة والمستوردة بنسبة 100%
     */
    suspend fun repairAllUnpostedEntries(): Int = withContext(Dispatchers.IO) {
        var repairedCount = 0

        // 1. فحص فواتير المبيعات غير الملغاة
        val invoices = db.cardSalesInvoiceDao().getSalesInvoicesList()
        for (inv in invoices) {
            if (!inv.isVoided && inv.totalAmount > BigDecimal.ZERO) {
                val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
                    "SALES_INVOICE",
                    inv.invoiceNumber,
                    inv.id.toString()
                )
                if (existing == null) {
                    postSalesInvoice(inv)
                    repairedCount++
                }
            }
        }

        // 2. فحص السندات المالية غير الملغاة
        val vouchers = db.financialVoucherDao().getVouchersList()
        for (v in vouchers) {
            if (!v.isVoided && v.amount > BigDecimal.ZERO) {
                val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
                    "FINANCIAL_VOUCHER",
                    v.voucherNumber,
                    v.id.toString()
                )
                if (existing == null) {
                    postFinancialVoucher(v)
                    repairedCount++
                }
            }
        }

        // 3. فحص فواتير المشتريات غير الملغاة
        val purchases = db.purchaseInvoiceDao().getPurchaseInvoicesList()
        for (p in purchases) {
            if (!p.isVoided && p.totalAmount > BigDecimal.ZERO) {
                val existing = db.journalEntryDao().getHeaderByReferenceWithAlt(
                    "PURCHASE_INVOICE",
                    p.invoiceNumber,
                    p.id.toString()
                )
                if (existing == null) {
                    postPurchaseInvoice(p)
                    repairedCount++
                }
            }
        }

        repairedCount
    }

    /**
     * تصنيف دقيق وقوي لنوع النفقة في سندات الصرف دون أي تطابق جزئي مضلل
     */
    fun mapPaymentCategoryToAccount(category: String, partyName: String): Pair<String, String> {
        val cat = category.trim().lowercase()
        val party = partyName.trim().lowercase()

        // 0. المطابقة المباشرة إذا تم إدخال أو تحديد رقم الحساب الصريح
        if (cat.startsWith("5101") || cat.contains("5101")) return Pair("5101", "تكلفة خدمة الإنترنت والمزودين (COGS)")
        if (cat.startsWith("5201") || cat.contains("5201")) return Pair("5201", "مصروفات تشغيلية وعمومية")
        if (cat.startsWith("5202") || cat.contains("5202")) return Pair("5202", "مصروفات الصيانة وقطع الغيار")
        if (cat.startsWith("1501") || cat.contains("1501")) return Pair("1501", "أصول ومعدات الشبكة (نفقات رأسمالية)")
        if (cat.startsWith("3201") || cat.contains("3201")) return Pair("3201", "جاري الشركاء والأرباح المسحوبة")
        if (cat.startsWith("2101") || cat.contains("2101")) return Pair("2101", "الموردون وذمم المشتريات / $partyName")

        // 1. مسحوبات الشركاء وجاري الشركاء
        if (cat.contains("شريك") || cat.contains("مسحوبات") || cat.contains("أرباح") ||
            party.contains("شريك") || cat.contains("توزيع أرباح")) {
            return Pair("3201", "جاري الشركاء والأرباح المسحوبة")
        }

        // 2. المحروقات والديزل وتوليد الطاقة (مصروف تشغيلي 5201) - فحص استباقي قبل الأصول
        if (cat.contains("ديزل") || cat.contains("وقود") || cat.contains("محروقات") ||
            cat.contains("طاقة شمسية") || cat.contains("كهرباء") || cat.contains("بنزين") ||
            cat.contains("توليد") || cat.contains("زيوت")) {
            return Pair("5201", "مصروفات تشغيلية (وقود وكهرباء وطاقة)")
        }

        // 3. الصيانة والإصلاحات وقطع الغيار (مصروف صيانة 5202) - فحص استباقي قبل المعدات
        if (cat.contains("صيانة") || cat.contains("تصليح") || cat.contains("قطع غيار") ||
            cat.contains("إصلاح") || cat.contains("تأهيل أبراج")) {
            return Pair("5202", "مصروفات الصيانة وقطع الغيار")
        }

        // 4. اشتراكات وسعات الإنترنت وباندويث ستارلينك (تكلفة الخدمة 5101 COGS)
        val isBandwidth = cat.contains("إنترنت") || cat.contains("انترنت") ||
                          cat.contains("اشتراك") || cat.contains("ستارلينك") || cat.contains("starlink") ||
                          cat.contains("باندويث") || cat.contains("bandwidth") || cat.contains("مزود") ||
                          cat.contains("حزم") || cat.split(" ", "-", "_", "/").any { it == "نت" }
        if (isBandwidth) {
            return Pair("5101", "تكلفة خدمة الإنترنت والمزودين (COGS)")
        }

        // 5. الرواتب والأجور والمكافآت (5201)
        if (cat.contains("رواتب") || cat.contains("أجور") || cat.contains("مكافآت") ||
            cat.contains("راتب") || cat.contains("مهندسين") || cat.contains("حوافز")) {
            return Pair("5201", "رواتب وأجور الموظفين والمهندسين")
        }

        // 6. الأصول الثابتة والمشتريات الرأسمالية (1501 Capex)
        if (cat.contains("أصول") || cat.contains("رأسمال") ||
            ((cat.contains("شراء") || cat.contains("توريد") || cat.contains("تركيب")) &&
             (cat.contains("معدات") || cat.contains("راوتر") || cat.contains("سيرفر") || cat.contains("برج") || cat.contains("أجهزة")))) {
            return Pair("1501", "أصول ومعدات الشبكة (نفقات رأسمالية)")
        }

        // 7. مصروفات تشغيلية وعمومية عامة (5201)
        return Pair("5201", "مصروفات تشغيلية وعمومية / $category")
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
