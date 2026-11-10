package com.eugentiw.fernziel.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.data.Category
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.Period
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
