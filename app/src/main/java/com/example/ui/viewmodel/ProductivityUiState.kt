package com.example.ui.viewmodel

import com.example.data.model.BrainDumpEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.TaskEntity
import com.example.sound.SoundscapeType

enum class TimerMode(val title: String, val focusMinutes: Int, val breakMinutes: Int) {
    POMODORO("Pomodoro", 25, 5),
    DEEP_WORK("Deep Work", 50, 10),
    SPRINT("Sprint", 15, 3),
    CUSTOM("Custom", 30, 5)
}

enum class TaskFilter(val label: String) {
    ALL("All Tasks"),
    Q1("Do First (Q1)"),
    Q2("Schedule (Q2)"),
    Q3("Delegate (Q3)"),
    Q4("Eliminate (Q4)"),
    COMPLETED("Completed")
}

data class DayFocusStat(
    val dayLabel: String,
    val dateString: String,
    val focusMinutes: Int
)

data class ProductivityUiState(
    // Timer State
    val timerMode: TimerMode = TimerMode.POMODORO,
    val currentSessionDurationSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isTimerRunning: Boolean = false,
    val isBreak: Boolean = false,
    val activeTaskId: Long? = null,
    val activeTaskTitle: String? = null,
    val soundscape: SoundscapeType = SoundscapeType.NONE,
    val selectedCustomSound: com.example.data.model.CustomSoundEntity? = null,
    val ambientVolume: Float = 0.5f,

    // Data lists
    val tasks: List<TaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val sessions: List<FocusSessionEntity> = emptyList(),
    val brainDumps: List<BrainDumpEntity> = emptyList(),
    val customSounds: List<com.example.data.model.CustomSoundEntity> = emptyList(),

    // Tasks screen state
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val isMatrixView: Boolean = true, // Matrix vs List

    // Analytics state
    val dailyProductivityScore: Int = 0,
    val todayFocusMinutes: Int = 0,
    val weeklyFocusStats: List<DayFocusStat> = emptyList(),
    val activeHabitStreakMax: Int = 0,
    val totalTasksCompleted: Int = 0
)
