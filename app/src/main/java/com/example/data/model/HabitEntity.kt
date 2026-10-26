package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "Productivity",
    val targetDaysPerWeek: Int = 7,
    val streakCount: Int = 0,
    val bestStreak: Int = 0,
    val completedDates: String = "", // Comma-separated "yyyy-MM-dd"
    val colorHex: String = "#6366F1",
    val iconName: String = "bolt", // bolt, book, fitness, code, water, meditation
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun isCompletedOn(dateString: String): Boolean {
        if (completedDates.isBlank()) return false
        val dates = completedDates.split(",")
        return dates.contains(dateString)
    }

    fun isCompletedToday(): Boolean {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return isCompletedOn(todayStr)
    }
}
