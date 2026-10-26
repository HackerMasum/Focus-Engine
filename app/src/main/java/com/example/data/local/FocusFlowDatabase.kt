package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BrainDumpEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        FocusSessionEntity::class,
        BrainDumpEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FocusFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun brainDumpDao(): BrainDumpDao

    companion object {
        @Volatile
        private var INSTANCE: FocusFlowDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FocusFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusFlowDatabase::class.java,
                    "focus_flow_database"
                )
                    .addCallback(FocusFlowDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class FocusFlowDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: FocusFlowDatabase) {
            val taskDao = database.taskDao()
            val habitDao = database.habitDao()
            val sessionDao = database.focusSessionDao()
            val brainDumpDao = database.brainDumpDao()

            val now = System.currentTimeMillis()

            // Pre-seed sample tasks showing Eisenhower Matrix value
            taskDao.insertTask(
                TaskEntity(
                    title = "Deliver product roadmap presentation",
                    description = "Finalize Q3 milestone slide deck with team KPI metrics",
                    quadrant = "Q1",
                    estimatedPomodoros = 3,
                    completedPomodoros = 1,
                    category = "Work"
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Deep Work: Core architecture refactoring",
                    description = "Eliminate technical debt and isolate domain business logic",
                    quadrant = "Q2",
                    estimatedPomodoros = 4,
                    completedPomodoros = 0,
                    category = "Development"
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Learn Kotlin Flow & Coroutines internals",
                    description = "Read official documentation on SharedFlow and StateFlow buffer strategies",
                    quadrant = "Q2",
                    estimatedPomodoros = 2,
                    completedPomodoros = 0,
                    category = "Study"
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Respond to non-critical vendor emails",
                    description = "Clear miscellaneous status inquiry pings",
                    quadrant = "Q3",
                    estimatedPomodoros = 1,
                    completedPomodoros = 0,
                    category = "Admin"
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Audit bookmarks & archive stale tabs",
                    description = "Declutter browser workspace for zero distraction",
                    quadrant = "Q4",
                    estimatedPomodoros = 1,
                    completedPomodoros = 0,
                    category = "Personal"
                )
            )

            // Pre-seed habits with some completed days for instant visual satisfaction
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val d0 = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d1 = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d2 = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d3 = sdf.format(cal.time)

            habitDao.insertHabit(
                HabitEntity(
                    title = "Morning Deep Focus (90 mins)",
                    category = "Focus",
                    targetDaysPerWeek = 5,
                    streakCount = 4,
                    bestStreak = 12,
                    completedDates = "$d3,$d2,$d1,$d0",
                    colorHex = "#6366F1",
                    iconName = "bolt"
                )
            )
            habitDao.insertHabit(
                HabitEntity(
                    title = "Daily Reading & Growth (20 pages)",
                    category = "Mindset",
                    targetDaysPerWeek = 7,
                    streakCount = 3,
                    bestStreak = 8,
                    completedDates = "$d3,$d2,$d1",
                    colorHex = "#10B981",
                    iconName = "book"
                )
            )
            habitDao.insertHabit(
                HabitEntity(
                    title = "Evening Plan: Top 3 for tomorrow",
                    category = "Organization",
                    targetDaysPerWeek = 5,
                    streakCount = 5,
                    bestStreak = 15,
                    completedDates = "$d3,$d2,$d1",
                    colorHex = "#F59E0B",
                    iconName = "checklist"
                )
            )

            // Seed initial completed focus sessions for analytics
            sessionDao.insertSession(
                FocusSessionEntity(
                    durationMinutes = 25,
                    sessionType = "FOCUS",
                    taskTitle = "Deliver product roadmap presentation",
                    completedAtMillis = now - 3600000 * 2
                )
            )
            sessionDao.insertSession(
                FocusSessionEntity(
                    durationMinutes = 50,
                    sessionType = "FOCUS",
                    taskTitle = "Deep Work: Core architecture refactoring",
                    completedAtMillis = now - 3600000 * 26
                )
            )
            sessionDao.insertSession(
                FocusSessionEntity(
                    durationMinutes = 25,
                    sessionType = "FOCUS",
                    taskTitle = "Audit bookmarks & archive stale tabs",
                    completedAtMillis = now - 3600000 * 50
                )
            )

            // Seed brain dump thought
            brainDumpDao.insertNote(
                BrainDumpEntity(
                    content = "Check if team needs new API tokens for CI build pipeline",
                    createdAtMillis = now - 1800000
                )
            )
        }
    }
}
