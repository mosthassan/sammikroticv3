package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.JournalEntryHeaderEntity
import com.example.data.local.entity.JournalEntryLineEntity
import com.example.data.model.CardSalesInvoiceItem
import androidx.room.withTransaction
import java.math.BigDecimal
import java.util.Locale
import kotlin.random.Random

class SalesInvoiceRepository(private val db: AppDatabase) {

    /**
     * Saves a sales invoice with an immediate payment or down-payment.
     * ELIMINATES any duplicate payment entries:
     * - NEVER creates or inserts any entity with prefix `REC-INV-` or `INV_PAY_`.
     * - Payment collected generates ONLY a SINGLE standalone receipt voucher (`REC-2026-XXXX`)
     *   and a SINGLE balanced double-entry JournalEntry.
     */
    suspend fun saveInvoiceWithPayment(
        customerName: String,
        customerPhone: String = "",
        retailerId: Long? = null,
        items: List<CardSalesInvoiceItem>,
        paymentType: String = "CASH",
        paidAmount: Double = 0.0,
        notes: String = "",
        issuerName: String = "المهندس حسن"
    ): Long {
        return db.withTransaction {
            val totalAmount = BigDecimal.valueOf(items.sumOf { it.lineTotal })
            val totalCards = items.sumOf { it.quantity }
            val inputPaidBd = BigDecimal.valueOf(paidAmount)

            val upperType = paymentType.trim().uppercase(Locale.ROOT)
            val isCreditType = upperType == "CREDIT" || paymentType.contains("آجل")
            val finalPaidAmount = if (isCreditType) BigDecimal.ZERO else inputPaidBd.max(BigDecimal.ZERO).min(totalAmount)
            val finalRemainingAmount = totalAmount.subtract(finalPaidAmount).max(BigDecimal.ZERO)
            val finalCanonicalType = when {
                isCreditType -> "CREDIT"
                finalRemainingAmount.compareTo(BigDecimal("0.01")) <= 0 -> "CASH"
                finalPaidAmount.compareTo(BigDecimal("0.01")) <= 0 -> "CREDIT"
                else -> "PARTIAL"
            }
            val finalStatus = when {
                finalRemainingAmount.compareTo(BigDecimal("0.01")) <= 0 -> "PAID"
                finalPaidAmount.compareTo(BigDecimal("0.01")) <= 0 -> "CREDIT"
                else -> "PARTIAL"
            }

            val count = db.cardSalesInvoiceDao().getSalesInvoicesList().size + 1
            var candidateNumber = "INV-2026-${String.format(Locale.US, "%04d", count)}"
            var offset = 1
            while (db.cardSalesInvoiceDao().getInvoiceByNumber(candidateNumber) != null) {
                candidateNumber = "INV-2026-${String.format(Locale.US, "%04d", count + offset)}"
                offset++
            }
            val invoiceNumber = candidateNumber
            val itemsSummary = items.joinToString(" + ") { "${it.quantity} كرت [${it.packageName}]" }

            val jsonArray = org.json.JSONArray()
            items.forEach { item ->
                val obj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("packageName", item.packageName)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("retailPrice", item.retailPrice)
                    put("lineTotal", item.lineTotal)
                }
                jsonArray.put(obj)
            }

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = customerName.ifBlank { "عميل نقدي" },
                customerPhone = customerPhone,
                retailerId = retailerId,
                paymentType = finalCanonicalType,
                totalAmount = totalAmount,
                paidAmount = finalPaidAmount,
                remainingAmount = finalRemainingAmount,
                totalCardsCount = totalCards,
                itemsCount = items.size,
                itemsSummary = itemsSummary,
                itemsJson = jsonArray.toString(),
                notes = notes,
                issuerName = issuerName,
                status = finalStatus
            )
            val invoiceId = db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)

            // Stock deduction and inventory movements
            items.forEach { item ->
                val inventoryItem = findInventoryItemFlexible(item.packageName)
                val updatedQty = if (inventoryItem != null) {
                    val q = (inventoryItem.quantityAvailable - item.quantity).coerceAtLeast(0)
                    db.inventoryDao().updateItem(inventoryItem.copy(quantityAvailable = q))
                    q
                } else {
                    val newItem = com.example.data.local.entity.InventoryItemEntity(
                        packageName = item.packageName.trim(),
                        quantityAvailable = 0,
                        wholesalePrice = BigDecimal.valueOf(item.unitPrice),
                        retailPrice = BigDecimal.valueOf(item.retailPrice)
                    )
                    db.inventoryDao().insertItem(newItem)
                    0
                }
                db.inventoryMovementDao().insertMovement(
                    InventoryMovementEntity(
                        packageName = item.packageName,
                        movementType = "SALE",
                        quantityChange = -item.quantity,
                        resultingBalance = updatedQty,
                        referenceNumber = invoiceNumber,
                        customerOrSupplier = customerName.ifBlank { "عميل نقدي" },
                        unitPrice = item.unitPrice,
                        notes = "خصم مبيعات فاتورة رقم $invoiceNumber"
                    )
                )
            }

            // Create ONLY a single standalone receipt voucher with prefix "REC-" if payment > 0
            // NEVER generate any entity with prefix "REC-INV-" or "INV_PAY_"
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            var voucherNumberGenerated = ""
            if (finalPaidAmount > BigDecimal.ZERO) {
                voucherNumberGenerated = db.numberSequenceDao().getNextNumber("REC", currentYear, "REC")
                val voucher = FinancialVoucherEntity(
                    voucherNumber = voucherNumberGenerated,
                    voucherType = "RECEIPT",
                    amount = finalPaidAmount,
                    partyName = customerName.ifBlank { "مبيعات كروت نقدية" },
                    retailerId = retailerId,
                    invoiceId = invoiceId,
                    invoiceNumber = invoiceNumber,
                    allocatedAmount = finalPaidAmount,
                    category = "مبيعات كروت",
                    paymentMethod = if (finalCanonicalType == "CASH") "نقداً" else "دفعة مقدمة",
                    description = "متحصلات من فاتورة مبيعات كروت $invoiceNumber - $itemsSummary",
                    issuerName = issuerName,
                    notes = notes
                )
                db.financialVoucherDao().insertVoucher(voucher)
            }

            // Generate SINGLE balanced JournalEntry for Sales
            val salesJeNumber = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")
            val lines = mutableListOf<JournalEntryLineEntity>()
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "1201",
                    accountName = "ذمم العملاء والوكلاء (مدينون) / $customerName",
                    lineType = "DEBIT",
                    debit = totalAmount,
                    partyId = retailerId,
                    partyType = "RETAILER",
                    lineDescription = "مبيعات فاتورة #$invoiceNumber"
                )
            )
            lines.add(
                JournalEntryLineEntity(
                    headerId = 0L,
                    accountCode = "4101",
                    accountName = "إيرادات مبيعات كروت الشبكة",
                    lineType = "CREDIT",
                    credit = totalAmount,
                    lineDescription = "إيراد مبيعات كروت فاتورة #$invoiceNumber"
                )
            )

            db.journalEntryDao().postBalancedEntry(
                JournalEntryHeaderEntity(
                    entryNumber = salesJeNumber,
                    dateMillis = invoiceEntity.invoiceDateMillis,
                    referenceType = "SALES_INVOICE",
                    referenceId = invoiceNumber,
                    description = "قيد مبيعات كروت فاتورة رقم $invoiceNumber ($customerName)",
                    createdBy = issuerName
                ),
                lines
            )

            if (finalPaidAmount > BigDecimal.ZERO) {
                val receiptJeNumber = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")
                val receiptLines = listOf(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "1101",
                        accountName = "الصندوق الرئيسي (النقدية)",
                        lineType = "DEBIT",
                        debit = finalPaidAmount,
                        lineDescription = "متحصلات سداد فاتورة #$invoiceNumber"
                    ),
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "1201",
                        accountName = "ذمم العملاء والوكلاء (مدينون) / $customerName",
                        lineType = "CREDIT",
                        credit = finalPaidAmount,
                        partyId = retailerId,
                        partyType = "RETAILER",
                        lineDescription = "سداد دفعة فاتورة #$invoiceNumber"
                    )
                )

                db.journalEntryDao().postBalancedEntry(
                    JournalEntryHeaderEntity(
                        entryNumber = receiptJeNumber,
                        dateMillis = invoiceEntity.invoiceDateMillis,
                        referenceType = "FINANCIAL_VOUCHER",
                        referenceId = voucherNumberGenerated.ifBlank { invoiceNumber },
                        description = "قيد تحصيل سداد فاتورة رقم $invoiceNumber ($customerName)",
                        createdBy = issuerName
                    ),
                    receiptLines
                )
            }

            // ترحيل قيد تكلفة البضاعة المباعة (COGS) ومخزون الكروت المطبوعة
            val cogsAmount = totalAmount.multiply(BigDecimal("0.5")) // احتساب تقديري محافظ
            if (cogsAmount > BigDecimal.ZERO) {
                val cogsJeNumber = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")
                val cogsLines = listOf(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "5101",
                        accountName = "تكلفة الكروت المباعة (COGS)",
                        lineType = "DEBIT",
                        debit = cogsAmount,
                        credit = BigDecimal.ZERO,
                        lineDescription = "تكلفة كروت مباعة فاتورة #$invoiceNumber"
                    ),
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "1301",
                        accountName = "مخزون كروت الشبكة المطبوعة",
                        lineType = "CREDIT",
                        debit = BigDecimal.ZERO,
                        credit = cogsAmount,
                        lineDescription = "صرف كروت من المخزون لفاتورة #$invoiceNumber"
                    )
                )
                db.journalEntryDao().postBalancedEntry(
                    JournalEntryHeaderEntity(
                        entryNumber = cogsJeNumber,
                        dateMillis = invoiceEntity.invoiceDateMillis,
                        referenceType = "COGS",
                        referenceId = invoiceNumber,
                        description = "إثبات تكلفة البضاعة المباعة لفاتورة #$invoiceNumber",
                        createdBy = issuerName
                    ),
                    cogsLines
                )
            }

            invoiceId
        }
    }

    private suspend fun findInventoryItemFlexible(packageName: String): com.example.data.local.entity.InventoryItemEntity? {
        val cleanName = packageName.trim()
        if (cleanName.isBlank()) return null

        val exact = db.inventoryDao().getItemByPackageName(packageName)
        if (exact != null) return exact

        val trimmed = db.inventoryDao().getItemByPackageNameTrimmed(cleanName)
        if (trimmed != null) return trimmed

        val allItems = db.inventoryDao().getAllItemsList()
        val normalizedTarget = normalizeArabicText(cleanName)

        return allItems.find { item ->
            val cleanItemName = item.packageName.trim()
            cleanItemName.equals(cleanName, ignoreCase = true) ||
            normalizeArabicText(cleanItemName) == normalizedTarget ||
            cleanItemName.contains(cleanName, ignoreCase = true) ||
            cleanName.contains(cleanItemName, ignoreCase = true)
        }
    }

    private fun normalizeArabicText(text: String): String {
        return text.trim()
            .replace("[أإآا]".toRegex(), "ا")
            .replace("[ةه]".toRegex(), "ه")
            .replace("[ئىي]".toRegex(), "ي")
            .lowercase()
    }
}
