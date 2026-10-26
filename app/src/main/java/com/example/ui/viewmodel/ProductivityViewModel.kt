package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FocusFlowDatabase
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.ProductivityRepository
import com.example.sound.AmbientSoundGenerator
import com.example.sound.SoundscapeType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProductivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductivityRepository
    private val soundGenerator = AmbientSoundGenerator()

    private val _uiState = MutableStateFlow(ProductivityUiState())
    val uiState: StateFlow<ProductivityUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        val database = FocusFlowDatabase.getDatabase(application, viewModelScope)
        repository = ProductivityRepository(
            taskDao = database.taskDao(),
            habitDao = database.habitDao(),
            focusSessionDao = database.focusSessionDao(),
            brainDumpDao = database.brainDumpDao()
        )

        observeData()
    }

    private fun observeData() {
        // Observe Tasks
        viewModelScope.launch {
            repository.allTasks.collectLatest { taskList ->
                _uiState.update { current ->
                    val completedCount = taskList.count { it.isCompleted }
                    current.copy(
                        tasks = taskList,
                        totalTasksCompleted = completedCount
                    )
                }
                recalculateAnalytics()
            }
        }

        // Observe Habits
        viewModelScope.launch {
            repository.allHabits.collectLatest { habitList ->
                _uiState.update { current ->
                    val maxStreak = habitList.maxOfOrNull { it.streakCount } ?: 0
                    current.copy(
                        habits = habitList,
                        activeHabitStreakMax = maxStreak
                    )
                }
                recalculateAnalytics()
            }
        }

        // Observe Sessions
        viewModelScope.launch {
            repository.allSessions.collectLatest { sessionList ->
                _uiState.update { current ->
                    current.copy(sessions = sessionList)
                }
                recalculateAnalytics()
            }
        }

        // Observe Brain Dumps
        viewModelScope.launch {
            repository.allNotes.collectLatest { notesList ->
                _uiState.update { current ->
                    current.copy(brainDumps = notesList)
                }
            }
        }
    }

    // --- Timer Actions ---

    fun startTimer() {
        if (_uiState.value.isTimerRunning) return

        _uiState.update { it.copy(isTimerRunning = true) }
        triggerHaptic(50)

        // If a soundscape is selected, start audio
        if (_uiState.value.soundscape != SoundscapeType.NONE) {
            soundGenerator.setSoundscape(_uiState.value.soundscape)
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.remainingSeconds > 0) {
                delay(1000L)
                _uiState.update { current ->
                    val nextSeconds = current.remainingSeconds - 1
                    current.copy(remainingSeconds = nextSeconds)
                }
            }

            if (_uiState.value.remainingSeconds <= 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isTimerRunning = false) }
        soundGenerator.stop()
        triggerHaptic(30)
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        soundGenerator.stop()

        val mode = _uiState.value.timerMode
        val durationSec = if (_uiState.value.isBreak) {
            mode.breakMinutes * 60
        } else {
            mode.focusMinutes * 60
        }

        _uiState.update {
            it.copy(
                isTimerRunning = false,
                currentSessionDurationSeconds = durationSec,
                remainingSeconds = durationSec
            )
        }
    }

    fun skipSession() {
        timerJob?.cancel()
        timerJob = null
        soundGenerator.stop()

        val nextIsBreak = !_uiState.value.isBreak
        val mode = _uiState.value.timerMode
        val nextDurationSec = if (nextIsBreak) mode.breakMinutes * 60 else mode.focusMinutes * 60

        _uiState.update {
            it.copy(
                isTimerRunning = false,
                isBreak = nextIsBreak,
                currentSessionDurationSeconds = nextDurationSec,
                remainingSeconds = nextDurationSec
            )
        }
    }

    private fun onTimerFinished() {
        soundGenerator.stop()
        triggerLongCelebrationHaptic()

        val isBreakFinished = _uiState.value.isBreak
        val mode = _uiState.value.timerMode
        val activeTask = _uiState.value.tasks.find { it.id == _uiState.value.activeTaskId }

        if (!isBreakFinished) {
            // Record completed focus session
            val focusDuration = mode.focusMinutes
            viewModelScope.launch {
                repository.recordFocusSession(
                    durationMinutes = focusDuration,
                    sessionType = "FOCUS",
                    taskId = activeTask?.id,
                    taskTitle = activeTask?.title
                )
            }
        }

        // Switch to break or focus
        val nextIsBreak = !isBreakFinished
        val nextDuration = if (nextIsBreak) mode.breakMinutes * 60 else mode.focusMinutes * 60

        _uiState.update {
            it.copy(
                isTimerRunning = false,
                isBreak = nextIsBreak,
                currentSessionDurationSeconds = nextDuration,
                remainingSeconds = nextDuration
            )
        }
    }

    fun selectTimerMode(mode: TimerMode, customMinutes: Int? = null) {
        val focusMin = customMinutes ?: mode.focusMinutes
        val durationSec = focusMin * 60
        pauseTimer()

        _uiState.update {
            it.copy(
                timerMode = mode,
                isBreak = false,
                currentSessionDurationSeconds = durationSec,
                remainingSeconds = durationSec
            )
        }
    }

    fun setActiveTask(task: TaskEntity?) {
        _uiState.update {
            it.copy(
                activeTaskId = task?.id,
                activeTaskTitle = task?.title
            )
        }
    }

    fun setSoundscape(type: SoundscapeType) {
        _uiState.update { it.copy(soundscape = type) }
        if (_uiState.value.isTimerRunning) {
            soundGenerator.setSoundscape(type)
        }
    }

    // --- Task Actions ---

    fun addTask(
        title: String,
        description: String,
        quadrant: String,
        estimatedPomodoros: Int,
        category: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    quadrant = quadrant,
                    estimatedPomodoros = estimatedPomodoros.coerceAtLeast(1),
                    category = category.ifBlank { "General" }
                )
            )
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val isNowCompleted = !task.isCompleted
            val updated = task.copy(
                isCompleted = isNowCompleted,
                completedAtMillis = if (isNowCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updated)
            if (isNowCompleted) {
                triggerHaptic(60)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            if (_uiState.value.activeTaskId == task.id) {
                setActiveTask(null)
            }
            repository.deleteTask(task)
        }
    }

    fun setFilter(filter: TaskFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleMatrixView(isMatrix: Boolean) {
        _uiState.update { it.copy(isMatrixView = isMatrix) }
    }

    // --- Habit Actions ---

    fun addHabit(
        title: String,
        category: String,
        targetDays: Int,
        colorHex: String,
        iconName: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertHabit(
                HabitEntity(
                    title = title.trim(),
                    category = category.ifBlank { "Daily" },
                    targetDaysPerWeek = targetDays,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
        }
    }

    fun toggleHabitToday(habit: HabitEntity) {
        viewModelScope.launch {
            repository.toggleHabitToday(habit)
            triggerHaptic(70)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    // --- Brain Dump Actions ---

    fun addBrainDump(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.insertNote(content)
        }
    }

    fun deleteBrainDump(id: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    fun convertBrainDumpToTask(id: Long, content: String, quadrant: String) {
        viewModelScope.launch {
            repository.convertNoteToTask(id, content, quadrant)
            triggerHaptic(50)
        }
    }

    // --- Analytics Recalculation ---

    private fun recalculateAnalytics() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)

        val sessions = _uiState.value.sessions
        val todaySessions = sessions.filter {
            sdf.format(Date(it.completedAtMillis)) == todayStr && it.sessionType == "FOCUS"
        }
        val todayFocusMinutes = todaySessions.sumOf { it.durationMinutes }

        // Past 7 days focus stats
        val dayStats = mutableListOf<DayFocusStat>()
        val daySdf = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = sdf.format(c.time)
            val label = if (i == 0) "Today" else daySdf.format(c.time)
            val dayMinutes = sessions.filter {
                sdf.format(Date(it.completedAtMillis)) == dStr && it.sessionType == "FOCUS"
            }.sumOf { it.durationMinutes }
            dayStats.add(DayFocusStat(label, dStr, dayMinutes))
        }

        // Productivity score formula:
        // Focus time (up to 40 pts for 120 mins) + Tasks done today (up to 30 pts) + Habits done today (up to 30 pts)
        val habits = _uiState.value.habits
        val habitsDoneToday = habits.count { it.isCompletedToday() }
        val habitPercent = if (habits.isNotEmpty()) habitsDoneToday.toFloat() / habits.size else 0.5f

        val focusScore = (todayFocusMinutes / 120f).coerceIn(0f, 1f) * 40
        val habitScore = habitPercent * 30
        val taskScore = (_uiState.value.totalTasksCompleted.coerceAtMost(5) / 5f) * 30
        val totalScore = (focusScore + habitScore + taskScore).toInt().coerceIn(0, 100)

        _uiState.update {
            it.copy(
                dailyProductivityScore = if (totalScore == 0) 45 else totalScore,
                todayFocusMinutes = todayFocusMinutes,
                weeklyFocusStats = dayStats
            )
        }
    }

    // --- Haptics & Feedback ---

    private fun triggerHaptic(durationMs: Long) {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun triggerLongCelebrationHaptic() {
        try {
            val context = getApplication<Application>()
            val pattern = longArrayOf(0, 150, 100, 250, 100, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            }
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundGenerator.stop()
    }
}
