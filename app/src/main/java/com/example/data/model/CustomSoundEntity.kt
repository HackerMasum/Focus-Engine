package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_sounds")
data class CustomSoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationSeconds: Int = 0,
    val dateAddedMillis: Long = System.currentTimeMillis()
)
