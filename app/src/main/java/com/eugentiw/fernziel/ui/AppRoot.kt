package com.eugentiw.fernziel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.eugentiw.fernziel.AppViewModel
import com.eugentiw.fernziel.R
import kotlinx.coroutines.launch

/**
 * Wurzel der Oberfläche: zwei Seiten (Heute, Ziele), zwischen denen man wischen kann,
 * dazu Verlauf, Monatsübersicht eines Ziels und Ziel-Editor als eigene Ansichten.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRoot(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    val hideDone by viewModel.hideDone.collectAsState()
    val c = LocalAppColors.current

    // Diese Zustände liegen bewusst hier oben, damit Seite und Scrollposition
    // erhalten bleiben, während eine andere Ansicht geöffnet ist.
    val pagerState = rememberPagerState(pageCount = { 2 })
    val todayScroll = rememberScrollState()
    val goalsScroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var detailGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editorGoalId by rememberSaveable { mutableStateOf<Long?>(null) }

    val detailGoal = state.goals.firstOrNull { it.id == detailGoalId }

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
                    detailGoalId = null
                    editorOpen = false
                },
                onSetAchieved = { goalId, achieved ->
                    viewModel.setAchieved(goalId, achieved)
                    editorOpen = false
                },
            )
        } else if (detailGoal != null) {
            BackHandler { detailGoalId = null }
            GoalDetailScreen(
                state = state,
                goal = detailGoal,
                onBack = { detailGoalId = null },
                onEdit = { openEditor(detailGoal.id) },
                onToggleDone = { actionId, day -> viewModel.toggleDone(actionId, day) },
            )
        } else if (historyOpen) {
            BackHandler { historyOpen = false }
            HistoryScreen(
                state = state,
                onBack = { historyOpen = false },
                onToggleDone = { actionId, day -> viewModel.toggleDone(actionId, day) },
                onToggleTodo = { todoId -> viewModel.toggleTodo(todoId) },
            )
        } else {
            BackHandler(enabled = pagerState.currentPage != 0) {
                scope.launch { pagerState.animateScrollToPage(0) }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    if (page == 0) {
                        TodayScreen(
                            state = state,
                            hideDone = hideDone,
                            scrollState = todayScroll,
                            onToggleHideDone = { viewModel.toggleHideDone() },
                            onOpenHistory = { historyOpen = true },
                            onToggleDone = { actionId, day -> viewModel.toggleDone(actionId, day) },
                            onTogglePlan = { actionId, day -> viewModel.togglePlan(actionId, day) },
                            onAddTodo = { title, day -> viewModel.addTodo(title, day) },
                            onToggleTodo = { todoId -> viewModel.toggleTodo(todoId) },
                            onDeleteTodo = { todoId -> viewModel.deleteTodo(todoId) },
                            onAddGoal = { openEditor(null) },
                        )
                    } else {
                        GoalsScreen(
                            state = state,
                            scrollState = goalsScroll,
                            onAdd = { openEditor(null) },
                            onOpen = { goalId -> detailGoalId = goalId },
                        )
                    }
                }
                BottomBar(
                    tab = pagerState.currentPage,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                )
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
