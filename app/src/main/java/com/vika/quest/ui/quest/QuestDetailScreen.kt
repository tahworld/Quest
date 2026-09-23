package com.vika.quest.ui.quest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vika.quest.model.QuestStatus

@Composable
fun QuestDetailScreen(
    viewModel: QuestDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("Back")
            }

            if (state.isLoading) {
                Spacer(Modifier.weight(1f))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.weight(1f))
                return@Column
            }

            val quest = state.quest
            if (quest == null) {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = state.errorMessage ?: "Quest not found.",
                    color = MaterialTheme.colorScheme.error,
                )
                return@Column
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = state.goalName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = quest.title,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Text(
                    text = "${quest.estimatedMinutes} minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "  ·  Difficulty ${quest.difficulty}/5",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(28.dp))
            Text(
                text = quest.instruction,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Done when",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
            quest.completionCriteria.forEach { criterion ->
                Text(
                    text = "• $criterion",
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Spacer(Modifier.weight(1f))
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
            }

            if (quest.status == QuestStatus.PENDING) {
                Button(
                    onClick = viewModel::startQuest,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isStarting,
                ) {
                    Text(if (state.isStarting) "Starting…" else "Start Quest")
                }
            } else if (quest.status == QuestStatus.ACTIVE) {
                Text(
                    text = "Quest in progress",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
