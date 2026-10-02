package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CardSalesInvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardSalesInvoiceDao {
    @Query("SELECT * FROM card_sales_invoices WHERE isVoided = 0 ORDER BY invoiceDateMillis DESC")
    fun getAllSalesInvoices(): Flow<List<CardSalesInvoiceEntity>>

    @Query("SELECT * FROM card_sales_invoices ORDER BY invoiceDateMillis DESC")
    fun getAllInvoicesIncludingVoided(): Flow<List<CardSalesInvoiceEntity>>

    @Query("SELECT * FROM card_sales_invoices WHERE isVoided = 0 ORDER BY invoiceDateMillis DESC")
    suspend fun getSalesInvoicesList(): List<CardSalesInvoiceEntity>

    @Query("SELECT * FROM card_sales_invoices WHERE retailerId = :retailerId AND isVoided = 0 ORDER BY invoiceDateMillis DESC")
    suspend fun getInvoicesForRetailerList(retailerId: Long): List<CardSalesInvoiceEntity>

    @Query("SELECT * FROM card_sales_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): CardSalesInvoiceEntity?

    @Query("SELECT * FROM card_sales_invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): CardSalesInvoiceEntity?

    @Query("SELECT * FROM card_sales_invoices WHERE retailerId = :retailerId AND isVoided = 0 ORDER BY invoiceDateMillis DESC")
    fun getInvoicesForRetailer(retailerId: Long): Flow<List<CardSalesInvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: CardSalesInvoiceEntity): Long

    @Query("SELECT COUNT(*) FROM card_sales_invoices")
    suspend fun getInvoicesCount(): Int

    @Update
    suspend fun updateInvoice(invoice: CardSalesInvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: CardSalesInvoiceEntity)

    @Query("SELECT SUM(CAST(totalAmount AS REAL)) FROM card_sales_invoices WHERE isVoided = 0")
    fun getTotalSalesAmount(): Flow<Double?>

    @Query("SELECT SUM(totalCardsCount) FROM card_sales_invoices WHERE isVoided = 0")
    fun getTotalSoldCardsCount(): Flow<Int?>

    @Query("SELECT SUM(CAST(remainingAmount AS REAL)) FROM card_sales_invoices WHERE isVoided = 0")
    fun getTotalCreditRemaining(): Flow<Double?>

    @Query("SELECT SUM(CAST(remainingAmount AS REAL)) FROM card_sales_invoices WHERE retailerId = :retailerId AND remainingAmount > '0' AND isVoided = 0")
    suspend fun getRemainingDebtForRetailer(retailerId: Long): Double?

    @Query("SELECT SUM(CAST(remainingAmount AS REAL)) FROM card_sales_invoices WHERE customerName = :customerName AND remainingAmount > '0' AND isVoided = 0")
    suspend fun getRemainingDebtForCustomerName(customerName: String): Double?

    @Query("SELECT SUM(CAST(remainingAmount AS REAL)) FROM card_sales_invoices WHERE invoiceDateMillis BETWEEN :startDate AND :endDate AND remainingAmount > '0' AND isVoided = 0")
    suspend fun getTotalNewDebt(startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(totalCardsCount) FROM card_sales_invoices WHERE invoiceDateMillis BETWEEN :startDate AND :endDate AND isVoided = 0")
    suspend fun getSoldCardsCountFromInvoices(startDate: Long, endDate: Long): Int?

    @Query("SELECT SUM(CAST(totalAmount AS REAL)) FROM card_sales_invoices WHERE invoiceDateMillis BETWEEN :startDate AND :endDate AND isVoided = 0")
    suspend fun getTotalSalesAmountForPeriod(startDate: Long, endDate: Long): Double?

    @Query("DELETE FROM card_sales_invoices WHERE invoiceNumber LIKE 'SYNTHETIC-%'")
    suspend fun deleteSyntheticInvoices()

    @Query("DELETE FROM card_sales_invoices")
    suspend fun deleteAllInvoices()
}
