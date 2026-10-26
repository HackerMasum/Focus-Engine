package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EisenhowerQuadrant(val code: String, val title: String, val subtitle: String) {
    Q1("Q1", "Do First", "Urgent & Important"),
    Q2("Q2", "Schedule", "Important, Not Urgent"),
    Q3("Q3", "Delegate", "Urgent, Not Important"),
    Q4("Q4", "Eliminate", "Neither");

    companion object {
        fun fromCode(code: String): EisenhowerQuadrant {
            return entries.find { it.code == code } ?: Q2
        }
    }
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val quadrant: String = EisenhowerQuadrant.Q2.code, // default to Q2 (highest value work)
    val estimatedPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    val category: String = "Work", // Work, Study, Health, Personal, Creative
    val isCompleted: Boolean = false,
    val dueDateMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null
)
