package com.vika.quest.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.data.repository.GoalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val input: String = "",
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
)

class OnboardingViewModel(
    private val goalRepository: GoalRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(input = value, errorMessage = null) }
    }

    fun submit() {
        val current = _uiState.value
        val text = current.input.trim()
        if (current.isSubmitting || current.isComplete) return
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a goal to continue.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                goalRepository.createGoal(
                    name = text,
                    description = text,
                )
                _uiState.update { it.copy(isSubmitting = false, isComplete = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Could not save your goal. Try again.",
                    )
                }
            }
        }
    }
}
