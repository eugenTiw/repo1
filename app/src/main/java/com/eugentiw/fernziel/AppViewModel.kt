package com.eugentiw.fernziel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eugentiw.fernziel.data.Db
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.reminders.Reminders
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = Db(app)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // Anzeige-Einstellung, lokal im privaten App-Speicher abgelegt.
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _hideDone = MutableStateFlow(prefs.getBoolean("hide_done", false))
    val hideDone: StateFlow<Boolean> = _hideDone.asStateFlow()

    // Eingeklappte Abschnitte der Heute-Seite ("week", "month").
    private val _collapsed = MutableStateFlow<Set<String>>(
        HashSet(prefs.getStringSet("collapsed", emptySet()) ?: emptySet())
    )
    val collapsed: StateFlow<Set<String>> = _collapsed.asStateFlow()

    fun toggleCollapsed(key: String) {
        val current = _collapsed.value
        val next = if (current.contains(key)) current - key else current + key
        _collapsed.value = next
        prefs.edit().putStringSet("collapsed", HashSet(next)).apply()
    }

    init {
        refresh()
    }

    /** Lädt den Zustand neu, z. B. wenn die App nach Mitternacht wieder geöffnet wird. */
    fun refresh() {
        change { }
    }

    /** Blendet Erledigtes aus oder wieder ein. */
    fun toggleHideDone() {
        val next = !_hideDone.value
        _hideDone.value = next
        prefs.edit().putBoolean("hide_done", next).apply()
    }

    fun toggleDone(actionId: Long, day: Long) {
        change { toggleRef(Db.COMPLETIONS, actionId, day) }
    }

    fun togglePlan(actionId: Long, day: Long) {
        change { toggleRef(Db.PLANS, actionId, day) }
    }

    fun saveGoal(goal: Goal, actions: List<GoalAction>) {
        change { writeGoal(goal, actions) }
    }

    fun deleteGoal(goalId: Long) {
        change { removeGoal(goalId) }
    }

    fun setAchieved(goalId: Long, achieved: Boolean) {
        change { markAchieved(goalId, if (achieved) LocalDate.now().toEpochDay() else null) }
    }

    /** Legt ein eigenes To-do für einen Tag an, unabhängig von Zielen. */
    fun addTodo(title: String, day: Long) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        change { insertTodo(clean, day) }
    }

    fun toggleTodo(todoId: Long) {
        change { flipTodo(todoId) }
    }

    fun deleteTodo(todoId: Long) {
        change { removeTodo(todoId) }
    }

    /** Führt eine Änderung aus und lädt danach den Zustand neu, streng nacheinander. */
    private fun change(block: Db.() -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                _state.value = withContext(Dispatchers.IO) {
                    db.block()
                    val next = load()
                    Reminders.sync(getApplication<Application>(), next.activeActions.map { it.first })
                    next
                }
            }
        }
    }

    private fun load(): AppState {
        val today = LocalDate.now().toEpochDay()
        val probe = AppState(today = today)
        val from = minOf(probe.weekStart, probe.monthStart)
        val to = maxOf(probe.weekEnd, probe.monthEnd)
        return AppState(
            loaded = true,
            today = today,
            goals = db.loadGoals(),
            actions = db.loadActions(),
            completions = db.loadAllRefs(Db.COMPLETIONS),
            plans = db.loadRefs(Db.PLANS, from, to),
            todos = db.loadTodos(),
        )
    }
}
