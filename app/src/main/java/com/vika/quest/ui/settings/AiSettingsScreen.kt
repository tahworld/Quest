package com.vika.quest.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable fun AiSettingsScreen(vm: AiSettingsViewModel, onBack: () -> Unit) { val s by vm.state.collectAsStateWithLifecycle(); Scaffold { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
    TextButton(onClick = onBack) { Text("返回") }; Text("AI 设置", style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(20.dp)); Text("提供器：DeepSeek", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(s.baseUrl, vm::baseUrl, Modifier.fillMaxWidth(), label = { Text("Base URL") }); OutlinedTextField(s.model, vm::model, Modifier.fillMaxWidth(), label = { Text("模型") }); OutlinedTextField(s.apiKey, vm::apiKey, Modifier.fillMaxWidth(), label = { Text(if (s.hasSavedKey) "API Key（已保存，留空则不修改）" else "API Key") }, visualTransformation = PasswordVisualTransformation())
    Spacer(Modifier.height(12.dp)); s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }; Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = vm::save, modifier = Modifier.weight(1f)) { Text("保存") }; OutlinedButton(onClick = vm::test, enabled = !s.isTesting, modifier = Modifier.weight(1f)) { Text(if (s.isTesting) "测试中…" else "测试连接") } }
    Spacer(Modifier.height(16.dp)); Text("未配置 API Key 时，Quest 自动使用离线测试提供器。Key 只加密保存在本机。", color = MaterialTheme.colorScheme.onSurfaceVariant)
} } }
