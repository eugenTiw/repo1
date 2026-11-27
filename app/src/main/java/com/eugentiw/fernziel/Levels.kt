package com.eugentiw.fernziel

import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import java.time.LocalDate

/**
 * Stand des Level-Systems einer Handlung.
 *
 * [index] ist das aktuelle Level (ab 0), [count] die Zeiträume am Stück, die im aktuellen
 * Level schon geschafft sind, [needed] die für den Aufstieg nötigen Zeiträume (null im
 * höchsten Level). [startDays] enthält den ersten Tag jedes bisher erreichten Levels.
 */
data class LevelProgress(
    val index: Int,
    val count: Int,
    val needed: Int?,
    val total: Int,
    val startDays: List<Long>,
) {
    val isMax: Boolean get() = needed == null

    /** Das Level, das an [day] galt. Ein Aufstieg gilt erst ab dem nächsten Zeitraum. */
    fun levelAt(day: Long): Int {
        var level = 0
        for (i in startDays.indices) {
            if (startDays[i] <= day) level = i
        }
        return level
    }
}

/** Aufstieg in ein neues Level, für den kurzen Hinweis nach dem Abhaken. */
data class LevelUp(
    val actionId: Long,
    val number: Int,
    val total: Int,
    val title: String,
    val period: Period,
) {
    val isMax: Boolean get() = number >= total
}

/** Text eines Levels; ist er leer, gilt der Name der Handlung. */
fun levelTitle(action: GoalAction, index: Int): String {
    val level = action.levels.getOrNull(index) ?: return action.title
    return level.title.ifBlank { action.title }
}

/**
 * Berechnet das Level einer Handlung aus ihrem Verlauf, oder null ohne Level.
 *
 * Gezählt wird ab dem Tag, an dem die Level eingeführt wurden. Ist ein Zeitraum geschafft,
 * wächst die Serie im aktuellen Level; ein verpasster Zeitraum setzt sie auf 0, das Level
 * bleibt aber erhalten (kein Abstieg). Erreicht die Serie die Länge des Levels, steigt man
 * ab dem nächsten Zeitraum auf. Im höchsten Level bleibt man und die Serie zählt weiter.
 * Der laufende Zeitraum unterbricht die Serie nicht, solange er offen ist.
 */
fun levelProgress(action: GoalAction, doneDays: Set<Long>, failedDays: Set<Long>, today: Long): LevelProgress? {
    val levels = action.levels
    if (levels.isEmpty()) return null
    val last = levels.size - 1
    val start = maxOf(action.createdDay, action.levelsSince ?: action.createdDay)
    val startDays = arrayListOf(start)
    var level = 0
    var count = 0

    fun success(nextStart: Long) {
        count++
        if (level < last && count >= maxOf(1, levels[level].length)) {
            level++
            count = 0
            startDays.add(nextStart)
        }
    }

    if (action.period == Period.DAILY) {
        var day = start
        while (day <= today) {
            if (doneDays.contains(day)) {
                success(day + 1)
            } else if (day < today || failedDays.contains(day)) {
                count = 0
            }
            day++
        }
    } else {
        val needed = maxOf(1, action.times)
        var periodStart = periodStartOf(action.period, start)
        while (periodStart <= today) {
            val nextStart = nextPeriodStart(action.period, periodStart)
            val from = maxOf(periodStart, start)
            val done = doneDays.count { it >= from && it < nextStart }
            if (done >= needed) {
                success(nextStart)
            } else if (nextStart <= today) {
                count = 0
            }
            periodStart = nextStart
        }
    }

    val neededNow = if (level < last) maxOf(1, levels[level].length) else null
    return LevelProgress(level, count, neededNow, levels.size, startDays)
}

private fun periodStartOf(period: Period, day: Long): Long = when (period) {
    Period.DAILY -> day
    Period.WEEKLY -> weekStartOf(day)
    Period.MONTHLY -> LocalDate.ofEpochDay(day).withDayOfMonth(1).toEpochDay()
}

private fun nextPeriodStart(period: Period, periodStart: Long): Long = when (period) {
    Period.DAILY -> periodStart + 1
    Period.WEEKLY -> periodStart + 7
    Period.MONTHLY -> LocalDate.ofEpochDay(periodStart).plusMonths(1).toEpochDay()
}
