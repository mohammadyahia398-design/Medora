package com.example.medora.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Pomodoro timer. Lives in a ViewModel so it keeps running while the user switches tabs.
// Times use the real clock (end time), so a slow frame never makes the timer drift.
class FocusViewModel : ViewModel() {
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

    fun toggle() = if (running) pause() else start()

    private fun start() {
        endAt = System.currentTimeMillis() + remainingMs
        running = true
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (running) {
                val left = endAt - System.currentTimeMillis()
                if (left <= 0L) {
                    finishPhase()
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

    // Focus done: count it and offer a short break. Break done (or skipped): back to focus.
    fun finishPhase() {
        running = false
        ticker?.cancel()
        if (phase == Phase.Focus) {
            sessionsDone++
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
