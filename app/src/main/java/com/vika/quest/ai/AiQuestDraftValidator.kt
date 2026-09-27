package com.vika.quest.ai

import com.vika.quest.model.NewQuest

class AiQuestDraftValidator {
    private val vagueTitles = listOf(
        Regex("(?i)^learn about\\b"), Regex("(?i)^continue working on\\b"), Regex("(?i)^make progress on\\b"),
        Regex("(?i)^create a small verifiable result\\b"), Regex("^学习.{0,20}$"), Regex("^继续(推进|做|处理).{0,20}$"),
        Regex("^推进.{0,20}$"), Regex("^(创建|完成)一个(小的)?可验证(的)?(结果|成果).{0,12}$"),
        Regex("^(读一读|了解一下|想一想|继续学习).{0,30}$"),
    )

    fun validate(draft: AiQuestDraft, context: QuestGenerationContext): NewQuest {
        val title = draft.title.trim()
        val steps = draft.steps.map(String::trim).filter(String::isNotEmpty).distinct()
        val criteria = draft.completionCriteria.map(String::trim).filter(String::isNotEmpty).distinct()
        require(title.length in 6..120 && vagueTitles.none { it.containsMatchIn(title) }) { "AI 返回的标题过于笼统" }
        require(draft.reason.isNotBlank() && draft.reason.length <= 500) { "AI 未说明任务为什么适合当前情况" }
        require(draft.estimatedMinutes in 1..context.availableMinutes) { "AI 返回的任务时长超出当前可用时间" }
        require(steps.size in 1..6 && steps.all { it.length <= 500 }) { "AI 必须返回 1 到 6 个具体步骤" }
        require(steps.none { step -> vagueTitles.any { it.containsMatchIn(step) } }) { "AI 返回了笼统步骤" }
        val actionText = steps.joinToString(" ")
        if (QuestResource.COMPUTER !in context.resources) {
            require(!Regex("打开电脑|使用电脑|在电脑上|运行电脑软件").containsMatchIn(actionText)) { "行动需要当前不可用的电脑" }
        }
        if (QuestResource.CAN_MOVE_OR_EXERCISE !in context.resources || context.energy <= 2) {
            require(!Regex("冲刺|高强度|快跑|负重训练").containsMatchIn(actionText)) { "行动强度与当前条件不符" }
        }
        require(criteria.size in 1..5 && criteria.all { it.length <= 500 }) { "AI 必须返回 1 到 5 条完成标准" }
        require(draft.expectedOutput.isNotBlank() && draft.expectedOutput.length <= 800 && vagueTitles.none { it.containsMatchIn(draft.expectedOutput.trim()) }) { "AI 未返回明确产出" }
        val repeatableMovement = context.currentDirection?.description?.startsWith("身体活动：") == true
        require(repeatableMovement || context.recentQuests.none { it.status == "COMPLETED" && it.title.trim() == title }) { "AI 重复了近期已完成的行动" }
        require(context.rejectedQuest?.title?.trim() != title) { "AI 重复了刚刚拒绝的行动" }
        require(draft.relatedGoalId == null || context.goals.any { it.id == draft.relatedGoalId }) { "AI 返回了未知目标" }
        require(draft.relatedProjectId == null || context.projects.any { it.id == draft.relatedProjectId }) { "AI 返回了未知项目" }
        return NewQuest(
            goalId = draft.relatedGoalId ?: context.currentDirection?.id, projectId = draft.relatedProjectId, title = title,
            reason = draft.reason.trim(), steps = steps, instruction = steps.joinToString("\n"),
            estimatedMinutes = draft.estimatedMinutes, completionCriteria = criteria,
            expectedOutput = draft.expectedOutput.trim(), difficulty = if (context.energy <= 2) 1 else 2,
            sourceIntention = context.userIntention, sourceAvailableMinutes = context.availableMinutes,
            sourceEnergy = context.energy, sourceResources = context.resources.map(Enum<*>::name),
        )
    }
}

class AiQuestResultValidator {
    fun validate(value: AiQuestResultAnalysis): AiQuestResultAnalysis {
        require(value.summary.isNotBlank() && value.summary.length <= 800) { "AI 分析缺少总结" }
        require(value.evidence.size <= 5 && value.evidence.none(String::isBlank)) { "AI 返回的证据无效" }
        require(value.insights.size <= 5 && value.insights.none(String::isBlank)) { "AI 返回的洞察无效" }
        require(value.suggestedNextStep.length <= 800 && value.projectProgress.length <= 1200) { "AI 分析文本过长" }
        require(value.memoriesToSave.size <= 3 && value.memoriesToSave.all { it.type.isNotBlank() && it.content.length in 20..500 && it.importance in 1..5 }) { "AI 返回的长期记忆无效" }
        return value.copy(
            evidence = value.evidence.map(String::trim).distinct(), insights = value.insights.map(String::trim).distinct(),
            memoriesToSave = value.memoriesToSave.filter { it.importance >= 3 }.distinctBy { it.content.trim().lowercase() },
        )
    }
}
