package com.eugentiw.fernziel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eugentiw.fernziel.AppState
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.data.Goal

/** Liste aller Ziele, mit erreichten Zielen in einem eigenen Abschnitt. */
@Composable
fun GoalsScreen(
    state: AppState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val c = LocalAppColors.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.goals_title),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                style = AppType.largeTitle,
                color = c.text,
            )
            GlyphButton(
                glyph = Glyph.PLUS,
                label = stringResource(R.string.add_goal),
                tint = c.accent,
                onClick = onAdd,
                glyphSize = 22.dp,
            )
        }
        Spacer(Modifier.height(20.dp))

        if (state.loaded && state.goals.isEmpty()) {
            EmptyState(onAdd)
        }

        state.activeGoals.forEachIndexed { index, goal ->
            if (index > 0) {
                Spacer(Modifier.height(12.dp))
            }
            GoalCard(state, goal, onOpen)
        }

        if (state.achievedGoals.isNotEmpty()) {
            SectionHeader(stringResource(R.string.section_achieved))
            state.achievedGoals.forEachIndexed { index, goal ->
                if (index > 0) {
                    Spacer(Modifier.height(12.dp))
                }
                GoalCard(state, goal, onOpen)
            }
        }

        Spacer(Modifier.height(36.dp))
        Text(
            stringResource(R.string.privacy_note),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            style = AppType.caption,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GoalCard(state: AppState, goal: Goal, onOpen: (Long) -> Unit) {
    val c = LocalAppColors.current
    val achievedDay = goal.achievedDay
    val actions = state.actionsOf(goal.id)
    val deadline = deadlineText(goal, state.today)

    Panel(onClick = { onOpen(goal.id) }) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(categoryLabelRes(goal.category)).uppercase(),
                style = AppType.overline,
                color = if (achievedDay == null) c.accent else c.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                goal.title,
                style = AppType.headline,
                color = if (achievedDay == null) c.text else c.textSecondary,
            )
            if (goal.criterion.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(goal.criterion, style = AppType.subhead, color = c.textSecondary)
            }
            if (achievedDay != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.achieved_on, formatDay(achievedDay)),
                    style = AppType.caption,
                    color = c.textSecondary,
                )
            } else {
                if (deadline.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(deadline, style = AppType.caption, color = c.textSecondary)
                }
                if (actions.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Hairline(startInset = 0.dp)
                    Spacer(Modifier.height(10.dp))
                    actions.forEach { action ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text(
                                action.title,
                                modifier = Modifier.weight(1f),
                                style = AppType.subhead,
                                color = c.text,
                            )
                            Text(
                                frequencyText(action.period, action.times),
                                modifier = Modifier.padding(start = 12.dp),
                                style = AppType.subhead,
                                color = c.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
