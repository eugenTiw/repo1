package com.eugentiw.fernziel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eugentiw.fernziel.AppState
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.TodayItem
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/** Übersicht: Wochenhandlungen zum Zuteilen, die heutige To-do-Liste und Monatshandlungen. */
@Composable
fun TodayScreen(
    state: AppState,
    onToggleDone: (Long, Long) -> Unit,
    onTogglePlan: (Long, Long) -> Unit,
    onAddGoal: () -> Unit,
) {
    val c = LocalAppColors.current
    val today = state.today
    val weekly = state.activeActions.filter { it.first.period == Period.WEEKLY }
    val monthly = state.activeActions.filter { it.first.period == Period.MONTHLY }
    val items = state.todayItems
    val dateText = remember(today) {
        LocalDate.ofEpochDay(today).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 32.dp)
    ) {
        Text(
            dateText.uppercase(),
            modifier = Modifier.padding(start = 4.dp),
            style = AppType.overline,
            color = c.textSecondary,
        )
        Text(
            stringResource(R.string.today_title),
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            style = AppType.largeTitle,
            color = c.text,
        )

        if (state.loaded && state.activeActions.isEmpty()) {
            Spacer(Modifier.height(28.dp))
            EmptyState(onAddGoal)
        } else if (state.loaded) {
            if (weekly.isNotEmpty()) {
                SectionHeader(stringResource(R.string.section_week))
                Panel {
                    weekly.forEachIndexed { index, pair ->
                        if (index > 0) {
                            Hairline()
                        }
                        WeeklyRow(state, pair.first, pair.second, onToggleDone, onTogglePlan)
                    }
                }
                Text(
                    stringResource(R.string.week_hint),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                    style = AppType.caption,
                    color = c.textSecondary,
                )
            }

            SectionHeader(
                text = stringResource(R.string.section_todo),
                trailing = if (items.isNotEmpty()) "${items.count { it.done }}/${items.size}" else null,
            )
            Panel {
                if (items.isEmpty()) {
                    Text(
                        stringResource(R.string.nothing_planned),
                        modifier = Modifier.padding(16.dp),
                        style = AppType.body,
                        color = c.textSecondary,
                    )
                } else {
                    items.forEachIndexed { index, item ->
                        if (index > 0) {
                            Hairline(startInset = 54.dp)
                        }
                        TodoRow(item, onToggle = { onToggleDone(item.action.id, today) })
                    }
                }
            }

            if (monthly.isNotEmpty()) {
                SectionHeader(stringResource(R.string.section_month))
                Panel {
                    monthly.forEachIndexed { index, pair ->
                        if (index > 0) {
                            Hairline()
                        }
                        MonthlyRow(state, pair.first, pair.second, onTogglePlan)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(item: TodayItem, onToggle: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = item.done, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMark(item.done)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.action.title,
                style = AppType.body,
                color = if (item.done) c.textSecondary else c.text,
            )
            Text(
                item.goal.title + " · " + frequencyText(item.action.period, item.action.times),
                style = AppType.caption,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeeklyRow(
    state: AppState,
    action: GoalAction,
    goal: Goal,
    onToggleDone: (Long, Long) -> Unit,
    onTogglePlan: (Long, Long) -> Unit,
) {
    val c = LocalAppColors.current
    val doneCount = state.doneInPeriod(action)
    val locale = Locale.getDefault()

    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(action.title, style = AppType.body, color = c.text)
                Text(
                    goal.title,
                    style = AppType.caption,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "$doneCount/${action.times}",
                style = AppType.subhead,
                fontWeight = FontWeight.Medium,
                color = if (doneCount >= action.times) c.accent else c.textSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (i in 0..6) {
                val day = state.weekStart + i
                val weekday = DayOfWeek.of(i + 1)
                val done = state.isDone(action.id, day)
                val planned = state.isPlanned(action.id, day)
                val past = day < state.today
                DayChip(
                    label = weekday.getDisplayName(JavaTextStyle.NARROW, locale),
                    description = weekday.getDisplayName(JavaTextStyle.FULL, locale),
                    dayState = if (done) DayState.DONE else if (planned) DayState.PLANNED else DayState.NONE,
                    isToday = day == state.today,
                    dimmed = past,
                    enabled = past || !done,
                    onClick = {
                        // Vergangene Tage: nachträglich abhaken. Heute und Zukunft: einplanen.
                        if (past) {
                            onToggleDone(action.id, day)
                        } else {
                            onTogglePlan(action.id, day)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthlyRow(
    state: AppState,
    action: GoalAction,
    goal: Goal,
    onTogglePlan: (Long, Long) -> Unit,
) {
    val c = LocalAppColors.current
    val doneCount = state.doneInPeriod(action)
    val doneToday = state.isDone(action.id, state.today)
    val plannedToday = state.isPlanned(action.id, state.today)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(action.title, style = AppType.body, color = c.text)
            Text(
                goal.title + " · " + doneCount + "/" + action.times,
                style = AppType.caption,
                color = if (doneCount >= action.times) c.accent else c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Pill(
            text = when {
                doneToday -> stringResource(R.string.done)
                plannedToday -> stringResource(R.string.planned)
                else -> stringResource(R.string.plan_today)
            },
            active = doneToday || plannedToday,
            enabled = !doneToday,
            onClick = { onTogglePlan(action.id, state.today) },
        )
    }
}
