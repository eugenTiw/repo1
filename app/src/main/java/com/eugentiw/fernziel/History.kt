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
 * Serie einer Handlung: wie viele Zeiträume in Folge die Vorgabe erfüllt wurde.
 * Gezählt wird in Tagen, Wochen oder Monaten, je nach Häufigkeit der Handlung.
 */
data class Streak(val current: Int, val best: Int)

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
    /** Tage, die ausdrücklich als „nicht geschafft" markiert wurden. */
    private val failedDays: Set<Long> = emptySet(),
) {

    fun isDone(day: Long): Boolean = doneDays.contains(day)

    fun doneBetween(from: Long, to: Long): Int = doneDays.count { it >= from && it <= to }

    /**
     * Tag: erledigt ist grün. Rot ist ein Tag, der ausdrücklich als nicht geschafft markiert
     * wurde, sowie jeder vergangene, nicht erledigte Tag einer täglichen Handlung.
     */
    fun dayMark(day: Long): Mark {
        if (doneDays.contains(day)) return Mark.DONE
        if (failedDays.contains(day)) return Mark.MISSED
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

    /**
     * Aktuelle und längste Serie. Der laufende Zeitraum (heute, diese Woche, dieser Monat)
     * unterbricht die aktuelle Serie nicht, solange er noch offen ist; er zählt mit, sobald
     * die Vorgabe erfüllt ist. Ein heute als nicht geschafft markierter Tag beendet sie sofort.
     */
    fun streak(): Streak = when (action.period) {
        Period.DAILY -> dailyStreak()
        Period.WEEKLY -> periodStreak(weekIndexOf(today)) { day -> weekIndexOf(day) }
        Period.MONTHLY -> periodStreak(monthIndexOf(today)) { day -> monthIndexOf(day) }
    }

    private fun dailyStreak(): Streak {
        var current = 0
        if (!failedDays.contains(today)) {
            var day = if (doneDays.contains(today)) today else today - 1
            while (doneDays.contains(day)) {
                current++
                day--
            }
        }
        var best = 0
        var run = 0
        var previous = Long.MIN_VALUE
        for (day in doneDays.sorted()) {
            if (day > today) break
            run = if (day == previous + 1) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return Streak(current, best)
    }

    /**
     * Serie über Wochen oder Monate. [indexOf] ordnet jedem Tag die fortlaufende Nummer
     * seines Zeitraums zu; [currentIndex] ist die Nummer des laufenden Zeitraums.
     */
    private fun periodStreak(currentIndex: Long, indexOf: (Long) -> Long): Streak {
        val counts = HashMap<Long, Int>()
        for (day in doneDays) {
            val index = indexOf(day)
            counts[index] = (counts[index] ?: 0) + 1
        }
        val needed = maxOf(1, action.times)
        val first = counts.keys.minOrNull() ?: return Streak(0, 0)

        var index = if ((counts[currentIndex] ?: 0) >= needed) currentIndex else currentIndex - 1
        var current = 0
        while ((counts[index] ?: 0) >= needed) {
            current++
            index--
        }

        var best = 0
        var run = 0
        index = first
        while (index <= currentIndex) {
            if ((counts[index] ?: 0) >= needed) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
            index++
        }
        return Streak(current, best)
    }

    private fun weekIndexOf(day: Long): Long = Math.floorDiv(weekStartOf(day), 7L)

    private fun monthIndexOf(day: Long): Long {
        val date = LocalDate.ofEpochDay(day)
        return date.year * 12L + date.monthValue - 1
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
