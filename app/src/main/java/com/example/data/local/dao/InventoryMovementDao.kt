package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.InventoryMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryMovementDao {
    @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<InventoryMovementEntity>>

    @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC")
    suspend fun getAllMovementsList(): List<InventoryMovementEntity>

    @Query("SELECT COUNT(*) FROM inventory_movements WHERE (referenceNumber = :refNum OR referenceNumber = 'SALE-' || :refNum) AND movementType = 'SALE'")
    suspend fun hasSaleMovementForInvoice(refNum: String): Int

    @Query("SELECT COUNT(*) FROM inventory_movements WHERE (referenceNumber = :refNum OR referenceNumber = 'REFUND-' || :refNum OR referenceNumber = 'CANCEL-' || :refNum) AND movementType = 'RETURN'")
    suspend fun hasReturnMovementForInvoice(refNum: String): Int

    @Query("SELECT * FROM inventory_movements WHERE referenceNumber = :refNum")
    suspend fun getMovementsByReference(refNum: String): List<InventoryMovementEntity>

    @Query("DELETE FROM inventory_movements WHERE referenceNumber = :refNum")
    suspend fun deleteMovementsByReference(refNum: String)

    @Query("SELECT * FROM inventory_movements WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getMovementsForPackage(packageName: String): Flow<List<InventoryMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: InventoryMovementEntity): Long

    @Query("DELETE FROM inventory_movements")
    suspend fun deleteAllMovements()
}
