package com.example.ui.vouchers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.repository.NetworkRepository
import com.example.util.CurrencyHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class VoucherSummaryRollup(
    val totalReceipts: Double = 0.0,
    val totalPayments: Double = 0.0,
    val netMovement: Double = 0.0,
    val summaryCurrency: String = "YER"
)

class VouchersViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NetworkRepository(db)

    val vouchers: StateFlow<List<FinancialVoucherEntity>> = repository.allVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sarToYerRate: StateFlow<Double> = repository.networkIdentity
        .map { if (it?.sarToYerRate ?: 0.0 > 0) it!!.sarToYerRate else 140.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 140.0)

    val usdToYerRate: StateFlow<Double> = repository.networkIdentity
        .map { if (it?.usdToYerRate ?: 0.0 > 0) it!!.usdToYerRate else 530.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 530.0)

    /**
     * Calculates dynamic multi-currency voucher summary rollup:
     * - If all non-voided vouchers belong to a single currency (e.g. "USD"), displays totals in "USD".
     * - If mixed currencies exist, converts foreign currencies to Base Currency ("YER") using actual exchange rates.
     */
    val voucherSummaryRollup: StateFlow<VoucherSummaryRollup> = combine(
        vouchers,
        sarToYerRate,
        usdToYerRate
    ) { voucherList, sarRate, usdRate ->
        val activeVouchers = voucherList.filter { !it.isVoided }
        if (activeVouchers.isEmpty()) {
            VoucherSummaryRollup()
        } else {
            val distinctCurrencies = activeVouchers.map { it.currency.uppercase() }.toSet()
            if (distinctCurrencies.size == 1) {
                val singleCurr = distinctCurrencies.first()
                val receipts = activeVouchers.filter { it.voucherType == "RECEIPT" }.sumOf { it.amount.toDouble() }
                val payments = activeVouchers.filter { it.voucherType == "PAYMENT" }.sumOf { it.amount.toDouble() }
                VoucherSummaryRollup(
                    totalReceipts = receipts,
                    totalPayments = payments,
                    netMovement = receipts - payments,
                    summaryCurrency = singleCurr
                )
            } else {
                val receiptsYer = activeVouchers.filter { it.voucherType == "RECEIPT" }.sumOf { v ->
                    CurrencyHelper.convertToYer(v.amount.toDouble(), v.currency, sarRate, usdRate)
                }
                val paymentsYer = activeVouchers.filter { it.voucherType == "PAYMENT" }.sumOf { v ->
                    CurrencyHelper.convertToYer(v.amount.toDouble(), v.currency, sarRate, usdRate)
                }
                VoucherSummaryRollup(
                    totalReceipts = receiptsYer,
                    totalPayments = paymentsYer,
                    netMovement = receiptsYer - paymentsYer,
                    summaryCurrency = "YER"
                )
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VoucherSummaryRollup()
    )
}
