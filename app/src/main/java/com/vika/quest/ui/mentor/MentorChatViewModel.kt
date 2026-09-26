package com.vika.quest.ui.mentor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.ai.AiMentorReplyValidator
import com.vika.quest.ai.AiProvider
import com.vika.quest.ai.AiQuestDraftValidator
import com.vika.quest.ai.ContextBuilder
import com.vika.quest.ai.MentorMessage
import com.vika.quest.ai.MentorMessageRole
import com.vika.quest.ai.QuestResource
import com.vika.quest.data.repository.QuestRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MentorChatUiState(
    val messages: List<MentorMessage> = emptyList(),
    val input: String = "",
    val refinedIntention: String = "",
    val readyForAction: Boolean = false,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generatedQuestId: String? = null,
    val errorMessage: String? = null,
) {
    val userTurns: Int get() = messages.count { it.role == MentorMessageRole.USER }
    val canSend: Boolean get() = !isLoading && !isGenerating && userTurns < MAX_USER_TURNS
}

class MentorChatViewModel(
    private val savedState: SavedStateHandle,
    private val contextBuilder: ContextBuilder,
    private val questRepository: QuestRepository,
    private val aiProvider: AiProvider,
    private val mentorReplyValidator: AiMentorReplyValidator,
    private val questValidator: AiQuestDraftValidator,
    initialIntention: String,
    initialMinutes: Int,
    initialEnergy: Int,
    initialResources: List<String>,
) : ViewModel() {
    private val intention = savedState.get<String>(INTENTION) ?: initialIntention.also { savedState[INTENTION] = it }
    private val minutes = savedState.get<Int>(MINUTES) ?: initialMinutes.also { savedState[MINUTES] = it }
    private val energy = savedState.get<Int>(ENERGY) ?: initialEnergy.also { savedState[ENERGY] = it }
    private val resources = restoreResources(savedState.get<ArrayList<String>>(RESOURCES)?.toList() ?: initialResources).also {
        savedState[RESOURCES] = ArrayList(it.map(QuestResource::name))
    }
    private val restoredMessages = restoreMessages()
    private val _uiState = MutableStateFlow(
        MentorChatUiState(
            messages = restoredMessages,
            input = savedState[INPUT] ?: "",
            refinedIntention = savedState[REFINED_INTENTION] ?: intention,
            readyForAction = savedState[READY_FOR_ACTION] ?: false,
            generatedQuestId = savedState[GENERATED_QUEST_ID],
        ),
    )
    val uiState: StateFlow<MentorChatUiState> = _uiState.asStateFlow()

    init {
        if (savedState[AWAITING_REPLY] == true || restoredMessages.isEmpty()) requestReply(restoredMessages)
    }

    fun setInput(value: String) {
        val bounded = value.take(1_000)
        savedState[INPUT] = bounded
        _uiState.update { it.copy(input = bounded, errorMessage = null) }
    }

    fun send() {
        val current = _uiState.value
        val text = current.input.trim()
        if (!current.canSend || text.isBlank()) return
        val messages = current.messages + MentorMessage(MentorMessageRole.USER, text)
        saveMessages(messages)
        savedState[INPUT] = ""
        _uiState.update { it.copy(messages = messages, input = "", errorMessage = null) }
        requestReply(messages)
    }

    fun retry() {
        val current = _uiState.value
        if (current.isLoading || current.isGenerating) return
        requestReply(current.messages)
    }

    fun restart() {
        if (_uiState.value.isLoading || _uiState.value.isGenerating) return
        saveMessages(emptyList())
        savedState[INPUT] = ""
        savedState[REFINED_INTENTION] = intention
        savedState[READY_FOR_ACTION] = false
        _uiState.value = MentorChatUiState(refinedIntention = intention)
        requestReply(emptyList())
    }

    fun generateQuest() {
        val current = _uiState.value
        if (current.isLoading || current.isGenerating || current.generatedQuestId != null) return
        val refined = current.refinedIntention.trim().ifBlank { intention.trim() }
        if (refined.isBlank()) {
            _uiState.update { it.copy(errorMessage = "先告诉导师你想讨论或推进什么。") }
            return
        }
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val context = contextBuilder.build(refined, minutes, energy, resources)
                val quest = questRepository.createQuest(questValidator.validate(aiProvider.generateQuest(context), context))
                savedState[GENERATED_QUEST_ID] = quest.id
                _uiState.update { it.copy(isGenerating = false, generatedQuestId = quest.id) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(isGenerating = false, errorMessage = error.message ?: "暂时无法生成行动，请重试。") }
            }
        }
    }

    fun consumeGeneratedQuest(id: String) {
        savedState.remove<String>(GENERATED_QUEST_ID)
        _uiState.update { if (it.generatedQuestId == id) it.copy(generatedQuestId = null) else it }
    }

    private fun requestReply(messages: List<MentorMessage>) {
        if (_uiState.value.isLoading || _uiState.value.isGenerating) return
        savedState[AWAITING_REPLY] = true
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val context = contextBuilder.buildMentorConversation(intention, minutes, energy, resources, messages)
                val reply = mentorReplyValidator.validate(aiProvider.continueMentorConversation(context))
                val visibleContent = listOfNotNull(reply.answer, reply.followUpQuestion).joinToString("\n\n")
                val updated = messages + MentorMessage(MentorMessageRole.MENTOR, visibleContent)
                saveMessages(updated)
                savedState[REFINED_INTENTION] = reply.refinedIntention
                savedState[READY_FOR_ACTION] = reply.readyForAction
                savedState[AWAITING_REPLY] = false
                _uiState.update {
                    it.copy(
                        messages = updated,
                        refinedIntention = reply.refinedIntention,
                        readyForAction = reply.readyForAction,
                        isLoading = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                savedState[AWAITING_REPLY] = false
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "导师暂时无法回答，请重试。") }
            }
        }
    }

    private fun restoreMessages(): List<MentorMessage> {
        val roles = savedState.get<ArrayList<String>>(MESSAGE_ROLES).orEmpty()
        val contents = savedState.get<ArrayList<String>>(MESSAGE_CONTENTS).orEmpty()
        return roles.zip(contents).mapNotNull { (role, content) ->
            runCatching { MentorMessage(MentorMessageRole.valueOf(role), content) }.getOrNull()
        }.takeLast(MAX_SAVED_MESSAGES)
    }

    private fun saveMessages(messages: List<MentorMessage>) {
        val bounded = messages.takeLast(MAX_SAVED_MESSAGES)
        savedState[MESSAGE_ROLES] = ArrayList(bounded.map { it.role.name })
        savedState[MESSAGE_CONTENTS] = ArrayList(bounded.map { it.content })
    }

    private fun restoreResources(values: List<String>): Set<QuestResource> =
        values.mapNotNull { runCatching { QuestResource.valueOf(it) }.getOrNull() }.toSet().ifEmpty { setOf(QuestResource.PHONE) }

    companion object {
        const val INTENTION = "mentor_intention"
        const val MINUTES = "mentor_minutes"
        const val ENERGY = "mentor_energy"
        const val RESOURCES = "mentor_resources"
        private const val INPUT = "mentor_input"
        private const val MESSAGE_ROLES = "mentor_message_roles"
        private const val MESSAGE_CONTENTS = "mentor_message_contents"
        private const val REFINED_INTENTION = "mentor_refined_intention"
        private const val READY_FOR_ACTION = "mentor_ready_for_action"
        private const val AWAITING_REPLY = "mentor_awaiting_reply"
        private const val GENERATED_QUEST_ID = "mentor_generated_quest_id"
    }
}

const val MAX_USER_TURNS = 8
private const val MAX_SAVED_MESSAGES = 20
