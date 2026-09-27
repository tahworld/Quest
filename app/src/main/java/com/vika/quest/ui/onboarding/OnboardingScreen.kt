package com.vika.quest.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vika.quest.model.DirectionChoice

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "选一个想慢慢推进的方向",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "不需要完整计划。先决定方向，今天只走容易开始的一步。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DirectionChoice.entries.forEach { choice ->
                    FilterChip(
                        selected = state.selectedDirection == choice,
                        onClick = { viewModel.selectDirection(choice) },
                        label = { Text(choice.label) },
                        enabled = !state.isSubmitting,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("具体想推进什么？") },
                placeholder = { Text(state.selectedDirection?.hint ?: "先选一个方向") },
                minLines = 2,
                maxLines = 5,
                enabled = !state.isSubmitting,
                isError = state.errorMessage != null,
                supportingText = state.errorMessage?.let { message ->
                    { Text(message) }
                },
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting,
            ) {
                Text(if (state.isSubmitting) "正在保存…" else "开始")
            }
        }
    }
}
