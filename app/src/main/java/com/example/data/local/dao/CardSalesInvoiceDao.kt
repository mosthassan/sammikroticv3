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
    @Query("SELECT * FROM card_sales_invoices ORDER BY invoiceDateMillis DESC")
    fun getAllSalesInvoices(): Flow<List<CardSalesInvoiceEntity>>

    @Query("SELECT * FROM card_sales_invoices ORDER BY invoiceDateMillis DESC")
    suspend fun getSalesInvoicesList(): List<CardSalesInvoiceEntity>

    @Query("SELECT * FROM card_sales_invoices WHERE retailerId = :retailerId ORDER BY invoiceDateMillis DESC")
    suspend fun getInvoicesForRetailerList(retailerId: Long): List<CardSalesInvoiceEntity>

    @Query("SELECT * FROM card_sales_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): CardSalesInvoiceEntity?

    @Query("SELECT * FROM card_sales_invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): CardSalesInvoiceEntity?

    @Query("SELECT * FROM card_sales_invoices WHERE retailerId = :retailerId ORDER BY invoiceDateMillis DESC")
    fun getInvoicesForRetailer(retailerId: Long): Flow<List<CardSalesInvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: CardSalesInvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: CardSalesInvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: CardSalesInvoiceEntity)

    @Query("SELECT SUM(totalAmount) FROM card_sales_invoices")
    fun getTotalSalesAmount(): Flow<Double?>

    @Query("SELECT SUM(totalCardsCount) FROM card_sales_invoices")
    fun getTotalSoldCardsCount(): Flow<Int?>

    @Query("SELECT SUM(remainingAmount) FROM card_sales_invoices")
    fun getTotalCreditRemaining(): Flow<Double?>

    @Query("SELECT SUM(remainingAmount) FROM card_sales_invoices WHERE retailerId = :retailerId AND remainingAmount > 0")
    suspend fun getRemainingDebtForRetailer(retailerId: Long): Double?

    @Query("SELECT SUM(remainingAmount) FROM card_sales_invoices WHERE customerName = :customerName AND remainingAmount > 0")
    suspend fun getRemainingDebtForCustomerName(customerName: String): Double?

    @Query("SELECT SUM(remainingAmount) FROM card_sales_invoices WHERE invoiceDateMillis BETWEEN :startDate AND :endDate AND remainingAmount > 0")
    suspend fun getTotalNewDebt(startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(totalCardsCount) FROM card_sales_invoices WHERE invoiceDateMillis BETWEEN :startDate AND :endDate")
    suspend fun getSoldCardsCountFromInvoices(startDate: Long, endDate: Long): Int?

    @Query("SELECT SUM(totalAmount) FROM card_sales_invoices WHERE invoiceDateMillis BETWEEN :startDate AND :endDate")
    suspend fun getTotalSalesAmountForPeriod(startDate: Long, endDate: Long): Double?

    @Query("DELETE FROM card_sales_invoices")
    suspend fun deleteAllInvoices()

    @Query("DELETE FROM card_sales_invoices WHERE invoiceNumber LIKE 'INV-DELIV-%'")
    suspend fun deleteSyntheticInvoices()
}
