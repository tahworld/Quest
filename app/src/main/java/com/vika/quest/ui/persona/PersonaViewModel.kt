package com.vika.quest.ui.persona

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.data.repository.UserPersona
import com.vika.quest.data.repository.UserPreferenceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonaUiState(
    val identityAndStage: String = "",
    val currentFocus: String = "",
    val guidanceStyle: String = "",
    val adviceToAvoid: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
)

class PersonaViewModel(private val repository: UserPreferenceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PersonaUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val persona = repository.getPersona()
            _uiState.value = PersonaUiState(
                identityAndStage = persona.identityAndStage,
                currentFocus = persona.currentFocus,
                guidanceStyle = persona.guidanceStyle,
                adviceToAvoid = persona.adviceToAvoid,
                isLoading = false,
            )
        }
    }

    fun setIdentity(value: String) = updateText { copy(identityAndStage = value) }
    fun setFocus(value: String) = updateText { copy(currentFocus = value) }
    fun setGuidance(value: String) = updateText { copy(guidanceStyle = value) }
    fun setAvoid(value: String) = updateText { copy(adviceToAvoid = value) }

    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.isComplete) return
        if (state.identityAndStage.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请先介绍你是谁以及当前所处的阶段。") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.savePersona(
                    UserPersona(
                        identityAndStage = state.identityAndStage,
                        currentFocus = state.currentFocus,
                        guidanceStyle = state.guidanceStyle,
                        adviceToAvoid = state.adviceToAvoid,
                    ),
                )
                _uiState.update { it.copy(isSaving = false, isComplete = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "暂时无法保存，请重试。") }
            }
        }
    }

    private fun updateText(block: PersonaUiState.() -> PersonaUiState) {
        _uiState.update { it.block().copy(errorMessage = null) }
    }
}
