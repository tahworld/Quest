package com.vika.quest.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.ai.AiClarificationValidator
import com.vika.quest.ai.AiProvider
import com.vika.quest.ai.AiQuestDraftValidator
import com.vika.quest.ai.ClarificationExchange
import com.vika.quest.ai.ClarificationStatus
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
    val intention: String = "",
    val decideForMe: Boolean = false,
    val selectedMinutes: Int = 15,
    val energy: Int = 3,
    val resources: Set<QuestResource> = setOf(QuestResource.PHONE),
    val isGenerating: Boolean = false,
    val isClarifying: Boolean = false,
    val clarificationQuestion: String? = null,
    val clarificationOptions: List<String> = emptyList(),
    val allowCustomAnswer: Boolean = false,
    val clarificationAnswer: String = "",
    val clarificationHistory: List<ClarificationExchange> = emptyList(),
    val generatedQuestId: String? = null,
    val errorMessage: String? = null,
) {
    val isBusy: Boolean get() = isGenerating || isClarifying
    val isClarificationActive: Boolean get() = clarificationQuestion != null
    val clarificationNumber: Int get() = (clarificationHistory.size + 1).coerceAtMost(MAX_CLARIFICATION_QUESTIONS)
}

class HomeViewModel(
    private val savedState: SavedStateHandle,
    private val contextBuilder: ContextBuilder,
    private val questRepository: QuestRepository,
    private val aiProvider: AiProvider,
    private val questValidator: AiQuestDraftValidator,
    private val clarificationValidator: AiClarificationValidator,
) : ViewModel() {
    private val restoredHistory = restoreHistory()
    private val _uiState = MutableStateFlow(
        HomeUiState(
            intention = savedState[INTENTION] ?: "",
            decideForMe = savedState[DECIDE_FOR_ME] ?: false,
            selectedMinutes = savedState[MINUTES] ?: 15,
            energy = savedState[ENERGY] ?: 3,
            resources = restoredResources(),
            clarificationQuestion = savedState[CLARIFICATION_QUESTION],
            clarificationOptions = savedState.get<ArrayList<String>>(CLARIFICATION_OPTIONS).orEmpty(),
            allowCustomAnswer = savedState[CLARIFICATION_ALLOW_CUSTOM] ?: false,
            clarificationAnswer = savedState[CLARIFICATION_ANSWER] ?: "",
            clarificationHistory = restoredHistory,
            generatedQuestId = savedState[GENERATED_QUEST_ID],
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun setIntention(value: String) {
        savedState[INTENTION] = value
        savedState[DECIDE_FOR_ME] = false
        clearClarification()
        _uiState.update { it.withoutClarification().copy(intention = value, decideForMe = false, errorMessage = null) }
    }

    fun decideForMe() {
        savedState[INTENTION] = ""
        savedState[DECIDE_FOR_ME] = true
        clearClarification()
        _uiState.update { it.withoutClarification().copy(intention = "", decideForMe = true, errorMessage = null) }
    }

    fun selectMinutes(value: Int) {
        require(value in setOf(5, 15, 30, 60))
        savedState[MINUTES] = value
        _uiState.update { it.copy(selectedMinutes = value) }
    }

    fun selectEnergy(value: Int) {
        require(value in 1..5)
        savedState[ENERGY] = value
        _uiState.update { it.copy(energy = value) }
    }

    fun toggleResource(value: QuestResource) = _uiState.update { old ->
        val next = old.resources.toMutableSet().apply { if (!add(value)) remove(value) }
        savedState[RESOURCES] = ArrayList(next.map { it.name })
        old.copy(resources = next)
    }

    fun applyConditions(intention: String, minutes: Int, energy: Int, resources: List<String>) {
        val parsed = resources.mapNotNull { runCatching { QuestResource.valueOf(it) }.getOrNull() }.toSet().ifEmpty { setOf(QuestResource.PHONE) }
        savedState[INTENTION] = intention
        savedState[MINUTES] = minutes
        savedState[ENERGY] = energy
        savedState[RESOURCES] = ArrayList(parsed.map { it.name })
        savedState[DECIDE_FOR_ME] = false
        savedState.remove<String>(GENERATED_QUEST_ID)
        clearClarification()
        _uiState.update {
            it.withoutClarification().copy(
                intention = intention,
                decideForMe = false,
                selectedMinutes = minutes,
                energy = energy,
                resources = parsed,
                generatedQuestId = null,
                errorMessage = null,
            )
        }
    }

    fun generateQuest() {
        val current = _uiState.value
        if (current.isBusy || current.generatedQuestId != null) return
        clearClarification()
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        viewModelScope.launch { generateAndPersist(current.intention, current) }
    }

    fun startClarification() {
        val current = _uiState.value
        if (current.isBusy || current.generatedQuestId != null || current.isClarificationActive) return
        clearClarification()
        _uiState.update { it.copy(isClarifying = true, errorMessage = null) }
        viewModelScope.launch { requestClarification(current, emptyList()) }
    }

    fun setClarificationAnswer(value: String) {
        savedState[CLARIFICATION_ANSWER] = value
        _uiState.update { it.copy(clarificationAnswer = value, errorMessage = null) }
    }

    fun submitClarificationAnswer() {
        val current = _uiState.value
        val question = current.clarificationQuestion ?: return
        val answer = current.clarificationAnswer.trim()
        if (current.isBusy) return
        if (answer.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请选择一个答案或写下你的回答。") }
            return
        }
        val history = (current.clarificationHistory + ClarificationExchange(question, answer)).takeLast(MAX_CLARIFICATION_QUESTIONS)
        saveHistory(history)
        clearCurrentQuestion()
        if (history.size >= MAX_CLARIFICATION_QUESTIONS) {
            _uiState.update { it.copy(isGenerating = true, clarificationHistory = history, clarificationQuestion = null, clarificationAnswer = "", errorMessage = null) }
            viewModelScope.launch { generateAndPersist(refineLocally(current.intention, history, current.selectedMinutes), current) }
        } else {
            _uiState.update { it.copy(isClarifying = true, clarificationHistory = history, clarificationQuestion = null, clarificationAnswer = "", errorMessage = null) }
            viewModelScope.launch { requestClarification(current, history) }
        }
    }

    fun cancelClarification() {
        if (_uiState.value.isBusy) return
        clearClarification()
        _uiState.update {
            it.copy(
                clarificationQuestion = null,
                clarificationOptions = emptyList(),
                allowCustomAnswer = false,
                clarificationAnswer = "",
                clarificationHistory = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun consumeGeneratedQuest(id: String) {
        savedState.remove<String>(GENERATED_QUEST_ID)
        _uiState.update { if (it.generatedQuestId == id) it.copy(generatedQuestId = null) else it }
    }

    private suspend fun requestClarification(base: HomeUiState, history: List<ClarificationExchange>) {
        try {
            val context = contextBuilder.buildClarification(base.intention, base.selectedMinutes, base.energy, base.resources, history)
            val turn = clarificationValidator.validate(aiProvider.clarifyQuest(context), base.intention)
            when (turn.status) {
                ClarificationStatus.ASK -> {
                    saveCurrentQuestion(checkNotNull(turn.question), turn.options, turn.allowCustomAnswer)
                    _uiState.update {
                        it.copy(
                            isClarifying = false,
                            clarificationQuestion = turn.question,
                            clarificationOptions = turn.options,
                            allowCustomAnswer = turn.allowCustomAnswer,
                            clarificationAnswer = "",
                            clarificationHistory = history,
                        )
                    }
                }
                ClarificationStatus.READY -> {
                    clearCurrentQuestion()
                    _uiState.update { it.copy(isClarifying = false, isGenerating = true, clarificationHistory = history) }
                    generateAndPersist(checkNotNull(turn.refinedIntention), base)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _uiState.update { it.copy(isClarifying = false, isGenerating = false, errorMessage = error.message ?: "暂时无法澄清，请重试。") }
        }
    }

    private suspend fun generateAndPersist(intention: String, base: HomeUiState) {
        try {
            val context = contextBuilder.build(intention, base.selectedMinutes, base.energy, base.resources)
            val quest = questRepository.createQuest(questValidator.validate(aiProvider.generateQuest(context), context))
            savedState[GENERATED_QUEST_ID] = quest.id
            clearClarification()
            _uiState.update { it.copy(isGenerating = false, isClarifying = false, generatedQuestId = quest.id, errorMessage = null) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _uiState.update { it.copy(isGenerating = false, isClarifying = false, errorMessage = error.message ?: "暂时无法生成行动，请重试。") }
        }
    }

    private fun restoredResources(): Set<QuestResource> =
        savedState.get<ArrayList<String>>(RESOURCES)?.mapNotNull { runCatching { QuestResource.valueOf(it) }.getOrNull() }?.toSet().orEmpty().ifEmpty { setOf(QuestResource.PHONE) }

    private fun restoreHistory(): List<ClarificationExchange> {
        val questions = savedState.get<ArrayList<String>>(CLARIFICATION_HISTORY_QUESTIONS).orEmpty()
        val answers = savedState.get<ArrayList<String>>(CLARIFICATION_HISTORY_ANSWERS).orEmpty()
        return questions.zip(answers) { question, answer -> ClarificationExchange(question, answer) }.takeLast(MAX_CLARIFICATION_QUESTIONS)
    }

    private fun saveHistory(history: List<ClarificationExchange>) {
        savedState[CLARIFICATION_HISTORY_QUESTIONS] = ArrayList(history.map { it.question })
        savedState[CLARIFICATION_HISTORY_ANSWERS] = ArrayList(history.map { it.answer })
    }

    private fun saveCurrentQuestion(question: String, options: List<String>, allowCustom: Boolean) {
        savedState[CLARIFICATION_QUESTION] = question
        savedState[CLARIFICATION_OPTIONS] = ArrayList(options)
        savedState[CLARIFICATION_ALLOW_CUSTOM] = allowCustom
        savedState[CLARIFICATION_ANSWER] = ""
    }

    private fun clearCurrentQuestion() {
        savedState.remove<String>(CLARIFICATION_QUESTION)
        savedState[CLARIFICATION_OPTIONS] = arrayListOf<String>()
        savedState[CLARIFICATION_ALLOW_CUSTOM] = false
        savedState[CLARIFICATION_ANSWER] = ""
    }

    private fun clearClarification() {
        clearCurrentQuestion()
        savedState[CLARIFICATION_HISTORY_QUESTIONS] = arrayListOf<String>()
        savedState[CLARIFICATION_HISTORY_ANSWERS] = arrayListOf<String>()
    }

    private fun refineLocally(original: String, history: List<ClarificationExchange>, minutes: Int): String {
        val direction = original.trim().ifBlank { "根据我的长期目标选择当前最值得推进的方向" }
        return "$direction。结合我的回答：${history.joinToString("；") { it.answer }}。在 $minutes 分钟内形成明确、可保存的产出。"
    }

    private fun HomeUiState.withoutClarification() = copy(
        clarificationQuestion = null,
        clarificationOptions = emptyList(),
        allowCustomAnswer = false,
        clarificationAnswer = "",
        clarificationHistory = emptyList(),
    )

    companion object {
        const val INTENTION = "home_intention"
        const val MINUTES = "home_minutes"
        const val ENERGY = "home_energy"
        const val RESOURCES = "home_resources"
        const val DECIDE_FOR_ME = "home_decide_for_me"
        const val GENERATED_QUEST_ID = "home_generated_quest_id"
        const val CLARIFICATION_QUESTION = "home_clarification_question"
        const val CLARIFICATION_OPTIONS = "home_clarification_options"
        const val CLARIFICATION_ALLOW_CUSTOM = "home_clarification_allow_custom"
        const val CLARIFICATION_ANSWER = "home_clarification_answer"
        const val CLARIFICATION_HISTORY_QUESTIONS = "home_clarification_history_questions"
        const val CLARIFICATION_HISTORY_ANSWERS = "home_clarification_history_answers"
    }
}

private const val MAX_CLARIFICATION_QUESTIONS = 3
