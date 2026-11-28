package com.example.medora.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.medora.data.MedoraDatabase
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// A planner task waiting to be focused on. The planner sets it, the Focus screen takes it.
object FocusLink {
    var pending by mutableStateOf<PendingFocus?>(null)
}

class PendingFocus(val taskId: Long, val title: String, val minutes: Int)

// Pomodoro timer. Lives in a ViewModel so it keeps running while the user switches tabs.
// Times use the real clock (end time), so a slow frame never makes the timer drift.
class FocusViewModel(app: Application) : AndroidViewModel(app) {
    private val tasks = MedoraDatabase.get(app).taskDao()

    enum class Phase { Focus, Break }

    var phase by mutableStateOf(Phase.Focus)
        private set
    var focusMinutes by mutableIntStateOf(DEFAULT_FOCUS)
        private set
    var running by mutableStateOf(false)
        private set
    var remainingMs by mutableLongStateOf(DEFAULT_FOCUS * MINUTE)
        private set
    var sessionsDone by mutableIntStateOf(0)
        private set

    // The planner task this focus is for (0 = none). Finished focus time is added to it.
    var taskId by mutableLongStateOf(0L)
        private set
    var taskTitle by mutableStateOf("")
        private set

    private var endAt = 0L
    private var ticker: Job? = null

    val totalMs: Long
        get() = (if (phase == Phase.Focus) focusMinutes else BREAK_MINUTES) * MINUTE

    // True before the first start of this phase (the dial can still be changed).
    val idle: Boolean
        get() = !running && remainingMs == totalMs

    fun setMinutes(m: Int) {
        if (!idle || phase != Phase.Focus) return
        val v = m.coerceIn(MIN_FOCUS, MAX_FOCUS)
        focusMinutes = v
        remainingMs = v * MINUTE
    }

    fun attachTask(p: PendingFocus) {
        reset()
        taskId = p.taskId
        taskTitle = p.title
        setMinutes(p.minutes)
    }

    fun detachTask() {
        taskId = 0L
        taskTitle = ""
    }

    fun toggle() = if (running) pause() else start()

    private fun start() {
        endAt = System.currentTimeMillis() + remainingMs
        running = true
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (running) {
                val left = endAt - System.currentTimeMillis()
                if (left <= 0L) {
                    complete(natural = true)
                    break
                }
                remainingMs = left
                delay(TICK_MS)
            }
        }
    }

    private fun pause() {
        running = false
        ticker?.cancel()
        remainingMs = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun reset() {
        running = false
        ticker?.cancel()
        phase = Phase.Focus
        remainingMs = focusMinutes * MINUTE
    }

    // Skip button: moves on without counting the focus time.
    fun skip() = complete(natural = false)

    // Focus done: count it and offer a short break. Break done (or skipped): back to focus.
    private fun complete(natural: Boolean) {
        running = false
        ticker?.cancel()
        if (phase == Phase.Focus) {
            if (natural) {
                sessionsDone++
                val id = taskId
                val m = focusMinutes
                if (id != 0L) viewModelScope.launch { tasks.addFocus(id, m) }
            }
            phase = Phase.Break
            remainingMs = BREAK_MINUTES * MINUTE
        } else {
            phase = Phase.Focus
            remainingMs = focusMinutes * MINUTE
        }
    }

    companion object {
        const val MIN_FOCUS = 5
        const val MAX_FOCUS = 60
        const val STEP = 5
        const val DEFAULT_FOCUS = 25
        const val BREAK_MINUTES = 5
        private const val MINUTE = 60_000L
        private const val TICK_MS = 250L
    }
}
