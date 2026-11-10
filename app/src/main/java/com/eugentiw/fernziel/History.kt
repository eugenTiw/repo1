package com.eugentiw.fernziel

import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Montag der Woche, in der [day] liegt. */
fun weekStartOf(day: Long): Long =
    LocalDate.ofEpochDay(day)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .toEpochDay()

/** Bewertung eines Zeitraums: offen, geschafft, teilweise geschafft oder verpasst. */
enum class Mark { NONE, DONE, PARTIAL, MISSED }

/**
 * Wertet den Verlauf einer Handlung aus.
 *
 * Bewertet wird jeweils der Zeitraum, auf den sich die Häufigkeit bezieht:
 * bei täglichen Handlungen der Tag, bei wöchentlichen die Woche, bei monatlichen der Monat.
 * Zeiträume vor dem Startdatum und noch laufende Zeiträume gelten nie als verpasst.
 */
class ActionHistory(
    val action: GoalAction,
    private val doneDays: Set<Long>,
    private val today: Long,
) {

    fun isDone(day: Long): Boolean = doneDays.contains(day)

    fun doneBetween(from: Long, to: Long): Int = doneDays.count { it >= from && it <= to }

    /** Tag: erledigt ist grün; rot wird nur ein vergangener Tag einer täglichen Handlung. */
    fun dayMark(day: Long): Mark {
        if (doneDays.contains(day)) return Mark.DONE
        val missed = action.period == Period.DAILY && day >= action.createdDay && day < today
        return if (missed) Mark.MISSED else Mark.NONE
    }

    /**
     * Woche (ab Montag [weekStart]). Wöchentliche Handlung: Vorgabe erfüllt oder verpasst.
     * Tägliche Handlung: geschafft, wenn an jedem verfolgten Tag der Woche erledigt
     * (verpasste Tage sind dort bereits einzeln rot markiert).
     */
    fun weekMark(weekStart: Long): Mark {
        val weekEnd = weekStart + 6
        if (action.period == Period.DAILY) {
            val from = maxOf(weekStart, action.createdDay)
            if (from > weekEnd) return Mark.NONE
            val tracked = (weekEnd - from + 1).toInt()
            return if (doneBetween(from, weekEnd) >= tracked) Mark.DONE else Mark.NONE
        }
        if (action.period != Period.WEEKLY) return Mark.NONE
        if (doneBetween(weekStart, weekEnd) >= action.times) return Mark.DONE
        val missed = weekEnd < today && weekStart >= action.createdDay
        return if (missed) Mark.MISSED else Mark.NONE
    }

    /** Monat: geschafft, wenn die Vorgabe darin vollständig erfüllt wurde. */
    fun monthMark(month: YearMonth): Mark {
        val first = month.atDay(1).toEpochDay()
        val last = month.atEndOfMonth().toEpochDay()
        val over = last < today
        return when (action.period) {
            Period.MONTHLY -> monthlyMark(first, last, over)
            Period.DAILY -> dailyMonthMark(first, last, over)
            Period.WEEKLY -> weeklyMonthMark(first, last, over)
        }
    }

    /** (erreicht, gesamt) für die Zusammenfassung unter dem Kalender. */
    fun monthSummary(month: YearMonth): Pair<Int, Int> {
        val first = month.atDay(1).toEpochDay()
        val last = month.atEndOfMonth().toEpochDay()
        return when (action.period) {
            Period.MONTHLY -> Pair(doneBetween(first, last), action.times)
            Period.DAILY -> {
                val from = maxOf(first, action.createdDay)
                val to = minOf(last, today)
                if (to < from) Pair(doneBetween(first, last), 0)
                else Pair(doneBetween(from, to), (to - from + 1).toInt())
            }
            Period.WEEKLY -> {
                var done = 0
                var total = 0
                for (weekStart in weeksEndingIn(first, last)) {
                    val mark = weekMark(weekStart)
                    if (mark == Mark.DONE) {
                        done++
                        total++
                    } else if (mark == Mark.MISSED) {
                        total++
                    }
                }
                Pair(done, total)
            }
        }
    }

    private fun monthlyMark(first: Long, last: Long, over: Boolean): Mark {
        if (doneBetween(first, last) >= action.times) return Mark.DONE
        return if (over && first >= action.createdDay) Mark.MISSED else Mark.NONE
    }

    private fun dailyMonthMark(first: Long, last: Long, over: Boolean): Mark {
        if (!over) return Mark.NONE
        val from = maxOf(first, action.createdDay)
        if (from > last) return Mark.NONE
        val tracked = (last - from + 1).toInt()
        return if (doneBetween(from, last) >= tracked) Mark.DONE else Mark.MISSED
    }

    private fun weeklyMonthMark(first: Long, last: Long, over: Boolean): Mark {
        if (!over) return Mark.NONE
        var any = false
        for (weekStart in weeksEndingIn(first, last)) {
            val mark = weekMark(weekStart)
            if (mark == Mark.MISSED) return Mark.MISSED
            if (mark == Mark.DONE) any = true
        }
        return if (any) Mark.DONE else Mark.NONE
    }

    /** Montage aller Wochen, deren Sonntag im Bereich [first, last] liegt. */
    private fun weeksEndingIn(first: Long, last: Long): List<Long> {
        val out = ArrayList<Long>()
        var weekStart = weekStartOf(first)
        while (weekStart <= last) {
            val weekEnd = weekStart + 6
            if (weekEnd >= first && weekEnd <= last) out.add(weekStart)
            weekStart += 7
        }
        return out
    }
}
