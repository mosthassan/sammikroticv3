package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NetworkAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkAssetDao {
    @Query("SELECT * FROM network_assets ORDER BY purchaseDateMillis DESC")
    fun getAllAssets(): Flow<List<NetworkAssetEntity>>

    @Query("SELECT * FROM network_assets WHERE status = 'ACTIVE' ORDER BY id ASC")
    suspend fun getActiveAssetsList(): List<NetworkAssetEntity>

    @Query("SELECT * FROM network_assets ORDER BY id ASC")
    suspend fun getAllAssetsList(): List<NetworkAssetEntity>

    @Query("SELECT * FROM network_assets WHERE category = :category ORDER BY purchaseDateMillis DESC")
    fun getAssetsByCategory(category: String): Flow<List<NetworkAssetEntity>>

    @Query("SELECT SUM(CAST(purchaseCost AS REAL)) FROM network_assets")
    fun getTotalPurchaseCost(): Flow<Double?>

    @Query("SELECT SUM(CAST(estimatedCurrentValue AS REAL)) FROM network_assets WHERE status != 'DEPRECATED'")
    fun getTotalCurrentAssetValue(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM network_assets")
    fun getAssetsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: NetworkAssetEntity): Long

    @Update
    suspend fun updateAsset(asset: NetworkAssetEntity)

    @Delete
    suspend fun deleteAsset(asset: NetworkAssetEntity)

    @Query("DELETE FROM network_assets")
    suspend fun deleteAllAssets()
}
