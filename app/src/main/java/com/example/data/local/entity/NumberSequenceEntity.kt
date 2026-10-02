package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول الترقيم التسلسلي الآمن والمتزامن (Number Sequences)
 * يضمن توليد أرقام متسلسلة غير قابلة للتكرار أو التصادم (Gapless & Thread-Safe)
 * يمنع الفقدان الصامت للبيانات الناتج عن الأرقام العشوائية أو REPLACE cascade
 */
@Entity(tableName = "number_sequences")
data class NumberSequenceEntity(
    @PrimaryKey
    val sequenceKey: String, // e.g. "JE_2026", "REC_2026", "PAY_2026", "INV_2026", "PUR_2026"
    val sequenceType: String,
    val year: Int,
    val lastValue: Long
)
