package com.eugentiw.fernziel.reminders

import com.eugentiw.fernziel.data.Period
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Berechnet die Zeitpunkte von Erinnerungen. Enthält keine Android-Abhängigkeiten.
 *
 * Tägliche Handlungen erinnern jeden Tag zur gewählten Uhrzeit, wöchentliche am Montag
 * und monatliche am Monatsersten (jeweils als Anstoß, die Tage festzulegen).
 */
object ReminderTimes {

    /** Erster Tag des Zeitraums, in dem [date] liegt. */
    private fun anchor(period: Period, date: LocalDate): LocalDate = when (period) {
        Period.DAILY -> date
        Period.WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Period.MONTHLY -> date.withDayOfMonth(1)
    }

    private fun shift(period: Period, date: LocalDate, steps: Long): LocalDate = when (period) {
        Period.DAILY -> date.plusDays(steps)
        Period.WEEKLY -> date.plusWeeks(steps)
        Period.MONTHLY -> date.plusMonths(steps)
    }

    /** Jüngster Erinnerungszeitpunkt, der nicht nach [now] liegt. */
    fun lastOccurrence(period: Period, minutes: Int, now: LocalDateTime): LocalDateTime {
        val base = anchor(period, now.toLocalDate())
        val candidate = base.atTime(minutes / 60, minutes % 60)
        return if (!candidate.isAfter(now)) {
            candidate
        } else {
            shift(period, base, -1).atTime(minutes / 60, minutes % 60)
        }
    }

    /** Nächster Erinnerungszeitpunkt, der echt nach [now] liegt. */
    fun nextOccurrence(period: Period, minutes: Int, now: LocalDateTime): LocalDateTime {
        val base = anchor(period, now.toLocalDate())
        val candidate = base.atTime(minutes / 60, minutes % 60)
        return if (candidate.isAfter(now)) {
            candidate
        } else {
            shift(period, base, 1).atTime(minutes / 60, minutes % 60)
        }
    }
}
