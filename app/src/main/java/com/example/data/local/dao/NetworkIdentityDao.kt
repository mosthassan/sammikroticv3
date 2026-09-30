package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NetworkIdentityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkIdentityDao {
    @Query("SELECT * FROM network_identity WHERE id = 1 LIMIT 1")
    fun getNetworkIdentityFlow(): Flow<NetworkIdentityEntity?>

    @Query("SELECT * FROM network_identity WHERE id = 1 LIMIT 1")
    suspend fun getNetworkIdentity(): NetworkIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(identity: NetworkIdentityEntity): Long

    @Query("DELETE FROM network_identity")
    suspend fun deleteNetworkIdentity()
}
