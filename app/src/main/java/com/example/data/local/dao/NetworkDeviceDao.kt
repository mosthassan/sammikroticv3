package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NetworkDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDeviceDao {
    @Query("SELECT * FROM network_devices ORDER BY id DESC")
    fun getAllDevices(): Flow<List<NetworkDeviceEntity>>

    @Query("SELECT * FROM network_devices WHERE ipAddress = :ip LIMIT 1")
    suspend fun getDeviceByIp(ip: String): NetworkDeviceEntity?

    @Query("SELECT * FROM network_devices WHERE TRIM(ipAddress) = TRIM(:ip)")
    suspend fun findDevicesWithIp(ip: String): List<NetworkDeviceEntity>

    @Query("SELECT * FROM network_devices WHERE id = :id")
    suspend fun getDeviceById(id: Long): NetworkDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: NetworkDeviceEntity): Long

    @Update
    suspend fun updateDevice(device: NetworkDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: NetworkDeviceEntity)

    @Query("SELECT COUNT(*) FROM network_devices")
    fun getDeviceCount(): Flow<Int>

    @Query("DELETE FROM network_devices")
    suspend fun deleteAllDevices()
}
