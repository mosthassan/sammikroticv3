package com.example.data.local

import android.util.Log
import com.example.data.local.entity.CustomerLedgerEntity
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal

object CustomerLedgerBackfillHelper {

    /**
     * Checks if customer_ledger is empty on first app start after update.
     * If empty, auto-populates customer_ledger from existing legacy tables asynchronously and safely.
     */
    suspend fun backfillIfEmpty(db: AppDatabase) {
        try {
            val sampleRetailers = try {
                db.retailerDao().getRetailersList()
            } catch (e: Throwable) {
                emptyList()
            }
            if (sampleRetailers.isEmpty()) return

            var totalLedgerEntriesCount = 0
            for (retailer in sampleRetailers) {
                try {
                    val summary = db.customerLedgerDao().getCustomerAccountSummary(retailer.id).firstOrNull()
                    if (summary != null && (summary.totalSales > BigDecimal.ZERO || summary.totalPaid > BigDecimal.ZERO)) {
                        totalLedgerEntriesCount++
                    }
                } catch (e: Throwable) {
                    // Ignore single customer check failure
                }
            }

            if (totalLedgerEntriesCount > 0) {
                Log.d("LedgerBackfill", "customer_ledger already populated. Skipping backfill.")
                return
            }

            Log.i("LedgerBackfill", "customer_ledger is empty. Backfilling legacy invoices and vouchers...")

            val allInvoices = try {
                db.cardSalesInvoiceDao().getSalesInvoicesList()
            } catch (e: Throwable) {
                emptyList()
            }

            val allVouchers = try {
                db.financialVoucherDao().getVouchersList()
            } catch (e: Throwable) {
                emptyList()
            }

            val retailersByName = sampleRetailers.associateBy { it.name.trim().lowercase() }
            val newEntries = mutableListOf<CustomerLedgerEntity>()

            for (inv in allInvoices) {
                try {
                    val customerId = inv.retailerId ?: retailersByName[inv.customerName.trim().lowercase()]?.id
                    if (customerId != null && customerId > 0) {
                        newEntries.add(
                            CustomerLedgerEntity(
                                customerId = customerId,
                                transactionDate = inv.invoiceDateMillis,
                                transactionType = "INVOICE",
                                referenceId = inv.id.toString(),
                                debit = inv.totalAmount,
                                credit = BigDecimal.ZERO,
                                description = "فاتورة مبيعات #${inv.invoiceNumber}"
                            )
                        )

                        val hasLinkedVoucher = allVouchers.any { v ->
                            !v.isVoided && (v.invoiceId == inv.id || (v.invoiceNumber.isNotBlank() && v.invoiceNumber == inv.invoiceNumber))
                        }
                        if (!hasLinkedVoucher && inv.paidAmount > BigDecimal.ZERO) {
                            newEntries.add(
                                CustomerLedgerEntity(
                                    customerId = customerId,
                                    transactionDate = inv.invoiceDateMillis,
                                    transactionType = "PAYMENT",
                                    referenceId = "INV_PAY_${inv.id}",
                                    debit = BigDecimal.ZERO,
                                    credit = inv.paidAmount,
                                    description = "سداد مع الفاتورة #${inv.invoiceNumber}"
                                )
                            )
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("LedgerBackfill", "Error parsing invoice entry for backfill: ${e.message}")
                }
            }

            for (v in allVouchers) {
                try {
                    if (v.isVoided) continue
                    val customerId = v.retailerId ?: retailersByName[v.partyName.trim().lowercase()]?.id
                    if (customerId != null && customerId > 0) {
                        newEntries.add(
                            CustomerLedgerEntity(
                                customerId = customerId,
                                transactionDate = v.dateMillis,
                                transactionType = if (v.voucherType == "RECEIPT") "PAYMENT" else "INVOICE",
                                referenceId = v.id.toString(),
                                debit = if (v.voucherType == "PAYMENT") v.amount else BigDecimal.ZERO,
                                credit = if (v.voucherType == "RECEIPT") v.amount else BigDecimal.ZERO,
                                description = "سند ${if (v.voucherType == "RECEIPT") "قبض" else "صرف"} #${v.voucherNumber}"
                            )
                        )
                    }
                } catch (e: Throwable) {
                    Log.e("LedgerBackfill", "Error parsing voucher entry for backfill: ${e.message}")
                }
            }

            if (newEntries.isNotEmpty()) {
                db.customerLedgerDao().insertLedgerEntries(newEntries)
                Log.i("LedgerBackfill", "Successfully backfilled ${newEntries.size} ledger entries into customer_ledger.")
            }
        } catch (e: Throwable) {
            Log.e("LedgerBackfill", "Error during customer_ledger backfill: ${e.message}", e)
        }
    }
}
