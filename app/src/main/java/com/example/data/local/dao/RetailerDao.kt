package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RetailerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RetailerDao {
    @Query("SELECT * FROM retailers ORDER BY balanceOwed DESC")
    fun getAllRetailers(): Flow<List<RetailerEntity>>

    @Query("SELECT * FROM retailers ORDER BY balanceOwed DESC")
    suspend fun getRetailersList(): List<RetailerEntity>

    @Query("SELECT * FROM retailers WHERE id = :id")
    suspend fun getRetailerById(id: Long): RetailerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRetailer(retailer: RetailerEntity): Long

    @Update
    suspend fun updateRetailer(retailer: RetailerEntity)

    @Delete
    suspend fun deleteRetailer(retailer: RetailerEntity)

    @Query("UPDATE retailers SET balanceOwed = balanceOwed + :amountDelta, activeCardsCount = activeCardsCount + :cardsDelta WHERE id = :retailerId")
    suspend fun updateBalanceAndCards(retailerId: Long, amountDelta: Double, cardsDelta: Int)

    @Query("UPDATE retailers SET balanceOwed = balanceOwed - :paymentAmount, totalPaid = totalPaid + :paymentAmount WHERE id = :retailerId")
    suspend fun recordPayment(retailerId: Long, paymentAmount: Double)

    @Query("DELETE FROM retailers")
    suspend fun deleteAllRetailers()
}
