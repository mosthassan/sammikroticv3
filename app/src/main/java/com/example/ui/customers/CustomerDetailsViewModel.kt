package com.example.ui.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CustomerAccountSummary
import com.example.data.local.entity.CustomerLedgerEntity
import com.example.data.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val customerRepository = CustomerRepository(db.customerLedgerDao(), db.retailerDao())

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    fun setCustomerId(customerId: Long) {
        _selectedCustomerId.value = customerId
    }

    val accountSummary: StateFlow<CustomerAccountSummary> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null && id > 0) {
                customerRepository.getCustomerAccountSummary(id)
            } else {
                flowOf(CustomerAccountSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CustomerAccountSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        )

    val ledgerEntries: StateFlow<List<CustomerLedgerEntity>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null && id > 0) {
                customerRepository.getLedgerForCustomer(id)
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
