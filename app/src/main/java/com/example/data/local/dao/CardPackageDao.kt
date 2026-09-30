package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CardPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardPackageDao {
    @Query("SELECT * FROM card_packages ORDER BY retailPrice ASC, id ASC")
    fun getAllPackages(): Flow<List<CardPackageEntity>>

    @Query("SELECT * FROM card_packages WHERE id = :id")
    suspend fun getPackageById(id: Long): CardPackageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: CardPackageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(packages: List<CardPackageEntity>)

    @Update
    suspend fun updatePackage(pkg: CardPackageEntity)

    @Delete
    suspend fun deletePackage(pkg: CardPackageEntity)

    @Query("DELETE FROM card_packages")
    suspend fun deleteAllPackages()
}
