package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CurrencyRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyRateDao {
    @Query("SELECT * FROM currency_rates")
    fun getAllRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates WHERE currencyCode = :code LIMIT 1")
    suspend fun getRateForCurrency(code: String): CurrencyRateEntity?

    @Query("SELECT * FROM currency_rates WHERE currencyCode = :code")
    fun getRateFlowForCurrency(code: String): Flow<CurrencyRateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRate(rate: CurrencyRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRates(rates: List<CurrencyRateEntity>)

    @Query("DELETE FROM currency_rates")
    suspend fun deleteAllRates()
}
