package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.NumberSequenceEntity
import java.util.Locale

@Dao
interface NumberSequenceDao {
    @Query("SELECT * FROM number_sequences WHERE sequenceKey = :key LIMIT 1")
    suspend fun getSequence(key: String): NumberSequenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(sequence: NumberSequenceEntity)

    @Transaction
    suspend fun getNextNumber(type: String, year: Int, prefix: String = type, padding: Int = 4): String {
        val key = "${type}_$year"
        val current = getSequence(key)
        val nextVal = (current?.lastValue ?: 0L) + 1L
        insertOrUpdate(
            NumberSequenceEntity(
                sequenceKey = key,
                sequenceType = type,
                year = year,
                lastValue = nextVal
            )
        )
        return "$prefix-$year-${String.format(Locale.US, "%0${padding}d", nextVal)}"
    }

    @Transaction
    suspend fun seedInitialSequenceIfEmpty(type: String, year: Int, initialValue: Long) {
        val key = "${type}_$year"
        val existing = getSequence(key)
        if (existing == null || existing.lastValue < initialValue) {
            insertOrUpdate(
                NumberSequenceEntity(
                    sequenceKey = key,
                    sequenceType = type,
                    year = year,
                    lastValue = initialValue
                )
            )
        }
    }
}
