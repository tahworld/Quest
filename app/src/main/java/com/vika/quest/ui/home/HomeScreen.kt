package com.vika.quest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vika.quest.ai.QuestResource
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onQuestGenerated: (String) -> Unit,
    onProjects: () -> Unit,
    onPersona: () -> Unit,
    onSettings: () -> Unit,
    onMentor: (String, Int, Int, List<String>) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.generatedQuestId) { state.generatedQuestId?.let(onQuestGenerated) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = onProjects) { Text("项目") }
                    TextButton(onClick = onPersona) { Text("协作") }
                    TextButton(onClick = onSettings) { Text("AI 设置") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text("此刻，你想推进什么？", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("把脑子里的想法告诉 Quest。越具体，行动越贴近你的方向。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.intention,
                onValueChange = viewModel::setIntention,
                modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp),
                placeholder = { Text("例如：我想探索 AI 产品，但不知道从哪里开始。") },
                label = { Text("我现在的想法") },
                enabled = !state.isBusy,
            )
            TextButton(onClick = viewModel::decideForMe, enabled = !state.isBusy) {
                Text(if (state.decideForMe) "✓ 不知道，由 Quest 决定" else "我不知道——替我决定")
            }
            Spacer(Modifier.height(18.dp))
            Text("可用时间", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 30, 60).forEach { value ->
                    FilterChip(
                        selected = state.selectedMinutes == value,
                        onClick = { viewModel.selectMinutes(value) },
                        label = { Text("$value 分钟") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isBusy,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("当前精力", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("${state.energy}/5", color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = state.energy.toFloat(),
                onValueChange = { viewModel.selectEnergy(it.roundToInt()) },
                valueRange = 1f..5f,
                steps = 3,
                enabled = !state.isBusy,
            )
            Spacer(Modifier.height(14.dp))
            Text("可用条件", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    QuestResource.PHONE to "手机",
                    QuestResource.COMPUTER to "电脑",
                    QuestResource.QUIET_THINKING to "安静思考",
                    QuestResource.CAN_MOVE_OR_EXERCISE to "可以走动/运动",
                ).forEach { (resource, label) ->
                    FilterChip(
                        selected = resource in state.resources,
                        onClick = { viewModel.toggleResource(resource) },
                        label = { Text(label) },
                        enabled = !state.isBusy,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
            }

            if (state.isClarificationActive) {
                ClarificationPanel(state, viewModel)
            } else {
                Button(
                    onClick = viewModel::generateQuest,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = !state.isBusy,
                ) {
                    if (state.isGenerating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("直接生成行动")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = viewModel::startClarification,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !state.isBusy,
                ) {
                    if (state.isClarifying) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("先帮我理清")
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { onMentor(state.intention, state.selectedMinutes, state.energy, state.resources.map { it.name }) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !state.isBusy,
                ) { Text("和导师聊聊") }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClarificationPanel(state: HomeUiState, viewModel: HomeViewModel) {
    Text("问题 ${state.clarificationNumber} / 3", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(8.dp))
    Text(checkNotNull(state.clarificationQuestion), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(14.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.clarificationOptions.forEach { option ->
            FilterChip(
                selected = state.clarificationAnswer == option,
                onClick = { viewModel.setClarificationAnswer(option) },
                label = { Text(option) },
                enabled = !state.isBusy,
            )
        }
    }
    if (state.allowCustomAnswer) {
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.clarificationAnswer,
            onValueChange = viewModel::setClarificationAnswer,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5,
            label = { Text("你的回答") },
            enabled = !state.isBusy,
        )
    }
    Spacer(Modifier.height(14.dp))
    Button(onClick = viewModel::submitClarificationAnswer, modifier = Modifier.fillMaxWidth(), enabled = !state.isBusy) {
        Text("继续")
    }
    TextButton(onClick = viewModel::cancelClarification, modifier = Modifier.fillMaxWidth(), enabled = !state.isBusy) {
        Text("取消，返回当前条件")
    }
}
