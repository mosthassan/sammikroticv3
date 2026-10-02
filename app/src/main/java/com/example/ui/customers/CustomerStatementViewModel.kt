package com.example.ui.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CustomerStatementSummary
import com.example.data.local.model.GeneralLedgerLineRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerStatementViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    fun setCustomerId(customerId: Long) {
        _selectedCustomerId.value = customerId
    }

    /**
     * Calculates customer statement summary solely from POSTED JournalEntryLineEntity rows (account 1201).
     * Formula:
     * - Total Sales (Debit) = SUM of debit for account 1201 (Accounts Receivable)
     * - Total Paid (Credit) = SUM of credit for account 1201
     * - Final Balance = Total Sales - Total Paid
     */
    fun getDetailedCustomerStatement(customerId: Long): Flow<CustomerStatementSummary> {
        return db.journalEntryDao().getDetailedCustomerStatement(customerId)
    }

    val statementSummary: StateFlow<CustomerStatementSummary> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null && id > 0) {
                db.journalEntryDao().getDetailedCustomerStatement(id)
            } else {
                flowOf(CustomerStatementSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CustomerStatementSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        )

    val statementLines: StateFlow<List<GeneralLedgerLineRow>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null && id > 0) {
                db.journalEntryDao().getDetailedCustomerStatementLines(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
}
