package com.vika.quest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiSettingsScreen(vm: AiSettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("AI 设置") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text("当前使用：${state.activeProvider}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(state.baseUrl, vm::baseUrl, Modifier.fillMaxWidth(), label = { Text("Base URL") }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(state.model, vm::model, Modifier.fillMaxWidth(), label = { Text("模型") }, singleLine = true)
            if (state.availableModels.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("账号可用模型", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableModels.forEach { model ->
                        FilterChip(selected = state.model == model, onClick = { vm.selectModel(model) }, label = { Text(model) })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                state.apiKey,
                vm::apiKey,
                Modifier.fillMaxWidth(),
                label = { Text(if (state.hasSavedKey) "API Key（已保存，留空则不修改）" else "API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Spacer(Modifier.height(14.dp))
            state.message?.let {
                Text(it, color = if (it.startsWith("连接成功") || it == "设置已保存") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = vm::save, modifier = Modifier.weight(1f)) { Text("保存") }
                OutlinedButton(onClick = vm::test, enabled = !state.isTesting, modifier = Modifier.weight(1f)) {
                    Text(if (state.isTesting) "测试中…" else "测试连接")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("未配置 API Key 时，Quest 自动使用离线测试提供器。Key 只加密保存在本机。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
}
