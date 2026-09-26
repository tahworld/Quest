package com.vika.quest.ai

class FakeAiProvider : AiProvider {
    override suspend fun clarifyQuest(context: QuestClarificationContext): AiClarificationTurn = when (context.history.size) {
        0 -> AiClarificationTurn(
            status = ClarificationStatus.ASK,
            question = if (context.userIntention.isBlank()) "你现在更想推进哪一类事情？" else "这次结束时，你最希望留下什么可见成果？",
            options = if (context.userIntention.isBlank()) listOf("学习或训练", "推进一个项目", "解决眼前问题") else listOf("一份清单或记录", "一个可以使用的成品", "一个经过验证的结论"),
            allowCustomAnswer = true,
            refinedIntention = null,
        )
        1 -> AiClarificationTurn(
            status = ClarificationStatus.ASK,
            question = "当前最需要优先解决的限制是什么？",
            options = listOf("时间很少", "信息不足", "不知道第一步", "缺少可用工具"),
            allowCustomAnswer = true,
            refinedIntention = null,
        )
        else -> AiClarificationTurn(
            status = ClarificationStatus.READY,
            question = null,
            options = emptyList(),
            allowCustomAnswer = false,
            refinedIntention = refinedIntention(context),
        )
    }

    override suspend fun continueMentorConversation(context: MentorConversationContext): AiMentorReply {
        val userTurns = context.messages.count { it.role == MentorMessageRole.USER }
        val latest = context.messages.lastOrNull { it.role == MentorMessageRole.USER }?.content.orEmpty()
        val direction = context.userIntention.ifBlank { latest }.ifBlank { "找出现在最值得推进的一件事" }
        return when (userTurns) {
            0 -> AiMentorReply(
                answer = "我会先围绕“${direction.take(36)}”判断当前最值得解决的部分，再把讨论收束成一次可执行行动。",
                followUpQuestion = "这件事当前最卡住你的具体问题是什么？",
                refinedIntention = "$direction，并找出当前最关键的阻碍。",
                readyForAction = false,
            )
            1 -> AiMentorReply(
                answer = "你提到的“${latest.take(48)}”已经把问题范围缩小了。下一步需要确定本轮讨论要留下什么结果。",
                followUpQuestion = "聊完以后，你希望得到判断、方案，还是一份可以直接执行的清单？",
                refinedIntention = "$direction。重点处理：${latest.take(300)}。",
                readyForAction = true,
            )
            else -> AiMentorReply(
                answer = "现有信息已经足够形成行动。先保留你的方向，再把最近的回答作为执行约束。",
                followUpQuestion = null,
                refinedIntention = "$direction。结合补充信息：${latest.take(300)}，形成一个可立即执行并留下成果的行动。",
                readyForAction = true,
            )
        }
    }

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

    private fun refinedIntention(context: QuestClarificationContext): String {
        val direction = context.userIntention.ifBlank { "选择一个符合长期方向且现在可以推进的事项" }
        val answers = context.history.joinToString("；") { it.answer }
        return "$direction。结合用户补充：$answers。在 ${context.availableMinutes} 分钟内形成明确、可保存的产出。"
    }
}
