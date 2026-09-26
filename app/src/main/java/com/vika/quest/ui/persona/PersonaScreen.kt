package com.vika.quest.ui.persona

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    viewModel: PersonaViewModel,
    isEditing: Boolean,
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isComplete) { if (state.isComplete) onComplete() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (isEditing) {
                TopAppBar(
                    title = { Text("协作设定") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                )
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            if (!isEditing) Text("让 Quest 了解你", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(if (isEditing) 4.dp else 10.dp))
            Text(
                "这些信息决定 Quest 怎样理解你、怎样提问，以及采用哪种导师视角。之后可以随时修改。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.identityAndStage,
                onValueChange = viewModel::setIdentity,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                label = { Text("你是谁，现在处于什么阶段？*") },
                placeholder = { Text("例如：我是时间碎片化的消防员，正在准备机械考研，也在开发个人 Android 产品。") },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.currentFocus,
                onValueChange = viewModel::setFocus,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                label = { Text("你目前长期想推进什么？") },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.guidanceStyle,
                onValueChange = viewModel::setGuidance,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                label = { Text("你希望怎样被指导？") },
                placeholder = { Text("例如：直接、具体、有证据，发现逻辑漏洞时明确指出。") },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.adviceToAvoid,
                onValueChange = viewModel::setAvoid,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                label = { Text("不希望看到什么建议？") },
                placeholder = { Text("例如：空泛鼓励、脱离现实条件的长期计划。") },
            )
            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isSaving,
            ) {
                Text(if (state.isSaving) "正在保存…" else if (isEditing) "保存设定" else "继续")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
