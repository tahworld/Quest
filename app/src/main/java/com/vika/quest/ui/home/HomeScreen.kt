package com.vika.quest.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vika.quest.ai.QuestResource
import com.vika.quest.model.DirectionChoice
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onQuestGenerated: (String) -> Unit,
    onProjects: () -> Unit,
    onPersona: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var directionDialog by remember { mutableStateOf(false) }
    var showConditions by remember { mutableStateOf(false) }
    LaunchedEffect(state.generatedQuestId) { state.generatedQuestId?.let(onQuestGenerated) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = { Text("Quest") }, actions = {
                TextButton(onClick = onProjects) { Text("项目") }
                TextButton(onClick = onPersona) { Text("偏好") }
                TextButton(onClick = onSettings) { Text("AI") }
            })
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text("我在推进什么？", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            val direction = state.goals.firstOrNull { it.id == state.currentGoalId }
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("●", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                    Text(direction?.name ?: "先选一个方向", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            TextButton(onClick = { directionDialog = true }, enabled = !state.isBusy && !state.isSavingDirection) {
                Text(if (direction == null) "选择方向" else "更换方向")
            }
            state.recentProgress?.let {
                Spacer(Modifier.height(8.dp))
                Text("上次留下：$it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(28.dp))
            Text("今天最容易开始的下一步是什么？", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.intention,
                onValueChange = viewModel::setIntention,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                label = { Text("此刻的想法（可留空）") },
                placeholder = { Text("比如：继续昨天的阅读，或解决项目里最卡的一点") },
                enabled = !state.isBusy,
            )
            Text("留空时，Quest 会承接当前方向和上次的进展。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Text("${state.selectedMinutes} 分钟 · 精力 ${state.energy}/5 · ${state.resources.joinToString("、") { it.label() }.ifBlank { "未选择设备" }}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { showConditions = !showConditions }) { Text(if (showConditions) "收起条件" else "调整时间与条件") }
            if (showConditions) {
                Text("可用时间", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30, 60).forEach { minutes ->
                        FilterChip(state.selectedMinutes == minutes, { viewModel.selectMinutes(minutes) }, label = { Text("$minutes 分钟") }, enabled = !state.isBusy)
                    }
                }
                Text("精力 ${state.energy}/5", style = MaterialTheme.typography.titleMedium)
                Slider(state.energy.toFloat(), { viewModel.selectEnergy(it.roundToInt()) }, valueRange = 1f..5f, steps = 3, enabled = !state.isBusy)
                Text("可用条件", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestResource.entries.forEach { resource ->
                        FilterChip(resource in state.resources, { viewModel.toggleResource(resource) }, label = { Text(resource.label()) }, enabled = !state.isBusy)
                    }
                }
            }
            state.errorMessage?.let { Spacer(Modifier.height(10.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(26.dp))
            Button(onClick = viewModel::generateQuest, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !state.isBusy && !state.isSavingDirection && state.currentGoalId != null) {
                if (state.isGenerating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("给我今天的一步")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (directionDialog) DirectionDialog(
        state = state,
        onSelect = { viewModel.selectDirection(it); directionDialog = false },
        onCreate = { choice, text -> viewModel.addDirection(choice, text); directionDialog = false },
        onDismiss = { directionDialog = false },
    )
}

private fun QuestResource.label(): String = when (this) {
    QuestResource.PHONE -> "手机"
    QuestResource.COMPUTER -> "电脑"
    QuestResource.QUIET_THINKING -> "安静思考"
    QuestResource.CAN_MOVE_OR_EXERCISE -> "可以走动/运动"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectionDialog(state: HomeUiState, onSelect: (String) -> Unit, onCreate: (DirectionChoice, String) -> Unit, onDismiss: () -> Unit) {
    var choice by remember { mutableStateOf<DirectionChoice?>(null) }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择当前方向") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                state.goals.forEach { goal ->
                    TextButton(onClick = { onSelect(goal.id) }, modifier = Modifier.fillMaxWidth()) { Text(goal.name) }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("或者新建一个方向", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DirectionChoice.entries.forEach { option ->
                        FilterChip(choice == option, { choice = option }, label = { Text(option.label) })
                    }
                }
                OutlinedTextField(description, { description = it }, label = { Text("具体推进什么？") }, placeholder = { Text(choice?.hint ?: "先选类型") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { choice?.let { onCreate(it, description) } }, enabled = choice != null && description.isNotBlank()) { Text("使用新方向") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
