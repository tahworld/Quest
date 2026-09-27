package com.vika.quest.ai

class FakeAiProvider : AiProvider {
    override suspend fun reviewQuest(context: QuestReviewContext): AiActionReviewReply {
        val blocked = listOf("太难", "没时间", "时间不够", "做不了", "无法", "没电脑", "不方便").any { it in context.question }
        if (!blocked || context.quest.status != "PENDING") return AiActionReviewReply(
            answer = "这一步的目标是“${context.expectedOutput.take(100)}”。先做第 1 步：${context.steps.firstOrNull().orEmpty()}；满足“${context.completionCriteria.firstOrNull().orEmpty()}”就可以结束。",
            adjustmentReason = null, proposedQuest = null,
        )
        val draft = AiQuestDraft(
            title = "把“${context.quest.title.take(20)}”缩成一条可执行记录",
            reason = "你提出的条件冲突值得先解除；在原方向上缩小范围。",
            estimatedMinutes = minOf(5, context.availableMinutes),
            steps = listOf("在当前可用设备上记录卡住的具体一步和缺少的条件", "写出一个此刻能完成的替代动作，并记录执行结果"),
            completionCriteria = listOf("留下一条具体障碍和一次替代动作的执行记录"),
            expectedOutput = "一条包含原行动障碍、替代动作和实际结果的记录。",
            relatedGoalId = context.goal?.id, relatedProjectId = context.project?.id,
        )
        return AiActionReviewReply("你的条件可能不适合原步骤，先不要硬做。可以缩成一条可保存的执行记录。", "当前条件与原步骤冲突。", draft)
    }

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
        val goal = context.currentDirection ?: context.goals.firstOrNull()
        val subject = intention.ifBlank { goal?.name ?: project?.name ?: "当前方向" }.take(28)
        val last = context.recentQuests.firstOrNull { it.status == "COMPLETED" && it.goalId == goal?.id }
        val blocked = context.rejectedQuest != null || context.recentRejections.isNotEmpty() || context.recentQuests.firstOrNull { it.goalId == goal?.id }?.status == "ABANDONED"
        val reason = when {
            blocked -> "承接同一方向，并把上次做不了的步骤缩小到当前条件允许的范围。"
            last?.resultSummary != null -> "承接你上次留下的“${last.resultSummary.take(60)}”，不重复已做过的起点。"
            else -> "从你选定的方向出发，先留下一个能接续的具体结果。"
        }
        val category = goal?.description.orEmpty()
        val content = when {
            intention.isNotBlank() -> AiQuestDraft(
                if (last?.resultSummary == null) "记录“$subject”的一个具体判断和依据" else "核实“${last?.resultSummary.orEmpty().take(14)}”里的一个缺口",
                reason, minOf(10, context.availableMinutes),
                if (last?.resultSummary == null) listOf("打开备忘录，写下“$subject”中此刻最需要判断的一个具体问题", "针对该问题列出一条已知事实和一个待核实的缺口", "写下基于这些信息可以立即采取的一个下一步")
                else listOf("打开上次的记录：“${last?.resultSummary.orEmpty().take(70)}”", "针对上次留下的缺口找一条可用证据或明确当前拿不到证据的原因", "记下因此改变的下一步"),
                listOf("记录中有一条事实、一个缺口和一个针对当前想法的下一步"), "一条关于“$subject”的判断记录，包含事实、缺口和下一步。", goal?.id, project?.id,
            )
            "身体活动" in category -> if (QuestResource.CAN_MOVE_OR_EXERCISE in context.resources && context.energy >= 2) AiQuestDraft(
                if (last?.resultSummary == null) "做一段舒适步行并记录身体感受" else "参考上次感受步行并记录变化", reason, minOf(5, context.availableMinutes),
                listOf("选择安全、容易到达的路线，以舒适速度步行；不追求强度", "停下后记录走了多久和此刻的感受"),
                listOf("完成一段自己感觉舒适的步行并留下时长与感受"), "一条步行时长与身体感受记录。", goal?.id, null,
            ) else AiQuestDraft(
                "为下一次轻量活动记录一个可行窗口", "当前条件不适合直接训练，先排除开始的障碍。", minOf(5, context.availableMinutes),
                listOf("写下下一次可以安全走动的具体时段和地点", "列出目前阻止你开始的一项条件及一个替代方案"),
                listOf("记录包含具体时段、地点、障碍和替代方案"), "一条可执行的轻量活动安排记录。", goal?.id, null,
            )
            "阅读/学习" in category -> if (last?.resultSummary != null) AiQuestDraft(
                "为“${last?.resultSummary.orEmpty().take(14)}”写一个新例子", reason, minOf(5, context.availableMinutes),
                listOf("打开上次的阅读记录：“${last.resultSummary.take(70)}”", "用自己的话写出其中一个观点适用的具体例子"),
                listOf("新增一个与上次观点直接相关的具体例子"), "一条例子笔记，链接到上次阅读记录。", goal?.id, null,
            ) else AiQuestDraft(
                "从现有材料提取一个可复述的观点", reason, minOf(5, context.availableMinutes),
                listOf("打开“${goal?.name ?: "你已有的材料"}”对应的书籍或学习材料，选一小段", "合上材料，用自己的话写出一个观点和对应的原文位置"),
                listOf("留下一个用自己语言复述的观点和可回看的位置"), "一条包含观点与材料位置的阅读笔记。", goal?.id, null,
            )
            else -> AiQuestDraft(
                if (last?.resultSummary == null) "为“$subject”核实一个当前阻碍" else "核对“${last?.resultSummary.orEmpty().take(14)}”的下一项假设",
                listOf("查看“${last?.resultSummary?.take(70) ?: subject}”的实际进展，指出阻止下一步的一项具体问题", "用当前可用设备记录一个可以立即检验的小动作，并执行它", "写下执行后的观察或新发现"),
                listOf("记录中包含当前阻碍、已经执行的检验和实际观察"), "一条关于“$subject”的阻碍检验及观察记录。", goal?.id, project?.id,
            )
        }
        return if (!blocked) content else content.copy(
            title = "缩小范围：${content.title}",
            steps = content.steps.take(2),
            completionCriteria = content.completionCriteria.take(1),
            estimatedMinutes = minOf(5, context.availableMinutes),
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
