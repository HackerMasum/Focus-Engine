package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val durationMinutes: Int,
    val sessionType: String = "FOCUS", // FOCUS, SHORT_BREAK, LONG_BREAK
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val notes: String = "",
    val completedAtMillis: Long = System.currentTimeMillis()
)
