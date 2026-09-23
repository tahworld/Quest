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
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onQuestGenerated: (String) -> Unit, onProjects: () -> Unit, onSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.generatedQuestId) { state.generatedQuestId?.let(onQuestGenerated) }
    Scaffold(topBar = { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) { TextButton(onClick = onProjects) { Text("项目") }; TextButton(onClick = onSettings) { Text("AI 设置") } } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("此刻，你想推进什么？", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp)); Text("把脑子里的想法告诉 Quest。越具体，行动越贴近你的方向。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(value = state.intention, onValueChange = viewModel::setIntention, modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp), placeholder = { Text("例如：我想探索 AI 产品，但不知道从哪里开始。") }, label = { Text("我现在的想法") })
            TextButton(onClick = viewModel::decideForMe) { Text(if (state.decideForMe) "✓ 不知道，由 Quest 决定" else "我不知道——替我决定") }
            Spacer(Modifier.height(20.dp)); Text("可用时间", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(5,15,30,60).forEach { value -> FilterChip(selected = state.selectedMinutes == value, onClick = { viewModel.selectMinutes(value) }, label = { Text("$value 分钟") }, modifier = Modifier.weight(1f)) } }
            Spacer(Modifier.height(20.dp)); Row(Modifier.fillMaxWidth()) { Text("当前精力", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.weight(1f)); Text("${state.energy}/5", color = MaterialTheme.colorScheme.primary) }
            Slider(value = state.energy.toFloat(), onValueChange = { viewModel.selectEnergy(it.roundToInt()) }, valueRange = 1f..5f, steps = 3)
            Spacer(Modifier.height(16.dp)); Text("可用条件", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(QuestResource.PHONE to "手机", QuestResource.COMPUTER to "电脑", QuestResource.QUIET_THINKING to "安静思考", QuestResource.CAN_MOVE_OR_EXERCISE to "可以走动/运动").forEach { (resource,label) -> FilterChip(selected = resource in state.resources, onClick = { viewModel.toggleResource(resource) }, label = { Text(label) }) } }
            Spacer(Modifier.height(28.dp)); state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(10.dp)) }
            Button(onClick = viewModel::generateQuest, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = !state.isGenerating) { if (state.isGenerating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text("生成行动") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
