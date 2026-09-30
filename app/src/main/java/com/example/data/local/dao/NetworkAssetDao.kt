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

    @Query("SELECT * FROM network_assets WHERE category = :category ORDER BY purchaseDateMillis DESC")
    fun getAssetsByCategory(category: String): Flow<List<NetworkAssetEntity>>

    @Query("SELECT SUM(purchaseCost) FROM network_assets")
    fun getTotalPurchaseCost(): Flow<Double?>

    @Query("SELECT SUM(estimatedCurrentValue) FROM network_assets WHERE status != 'DEPRECATED'")
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
