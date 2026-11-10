package com.eugentiw.fernziel.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eugentiw.fernziel.AppState
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.data.Category
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Bearbeitungsstand einer Handlung im Editor. */
private class ActionDraft(
    val id: Long,
    val createdDay: Long,
    title: String,
    period: Period,
    times: Int,
    reminder: Int?,
) {
    var reminder by mutableStateOf(reminder)
    var title by mutableStateOf(title)
    var period by mutableStateOf(period)
    var times by mutableIntStateOf(times)
}

/** Legt ein Ziel an oder bearbeitet es: Titel, Lebensbereich, Zieltermin und Handlungen. */
@Composable
fun GoalEditorScreen(
    state: AppState,
    goalId: Long?,
    onClose: () -> Unit,
    onSave: (Goal, List<GoalAction>) -> Unit,
    onDelete: (Long) -> Unit,
    onSetAchieved: (Long, Boolean) -> Unit,
) {
    val c = LocalAppColors.current
    val existing: Goal? = remember(goalId) { state.goals.firstOrNull { it.id == goalId } }

    var goalTitle by remember(goalId) { mutableStateOf(existing?.title ?: "") }
    var criterion by remember(goalId) { mutableStateOf(existing?.criterion ?: "") }
    var category by remember(goalId) { mutableStateOf(existing?.category ?: Category.HEALTH) }
    var targetDay by remember(goalId) { mutableStateOf<Long?>(existing?.targetDay) }
    val drafts = remember(goalId) {
        val list = mutableStateListOf<ActionDraft>()
        if (existing != null) {
            state.actionsOf(existing.id).forEach { action ->
                list.add(ActionDraft(action.id, action.createdDay, action.title, action.period, action.times, action.reminderMinutes))
            }
        }
        if (list.isEmpty()) {
            list.add(ActionDraft(0L, state.today, "", Period.DAILY, 1, null))
        }
        list
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Freiwillige Erinnerungen: Android fragt beim ersten Einrichten nach der Erlaubnis.
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(notificationsEnabled(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsAllowed = granted }
    val requestNotifications: () -> Unit = {
        val enabled = notificationsEnabled(context)
        notificationsAllowed = enabled
        if (!enabled && Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val canSave = goalTitle.isNotBlank()
    val save: () -> Unit = {
        val goal = Goal(
            id = existing?.id ?: 0L,
            title = goalTitle.trim(),
            category = category,
            criterion = criterion.trim(),
            targetDay = targetDay,
            createdDay = existing?.createdDay ?: state.today,
            achievedDay = existing?.achievedDay,
        )
        val actions = drafts
            .filter { it.title.isNotBlank() }
            .map { draft ->
                GoalAction(
                    id = draft.id,
                    title = draft.title.trim(),
                    period = draft.period,
                    times = if (draft.period == Period.DAILY) 1 else draft.times,
                    createdDay = draft.createdDay,
                    reminderMinutes = draft.reminder,
                )
            }
        onSave(goal, actions)
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        // Kopfzeile
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextAction(stringResource(R.string.cancel), c.accent, onClose)
            Text(
                stringResource(if (existing == null) R.string.new_goal else R.string.edit_goal),
                modifier = Modifier.weight(1f),
                style = AppType.headline,
                color = c.text,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            TextAction(stringResource(R.string.save), c.accent, save, enabled = canSave, bold = true)
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp)
        ) {
            Panel {
                PlainField(goalTitle, { goalTitle = it }, stringResource(R.string.field_title))
                Hairline()
                PlainField(criterion, { criterion = it }, stringResource(R.string.field_criterion))
            }

            SectionHeader(stringResource(R.string.section_category))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Category.entries.forEach { item ->
                    Chip(
                        text = stringResource(categoryLabelRes(item)),
                        selected = item == category,
                        onClick = { category = item },
                    )
                }
            }

            SectionHeader(stringResource(R.string.section_deadline))
            Panel {
                val currentTarget = targetDay
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.deadline_label),
                        modifier = Modifier.weight(1f),
                        style = AppType.body,
                        color = c.text,
                    )
                    Text(
                        if (currentTarget != null) formatDay(currentTarget) else stringResource(R.string.no_date),
                        style = AppType.body,
                        color = if (currentTarget != null) c.accent else c.textSecondary,
                    )
                }
                if (currentTarget != null) {
                    Hairline()
                    TextAction(
                        text = stringResource(R.string.remove_date),
                        color = c.textSecondary,
                        onClick = { targetDay = null },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            SectionHeader(stringResource(R.string.section_actions))
            Text(
                stringResource(R.string.actions_hint),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
                style = AppType.caption,
                color = c.textSecondary,
            )
            drafts.forEachIndexed { index, draft ->
                key(draft) {
                    if (index > 0) {
                        Spacer(Modifier.height(12.dp))
                    }
                    ActionEditor(
                        draft = draft,
                        notificationsAllowed = notificationsAllowed,
                        onReminderSet = requestNotifications,
                        onRemove = { drafts.remove(draft) },
                    )
                }
            }
            TextAction(
                text = stringResource(R.string.add_action),
                color = c.accent,
                onClick = { drafts.add(ActionDraft(0L, state.today, "", Period.DAILY, 1, null)) },
                modifier = Modifier.padding(top = 4.dp),
            )

            if (existing != null) {
                Spacer(Modifier.height(28.dp))
                Panel {
                    TextAction(
                        text = stringResource(
                            if (existing.achievedDay == null) R.string.mark_achieved else R.string.reactivate
                        ),
                        color = c.accent,
                        onClick = { onSetAchieved(existing.id, existing.achievedDay == null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                    Hairline()
                    TextAction(
                        text = stringResource(R.string.delete_goal),
                        color = c.danger,
                        onClick = { confirmDelete = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DateDialog(
            initialDay = targetDay,
            onDismiss = { showDatePicker = false },
            onPick = { day ->
                targetDay = day
                showDatePicker = false
            },
        )
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { onDelete(existing.id) }) {
                    Text(stringResource(R.string.delete), color = c.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ActionEditor(
    draft: ActionDraft,
    notificationsAllowed: Boolean,
    onReminderSet: () -> Unit,
    onRemove: () -> Unit,
) {
    val c = LocalAppColors.current
    var showTime by remember { mutableStateOf(false) }
    val reminder = draft.reminder
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlainField(
                value = draft.title,
                onValueChange = { draft.title = it },
                placeholder = stringResource(R.string.action_placeholder),
                modifier = Modifier.weight(1f),
            )
            GlyphButton(
                glyph = Glyph.CLOSE,
                label = stringResource(R.string.remove_action),
                tint = c.textTertiary,
                onClick = onRemove,
                glyphSize = 13.dp,
            )
        }
        Hairline()
        Column(Modifier.padding(12.dp)) {
            Segmented(
                options = listOf(
                    stringResource(R.string.period_daily),
                    stringResource(R.string.period_weekly),
                    stringResource(R.string.period_monthly),
                ),
                selected = draft.period.ordinal,
                onSelect = { index ->
                    val next = Period.entries[index]
                    if (next != draft.period) {
                        val wasDaily = draft.period == Period.DAILY
                        draft.times = when (next) {
                            Period.DAILY -> 1
                            Period.WEEKLY -> if (wasDaily) 3 else draft.times.coerceIn(1, 7)
                            Period.MONTHLY -> if (wasDaily) 2 else draft.times.coerceIn(1, 31)
                        }
                        draft.period = next
                    }
                },
            )
            if (draft.period != Period.DAILY) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        frequencyText(draft.period, draft.times),
                        modifier = Modifier.weight(1f),
                        style = AppType.body,
                        color = c.text,
                    )
                    Stepper(
                        value = draft.times,
                        min = 1,
                        max = if (draft.period == Period.WEEKLY) 7 else 31,
                        onChange = { draft.times = it },
                    )
                }
            }
        }
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { showTime = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminder), style = AppType.body, color = c.text)
                Text(
                    stringResource(
                        when (draft.period) {
                            Period.DAILY -> R.string.reminder_desc_daily
                            Period.WEEKLY -> R.string.reminder_desc_weekly
                            else -> R.string.reminder_desc_monthly
                        }
                    ),
                    style = AppType.caption,
                    color = c.textSecondary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (reminder != null) formatTime(reminder) else stringResource(R.string.reminder_off),
                style = AppType.body,
                color = if (reminder != null) c.accent else c.textSecondary,
            )
        }
        if (reminder != null && !notificationsAllowed) {
            Text(
                stringResource(R.string.notifications_blocked),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                style = AppType.caption,
                color = c.danger,
            )
        }
    }

    if (showTime) {
        TimeDialog(
            initialMinutes = reminder,
            onDismiss = { showTime = false },
            onPick = { minutes ->
                draft.reminder = minutes
                showTime = false
                onReminderSet()
            },
            onRemove = {
                draft.reminder = null
                showTime = false
            },
        )
    }
}

/** Ob die App derzeit Benachrichtigungen anzeigen darf. */
private fun notificationsEnabled(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java)
    return manager != null && manager.areNotificationsEnabled()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    initialMinutes: Int?,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val initial = initialMinutes ?: 8 * 60
    val pickerState = rememberTimePickerState(
        initialHour = initial / 60,
        initialMinute = initial % 60,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(pickerState.hour * 60 + pickerState.minute) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { if (initialMinutes != null) onRemove() else onDismiss() }) {
                Text(stringResource(if (initialMinutes != null) R.string.reminder_remove else R.string.cancel))
            }
        },
        text = { TimePicker(state = pickerState) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initialDay: Long?, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val initialDate = if (initialDay != null) LocalDate.ofEpochDay(initialDay) else LocalDate.now()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
