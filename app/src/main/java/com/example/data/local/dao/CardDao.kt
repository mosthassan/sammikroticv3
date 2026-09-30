package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM card_batches ORDER BY id DESC")
    fun getAllBatches(): Flow<List<CardBatchEntity>>

    @Query("SELECT * FROM cards ORDER BY id DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE batchId = :batchId ORDER BY id ASC")
    fun getCardsByBatch(batchId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE status = 'AVAILABLE' AND batchId = :batchId LIMIT :limit")
    suspend fun getAvailableCardsForBatch(batchId: Long, limit: Int): List<CardEntity>

    @Query("SELECT * FROM cards WHERE status = 'AVAILABLE'")
    fun getAvailableCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE retailerId = :retailerId")
    fun getCardsByRetailer(retailerId: Long): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards WHERE retailerId = :retailerId AND status = 'DISTRIBUTED'")
    suspend fun getDistributedCardsCountForRetailer(retailerId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: CardBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Update
    suspend fun updateCards(cards: List<CardEntity>)

    @Query("UPDATE cards SET status = 'DISTRIBUTED', retailerId = :retailerId, retailerName = :retailerName, distributedAt = :timestamp WHERE id IN (:cardIds)")
    suspend fun assignCardsToRetailer(cardIds: List<Long>, retailerId: Long, retailerName: String, timestamp: Long)

    @Query("SELECT * FROM cards WHERE status = 'AVAILABLE' AND (categoryName = :categoryName OR batchId IN (SELECT id FROM card_batches WHERE categoryName = :categoryName)) ORDER BY id ASC LIMIT :limit")
    suspend fun getAvailableCardsByCategory(categoryName: String, limit: Int): List<CardEntity>

    @Query("UPDATE cards SET status = 'SOLD', soldAt = :timestamp, invoiceId = :invoiceId WHERE id IN (:cardIds)")
    suspend fun markCardsSoldForInvoice(cardIds: List<Long>, invoiceId: Long, timestamp: Long)

    @Query("UPDATE cards SET status = 'SOLD', soldAt = :timestamp WHERE id = :cardId")
    suspend fun markCardSold(cardId: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM cards WHERE status = 'AVAILABLE'")
    fun countAvailableCards(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE status = 'DISTRIBUTED'")
    fun countDistributedCards(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE status = 'SOLD'")
    fun countSoldCards(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE createdAt BETWEEN :startDate AND :endDate")
    suspend fun getGeneratedCardsCount(startDate: Long, endDate: Long): Int

    @Query("SELECT COUNT(*) FROM cards WHERE status = 'SOLD' AND soldAt BETWEEN :startDate AND :endDate")
    suspend fun getSoldCardsCount(startDate: Long, endDate: Long): Int

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("DELETE FROM card_batches")
    suspend fun deleteAllBatches()
}
