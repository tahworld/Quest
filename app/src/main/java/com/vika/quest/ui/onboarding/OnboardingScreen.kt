package com.vika.quest.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "What do you want to move forward?",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tell Quest what you want to improve, build, learn or accomplish.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your goal") },
                minLines = 4,
                maxLines = 7,
                enabled = !state.isSubmitting,
                isError = state.errorMessage != null,
                supportingText = state.errorMessage?.let { message ->
                    { Text(message) }
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Examples: Build commercial skills · Learn Android development · Improve my writing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting,
            ) {
                Text(if (state.isSubmitting) "Saving…" else "Start")
            }
        }
    }
}
