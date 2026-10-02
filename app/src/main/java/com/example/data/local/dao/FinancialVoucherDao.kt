package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FinancialVoucherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialVoucherDao {
    @Query("SELECT * FROM financial_vouchers ORDER BY dateMillis DESC")
    fun getAllVouchers(): Flow<List<FinancialVoucherEntity>>

    @Query("SELECT * FROM financial_vouchers WHERE voucherType = :type ORDER BY dateMillis DESC")
    fun getVouchersByType(type: String): Flow<List<FinancialVoucherEntity>>

    @Query("SELECT * FROM financial_vouchers WHERE retailerId = :retailerId ORDER BY dateMillis DESC")
    fun getVouchersForRetailer(retailerId: Long): Flow<List<FinancialVoucherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: FinancialVoucherEntity): Long

    @Query("SELECT COUNT(*) FROM financial_vouchers")
    suspend fun getVouchersCount(): Int

    @Update
    suspend fun updateVoucher(voucher: FinancialVoucherEntity)

    @Delete
    suspend fun deleteVoucher(voucher: FinancialVoucherEntity)

    @Query("SELECT * FROM financial_vouchers ORDER BY dateMillis DESC")
    suspend fun getVouchersList(): List<FinancialVoucherEntity>

    @Query("SELECT * FROM financial_vouchers WHERE invoiceId = :invoiceId")
    suspend fun getVouchersByInvoiceId(invoiceId: Long): List<FinancialVoucherEntity>

    @Query("SELECT * FROM financial_vouchers WHERE invoiceNumber = :invoiceNumber")
    suspend fun getVouchersByInvoiceNumber(invoiceNumber: String): List<FinancialVoucherEntity>

    @Query("DELETE FROM financial_vouchers WHERE invoiceId = :invoiceId")
    suspend fun deleteVouchersByInvoiceId(invoiceId: Long)

    @Query("DELETE FROM financial_vouchers WHERE invoiceNumber = :invoiceNumber")
    suspend fun deleteVouchersByInvoiceNumber(invoiceNumber: String)

    @Query("SELECT SUM(amount) FROM financial_vouchers WHERE voucherType = 'RECEIPT' AND isVoided = 0")
    fun getTotalReceipts(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_vouchers WHERE voucherType = 'PAYMENT' AND isVoided = 0 AND category NOT IN ('CAPEX', 'أصول ثابتة', 'أصول', 'رأس المال', 'CAPEX / أصول ثابتة')")
    fun getTotalPayments(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_vouchers WHERE voucherType = 'PAYMENT' AND isVoided = 0 AND category IN ('CAPEX', 'أصول ثابتة', 'أصول', 'CAPEX / أصول ثابتة')")
    fun getTotalAssetsValue(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_vouchers WHERE voucherType = 'RECEIPT' AND isVoided = 0 AND dateMillis BETWEEN :startDate AND :endDate")
    suspend fun getTotalReceiptsAmount(startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(amount) FROM financial_vouchers WHERE voucherType = 'PAYMENT' AND isVoided = 0 AND category NOT IN ('CAPEX', 'أصول ثابتة', 'أصول', 'رأس المال', 'CAPEX / أصول ثابتة') AND dateMillis BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpensesAmount(startDate: Long, endDate: Long): Double?

    @Query("DELETE FROM financial_vouchers WHERE voucherNumber LIKE 'REC-INV-%'")
    suspend fun sanitizeLegacyRecInvVouchers()

    @Query("DELETE FROM financial_vouchers")
    suspend fun deleteAllVouchers()
}
