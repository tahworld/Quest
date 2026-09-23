package com.vika.quest.ai

import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.MemoryRepository
import com.vika.quest.data.repository.ProjectRepository
import com.vika.quest.data.repository.QuestRepository
import com.vika.quest.data.repository.UserPreferenceRepository

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
    }

    suspend fun build(
        intention: String,
        availableMinutes: Int,
        energy: Int,
        resources: Set<QuestResource>,
        rejectedQuestId: String? = null,
        rejectionReason: String? = null,
    ): QuestGenerationContext {
        val goalEntities = goals.getTop(GOAL_LIMIT)
        val projectEntities = projects.getActive(PROJECT_LIMIT)
        val memoryEntities = memories.getImportant(MEMORY_LIMIT)
        val questEntities = quests.getRecentQuests(QUEST_LIMIT)
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
            userPreferences = preferences.getRecent(PREFERENCE_LIMIT).map { PreferenceSnapshot(it.key.take(100), it.value.take(500)) },
            rejectedQuest = rejectedQuestId?.let { id -> questEntities.firstOrNull { it.id == id } ?: quests.getQuest(id) }?.let { QuestSnapshot(it.id, it.goalId, it.projectId, it.title, it.status.name, results[it.id]?.resultText?.take(500)) },
            rejectionReason = rejectionReason?.take(300),
        )
    }
}
