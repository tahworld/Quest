package com.vika.quest.ai

import com.vika.quest.model.NewQuest

class AiQuestDraftValidator {
    fun validate(
        draft: AiQuestDraft,
        context: QuestGenerationContext,
    ): NewQuest {
        require(context.activeGoals.any { it.id == draft.goalId }) {
            "AI 返回了未知的 goalId"
        }
        require(draft.estimatedMinutes in 1..context.availableMinutes) {
            "AI 返回的任务时长超出当前可用时间"
        }
        require(draft.title.isNotBlank()) { "AI 返回的任务标题为空" }
        require(draft.instruction.isNotBlank()) { "AI 返回的执行说明为空" }
        require(draft.completionCriteria.isNotEmpty()) { "AI 未返回完成标准" }
        require(draft.completionCriteria.none(String::isBlank)) { "AI 返回了空的完成标准" }
        require(draft.difficulty in 1..5) { "AI 返回的难度不在 1 到 5 之间" }

        return NewQuest(
            goalId = draft.goalId,
            title = draft.title.trim(),
            instruction = draft.instruction.trim(),
            estimatedMinutes = draft.estimatedMinutes,
            completionCriteria = draft.completionCriteria.map(String::trim).distinct(),
            difficulty = draft.difficulty,
        )
    }
}
