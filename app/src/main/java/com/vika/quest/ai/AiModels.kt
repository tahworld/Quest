package com.vika.quest.ai

data class QuestGenerationContext(
    val userIntention: String,
    val availableMinutes: Int,
    val energy: Int,
    val resources: Set<QuestResource>,
    val goals: List<GoalSnapshot>,
    val projects: List<ProjectSnapshot>,
    val memories: List<MemorySnapshot>,
    val recentQuests: List<QuestSnapshot>,
    val recentRejections: List<RejectionSnapshot>,
    val userPreferences: List<PreferenceSnapshot>,
    val rejectedQuest: QuestSnapshot? = null,
    val rejectionReason: String? = null,
) {
    init { require(availableMinutes > 0); require(energy in 1..5) }
}

data class GoalSnapshot(val id: String, val name: String, val description: String, val priority: Int)
data class ProjectSnapshot(val id: String, val goalId: String?, val name: String, val description: String, val currentState: String)
data class MemorySnapshot(val type: String, val content: String, val importance: Int)
data class QuestSnapshot(val id: String, val goalId: String?, val projectId: String?, val title: String, val status: String, val resultSummary: String?)
data class RejectionSnapshot(val questTitle: String, val reason: String, val details: String?)
data class PreferenceSnapshot(val key: String, val value: String)

data class ClarificationExchange(val question: String, val answer: String)

data class QuestClarificationContext(
    val userIntention: String,
    val availableMinutes: Int,
    val energy: Int,
    val resources: Set<QuestResource>,
    val projects: List<ProjectSnapshot>,
    val memories: List<MemorySnapshot>,
    val userPreferences: List<PreferenceSnapshot>,
    val history: List<ClarificationExchange>,
) {
    init {
        require(availableMinutes > 0)
        require(energy in 1..5)
        require(projects.size <= 2)
        require(memories.size <= 3)
        require(history.size <= 3)
    }
}

enum class ClarificationStatus { ASK, READY }

data class AiClarificationTurn(
    val status: ClarificationStatus,
    val question: String?,
    val options: List<String>,
    val allowCustomAnswer: Boolean,
    val refinedIntention: String?,
)

enum class QuestResource { PHONE, COMPUTER, QUIET_THINKING, CAN_MOVE_OR_EXERCISE }

data class AiQuestDraft(
    val title: String,
    val reason: String,
    val estimatedMinutes: Int,
    val steps: List<String>,
    val completionCriteria: List<String>,
    val expectedOutput: String,
    val relatedGoalId: String?,
    val relatedProjectId: String?,
)

data class QuestResultAnalysisContext(
    val goal: GoalSnapshot?,
    val project: ProjectSnapshot?,
    val memories: List<MemorySnapshot>,
    val quest: QuestSnapshot,
    val steps: List<String>,
    val completionCriteria: List<String>,
    val resultText: String,
    val actualMinutes: Int?,
    val difficultyRating: String?,
    val usefulnessRating: Int?,
    val userPreferences: List<PreferenceSnapshot> = emptyList(),
)

data class ProposedMemory(val type: String, val content: String, val importance: Int)
data class AiQuestResultAnalysis(
    val summary: String,
    val evidence: List<String>,
    val insights: List<String>,
    val projectProgress: String,
    val suggestedNextStep: String,
    val memoriesToSave: List<ProposedMemory>,
)

data class AiConnectionSettings(val baseUrl: String, val model: String, val apiKey: String)
data class AiConnectionResult(val success: Boolean, val message: String, val availableModels: List<String> = emptyList())
