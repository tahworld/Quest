package com.vika.quest.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class QuestDetailUiState(
    val isLoading: Boolean = true,
    val quest: QuestEntity? = null,
    val goalName: String = "",
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
)

class QuestDetailViewModel(
    private val questId: String,
    private val goalRepository: GoalRepository,
    private val questRepository: QuestRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestDetailUiState())
    val uiState: StateFlow<QuestDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                questRepository.observeQuests(),
                goalRepository.observeGoals(),
            ) { quests, goals ->
                val quest = quests.firstOrNull { it.id == questId }
                val goalName = goals.firstOrNull { it.id == quest?.goalId }?.name.orEmpty()
                quest to goalName
            }.collect { (quest, goalName) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        quest = quest,
                        goalName = goalName,
                        isStarting = false,
                        errorMessage = if (quest == null) "未找到这个任务。" else null,
                    )
                }
            }
        }
    }

    fun startQuest() {
        val current = _uiState.value
        val quest = current.quest ?: return
        if (current.isStarting || quest.status != QuestStatus.PENDING) return

        _uiState.update { it.copy(isStarting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                questRepository.saveQuest(quest.copy(status = QuestStatus.ACTIVE))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isStarting = false,
                        errorMessage = error.message ?: "暂时无法开始任务，请重试。",
                    )
                }
            }
        }
    }
}
