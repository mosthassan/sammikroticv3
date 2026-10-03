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
            COALESCE(SUM(jel.debit), '0') AS totalSales,
            COALESCE(SUM(jel.credit), '0') AS totalPaid,
            COALESCE(SUM(jel.debit - jel.credit), '0') AS finalBalance
        FROM journal_entry_lines jel
        INNER JOIN journal_entry_headers jeh ON jel.headerId = jeh.id
        WHERE jeh.status = 'POSTED'
          AND jeh.reversalOfEntryId IS NULL
          AND jeh.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
          AND jel.accountCode = '1201'
          AND jel.partyId = :customerId
        """
    )
    fun getCustomerAccountSummary(customerId: Long): Flow<CustomerAccountSummary>

    @Query(
        """
        SELECT 
            COALESCE(SUM(jel.debit), '0') AS totalSales,
            COALESCE(SUM(jel.credit), '0') AS totalPaid,
            COALESCE(SUM(jel.debit - jel.credit), '0') AS finalBalance
        FROM journal_entry_lines jel
        INNER JOIN journal_entry_headers jeh ON jel.headerId = jeh.id
        WHERE jeh.status = 'POSTED'
          AND jeh.reversalOfEntryId IS NULL
          AND jeh.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
          AND jel.accountCode = '1201'
          AND jel.partyId = :customerId
        """
    )
    fun getAccountSummary(customerId: Long): Flow<CustomerAccountSummary>

    @Query(
        """
        SELECT 
            jel.id AS id,
            jel.partyId AS customerId,
            jeh.dateMillis AS transactionDate,
            CASE 
                WHEN jeh.referenceType LIKE '%INVOICE%' THEN 'INVOICE'
                WHEN jeh.referenceType LIKE '%VOUCHER%' THEN 'PAYMENT'
                ELSE jeh.referenceType 
            END AS transactionType,
            COALESCE(jeh.referenceId, jeh.entryNumber) AS referenceId,
            jel.debit AS debit,
            jel.credit AS credit,
            COALESCE(jel.lineDescription, jeh.description) AS description
        FROM journal_entry_lines jel
        INNER JOIN journal_entry_headers jeh ON jel.headerId = jeh.id
        WHERE jeh.status = 'POSTED'
          AND jeh.reversalOfEntryId IS NULL
          AND jeh.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
          AND jel.accountCode = '1201'
          AND jel.partyId = :customerId
        ORDER BY jeh.dateMillis DESC, jel.id DESC
        """
    )
    fun getLedgerForCustomer(customerId: Long): Flow<List<CustomerLedgerEntity>>

    @Query("DELETE FROM customer_ledger WHERE referenceId = :referenceId")
    suspend fun deleteByReferenceId(referenceId: String)

    @Query("DELETE FROM customer_ledger WHERE referenceId = :referenceId AND transactionType = :transactionType")
    suspend fun deleteByReferenceAndType(referenceId: String, transactionType: String)

    @Query("DELETE FROM customer_ledger WHERE customerId = :customerId")
    suspend fun deleteByCustomerId(customerId: Long)

    @Query("DELETE FROM customer_ledger WHERE referenceId LIKE 'REC-INV-%' OR referenceId LIKE 'INV_PAY_%'")
    suspend fun sanitizeLegacyRecInvLedger()

    @Query("DELETE FROM customer_ledger")
    suspend fun deleteAllEntries()
}
