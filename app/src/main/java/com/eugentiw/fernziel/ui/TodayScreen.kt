package com.eugentiw.fernziel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eugentiw.fernziel.AppState
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Übersicht der laufenden Woche: oben die Wochen- und Monatshandlungen zum Zuteilen,
 * darunter jeder Tag mit seinen Aufgaben und eigenen To-dos.
 */
@Composable
fun TodayScreen(
    state: AppState,
    hideDone: Boolean,
    scrollState: ScrollState,
    onToggleHideDone: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleDone: (Long, Long) -> Unit,
    onTogglePlan: (Long, Long) -> Unit,
    onAddTodo: (String, Long) -> Unit,
    onToggleTodo: (Long) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    onAddGoal: () -> Unit,
) {
    val c = LocalAppColors.current
    val today = state.today
    val locale = Locale.getDefault()
    val dateText = remember(today) {
        LocalDate.ofEpochDay(today).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    }
    val shortDate = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }

    // Handlungen, deren Vorgabe erfüllt ist, verschwinden bei durchgestrichenem Auge.
    val weekly = state.activeActions.filter { pair ->
        pair.first.period == Period.WEEKLY && !(hideDone && state.targetReached(pair.first))
    }
    val monthly = state.activeActions.filter { pair ->
        pair.first.period == Period.MONTHLY && !(hideDone && state.targetReached(pair.first))
    }

    // Vergangene Tage sind eingeklappt, bis man sie antippt.
    var expandedPast by remember(state.weekStart) { mutableStateOf(emptySet<Long>()) }
    // Tag, für den gerade ein eigenes To-do eingegeben wird.
    var addingDay by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = addingDay != null) { addingDay = null }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlyphButton(
                glyph = Glyph.CALENDAR,
                label = stringResource(R.string.open_history),
                tint = c.textSecondary,
                onClick = onOpenHistory,
                glyphSize = 22.dp,
            )
            Spacer(Modifier.weight(1f))
            GlyphButton(
                glyph = if (hideDone) Glyph.EYE_OFF else Glyph.EYE,
                label = stringResource(if (hideDone) R.string.show_done else R.string.hide_done),
                tint = if (hideDone) c.textTertiary else c.textSecondary,
                onClick = onToggleHideDone,
                glyphSize = 24.dp,
            )
        }
        Text(
            dateText.uppercase(),
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            style = AppType.overline,
            color = c.textSecondary,
        )
        Text(
            stringResource(R.string.today_title),
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            style = AppType.largeTitle,
            color = c.text,
        )

        if (state.loaded) {
            if (state.activeActions.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                EmptyState(onAddGoal)
            }

            if (weekly.isNotEmpty()) {
                SectionHeader(stringResource(R.string.section_week))
                Panel {
                    weekly.forEachIndexed { index, pair ->
                        key(pair.first.id) {
                            if (index > 0) {
                                Hairline()
                            }
                            AssignRow(state, pair.first, pair.second, onToggleDone, onTogglePlan)
                        }
                    }
                }
            }
            if (monthly.isNotEmpty()) {
                SectionHeader(stringResource(R.string.section_month))
                Panel {
                    monthly.forEachIndexed { index, pair ->
                        key(pair.first.id) {
                            if (index > 0) {
                                Hairline()
                            }
                            AssignRow(state, pair.first, pair.second, onToggleDone, onTogglePlan)
                        }
                    }
                }
            }
            if (weekly.isNotEmpty() || monthly.isNotEmpty()) {
                Text(
                    stringResource(R.string.week_hint),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                    style = AppType.caption,
                    color = c.textSecondary,
                )
            }

            for (i in 0..6) {
                val day = state.weekStart + i
                val isToday = day == today
                val past = day < today
                val items = state.itemsFor(day)
                val ownTodos = state.todosFor(day)
                val visibleItems = if (hideDone) items.filter { !it.done } else items
                val visibleTodos = if (hideDone) ownTodos.filter { !it.done } else ownTodos
                val total = items.size + ownTodos.size
                val doneCount = items.count { it.done } + ownTodos.count { it.done }
                val adding = addingDay == day
                val expanded = !past || adding || expandedPast.contains(day)
                val weekdayName = DayOfWeek.of(i + 1).getDisplayName(JavaTextStyle.FULL, locale)

                DayHeader(
                    title = if (isToday) stringResource(R.string.today_title) + " · " + weekdayName else weekdayName,
                    date = LocalDate.ofEpochDay(day).format(shortDate),
                    count = if (total > 0) "$doneCount/$total" else null,
                    isToday = isToday,
                    collapsible = past,
                    expanded = expanded,
                    onClick = {
                        expandedPast = if (expandedPast.contains(day)) expandedPast - day else expandedPast + day
                    },
                    onAdd = { addingDay = day },
                )
                if (expanded) {
                    Panel {
                        if (visibleItems.isEmpty() && visibleTodos.isEmpty() && !adding) {
                            Text(
                                stringResource(if (total == 0) R.string.nothing_planned else R.string.all_done),
                                modifier = Modifier.padding(16.dp),
                                style = AppType.body,
                                color = c.textSecondary,
                            )
                        }
                        visibleItems.forEachIndexed { index, item ->
                            key("a" + item.action.id) {
                                if (index > 0) {
                                    Hairline(startInset = 54.dp)
                                }
                                TaskRow(
                                    title = item.action.title,
                                    subtitle = item.goal.title + " · " +
                                        frequencyText(item.action.period, item.action.times),
                                    done = item.done,
                                    enabled = day <= today,
                                    onToggle = { onToggleDone(item.action.id, day) },
                                )
                            }
                        }
                        visibleTodos.forEachIndexed { index, todo ->
                            key("t" + todo.id) {
                                if (index > 0 || visibleItems.isNotEmpty()) {
                                    Hairline(startInset = 54.dp)
                                }
                                TaskRow(
                                    title = todo.title,
                                    subtitle = null,
                                    done = todo.done,
                                    enabled = true,
                                    onToggle = { onToggleTodo(todo.id) },
                                    onRemove = { onDeleteTodo(todo.id) },
                                    removeLabel = stringResource(R.string.remove_todo),
                                )
                            }
                        }
                        if (adding) {
                            if (visibleItems.isNotEmpty() || visibleTodos.isNotEmpty()) {
                                Hairline(startInset = 54.dp)
                            }
                            NewTodoRow(
                                onSubmit = { text -> onAddTodo(text, day) },
                                onClose = { addingDay = null },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kopfzeile eines Tages mit Datum, Zähler und Plus für ein eigenes To-do.
 * Vergangene Tage lassen sich auf- und zuklappen.
 */
@Composable
private fun DayHeader(
    title: String,
    date: String,
    count: String?,
    isToday: Boolean,
    collapsible: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    val c = LocalAppColors.current
    val chevronColor = c.textTertiary
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = collapsible, role = Role.Button, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = AppType.headline,
                color = if (isToday) c.accent else if (collapsible) c.textSecondary else c.text,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                date,
                modifier = Modifier.weight(1f),
                style = AppType.caption,
                color = c.textSecondary,
                maxLines = 1,
            )
            if (count != null) {
                Text(count, style = AppType.caption, fontWeight = FontWeight.Medium, color = c.textSecondary)
            }
            if (collapsible) {
                Spacer(Modifier.width(8.dp))
                Canvas(Modifier.size(10.dp)) {
                    drawGlyph(if (expanded) Glyph.CHEVRON_DOWN else Glyph.CHEVRON_RIGHT, chevronColor)
                }
            }
        }
        GlyphButton(
            glyph = Glyph.PLUS,
            label = stringResource(R.string.add_todo),
            tint = c.textSecondary,
            onClick = onAdd,
            glyphSize = 15.dp,
        )
    }
}

/** Eingabezeile für ein eigenes To-do. Die Tastatur-Taste „Fertig" fügt es hinzu. */
@Composable
private fun NewTodoRow(onSubmit: (String) -> Unit, onClose: () -> Unit) {
    val c = LocalAppColors.current
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMark(false)
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = AppType.body.copy(color = c.text),
            placeholder = {
                Text(stringResource(R.string.new_todo_placeholder), style = AppType.body, color = c.textTertiary)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isBlank()) {
                        onClose()
                    } else {
                        onSubmit(text)
                        text = ""
                    }
                },
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = c.accent,
            ),
        )
        GlyphButton(
            glyph = Glyph.CLOSE,
            label = stringResource(R.string.cancel),
            tint = c.textTertiary,
            onClick = onClose,
            glyphSize = 12.dp,
        )
    }
}

/**
 * Wochen- oder Monatshandlung zum Zuteilen auf die Tage der laufenden Woche.
 * Ist die Vorgabe des Zeitraums erfüllt, wird der Titel durchgestrichen.
 */
@Composable
private fun AssignRow(
    state: AppState,
    action: GoalAction,
    goal: Goal,
    onToggleDone: (Long, Long) -> Unit,
    onTogglePlan: (Long, Long) -> Unit,
) {
    val c = LocalAppColors.current
    val doneCount = state.doneInPeriod(action)
    val reached = doneCount >= action.times
    val locale = Locale.getDefault()

    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    action.title,
                    style = AppType.body,
                    color = if (reached) c.textSecondary else c.text,
                    textDecoration = if (reached) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    goal.title + " · " + frequencyText(action.period, action.times),
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
                color = if (reached) c.accent else c.textSecondary,
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
