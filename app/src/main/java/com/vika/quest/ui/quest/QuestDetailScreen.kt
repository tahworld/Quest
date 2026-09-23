package com.vika.quest.ui.quest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vika.quest.model.DifficultyRating
import com.vika.quest.model.QuestStatus
import com.vika.quest.model.RejectionReason

@Composable
fun QuestDetailScreen(viewModel: QuestDetailViewModel, onBack: () -> Unit, onAdjusted: (String, Int, Int, List<String>) -> Unit, onReplaced: (String) -> Unit, onResult: (String) -> Unit, onAbandoned: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var rejectionDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.newQuestId) { state.newQuestId?.let(onReplaced) }
    LaunchedEffect(state.resultReady) { if (state.resultReady) onResult(checkNotNull(state.quest).id) }
    LaunchedEffect(state.abandoned) { if (state.abandoned) onAbandoned() }
    Scaffold { padding -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)) {
        TextButton(onClick = onBack) { Text("返回") }
        if (state.isLoading) { CircularProgressIndicator(); return@Column }
        val quest = state.quest ?: run { Text(state.errorMessage ?: "未找到行动", color = MaterialTheme.colorScheme.error); return@Column }
        if (state.goalName.isNotBlank()) Text(state.goalName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text(quest.title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp)); Text("预计 ${quest.estimatedMinutes} 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val reason = quest.reason.ifBlank { "这项行动承接你当前的目标。" }
        Spacer(Modifier.height(16.dp)); Text(reason, style = MaterialTheme.typography.bodyLarge)
        Section("怎么做") { (quest.steps.ifEmpty { listOf(quest.instruction) }).forEachIndexed { index, step -> Text("${index + 1}. $step", modifier = Modifier.padding(vertical = 4.dp)) } }
        Section("完成标准") { quest.completionCriteria.forEach { Text("• $it", modifier = Modifier.padding(vertical = 4.dp)) } }
        Section("最终产出") { Text(quest.expectedOutput.ifBlank { "完成上述步骤并保留结果。" }) }
        state.errorMessage?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(24.dp))
        when (quest.status) {
            QuestStatus.PENDING -> {
                Button(onClick = viewModel::startQuest, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text("开始行动") }
                OutlinedButton(onClick = { rejectionDialog = true }, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text("换一个") }
                TextButton(onClick = { onAdjusted(quest.sourceIntention, quest.sourceAvailableMinutes, quest.sourceEnergy, quest.sourceResources) }, modifier = Modifier.fillMaxWidth()) { Text("调整条件") }
            }
            QuestStatus.ACTIVE -> {
                Text("行动进行中", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp)); Button(onClick = viewModel::showCompletion, modifier = Modifier.fillMaxWidth()) { Text("完成行动") }
                TextButton(onClick = viewModel::abandonQuest, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text("放弃") }
                if (state.showCompletion) CompletionForm(state, viewModel)
            }
            QuestStatus.COMPLETED -> Button(onClick = { onResult(quest.id) }, modifier = Modifier.fillMaxWidth()) { Text("查看结果") }
            QuestStatus.ABANDONED -> Text("这项行动已结束", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } }
    if (rejectionDialog) RejectionDialog(onDismiss = { rejectionDialog = false }, onSelect = { reason, details -> rejectionDialog = false; viewModel.reroll(reason, details) })
}

@Composable private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) { Spacer(Modifier.height(24.dp)); HorizontalDivider(); Spacer(Modifier.height(20.dp)); Text(title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)); Column(content = content) }

@Composable private fun CompletionForm(state: QuestDetailUiState, vm: QuestDetailViewModel) {
    Spacer(Modifier.height(20.dp)); OutlinedTextField(value = state.resultText, onValueChange = vm::setResultText, label = { Text("你产出或发现了什么？*") }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp))
    Spacer(Modifier.height(10.dp)); OutlinedTextField(value = state.actualMinutes, onValueChange = vm::setActualMinutes, label = { Text("实际用时（可选）") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp)); Text("难度（可选）"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(DifficultyRating.TOO_EASY to "太简单", DifficultyRating.APPROPRIATE to "合适", DifficultyRating.TOO_HARD to "太难").forEach { (value,label) -> FilterChip(selected = state.difficultyRating == value, onClick = { vm.setDifficulty(value) }, label = { Text(label) }) } }
    Text("有用程度（可选）"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (1..5).forEach { value -> FilterChip(selected = state.usefulnessRating == value, onClick = { vm.setUsefulness(value) }, label = { Text(value.toString()) }) } }
    Button(onClick = vm::submitResult, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text(if (state.isWorking) "正在保存…" else "提交结果") }
}

@Composable private fun RejectionDialog(onDismiss: () -> Unit, onSelect: (RejectionReason, String?) -> Unit) {
    var selected by remember { mutableStateOf<RejectionReason?>(null) }; var details by remember { mutableStateOf("") }
    val labels = linkedMapOf(RejectionReason.CANNOT_DO_NOW to "现在做不了", RejectionReason.NOT_INTERESTED to "不感兴趣", RejectionReason.TOO_EASY to "太简单", RejectionReason.TOO_HARD to "太难", RejectionReason.LOW_VALUE to "价值不高", RejectionReason.ALREADY_DONE to "已经做过", RejectionReason.JUST_WANT_ANOTHER to "只是想换一个", RejectionReason.OTHER to "其他")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("为什么想换一个？") }, text = { Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) { labels.forEach { (reason,label) -> TextButton(onClick = { selected = reason }, colors = ButtonDefaults.textButtonColors(contentColor = if (selected == reason) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)) { Text(if (selected == reason) "✓ $label" else label) } }; if (selected == RejectionReason.OTHER) OutlinedTextField(details, { details = it }, label = { Text("原因") }) } }, confirmButton = { TextButton(onClick = { selected?.let { onSelect(it, details.takeIf(String::isNotBlank)) } }, enabled = selected != null && (selected != RejectionReason.OTHER || details.isNotBlank())) { Text("重新生成") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
