package com.eugentiw.fernziel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.eugentiw.fernziel.AppState
import com.eugentiw.fernziel.Mark
import com.eugentiw.fernziel.PeriodRow
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.data.Period
import com.eugentiw.fernziel.weekStartOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Verlauf über alle Ziele: Monatskalender mit der Erledigung jedes Tages,
 * darunter die Aufgaben des gewählten Tages oder die Summen der Woche bzw. des Monats.
 */
@Composable
fun HistoryScreen(
    state: AppState,
    onBack: () -> Unit,
    onToggleDone: (Long, Long) -> Unit,
    onToggleTodo: (Long) -> Unit,
) {
    val c = LocalAppColors.current
    val locale = Locale.getDefault()
    val todayDate = LocalDate.ofEpochDay(state.today)

    var monthIndex by rememberSaveable { mutableIntStateOf(todayDate.year * 12 + todayDate.monthValue - 1) }
    var selectedDay by rememberSaveable { mutableStateOf(state.today) }
    var mode by rememberSaveable { mutableIntStateOf(0) }

    val month: YearMonth = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val monthTitle = remember(month, locale) {
        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    }
    val firstDay = month.atDay(1).toEpochDay()
    val lastDay = month.atEndOfMonth().toEpochDay()
    val selectedWeek = weekStartOf(selectedDay)

    val goToMonth: (Int) -> Unit = { index ->
        monthIndex = index
        val target = YearMonth.of(index / 12, index % 12 + 1)
        selectedDay = if (YearMonth.from(todayDate) == target) state.today else target.atDay(1).toEpochDay()
    }

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
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp)
        ) {
            Text(
                stringResource(R.string.history_title),
                modifier = Modifier.padding(start = 4.dp),
                style = AppType.largeTitle,
                color = c.text,
            )
            Spacer(Modifier.height(20.dp))

            Panel {
                OverviewCalendar(
                    state = state,
                    title = monthTitle,
                    firstDay = firstDay,
                    lastDay = lastDay,
                    selectedDay = selectedDay,
                    highlightWeek = if (mode == 1) selectedWeek else null,
                    onPrevious = { goToMonth(monthIndex - 1) },
                    onNext = { goToMonth(monthIndex + 1) },
                    onSelect = { day -> selectedDay = day },
                )
            }
            Row(
                Modifier.padding(start = 4.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendItem(c.success, stringResource(R.string.legend_all))
                Spacer(Modifier.width(14.dp))
                LegendItem(c.successSoft, stringResource(R.string.legend_partial))
                Spacer(Modifier.width(14.dp))
                LegendItem(c.dangerSoft, stringResource(R.string.legend_none))
            }

            Spacer(Modifier.height(24.dp))
            Segmented(
                options = listOf(
                    stringResource(R.string.mode_day),
                    stringResource(R.string.mode_week),
                    stringResource(R.string.mode_month),
                ),
                selected = mode,
                onSelect = { mode = it },
            )

            if (mode == 0) {
                val stats = state.dayStats(selectedDay)
                val dayTitle = remember(selectedDay, locale) {
                    LocalDate.ofEpochDay(selectedDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
                }
                SectionHeader(
                    text = dayTitle,
                    trailing = if (stats.second > 0) "${stats.first}/${stats.second}" else null,
                )
                DayPanel(state, selectedDay, onToggleDone, onToggleTodo)
            } else if (mode == 1) {
                SectionHeader(formatDay(selectedWeek) + " – " + formatDay(selectedWeek + 6))
                PeriodPanel(
                    rows = state.periodRows(selectedWeek, selectedWeek + 6, Period.WEEKLY),
                    todoStats = state.todoStats(selectedWeek, selectedWeek + 6),
                )
            } else {
                SectionHeader(monthTitle)
                PeriodPanel(
                    rows = state.periodRows(firstDay, lastDay, Period.MONTHLY),
                    todoStats = state.todoStats(firstDay, lastDay),
                )
            }
        }
    }
}

