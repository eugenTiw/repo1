package com.eugentiw.fernziel.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eugentiw.fernziel.ActionHistory
import com.eugentiw.fernziel.AppState
import com.eugentiw.fernziel.Mark
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import com.eugentiw.fernziel.weekStartOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Monatsübersicht eines Ziels: Kalender mit erledigten (grün) und verpassten (rot) Tagen,
 * dazu die zwölf Monate des Jahres, in denen die Vorgabe geschafft oder verpasst wurde.
 */
@Composable
fun GoalDetailScreen(
    state: AppState,
    goal: Goal,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleDone: (Long, Long) -> Unit,
) {
    val c = LocalAppColors.current
    val actions = state.actionsOf(goal.id)
    val todayDate = LocalDate.ofEpochDay(state.today)

    var selectedActionId by rememberSaveable(goal.id) { mutableStateOf(0L) }
    var monthIndex by rememberSaveable(goal.id) {
        mutableIntStateOf(todayDate.year * 12 + todayDate.monthValue - 1)
    }

    val action: GoalAction? = actions.firstOrNull { it.id == selectedActionId } ?: actions.firstOrNull()
    val month: YearMonth = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val history: ActionHistory? = remember(action, state.completions, state.today) {
        if (action == null) null else ActionHistory(action, state.doneDaysOf(action.id), state.today)
    }
    val deadline = deadlineText(goal, state.today)
    val achievedDay = goal.achievedDay

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextAction(stringResource(R.string.back), c.accent, onBack)
            Spacer(Modifier.weight(1f))
            TextAction(stringResource(R.string.edit), c.accent, onEdit)
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp)
        ) {
            Text(
                stringResource(categoryLabelRes(goal.category)).uppercase(),
                modifier = Modifier.padding(start = 4.dp),
                style = AppType.overline,
                color = c.accent,
            )
            Text(
                goal.title,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                style = AppType.title,
                color = c.text,
            )
            if (goal.criterion.isNotBlank()) {
                Text(
                    goal.criterion,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    style = AppType.subhead,
                    color = c.textSecondary,
                )
            }
            if (achievedDay != null) {
                Text(
                    stringResource(R.string.achieved_on, formatDay(achievedDay)),
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    style = AppType.caption,
                    color = c.textSecondary,
                )
            } else if (deadline.isNotEmpty()) {
                Text(
                    deadline,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    style = AppType.caption,
                    color = c.textSecondary,
                )
            }

            if (action == null || history == null) {
                Spacer(Modifier.height(24.dp))
                Panel {
                    Text(
                        stringResource(R.string.no_actions),
                        modifier = Modifier.padding(16.dp),
                        style = AppType.body,
                        color = c.textSecondary,
                    )
                }
            } else {
                if (actions.size > 1) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        actions.forEach { item ->
                            Chip(
                                text = item.title,
                                selected = item.id == action.id,
                                onClick = { selectedActionId = item.id },
                            )
                        }
                    }
                }

                SectionHeader(
                    text = action.title,
                    trailing = frequencyText(action.period, action.times).uppercase(),
                )
                Panel {
                    MonthCalendar(
                        month = month,
                        history = history,
                        today = state.today,
                        onPrevious = { monthIndex = monthIndex - 1 },
                        onNext = { monthIndex = monthIndex + 1 },
                        onToggle = { day -> onToggleDone(action.id, day) },
                    )
                }
                Text(
                    stringResource(R.string.calendar_hint),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                    style = AppType.caption,
                    color = c.textSecondary,
                )

                SectionHeader(month.year.toString())
                Panel {
                    YearStrip(
                        year = month.year,
                        selected = month,
                        history = history,
                        onSelect = { monthNumber -> monthIndex = month.year * 12 + monthNumber - 1 },
                    )
                }
                Legend()
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    history: ActionHistory,
    today: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggle: (Long) -> Unit,
) {
    val c = LocalAppColors.current
    val locale = Locale.getDefault()
    val title = remember(month, locale) {
        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    }
    val firstDay = month.atDay(1).toEpochDay()
    val lastDay = month.atEndOfMonth().toEpochDay()
    val weekStarts = remember(month) {
        val list = ArrayList<Long>()
        var start = weekStartOf(firstDay)
        while (start <= lastDay) {
            list.add(start)
            start += 7
        }
        list
    }
    val showWeeks = history.action.period != Period.MONTHLY
    val summary = history.monthSummary(month)
    val doneLabel = stringResource(R.string.done)
    val missedLabel = stringResource(R.string.mark_missed)

    Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphButton(Glyph.CHEVRON_LEFT, stringResource(R.string.prev_month), c.text, onPrevious, glyphSize = 14.dp)
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = AppType.headline,
                color = c.text,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            GlyphButton(Glyph.CHEVRON_RIGHT, stringResource(R.string.next_month), c.text, onNext, glyphSize = 14.dp)
        }

        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                Text(
                    DayOfWeek.of(i + 1).getDisplayName(JavaTextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    style = AppType.caption,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            if (showWeeks) {
                Spacer(Modifier.width(22.dp))
            }
        }
        Spacer(Modifier.height(4.dp))

        weekStarts.forEach { weekStart ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0..6) {
                    val day = weekStart + i
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day >= firstDay && day <= lastDay) {
                            val mark = history.dayMark(day)
                            val dateLabel = formatDay(day)
                            CalendarDay(
                                number = (day - firstDay + 1).toInt(),
                                description = when (mark) {
                                    Mark.DONE -> "$dateLabel, $doneLabel"
                                    Mark.MISSED -> "$dateLabel, $missedLabel"
                                    else -> dateLabel
                                },
                                mark = mark,
                                isToday = day == today,
                                enabled = day <= today,
                                onClick = { onToggle(day) },
                            )
                        } else {
                            Spacer(Modifier.size(34.dp))
                        }
                    }
                }
                if (showWeeks) {
                    WeekMark(history.weekMark(weekStart))
                }
            }
        }

        if (summary.second > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = when (history.action.period) {
                    Period.DAILY -> stringResource(R.string.summary_days, summary.first, summary.second)
                    Period.WEEKLY -> stringResource(R.string.summary_weeks, summary.first, summary.second)
                    Period.MONTHLY -> stringResource(R.string.summary_count, summary.first, summary.second)
                },
                modifier = Modifier.fillMaxWidth(),
                style = AppType.caption,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Ein Kalendertag: grün gefüllt = erledigt, zartrot = verpasst. Tippen schaltet „erledigt" um. */
@Composable
private fun CalendarDay(
    number: Int,
    description: String,
    mark: Mark,
    isToday: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    val fillColor = when (mark) {
        Mark.DONE -> c.success
        Mark.MISSED -> c.dangerSoft
        else -> Color.Transparent
    }
    val textColor = when (mark) {
        Mark.DONE -> c.onSuccess
        Mark.MISSED -> c.danger
        else -> if (enabled) c.text else c.textTertiary
    }
    val ringColor = if (isToday && mark == Mark.NONE) c.textTertiary else Color.Transparent
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(fillColor)
            .border(1.5.dp, ringColor, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = AppType.subhead,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

/** Bewertung einer Woche am Zeilenende: Haken = geschafft, Kreuz = verpasst. */
@Composable
private fun WeekMark(mark: Mark) {
    val c = LocalAppColors.current
    val doneColor = c.success
    val missedColor = c.danger
    Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
        if (mark == Mark.DONE) {
            Canvas(Modifier.size(13.dp)) { drawGlyph(Glyph.CHECK, doneColor) }
        } else if (mark == Mark.MISSED) {
            Canvas(Modifier.size(10.dp)) { drawGlyph(Glyph.CLOSE, missedColor) }
        }
    }
}

/** Die zwölf Monate eines Jahres: grün = geschafft, zartrot = verpasst. */
@Composable
private fun YearStrip(
    year: Int,
    selected: YearMonth,
    history: ActionHistory,
    onSelect: (Int) -> Unit,
) {
    val locale = Locale.getDefault()
    Column(
        Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in 0..1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (column in 0..5) {
                    val monthNumber = row * 6 + column + 1
                    val yearMonth = YearMonth.of(year, monthNumber)
                    MonthPill(
                        label = Month.of(monthNumber).getDisplayName(JavaTextStyle.SHORT_STANDALONE, locale),
                        mark = history.monthMark(yearMonth),
                        selected = yearMonth == selected,
                        onClick = { onSelect(monthNumber) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthPill(
    label: String,
    mark: Mark,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(10.dp)
    val fillColor = when (mark) {
        Mark.DONE -> c.success
        Mark.MISSED -> c.dangerSoft
        else -> c.fill
    }
    val textColor = when (mark) {
        Mark.DONE -> c.onSuccess
        Mark.MISSED -> c.danger
        else -> c.textSecondary
    }
    Box(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(fillColor)
            .border(1.5.dp, if (selected) c.text else Color.Transparent, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = AppType.caption,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun Legend() {
    val c = LocalAppColors.current
    val doneColor = c.success
    val missedColor = c.dangerSoft
    Row(
        Modifier.padding(start = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(10.dp)) { drawCircle(doneColor) }
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.legend_done), style = AppType.caption, color = c.textSecondary)
        Spacer(Modifier.width(16.dp))
        Canvas(Modifier.size(10.dp)) { drawCircle(missedColor) }
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.legend_missed), style = AppType.caption, color = c.textSecondary)
    }
}
