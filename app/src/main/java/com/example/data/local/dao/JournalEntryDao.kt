package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.JournalEntryHeaderEntity
import com.example.data.local.entity.JournalEntryLineEntity
import com.example.data.local.model.AccountDebitCreditSum
import com.example.data.local.model.GeneralLedgerLineRow
import com.example.data.local.model.IncomeStatementRow
import com.example.data.local.model.JournalEntryWithLines
import com.example.data.local.model.TrialBalanceRow
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

data class CustomerStatementSummary(
    val totalSales: BigDecimal,
    val totalPaid: BigDecimal,
    val finalBalance: BigDecimal
)

@Dao
interface JournalEntryDao {

    // ==========================================
    // 1. نظام القيود المعياري المزدوج (Header & Lines)
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHeader(header: JournalEntryHeaderEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLines(lines: List<JournalEntryLineEntity>): List<Long>

    @Update
    suspend fun updateHeader(header: JournalEntryHeaderEntity)

    @Transaction
    @Query("SELECT * FROM journal_entry_headers ORDER BY dateMillis DESC")
    fun getAllHeadersWithLines(): Flow<List<JournalEntryWithLines>>

    @Transaction
    @Query("SELECT * FROM journal_entry_headers WHERE id = :id LIMIT 1")
    suspend fun getHeaderWithLinesById(id: Long): JournalEntryWithLines?

    @Transaction
    @Query("SELECT * FROM journal_entry_headers WHERE entryNumber = :entryNumber LIMIT 1")
    suspend fun getHeaderWithLinesByNumber(entryNumber: String): JournalEntryWithLines?

    @Query("SELECT * FROM journal_entry_headers WHERE referenceType = :refType AND referenceId = :refId LIMIT 1")
    suspend fun getHeaderByReference(refType: String, refId: String): JournalEntryHeaderEntity?

    @Query("SELECT * FROM journal_entry_lines WHERE accountCode = :code ORDER BY id ASC")
    fun getLinesForAccount(code: String): Flow<List<JournalEntryLineEntity>>

    @Query(
        """
        SELECT 
            coa.accountCode AS accountCode,
            coa.accountNameAr AS accountName,
            COALESCE(SUM(jel.debit), '0') AS totalDebit,
            COALESCE(SUM(jel.credit), '0') AS totalCredit,
            COALESCE(SUM(jel.debit - jel.credit), '0') AS netBalance,
            coa.normalBalance AS normalBalance,
            coa.accountType AS accountType
        FROM chart_of_accounts coa
        LEFT JOIN (
            SELECT l.accountCode, l.debit, l.credit
            FROM journal_entry_lines l
            INNER JOIN journal_entry_headers h ON l.headerId = h.id
            WHERE h.status = 'POSTED'
              AND h.reversalOfEntryId IS NULL
              AND h.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
        ) jel ON coa.accountCode = jel.accountCode
        WHERE coa.isActive = 1
        GROUP BY coa.accountCode, coa.accountNameAr, coa.normalBalance, coa.accountType
        ORDER BY coa.accountCode ASC
        """
    )
    fun getTrialBalance(): Flow<List<TrialBalanceRow>>

    @Query(
        """
        SELECT 
            coa.accountCode AS accountCode,
            coa.accountNameAr AS accountName,
            COALESCE(SUM(jel.debit), '0') AS totalDebit,
            COALESCE(SUM(jel.credit), '0') AS totalCredit,
            COALESCE(SUM(jel.debit - jel.credit), '0') AS netBalance,
            coa.normalBalance AS normalBalance,
            coa.accountType AS accountType
        FROM chart_of_accounts coa
        LEFT JOIN (
            SELECT l.accountCode, l.debit, l.credit
            FROM journal_entry_lines l
            INNER JOIN journal_entry_headers h ON l.headerId = h.id
            WHERE h.status = 'POSTED' 
              AND h.reversalOfEntryId IS NULL
              AND h.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
              AND h.dateMillis BETWEEN :startDate AND :endDate
        ) jel ON coa.accountCode = jel.accountCode
        WHERE coa.isActive = 1
        GROUP BY coa.accountCode, coa.accountNameAr, coa.normalBalance, coa.accountType
        ORDER BY coa.accountCode ASC
        """
    )
    fun getPeriodTrialBalance(startDate: Long, endDate: Long): Flow<List<TrialBalanceRow>>

    @Transaction
    @Query(
        """
        SELECT 
            jel.id AS lineId,
            jeh.id AS headerId,
            jeh.entryNumber AS entryNumber,
            jeh.dateMillis AS dateMillis,
            jel.accountCode AS accountCode,
            jel.accountName AS accountName,
            jel.lineDescription AS lineDescription,
            jeh.description AS headerDescription,
            jel.debit AS debit,
            jel.credit AS credit,
            jeh.referenceType AS referenceType,
            jeh.referenceId AS referenceId
        FROM journal_entry_lines jel
        INNER JOIN journal_entry_headers jeh ON jel.headerId = jeh.id
        WHERE jeh.status = 'POSTED' 
          AND jeh.reversalOfEntryId IS NULL
          AND jeh.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
          AND jel.accountCode = :accountCode
          AND jeh.dateMillis BETWEEN :startDate AND :endDate
        ORDER BY jeh.dateMillis ASC, jel.id ASC
        """
    )
    fun getLedgerLinesForAccount(
        accountCode: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<GeneralLedgerLineRow>>

    @Query(
        """
        SELECT 
            COALESCE(SUM(jel.debit), '0') AS totalDebit,
            COALESCE(SUM(jel.credit), '0') AS totalCredit
        FROM journal_entry_lines jel
        INNER JOIN journal_entry_headers jeh ON jel.headerId = jeh.id
        WHERE jeh.status = 'POSTED'
          AND jeh.reversalOfEntryId IS NULL
          AND jeh.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
          AND jel.accountCode = :accountCode
          AND jeh.dateMillis < :startDate
        """
    )
    suspend fun getOpeningDebitCredit(accountCode: String, startDate: Long): AccountDebitCreditSum?

    @Query(
        """
        SELECT 
            coa.accountCode AS accountCode,
            coa.accountNameAr AS accountName,
            COALESCE(SUM(jel.debit), '0') AS totalDebit,
            COALESCE(SUM(jel.credit), '0') AS totalCredit,
            coa.normalBalance AS normalBalance,
            coa.accountType AS accountType
        FROM chart_of_accounts coa
        LEFT JOIN (
            SELECT l.accountCode, l.debit, l.credit
            FROM journal_entry_lines l
            INNER JOIN journal_entry_headers h ON l.headerId = h.id
            WHERE h.status = 'POSTED' 
              AND h.reversalOfEntryId IS NULL
              AND h.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
              AND h.dateMillis BETWEEN :startDate AND :endDate
        ) jel ON coa.accountCode = jel.accountCode
        WHERE coa.accountType IN ('REVENUE', 'EXPENSE') OR coa.accountCode LIKE '4%' OR coa.accountCode LIKE '5%'
        GROUP BY coa.accountCode, coa.accountNameAr, coa.normalBalance, coa.accountType
        ORDER BY coa.accountCode ASC
        """
    )
    fun getIncomeStatementRows(startDate: Long, endDate: Long): Flow<List<IncomeStatementRow>>

    // ==========================================
    // كشف حساب العميل التفصيلي وسندات التطهير
    // ==========================================

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
    fun getDetailedCustomerStatement(customerId: Long): Flow<CustomerStatementSummary>

    @Query(
        """
        SELECT 
            jel.id AS lineId,
            jeh.id AS headerId,
            jeh.entryNumber AS entryNumber,
            jeh.dateMillis AS dateMillis,
            jel.accountCode AS accountCode,
            jel.accountName AS accountName,
            jel.lineDescription AS lineDescription,
            jeh.description AS headerDescription,
            jel.debit AS debit,
            jel.credit AS credit,
            jeh.referenceType AS referenceType,
            jeh.referenceId AS referenceId
        FROM journal_entry_lines jel
        INNER JOIN journal_entry_headers jeh ON jel.headerId = jeh.id
        WHERE jeh.status = 'POSTED'
          AND jeh.reversalOfEntryId IS NULL
          AND jeh.id NOT IN (SELECT reversalOfEntryId FROM journal_entry_headers WHERE reversalOfEntryId IS NOT NULL)
          AND jel.accountCode = '1201'
          AND jel.partyId = :customerId
        ORDER BY jeh.dateMillis ASC, jel.id ASC
        """
    )
    fun getDetailedCustomerStatementLines(customerId: Long): Flow<List<GeneralLedgerLineRow>>

    @Query("DELETE FROM journal_entry_lines WHERE headerId IN (SELECT id FROM journal_entry_headers WHERE referenceId LIKE 'REC-INV-%' OR entryNumber LIKE 'REC-INV-%')")
    suspend fun sanitizeLegacyRecInvLines()

    @Query("DELETE FROM journal_entry_headers WHERE referenceId LIKE 'REC-INV-%' OR entryNumber LIKE 'REC-INV-%'")
    suspend fun sanitizeLegacyRecInvHeaders()

    @Query("DELETE FROM journal_entry_lines WHERE headerId IN (SELECT id FROM journal_entry_headers WHERE (referenceType = :refType AND referenceId = :refId) OR entryNumber = :refId OR referenceId = :refId)")
    suspend fun deleteLinesByReference(refType: String, refId: String)

    @Query("DELETE FROM journal_entry_headers WHERE (referenceType = :refType AND referenceId = :refId) OR entryNumber = :refId OR referenceId = :refId")
    suspend fun deleteHeadersByReference(refType: String, refId: String)

    @Query("DELETE FROM journal_entries WHERE (referenceType = :refType AND referenceId = :refId) OR entryNumber = :refId OR referenceId = :refId")
    suspend fun deleteLegacyEntriesByReference(refType: String, refId: String)

    @Transaction
    suspend fun purgeJournalEntriesByReference(refType: String, refId: String) {
        deleteLinesByReference(refType, refId)
        deleteHeadersByReference(refType, refId)
        deleteLegacyEntriesByReference(refType, refId)
        deleteLinesByReference("VOID_VOUCHER", refId)
        deleteHeadersByReference("VOID_VOUCHER", refId)
        deleteLegacyEntriesByReference("VOID_VOUCHER", refId)
        deleteLinesByReference("VOID_VOUCHER", "REV-$refId")
        deleteHeadersByReference("VOID_VOUCHER", "REV-$refId")
        deleteLegacyEntriesByReference("VOID_VOUCHER", "REV-$refId")
    }

    /**
     * ترحيل قيد محاسبي مزدوج متوازن إجبارياً
     * شرط التحقق: SUM(Debit) == SUM(Credit)
     */
    @Transaction
    suspend fun postBalancedEntry(
        header: JournalEntryHeaderEntity,
        lines: List<JournalEntryLineEntity>
    ): Long {
        require(lines.isNotEmpty()) { "لا يمكن ترحيل قيد محاسبي فارغ دون سطور" }

        val totalDebit = lines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.debit) }
        val totalCredit = lines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.credit) }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw IllegalArgumentException(
                "خطأ محاسبي: القيد غير متوازن! إجمالي المدين ($totalDebit) لا يساوي إجمالي الدائن ($totalCredit)"
            )
        }

        val finalizedHeader = header.copy(
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            status = "POSTED"
        )
        val headerId = insertHeader(finalizedHeader)

        val finalizedLines = lines.map { it.copy(headerId = headerId) }
        insertLines(finalizedLines)

        return headerId
    }

    /**
     * إلغاء قيد محاسبي بقيد عكسي تسوية (Audit-Safe Reversal)
     * يعكس المدين دائناً والدائن مديناً دون أي حذف صلب للبيانات
     */
    @Transaction
    suspend fun voidEntryWithReversal(
        headerId: Long,
        voidReason: String,
        performedBy: String
    ): Long? {
        val originalWithLines = getHeaderWithLinesById(headerId) ?: return null
        val originalHeader = originalWithLines.header

        if (originalHeader.voidReason != null || originalHeader.status == "VOIDED") return null

        // 1. تحديث القيد الأصلي لتوثيق سبب الإلغاء مع إبقائه POSTED للحفاظ على التوازن الدفتري المتكافئ
        updateHeader(
            originalHeader.copy(
                status = "POSTED",
                voidReason = voidReason
            )
        )

        // 2. إنشاء قيد عكسي تلقائي
        val reversalEntryNumber = "REV-${originalHeader.entryNumber}"
        val reversalHeader = JournalEntryHeaderEntity(
            entryNumber = reversalEntryNumber,
            dateMillis = System.currentTimeMillis(),
            referenceType = "VOID_REVERSAL",
            referenceId = originalHeader.entryNumber,
            description = "قيد عكسي لتسوية وإلغاء القيد #${originalHeader.entryNumber} - السبب: $voidReason",
            status = "POSTED",
            totalDebit = originalHeader.totalCredit,
            totalCredit = originalHeader.totalDebit,
            createdBy = performedBy,
            reversalOfEntryId = originalHeader.id
        )

        val reversalHeaderId = insertHeader(reversalHeader)

        // 3. عكس السطور: المدين يصبح دائناً والدائن يصبح مديناً
        val reversalLines = originalWithLines.lines.map { origLine ->
            JournalEntryLineEntity(
                headerId = reversalHeaderId,
                accountCode = origLine.accountCode,
                accountName = origLine.accountName,
                lineType = if (origLine.lineType == "DEBIT") "CREDIT" else "DEBIT",
                debit = origLine.credit,   // عكس القيمة
                credit = origLine.debit,   // عكس القيمة
                currency = origLine.currency,
                exchangeRate = origLine.exchangeRate,
                originalAmount = origLine.originalAmount,
                lineDescription = "عكس: ${origLine.lineDescription}".take(200),
                partyId = origLine.partyId,
                partyType = origLine.partyType
            )
        }

        insertLines(reversalLines)
        return reversalHeaderId
    }

    // ==========================================
    // 2. التوافقية مع القيود البسيطة السابقة
    // ==========================================

    @Query("SELECT * FROM journal_entries ORDER BY dateMillis DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE entryNumber = :entryNumber")
    suspend fun getEntryByNumber(entryNumber: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<JournalEntryEntity>): List<Long>

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllEntries()
}
