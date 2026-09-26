package com.vika.quest.ui.mentor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vika.quest.ai.MentorMessage
import com.vika.quest.ai.MentorMessageRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorChatScreen(
    viewModel: MentorChatViewModel,
    onBack: () -> Unit,
    onQuestGenerated: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(state.generatedQuestId) { state.generatedQuestId?.let(onQuestGenerated) }
    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("和导师聊聊") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = { TextButton(onClick = viewModel::restart, enabled = !state.isLoading && !state.isGenerating) { Text("重新开始") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp)) {
            Text(
                "围绕当前方向讨论，最多 $MAX_USER_TURNS 轮 · ${state.userTurns}/$MAX_USER_TURNS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.messages) { message -> MentorMessageItem(message) }
                if (state.isLoading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text("导师正在整理信息……", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            state.errorMessage?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                TextButton(onClick = viewModel::retry, enabled = !state.isLoading && !state.isGenerating) { Text("重试回答") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::setInput,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                placeholder = { Text(if (state.userTurns >= MAX_USER_TURNS) "本轮问答已结束，请转成行动或重新开始" else "继续追问，或者补充你的判断……") },
                enabled = state.canSend,
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = viewModel::send,
                    modifier = Modifier.weight(1f),
                    enabled = state.canSend && state.input.isNotBlank(),
                ) { Text("发送") }
                Button(
                    onClick = viewModel::generateQuest,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading && !state.isGenerating && state.refinedIntention.isNotBlank(),
                ) {
                    if (state.isGenerating) CircularProgressIndicator(strokeWidth = 2.dp)
                    else Text(if (state.readyForAction) "转成行动" else "按当前信息生成")
                }
            }
        }
    }
}

@Composable
private fun MentorMessageItem(message: MentorMessage) {
    val fromUser = message.role == MentorMessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(if (fromUser) 0.82f else 0.94f),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(if (fromUser) "你" else "导师", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(message.content, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
