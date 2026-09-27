package com.vika.quest.ui.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ResultScreen(vm: ResultViewModel, onDone: () -> Unit, onContinue: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)) {
            if (state.loading) { CircularProgressIndicator(); return@Column }
            val result = state.result ?: run { Text("结果未找到"); return@Column }
            Text("这一步，做到了。", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("留下的结果会帮助 Quest 判断下一步。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            Text("完成了什么", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(result.analysisSummary ?: result.resultText, style = MaterialTheme.typography.bodyLarge)
            result.insights.firstOrNull()?.let {
                Spacer(Modifier.height(24.dp)); Text("发现", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp)); Text(it)
            }
            result.suggestedNextStep?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(24.dp)); Text("可以接着做", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp)); Text(it)
            }
            Spacer(Modifier.height(36.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("先到这里") }
            OutlinedButton(onClick = { onContinue(result.suggestedNextStep.orEmpty()) }, modifier = Modifier.fillMaxWidth()) { Text("继续这个方向") }
        }
    }
}
