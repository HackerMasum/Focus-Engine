package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BrainDumpEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAtMillis DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE quadrant = :quadrant ORDER BY isCompleted ASC, createdAtMillis DESC")
    fun getTasksByQuadrant(quadrant: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET completedPomodoros = completedPomodoros + 1 WHERE id = :id")
    suspend fun incrementPomodoro(id: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY streakCount DESC, id ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY completedAtMillis DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE completedAtMillis >= :sinceMillis ORDER BY completedAtMillis DESC")
    fun getSessionsSince(sinceMillis: Long): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: FocusSessionEntity)
}

@Dao
interface BrainDumpDao {
    @Query("SELECT * FROM brain_dumps ORDER BY createdAtMillis DESC")
    fun getAllNotes(): Flow<List<BrainDumpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: BrainDumpEntity): Long

    @Delete
    suspend fun deleteNote(note: BrainDumpEntity)

    @Query("DELETE FROM brain_dumps WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}
