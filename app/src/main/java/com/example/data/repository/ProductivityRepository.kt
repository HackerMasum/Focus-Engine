package com.example.data.repository

import com.example.data.local.BrainDumpDao
import com.example.data.local.FocusSessionDao
import com.example.data.local.HabitDao
import com.example.data.local.TaskDao
import com.example.data.model.BrainDumpEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProductivityRepository(
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val focusSessionDao: FocusSessionDao,
    private val brainDumpDao: BrainDumpDao
) {
    // Tasks
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTasksByQuadrant(quadrant: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByQuadrant(quadrant)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun incrementPomodoro(taskId: Long) = taskDao.incrementPomodoro(taskId)

    // Habits
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()

    suspend fun insertHabit(habit: HabitEntity): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)

    suspend fun toggleHabitToday(habit: HabitEntity) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val dates = habit.completedDates.split(",").filter { it.isNotBlank() }.toMutableList()

        val isCurrentlyCompleted = dates.contains(todayStr)
        val newDates: List<String>
        var newStreak = habit.streakCount

        if (isCurrentlyCompleted) {
            dates.remove(todayStr)
            newDates = dates
            newStreak = (newStreak - 1).coerceAtLeast(0)
        } else {
            dates.add(todayStr)
            newDates = dates
            newStreak += 1
        }

        val bestStreak = maxOf(habit.bestStreak, newStreak)
        val updatedHabit = habit.copy(
            completedDates = newDates.joinToString(","),
            streakCount = newStreak,
            bestStreak = bestStreak
        )
        habitDao.updateHabit(updatedHabit)
    }

    // Focus Sessions
    val allSessions: Flow<List<FocusSessionEntity>> = focusSessionDao.getAllSessions()

    fun getSessionsPastDays(days: Int): Flow<List<FocusSessionEntity>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return focusSessionDao.getSessionsSince(cal.timeInMillis)
    }

    suspend fun recordFocusSession(
        durationMinutes: Int,
        sessionType: String,
        taskId: Long? = null,
        taskTitle: String? = null,
        notes: String = ""
    ): Long {
        if (taskId != null) {
            taskDao.incrementPomodoro(taskId)
        }
        return focusSessionDao.insertSession(
            FocusSessionEntity(
                durationMinutes = durationMinutes,
                sessionType = sessionType,
                taskId = taskId,
                taskTitle = taskTitle,
                notes = notes,
                completedAtMillis = System.currentTimeMillis()
            )
        )
    }

    // Brain Dump
    val allNotes: Flow<List<BrainDumpEntity>> = brainDumpDao.getAllNotes()

    suspend fun insertNote(content: String): Long =
        brainDumpDao.insertNote(BrainDumpEntity(content = content.trim()))

    suspend fun deleteNoteById(id: Long) = brainDumpDao.deleteNoteById(id)

    suspend fun convertNoteToTask(noteId: Long, content: String, quadrant: String) {
        taskDao.insertTask(
            TaskEntity(
                title = content,
                quadrant = quadrant,
                category = "Captured"
            )
        )
        brainDumpDao.deleteNoteById(noteId)
    }
}
