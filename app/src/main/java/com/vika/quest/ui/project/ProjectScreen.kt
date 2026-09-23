package com.vika.quest.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable fun ProjectScreen(vm: ProjectViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold { padding -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)) {
        TextButton(onClick = onBack) { Text("返回") }; Text("项目", style = MaterialTheme.typography.headlineLarge); Text("保存长期背景，让下一次行动承接真实进度。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp)); s.projects.forEach { project -> ListItem(headlineContent = { Text(project.name) }, supportingContent = { Text(project.currentState.ifBlank { project.description }) }, trailingContent = { TextButton(onClick = { vm.edit(project) }) { Text("编辑") } }); HorizontalDivider() }
        Spacer(Modifier.height(20.dp)); Text(if (s.editingId == null) "新建项目" else "编辑项目", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(s.name, vm::name, Modifier.fillMaxWidth(), label = { Text("名称*") }); OutlinedTextField(s.description, vm::description, Modifier.fillMaxWidth(), label = { Text("你想完成什么？") }); OutlinedTextField(s.currentState, vm::currentState, Modifier.fillMaxWidth().heightIn(min = 100.dp), label = { Text("现在进展到哪里？") })
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Button(onClick = vm::save, Modifier.fillMaxWidth()) { Text("保存") }
    } }
}