@Composable
private fun OverviewCalendar(
    state: AppState,
    title: String,
    firstDay: Long,
    lastDay: Long,
    selectedDay: Long,
    highlightWeek: Long?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val c = LocalAppColors.current
    val locale = Locale.getDefault()
    val weekStarts = remember(firstDay, lastDay) {
        val list = ArrayList<Long>()
        var start = weekStartOf(firstDay)
        while (start <= lastDay) {
            list.add(start)
            start += 7
        }
        list
    }

    Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp)) {
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
        }
        Spacer(Modifier.height(4.dp))

        weekStarts.forEach { weekStart ->
            val highlighted = highlightWeek != null && highlightWeek == weekStart
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (highlighted) c.fill else Color.Transparent)
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0..6) {
                    val day = weekStart + i
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day >= firstDay && day <= lastDay) {
                            OverviewDay(
                                number = (day - firstDay + 1).toInt(),
                                description = formatDay(day),
                                mark = state.overallDayMark(day),
                                isToday = day == state.today,
                                selected = day == selectedDay,
                                future = day > state.today,
                                onClick = { onSelect(day) },
                            )
                        } else {
                            Spacer(Modifier.size(34.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Ein Tag im Verlauf: grün = alles erledigt, hellgrün = teilweise, zartrot = nichts erledigt. */
@Composable
private fun OverviewDay(
    number: Int,
    description: String,
    mark: Mark,
    isToday: Boolean,
    selected: Boolean,
    future: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    val fillColor = when (mark) {
        Mark.DONE -> c.success
        Mark.PARTIAL -> c.successSoft
        Mark.MISSED -> c.dangerSoft
        else -> Color.Transparent
    }
    val textColor = when (mark) {
        Mark.DONE -> c.onSuccess
        Mark.PARTIAL -> c.success
        Mark.MISSED -> c.danger
        else -> if (future) c.textTertiary else c.text
    }
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(fillColor)
            .border(1.5.dp, if (selected) c.text else Color.Transparent, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = AppType.subhead,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}

/** Aufgaben und eigene To-dos eines Tages. Bis einschließlich heute lassen sie sich abhaken. */
@Composable
private fun DayPanel(
    state: AppState,
    day: Long,
    onToggleDone: (Long, Long) -> Unit,
    onToggleTodo: (Long) -> Unit,
) {
    val c = LocalAppColors.current
    val items = state.historyItemsFor(day)
    val ownTodos = state.todosFor(day)
    Panel {
        if (items.isEmpty() && ownTodos.isEmpty()) {
            Text(
                stringResource(R.string.no_entries),
                modifier = Modifier.padding(16.dp),
                style = AppType.body,
                color = c.textSecondary,
            )
        }
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Hairline(startInset = 54.dp)
            }
            TaskRow(
                title = item.action.title,
                subtitle = item.goal.title + " · " + frequencyText(item.action.period, item.action.times),
                done = item.done,
                enabled = day <= state.today,
                onToggle = { onToggleDone(item.action.id, day) },
            )
        }
        ownTodos.forEachIndexed { index, todo ->
            if (index > 0 || items.isNotEmpty()) {
                Hairline(startInset = 54.dp)
            }
            TaskRow(
                title = todo.title,
                subtitle = stringResource(R.string.own_todo),
                done = todo.done,
                enabled = true,
                onToggle = { onToggleTodo(todo.id) },
            )
        }
    }
}

/** Summen je Handlung für eine Woche oder einen Monat, dazu die eigenen To-dos. */
@Composable
private fun PeriodPanel(rows: List<PeriodRow>, todoStats: Pair<Int, Int>) {
    val c = LocalAppColors.current
    Panel {
        if (rows.isEmpty() && todoStats.second == 0) {
            Text(
                stringResource(R.string.no_entries),
                modifier = Modifier.padding(16.dp),
                style = AppType.body,
                color = c.textSecondary,
            )
        }
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                Hairline()
            }
            val target = row.target
            PeriodLine(
                title = row.action.title,
                subtitle = row.goal.title + " · " + frequencyText(row.action.period, row.action.times),
                value = if (target != null) "${row.done}/$target" else stringResource(R.string.times_count, row.done),
                reached = target != null && target > 0 && row.done >= target,
            )
        }
        if (todoStats.second > 0) {
            if (rows.isNotEmpty()) {
                Hairline()
            }
            PeriodLine(
                title = stringResource(R.string.own_todos),
                subtitle = null,
                value = "${todoStats.first}/${todoStats.second}",
                reached = todoStats.first >= todoStats.second,
            )
        }
    }
}

@Composable
private fun PeriodLine(title: String, subtitle: String?, value: String, reached: Boolean) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppType.body, color = c.text)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = AppType.caption,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = AppType.body,
            fontWeight = FontWeight.Medium,
            color = if (reached) c.success else c.textSecondary,
        )
    }
}
