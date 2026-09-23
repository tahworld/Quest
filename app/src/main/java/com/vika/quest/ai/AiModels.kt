package com.vika.quest.ai

data class QuestGenerationContext(
    val activeGoals: List<GoalSnapshot>,
    val skills: List<SkillSnapshot>,
    val recentQuests: List<QuestSnapshot>,
    val recentFeedback: List<FeedbackSnapshot>,
    val availableMinutes: Int,
    val energy: Int,
    val resources: Set<QuestResource>,
    val unfinishedChain: ChainSnapshot?,
) {
    init {
        require(availableMinutes > 0) { "可用时间必须大于 0" }
        require(energy in 1..5) { "精力必须在 1 到 5 之间" }
    }
}

data class GoalSnapshot(
    val id: String,
    val name: String,
    val description: String,
    val priority: Int,
)

data class SkillSnapshot(
    val id: String,
    val goalId: String,
    val name: String,
    val level: Int,
    val progress: Int,
)

data class QuestSnapshot(
    val id: String,
    val goalId: String,
    val chainId: String?,
    val title: String,
    val result: String?,
    val completed: Boolean,
)

data class FeedbackSnapshot(
    val questId: String,
    val completed: Boolean,
    val actualMinutes: Int,
    val difficultyRating: Int,
    val usefulnessRating: Int,
    val result: String,
    val skipReason: String?,
)

data class ChainSnapshot(
    val id: String,
    val goalId: String,
    val title: String,
    val description: String,
)

enum class QuestResource {
    PHONE,
    COMPUTER,
    QUIET_THINKING,
    CAN_MOVE_OR_EXERCISE,
}

data class AiQuestDraft(
    val goalId: String,
    val skill: String?,
    val title: String,
    val estimatedMinutes: Int,
    val instruction: String,
    val completionCriteria: List<String>,
    val difficulty: Int,
    val chainTitle: String?,
    val reason: String,
)

data class QuestResultInput(
    val quest: QuestSnapshot,
    val feedback: FeedbackSnapshot,
)

data class QuestResultAnalysis(
    val summary: String,
    val suggestedNextFocus: String?,
)
