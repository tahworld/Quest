package com.vika.quest.ai

import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.MemoryRepository
import com.vika.quest.data.repository.ProjectRepository
import com.vika.quest.data.repository.QuestRepository
import com.vika.quest.data.repository.UserPreferenceRepository
import com.vika.quest.data.repository.UserPreferenceKeys
import com.vika.quest.data.local.entity.QuestEntity

class ContextBuilder(
    private val goals: GoalRepository,
    private val projects: ProjectRepository,
    private val memories: MemoryRepository,
    private val quests: QuestRepository,
    private val preferences: UserPreferenceRepository,
) {
    companion object {
        const val GOAL_LIMIT = 5
        const val PROJECT_LIMIT = 5
        const val MEMORY_LIMIT = 8
        const val QUEST_LIMIT = 8
        const val REJECTION_LIMIT = 5
        const val PREFERENCE_LIMIT = 10
        const val CLARIFICATION_PROJECT_LIMIT = 2
        const val CLARIFICATION_MEMORY_LIMIT = 3
        const val CLARIFICATION_HISTORY_LIMIT = 3
        const val MENTOR_PROJECT_LIMIT = 2
        const val MENTOR_MEMORY_LIMIT = 3
        const val MENTOR_MESSAGE_LIMIT = 12
        const val REVIEW_MEMORY_LIMIT = 3
        const val REVIEW_HISTORY_LIMIT = 3
    }

    suspend fun build(
        intention: String,
        availableMinutes: Int,
        energy: Int,
        resources: Set<QuestResource>,
        rejectedQuestId: String? = null,
        rejectionReason: String? = null,
        goalIdOverride: String? = null,
    ): QuestGenerationContext {
        val selected = (goalIdOverride ?: preferences.get(UserPreferenceKeys.ACTIVE_DIRECTION_GOAL_ID))?.let { goals.getGoal(it) }
        val goalEntities = (listOfNotNull(selected) + goals.getTop(GOAL_LIMIT)).distinctBy { it.id }.take(GOAL_LIMIT)
        val projectEntities = projects.getActive(PROJECT_LIMIT * 2).sortedBy { if (it.goalId == selected?.id) 0 else 1 }.take(PROJECT_LIMIT)
        val memoryEntities = memories.getImportant(MEMORY_LIMIT * 2).sortedBy { if (it.goalId == selected?.id) 0 else 1 }.take(MEMORY_LIMIT)
        val questEntities = quests.getRecentQuests(QUEST_LIMIT * 3).let { recent ->
            (recent.filter { it.goalId == selected?.id }.take(QUEST_LIMIT / 2) + recent).distinctBy { it.id }.take(QUEST_LIMIT)
        }
        val results = quests.getResults(questEntities.map { it.id }).associateBy { it.questId }
        val questById = questEntities.associateBy { it.id }
        val rejections = quests.getRecentRejections(REJECTION_LIMIT)
        return QuestGenerationContext(
            userIntention = intention.trim().take(2_000), availableMinutes = availableMinutes, energy = energy, resources = resources,
            goals = goalEntities.map { GoalSnapshot(it.id, it.name.take(200), it.description.take(500), it.priority) },
            projects = projectEntities.map { ProjectSnapshot(it.id, it.goalId, it.name.take(200), it.description.take(500), it.currentState.take(1_200)) },
            memories = memoryEntities.map { MemorySnapshot(it.type.take(80), it.content.take(500), it.importance) },
            recentQuests = questEntities.map { QuestSnapshot(it.id, it.goalId, it.projectId, it.title.take(200), it.status.name, results[it.id]?.resultText?.take(500)) },
            recentRejections = rejections.map { RejectionSnapshot(questById[it.questId]?.title.orEmpty().take(200), it.reason.storageValue, it.details?.take(300)) },
            userPreferences = getUserPreferences(),
            currentDirection = selected?.let { GoalSnapshot(it.id, it.name.take(200), it.description.take(500), it.priority) }
                ?: goalEntities.firstOrNull()?.let { GoalSnapshot(it.id, it.name.take(200), it.description.take(500), it.priority) },
            rejectedQuest = rejectedQuestId?.let { id -> questEntities.firstOrNull { it.id == id } ?: quests.getQuest(id) }?.let { QuestSnapshot(it.id, it.goalId, it.projectId, it.title, it.status.name, results[it.id]?.resultText?.take(500)) },
            rejectionReason = rejectionReason?.take(300),
        )
    }

    suspend fun buildClarification(
        intention: String,
        availableMinutes: Int,
        energy: Int,
        resources: Set<QuestResource>,
        history: List<ClarificationExchange>,
    ) = QuestClarificationContext(
        userIntention = intention.trim().take(2_000),
        availableMinutes = availableMinutes,
        energy = energy,
        resources = resources,
        projects = projects.getActive(CLARIFICATION_PROJECT_LIMIT).map {
            ProjectSnapshot(it.id, it.goalId, it.name.take(160), it.description.take(300), it.currentState.take(500))
        },
        memories = memories.getImportant(CLARIFICATION_MEMORY_LIMIT).map {
            MemorySnapshot(it.type.take(80), it.content.take(300), it.importance)
        },
        userPreferences = getUserPreferences(),
        history = history.takeLast(CLARIFICATION_HISTORY_LIMIT).map {
            ClarificationExchange(it.question.take(240), it.answer.take(500))
        },
    )

    suspend fun buildMentorConversation(
        intention: String,
        availableMinutes: Int,
        energy: Int,
        resources: Set<QuestResource>,
        messages: List<MentorMessage>,
    ) = MentorConversationContext(
        userIntention = intention.trim().take(2_000),
        availableMinutes = availableMinutes,
        energy = energy,
        resources = resources,
        projects = projects.getActive(MENTOR_PROJECT_LIMIT).map {
            ProjectSnapshot(it.id, it.goalId, it.name.take(160), it.description.take(300), it.currentState.take(500))
        },
        memories = memories.getImportant(MENTOR_MEMORY_LIMIT).map {
            MemorySnapshot(it.type.take(80), it.content.take(300), it.importance)
        },
        userPreferences = getUserPreferences(),
        messages = messages.takeLast(MENTOR_MESSAGE_LIMIT).map {
            MentorMessage(it.role, it.content.trim().take(1_000))
        },
    )

    suspend fun getUserPreferences(): List<PreferenceSnapshot> =
        preferences.getPromptPreferences(PREFERENCE_LIMIT).map { PreferenceSnapshot(it.key.take(100), it.value.take(2_000)) }

    suspend fun buildReview(quest: QuestEntity, question: String, history: List<ActionReviewExchange>): QuestReviewContext {
        val goal = quest.goalId?.let { goals.getGoal(it) }
        val project = quest.projectId?.let { projects.getProject(it) }
        return QuestReviewContext(
            quest = QuestSnapshot(quest.id, quest.goalId, quest.projectId, quest.title.take(200), quest.status.name, null),
            reason = quest.reason.take(500), steps = quest.steps.take(6).map { it.take(500) },
            completionCriteria = quest.completionCriteria.take(5).map { it.take(500) }, expectedOutput = quest.expectedOutput.take(800),
            sourceIntention = quest.sourceIntention.take(2_000), availableMinutes = quest.sourceAvailableMinutes,
            energy = quest.sourceEnergy, resources = quest.sourceResources.mapNotNull { runCatching { QuestResource.valueOf(it) }.getOrNull() }.toSet(),
            goal = goal?.let { GoalSnapshot(it.id, it.name.take(200), it.description.take(500), it.priority) },
            project = project?.let { ProjectSnapshot(it.id, it.goalId, it.name.take(200), it.description.take(500), it.currentState.take(600)) },
            memories = memories.getImportant(REVIEW_MEMORY_LIMIT * 3)
                .filter { it.goalId == null || it.goalId == quest.goalId || it.projectId == quest.projectId && quest.projectId != null }
                .take(REVIEW_MEMORY_LIMIT).map { MemorySnapshot(it.type.take(80), it.content.take(300), it.importance) },
            userPreferences = getUserPreferences(),
            question = question.trim().take(500), history = history.takeLast(REVIEW_HISTORY_LIMIT).map { ActionReviewExchange(it.question.take(500), it.answer.take(1_000)) },
        )
    }
}
