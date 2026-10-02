package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY id DESC")
    fun getAllInventoryItems(): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE packageName = :packageName LIMIT 1")
    suspend fun getItemByPackageName(packageName: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE TRIM(packageName) = TRIM(:packageName) LIMIT 1")
    suspend fun getItemByPackageNameTrimmed(packageName: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items ORDER BY id DESC")
    suspend fun getAllItemsList(): List<InventoryItemEntity>

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAllItems()
}
