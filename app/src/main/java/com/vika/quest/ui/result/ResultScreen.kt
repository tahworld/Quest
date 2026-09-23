package com.vika.quest.ui.result

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable fun ResultScreen(vm: ResultViewModel, onDone: () -> Unit, onContinue: (String) -> Unit) { val s by vm.state.collectAsStateWithLifecycle(); Scaffold { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { if (s.loading) { CircularProgressIndicator(); return@Column }; val r = s.result ?: run { Text("结果未找到"); return@Column }; Text("已完成", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(28.dp)); Text("完成了什么", style = MaterialTheme.typography.titleMedium); Text(r.analysisSummary ?: r.resultText); r.insights.firstOrNull()?.let { Spacer(Modifier.height(24.dp)); Text("学到了什么", style = MaterialTheme.typography.titleMedium); Text(it) }; r.suggestedNextStep?.takeIf(String::isNotBlank)?.let { Spacer(Modifier.height(24.dp)); Text("下一步方向", style = MaterialTheme.typography.titleMedium); Text(it) }; Spacer(Modifier.weight(1f)); Button(onClick = onDone, Modifier.fillMaxWidth()) { Text("完成") }; OutlinedButton(onClick = { onContinue(r.suggestedNextStep.orEmpty()) }, Modifier.fillMaxWidth()) { Text("继续") } } } }
