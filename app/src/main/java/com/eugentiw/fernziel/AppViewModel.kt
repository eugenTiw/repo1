package com.eugentiw.fernziel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eugentiw.fernziel.data.DayRef
import com.eugentiw.fernziel.data.Db
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Ein Eintrag der To-do-Liste eines Tages. */
data class TodayItem(val action: GoalAction, val goal: Goal, val done: Boolean)

/** Vollständiger, unveränderlicher Zustand der App. */
data class AppState(
    val loaded: Boolean = false,
    val today: Long = LocalDate.now().toEpochDay(),
    val goals: List<Goal> = emptyList(),
    val actions: List<GoalAction> = emptyList(),
    val completions: Set<DayRef> = emptySet(),
    val plans: Set<DayRef> = emptySet(),
) {
    val weekStart: Long = weekStartOf(today)
    val weekEnd: Long = weekStart + 6
    val monthStart: Long = LocalDate.ofEpochDay(today).withDayOfMonth(1).toEpochDay()
    val monthEnd: Long = LocalDate.ofEpochDay(today).let { it.withDayOfMonth(it.lengthOfMonth()).toEpochDay() }

    private val goalsById: Map<Long, Goal> = goals.associateBy { it.id }

    val activeGoals: List<Goal> = goals.filter { it.achievedDay == null }
    val achievedGoals: List<Goal> = goals.filter { it.achievedDay != null }

    /** Handlungen aller noch nicht erreichten Ziele, jeweils mit ihrem Ziel. */
    val activeActions: List<Pair<GoalAction, Goal>> = actions.mapNotNull { action ->
        val goal = goalsById[action.goalId]
        if (goal != null && goal.achievedDay == null) Pair(action, goal) else null
    }

    fun actionsOf(goalId: Long): List<GoalAction> = actions.filter { it.goalId == goalId }

    fun isDone(actionId: Long, day: Long): Boolean = completions.contains(DayRef(actionId, day))

    fun isPlanned(actionId: Long, day: Long): Boolean = plans.contains(DayRef(actionId, day))

    /** Wie oft die Handlung im laufenden Zeitraum (Tag, Woche, Monat) erledigt wurde. */
    fun doneInPeriod(action: GoalAction): Int {
        val from: Long = when (action.period) {
            Period.DAILY -> today
            Period.WEEKLY -> weekStart
            Period.MONTHLY -> monthStart
        }
        val to: Long = when (action.period) {
            Period.DAILY -> today
            Period.WEEKLY -> weekEnd
            Period.MONTHLY -> monthEnd
        }
        return completions.count { it.actionId == action.id && it.day >= from && it.day <= to }
    }

    /**
     * To-dos eines Tages: tägliche Handlungen (ab ihrem Startdatum) sowie
     * Wochen- und Monatshandlungen, die für den Tag eingeplant oder an ihm erledigt wurden.
     */
    fun itemsFor(day: Long): List<TodayItem> = activeActions
        .filter { pair ->
            val action = pair.first
            if (action.period == Period.DAILY) {
                action.createdDay <= day
            } else {
                isPlanned(action.id, day) || isDone(action.id, day)
            }
        }
        .map { pair -> TodayItem(pair.first, pair.second, isDone(pair.first.id, day)) }

    /** Alle Tage, an denen die Handlung erledigt wurde. */
    fun doneDaysOf(actionId: Long): Set<Long> {
        val out = HashSet<Long>()
        for (ref in completions) {
            if (ref.actionId == actionId) out.add(ref.day)
        }
        return out
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = Db(app)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // Anzeige-Einstellung, lokal im privaten App-Speicher abgelegt.
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _hideDone = MutableStateFlow(prefs.getBoolean("hide_done", false))
    val hideDone: StateFlow<Boolean> = _hideDone.asStateFlow()

    init {
        refresh()
    }

    /** Lädt den Zustand neu, z. B. wenn die App nach Mitternacht wieder geöffnet wird. */
    fun refresh() {
        change { }
    }

    /** Blendet erledigte To-dos aus oder wieder ein. */
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

    /** Führt eine Änderung aus und lädt danach den Zustand neu, streng nacheinander. */
    private fun change(block: Db.() -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                _state.value = withContext(Dispatchers.IO) {
                    db.block()
                    load()
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
        )
    }
}
