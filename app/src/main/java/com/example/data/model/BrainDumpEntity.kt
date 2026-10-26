package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brain_dumps")
data class BrainDumpEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false
)
