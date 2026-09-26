package com.vika.quest.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.ai.*
import com.vika.quest.data.local.entity.MemoryEntity
import com.vika.quest.data.local.entity.QuestEntity
import com.vika.quest.data.repository.*
import com.vika.quest.model.DifficultyRating
import com.vika.quest.model.QuestStatus
import com.vika.quest.model.RejectionReason
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class QuestDetailUiState(
    val isLoading: Boolean = true, val quest: QuestEntity? = null, val goalName: String = "",
    val isWorking: Boolean = false, val newQuestId: String? = null, val abandoned: Boolean = false,
    val showCompletion: Boolean = false, val resultText: String = "", val actualMinutes: String = "",
    val difficultyRating: DifficultyRating? = null, val usefulnessRating: Int? = null,
    val resultReady: Boolean = false, val errorMessage: String? = null,
)

class QuestDetailViewModel(
    private val questId: String,
    private val goals: GoalRepository,
    private val projects: ProjectRepository,
    private val memories: MemoryRepository,
    private val quests: QuestRepository,
    private val contextBuilder: ContextBuilder,
    private val ai: AiProvider,
    private val questValidator: AiQuestDraftValidator,
    private val resultValidator: AiQuestResultValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { viewModelScope.launch { combine(quests.observeQuests(), goals.observeGoals()) { q, g -> q.firstOrNull { it.id == questId } to g }.collect { (quest, allGoals) -> _uiState.update { it.copy(isLoading = false, quest = quest, goalName = allGoals.firstOrNull { goal -> goal.id == quest?.goalId }?.name.orEmpty(), errorMessage = if (quest == null) "未找到这个行动。" else it.errorMessage) } } } }

    fun startQuest() = work { quests.startQuest(questId) }
    fun abandonQuest() = work { quests.abandonQuest(questId); _uiState.update { it.copy(abandoned = true) } }
    fun showCompletion() = _uiState.update { it.copy(showCompletion = true, errorMessage = null) }
    fun setResultText(value: String) = _uiState.update { it.copy(resultText = value) }
    fun setActualMinutes(value: String) = _uiState.update { it.copy(actualMinutes = value.filter(Char::isDigit)) }
    fun setDifficulty(value: DifficultyRating) = _uiState.update { it.copy(difficultyRating = value) }
    fun setUsefulness(value: Int) = _uiState.update { it.copy(usefulnessRating = value) }

    fun reroll(reason: RejectionReason, details: String?) {
        val quest = _uiState.value.quest ?: return
        if (_uiState.value.isWorking || quest.status != QuestStatus.PENDING) return
        _uiState.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val resources = quest.sourceResources.mapNotNull { runCatching { QuestResource.valueOf(it) }.getOrNull() }.toSet()
                val reasonText = listOf(reason.storageValue, details.orEmpty()).filter(String::isNotBlank).joinToString(": ")
                val context = contextBuilder.build(quest.sourceIntention, quest.sourceAvailableMinutes, quest.sourceEnergy, resources, quest.id, reasonText)
                val replacement = questValidator.validate(ai.generateQuest(context), context)
                val created = quests.reroll(quest.id, reason, details, replacement)
                _uiState.update { it.copy(isWorking = false, newQuestId = created.id) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _uiState.update { it.copy(isWorking = false, errorMessage = error.message ?: "暂时无法更换行动。") } }
        }
    }

    fun submitResult() {
        val state = _uiState.value; val quest = state.quest ?: return
        if (state.isWorking) return
        if (state.resultText.isBlank()) { _uiState.update { it.copy(errorMessage = "请填写你产出或发现了什么。") }; return }
        val actual = state.actualMinutes.takeIf(String::isNotBlank)?.toIntOrNull()
        if (state.actualMinutes.isNotBlank() && actual == null) { _uiState.update { it.copy(errorMessage = "实际用时格式不正确。") }; return }
        _uiState.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = quests.completeQuest(quest.id, NewQuestResult(state.resultText, actual, state.difficultyRating, state.usefulnessRating))
                try {
                    val goal = quest.goalId?.let { goals.getGoal(it) }
                    val project = quest.projectId?.let { projects.getProject(it) }
                    val context = QuestResultAnalysisContext(
                        goal = goal?.let { GoalSnapshot(it.id, it.name, it.description, it.priority) },
                        project = project?.let { ProjectSnapshot(it.id, it.goalId, it.name, it.description, it.currentState) },
                        memories = memories.getImportant(ContextBuilder.MEMORY_LIMIT).map { MemorySnapshot(it.type, it.content, it.importance) },
                        quest = QuestSnapshot(quest.id, quest.goalId, quest.projectId, quest.title, QuestStatus.COMPLETED.name, result.resultText.take(500)),
                        steps = quest.steps.ifEmpty { listOf(quest.instruction) }, completionCriteria = quest.completionCriteria,
                        resultText = result.resultText, actualMinutes = result.actualMinutes, difficultyRating = result.difficultyRating?.name, usefulnessRating = result.usefulnessRating,
                        userPreferences = contextBuilder.getUserPreferences(),
                    )
                    val analysis = resultValidator.validate(ai.analyzeQuestResult(context))
                    val memoryEntities = analysis.memoriesToSave.map { MemoryEntity(UUID.randomUUID().toString(), quest.goalId, quest.projectId, it.type.trim(), it.content.trim(), it.importance, System.currentTimeMillis()) }
                    quests.saveAnalysis(quest.id, analysis, memoryEntities)
                } catch (analysisError: Exception) {
                    _uiState.update { it.copy(errorMessage = "结果已保存，但 AI 分析暂时失败：${analysisError.message}") }
                }
                _uiState.update { it.copy(isWorking = false, resultReady = true) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _uiState.update { it.copy(isWorking = false, errorMessage = error.message ?: "提交结果失败。") } }
        }
    }

    private fun work(block: suspend () -> Unit) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch { try { block(); _uiState.update { it.copy(isWorking = false) } } catch (e: CancellationException) { throw e } catch (e: Exception) { _uiState.update { it.copy(isWorking = false, errorMessage = e.message ?: "操作失败，请重试。") } } }
    }
}
