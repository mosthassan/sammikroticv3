package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partners ORDER BY CAST(capitalInvested AS REAL) DESC")
    fun getAllPartners(): Flow<List<PartnerEntity>>

    @Query("SELECT * FROM partners WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getActivePartnersList(): List<PartnerEntity>

    @Query("SELECT * FROM partners ORDER BY id ASC")
    suspend fun getAllPartnersList(): List<PartnerEntity>

    @Query("SELECT * FROM partners WHERE id = :id LIMIT 1")
    suspend fun getPartnerById(id: Long): PartnerEntity?

    @Query("SELECT SUM(CAST(capitalInvested AS REAL)) FROM partners WHERE isActive = 1")
    fun getTotalInvestedCapital(): Flow<Double?>

    @Query("SELECT SUM(CAST(totalWithdrawnProfit AS REAL)) FROM partners")
    fun getTotalDistributedProfits(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PartnerEntity): Long

    @Update
    suspend fun updatePartner(partner: PartnerEntity)

    @Delete
    suspend fun deletePartner(partner: PartnerEntity)

    // Transactions
    @Query("SELECT * FROM partner_transactions WHERE partnerId = :partnerId ORDER BY dateMillis DESC")
    fun getTransactionsForPartner(partnerId: Long): Flow<List<PartnerTransactionEntity>>

    @Query("SELECT * FROM partner_transactions ORDER BY dateMillis DESC")
    fun getAllPartnerTransactions(): Flow<List<PartnerTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartnerTransaction(tx: PartnerTransactionEntity): Long

    @Delete
    suspend fun deletePartnerTransaction(tx: PartnerTransactionEntity)

    @Query("DELETE FROM partners")
    suspend fun deleteAllPartners()

    @Query("DELETE FROM partner_transactions")
    suspend fun deleteAllTransactions()
}
