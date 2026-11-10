package com.eugentiw.fernziel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.eugentiw.fernziel.AppViewModel
import com.eugentiw.fernziel.R

/** Wurzel der Oberfläche: zwei Reiter (Heute, Ziele) und der Ziel-Editor als eigene Ansicht. */
@Composable
fun AppRoot(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    val c = LocalAppColors.current

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editorGoalId by rememberSaveable { mutableStateOf<Long?>(null) }

    val openEditor: (Long?) -> Unit = { goalId ->
        editorGoalId = goalId
        editorOpen = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        if (editorOpen) {
            BackHandler { editorOpen = false }
            GoalEditorScreen(
                state = state,
                goalId = editorGoalId,
                onClose = { editorOpen = false },
                onSave = { goal, actions ->
                    viewModel.saveGoal(goal, actions)
                    editorOpen = false
                },
                onDelete = { goalId ->
                    viewModel.deleteGoal(goalId)
                    editorOpen = false
                },
                onSetAchieved = { goalId, achieved ->
                    viewModel.setAchieved(goalId, achieved)
                    editorOpen = false
                },
            )
        } else {
            BackHandler(enabled = tab != 0) { tab = 0 }
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (tab == 0) {
                        TodayScreen(
                            state = state,
                            onToggleDone = { actionId, day -> viewModel.toggleDone(actionId, day) },
                            onTogglePlan = { actionId, day -> viewModel.togglePlan(actionId, day) },
                            onAddGoal = { openEditor(null) },
                        )
                    } else {
                        GoalsScreen(
                            state = state,
                            onAdd = { openEditor(null) },
                            onOpen = { goalId -> openEditor(goalId) },
                        )
                    }
                }
                BottomBar(tab = tab, onSelect = { tab = it })
            }
        }
    }
}

@Composable
private fun BottomBar(tab: Int, onSelect: (Int) -> Unit) {
    Column {
        Hairline(startInset = 0.dp)
        Row(Modifier.fillMaxWidth()) {
            BarItem(
                glyph = Glyph.CHECK_CIRCLE,
                label = stringResource(R.string.tab_today),
                selected = tab == 0,
                onClick = { onSelect(0) },
                modifier = Modifier.weight(1f),
            )
            BarItem(
                glyph = Glyph.TARGET,
                label = stringResource(R.string.tab_goals),
                selected = tab == 1,
                onClick = { onSelect(1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BarItem(
    glyph: Glyph,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    val tint = if (selected) c.accent else c.textSecondary
    Column(
        modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(22.dp)) { drawGlyph(glyph, tint) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = AppType.tab, color = tint)
    }
}
