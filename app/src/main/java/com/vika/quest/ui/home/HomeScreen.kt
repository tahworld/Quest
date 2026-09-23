package com.vika.quest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vika.quest.ai.QuestResource
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onQuestGenerated: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.generatedQuestId) {
        state.generatedQuestId?.let(onQuestGenerated)
    }

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = "What can I do right now?",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.goal?.name ?: "Loading your goal…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(36.dp))
            SectionLabel("Available time")
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(5, 15, 30, 60).forEach { minutes ->
                    FilterChip(
                        selected = state.selectedMinutes == minutes,
                        onClick = { viewModel.selectMinutes(minutes) },
                        modifier = Modifier.weight(1f),
                        label = { Text("$minutes min") },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Energy")
                Spacer(Modifier.weight(1f))
                Text(
                    text = state.energy.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = state.energy.toFloat(),
                onValueChange = { viewModel.selectEnergy(it.roundToInt()) },
                valueRange = 1f..5f,
                steps = 3,
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Available resources")
            Spacer(Modifier.height(10.dp))
            ResourceRow(
                first = ResourceOption(QuestResource.PHONE, "Phone"),
                second = ResourceOption(QuestResource.COMPUTER, "Computer"),
                selected = state.resources,
                onToggle = viewModel::toggleResource,
            )
            ResourceRow(
                first = ResourceOption(QuestResource.QUIET_THINKING, "Quiet thinking"),
                second = ResourceOption(QuestResource.CAN_MOVE_OR_EXERCISE, "Can move/exercise"),
                selected = state.resources,
                onToggle = viewModel::toggleResource,
            )

            Spacer(Modifier.weight(1f))
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = viewModel::generateQuest,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.goal != null && !state.isGenerating,
            ) {
                Text(if (state.isGenerating) "Creating your Quest…" else "Give me a Quest")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}

private data class ResourceOption(
    val resource: QuestResource,
    val label: String,
)

@Composable
private fun ResourceRow(
    first: ResourceOption,
    second: ResourceOption,
    selected: Set<QuestResource>,
    onToggle: (QuestResource) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        FilterChip(
            selected = first.resource in selected,
            onClick = { onToggle(first.resource) },
            label = { Text(first.label) },
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = second.resource in selected,
            onClick = { onToggle(second.resource) },
            label = { Text(second.label) },
        )
    }
}
