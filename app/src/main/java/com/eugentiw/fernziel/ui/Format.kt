package com.eugentiw.fernziel.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.eugentiw.fernziel.AppState
import com.eugentiw.fernziel.Mark
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.data.Category
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import com.eugentiw.fernziel.data.Todo
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun categoryLabelRes(category: Category): Int = when (category) {
    Category.HEALTH -> R.string.cat_health
    Category.FINANCE -> R.string.cat_finance
    Category.CAREER -> R.string.cat_career
    Category.EDUCATION -> R.string.cat_education
    Category.RELATIONSHIPS -> R.string.cat_relationships
    Category.PERSONAL -> R.string.cat_personal
    Category.LEISURE -> R.string.cat_leisure
    Category.OTHER -> R.string.cat_other
}

/** Datum in der üblichen Schreibweise der eingestellten Sprache, z. B. 31.12.2027. */
fun formatDay(day: Long): String =
    LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** Uhrzeit aus Minuten seit Mitternacht, in der Schreibweise der eingestellten Sprache. */
fun formatTime(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/** Häufigkeit einer Handlung, z. B. „3× pro Woche". */
@Composable
fun frequencyText(period: Period, times: Int): String = when (period) {
    Period.DAILY -> stringResource(R.string.freq_daily)
    Period.WEEKLY -> stringResource(R.string.freq_weekly, times)
    Period.MONTHLY -> stringResource(R.string.freq_monthly, times)
}

/** Zieltermin mit verbleibender Zeit, z. B. „31.12.2027 · noch 416 Tage". Leer ohne Termin. */
@Composable
fun deadlineText(goal: Goal, today: Long): String {
    val target = goal.targetDay
    if (target == null) {
        return ""
    }
    val diff = (target - today).toInt()
    val relative = when {
        diff > 1 -> stringResource(R.string.days_left_other, diff)
        diff == 1 -> stringResource(R.string.days_left_one)
        diff == 0 -> stringResource(R.string.due_today)
        diff == -1 -> stringResource(R.string.overdue_one)
        else -> stringResource(R.string.overdue_other, -diff)
    }
    return formatDay(target) + " · " + relative
}

/**
 * Unterzeile einer Handlung an einem Tag: Ziel, bei Level-Handlungen das damals gültige
 * Level, und die Häufigkeit. Beispiel: „Gesunder Schlaf · Level 2/4 · Täglich".
 */
@Composable
fun actionSubtitle(state: AppState, action: GoalAction, goal: Goal, day: Long): String {
    val progress = state.levelProgressOf(action)
    val level = if (progress != null) {
        stringResource(R.string.level_short, progress.levelAt(day) + 1, progress.total) + " · "
    } else {
        ""
    }
    return goal.title + " · " + level + frequencyText(action.period, action.times)
}

/** Fortschritt im aktuellen Level, z. B. „4/7 Tage". */
@Composable
fun levelProgressText(period: Period, count: Int, needed: Int): String {
    val res = when (period) {
        Period.DAILY -> R.plurals.level_progress_days
        Period.WEEKLY -> R.plurals.level_progress_weeks
        Period.MONTHLY -> R.plurals.level_progress_months
    }
    return pluralStringResource(res, needed, count, needed)
}

/**
 * Zusatz rechts an einer Handlung von heute: Fortschritt zum nächsten Level, „Level geschafft",
 * oder im höchsten Level bzw. ohne Level die Serie täglicher Handlungen.
 */
@Composable
fun todayTrailing(state: AppState, action: GoalAction): String? {
    val progress = state.levelProgressOf(action)
    if (progress != null) {
        val needed = progress.needed
        if (progress.levelAt(state.today) < progress.index) return stringResource(R.string.level_done_today)
        if (needed != null) return levelProgressText(action.period, progress.count, needed)
        if (action.period != Period.DAILY) return null
        return if (progress.count > 0) pluralStringResource(R.plurals.streak_days, progress.count, progress.count) else null
    }
    if (action.period != Period.DAILY) return null
    val days = state.historyOf(action).streak().current
    return if (days > 0) pluralStringResource(R.plurals.streak_days, days, days) else null
}

/** Anzeige eines eigenen To-dos: grüner Haken, rotes Kreuz oder offen. */
fun todoMark(todo: Todo): Mark = when {
    todo.done -> Mark.DONE
    todo.failed -> Mark.MISSED
    else -> Mark.NONE
}
