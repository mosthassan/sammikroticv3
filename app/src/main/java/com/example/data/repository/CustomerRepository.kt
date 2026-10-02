package com.example.data.repository

import com.example.data.local.dao.CustomerAccountSummary
import com.example.data.local.dao.CustomerLedgerDao
import com.example.data.local.dao.RetailerDao
import com.example.data.local.entity.CustomerLedgerEntity
import com.example.data.local.entity.RetailerEntity
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerLedgerDao: CustomerLedgerDao,
    private val retailerDao: RetailerDao
) {
    /**
     * Calculates customer balance, total sales, and total paid EXCLUSIVELY via CustomerLedgerDao.getCustomerAccountSummary (account 1201)
     */
    fun getCustomerAccountSummary(customerId: Long): Flow<CustomerAccountSummary> {
        return customerLedgerDao.getCustomerAccountSummary(customerId)
    }

    fun getDetailedCustomerStatement(customerId: Long): Flow<CustomerAccountSummary> {
        return customerLedgerDao.getCustomerAccountSummary(customerId)
    }

    fun getLedgerForCustomer(customerId: Long): Flow<List<CustomerLedgerEntity>> {
        return customerLedgerDao.getLedgerForCustomer(customerId)
    }

    suspend fun insertLedgerEntry(entry: CustomerLedgerEntity) {
        customerLedgerDao.insertLedgerEntry(entry)
    }

    suspend fun insertLedgerEntries(entries: List<CustomerLedgerEntity>) {
        customerLedgerDao.insertLedgerEntries(entries)
    }

    suspend fun deleteLedgerByReference(referenceId: String) {
        customerLedgerDao.deleteByReferenceId(referenceId)
    }

    fun getAllCustomers(): Flow<List<RetailerEntity>> {
        return retailerDao.getAllRetailers()
    }

    suspend fun getCustomerById(id: Long): RetailerEntity? {
        return retailerDao.getRetailerById(id)
    }
}
