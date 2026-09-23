package com.vika.quest.ai

class FakeAiProvider : AiProvider {
    override suspend fun generateQuest(context: QuestGenerationContext): AiQuestDraft {
        val intention = context.userIntention.trim()
        val project = context.projects.firstOrNull()
        val goal = context.goals.firstOrNull()
        val subject = intention.ifBlank { project?.name ?: goal?.name ?: "当前最重要的方向" }
        val previous = context.rejectedQuest
        return AiQuestDraft(
            title = "写出“${subject.take(28)}”的三项下一步清单",
            reason = if (previous == null) "把当前方向收敛成今天可以执行和保存的具体下一步。" else "已避开刚才被拒绝的任务，并把范围缩小为一份可直接使用的清单。",
            estimatedMinutes = minOf(15, context.availableMinutes),
            steps = listOf("打开备忘录，写下你希望推进的具体结果", "列出三个按顺序可执行的下一步，每步写清动作和对象", "圈出最先执行的一步，并补充开始所需的材料"),
            completionCriteria = listOf("清单中恰好包含三个可执行步骤", "每一步都有明确动作和对象", "已标记下一次首先执行的步骤"),
            expectedOutput = "一份可保存的三步行动清单，其中第一步已明确标记。",
            relatedGoalId = goal?.id,
            relatedProjectId = project?.id,
        )
    }

    override suspend fun analyzeQuestResult(context: QuestResultAnalysisContext) = AiQuestResultAnalysis(
        summary = "已完成“${context.quest.title}”，并留下了可继续使用的结果。",
        evidence = listOf(context.resultText.take(200)),
        insights = listOf("下一步应直接承接本次产出，而不是重新开始。"),
        projectProgress = context.resultText.take(500),
        suggestedNextStep = "打开本次产出，执行其中标记的第一步。",
        memoriesToSave = emptyList(),
    )

    override suspend fun testConnection(settings: AiConnectionSettings) = AiConnectionResult(true, "离线测试提供器可用")
}
