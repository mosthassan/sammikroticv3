package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PurchaseInvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseInvoiceDao {
    @Query("SELECT * FROM purchase_invoices WHERE isVoided = 0 ORDER BY invoiceDateMillis DESC")
    fun getAllInvoices(): Flow<List<PurchaseInvoiceEntity>>

    @Query("SELECT * FROM purchase_invoices WHERE isVoided = 0 ORDER BY invoiceDateMillis DESC")
    suspend fun getPurchaseInvoicesList(): List<PurchaseInvoiceEntity>

    @Query("SELECT * FROM purchase_invoices WHERE targetType = :targetType AND isVoided = 0 ORDER BY invoiceDateMillis DESC")
    fun getInvoicesByTargetType(targetType: String): Flow<List<PurchaseInvoiceEntity>>

    @Query("SELECT * FROM purchase_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): PurchaseInvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: PurchaseInvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: PurchaseInvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: PurchaseInvoiceEntity)

    @Query("SELECT SUM(CAST(totalAmount AS REAL)) FROM purchase_invoices WHERE isVoided = 0")
    fun getTotalInvoicesAmount(): Flow<Double?>

    @Query("SELECT SUM(CAST(totalAmount AS REAL)) FROM purchase_invoices WHERE invoiceDateMillis BETWEEN :startDate AND :endDate AND isVoided = 0")
    suspend fun getTotalPurchaseInvoicesAmount(startDate: Long, endDate: Long): Double?

    @Query("DELETE FROM purchase_invoices")
    suspend fun deleteAllInvoices()
}
