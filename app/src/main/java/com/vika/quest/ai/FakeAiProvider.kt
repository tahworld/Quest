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
        val resourceHint = context.resources
            .map { it.displayName() }
            .sorted()
            .joinToString("、")

        return AiQuestDraft(
            goalId = goal.id,
            skill = null,
            title = "为“${goal.name}”完成一个可验证的小成果",
            estimatedMinutes = estimatedMinutes,
            instruction = buildString {
                append("用 $estimatedMinutes 分钟，围绕“${goal.name}”完成一个可以保存或展示的具体成果。")
                if (resourceHint.isNotBlank()) {
                    append("当前可用条件：$resourceHint。")
                }
                append("不要只浏览资料，请留下文字、清单、草稿或截图证据。")
                if (previous != null) {
                    append("先查看上一次任务“${previous.title}”的结果，在它的基础上继续，不要从头开始。")
                }
            },
            completionCriteria = listOf(
                "留下一个可查看的成果（文字、清单、草稿或截图）",
                "写下一句话，说明下次从哪里继续",
            ),
            difficulty = if (context.energy <= 2) 1 else 2,
            chainTitle = context.unfinishedChain?.title ?: "持续推进“${goal.name}”",
            reason = if (previous == null) {
                "建立该目标的第一个可观察进展。"
            } else {
                "承接最近一次已完成的 Quest，保持进展连续。"
            },
        )
    }

    private fun QuestResource.displayName(): String = when (this) {
        QuestResource.PHONE -> "手机"
        QuestResource.COMPUTER -> "电脑"
        QuestResource.QUIET_THINKING -> "安静思考"
        QuestResource.CAN_MOVE_OR_EXERCISE -> "可以走动或运动"
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
