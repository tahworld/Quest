package com.vika.quest.ai

class FakeAiProvider : AiProvider {
    override suspend fun generateQuest(context: QuestGenerationContext): AiQuestDraft {
        require(context.availableMinutes > 0) { "可用时间必须大于 0" }
        require(context.energy in 1..5) { "精力必须在 1 到 5 之间" }

        val goal = context.activeGoals
            .sortedByDescending(GoalSnapshot::priority)
            .firstOrNull()
            ?: error("至少需要一个目标才能生成 Quest")
        val previous = context.recentQuests.firstOrNull { it.goalId == goal.id && it.completed }
        val estimatedMinutes = minOf(context.availableMinutes, 15)

        return AiQuestDraft(
            goalId = goal.id,
            skill = null,
            title = "确定“${goal.name}”的下一项可验证产出",
            estimatedMinutes = estimatedMinutes,
            instruction = buildString {
                append("围绕“${goal.name}”写下一项能在本次时间内完成的具体产出，并立即完成它。")
                if (previous != null) {
                    append("优先承接上一次任务“${previous.title}”留下的结果。")
                }
            },
            completionCriteria = listOf(
                "产出一份可保存的文字、清单或证据",
                "写明下一次可以从哪里继续",
            ),
            difficulty = if (context.energy <= 2) 1 else 2,
            chainTitle = context.unfinishedChain?.title ?: "推进${goal.name}",
            reason = if (previous == null) {
                "建立该目标的第一个可观察进展。"
            } else {
                "承接最近一次已完成的 Quest，保持进展连续。"
            },
        )
    }

    override suspend fun analyzeQuestResult(input: QuestResultInput): QuestResultAnalysis =
        QuestResultAnalysis(
            summary = if (input.feedback.completed) {
                "已完成：${input.feedback.result}"
            } else {
                "未完成：${input.feedback.skipReason.orEmpty()}"
            },
            suggestedNextFocus = input.feedback.result.takeIf(String::isNotBlank),
        )
}
