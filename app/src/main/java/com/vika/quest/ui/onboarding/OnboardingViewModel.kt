package com.vika.quest.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.UserPreferenceKeys
import com.vika.quest.data.repository.UserPreferenceRepository
import com.vika.quest.model.DirectionChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val input: String = "",
    val selectedDirection: DirectionChoice? = null,
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
)

class OnboardingViewModel(
    private val goalRepository: GoalRepository,
    private val preferences: UserPreferenceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(input = value, errorMessage = null) }
    }

    fun selectDirection(value: DirectionChoice) {
        _uiState.update { it.copy(selectedDirection = value, errorMessage = null) }
    }

    fun submit() {
        val current = _uiState.value
        val text = current.input.trim()
        if (current.isSubmitting || current.isComplete) return
        if (current.selectedDirection == null || text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "先选一个方向，再写下你想推进的具体内容。") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val goal = goalRepository.createGoal(
                    name = text,
                    description = "${current.selectedDirection.label}：$text",
                )
                preferences.save(UserPreferenceKeys.ACTIVE_DIRECTION_GOAL_ID, goal.id)
                _uiState.update { it.copy(isSubmitting = false, isComplete = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "暂时无法保存目标，请重试。",
                    )
                }
            }
        }
    }
}
