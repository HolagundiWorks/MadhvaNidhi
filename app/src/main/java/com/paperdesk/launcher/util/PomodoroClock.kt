package com.paperdesk.launcher.util

import android.os.Handler
import android.os.Looper

enum class PomodoroPhase { FOCUS, SHORT_BREAK, LONG_BREAK }

object PomodoroClock {
    const val MAX_RING_MS = 60 * 60 * 1000L

    var focusMs: Long = 25 * 60 * 1000L
    var shortBreakMs: Long = 5 * 60 * 1000L
    var longBreakMs: Long = 15 * 60 * 1000L
    var cyclesUntilLong: Int = 4
    var autoBreak: Boolean = true
    var autoNext: Boolean = false

    var phase: PomodoroPhase = PomodoroPhase.FOCUS
        private set
    var durationMs: Long = focusMs
        private set
    var remainingMs: Long = focusMs
        private set
    var running: Boolean = false
        private set
    var completedCycles: Int = 0
        private set
    var justFinished: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<() -> Unit>()

    private val tick = object : Runnable {
        override fun run() {
            if (running) {
                remainingMs = (remainingMs - 200L).coerceAtLeast(0L)
                if (remainingMs == 0L) {
                    running = false
                    justFinished = true
                    onSessionComplete()
                }
                notifyListeners()
            }
            if (running) handler.postDelayed(this, 200)
        }
    }

    fun addListener(listener: () -> Unit) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun toggle() {
        justFinished = false
        if (remainingMs <= 0L) remainingMs = durationMs
        running = !running
        handler.removeCallbacks(tick)
        if (running) handler.post(tick)
        notifyListeners()
    }

    fun reset() {
        running = false
        handler.removeCallbacks(tick)
        phase = PomodoroPhase.FOCUS
        durationMs = focusMs
        remainingMs = focusMs
        justFinished = false
        notifyListeners()
    }

    fun setFocusMinutes(minutes: Int) {
        focusMs = minutes.coerceIn(5, 60) * 60_000L
        if (phase == PomodoroPhase.FOCUS && !running) {
            durationMs = focusMs
            remainingMs = focusMs
            notifyListeners()
        }
    }

    fun setDurationFromRing(ms: Long) {
        if (phase != PomodoroPhase.FOCUS) return
        focusMs = ms.coerceIn(5 * 60_000L, MAX_RING_MS)
        durationMs = focusMs
        remainingMs = focusMs
        running = false
        handler.removeCallbacks(tick)
        notifyListeners()
    }

    fun statusLabel(): String = when {
        remainingMs == 0L && justFinished -> "COMPLETE"
        running && phase == PomodoroPhase.FOCUS -> "FOCUS"
        running && phase != PomodoroPhase.FOCUS -> "BREAK"
        !running && remainingMs < durationMs && remainingMs > 0L -> "PAUSED"
        phase == PomodoroPhase.SHORT_BREAK -> "SHORT BREAK"
        phase == PomodoroPhase.LONG_BREAK -> "LONG BREAK"
        else -> "POMODORO"
    }

    private fun onSessionComplete() {
        if (phase == PomodoroPhase.FOCUS) {
            completedCycles++
            if (autoBreak) {
                val long = completedCycles % cyclesUntilLong == 0
                phase = if (long) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
                durationMs = if (long) longBreakMs else shortBreakMs
                remainingMs = durationMs
                if (autoBreak) {
                    running = true
                    handler.post(tick)
                }
            }
        } else {
            phase = PomodoroPhase.FOCUS
            durationMs = focusMs
            remainingMs = focusMs
            if (autoNext) {
                running = true
                handler.post(tick)
            }
        }
    }

    private fun notifyListeners() {
        listeners.toList().forEach { it.invoke() }
    }
}
