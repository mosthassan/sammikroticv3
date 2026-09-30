package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CustomerLedgerEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

data class CustomerAccountSummary(
    val totalSales: BigDecimal,
    val totalPaid: BigDecimal,
    val finalBalance: BigDecimal
)

@Dao
interface CustomerLedgerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: CustomerLedgerEntity)

    @Insert
    suspend fun insertLedgerEntries(entries: List<CustomerLedgerEntity>)

    @Query(
        """
        SELECT 
            COALESCE(SUM(debit), '0') AS totalSales,
            COALESCE(SUM(credit), '0') AS totalPaid,
            COALESCE(SUM(debit - credit), '0') AS finalBalance
        FROM customer_ledger
        WHERE customerId = :customerId
        """
    )
    fun getCustomerAccountSummary(customerId: Long): Flow<CustomerAccountSummary>

    @Query(
        """
        SELECT 
            COALESCE(SUM(debit), '0') AS totalSales,
            COALESCE(SUM(credit), '0') AS totalPaid,
            COALESCE(SUM(debit - credit), '0') AS finalBalance
        FROM customer_ledger
        WHERE customerId = :customerId
        """
    )
    fun getAccountSummary(customerId: Long): Flow<CustomerAccountSummary>

    @Query("SELECT * FROM customer_ledger WHERE customerId = :customerId ORDER BY transactionDate DESC")
    fun getLedgerForCustomer(customerId: Long): Flow<List<CustomerLedgerEntity>>

    @Query("DELETE FROM customer_ledger WHERE referenceId = :referenceId")
    suspend fun deleteByReferenceId(referenceId: String)

    @Query("DELETE FROM customer_ledger WHERE referenceId = :referenceId AND transactionType = :transactionType")
    suspend fun deleteByReferenceAndType(referenceId: String, transactionType: String)

    @Query("DELETE FROM customer_ledger WHERE customerId = :customerId")
    suspend fun deleteByCustomerId(customerId: Long)

    @Query("DELETE FROM customer_ledger")
    suspend fun deleteAllEntries()
}
