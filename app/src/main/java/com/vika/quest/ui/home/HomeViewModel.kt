package com.vika.quest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.ai.AiProvider
import com.vika.quest.ai.AiQuestDraftValidator
import com.vika.quest.ai.GoalSnapshot
import com.vika.quest.ai.QuestGenerationContext
import com.vika.quest.ai.QuestResource
import com.vika.quest.ai.QuestSnapshot
import com.vika.quest.data.local.entity.GoalEntity
import com.vika.quest.data.local.entity.QuestEntity
import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.QuestRepository
import com.vika.quest.model.QuestStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val goal: GoalEntity? = null,
    val selectedMinutes: Int = 15,
    val energy: Int = 3,
    val resources: Set<QuestResource> = setOf(QuestResource.PHONE),
    val isGenerating: Boolean = false,
    val generatedQuestId: String? = null,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val goalRepository: GoalRepository,
    private val questRepository: QuestRepository,
    private val aiProvider: AiProvider,
    private val validator: AiQuestDraftValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var recentQuests: List<QuestEntity> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                goalRepository.observeGoals(),
                questRepository.observeRecentQuests(),
            ) { goals, quests -> goals.firstOrNull() to quests }
                .collect { (goal, quests) ->
                    recentQuests = quests
                    _uiState.update { it.copy(goal = goal) }
                }
        }
    }

    fun selectMinutes(minutes: Int) {
        require(minutes in setOf(5, 15, 30, 60)) { "不支持该时间选项" }
        _uiState.update { it.copy(selectedMinutes = minutes, errorMessage = null) }
    }

    fun selectEnergy(energy: Int) {
        require(energy in 1..5) { "精力必须在 1 到 5 之间" }
        _uiState.update { it.copy(energy = energy, errorMessage = null) }
    }

    fun toggleResource(resource: QuestResource) {
        _uiState.update { current ->
            val resources = current.resources.toMutableSet().apply {
                if (!add(resource)) remove(resource)
            }
            current.copy(resources = resources, errorMessage = null)
        }
    }

    fun generateQuest() {
        val current = _uiState.value
        if (current.isGenerating || current.generatedQuestId != null) return
        val goal = current.goal
        if (goal == null) {
            _uiState.update { it.copy(errorMessage = "当前没有可用目标。") }
            return
        }

        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val context = buildContext(goal, current)
                val draft = aiProvider.generateQuest(context)
                val newQuest = validator.validate(draft, context)
                val quest = questRepository.createQuest(newQuest)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedQuestId = quest.id,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = error.message ?: "暂时无法生成任务，请重试。",
                    )
                }
            }
        }
    }

    fun consumeGeneratedQuest(questId: String) {
        _uiState.update { current ->
            if (current.generatedQuestId == questId) current.copy(generatedQuestId = null) else current
        }
    }

    private fun buildContext(
        goal: GoalEntity,
        state: HomeUiState,
    ) = QuestGenerationContext(
        activeGoals = listOf(
            GoalSnapshot(
                id = goal.id,
                name = goal.name,
                description = goal.description,
                priority = goal.priority,
            ),
        ),
        skills = emptyList(),
        recentQuests = recentQuests.map { quest ->
            QuestSnapshot(
                id = quest.id,
                goalId = quest.goalId,
                chainId = quest.chainId,
                title = quest.title,
                result = null,
                completed = quest.status == QuestStatus.COMPLETED,
            )
        },
        recentFeedback = emptyList(),
        availableMinutes = state.selectedMinutes,
        energy = state.energy,
        resources = state.resources,
        unfinishedChain = null,
    )
}
