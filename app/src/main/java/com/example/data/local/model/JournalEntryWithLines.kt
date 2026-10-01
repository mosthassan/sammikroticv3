package com.example.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.JournalEntryHeaderEntity
import com.example.data.local.entity.JournalEntryLineEntity

data class JournalEntryWithLines(
    @Embedded
    val header: JournalEntryHeaderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "headerId"
    )
    val lines: List<JournalEntryLineEntity>
)
