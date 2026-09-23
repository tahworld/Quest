package com.vika.quest.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.ai.AiProvider
import com.vika.quest.ai.AiQuestDraftValidator
import com.vika.quest.ai.ContextBuilder
import com.vika.quest.ai.QuestResource
import com.vika.quest.data.repository.QuestRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val intention: String = "", val decideForMe: Boolean = false,
    val selectedMinutes: Int = 15, val energy: Int = 3,
    val resources: Set<QuestResource> = setOf(QuestResource.PHONE),
    val isGenerating: Boolean = false, val generatedQuestId: String? = null, val errorMessage: String? = null,
)

class HomeViewModel(
    private val savedState: SavedStateHandle,
    private val contextBuilder: ContextBuilder,
    private val questRepository: QuestRepository,
    private val aiProvider: AiProvider,
    private val validator: AiQuestDraftValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(
        intention = savedState[INTENTION] ?: "", selectedMinutes = savedState[MINUTES] ?: 15,
        energy = savedState[ENERGY] ?: 3,
        resources = (savedState.get<ArrayList<String>>(RESOURCES)?.mapNotNull { runCatching { QuestResource.valueOf(it) }.getOrNull() }?.toSet()).orEmpty().ifEmpty { setOf(QuestResource.PHONE) },
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun setIntention(value: String) { savedState[INTENTION] = value; _uiState.update { it.copy(intention = value, decideForMe = false, errorMessage = null) } }
    fun decideForMe() { savedState[INTENTION] = ""; _uiState.update { it.copy(intention = "", decideForMe = true, errorMessage = null) } }
    fun selectMinutes(value: Int) { require(value in setOf(5, 15, 30, 60)); savedState[MINUTES] = value; _uiState.update { it.copy(selectedMinutes = value) } }
    fun selectEnergy(value: Int) { require(value in 1..5); savedState[ENERGY] = value; _uiState.update { it.copy(energy = value) } }
    fun toggleResource(value: QuestResource) = _uiState.update { old ->
        val next = old.resources.toMutableSet().apply { if (!add(value)) remove(value) }
        savedState[RESOURCES] = ArrayList(next.map { it.name }); old.copy(resources = next)
    }

    fun applyConditions(intention: String, minutes: Int, energy: Int, resources: List<String>) {
        val parsed = resources.mapNotNull { runCatching { QuestResource.valueOf(it) }.getOrNull() }.toSet()
        savedState[INTENTION] = intention; savedState[MINUTES] = minutes; savedState[ENERGY] = energy; savedState[RESOURCES] = ArrayList(resources)
        _uiState.update { it.copy(intention = intention, selectedMinutes = minutes, energy = energy, resources = parsed, generatedQuestId = null) }
    }

    fun generateQuest() {
        val current = _uiState.value
        if (current.isGenerating || current.generatedQuestId != null) return
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val context = contextBuilder.build(current.intention, current.selectedMinutes, current.energy, current.resources)
                val quest = questRepository.createQuest(validator.validate(aiProvider.generateQuest(context), context))
                _uiState.update { it.copy(isGenerating = false, generatedQuestId = quest.id) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _uiState.update { it.copy(isGenerating = false, errorMessage = error.message ?: "暂时无法生成行动，请重试。") } }
        }
    }

    fun consumeGeneratedQuest(id: String) = _uiState.update { if (it.generatedQuestId == id) it.copy(generatedQuestId = null) else it }

    companion object { const val INTENTION = "home_intention"; const val MINUTES = "home_minutes"; const val ENERGY = "home_energy"; const val RESOURCES = "home_resources" }
}
