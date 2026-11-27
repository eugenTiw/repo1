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

    // Kurzer Hinweis nach einem Level-Aufstieg; verschwindet von selbst.
    private val _levelUp = MutableStateFlow<LevelUp?>(null)
    val levelUp: StateFlow<LevelUp?> = _levelUp.asStateFlow()

    fun dismissLevelUp() {
        _levelUp.value = null
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

    /** Schaltet „erledigt" für einen Tag um (Tageskreise der Wochen- und Monatshandlungen). */
    fun toggleDone(actionId: Long, day: Long) {
        change(celebrate = true) { toggleDone(actionId, day) }
    }

    /** Schaltet die Bewertung weiter: offen → erledigt (Haken) → nicht geschafft (Kreuz) → offen. */
    fun cycleMark(actionId: Long, day: Long) {
        val current = _state.value
        val action = current.actions.firstOrNull { it.id == actionId } ?: return
        val missedWhenOpen = current.missedWhenOpen(action, day)
        change(celebrate = true) { cycleMark(actionId, day, missedWhenOpen) }
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

    /**
     * Führt eine Änderung aus und lädt danach den Zustand neu, streng nacheinander.
     * Mit [celebrate] wird geprüft, ob die Änderung zu einem Level-Aufstieg geführt hat.
     */
    private fun change(celebrate: Boolean = false, block: Db.() -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                val before = _state.value
                val next = withContext(Dispatchers.IO) {
                    db.block()
                    val loaded = load()
                    Reminders.sync(getApplication<Application>(), loaded.activeActions.map { it.first })
                    loaded
                }
                _state.value = next
                if (celebrate && before.loaded) {
                    val up = findLevelUp(before, next)
                    if (up != null) _levelUp.value = up
                }
            }
        }
    }

    /** Die erste Handlung, die zwischen [before] und [after] ein Level aufgestiegen ist. */
    private fun findLevelUp(before: AppState, after: AppState): LevelUp? {
        for (pair in after.activeActions) {
            val action = pair.first
            val now = after.levelProgressOf(action) ?: continue
            val old = before.actions.firstOrNull { it.id == action.id }?.let { before.levelProgressOf(it) } ?: continue
            if (now.index > old.index) {
                return LevelUp(action.id, now.index + 1, now.total, levelTitle(action, now.index), action.period)
            }
        }
        return null
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
            failures = db.loadAllRefs(Db.FAILURES),
            plans = db.loadRefs(Db.PLANS, from, to),
            todos = db.loadTodos(),
        )
    }
}
