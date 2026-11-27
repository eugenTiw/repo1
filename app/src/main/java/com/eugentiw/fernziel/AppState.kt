package com.eugentiw.fernziel

import com.eugentiw.fernziel.data.DayRef
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import com.eugentiw.fernziel.data.Todo
import java.time.LocalDate

/**
 * Eine Handlung in der To-do-Liste eines Tages.
 * [mark] ist die Anzeige (grüner Haken, rotes Kreuz oder offen); [failed] sagt, ob der Tag
 * ausdrücklich als nicht geschafft markiert wurde.
 */
data class TodayItem(
    val action: GoalAction,
    val goal: Goal,
    val done: Boolean,
    val failed: Boolean,
    val mark: Mark,
) {
    /** Abgeschlossen: erledigt oder ausdrücklich als nicht geschafft markiert. */
    val resolved: Boolean get() = done || failed
}

/**
 * Zeile der Wochen- oder Monatsauswertung im Verlauf.
 * [target] ist null, wenn die Handlung für diesen Zeitraum keine eigene Vorgabe hat.
 */
data class PeriodRow(val action: GoalAction, val goal: Goal, val done: Int, val target: Int?)

/** Vollständiger, unveränderlicher Zustand der App. Enthält keine Android-Abhängigkeiten. */
data class AppState(
    val loaded: Boolean = false,
    val today: Long = LocalDate.now().toEpochDay(),
    val goals: List<Goal> = emptyList(),
    val actions: List<GoalAction> = emptyList(),
    val completions: Set<DayRef> = emptySet(),
    val failures: Set<DayRef> = emptySet(),
    val plans: Set<DayRef> = emptySet(),
    val todos: List<Todo> = emptyList(),
) {
    val weekStart: Long = weekStartOf(today)
    val weekEnd: Long = weekStart + 6
    val monthStart: Long = LocalDate.ofEpochDay(today).withDayOfMonth(1).toEpochDay()
    val monthEnd: Long = LocalDate.ofEpochDay(today).let { it.withDayOfMonth(it.lengthOfMonth()).toEpochDay() }

    private val goalsById: Map<Long, Goal> = goals.associateBy { it.id }
    private val todosByDay: Map<Long, List<Todo>> = todos.groupBy { it.day }
    private val doneDaysByAction: Map<Long, Set<Long>> =
        completions.groupBy({ it.actionId }, { it.day }).mapValues { it.value.toSet() }
    private val failedDaysByAction: Map<Long, Set<Long>> =
        failures.groupBy({ it.actionId }, { it.day }).mapValues { it.value.toSet() }

    val activeGoals: List<Goal> = goals.filter { it.achievedDay == null }
    val achievedGoals: List<Goal> = goals.filter { it.achievedDay != null }

    /** Alle Handlungen mit ihrem Ziel, auch die bereits erreichter Ziele (für den Verlauf). */
    private val allActions: List<Pair<GoalAction, Goal>> = actions.mapNotNull { action ->
        val goal = goalsById[action.goalId]
        if (goal != null) Pair(action, goal) else null
    }

    /** Handlungen aller noch nicht erreichten Ziele, jeweils mit ihrem Ziel. */
    val activeActions: List<Pair<GoalAction, Goal>> = allActions.filter { it.second.achievedDay == null }

    fun actionsOf(goalId: Long): List<GoalAction> = actions.filter { it.goalId == goalId }

    fun isDone(actionId: Long, day: Long): Boolean = completions.contains(DayRef(actionId, day))

    /** Ob der Tag ausdrücklich als nicht geschafft markiert wurde. */
    fun isFailed(actionId: Long, day: Long): Boolean = failures.contains(DayRef(actionId, day))

    fun isPlanned(actionId: Long, day: Long): Boolean = plans.contains(DayRef(actionId, day))

    /**
     * Ob ein offener Tag von selbst als nicht geschafft zählt: jeder vergangene Tag einer
     * täglichen Handlung ab ihrem Startdatum.
     */
    fun missedWhenOpen(action: GoalAction, day: Long): Boolean =
        action.period == Period.DAILY && day < today && action.createdDay <= day

    /** Anzeige einer Handlung an einem Tag: grüner Haken, rotes Kreuz oder offen. */
    fun markOf(action: GoalAction, day: Long): Mark = when {
        isDone(action.id, day) -> Mark.DONE
        isFailed(action.id, day) || missedWhenOpen(action, day) -> Mark.MISSED
        else -> Mark.NONE
    }

    private fun itemOf(action: GoalAction, goal: Goal, day: Long): TodayItem =
        TodayItem(action, goal, isDone(action.id, day), isFailed(action.id, day), markOf(action, day))

    /** Alle Tage, die ausdrücklich als nicht geschafft markiert wurden. */
    fun failedDaysOf(actionId: Long): Set<Long> = failedDaysByAction[actionId] ?: emptySet()

    private val progressByAction: Map<Long, LevelProgress> by lazy {
        val out = HashMap<Long, LevelProgress>()
        for (action in actions) {
            val progress = levelProgress(action, doneDaysOf(action.id), failedDaysOf(action.id), today)
            if (progress != null) out[action.id] = progress
        }
        out
    }

    /** Level-Stand einer Handlung, oder null, wenn sie keine Level hat. */
    fun levelProgressOf(action: GoalAction): LevelProgress? = progressByAction[action.id]

    /** Was die Handlung an [day] verlangte: der Text des damals gültigen Levels oder ihr Name. */
    fun titleOn(action: GoalAction, day: Long): String {
        val progress = levelProgressOf(action) ?: return action.title
        return levelTitle(action, progress.levelAt(day))
    }

    /** Text des aktuellen Levels bzw. der Name der Handlung ohne Level. */
    fun currentTitle(action: GoalAction): String {
        val progress = levelProgressOf(action) ?: return action.title
        return levelTitle(action, progress.index)
    }

    /** Verlauf einer Handlung mit Kalenderbewertung und Serie. */
    fun historyOf(action: GoalAction): ActionHistory =
        ActionHistory(action, doneDaysOf(action.id), today, failedDaysOf(action.id))

    /** Alle Tage, an denen die Handlung erledigt wurde. */
    fun doneDaysOf(actionId: Long): Set<Long> = doneDaysByAction[actionId] ?: emptySet()

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
        return doneDaysOf(action.id).count { it >= from && it <= to }
    }

    /** Ob die Vorgabe der Handlung im laufenden Zeitraum bereits erfüllt ist. */
    fun targetReached(action: GoalAction): Boolean = doneInPeriod(action) >= action.times

    /**
     * Handlungen eines Tages: tägliche (ab ihrem Startdatum) sowie Wochen- und Monatshandlungen,
     * die für den Tag eingeplant oder an ihm bewertet (erledigt, nicht geschafft) wurden.
     */
    fun itemsFor(day: Long): List<TodayItem> = activeActions
        .filter { pair ->
            val action = pair.first
            if (action.period == Period.DAILY) {
                action.createdDay <= day
            } else {
                isPlanned(action.id, day) || isDone(action.id, day) || isFailed(action.id, day)
            }
        }
        .map { pair -> itemOf(pair.first, pair.second, day) }

    /** Eigene To-dos eines Tages, unabhängig von Zielen. */
    fun todosFor(day: Long): List<Todo> = todosByDay[day] ?: emptyList()

    /** Ob die Handlung an [day] verfolgt wurde: nach ihrem Start und bevor das Ziel erreicht war. */
    private fun tracked(action: GoalAction, goal: Goal, day: Long): Boolean {
        val achieved = goal.achievedDay
        return action.createdDay <= day && (achieved == null || day <= achieved)
    }

    /**
     * Handlungen eines Tages für den Verlauf: tägliche, die an dem Tag verfolgt wurden,
     * und alle übrigen, die an dem Tag bewertet wurden. Schließt erreichte Ziele ein.
     */
    fun historyItemsFor(day: Long): List<TodayItem> = allActions
        .filter { pair ->
            val action = pair.first
            if (action.period == Period.DAILY) {
                tracked(action, pair.second, day) || isDone(action.id, day) || isFailed(action.id, day)
            } else {
                isDone(action.id, day) || isFailed(action.id, day)
            }
        }
        .map { pair -> itemOf(pair.first, pair.second, day) }

    /** (erledigt, gesamt) eines Tages über alle Handlungen und eigenen To-dos. */
    fun dayStats(day: Long): Pair<Int, Int> {
        val items = historyItemsFor(day)
        val own = todosFor(day)
        return Pair(items.count { it.done } + own.count { it.done }, items.size + own.size)
    }

    /** Färbung eines Tages im Verlauf: alles erledigt, teilweise, nichts erledigt oder offen. */
    fun overallDayMark(day: Long): Mark {
        if (day > today) return Mark.NONE
        val stats = dayStats(day)
        val done = stats.first
        val total = stats.second
        return when {
            total == 0 -> Mark.NONE
            done >= total -> Mark.DONE
            done > 0 -> Mark.PARTIAL
            day < today -> Mark.MISSED
            else -> Mark.NONE
        }
    }

    /**
     * Auswertung je Handlung für den Zeitraum [from, to].
     * [scope] gibt an, ob es sich um eine Woche (WEEKLY) oder einen Monat (MONTHLY) handelt.
     */
    fun periodRows(from: Long, to: Long, scope: Period): List<PeriodRow> {
        val out = ArrayList<PeriodRow>()
        if (from > today) return out
        val upTo = minOf(to, today)
        for (pair in allActions) {
            val action = pair.first
            val goal = pair.second
            val achieved = goal.achievedDay
            val done = doneDaysOf(action.id).count { it >= from && it <= to }
            val activeInRange = action.createdDay <= upTo && (achieved == null || achieved >= from)
            val target: Int? = when (action.period) {
                Period.DAILY -> {
                    val start = maxOf(from, action.createdDay)
                    val end = if (achieved == null) upTo else minOf(upTo, achieved)
                    if (end < start) 0 else (end - start + 1).toInt()
                }
                Period.WEEKLY -> if (scope == Period.WEEKLY && activeInRange) action.times else null
                Period.MONTHLY -> if (scope == Period.MONTHLY && activeInRange) action.times else null
            }
            if (done > 0 || (target != null && target > 0)) {
                out.add(PeriodRow(action, goal, done, target))
            }
        }
        return out
    }

    /** (erledigt, gesamt) der eigenen To-dos im Zeitraum [from, to]. */
    fun todoStats(from: Long, to: Long): Pair<Int, Int> {
        var done = 0
        var total = 0
        for (todo in todos) {
            if (todo.day >= from && todo.day <= to) {
                total++
                if (todo.done) done++
            }
        }
        return Pair(done, total)
    }
}
